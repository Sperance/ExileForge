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
 * The campaign's scene, drawn by Compose itself: the map in pseudo-isometry while walking — its ground,
 * rock and air in the biome's [MapStyle] since 2.64.0 — with
 * the hero and the monsters as round tokens of their portraits (since 2.31.0), and while fighting
 * the cave of the owner's mockup VI behind the fight's cards (2.57.0, [fightBackdrop]). Everything is a shape — rule 17, no picture is ever loaded
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
        if (run.fight != null) scope.fightBackdrop(palette, time) else map(scope, run, palette)
    }

    // ==================== The map ====================

    private fun isoX(x: Double, y: Double) = ((x - y) * unit).toFloat()
    private fun isoY(x: Double, y: Double) = (-(x + y) * unit / 2).toFloat()

    private fun map(scope: DrawScope, run: ExpeditionRun, palette: Palette) {
        val world = run.world
        val map = world.map
        val biome = run.map.biome
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
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) decor(map, x, y, palette, biome, glow(x, y))
            if (world.explored(map.exit.x, map.exit.y)) portal(map.exit.x + .5, map.exit.y + .5, world.sealed)
            world.portal?.takeIf { world.explored(it.x, it.y) }?.let { vaalPortal(it.x + .5, it.y + .5, glow(it.x, it.y)) }
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
                standing += depth to { style.wall(frame, spot(map, x, y), palette, if (near) .4f else 1f, glow(x, y)) }
            }
            // A chest stands once the hero has seen its place (2.33.0); an opened one stays, open.
            // A fountain stands once seen (2.48.0): brimming until drunk, dry after.
            world.fountains.filter { world.explored(it.cell.x, it.cell.y) }.forEach { fountain ->
                standing += (fountain.cell.x + fountain.cell.y + 1.0) to { drawFountain(fountain.cell.x + .5, fountain.cell.y + .5, fountain.used, glow(fountain.cell.x, fountain.cell.y)) }
            }
            world.chests.filter { world.explored(it.cell.x, it.cell.y) }.forEach { chest ->
                standing += (chest.cell.x + chest.cell.y + 1.0) to { drawChest(chest.cell.x + .5, chest.cell.y + .5, chest.opened, glow(chest.cell.x, chest.cell.y)) }
            }
            // Since 2.31.0 whoever walks the map is a round token cut from their portrait's face: the
            // class's for the hero, the monster's own or its form's for a monster, ringed by what it is.
            // Since 2.32.0 a monster is drawn only where the hero's light reaches.
            world.agents.filter { it.alive && world.lit(floor(it.x).toInt(), floor(it.y).toInt()) }.forEach { agent ->
                standing += (agent.x + agent.y) to {
                    val monster = agent.monster
                    // A rarer monster is a bigger one: the tier is read before the ring is noticed.
                    val size = unit * when (monster.rarity) { MonsterRarity.NORMAL -> .7f; MonsterRarity.MAGIC -> .8f; MonsterRarity.RARE -> .92f; MonsterRarity.UNIQUE -> 1.2f }
                    val ring = when (monster.rarity) { MonsterRarity.NORMAL -> Palettes.bronze; MonsterRarity.MAGIC -> Palettes.magic; MonsterRarity.RARE -> Palettes.rare; MonsterRarity.UNIQUE -> Palettes.unique }
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
        // The air over everything (2.64.0): drips, fog, sparks or fireflies, by the biome's style.
        style.atmosphere(scope, palette, time)
    }

    /** A cell as the style reads it: where its centre falls and how much rock borders it. */
    private fun spot(map: ExpeditionMap, x: Int, y: Int) = TileSpot(x, y, isoX(x + .5, y + .5), isoY(x + .5, y + .5),
        listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).count { (nx, ny) -> !map.walkable(nx, ny) })

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
            "VAAL" -> when (kind) { 1 -> bones(); 2 -> flame(); else -> puddle() }
            "TEMPLE" -> when (kind) { 1 -> column(); 2 -> flame(); else -> { pen.color = glowing; pen.circle(cx, cy, u * 1.3f); pen.color = tone(palette.floor, light); pen.circle(cx, cy, u * .8f) } }
            else -> when (kind) { 1 -> stone(); 2 -> tuft(); else -> shard() }
        }
    }

    /** A fountain: a stone basin on the ground, its water glowing while it can still heal, dark once drunk. */
    private fun drawFountain(x: Double, y: Double, used: Boolean, light: Float) {
        val cx = isoX(x, y)
        val cy = isoY(x, y)
        val w = unit * .42f
        val d = unit * .21f
        val h = unit * .2f
        val stone = Color(0xFF6E6A62)
        pen.color = Color.Black.copy(alpha = .35f)
        pen.ellipse(cx - w * 1.2f, cy - d * .8f, w * 2.4f, d * 1.6f)
        pen.color = tone(stone, .7f * light)
        pen.quad(cx - w, cy, cx, cy - d, cx, cy - d + h, cx - w, cy + h)
        pen.color = tone(stone, .55f * light)
        pen.quad(cx, cy - d, cx + w, cy, cx + w, cy + h, cx, cy - d + h)
        pen.color = tone(stone, .95f * light)
        diamond(cx, cy + h, w, d)
        val water = if (used) Color(0xFF1B2226) else Color(0xFF4FB8D8)
        pen.color = tone(water, light)
        diamond(cx, cy + h, w * .72f, d * .72f)
        if (!used) {
            pen.color = water.copy(alpha = (.35f + .25f * sin(time * 2.5f + x.toFloat())) * light)
            pen.circle(cx, cy + h, unit * .45f)
            pen.color = Color.White.copy(alpha = .5f * light)
            pen.circle(cx, cy + h - unit * .05f, unit * .05f + unit * .03f * sin(time * 4f))
        }
    }

    /** A chest: an iron-bound box on the ground, its lid shut and gleaming, or thrown back on an empty one. */
    private fun drawChest(x: Double, y: Double, opened: Boolean, light: Float) {
        val cx = isoX(x, y)
        val cy = isoY(x, y)
        val w = unit * .45f
        val d = unit * .22f
        val h = unit * .38f
        val wood = Color(0xFF6B4423)
        val iron = Color(0xFF3A3A40)
        pen.color = Color.Black.copy(alpha = .35f)
        pen.ellipse(cx - w * 1.2f, cy - d * .8f, w * 2.4f, d * 1.6f)
        // Two faces seen at a slant, and the top.
        pen.color = tone(wood, .85f * light)
        pen.quad(cx - w, cy, cx, cy - d, cx, cy - d + h, cx - w, cy + h)
        pen.color = tone(wood, .65f * light)
        pen.quad(cx, cy - d, cx + w, cy, cx + w, cy + h, cx, cy - d + h)
        pen.color = tone(iron, light)
        pen.line(cx - w, cy + h * .5f, cx, cy - d + h * .5f, unit * .04f)
        pen.line(cx, cy - d + h * .5f, cx + w, cy + h * .5f, unit * .04f)
        if (opened) {
            pen.color = tone(Color(0xFF1A120B), light)
            diamond(cx, cy + h, w, d)
            pen.color = tone(wood, .95f * light)
            pen.quad(cx - w, cy + h, cx, cy + h + d, cx, cy + h + d + h * .8f, cx - w, cy + h + h * .8f)
        } else {
            pen.color = tone(wood, 1.05f * light)
            diamond(cx, cy + h, w, d)
            pen.color = Palettes.torch.copy(alpha = (.55f + .35f * sin(time * 3f + x.toFloat())) * light)
            pen.circle(cx, cy + h * .55f, unit * .06f)
            pen.color = Palettes.torch.copy(alpha = .12f * light)
            pen.circle(cx, cy + h * .7f, unit * .5f)
        }
    }

    /** The Vaal portal (2.65.0): a black mouth in scarlet rings, beating like the zone behind it. */
    private fun vaalPortal(x: Double, y: Double, light: Float) {
        val cx = isoX(x, y)
        val cy = isoY(x, y)
        val beat = .5f + .5f * sin(time * 2.4f)
        val lit = light.coerceAtLeast(.5f)
        pen.color = Palettes.blood.copy(alpha = .25f * lit)
        pen.ellipse(cx - unit * .9f, cy - unit * .3f, unit * 1.8f, unit * .6f)
        for (i in 3 downTo 1) {
            pen.color = Color(0xFFFF3C28).copy(alpha = (.1f + .12f * i * beat) * lit)
            pen.ellipse(cx - unit * .32f * i, cy - unit * .12f * i + unit * .7f, unit * .64f * i, unit * .55f * i)
        }
        pen.color = Color(0xFF120204)
        pen.ellipse(cx - unit * .28f, cy + unit * .72f, unit * .56f, unit * .95f)
        pen.color = Color(0xFFFF5A46).copy(alpha = (.55f + .35f * beat) * lit)
        pen.ring(cx - unit * .3f, cy + unit * .7f, unit * .6f, unit * 1f, unit * .06f)
    }

    /** The exit: a pale gate, or — while its guardian lives (since 2.34.0) — a dim red one crossed by chains. */
    private fun portal(x: Double, y: Double, sealed: Boolean) {
        val cx = isoX(x, y)
        val cy = isoY(x, y)
        val pulse = .5f + .5f * sin(time * (if (sealed) 1.5f else 3f))
        val hue = if (sealed) Palettes.blood else Palettes.portal
        for (i in 3 downTo 1) {
            pen.color = hue.copy(alpha = .12f + .1f * i * pulse)
            pen.ellipse(cx - unit * .35f * i, cy - unit * .15f * i + unit * .6f, unit * .7f * i, unit * .5f * i)
        }
        pen.color = (if (sealed) Palettes.blood else Color.White).copy(alpha = .5f + .4f * pulse)
        pen.ellipse(cx - unit * .25f, cy + unit * .75f, unit * .5f, unit * .8f)
        if (sealed) {
            pen.color = Palettes.steel.copy(alpha = .85f)
            pen.line(cx - unit * .4f, cy + unit * .7f, cx + unit * .4f, cy + unit * 1.5f, unit * .06f)
            pen.line(cx + unit * .4f, cy + unit * .7f, cx - unit * .4f, cy + unit * 1.5f, unit * .06f)
            pen.circle(cx, cy + unit * 1.1f, unit * .1f)
        }
    }
}
