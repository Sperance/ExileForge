package com.sperance.exileforge.core.market

import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionMine
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.LotTerms
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.freshness
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.trade.LotMode
import kotlinx.coroutines.CancellationException

/**
 * Рынок игроков и лавка торговца. Все правила у сервера; клиент называет лот и печатает отказ. Сделка - один
 * запрос: ответ несёт снимок героя, а команды над своими лотами - ещё и свой рынок. Полка торговца едет в снимке героя.
 * Действия общие для экранов Города и для продажи из сундука героя. Витрина и торги читаются под одним фильтром.
 */
class MarketActions(
    private val repository: MarketRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val events: GameEvents,
) {
    private val api: GameApi get() = connection.api
    private val market: Market get() = repository.state.value

    private fun market(transform: (Market) -> Market) = repository.update(transform)

    /**
     * Вкладка [tab]; витрина и торги читаются при входе на свою вкладку, если прочитаны не под нынешним фильтром, свой рынок («Мои лоты»,
     * «История») - если прочитан дольше полуминуты назад. Подсветка входа с плитки гаснет.
     */
    fun tab(tab: MarketTab) {
        market { it.copy(tab = tab, spotlight = null) }
        if (tab.mode != null && market.readUnder[tab] != showcaseFilter()) loadShowcase()
        if (tab.ownMarket) loadMyLots(fresh = true)
    }

    /**
     * Вход с плитки Города: есть дело - рынок открывается на «Моих лотах» с подсвеченной строкой [lotId] (первое дело,
     * [MarketChores.first]); дел нет - вкладка как была, без подсветки.
     */
    fun attend(lotId: String?) = market { if (lotId == null) it.copy(spotlight = null) else it.copy(tab = MarketTab.LOTS, spotlight = lotId) }

    fun filter(filter: AuctionFilter) = market { it.copy(filter = filter) }

    /**
     * Лоты одного продавца [sellerId] по имени [name] («Лоты игрока» карточки игрока): витрина, суженная до него, с первой
     * страницы; свои лоты при этом не прячутся.
     */
    fun sellerLots(sellerId: String, name: String) {
        market { it.copy(tab = MarketTab.SHOWCASE, filter = AuctionFilter(sellerId = sellerId, sellerName = name, showOwnLots = it.filter.showOwnLots || sellerId == heroes.heroId)) }
        loadShowcase()
    }

    /**
     * Страница активной вкладки - витрина или торги - с первой страницы под фильтром, как он стоит; другая читается при входе на свою
     * вкладку ([tab]). На вкладках без страницы обе лишь помечены к чтению. Не загрузилась - отметка отказа ([Market.showcaseFailed]).
     */
    fun loadShowcase() {
        val tab = market.tab
        if (tab.mode == null) return market { it.copy(readUnder = emptyMap()) }
        trade(restart = true) {
            val read = try {
                page(tab)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if ((e as? ApiFailure)?.code != LEVEL_GATE) market { it.copy(showcaseFailed = true) }
                throw e
            }
            market { read(it.copy(showcaseFailed = false)) }
        }
    }

    /** Первая страница вкладки [tab] под фильтром, как он стоит: что положить в рынок; у вкладки без страницы - ничего. */
    private suspend fun page(tab: MarketTab): (Market) -> Market {
        val mode = tab.mode ?: return { it }
        val filter = showcaseFilter()
        val page = api.auction.search(heroes.heroId, filter, listOf(""), mode)
        return { state ->
            val read = state.readUnder + (tab to filter)
            if (mode == LotMode.BID) state.copy(auctions = page, readUnder = read) else state.copy(showcase = page, readUnder = read)
        }
    }

    /**
     * «Показать ещё»: следующая страница по курсору под уже показанными лотами. Чтение не перезапускается - это
     * уронило бы свежую первую страницу, - а страница остаётся, только пока фильтр и показанные страницы те же.
     */
    fun moreShowcase() = trade {
        val shown = market.showcase
        val trail = shown.nextTrail ?: return@trade
        val filter = showcaseFilter()
        val more = api.auction.search(heroes.heroId, filter, trail)
        market {
            val showcase = it.showcase
            if (showcase.cursors != shown.cursors || showcase.next != shown.next || showcaseFilter() != filter) it else it.copy(showcase = showcase.followedBy(more))
        }
    }

    /** «Показать ещё» торгов: следующая страница по курсору, как у витрины ([moreShowcase]). */
    fun moreAuctions() = trade {
        val shown = market.auctions
        val trail = shown.nextTrail ?: return@trade
        val filter = showcaseFilter()
        val more = api.auction.search(heroes.heroId, filter, trail, LotMode.BID)
        market {
            val auctions = it.auctions
            if (auctions.cursors != shown.cursors || auctions.next != shown.next || showcaseFilter() != filter) it else it.copy(auctions = auctions.followedBy(more))
        }
    }

    /** Фильтр, как его читает сервер: свои лоты убраны, если не просили, названия на языке экрана. */
    private fun showcaseFilter(): AuctionFilter = market.filter.copy(excludeSellerId = if (market.filter.showOwnLots) "" else heroes.heroId, lang = uiLanguage.code)

    /**
     * [glance] - площадь Города: без полосы. Ниже уровня аукциона площадь не спрашивает свои лоты вовсе - замок на плитке здания;
     * отказ сервера по уровню здесь молчит.
     */
    fun loadMyLots(glance: Boolean = false, fresh: Boolean = false) = trade(key = Reads.LOTS, glance = glance, maxAgeMs = freshness(fresh)) {
        mine(api.auction.mine(heroes.heroId))
    }

    /** Два списка аукциона; торговец - своё здание его полка едет в снимке героя. */
    fun loadAuction(fresh: Boolean = false) {
        loadShowcase()
        loadMyLots(fresh = fresh)
    }

    fun buyOffer(offerId: String) = trade(writing = true) {
        restocked { api.merchant.buy(heroes.heroId, offerId) }
    }

    /**
     * Покупка с полки, что успела смениться: отказ `CH_017` - не ошибка игрока, а старая витрина - герой
     * перечитывается (полка едет в его снимке), тост «Витрина обновилась».
     */
    private suspend fun restocked(buy: suspend () -> Unit) {
        try {
            buy()
        } catch (e: ApiFailure) {
            if (e.code != RESTOCKED) throw e
            heroes.stale()
            events.heroChanged()
            notices.toast(ui("merchant.restocked"), NoticeKind.DONE)
        }
    }

    /**
     * Трофеи босса [boss] - на его уникалку (Предначертание «Трофейщик»): копия в тайнике, тост называет её. Отказы
     * `FT_003`-`FT_005` - словами сервера.
     */
    fun tradeTrophy(boss: String) = trade(writing = true) {
        val bought = api.merchant.trophy(heroes.heroId, boss)
        notices.toast(ui("fate.trophy_traded", equipmentTitle(bought.item.template)), NoticeKind.LOOT)
    }

    /** Купленный лот уходит с витрины; страницы под ним остаются, сделка - в истории ([settled]). */
    fun buy(lotId: String) = trade(writing = true) {
        val lot = api.auction.buy(heroes.heroId, lotId)
        notices.toast(ui("toast.bought", lot.title))
        market { it.copy(showcase = it.showcase.without(lotId)) }
        settled()
    }

    /** Выставляет копию за цену в сферах - [priceOrb] код предмета-валюты; торгами - [terms], цена - выкуп. */
    fun sellEquipment(itemId: String, priceOrb: String, price: Long, terms: LotTerms = LotTerms()) = trade(writing = true) {
        listed(api.auction.sellEquipment(heroes.heroId, itemId, priceOrb, price, terms))
    }

    /** Выставляет стопку из сумки по коду; торгами - [terms]. */
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long, terms: LotTerms = LotTerms()) = trade(writing = true) {
        listed(api.auction.sellItem(heroes.heroId, code, amount, priceOrb, price, terms))
    }

    /**
     * Ставка [amount] сфер в торгах [lotId]: лот в торгах и в своих ставках - как ответил сервер; ставка от выкупа - выкуп, лот уходит из
     * торгов. Тост - «ставка принята» или «выкуплено»; свой рынок - заново ([settled]).
     */
    fun bid(lotId: String, amount: Long) = trade(writing = true) {
        val lot = api.auction.bid(heroes.heroId, lotId, amount)
        notices.toast(ui(if (lot.onSale) "toast.bid_placed" else "toast.bought", lot.title))
        traded(lot)
        settled()
    }

    /** Выкуп торгов [lotId] за цену выкупа: лот уходит из торгов и своих ставок, сделка - в истории ([settled]). */
    fun buyout(lotId: String) = trade(writing = true) {
        val lot = api.auction.buyout(heroes.heroId, lotId)
        notices.toast(ui("toast.bought", lot.title))
        traded(lot)
        settled()
    }

    /**
     * Сделка или ставка прошла: свой рынок (сделки, ставки, места) читается заново внутри команды - её ответ несёт только лот. Сбой чтения
     * прошедшую команду не заслоняет.
     */
    private suspend fun settled() {
        quietly { api.auction.mine(heroes.heroId) }?.let { mine(it) }
    }

    /** Продажа своих торгов [lotId] лучшей ставке досрочно: ответ - свой рынок. */
    fun accept(lotId: String) = trade(writing = true) {
        mine(api.auction.accept(heroes.heroId, lotId), "toast.sold_to_bid")
    }

    /** Лот торгов после своей команды: открытый - на своё место в торгах и своих ставках, закрытый - из торгов и ставок прочь. */
    private fun traded(lot: AuctionLot) = market { state ->
        if (lot.onSale) {
            val bids = listOf(lot) + state.myBids.filterNot { it.id == lot.id }
            state.copy(auctions = state.auctions.copy(items = state.auctions.items.map { if (it.id == lot.id) lot else it }), myBids = bids)
        } else {
            state.copy(auctions = state.auctions.without(lot.id), myBids = state.myBids.filterNot { it.id == lot.id })
        }
    }

    /** Ещё неделя своему лоту в его последний день. */
    fun extend(lotId: String) = trade(writing = true) {
        mine(api.auction.extend(heroes.heroId, lotId), "toast.extended")
    }

    /**
     * Подсказка цены - медиана похожих сделок - тому, что герой выставляет или на что ставит: читается тихо, ошибка - просто нет подсказки;
     * одна на вещь за сеанс.
     */
    suspend fun priceHint(itemCode: String, rarity: Rarity?, itemLevel: Int): PriceHint? {
        val key = "$itemCode/${rarity?.name}/$itemLevel"
        hints[key]?.let { return it.value }
        return quietly { api.auction.priceHint(heroes.heroId, itemCode, rarity, itemLevel).also { hints[key] = Hint(it) } }
    }

    /** Подсказка цены товара лота [lot] ([priceHint]): вещь - по базе, редкости и уровню, стопка - по коду. */
    suspend fun priceHint(lot: AuctionLot): PriceHint? = priceHint(lot.itemCode, lot.rarity, lot.itemLevel)

    /** Подсказки цен сеанса: сделки копятся медленно, а лист выставления открывают часто. */
    private val hints = java.util.concurrent.ConcurrentHashMap<String, Hint>()

    private class Hint(val value: PriceHint?)

    fun cancel(lotId: String) = trade(writing = true) {
        mine(api.auction.cancel(heroes.heroId, lotId), "toast.withdrawn")
    }

    /** Выставлено: ответ уже несёт свой рынок и героя - перечитывать нечего; экран - на свои лоты. */
    private fun listed(answer: AuctionMine) {
        mine(answer, "toast.listed")
        market { it.copy(tab = MarketTab.LOTS) }
    }

    /** Свой рынок из ответа команды и тост о её лоте. */
    private fun mine(answer: AuctionMine, toast: String) {
        answer.lot?.let { notices.toast(ui(toast, it.title)) }
        mine(answer)
    }

    /** Свой рынок из ответа: лоты, места, сделки и свои ставки. */
    private fun mine(answer: AuctionMine) = market { it.copy(myLots = answer.lots, slots = answer.slots, history = answer.history, myBids = answer.bids) }

    /** Свой рынок героя [id] из входа в Город: рынок открыт, замка по уровню нет. */
    internal fun land(id: String, answer: AuctionMine) {
        if (!heroes.onScreen(id)) return
        mine(answer)
        market { it.copy(locked = null) }
    }

    private fun trade(writing: Boolean = false, restart: Boolean = false, key: String = Reads.AUCTION, glance: Boolean = false, maxAgeMs: Long = 0, block: suspend () -> Unit) {
        if (writing) {
            commands.task(writing = true, touches = setOf(Reads.AUCTION, Reads.LOTS, Reads.HERO)) { gated { reread(block) } }
        } else {
            commands.read(key, restart, silent = glance, tag = heroes.heroId, maxAgeMs = maxAgeMs) { gated(glance, block) }
        }
    }

    /**
     * Команда [block]; отказ, что значит «показанное устарело» ([STALE]: ставку перебили, своя уже лидирует, лот закрыт), - страница
     * активной вкладки и свои лоты со ставками читаются заново внутри команды (её ключи чтения заняты ею), и лист пересчитывает
     * наименьшую ставку и срок. Отказ печатается всё равно; сбой перечитывания его не заслоняет.
     */
    private suspend fun reread(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: ApiFailure) {
            if (e.code in STALE) {
                fresh()?.let { (page, answer) ->
                    market(page)
                    mine(answer)
                }
            }
            throw e
        }
    }

    /** Страница активной вкладки и свой рынок, прочитанные заново; не прочитались - null. */
    private suspend fun fresh(): Pair<(Market) -> Market, AuctionMine>? = quietly { page(market.tab) to api.auction.mine(heroes.heroId) }

    /** Тихое чтение [read]: сбой - null, отмена идёт дальше. */
    private suspend fun <T> quietly(read: suspend () -> T): T? = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun gated(glance: Boolean = false, block: suspend () -> Unit) {
        check(heroes.heroId.isNotBlank()) { ui("auction.choose_character") }
        try {
            block()
            market { it.copy(locked = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFailure) {
            if (e.code != LEVEL_GATE) throw e
            market { it.copy(locked = locError(e.code, e.message.orEmpty()), showcase = AuctionPage(), auctions = AuctionPage(), myLots = emptyList(), myBids = emptyList()) }
            if (!glance) throw e
        }
    }

    private companion object {
        /** Код сервера «уровень героя слишком мал для аукциона». */
        const val LEVEL_GATE = "AU_002"

        /** Код сервера «у торговца нет такого предложения»: полка сменилась. */
        const val RESTOCKED = "CH_017"

        /** Отказы сервера, после которых показанные лоты устарели: лот закрыт, ставка ниже наименьшей, своя ставка уже лидирует. */
        val STALE = setOf("AU_004", "AU_019", "AU_020")

        /** Режим лотов страницы вкладки: витрина - за цену, «Аукцион» - торги; у прочих вкладок страницы нет. */
        val MarketTab.mode: LotMode?
            get() = when (this) {
                MarketTab.SHOWCASE -> LotMode.FIXED
                MarketTab.AUCTION -> LotMode.BID
                MarketTab.LOTS, MarketTab.HISTORY -> null
            }

        /** Вкладка своего рынка - читает `auction/mine`: свои лоты и ставки, сделки. */
        val MarketTab.ownMarket: Boolean get() = this == MarketTab.LOTS || this == MarketTab.HISTORY
    }
}
