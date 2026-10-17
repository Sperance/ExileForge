package com.sperance.exileforge.ui.screens.city

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.Building
import com.sperance.exileforge.core.app.Feature
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_CITY
import com.sperance.exileforge.core.app.level
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.guild.Guilds
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.model.trade.MerchantStock
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.guild.GuildViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.market.MarketViewModel
import com.sperance.exileforge.presentation.quests.QuestViewModel
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.AuctionScreen
import com.sperance.exileforge.ui.screens.auction.MerchantScreen
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.guild.GuildScreen
import com.sperance.exileforge.ui.screens.hall.HallScreen
import com.sperance.exileforge.ui.screens.quests.QuestsScreen
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The City: a square of buildings — the quest board, the merchant, the auction, the guild,
 * «История» (с Летописью внутри) and «Доска славы».
 * Each card says in a line what waits inside; a tap goes in, and «back», on screen or the system's, comes out to the square.
 * Each building explains itself on its first visit.
 */
@Composable fun CityScreen(building: Building?) {
    val shell: ShellViewModel = koinViewModel()
    if (building == null) {
        CitySquare()
        return
    }
    BackTrailHost(ui("nav.city")) {
        TrailLevel(ui(building.titleKey), onBack = { shell.building(null) }) {
            when (building) {
                Building.QUESTS -> QuestsScreen()
                Building.MERCHANT -> MerchantScreen()
                Building.AUCTION -> AuctionScreen()
                Building.GUILD -> GuildScreen()
                Building.HISTORY -> HistoryScreen()
                Building.HALL -> HallScreen()
            }
        }
    }
}

/** Имя здания в словаре: на плитке площади и в цепочке «назад». */
private val Building.titleKey: String
    get() = when (this) {
        Building.QUESTS -> "quest.title"
        Building.MERCHANT -> "merchant.title"
        Building.AUCTION -> "nav.auction"
        Building.GUILD -> "guild.title"
        Building.HISTORY -> "history.title"
        Building.HALL -> "hall.title"
    }

/**
 * The square: the three buildings and their news, read when the square opens — the merchant's comes with the hero.
 *
 * Площадь, открытая снова за полминуты, сервер не спрашивает; вкладка нажата снова - наверх.
 */
@Composable private fun CitySquare() {
    val shell: ShellViewModel = koinViewModel()
    val game by shell.game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    val quests = koinViewModel<QuestViewModel>()
    val market = koinViewModel<MarketViewModel>()
    val guild = koinViewModel<GuildViewModel>()
    val board by quests.quests.collectAsStateWithLifecycle()
    val trade by market.market.collectAsStateWithLifecycle()
    val guilds by guild.guilds.collectAsStateWithLifecycle()
    val auction = game.unlocked(Feature.AUCTION)
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        if (game.heroId.isNotBlank()) {
            heroModel.ensure()
            guild.open()
            quests.open()
        }
    }
    LaunchedEffect(game.heroId, game.sessionEpoch, auction) { if (game.heroId.isNotBlank() && auction) market.glanceLots() }
    val scroll = rememberScrollState()
    OnReselect(TAB_CITY) { scroll.animateScrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(ui("nav.city"), ui("city.subtitle"), ForgeGlyphs.Keep)
        HubGrid(cityTiles(game, board, trade, guilds) { shell.building(it) })
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Здания площади плитками хаба (как «Развитие»): у каждого строка новостей и замок уровня; счётчик - что ждёт действия:
 * награды заданий, приглашения в гильдию, свои лоты аукциона, что пора продлить.
 */
private fun cityTiles(game: GameUi, board: Quests, trade: Market, guilds: Guilds, open: (Building) -> Unit): List<HubTile> {
    fun tile(building: Building, icon: ImageVector, accent: Color, news: String, badge: Int = 0) = HubTile(ui(building.titleKey), icon, accent, note = null, news = news, badge = badge, lockedUntil = game.lockOf(building)) { open(building) }
    return listOf(
        tile(Building.QUESTS, ForgeGlyphs.Scroll, Vital, questNews(board), questsReady(board)),
        tile(Building.MERCHANT, ForgeGlyphs.Coins, Gold, merchantNews(game.hero?.merchant)),
        tile(Building.AUCTION, ForgeGlyphs.Orb, Gold, auctionNews(trade), lotsToExtend(game, trade)),
        tile(Building.GUILD, ForgeGlyphs.Banner, Rune, guildNews(guilds), guilds.mine?.takeIf { it.guild == null }?.invites?.size ?: 0),
        tile(Building.HISTORY, ForgeGlyphs.Tome, Parchment, ui("city.history_idle")),
        tile(Building.HALL, ForgeGlyphs.Gem, rarityColor(Rarity.MYTHICAL.name), ui("city.hall_idle")),
    )
}

/** Задания доски, что ждут награды. */
private fun questsReady(quests: Quests): Int = quests.board?.let { board -> (board.daily + board.weekly + board.contracts).count { it.done && !it.claimed } } ?: 0

/** Свои лоты, срок которых подходит к концу и которые уже можно продлить (окно продления правил). */
private fun lotsToExtend(game: GameUi, market: Market): Int = game.index?.rules?.auction?.let { rules -> market.myLots.count { it.extendable(rules) } } ?: 0

/** How many quests wait for their reward, or how many are under way; сюжет - в «Походе», не здесь. */
private fun questNews(quests: Quests): String {
    val board = quests.board ?: return ui("city.quests_idle")
    val ready = questsReady(quests)
    return if (ready > 0) ui("city.quests_ready", ready) else ui("city.quests_active", (board.daily + board.weekly + board.contracts).count { !it.claimed })
}

private fun merchantNews(merchant: MerchantStock?): String = merchant?.takeIf { it.refreshAt > 0 }?.let { ui("merchant.renews", untilText(it.refreshAt)) } ?: ui("city.merchant_idle")

private fun auctionNews(market: Market): String = market.locked
    ?: market.slots?.let { ui("city.auction_lots", market.myLots.count { lot -> lot.onSale }, it.limit) }
    ?: ui("city.auction_idle")

private fun guildNews(guilds: Guilds): String {
    val mine = guilds.mine ?: return ui("city.guild_idle")
    mine.guild?.let { return ui("city.guild_in", GuildText.title(it.name, it.tag), it.level) }
    return if (mine.invites.isNotEmpty()) ui("city.guild_invites", mine.invites.size) else ui("city.guild_none")
}

/** The level a building opens at, while the hero is below it. */
private fun GameUi.lockOf(building: Building): Int? = Feature.ofBuilding(building)?.takeIf { !unlocked(it) }?.level(index?.rules)
