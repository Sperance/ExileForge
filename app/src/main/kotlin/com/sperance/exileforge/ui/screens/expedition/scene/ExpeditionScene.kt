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
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.AgentMode
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.ui.icons.drawToken
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The campaign's scene, drawn by Compose itself: the map in pseudo-isometry while walking, with
 * the hero and the monsters as round tokens of their portraits (since 2.31.0), and the
 * ground alone while fighting — the fighters are the arena overlay's framed portraits. Everything is a shape — rule 17, no picture is ever loaded
 * — and nothing here is text: names, bars and numbers are the overlay's, in the app's dictionary.
 *
 * The scene is also the run's clock: every frame [ExpeditionRun.update] is called once from the
 * frame loop and the canvas is drawn again, so the world is stepped and drawn by the same hand and
 * never seen half-moved. Reading [clock] in the draw block is what redraws it: only the drawing is
 * repeated each frame, never the composition.
 */
@Composable fun ExpeditionScene(run: ExpeditionRun, classCode: String?, modifier: Modifier = Modifier) {
    var clock by remember(run) { mutableFloatStateOf(0f) }
    val painter = remember { ScenePainter() }
    LaunchedEffect(run) {
        var last = 0L
        while (true) withFrameNanos { now ->
            if (last != 0L) {
                val dt = ((now - last) / 1e9).coerceAtMost(.05)
                run.update(dt)
                clock += dt.toFloat()
            }
            last = now
        }
    }
    Canvas(modifier) { painter.draw(this, run, clock, classCode) }
}

/** How bright a cell the hero saw once but does not see now is, against one in full light. */
private const val REMEMBERED = .32f

/** Everything the scene draws, holding the one pen it draws with. */
private class ScenePainter {
    private val pen = Pen()
    private var time = 0f
    /** Half a tile's width on screen; the tile is twice as wide as it is tall. */
    private var unit = 30f

    /** The hero's class, whose portrait is the hero's token. */
    private var classCode: String? = null

    fun draw(scope: DrawScope, run: ExpeditionRun, time: Float, classCode: String?) {
        this.time = time
        this.classCode = classCode
        pen.scope = scope
        unit = with(scope) { 30.dp.toPx() }
        val palette = Palettes.of(run.map.biome)
        scope.drawRect(palette.void)
        if (run.fight != null) fight(scope, palette) else map(scope, run, palette)
    }

    // ==================== The map ====================

    private fun isoX(x: Double, y: Double) = ((x - y) * unit).toFloat()
    private fun isoY(x: Double, y: Double) = (-(x + y) * unit / 2).toFloat()

