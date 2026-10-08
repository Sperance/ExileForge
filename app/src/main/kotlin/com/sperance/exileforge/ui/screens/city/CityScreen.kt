package com.sperance.exileforge.ui.screens.city

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.AuctionScreen
import com.sperance.exileforge.ui.screens.auction.MerchantScreen
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.guild.GuildScreen
import com.sperance.exileforge.ui.screens.hero.ChronicleScreen
import com.sperance.exileforge.ui.screens.hero.chronicleDone
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.screens.quests.QuestsScreen
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The City (3.22.0): where the auction's tab was, a square of buildings — the quest board (3.23.0), the merchant, the auction, the guild
 * and «История» (3.90.2).
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
        BuildingCard(ui("quest.title"), ForgeGlyphs.Scroll, questNews(board), game.lockOf(Building.QUESTS), accent = Vital) { shell.building(Building.QUESTS) }
        BuildingCard(ui("merchant.title"), ForgeGlyphs.Coins, merchantNews(game.hero?.merchant), game.lockOf(Building.MERCHANT)) { shell.building(Building.MERCHANT) }
        BuildingCard(ui("nav.auction"), ForgeGlyphs.Orb, auctionNews(trade), game.lockOf(Building.AUCTION)) { shell.building(Building.AUCTION) }
        BuildingCard(ui("guild.title"), ForgeGlyphs.Banner, guildNews(guilds), game.lockOf(Building.GUILD), accent = Rune) { shell.building(Building.GUILD) }
        BuildingCard(ui("history.title"), ForgeGlyphs.Tome, ui("city.history_idle"), game.lockOf(Building.HISTORY), accent = Parchment) { shell.building(Building.HISTORY) }
        BuildingCard(ui("chronicle.title"), ForgeGlyphs.Scroll, chronicleNews(game), game.lockOf(Building.CHRONICLE), accent = GoldBright) { shell.building(Building.CHRONICLE) }
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

/** How many quests wait for their reward, or how many are under way. */
private fun questNews(quests: Quests): String {
    val board = quests.board ?: return ui("city.quests_idle")
    val quests = board.daily + board.weekly + board.contracts + listOfNotNull(board.story)
    val ready = quests.count { it.done && !it.claimed }
    return if (ready > 0) ui("city.quests_ready", ready) else ui("city.quests_active", quests.count { !it.claimed })
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

/** One building: its sign, its name and the line of news; the whole card is the door. */

/** The level a building opens at (3.76.0), while the hero is below it. */
private fun GameUi.lockOf(building: Building): Int? = Feature.ofBuilding(building)?.takeIf { !unlocked(it) }?.level(index?.rules)

@Composable private fun BuildingCard(title: String, icon: ImageVector, news: String, lockedUntil: Int?, accent: Color = Gold, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().alpha(if (lockedUntil != null) LOCKED_ALPHA else 1f).clip(shape).background(Brush.horizontalGradient(listOf(accent.copy(alpha = .14f), Panel)), shape)
            .border(1.dp, Bronze, shape).clickable(onClick = onOpen).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(52.dp).border(1.dp, accent.copy(alpha = .5f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(30.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium)
            Text(lockedUntil?.let { ui("unlock.from", it) } ?: news, color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(if (lockedUntil != null) Icons.Outlined.Lock else Icons.Outlined.ChevronRight, null, tint = Muted)
    }
}

/** How dim a place the hero's level has not opened is drawn (3.76.0). */
internal const val LOCKED_ALPHA = .45f
