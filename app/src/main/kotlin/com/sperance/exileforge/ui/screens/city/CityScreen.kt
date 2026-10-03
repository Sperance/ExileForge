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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.AuctionScreen
import com.sperance.exileforge.ui.screens.auction.MerchantScreen
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.guild.GuildScreen
import com.sperance.exileforge.ui.screens.quests.QuestsScreen
import com.sperance.exileforge.ui.theme.*

/**
 * The City (3.22.0): where the auction's tab was, a square of buildings — the quest board (3.23.0), the merchant, the auction and the guild.
 * Each card says in a line what waits inside; a tap goes in, and «back», on screen or the system's, comes out to the square.
 * The buildings are the screens they always were, each explaining itself on its first visit.
 */
@Composable fun CityScreen(s: ForgeState, vm: ForgeViewModel) {
    val building = s.building
    if (building == null) {
        CitySquare(s, vm)
        return
    }
    Column(Modifier.fillMaxSize()) {
        BackRow(ui("nav.city")) { vm.building(null) }
        Box(Modifier.weight(1f)) {
            when (building) {
                Building.QUESTS -> QuestsScreen(s, vm)
                Building.MERCHANT -> MerchantScreen(s, vm)
                Building.AUCTION -> AuctionScreen(s, vm)
                Building.GUILD -> GuildScreen(s, vm)
            }
        }
    }
}

/** The square: the three buildings and their news, read when the square opens — the merchant's comes with the hero. */
@Composable private fun CitySquare(s: ForgeState, vm: ForgeViewModel) {
    LaunchedEffect(s.play.heroId, s.account.sessionEpoch) {
        if (s.play.heroId.isNotBlank()) {
            vm.ensureHero()
            vm.loadMyLots(glance = true)
            vm.loadGuild()
            vm.loadQuests()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader(ui("nav.city"), ui("city.subtitle"), ForgeGlyphs.Keep, guide = Guide.CITY)
        BuildingCard(ui("quest.title"), ForgeGlyphs.Scroll, questNews(s), s.lockOf(Building.QUESTS), accent = Vital) { vm.building(Building.QUESTS) }
        BuildingCard(ui("merchant.title"), ForgeGlyphs.Coins, merchantNews(s), s.lockOf(Building.MERCHANT)) { vm.building(Building.MERCHANT) }
        BuildingCard(ui("nav.auction"), ForgeGlyphs.Orb, auctionNews(s), s.lockOf(Building.AUCTION)) { vm.building(Building.AUCTION) }
        BuildingCard(ui("guild.title"), ForgeGlyphs.Banner, guildNews(s), s.lockOf(Building.GUILD), accent = Rune) { vm.building(Building.GUILD) }
        Spacer(Modifier.height(12.dp))
    }
}

/** How many quests wait for their reward, or how many are under way. */
private fun questNews(s: ForgeState): String {
    val board = s.quests.board ?: return ui("city.quests_idle")
    val quests = board.daily + board.weekly + board.contracts + listOfNotNull(board.story)
    val ready = quests.count { it.done && !it.claimed }
    return if (ready > 0) ui("city.quests_ready", ready) else ui("city.quests_active", quests.count { !it.claimed })
}

private fun merchantNews(s: ForgeState): String = s.market.merchant?.takeIf { it.refreshAt > 0 }?.let { ui("merchant.renews", untilText(it.refreshAt)) } ?: ui("city.merchant_idle")

private fun auctionNews(s: ForgeState): String = s.market.locked
    ?: s.market.slots?.let { ui("city.auction_lots", s.ownLots.size, it.limit) }
    ?: ui("city.auction_idle")

private fun guildNews(s: ForgeState): String {
    val mine = s.guild.mine ?: return ui("city.guild_idle")
    mine.guild?.let { return ui("city.guild_in", GuildText.title(it.name, it.tag), it.level) }
    return if (mine.invites.isNotEmpty()) ui("city.guild_invites", mine.invites.size) else ui("city.guild_none")
}

/** One building: its sign, its name and the line of news; the whole card is the door. */

/** The level a building opens at (3.76.0), while the hero is below it. */
private fun ForgeState.lockOf(building: Building): Int? = Feature.ofBuilding(building)?.takeIf { !unlocked(it) }?.level

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
