package com.sperance.exileforge.presentation.market

import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.locError
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionMine
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.freshness
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.coroutines.CancellationException

/**
 * Аукцион игроков и лавка торговца. Все правила у сервера; клиент называет лот и печатает отказ. С 3.94.1 сделка - один
 * запрос: ответ несёт снимок героя, а команды над своими лотами - ещё и свой аукцион. Полка торговца едет в снимке героя.
 * Действия общие для экранов Города и для продажи из сундука героя (3.80.11).
 */
class MarketActions(
    private val repository: MarketRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
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
    fun loadMyLots(glance: Boolean = false, fresh: Boolean = false) = trade(key = Reads.LOTS, glance = glance, maxAgeMs = freshness(fresh)) {
        val mine = api.auction.mine(heroes.heroId)
        market { it.copy(myLots = mine.lots, slots = mine.slots, history = mine.history) }
    }

    /** Два списка аукциона; торговец - своё здание с 3.22.0, его полка едет в снимке героя (3.94.1). */
    fun loadAuction(fresh: Boolean = false) {
        loadShowcase()
        loadMyLots(fresh = fresh)
    }

    fun buyOffer(offerId: String) = trade(writing = true) {
        api.merchant.buy(heroes.heroId, offerId)
    }

    fun buyOrb(code: String) = trade(writing = true) {
        api.merchant.buyOrb(heroes.heroId, code)
    }

    fun buy(lotId: String) = trade(writing = true) {
        val lot = api.auction.buy(heroes.heroId, lotId)
        notices.toast(ui("toast.bought", lot.title))
        // Купленный лот уходит с витрины; страницы, добавленные под ним «Показать ещё», остаются.
        market { it.copy(showcase = it.showcase.without(lotId)) }
    }

    /** Выставляет копию за цену в сферах - [priceOrb] код предмета-валюты. */
    fun sellEquipment(itemId: String, priceOrb: String, price: Long) = trade(writing = true) {
        listed(api.auction.sellEquipment(heroes.heroId, itemId, priceOrb, price))
    }

    /** Выставляет стопку из сумки по коду. */
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long) = trade(writing = true) {
        listed(api.auction.sellItem(heroes.heroId, code, amount, priceOrb, price))
    }

    /** Выставляет питомца (3.91.0). */
    fun sellPet(petId: String, priceOrb: String, price: Long) = trade(writing = true) {
        listed(api.auction.sellPet(heroes.heroId, petId, priceOrb, price))
    }

    /** Ещё неделя своему лоту в его последний день (3.79.0). */
    fun extend(lotId: String) = trade(writing = true) {
        mine(api.auction.extend(heroes.heroId, lotId), "toast.extended")
    }

    /** Подсказка цены тому, что герой выставляет (3.79.0): читается тихо, ошибка - просто нет подсказки; одна на вещь за сеанс (3.94.1). */
    suspend fun priceHint(itemCode: String, rarity: Rarity?, itemLevel: Int): PriceHint? {
        val key = "$itemCode/${rarity?.name}/$itemLevel"
        hints[key]?.let { return it.value }
        return try {
            api.auction.priceHint(heroes.heroId, itemCode, rarity, itemLevel).also { hints[key] = Hint(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** Подсказки цен сеанса: сделки копятся медленно, а лист выставления открывают часто. */
    private val hints = java.util.concurrent.ConcurrentHashMap<String, Hint>()

    private class Hint(val value: PriceHint?)

    fun cancel(lotId: String) = trade(writing = true) {
        mine(api.auction.cancel(heroes.heroId, lotId), "toast.withdrawn")
    }

    /** Выставлено (3.94.1): ответ уже несёт свой аукцион и героя - перечитывать нечего; экран - на свои лоты. */
    private fun listed(answer: AuctionMine) {
        mine(answer, "toast.listed")
        market { it.copy(tab = 1) }
    }

    /** Свой аукцион из ответа команды и тост о её лоте. */
    private fun mine(answer: AuctionMine, toast: String) {
        answer.lot?.let { notices.toast(ui(toast, it.title)) }
        market { it.copy(myLots = answer.lots, slots = answer.slots, history = answer.history) }
    }

    private fun trade(writing: Boolean = false, restart: Boolean = false, key: String = Reads.AUCTION, glance: Boolean = false, maxAgeMs: Long = 0, block: suspend () -> Unit) {
        if (writing) {
            commands.task(writing = true, touches = setOf(Reads.AUCTION, Reads.LOTS, Reads.HERO)) { gated(block) }
        } else {
            commands.read(key, restart, silent = glance, tag = heroes.heroId, maxAgeMs = maxAgeMs) { gated(block, glance) }
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
