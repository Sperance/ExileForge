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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.modNumber
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.run.FeatureKind
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.arena.ArenaOverlay
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.rarityTint
import com.sperance.exileforge.ui.screens.expedition.scene.BlightTint
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.expedition.scene.MapProjection
import com.sperance.exileforge.ui.screens.expedition.scene.liquidTint
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Карта похода: мини-карта и полная карта с легендой. */
/**
 * The map in small: round, framed in bronze, the hero at its centre and the explored ground moving
 * under them — rock darker than floor, the exit once seen, the chests and fountains still
 * standing. «−» and «+» in a capsule under the circle zoom out and in, and a tap opens the whole map.
 * It redraws a few times a second on its own tick — the scene's clock is the scene's.
 *
 * Изометрия сцены своим масштабом: «вверх» на мини-карте - «вверх» на сцене.
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MINIMAP_GAP)) {
        Canvas(Modifier.size(MINIMAP_SIDE).clip(CircleShape).background(Color(0xE60A0D12)).clickable { full = true }) {
            if (tick < 0) return@Canvas
            drawExplored(world, MapProjection.centred(world.heroX, world.heroY, size, size.width / cells), monsters = false)
            val centre = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xB30A0D12)), centre, radius), radius, centre)
            drawCircle(Bronze, radius - 1.5.dp.toPx(), centre, style = Stroke(3.dp.toPx()))
            drawCircle(GoldBright.copy(alpha = .35f), radius - 5.dp.toPx(), centre, style = Stroke(1.dp.toPx()))
        }
        CanvasToolbar(
            zoomTools(
                canZoomOut = cells < MINIMAP_MAX,
                canZoomIn = cells > MINIMAP_MIN,
                onZoomOut = { cells = (cells * MINIMAP_STEP).coerceAtMost(MINIMAP_MAX) },
                onZoomIn = { cells = (cells / MINIMAP_STEP).coerceAtLeast(MINIMAP_MIN) },
            ),
        )
    }
    if (full) {
        HoldsRun(run)
        FullMap(run, hud, tick) { full = false }
    }
}

/** Поперечник мини-карты в углу полосы карты. */
internal val MINIMAP_SIDE = 96.dp

/** Зазор между кругом мини-карты и капсулой «−/+» под ним: зоны касания кнопок не наезжают на круг. */
private val MINIMAP_GAP = 4.dp

/** Шаг «−/+» мини-карты: клеток в поперечнике больше или меньше во столько раз. */
private const val MINIMAP_STEP = 1.4f

/**
 * The explored ground through [projection], only what falls on the canvas: floor lighter where lit, rock dark, the chests,
 * fountains, portal and exit, the hero — and, on the whole map, the monsters standing in the hero's light, in their rarity's
 * colour. Изометрией сцены ([MapProjection]): клетка - ромб, ромбы одного цвета - одним контуром.
 *
 * Объекты карты - не исчерпанные и видимые, у комнаты - вход и рычаг; рычаг найденной запертой двери - пятно кольцами вокруг неувиденного
 * места.
 */
