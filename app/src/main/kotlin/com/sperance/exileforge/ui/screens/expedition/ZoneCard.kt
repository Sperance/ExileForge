package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.MapLaunch
import com.sperance.exileforge.core.campaign.MapLineKind
import com.sperance.exileforge.core.campaign.MapStats
import com.sperance.exileforge.core.campaign.TokenState
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.campaign.WorldToken
import com.sperance.exileforge.core.campaign.run.AutoPlan
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.mapDescription
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.presentation.expedition.ExpeditionViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.Monster
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.ItemIcon
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*

/** One stash map of a zone, with the view its square and its lines are drawn from. */
internal data class StashMap(val item: ItemInstance, val view: ItemView)

/** A line of the picked map: its sentence, what it is, and what it pays if it is a harm. */
internal data class MapLine(val text: String, val kind: MapLineKind, val risk: Double)

/** The stash's loose maps, zone by zone: the world map marks each token with how many wait for it. */

/**
 * How many maps of each zone the stash holds. Read off the items' zone alone (3.76.0): building a full view of every
 * thing in the stash on each opening of the tab froze the tap.
 */
fun stashCounts(game: GameUi): Map<String, Int> = game.hero?.stash.orEmpty().filter { it.mapZone.isNotEmpty() }.groupingBy { it.mapZone.value }.eachCount()

internal fun stashMaps(game: GameUi): List<StashMap> = game.hero?.stash.orEmpty()
    .filter { it.mapZone.isNotEmpty() }.mapNotNull { item -> game.view(item)?.takeIf { it.slot == Slot.MAP }?.let { StashMap(item, it) } }

/**
 * A zone's card on the world map (2.76.0, in place of the launch window): it rises over the map's
 * foot when a token is tapped. It names the zone, its level, biome and region and whether it is
 * passed; for a zone the hero may enter, its guardian — waiting, or slain until its time — the atlas
 * points the zone has given, the stash's maps of it with what the picked one pays, line by line, and
 * «Войти в портал». A «???» zone says only whose guardian opens it. The rules and the content are the
 * index's; the windows of the zone — its guardian's return — are the hero's own campaign.
 */