    private fun map(scope: DrawScope, run: ExpeditionRun, palette: Palette) {
        val world = run.world
        val map = world.map
        val biome = run.map.biome
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
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) floor(x, y, palette, glow(x, y))
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) decor(map, x, y, palette, biome, glow(x, y))
            if (world.explored(map.exit.x, map.exit.y)) portal(map.exit.x + .5, map.exit.y + .5)
            // The torch's warmth on the ground, an ellipse because the ground is seen at a slant.
            val hxs = isoX(world.heroX, world.heroY)
            val hys = -isoY(world.heroX, world.heroY)
            val warm = radius * unit * 1.414f
            scope.withTransform({ scale(1f, .5f, Offset(hxs, hys)) }) {
                drawCircle(Brush.radialGradient(listOf(Palettes.torch.copy(alpha = .16f), Palettes.torch.copy(alpha = .05f), Color.Transparent),
                    Offset(hxs, hys), warm), warm, Offset(hxs, hys))
            }

            // Then everything that stands, back to front: rock, monsters and the hero by x + y.
            val heroDepth = world.heroX + world.heroY
            val standing = mutableListOf<Pair<Double, () -> Unit>>()
            for (y in ys) for (x in xs) if (map.tile(x, y) == Tile.WALL && visible(x, y) && touchesFloor(map, x, y)) {
                val depth = x + y + 1.0
                // Rock between the hero and the player is see-through, or a corridor would hide them.
                val near = depth > heroDepth && depth - heroDepth < 4 && abs((x - y) - (world.heroX - world.heroY)) < 3
                standing += depth to { wall(x, y, palette, biome, if (near) .4f else 1f, glow(x, y)) }
            }
            // Since 2.31.0 whoever walks the map is a round token cut from their portrait's face: the
            // class's for the hero, the monster's own or its form's for a monster, ringed by what it is.
            // Since 2.32.0 a monster is drawn only where the hero's light reaches.
            world.agents.filter { it.alive && world.lit(floor(it.x).toInt(), floor(it.y).toInt()) }.forEach { agent ->
                standing += (agent.x + agent.y) to {
                    val monster = agent.monster
                    // A rarer monster is a bigger one: the tier is read before the ring is noticed.
                    val size = unit * when (monster.rarity) { MonsterRarity.NORMAL -> .7f; MonsterRarity.MAGIC -> .8f; MonsterRarity.RARE -> .92f }
                    val ring = when (monster.rarity) { MonsterRarity.NORMAL -> Palettes.bronze; MonsterRarity.MAGIC -> Palettes.magic; MonsterRarity.RARE -> Palettes.rare }
                    // A sleeper sits still and a lurker low; whoever hunts bobs faster.
                    val bob = when (agent.mode) {
                        AgentMode.ASLEEP, AgentMode.LURKING -> 0f
                        AgentMode.CHASING, AgentMode.HUNTING -> abs(sin(time * 8f + agent.id)) * unit * .12f
                        else -> abs(sin(time * 3f + agent.id)) * unit * .08f
                    }
                    token(isoX(agent.x, agent.y), isoY(agent.x, agent.y), size, ring, bob) {
                        Portraits.monster(this, monster.code, monster.form, ring, time)
                        if (agent.mode == AgentMode.ASLEEP) drawRect(Color.Black.copy(alpha = .35f))
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
    }

    /**
     * A token standing on its feet at ([x], [y]) in the pen's upward measure: a shadow on the floor
     * and the disc above it, lifted by [bob]. The pen turns `y` over; a token is drawn on the scope
     * itself, so it turns it over here.
     */
    private fun token(x: Float, y: Float, radius: Float, ring: Color, bob: Float, draw: DrawScope.() -> Unit) {
        val scope = pen.scope
        val feet = -y
        scope.drawOval(Color.Black.copy(alpha = .45f), Offset(x - radius * .8f, feet - radius * .22f), Size(radius * 1.6f, radius * .44f))
        scope.drawToken(Offset(x, feet - radius * 1.1f - bob), radius, ring, draw)
    }

    private fun touchesFloor(map: ExpeditionMap, x: Int, y: Int) =
        (-1..1).any { dy -> (-1..1).any { dx -> map.walkable(x + dx, y + dy) } }

    /** A stable, faint unevenness per tile, so the ground does not read as a printed grid; [light] darkens it. */
    private fun shade(base: Color, x: Int, y: Int, spread: Float = .08f, alpha: Float = 1f, light: Float = 1f): Color {
        val noise = ((x * 73856093) xor (y * 19349663)).let { (it and 0xFF) / 255f } - .5f
        return tone(base, (1f + noise * spread * 2f) * light, alpha = alpha)
    }

    private fun diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) =
        pen.quad(cx - halfWidth, cy, cx, cy + halfHeight, cx + halfWidth, cy, cx, cy - halfHeight)

    private fun floor(x: Int, y: Int, palette: Palette, light: Float) {
        pen.color = shade(palette.floor, x, y, light = light)
        diamond(isoX(x + .5, y + .5), isoY(x + .5, y + .5), unit, unit / 2)
    }

    /**
     * What lies on the ground, by biome (since 2.32.0): the seed says which of three kinds grows on
     * a tile, the biome what that kind is — a shell on the shore, a skull in the crypt, embers in the ash.
     */
    private fun decor(map: ExpeditionMap, x: Int, y: Int, palette: Palette, biome: String, light: Float) {
        val kind = map.decorAt(x, y)
        if (kind == 0) return
        val cx = isoX(x + .5, y + .5)
        val cy = isoY(x + .5, y + .5)
        val u = unit / 6
        val base = shade(palette.decor, x, y, .15f, light = light)
        val glowing = palette.accent.copy(alpha = (.35f + .2f * sin(time * 2f + x)) * light)
        pen.color = base
        fun stone() = pen.ellipse(cx - u * 1.6f, cy - u * .6f, u * 3.2f, u * 1.6f)
        fun tuft() = (-1..1).forEach { i -> pen.triangle(cx + i * u - u * .4f, cy, cx + i * u + u * .4f, cy, cx + i * u * 1.4f, cy + u * 2.6f) }
        fun shard(color: Color = glowing) {
            pen.triangle(cx - u * .7f, cy, cx + u * .7f, cy, cx, cy + u * 3.4f)
            pen.color = color
            pen.circle(cx, cy + u * 1.2f, u * 1.4f)
        }
        fun puddle() { pen.color = tone(palette.accent, .5f * light, alpha = .55f); pen.ellipse(cx - u * 2.4f, cy - u * .9f, u * 4.8f, u * 1.8f)
            pen.color = palette.accent.copy(alpha = .25f * light); pen.ellipse(cx - u * .8f, cy - u * .2f, u * 1.2f, u * .5f) }
        fun bones() { pen.line(cx - u * 1.8f, cy - u * .4f, cx + u * 1.8f, cy + u * .4f, u * .5f); pen.circle(cx - u * 1.8f, cy - u * .4f, u * .45f)
            pen.circle(cx + u * 1.8f, cy + u * .4f, u * .45f); pen.circle(cx + u * .2f, cy + u * 1.2f, u * .9f) }
        fun flame() { pen.color = tone(palette.wallSide, light); pen.rect(cx - u * .3f, cy, u * .6f, u * 2.2f)
            pen.color = Palettes.torch.copy(alpha = (.7f + .3f * sin(time * 11f + y)) * light.coerceAtLeast(.6f)); pen.circle(cx, cy + u * 2.6f, u * .7f)
            pen.color = Palettes.torch.copy(alpha = .15f); pen.circle(cx, cy + u * 2.6f, u * 2.2f) }
        fun column() { pen.rect(cx - u * .8f, cy, u * 1.6f, u * 3.4f); pen.color = tone(palette.decor, 1.25f * light); pen.ellipse(cx - u * .8f, cy + u * 3.1f, u * 1.6f, u * .6f) }
        fun mushroom() { pen.rect(cx - u * .2f, cy, u * .4f, u * 1.4f); pen.color = glowing; pen.ellipse(cx - u * .9f, cy + u * 1.2f, u * 1.8f, u * .9f) }
        when (biome) {
            "SHORE" -> when (kind) { 1 -> stone(); 2 -> puddle(); else -> { pen.color = tone(Color(0xFFE6D2B4), light); pen.arc(cx, cy, u * 1.2f, 0f, 180f) } }
            "CAVE" -> when (kind) { 1 -> stone(); 2 -> { pen.triangle(cx - u, cy, cx + u, cy, cx, cy + u * 4f) }; else -> shard() }
            "MIRE" -> when (kind) { 1 -> puddle(); 2 -> tuft(); else -> mushroom() }
            "FOREST" -> when (kind) { 1 -> tuft(); 2 -> mushroom(); else -> { pen.ellipse(cx - u * 1.4f, cy - u * .5f, u * 2.8f, u * 1.4f); pen.color = tone(palette.decor, .7f * light); pen.ellipse(cx - u * .8f, cy + u * .2f, u * 1.6f, u * .6f) } }
            "RUINS" -> when (kind) { 1 -> stone(); 2 -> column(); else -> bones() }
            "CRYPT" -> when (kind) { 1 -> bones(); 2 -> flame(); else -> { pen.rect(cx - u, cy, u * 2f, u * 2.4f); pen.circle(cx, cy + u * 2.4f, u) } }
            "MINES" -> when (kind) { 1 -> stone(); 2 -> shard(); else -> flame() }
            "ASH" -> when (kind) { 1 -> stone(); 2 -> { pen.color = Palettes.torch.copy(alpha = (.35f + .25f * sin(time * 3f + x + y)) * light); pen.ellipse(cx - u * 2f, cy - u * .6f, u * 4f, u * 1.2f) }
                else -> { pen.rect(cx - u * .5f, cy, u, u * 2.6f); pen.color = Palettes.torch.copy(alpha = .5f * light); pen.circle(cx, cy + u * .3f, u * .5f) } }
            "FROST" -> when (kind) { 1 -> { pen.color = tone(Color(0xFFE8F2FA), light); pen.ellipse(cx - u * 2f, cy - u * .6f, u * 4f, u * 1.6f) }; 2 -> shard(); else -> stone() }
            "TEMPLE" -> when (kind) { 1 -> column(); 2 -> flame(); else -> { pen.color = glowing; pen.circle(cx, cy, u * 1.3f); pen.color = tone(palette.floor, light); pen.circle(cx, cy, u * .8f) } }
            else -> when (kind) { 1 -> stone(); 2 -> tuft(); else -> shard() }
        }
    }

    /**
     * A block of rock, a tile and a half tall since 2.32.0: two lit faces and a cap, and the biome's
     * mark on it — courses of stone in halls, ice on frost, leaves over the forest.
     */
    private fun wall(x: Int, y: Int, palette: Palette, biome: String, alpha: Float, light: Float) {
        val cx = isoX(x + .5, y + .5)
        val cy = isoY(x + .5, y + .5)
        val h = unit * 1.5f
        val left = cx - unit
        val right = cx + unit
        val bottom = cy - unit / 2
        pen.color = shade(palette.wallSide, x, y, alpha = alpha, light = light)
        pen.quad(left, cy, cx, bottom, cx, bottom + h, left, cy + h)
        pen.color = tone(shade(palette.wallSide, x, y, light = light), .7f, alpha = alpha)
        pen.quad(cx, bottom, right, cy, right, cy + h, cx, bottom + h)
        pen.color = shade(palette.wallTop, x, y, alpha = alpha, light = light)
        diamond(cx, cy + h, unit, unit / 2)
        // The cap's front edges catch the light.
        pen.color = tone(palette.wallTop, 1.35f * light, alpha = alpha * .8f)
        pen.line(left, cy + h, cx, bottom + h, unit * .05f)
        pen.line(cx, bottom + h, right, cy + h, unit * .05f)
        val mark = tone(palette.wallSide, .55f * light, alpha = alpha * .7f)
        when (biome) {
            "RUINS", "CRYPT", "TEMPLE", "MINES" -> {
                pen.color = mark
                for (i in 1..2) {
                    val lift = h * i / 3f
                    pen.line(left, cy + lift, cx, bottom + lift, unit * .03f)
                    pen.line(cx, bottom + lift, right, cy + lift, unit * .03f)
                }
            }
            "FROST" -> {
                pen.color = Color(0xFFDDEEFF).copy(alpha = alpha * .6f * light)
                for (i in 0..2) { val t = .2f + i * .3f
                    pen.triangle(left + unit * t, cy + h - unit * t / 2, left + unit * (t + .12f), cy + h - unit * (t + .12f) / 2, left + unit * (t + .06f), cy + h - unit * .6f) }
            }
            "FOREST" -> {
                pen.color = tone(palette.decor, light, alpha = alpha)
                for (i in -1..1) pen.circle(cx + i * unit * .45f, cy + h + unit * .12f, unit * .32f)
            }
            else -> {
                pen.color = mark
                pen.line(left + unit * .3f, cy + h * .7f, left + unit * .5f, cy + h * .45f, unit * .03f)
                pen.line(left + unit * .5f, cy + h * .45f, left + unit * .42f, cy + h * .2f, unit * .03f)
            }
        }
    }

    private fun portal(x: Double, y: Double) {
        val cx = isoX(x, y)
        val cy = isoY(x, y)
        val pulse = .5f + .5f * sin(time * 3f)
        for (i in 3 downTo 1) {
            pen.color = Palettes.portal.copy(alpha = .12f + .1f * i * pulse)
            pen.ellipse(cx - unit * .35f * i, cy - unit * .15f * i + unit * .6f, unit * .7f * i, unit * .5f * i)
        }
        pen.color = Color.White.copy(alpha = .5f + .4f * pulse)
        pen.ellipse(cx - unit * .25f, cy + unit * .75f, unit * .5f, unit * .8f)
    }

    // ==================== The fight ====================

    /**
     * Under the arena's frames (2.28.0) the scene keeps only the ground: the biome's floor as a lit
     * oval fading into its dark, and still (2.30.0): only the two frames move. The fighters are the overlay's framed portraits.
     */
    private fun fight(scope: DrawScope, palette: Palette) {
        val w = scope.size.width
        val h = scope.size.height
        scope.translate(0f, h * .62f) {
            for (i in 6 downTo 1) {
                pen.color = palette.floor.copy(alpha = .14f * (7 - i) / 6f + .04f)
                pen.ellipse(w / 2 - w * .09f * i, -h * .035f * i, w * .18f * i, h * .07f * i)
            }
            pen.color = palette.accent.copy(alpha = .08f)
            pen.ellipse(w / 2 - w * .3f, -h * .02f, w * .6f, h * .04f)
        }
    }
}
