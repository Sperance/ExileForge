package com.sperance.exileforge.presentation.market

import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.coroutines.CancellationException

/**
 * Аукцион игроков и лавка торговца. Все правила у сервера; клиент называет лот и печатает отказ. Сделка просит
 * перечитать героя - один запрос о том, что сдвинулось, - и списки, которых коснулась. Действия общие для экранов
 * Города и для продажи из сундука героя (3.80.11).
 */
class MarketActions(
    private val repository: MarketRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val events: GameEvents,
    private val content: ContentLoader,
) {
    private val api: GameApi get() = connection.api
    private val market: Market get() = repository.state.value

    private fun market(transform: (Market) -> Market) = repository.update(transform)

    fun tab(tab: Int) = market { it.copy(tab = tab) }
    fun filter(filter: AuctionFilter) = market { it.copy(filter = filter) }
    fun showOwnLots(show: Boolean) = market { it.copy(showOwnLots = show) }

    /** Витрина с первой страницы под фильтром, как он стоит. */
    fun loadShowcase() = trade(restart = true) {
        val showcase = api.auction.search(heroes.heroId, showcaseFilter(), listOf(""))
        market { it.copy(showcase = showcase) }
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

    /** Фильтр, как его читает сервер: свои лоты убраны, если не просили, названия на языке экрана. */
    private fun showcaseFilter(): AuctionFilter = market.filter.copy(excludeSellerId = if (market.showOwnLots) "" else heroes.heroId, lang = uiLanguage.code)

    /** [glance] - площадь Города (3.22.0): без полосы, а герой ниже уровня аукциона узнаёт это с карточки, не отказом. */
    fun loadMyLots(glance: Boolean = false) = trade(key = Reads.LOTS, glance = glance) {
        val id = heroes.heroId
        val lots = api.auction.myLots(id)
        val slots = api.auction.slots(id)
        val history = if (glance) market.history else api.auction.history(id)
        market { it.copy(myLots = lots, slots = slots, history = history) }
    }

    /** Полка торговца: приходит со снимком героя, а при входе в лавку читается заново. */
    fun loadMerchant() = trade(key = Reads.MERCHANT) {
        content.ensure()
        val stock = api.merchant.stock(heroes.heroId)
        market { it.copy(merchant = stock) }
    }

    /** Два списка аукциона; торговец - своё здание с 3.22.0 и читает полку сам. */
    fun loadAuction() {
        loadShowcase()
        loadMyLots()
    }

    fun buyOffer(offerId: String) = trade(writing = true) {
        val id = heroes.heroId
        val purchase = api.merchant.buy(id, offerId)
        market { it.copy(merchant = it.merchant?.let { stock -> stock.copy(offers = stock.offers.filter { offer -> offer.id != offerId }) }) }
        bought(id, purchase.money)
    }

    fun buyOrb(code: String) = trade(writing = true) {
        val id = heroes.heroId
        val purchase = api.merchant.buyOrb(id, code)
        market {
            it.copy(
                merchant = it.merchant?.let { stock ->
                    stock.copy(orbs = stock.orbs.map { orb -> if (orb.code == code) orb.copy(price = purchase.next, bought = orb.bought + 1, left = orb.left?.let { n -> (n - 1).coerceAtLeast(0) }) else orb })
                },
            )
        }
        bought(id, purchase.money)
    }

    fun buy(lotId: String) = trade(writing = true) {
        val id = heroes.heroId
        val lot = api.auction.buy(id, lotId)
        notices.toast(ui("toast.bought", lot.title))
        // Купленный лот уходит с витрины; страницы, добавленные под ним «Показать ещё», остаются.
        market { it.copy(showcase = it.showcase.without(lotId)) }
        refreshHero(id)
    }

    /** Выставляет копию за цену в сферах - [priceOrb] код предмета-валюты. */
    fun sellEquipment(itemId: String, priceOrb: String, price: Long) = trade(writing = true) {
        val id = heroes.heroId
        notices.toast(ui("toast.listed", api.auction.sellEquipment(id, itemId, priceOrb, price).title))
        listed(id)
    }

    /** Выставляет стопку из сумки по коду. */
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long) = trade(writing = true) {
        val id = heroes.heroId
        notices.toast(ui("toast.listed", api.auction.sellItem(id, code, amount, priceOrb, price).title))
        listed(id)
    }

    /** Ещё неделя своему лоту в его последний день (3.79.0). */
    fun extend(lotId: String) = trade(writing = true) {
        val id = heroes.heroId
        notices.toast(ui("toast.extended", api.auction.extend(id, lotId).title))
        afterTrade {
            val lots = api.auction.myLots(id)
            market { it.copy(myLots = lots) }
        }
    }

    /** Подсказка цены тому, что герой выставляет (3.79.0): читается тихо, ошибка - просто нет подсказки. */
    suspend fun priceHint(itemCode: String, rarity: Rarity?, itemLevel: Int): PriceHint? = try {
        api.auction.priceHint(heroes.heroId, itemCode, rarity, itemLevel)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    fun cancel(lotId: String) = trade(writing = true) {
        val id = heroes.heroId
        notices.toast(ui("toast.withdrawn", api.auction.cancel(id, lotId).title))
        afterTrade {
            refreshHero(id)
            val lots = api.auction.myLots(id)
            val slots = api.auction.slots(id)
            market { it.copy(myLots = lots, slots = slots) }
        }
    }

    private suspend fun listed(heroId: String) = afterTrade {
        refreshHero(heroId)
        val lots = api.auction.myLots(heroId)
        val slots = api.auction.slots(heroId)
        market { it.copy(myLots = lots, slots = slots, tab = 1) }
    }

    /** Чтения после сделки, которую сервер уже совершил: их ошибка - не ошибка сделки. */
    private suspend fun afterTrade(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            commands.refuse(phrase("auction.done_refresh"))
        }
    }

    private fun bought(heroId: String, money: Long) {
        heroes.money(money)
        refreshHero(heroId)
    }

    private fun refreshHero(heroId: String) {
        if (heroes.onScreen(heroId)) events.heroChanged()
    }

    private fun trade(writing: Boolean = false, restart: Boolean = false, key: String = Reads.AUCTION, glance: Boolean = false, block: suspend () -> Unit) {
        if (writing) {
            commands.task(writing = true, touches = setOf(Reads.AUCTION, Reads.LOTS, Reads.HERO, Reads.MERCHANT)) { gated(block) }
        } else {
            commands.read(key, restart, silent = glance) { gated(block, glance) }
        }
    }

    private suspend fun gated(block: suspend () -> Unit, glance: Boolean = false) {
        check(heroes.heroId.isNotBlank()) { ui("auction.choose_character") }
        try {
            block()
            market { it.copy(locked = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFailure) {
            if (e.code != LEVEL_GATE) throw e
            market { it.copy(locked = locError(e.code, e.message.orEmpty()), showcase = AuctionPage(), myLots = emptyList()) }
            if (!glance) throw e
        }
    }

    private companion object {
        /** Код сервера «уровень героя слишком мал для аукциона». */
        const val LEVEL_GATE = "AU_002"
    }
}
