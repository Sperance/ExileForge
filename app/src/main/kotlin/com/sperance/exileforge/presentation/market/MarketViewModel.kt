package com.sperance.exileforge.presentation.market

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.StateFlow

/** Лавка торговца и аукцион (3.80.11): рынок из репозитория, действия общие с продажей из сундука. */
class MarketViewModel(
    private val actions: MarketActions,
    repository: MarketRepository,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val market: StateFlow<Market> = repository.state
    val activity: StateFlow<Activity> = commands.state

    fun tab(tab: Int) = actions.tab(tab)
    fun filter(filter: AuctionFilter) = actions.filter(filter)
    fun showOwnLots(show: Boolean) = actions.showOwnLots(show)
    fun loadShowcase() = actions.loadShowcase()
    fun moreShowcase() = actions.moreShowcase()
    fun loadMyLots(glance: Boolean = false) = actions.loadMyLots(glance)
    fun loadAuction() = actions.loadAuction()

    /** Экран открылся (3.94.1): свои лоты моложе полуминуты не перечитываются; [glance] - карточка Города. */
    fun openAuction() = actions.loadAuction(fresh = true)
    fun glanceLots() = actions.loadMyLots(glance = true, fresh = true)
    fun buyOffer(offerId: String) = actions.buyOffer(offerId)
    fun buyOrb(code: String) = actions.buyOrb(code)
    fun tradeTrophy(boss: String) = actions.tradeTrophy(boss)
    fun buy(lotId: String) = actions.buy(lotId)
    fun extend(lotId: String) = actions.extend(lotId)
    fun cancel(lotId: String) = actions.cancel(lotId)
}
