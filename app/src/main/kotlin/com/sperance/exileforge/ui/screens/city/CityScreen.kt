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
import com.sperance.exileforge.core.app.reachable
import com.sperance.exileforge.core.app.unlocked
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.timeLeft
import com.sperance.exileforge.core.guild.Guilds
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.Market
import com.sperance.exileforge.core.market.MarketChores
import com.sperance.exileforge.core.market.MarketDue
import com.sperance.exileforge.core.model.trade.MerchantStock
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.rules.content.ContestBoard
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.AuctionScreen
import com.sperance.exileforge.ui.screens.auction.MarketViewModel
import com.sperance.exileforge.ui.screens.auction.MerchantScreen
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.contest.ContestScreen
import com.sperance.exileforge.ui.screens.contest.ContestViewModel
import com.sperance.exileforge.ui.screens.guild.GuildScreen
import com.sperance.exileforge.ui.screens.guild.GuildViewModel
import com.sperance.exileforge.ui.screens.hall.HallScreen
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.quests.QuestViewModel
import com.sperance.exileforge.ui.screens.quests.QuestsScreen
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The City: a square of buildings — the quest board, the merchant, the auction, the guild,
 * «История» (с Летописью внутри), «Доска славы» и «Состязания».
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
                Building.CONTESTS -> ContestScreen()
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
        Building.CONTESTS -> "contest.title"
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
    val contests = koinViewModel<ContestViewModel>().model
    val contest by contests.contests.collectAsStateWithLifecycle()
    val board by quests.quests.collectAsStateWithLifecycle()
    val trade by market.market.collectAsStateWithLifecycle()
    val guilds by guild.guilds.collectAsStateWithLifecycle()
    val auction = game.reachable(Feature.AUCTION)
    LaunchedEffect(game.heroId, game.sessionEpoch) {
        if (game.heroId.isNotBlank()) {
            heroModel.ensure()
            guild.open()
            quests.open()
            contests.load()
        }
    }
    LaunchedEffect(game.heroId, game.sessionEpoch, auction) { if (game.heroId.isNotBlank() && auction) market.glanceLots() }
    val chores by market.chores.collectAsStateWithLifecycle()
    val now = rememberServerNow(CONTEST_TICK_MS)
    val scroll = rememberScrollState()
    OnReselect(TAB_CITY) { scroll.animateScrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(12.dp))
        HubGrid(
            cityTiles(game, board, trade, chores, guilds) { building ->
                if (building == Building.AUCTION) market.attend(chores.first)
                shell.building(building)
            } + contestTile(contest.board, now) { shell.building(Building.CONTESTS) },
        )
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Здания площади плитками хаба (как «Развитие»): у каждого строка новостей и замок уровня; счётчик - что ждёт действия:
 * награды заданий, приглашения в гильдию, дела рынка ([chores]: лоты к продлению, перебитые ставки, торги, что можно продать) - у рынка
 * ещё и строкой на каждое дело.
 */
private fun cityTiles(game: GameUi, board: Quests, trade: Market, chores: MarketChores, guilds: Guilds, open: (Building) -> Unit): List<HubTile> {
    fun tile(building: Building, icon: ImageVector, accent: Color, news: String, badge: Int = 0, lines: List<String> = emptyList()) =
        HubTile(ui(building.titleKey), icon, accent, note = null, news = news, badge = badge, lockedUntil = game.lockOf(building), chores = lines) { open(building) }
    return listOf(
        tile(Building.QUESTS, ForgeGlyphs.Scroll, Vital, questNews(board), questsReady(board)),
        tile(Building.MERCHANT, ForgeGlyphs.Coins, Gold, merchantNews(game.hero?.merchant)),
        tile(Building.AUCTION, ForgeGlyphs.Orb, Gold, auctionNews(trade), chores.total, choreLines(chores)),
        tile(Building.GUILD, ForgeGlyphs.Banner, Rune, guildNews(guilds), guilds.mine?.takeIf { it.guild == null }?.invites?.size ?: 0),
        tile(Building.HISTORY, ForgeGlyphs.Tome, Parchment, ui("city.history_idle")),
        tile(Building.HALL, ForgeGlyphs.Gem, rarityColor(Rarity.MYTHICAL.name), ui("city.hall_idle")),
    )
}

/** Такт сроков на плитке состязаний: подписи - до минут. */
private const val CONTEST_TICK_MS = 30_000L

/**
 * Плитка «Состязания» (последней - во всю ширину): строка на каждое состязание - «День · Находки · 23-е · 5 ч 13 мин»; здание ещё не
 * прочитано - строка ожидания.
 */
private fun contestTile(board: ContestBoard?, now: Long, open: () -> Unit): HubTile {
    val lines = board?.stands.orEmpty().map { stand ->
        val place = if (stand.place > 0) ui("contest.place_short", stand.place) else ui("contest.unranked_short")
        ui("contest.tile_line", ui("contest.kind_short.${stand.kind.name}"), ui("contest.metric_short.${stand.metric.name}"), place, timeLeft(stand.endsAt - now))
    }
    return HubTile(ui("contest.title"), ForgeGlyphs.Trophy, Medals.last(), note = null, news = lines.firstOrNull() ?: ui("contest.tile_idle"), chores = lines.drop(1)) { open() }
}

/** Задания доски, что ждут награды. */
private fun questsReady(quests: Quests): Int = quests.board?.let { board -> (board.daily + board.weekly + board.contracts).count { it.done && !it.claimed } } ?: 0

/** Строки дел рынка на плитке: «Можно продлить: 1», «Ставка перебита: 2», «Можно продать: 1» - только непустые. */
private fun choreLines(chores: MarketChores): List<String> = MarketDue.entries.mapNotNull { due -> chores.count(due).takeIf { it > 0 }?.let { ui("auction.due.${due.name}", it) } }

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
