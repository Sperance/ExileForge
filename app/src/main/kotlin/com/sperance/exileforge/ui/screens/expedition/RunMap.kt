package com.sperance.exileforge.ui.screens.expedition
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.rarityTint
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.expedition.scene.SCENE_UNIT
import com.sperance.exileforge.ui.screens.expedition.scene.sceneToWorld
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Карта похода (3.80.24): мини-карта и полная карта с легендой. */
/**
 * The map in small: round, framed in bronze, the hero at its centre and the explored ground moving
 * under them (2.56.1) — rock darker than floor, the exit once seen, the chests and fountains still
 * standing. Two small buttons under it zoom in and out (2.72.0), and a tap opens the whole map.
 * It redraws a few times a second on its own tick — the scene's clock is the scene's.
 */
@Composable internal fun MiniMap(run: ExpeditionRun, hud: RunHud) {
    val world = run.world
    var tick by remember(world) { mutableIntStateOf(0) }
    var cells by rememberSaveable { mutableFloatStateOf(MINIMAP_CELLS) }
    var full by remember { mutableStateOf(false) }
    LaunchedEffect(world) {
        while (true) {
            kotlinx.coroutines.delay(200)
            tick++
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.size(150.dp).clip(CircleShape).background(Color(0xE60A0D12)).clickable { full = true }) {
            if (tick < 0) return@Canvas
            val cell = size.width / cells
            drawExplored(world, Offset(size.width / 2 - world.heroX.toFloat() * cell, size.height / 2 - world.heroY.toFloat() * cell), cell, monsters = false)
            val centre = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xB30A0D12)), centre, radius), radius, centre)
            drawCircle(Bronze, radius - 1.5.dp.toPx(), centre, style = Stroke(3.dp.toPx()))
            drawCircle(GoldBright.copy(alpha = .35f), radius - 5.dp.toPx(), centre, style = Stroke(1.dp.toPx()))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZoomButton("−", ui("expedition.zoom_out")) { cells = (cells * 1.4f).coerceAtMost(MINIMAP_MAX) }
            ZoomButton("+", ui("expedition.zoom_in")) { cells = (cells / 1.4f).coerceAtLeast(MINIMAP_MIN) }
        }
    }
    if (full) {
        HoldsRun(run)
        FullMap(run, hud, tick) { full = false }
    }
}

@Composable internal fun ZoomButton(sign: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(Color(0xE60A0D12)).border(1.dp, Bronze, CircleShape)
            .clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(sign, color = GoldBright, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * The explored ground from [origin] at [cell] pixels a cell, only what falls on the canvas: floor
 * lighter where lit, rock dark, the chests, fountains, portal and exit, the hero — and, on the whole
 * map, the monsters standing in the hero's light, in their rarity's colour.
 */
internal fun DrawScope.drawExplored(world: ExpeditionWorld, origin: Offset, cell: Float, monsters: Boolean) {
    val map = world.map
    val square = Size(cell, cell)
    val xs = (floor(-origin.x / cell).toInt() - 1).coerceAtLeast(0)..(ceil((size.width - origin.x) / cell).toInt() + 1).coerceAtMost(map.width - 1)
    val ys = (floor(-origin.y / cell).toInt() - 1).coerceAtLeast(0)..(ceil((size.height - origin.y) / cell).toInt() + 1).coerceAtMost(map.height - 1)
    for (y in ys) {
        for (x in xs) {
            if (!world.explored(x, y)) continue
            drawRect(if (map.walkable(x, y)) Parchment.copy(alpha = if (world.lit(x, y)) .55f else .3f) else Color(0xFF2A2B33), Offset(origin.x + x * cell, origin.y + y * cell), square)
        }
    }
    val dot = (cell * .5f).coerceAtLeast(2.5f)
    fun mark(x: Double, y: Double, color: Color, size: Float = dot) = drawCircle(color, size, Offset(origin.x + x.toFloat() * cell, origin.y + y.toFloat() * cell))
    world.chests.filter { !it.opened && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, GoldBright) }
    world.fountains.filter { !it.used && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, ShieldCyan) }
    world.crystals.filter { !it.freed && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, CrystalViolet) }
    world.cracks.filter { !it.opened && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, AbyssGlow, dot * 1.2f) }
    world.portal?.takeIf { world.explored(it.x, it.y) }?.let { mark(it.x + .5, it.y + .5, PortalTint, dot * 1.2f) }
    if (world.explored(map.exit.x, map.exit.y)) mark(map.exit.x + .5, map.exit.y + .5, if (world.sealed) LifeRed else Vital, dot * 1.4f)
    if (monsters) {
        world.agents.filter { it.alive && world.lit(it.x.toInt(), it.y.toInt()) }.forEach { agent ->
            mark(agent.x, agent.y, Color.Black, dot * 1.25f)
            mark(agent.x, agent.y, rarityTint(agent.monster.rarity), dot)
        }
    }
    mark(world.heroX, world.heroY, Gold, dot * 1.4f)
    mark(world.heroX, world.heroY, Ink, dot * .5f)
}

