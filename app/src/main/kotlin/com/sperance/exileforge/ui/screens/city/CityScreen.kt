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
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.TAB_CITY
import com.sperance.exileforge.presentation.state.level
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.AuctionScreen
import com.sperance.exileforge.ui.screens.auction.MerchantScreen
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.guild.GuildScreen
import com.sperance.exileforge.ui.screens.hall.HallScreen
import com.sperance.exileforge.ui.screens.hero.ChronicleScreen
import com.sperance.exileforge.ui.screens.hero.chronicleDone
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.screens.quests.QuestsScreen
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The City (3.22.0): where the auction's tab was, a square of buildings — the quest board (3.23.0), the merchant, the auction, the guild,
 * «История» (3.90.2) and «Доска славы» (4.2.0).
 * Each card says in a line what waits inside; a tap goes in, and «back», on screen or the system's, comes out to the square.
 * The buildings are the screens they always were, each explaining itself on its first visit.
 */
@Composable fun CityScreen(building: Building?) {
    val shell: ShellViewModel = koinViewModel()
    if (building == null) {
        CitySquare()
        return
    }
    Column(Modifier.fillMaxSize()) {
        BackRow(ui("nav.city")) { shell.building(null) }
        Box(Modifier.weight(1f)) {
            when (building) {
                Building.QUESTS -> QuestsScreen()
                Building.MERCHANT -> MerchantScreen()
                Building.AUCTION -> AuctionScreen()
                Building.GUILD -> GuildScreen()
                Building.HISTORY -> HistoryScreen()
                Building.CHRONICLE -> CityChronicle()
                Building.HALL -> HallScreen()
            }
        }
    }
}

/** The square: the three buildings and their news, read when the square opens — the merchant's comes with the hero. */
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
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        if (game.heroId.isNotBlank()) {
            heroModel.ensure()
            // Срок свежести (3.94.1): площадь, открытая снова за полминуты, сервер не спрашивает
            market.glanceLots()
            guild.open()
            quests.open()
        }
    }
    val scroll = rememberScrollState()
    // Вкладка нажата снова на площади (3.95.0): наверх; из здания навигатор и так вернул на площадь
    OnReselect(TAB_CITY) { scroll.animateScrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(ui("nav.city"), ui("city.subtitle"), ForgeGlyphs.Keep)
        HubGrid(cityTiles(game, board, trade, guilds) { shell.building(it) })
        Spacer(Modifier.height(12.dp))
    }
}

/** Летопись (4.0.0): сколько деяний свершено и носимый титул. */
private fun chronicleNews(game: GameUi): String {
    val done = game.chronicleDone()?.let { (done, all) -> ui("chronicle.done", done, all) } ?: return ui("common.loading")
    val title = game.hero?.info?.title?.takeIf { it.isNotBlank() } ?: return done
    return "$done · ${titleName(title)}"
}

/** Летопись в здании Города (4.0.0): её экран читает героя, как прежде на странице «Развития». */
@Composable private fun CityChronicle() {
    val game by koinViewModel<ShellViewModel>().game.collectAsStateWithLifecycle()
    val heroModel: HeroViewModel = koinViewModel()
    LaunchedEffect(game.heroId, game.sessionEpoch) { heroModel.ensure() }
    ChronicleScreen(game, heroModel)
}

/**
 * Здания площади плитками хаба (4.4.x, как «Развитие»): у каждого строка новостей и замок уровня; счётчик - что ждёт действия:
 * награды заданий, приглашения в гильдию, свои лоты аукциона, что пора продлить.
 */
private fun cityTiles(game: GameUi, board: Quests, trade: Market, guilds: Guilds, open: (Building) -> Unit): List<HubTile> {
    fun tile(building: Building, title: String, icon: ImageVector, accent: Color, news: String, badge: Int = 0) = HubTile(title, icon, accent, note = null, news = news, badge = badge, lockedUntil = game.lockOf(building)) { open(building) }
    return listOf(
        tile(Building.QUESTS, ui("quest.title"), ForgeGlyphs.Scroll, Vital, questNews(board), questsReady(board)),
        tile(Building.MERCHANT, ui("merchant.title"), ForgeGlyphs.Coins, Gold, merchantNews(game.hero?.merchant)),
        tile(Building.AUCTION, ui("nav.auction"), ForgeGlyphs.Orb, Gold, auctionNews(trade), lotsToExtend(game, trade)),
        tile(Building.GUILD, ui("guild.title"), ForgeGlyphs.Banner, Rune, guildNews(guilds), guilds.mine?.takeIf { it.guild == null }?.invites?.size ?: 0),
        tile(Building.HISTORY, ui("history.title"), ForgeGlyphs.Tome, Parchment, ui("city.history_idle")),
        tile(Building.CHRONICLE, ui("chronicle.title"), ForgeGlyphs.Scroll, GoldBright, chronicleNews(game)),
        tile(Building.HALL, ui("hall.title"), ForgeGlyphs.Gem, rarityColor(Rarity.MYTHICAL.name), ui("city.hall_idle")),
    )
}

/** Задания доски, что ждут награды. */
private fun questsReady(quests: Quests): Int = quests.board?.let { board -> (board.daily + board.weekly + board.contracts).count { it.done && !it.claimed } } ?: 0

/** Свои лоты, срок которых подходит к концу и которые уже можно продлить (окно продления правил). */
private fun lotsToExtend(game: GameUi, market: Market): Int = game.index?.rules?.auction?.let { rules -> market.myLots.count { it.extendable(rules.extendWindowMillis) } } ?: 0

/** How many quests wait for their reward, or how many are under way; сюжет (4.2.0) - в «Походе», не здесь. */
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

/** The level a building opens at (3.76.0), while the hero is below it. */
private fun GameUi.lockOf(building: Building): Int? = Feature.ofBuilding(building)?.takeIf { !unlocked(it) }?.level(index?.rules)
