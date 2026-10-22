package com.sperance.exileforge.ui.screens.auction

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketActions
import com.sperance.exileforge.core.market.MarketChores
import com.sperance.exileforge.core.market.MarketChoresModel
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.market.MarketTab
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.PriceHint
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import kotlinx.coroutines.flow.StateFlow

/**
 * Лавка торговца и рынок игроков: состояние из репозитория, дела рынка - поток ядра ([MarketChoresModel]), действия общие с продажей из
 * сундука.
 */
class MarketViewModel(
    private val actions: MarketActions,
    private val repository: MarketRepository,
    commands: CommandRunner,
    slice: GameSlice,
    dues: MarketChoresModel,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val market: StateFlow<Market> = repository.state
    val activity: StateFlow<Activity> = commands.state

    /** Дела рынка героя к часам сервера: счётчик плитки, точки вкладки и строк. */
    val chores: StateFlow<MarketChores> = dues.chores

    /** Строка «Моих лотов», которую просит показать вход с плитки Города; null - просьбы нет. */
    val focus: StateFlow<String?> = repository.focus

    fun tab(tab: MarketTab) = actions.tab(tab)
    fun filter(filter: AuctionFilter) = actions.filter(filter)
    fun loadShowcase() = actions.loadShowcase()
    fun moreShowcase() = actions.moreShowcase()
    fun moreAuctions() = actions.moreAuctions()
    fun loadMyLots(glance: Boolean = false) = actions.loadMyLots(glance)
    fun loadAuction() = actions.loadAuction()

    /** Экран открылся: свои лоты моложе полуминуты не перечитываются; [glance] - карточка Города. */
    fun openAuction() = actions.loadAuction(fresh = true)
    fun glanceLots() = actions.loadMyLots(glance = true, fresh = true)
    fun buyOffer(offerId: String) = actions.buyOffer(offerId)
    fun tradeTrophy(boss: String) = actions.tradeTrophy(boss)
    fun buy(lotId: String) = actions.buy(lotId)
    fun extend(lotId: String) = actions.extend(lotId)
    fun cancel(lotId: String) = actions.cancel(lotId)
    fun bid(lotId: String, amount: Long) = actions.bid(lotId, amount)
    fun buyout(lotId: String) = actions.buyout(lotId)
    fun accept(lotId: String) = actions.accept(lotId)

    /** Вход с плитки Города: открыть «Мои лоты» на строке [lotId], что ждёт действия; null - дел нет, вкладка как была. */
    fun attend(lotId: String?) = actions.attend(lotId)

    /** Экран показал строку [lotId], о которой просил вход с плитки. */
    fun focused(lotId: String) = repository.focused(lotId)

    /** Медиана похожих сделок для шкалы листа ставки. */
    suspend fun priceHint(lot: AuctionLot): PriceHint? = actions.priceHint(lot)
}