internal fun DrawScope.drawExplored(world: ExpeditionWorld, projection: MapProjection, monsters: Boolean) {
    val map = world.map
    val unit = projection.unit
    val (xs, ys) = projection.cells(size, map.width, map.height)
    val ground = HashMap<Color, Path>()
    for (y in ys) {
        for (x in xs) {
            if (!world.explored(x, y)) continue
            val tint = when (map.tile(x, y)) {
                Tile.FLOOR -> Parchment.copy(alpha = if (world.lit(x, y)) .55f else .3f)
                Tile.CHASM -> ChasmTint
                Tile.WATER -> map.liquid?.let(::liquidTint) ?: ChasmTint
                Tile.WALL -> Color(0xFF2A2B33)
            }
            ground.getOrPut(tint) { Path() }.diamond(projection.at(x + .5, y + .5), unit)
        }
    }
    ground.forEach { (tint, path) -> drawPath(path, tint) }
    val dot = (unit * .5f).coerceAtLeast(2.5f)
    fun mark(x: Double, y: Double, color: Color, size: Float = dot) = drawCircle(color, size, projection.at(x, y))
    world.chests.filter { !it.opened && world.shows(it) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, GoldBright) }
    world.fountains.filter { !it.used && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, ShieldCyan) }
    world.crystals.filter { !it.freed && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, CrystalViolet) }
    world.cracks.filter { !it.opened && world.explored(it.cell.x, it.cell.y) }.forEach { mark(it.cell.x + .5, it.cell.y + .5, AbyssGlow, dot * 1.2f) }
    world.portal?.takeIf { world.explored(it.x, it.y) }?.let { mark(it.x + .5, it.y + .5, PortalTint, dot * 1.2f) }
    world.features.filter { !it.spent && it.shown(world) }.forEach { spot -> featureMark(spot.kind)?.let { mark(spot.focus.x + .5, spot.focus.y + .5, it.tint) } }
    world.features.filterIsInstance<RoomSpot>().mapNotNull { spot -> spot.lever?.takeIf { !spot.opened && world.explored(it.x, it.y) } }.forEach { mark(it.x + .5, it.y + .5, RoomMark.tint) }
    world.features.forEach { spot ->
        val hint = (spot as? RoomSpot)?.takeIf { it.hinted(world) }?.hint ?: return@forEach
        val radius = world.rules.lever.hint.toFloat() * unit
        for (k in 1..HINT_RINGS) mark(hint.first, hint.second, RoomMark.tint.copy(alpha = HINT_ALPHA), radius * k / HINT_RINGS)
    }
    if (world.explored(map.exit.x, map.exit.y)) mark(map.exit.x + .5, map.exit.y + .5, if (world.sealed) LifeRed else Vital, dot * 1.4f)
    if (monsters) {
        world.agents.filter { it.alive && world.shows(it) }.forEach { agent ->
            mark(agent.x, agent.y, Color.Black, dot * 1.25f)
            mark(agent.x, agent.y, rarityTint(agent.monster.rarity), dot)
        }
    }
    mark(world.heroX, world.heroY, Gold, dot * 1.4f)
    mark(world.heroX, world.heroY, Ink, dot * .5f)
}

/** Ромб клетки с серединой [centre] при полуширине [unit] - в контур. */
private fun Path.diamond(centre: Offset, unit: Float) {
    moveTo(centre.x, centre.y - unit / 2)
    lineTo(centre.x + unit, centre.y)
    lineTo(centre.x, centre.y + unit / 2)
    lineTo(centre.x - unit, centre.y)
    close()
}

