package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.run.AgentMode
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.sealed
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.ui.icons.drawToken
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** How bright a cell the hero saw once but does not see now is, against one in full light. */
private const val REMEMBERED = .32f

/** Everything the scene draws, holding the one pen it draws with. */
internal class ScenePainter {
    internal val pen = Pen()
    internal var time = 0f

    /** Half a tile's width on screen; the tile is twice as wide as it is tall. */
    internal var unit = 30f

    /** The hero's class, whose portrait is the hero's token. */
    internal var classCode: String? = null

    /** Босс этого захода запечатан (3.93.0). */
    private var sealed = false

    fun draw(scope: DrawScope, run: ExpeditionRun, time: Float, classCode: String?) {
        this.time = time
        sealed = run.sealed
        this.classCode = classCode
        pen.scope = scope
        unit = with(scope) { SCENE_UNIT.toPx() }
        val palette = Palettes.of(run.zone.biome)
        scope.drawRect(palette.void)
        if (run.fight != null) scope.fightBackdrop(palette, time) else map(scope, run, palette)
    }

    // ==================== The map ====================

    internal fun isoX(x: Double, y: Double) = ((x - y) * unit).toFloat()
    internal fun isoY(x: Double, y: Double) = (-(x + y) * unit / 2).toFloat()

    internal fun map(scope: DrawScope, run: ExpeditionRun, palette: Palette) {
        val world = run.world
        val map = world.map
        val biome = run.zone.biome
        val style = MapStyles.of(biome)
        val frame = SceneFrame(pen, unit, time)
        val width = scope.size.width
        val height = scope.size.height
        // The hero stands a little below the middle: more of the map lies ahead of a player than behind.
        val cameraX = isoX(world.heroX, world.heroY)
        val cameraY = isoY(world.heroX, world.heroY) + unit
        val reach = ceil(max(width, height) / unit).toInt() / 2 + 3
        val hx = world.heroX.toInt()
        val hy = world.heroY.toInt()
        val xs = (hx - reach).coerceAtLeast(0)..(hx + reach).coerceAtMost(map.width - 1)
        val ys = (hy - reach).coerceAtLeast(0)..(hy + reach).coerceAtMost(map.height - 1)
        // The torch breathes a little: the edge of the light is never a printed circle.
        val radius = (world.lightRadius * (1 + .03 * sin(time * 7f) + .02 * sin(time * 13f + 1f))).toFloat()

        // How lit a cell is (since 2.32.0): full near the hero, fading to the edge of the light, and
        // what was only remembered stays dim. What was never seen is not drawn at all.
        fun glow(x: Int, y: Int): Float {
            if (!world.lit(x, y)) return REMEMBERED
            val d = (hypot(x + .5 - world.heroX, y + .5 - world.heroY) / radius).toFloat().coerceIn(0f, 1f)
            return 1f - (1f - REMEMBERED) * d * d
        }

        // A tile off either side of the screen is not drawn: the square around the hero is wider
        // than the diamond the screen shows.
        val across = (width / 2 / unit + 2).toDouble()
        fun visible(x: Int, y: Int) = world.explored(x, y) && abs((x - y) - (world.heroX - world.heroY)) < across
        scope.translate(width / 2 - cameraX, height * .55f + cameraY) {
            // The ground first, all of it: nothing stands below the floor.
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) style.floor(frame, spot(map, x, y), palette, glow(x, y))
            // Chasms (3.91.0) sink into the ground: drawn with it, under everything that stands.
            for (y in ys) for (x in xs) if (map.tile(x, y) == Tile.CHASM && visible(x, y)) style.chasm(frame, spot(map, x, y), palette, glow(x, y))
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) decor(map, x, y, palette, biome, glow(x, y))
            if (world.explored(map.exit.x, map.exit.y)) portal(map.exit.x + .5, map.exit.y + .5, world.sealed)
            world.portal?.takeIf { world.explored(it.x, it.y) }?.let { vaalPortal(it.x + .5, it.y + .5, glow(it.x, it.y)) }
            // The torch's warmth on the ground, an ellipse because the ground is seen at a slant.
            val hxs = isoX(world.heroX, world.heroY)
            val hys = -isoY(world.heroX, world.heroY)
            val warm = radius * unit * 1.414f
            scope.withTransform({ scale(1f, .5f, Offset(hxs, hys)) }) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Palettes.torch.copy(alpha = .16f), Palettes.torch.copy(alpha = .05f), Color.Transparent),
                        Offset(hxs, hys),
                        warm,
                    ),
                    warm,
                    Offset(hxs, hys),
                )
            }

            // Then everything that stands, back to front: rock, monsters and the hero by x + y.
            val heroDepth = world.heroX + world.heroY
            val standing = mutableListOf<Pair<Double, () -> Unit>>()
            for (y in ys) {
                for (x in xs) {
                    if (map.tile(x, y) == Tile.WALL && visible(x, y) && touchesFloor(map, x, y)) {
                        val depth = x + y + 1.0
                        // Rock between the hero and the player is see-through, or a corridor would hide them.
                        val near = depth > heroDepth && depth - heroDepth < 4 && abs((x - y) - (world.heroX - world.heroY)) < 3
                        standing += depth to { style.wall(frame, spot(map, x, y), palette, if (near) .4f else 1f, glow(x, y)) }
                    }
                }
            }
            // A chest stands once the hero has seen its place (2.33.0); an opened one stays, open.
            // A fountain stands once seen (2.48.0): brimming until drunk, dry after.
            world.fountains.filter { world.explored(it.cell.x, it.cell.y) }.forEach { fountain ->
                standing += (fountain.cell.x + fountain.cell.y + 1.0) to { drawFountain(fountain.cell.x + .5, fountain.cell.y + .5, fountain.used, glow(fountain.cell.x, fountain.cell.y)) }
            }
            world.chests.filter { world.explored(it.cell.x, it.cell.y) }.forEach { chest ->
                standing += (chest.cell.x + chest.cell.y + 1.0) to { drawChest(chest.cell.x + .5, chest.cell.y + .5, chest.opened, glow(chest.cell.x, chest.cell.y)) }
            }
            // A crystal of essences stands once seen (2.78.0): humming until its guardian is slain, dull after.
            world.crystals.filter { world.explored(it.cell.x, it.cell.y) }.forEach { spot ->
                standing += (spot.cell.x + spot.cell.y + 1.0) to { drawCrystal(spot.cell.x + .5, spot.cell.y + .5, spot.freed, spot.crystal.stronger, glow(spot.cell.x, spot.cell.y)) }
            }
            // A crack of the Abyss gapes once seen (2.82.0): breathing violet until opened, a dull scar after.
            world.cracks.filter { world.explored(it.cell.x, it.cell.y) }.forEach { spot ->
                standing += (spot.cell.x + spot.cell.y + .5) to { drawCrack(spot.cell.x + .5, spot.cell.y + .5, spot.opened, spot.id, glow(spot.cell.x, spot.cell.y)) }
            }
            // Объекты карты (3.90.0): каждый своим знаком, когда виден; ловушка - только замеченная светом.
            world.features.filter { it.shown(world) }.forEach { spot -> standing += featureParts(spot, { x, y -> glow(x, y) }, { x, y -> world.explored(x, y) }) }
            // Since 2.31.0 whoever walks the map is a round token cut from their portrait's face: the
            // class's for the hero, the monster's own or its form's for a monster, ringed by what it is.
            // Since 2.32.0 a monster is drawn only where the hero's light reaches.
            world.agents.filter { it.alive && world.lit(floor(it.x).toInt(), floor(it.y).toInt()) }.forEach { agent ->
                standing += (agent.x + agent.y) to {
                    val monster = agent.monster
                    // A rarer monster is a bigger one: the tier is read before the ring is noticed.
                    val size = unit * when (monster.rarity) {
                        MonsterRarity.NORMAL -> .7f
                        MonsterRarity.MAGIC -> .8f
                        MonsterRarity.RARE -> .92f
                        MonsterRarity.UNIQUE -> 1.2f
                    }
                    val ring = when (monster.rarity) {
                        MonsterRarity.NORMAL -> Palettes.bronze
                        MonsterRarity.MAGIC -> Palettes.magic
                        MonsterRarity.RARE -> Palettes.rare
                        MonsterRarity.UNIQUE -> Palettes.unique
                    }
                    // A sleeper sits still and a lurker low; whoever hunts bobs faster.
                    val bob = when (agent.mode) {
                        AgentMode.ASLEEP, AgentMode.LURKING -> 0f
                        AgentMode.CHASING, AgentMode.HUNTING -> abs(sin(time * 8f + agent.id)) * unit * .12f
                        else -> abs(sin(time * 3f + agent.id)) * unit * .08f
                    }
                    token(isoX(agent.x, agent.y), isoY(agent.x, agent.y), size, ring, bob) {
                        Portraits.monster(this, monster.code.value, monster.form, ring, time)
                        if (agent.mode == AgentMode.ASLEEP) drawRect(Color.Black.copy(alpha = .35f))
                        // Печать стража (3.93.0): запечатанный босс - под фиолетовой пеленой с бегущим кругом
                        if (sealed && agent === world.boss) {
                            drawRect(Color(0xFF2A1040).copy(alpha = .55f + .1f * sin(time * 2f)))
                            drawArc(Color(0xFFB07CFF), time * 90f, 270f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(drawContext.size.minDimension * .06f))
                        }
                    }
                }
            }
            standing += heroDepth to {
                val bob = if (world.moving) abs(sin(time * 9f)) * unit * .14f else 0f
                token(isoX(world.heroX, world.heroY), isoY(world.heroX, world.heroY), unit * .85f, Palettes.hero, bob) {
                    Portraits.hero(this, classCode, time)
                }
            }
            standing.sortedBy { it.first }.forEach { it.second() }
        }
        // The air over everything (2.64.0): drips, fog, sparks or fireflies, by the biome's style.
        style.atmosphere(scope, palette, time)
    }

    /** A cell as the style reads it: where its centre falls and how much rock borders it. */
    internal fun spot(map: ExpeditionMap, x: Int, y: Int) = TileSpot(
        x,
        y,
        isoX(x + .5, y + .5),
        isoY(x + .5, y + .5),
        listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).count { (nx, ny) -> !map.walkable(nx, ny) },
    )

    /**
     * A token standing on its feet at ([x], [y]) in the pen's upward measure: a shadow on the floor
     * and the disc above it, lifted by [bob]. The pen turns `y` over; a token is drawn on the scope
     * itself, so it turns it over here.
     */
    internal fun token(x: Float, y: Float, radius: Float, ring: Color, bob: Float, draw: DrawScope.() -> Unit) {
        val scope = pen.scope
        val feet = -y
        scope.drawOval(Color.Black.copy(alpha = .45f), Offset(x - radius * .8f, feet - radius * .22f), Size(radius * 1.6f, radius * .44f))
        scope.drawToken(Offset(x, feet - radius * 1.1f - bob), radius, ring, draw)
    }

    internal fun touchesFloor(map: ExpeditionMap, x: Int, y: Int) = (-1..1).any { dy -> (-1..1).any { dx -> map.walkable(x + dx, y + dy) } }

    /** A stable, faint unevenness per tile, so the ground does not read as a printed grid; [light] darkens it. */
    internal fun shade(base: Color, x: Int, y: Int, spread: Float = .08f, alpha: Float = 1f, light: Float = 1f): Color {
        val noise = ((x * 73856093) xor (y * 19349663)).let { (it and 0xFF) / 255f } - .5f
        return tone(base, (1f + noise * spread * 2f) * light, alpha = alpha)
    }

    internal fun diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) = pen.quad(cx - halfWidth, cy, cx, cy + halfHeight, cx + halfWidth, cy, cx, cy - halfHeight)
}