@Composable fun ZoneCard(game: GameUi, vm: ExpeditionViewModel, world: WorldMap, launch: MapLaunch, modifier: Modifier = Modifier) {
    val token = world.token(launch.mapCode) ?: return
    val index = game.index ?: return
    val zone = token.zone
    FirstVisit(Guide.MAP_LAUNCH)
    // A locked map is kept on purpose: spending it on a run is asked about first, for either launch.
    var askLocked by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launchGuarded: (() -> Unit) -> Unit = { go -> if (stashMaps(game).any { it.item.id == launch.picked && it.item.locked }) askLocked = go else go() }
    askLocked?.let { go ->
        ConfirmSheet(
            title = ui("expedition.launch_locked_q"),
            confirm = ui("expedition.launch_locked_go"),
            danger = true,
            onDismiss = { askLocked = null },
        ) { go() }
    }
    Surface(
        modifier.fillMaxWidth(),
        color = Panel,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = BorderStroke(1.dp, PanelRaised),
        shadowElevation = 12.dp,
    ) {
        Column(
            Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(28.dp)) {
                Box(Modifier.align(Alignment.Center).size(38.dp, 4.dp).background(Color(0xFF3A414B), RoundedCornerShape(2.dp)))
                GuideButton(Guide.MAP_LAUNCH, Modifier.align(Alignment.CenterStart))
                IconButton(onClick = vm::closeZone, modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)) {
                    Icon(Icons.Outlined.Close, ui("common.close"), tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
            Header(token, world.campaign.regions.firstOrNull { region -> region.zones.any { it.code == zone.code } }?.code)
            if (token.state == TokenState.LOCKED) {
                val keys = world.keysTo(zone.code).joinToString(ui("expedition.or")) { "«${mapTitle(it.code)}»" }
                Text(if (keys.isEmpty()) ui("expedition.opens_after_any") else ui("expedition.opens_after", keys), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            MutedText(mapDescription(zone.code.value), style = MaterialTheme.typography.bodySmall)
            index.monster(zone.boss)?.let { Guardian(game, zone, it) }
            AtlasKeys(game.atlasState?.earned.orEmpty(), zone.code.value)
            Maps(game, vm, index, zone, launch)
            Brews(game, vm, launch)
            ForgeButton(
                enabled = game.hero != null && !game.busy,
                onClick = { launchGuarded { vm.startRun(zone.code.value) } },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Icon(ForgeGlyphs.Portal, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(ui("expedition.launch_go"), style = MaterialTheme.typography.titleMedium)
            }
            AutoLaunch(game, vm, zone.code.value, launch, launchGuarded)
        }
    }
}

/**
 * The autorun (3.2.0): once the zone's guardian has fallen, a map of it can be run by itself — its packs as
 * waves on the arena, the guardian last. What else it takes on is chosen here; a crack of the Abyss and the
 * Vaal portal still stop it for the player's word.
 */
@Composable internal fun AutoLaunch(game: GameUi, vm: ExpeditionViewModel, zone: String, launch: MapLaunch, guarded: (() -> Unit) -> Unit) {
    if (game.progress?.cleared?.contains(MapCode(zone)) != true) return
    var chests by rememberSaveable { mutableStateOf(true) }
    var crystals by rememberSaveable { mutableStateOf(true) }
    var abyss by rememberSaveable { mutableStateOf(true) }
    ForgePanel {
        Engraved(ui("auto.title"))
        MutedText(ui("auto.hint"))
        listOf(
            Triple("auto.chests", chests) { v: Boolean -> chests = v },
            Triple("auto.crystals", crystals) { v: Boolean -> crystals = v },
            Triple("auto.abyss", abyss) { v: Boolean -> abyss = v },
        ).forEach { (key, on, set) ->
            Row(Modifier.fillMaxWidth().clickable { set(!on) }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = on, onCheckedChange = set)
                Text(ui(key), color = Parchment, style = MaterialTheme.typography.bodyMedium)
            }
        }
        ForgeOutlinedButton(
            enabled = game.hero != null && !game.busy && launch.picked != null,
            modifier = Modifier.fillMaxWidth(),
            onClick = { guarded { vm.startAutoRun(zone, AutoPlan(chests, crystals, abyss)) } },
        ) {
            Text(ui(if (launch.picked == null) "auto.needs_map" else "auto.go"))
        }
    }
}

/** The zone's level, biome and region over its name, and its state as a chip. */
@Composable internal fun Header(token: WorldToken, region: String?) {
    val zone = token.zone
    val locked = token.state == TokenState.LOCKED
    val kicker = listOfNotNull(
        ui("expedition.map_level", zone.level),
        zone.biome.takeUnless { locked || it.isBlank() }?.let { uiOr(uiLanguage, "expedition.biome.$it", "") }?.takeIf { it.isNotBlank() },
        ui("expedition.finale").takeIf { zone.finale && !locked },
        region?.let(::regionTitle),
    ).joinToString(" · ")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(kicker, color = Muted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (locked) ui("expedition.unknown_zone") else mapTitle(zone.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
        }
        val (label, tint) = when (token.state) {
            TokenState.PASSED -> ui("expedition.token_passed") to Gold
            TokenState.OPEN -> ui("expedition.token_open") to Rune
            TokenState.LOCKED -> ui("expedition.token_locked") to Muted
        }
        Row(
            Modifier.border(1.dp, tint.copy(alpha = .55f), CircleShape).background(tint.copy(alpha = .1f), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (locked) Icon(Icons.Outlined.Lock, null, tint = tint, modifier = Modifier.size(12.dp))
            Text(label.uppercase(), color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * The zone's guardian: its bust and name, and whether it waits by the exit or lies slain until its
 * time — the hero's campaign says when it is back; summoning it early is the map's own bar's (3.0.0).
 */
@Composable internal fun Guardian(game: GameUi, zone: Zone, boss: Monster) {
    val campaign = game.hero?.campaign
    val back = campaign?.takeIf { it.bossDown(zone.code, System.currentTimeMillis()) }?.bosses?.get(zone.code.value)
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().background(Abyss, shape).border(1.dp, PanelRaised, shape).padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(34.dp, 42.dp).clip(RoundedCornerShape(6.dp))) { Portraits.monster(this, boss.code.value, boss.form, Gold, 0f) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(monsterTitle(boss.code), color = Parchment, style = MaterialTheme.typography.titleSmall)
            Text(
                back?.let { ui("expedition.guardian_slain", untilText(it)) } ?: ui("expedition.guardian_waits"),
                color = if (back != null) Muted else Color(0xFFE0907F),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** The three atlas points a zone gives — its guardian, its guardian with a rare map, its Vaal guardian — lit once earned. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AtlasKeys(earned: List<String>, zone: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ui("atlas.open").uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
        listOf(AtlasPoints.BOSS, AtlasPoints.RARE, AtlasPoints.VAAL).forEach { kind ->
            val on = AtlasPoints.key(kind, zone) in earned
            val tint = if (on) GoldBright else Muted
            Row(
                Modifier.border(1.dp, if (on) Gold.copy(alpha = .45f) else PanelRaised, CircleShape).padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(7.dp).border(1.dp, if (on) Gold else Muted, CircleShape).background(if (on) Gold else Color.Transparent, CircleShape))
                Text(ui("expedition.key_$kind"), color = tint, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