/**
 * The whole map (2.72.0), over everything: all the explored ground fitted to the screen, the
 * monsters in the hero's light, a legend of what each mark is, what the map still holds, and what
 * the map item and the atlas lay on it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FullMap(run: ExpeditionRun, hud: RunHud, tick: Int, onClose: () -> Unit) {
    val world = run.world
    val map = world.map
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(Ink.copy(alpha = .96f)).statusBarsPadding().navigationBarsPadding().padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mapTitle(hud.mapCode), color = GoldBright, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, ui("common.close"), tint = Gold) }
            }
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(map.width / map.height.toFloat()).background(Color(0xFF07090C), RoundedCornerShape(8.dp))
                    .border(1.dp, Bronze, RoundedCornerShape(8.dp)),
            ) {
                if (tick < 0) return@Canvas
                drawExplored(world, Offset.Zero, minOf(size.width / map.width, size.height / map.height), monsters = true)
            }
            val explored = (0 until map.height).sumOf { y -> (0 until map.width).count { x -> map.walkable(x, y) && world.explored(x, y) } }
            val floorCells = (0 until map.height).sumOf { y -> (0 until map.width).count { x -> map.walkable(x, y) } }.coerceAtLeast(1)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Counter(ui("map.monsters_left", hud.alive, hud.total), LifeRed)
                Counter(ui("map.chests_left", hud.chestsLeft), GoldBright)
                Counter(ui("map.fountains_left", hud.fountainsLeft), ShieldCyan)
                if (hud.crystalsLeft > 0) Counter(ui("map.crystals_left", hud.crystalsLeft), CrystalViolet)
                if (hud.cracksLeft > 0) Counter(ui("map.cracks_left", hud.cracksLeft), AbyssGlow)
                Counter(ui(if (hud.sealed) "expedition.boss_alive" else "expedition.boss_slain"), if (hud.sealed) LifeRed else Vital)
                Counter(ui("map.explored", explored * 100 / floorCells), Parchment)
            }
            Engraved(ui("map.legend"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                legendOf(world).forEach { (tint, text) -> Legend(tint, text) }
            }
            if (run.mapEffects.isNotEmpty()) {
                Engraved(ui("map.modifiers"))
                // Read as an item's modifiers read, a sentence under its glyph, only without a tier: the lines are the map's and the atlas's summed.
                run.mapEffects.forEach { (stat, value) ->
                    Tipped({ Tip(statTitle(stat), tint = ModBlue) }) { ModifierLine(effectText(stat, value), Glyph.ofStat(stat)) }
                }
            } else {
                MutedText(ui("map.no_modifiers"))
            }
            // The atlas's bonuses on this run (3.81.0), every one of them — the fight's and the rolls' alike.
            if (run.atlas.isNotEmpty()) {
                Engraved(ui("map.atlas"))
                run.atlas.forEach { (stat, value) ->
                    Tipped({ Tip(statTitle(stat), tint = Rune) }) { ModifierLine(effectText(stat, value), Glyph.ofStat(stat)) }
                }
            }
        }
    }
}

/**
 * The marks the whole map draws that stand on it now and are in sight: a chest found and still shut, the exit
 * once seen in the colour it has, the monsters of each rarity in the hero's light. The hero is always there.
 */
internal fun legendOf(world: ExpeditionWorld): List<Pair<Color, String>> = buildList {
    add(Gold to ui("map.legend_hero"))
    if (world.chests.any { !it.opened && world.explored(it.cell.x, it.cell.y) }) add(GoldBright to ui("map.legend_chest"))
    if (world.fountains.any { !it.used && world.explored(it.cell.x, it.cell.y) }) add(ShieldCyan to ui("map.legend_fountain"))
    if (world.crystals.any { !it.freed && world.explored(it.cell.x, it.cell.y) }) add(CrystalViolet to ui("map.legend_crystal"))
    if (world.cracks.any { !it.opened && world.explored(it.cell.x, it.cell.y) }) add(AbyssGlow to ui("map.legend_abyss"))
    if (world.portal?.let { world.explored(it.x, it.y) } == true) add(PortalTint to ui("map.legend_portal"))
    val exit = world.map.exit
    if (world.explored(exit.x, exit.y)) add(if (world.sealed) LifeRed to ui("map.legend_sealed") else Vital to ui("map.legend_exit"))
    world.agents.filter { it.alive && world.lit(it.x.toInt(), it.y.toInt()) }.map { it.monster.rarity }.distinct().sorted()
        .forEach { add(rarityTint(it) to ui(it.key())) }
}

/** A map's summed line (3.81.0) in the server's own sentence for it, as an item's line reads: «Игрок получает на 20% больше физического урона». */
internal fun effectText(stat: String, value: Double): String = SkillText.statLine(stat, Op.ADD, value)

/** The Vaal portal's mark on the maps and in their legend. */
internal val PortalTint = Color(0xFFFF8A78)

@Composable internal fun Counter(text: String, tint: Color) {
    Text(
        text,
        color = tint,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(tint.copy(alpha = .1f), RoundedCornerShape(6.dp)).border(1.dp, tint.copy(alpha = .4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable internal fun Legend(tint: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(tint, CircleShape))
        Text(text, color = Parchment, style = MaterialTheme.typography.labelSmall)
    }
}

/** A crystal of essences on the maps and its sheet (2.78.0): the violet of its glass. */
internal val CrystalViolet = Color(0xFFB07FE0)

/** How many cells the minimap shows across by default, and how near and how far its zoom goes. */
internal const val MINIMAP_CELLS = 22f
internal const val MINIMAP_MIN = 10f
internal const val MINIMAP_MAX = 60f
