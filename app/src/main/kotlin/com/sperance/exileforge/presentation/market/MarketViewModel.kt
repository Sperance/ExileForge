package com.sperance.exileforge.presentation.market

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import kotlinx.coroutines.flow.StateFlow

/** Лавка торговца и аукцион (3.80.11): рынок из репозитория, действия общие с продажей из сундука. */
class MarketViewModel(
    private val actions: MarketActions,
    repository: MarketRepository,
    commands: CommandRunner,
) : ViewModel() {
    val market: StateFlow<Market> = repository.state
    val activity: StateFlow<Activity> = commands.state

    fun tab(tab: Int) = actions.tab(tab)
    fun filter(filter: AuctionFilter) = actions.filter(filter)
    fun showOwnLots(show: Boolean) = actions.showOwnLots(show)
    fun loadShowcase() = actions.loadShowcase()
    fun moreShowcase() = actions.moreShowcase()
    fun loadMyLots(glance: Boolean = false) = actions.loadMyLots(glance)
    fun loadMerchant() = actions.loadMerchant()
    fun loadAuction() = actions.loadAuction()
    fun buyOffer(offerId: String) = actions.buyOffer(offerId)
    fun buyOrb(code: String) = actions.buyOrb(code)
    fun buy(lotId: String) = actions.buy(lotId)
    fun extend(lotId: String) = actions.extend(lotId)
    fun cancel(lotId: String) = actions.cancel(lotId)
}