/**
 * The whole map, over everything: all the explored ground fitted to the screen, the
 * monsters in the hero's light, a legend of what each mark is, what the map still holds, and its modifiers.
 *
 * Модификаторы - одно окно и для карты, и для зоны Ваал ([ExpeditionRun.modifiers], [ModifierList]): список сумм по характеристикам,
 * строка с несколькими источниками раскрывается стрелкой в «источник … доля».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FullMap(run: ExpeditionRun, hud: RunHud, tick: Int, onClose: () -> Unit) {
    val world = run.world
    val map = world.map
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        ChordOverWindow()
        Column(
            Modifier.fillMaxSize().background(Ink.copy(alpha = .96f)).statusBarsPadding().navigationBarsPadding().padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mapTitle(hud.mapCode), color = GoldBright, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                ForgeIconButton(Icons.Outlined.Close, ui("common.close"), onClick = onClose)
            }
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(MapProjection.FITTED_ASPECT).background(Color(0xFF07090C), RoundedCornerShape(8.dp))
                    .border(1.dp, Bronze, RoundedCornerShape(8.dp)),
            ) {
                if (tick < 0) return@Canvas
                drawExplored(world, MapProjection.fitted(map.width, map.height, size), monsters = true)
            }
            val explored = (0 until map.height).sumOf { y -> (0 until map.width).count { x -> map.walkable(x, y) && world.explored(x, y) } }
            val floorCells = (0 until map.height).sumOf { y -> (0 until map.width).count { x -> map.walkable(x, y) } }.coerceAtLeast(1)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Counter(ui("map.monsters_left", hud.alive, hud.total), LifeRed)
                Counter(ui("map.chests_left", hud.chestsLeft), GoldBright)
                Counter(ui("map.fountains_left", hud.fountainsLeft), ShieldCyan)
                if (hud.crystalsLeft > 0) Counter(ui("map.crystals_left", hud.crystalsLeft), CrystalViolet)
                if (hud.cracksLeft > 0) Counter(ui("map.cracks_left", hud.cracksLeft), AbyssGlow)
                GuardianLine.of(hud).let { Counter(it.text, it.color) }
                Counter(ui("map.explored", explored * 100 / floorCells), Parchment)
            }
            Engraved(ui("map.legend"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                legendOf(world).forEach { (tint, text) -> Legend(tint, text) }
            }
            val modifiers = remember(run, run.mapEffects) { run.modifiers }
            ModifierList(modifiers, run.index)
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
    world.features.filter { !it.spent && it.shown(world) }.mapNotNull { featureMark(it.kind) }.distinct().forEach { add(it.tint to ui(it.legend)) }
    if (world.features.any { it is RoomSpot && it.lever?.let { lever -> world.explored(lever.x, lever.y) } == true && !it.opened }) add(RoomMark.tint to ui("map.legend_lever"))
    if (world.features.any { it is RoomSpot && it.hinted(world) }) add(RoomMark.tint.copy(alpha = HINT_ALPHA * HINT_RINGS) to ui("map.legend_lever_hint"))
    world.features.filterIsInstance<BlightSpot>().filter { it.shown(world) }.forEach { spot ->
        val point = spot.active ?: return@forEach
        if (spot.cleared(world, point)) add(BlightTint.bile to ui("map.legend_blight_chest"))
        if (point == spot.blight.motherChoice && !spot.cleared(world, point)) add(BlightTint.hot to ui("map.legend_mother"))
    }
    val exit = world.map.exit
    if (world.explored(exit.x, exit.y)) add(if (world.sealed) LifeRed to ui("map.legend_sealed") else Vital to ui("map.legend_exit"))
    world.agents.filter { it.alive && world.shows(it) }.map { it.monster.rarity }.distinct().sorted()
        .forEach { add(rarityTint(it) to ui(it.key())) }
}

/**
 * A map's summed line in the server's own sentence for it, as an item's line reads: «Игрок получает на 20% больше физического урона»;
 * числа правил шаблона - из контента [index].
 */
internal fun effectText(stat: String, value: Double, index: ContentIndex): String = SkillText.statLine(stat, Op.ADD, value, index)

/** Отметка объекта карты на миникарте: цвет и ключ легенды. */
internal data class FeatureMark(val tint: Color, val legend: String)

private val RoomMark = FeatureMark(Color(0xFFE8E0C8), "map.legend_room")

/** Пятно «рычаг где-то здесь»: столько колец, каждое такой прозрачности - к середине гуще, край размыт. */
private const val HINT_RINGS = 4
private const val HINT_ALPHA = .09f

/** Отметка объекта карты; ловушек на миникарте нет - их видно только на самой карте. */
internal fun featureMark(kind: FeatureKind): FeatureMark? = when (kind) {
    FeatureKind.ALTAR -> FeatureMark(Color(0xFFD03040), "map.legend_altar")
    FeatureKind.MERCHANT -> FeatureMark(Color(0xFFE8C060), "map.legend_merchant")
    FeatureKind.TRAP -> null
    FeatureKind.SECRET, FeatureKind.VAULT -> RoomMark
    FeatureKind.NODE -> FeatureMark(Color(0xFF4FA048), "map.legend_node")
    FeatureKind.BLIGHT -> FeatureMark(BlightTint.vein, "map.legend_blight")
}

/** Пропасть на миникарте: темнее пола, но не скала - сквозь неё видно. */
private val ChasmTint = Color(0xFF0E1420)

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

/** A crystal of essences on the maps and its sheet: the violet of its glass. */
internal val CrystalViolet = Color(0xFFB07FE0)

/** How many cells the minimap shows across by default, and how near and how far its zoom goes. */
internal const val MINIMAP_CELLS = 22f
internal const val MINIMAP_MIN = 10f
internal const val MINIMAP_MAX = 60f
