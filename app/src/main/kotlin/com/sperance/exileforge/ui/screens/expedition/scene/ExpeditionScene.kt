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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.Action
import com.sperance.exileforge.core.campaign.Battle
import com.sperance.exileforge.core.campaign.HitKind
import com.sperance.exileforge.core.campaign.Outcome
import com.sperance.exileforge.core.campaign.Side
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin

/**
 * Where the fighters stand on the fight screen, as shares of its width and height from the top —
 * the scene draws them there and the overlay prints their bars and numbers above the same spots.
 */
object FightLayout {
    const val HERO_X = .28f
    const val MONSTER_X = .72f
    const val GROUND_Y = .62f
}

/**
 * The campaign's scene, drawn by Compose itself: the map in pseudo-isometry while walking, two
 * fighters face to face while fighting — lunging, casting bolts, drinking, coloured by what is on
 * them and ringed by its particles. Everything is a shape — rule 17, no picture is ever loaded
 * — and nothing here is text: names, bars and numbers are the overlay's, in the app's dictionary.
 *
 * The scene is also the run's clock: every frame [ExpeditionRun.update] is called once from the
 * frame loop and the canvas is drawn again, so the world is stepped and drawn by the same hand and
 * never seen half-moved. Reading [clock] in the draw block is what redraws it: only the drawing is
 * repeated each frame, never the composition.
 */
@Composable fun ExpeditionScene(run: ExpeditionRun, modifier: Modifier = Modifier) {
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
    Canvas(modifier) { painter.draw(this, run, clock) }
}

/** Everything the scene draws, holding the one pen and the one set of figures it draws with. */
private class ScenePainter {
    private val pen = Pen()
    private val figures = Figures(pen)
    private var time = 0f
    /** Half a tile's width on screen; the tile is twice as wide as it is tall. */
    private var unit = 30f

    fun draw(scope: DrawScope, run: ExpeditionRun, time: Float) {
        this.time = time
        pen.scope = scope
        unit = with(scope) { 30.dp.toPx() }
        val palette = Palettes.of(run.map.biome)
        scope.drawRect(palette.void)
        val battle = run.fight
        if (battle != null) fight(scope, run, battle, palette) else map(scope, run, palette)
    }

    // ==================== The map ====================

    private fun isoX(x: Double, y: Double) = ((x - y) * unit).toFloat()
    private fun isoY(x: Double, y: Double) = (-(x + y) * unit / 2).toFloat()

    private fun map(scope: DrawScope, run: ExpeditionRun, palette: Palette) {
        val world = run.world
        val map = world.map
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

        // A tile off either side of the screen is not drawn: the square around the hero is wider
        // than the diamond the screen shows.
        val across = (width / 2 / unit + 2).toDouble()
        fun visible(x: Int, y: Int) = abs((x - y) - (world.heroX - world.heroY)) < across
        scope.translate(width / 2 - cameraX, height * .55f + cameraY) {
            // The ground first, all of it: nothing stands below the floor.
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) floor(x, y, palette)
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) decor(map, x, y, palette)
            portal(map.exit.x + .5, map.exit.y + .5)

            // Then everything that stands, back to front: rock, monsters and the hero by x + y.
            val heroDepth = world.heroX + world.heroY
            val standing = mutableListOf<Pair<Double, () -> Unit>>()
            for (y in ys) for (x in xs) if (map.tile(x, y) == Tile.WALL && visible(x, y) && touchesFloor(map, x, y)) {
                val depth = x + y + 1.0
                // Rock between the hero and the player is see-through, or a corridor would hide them.
                val near = depth > heroDepth && depth - heroDepth < 3 && abs((x - y) - (world.heroX - world.heroY)) < 3
                standing += depth to { wall(x, y, palette, if (near) .45f else 1f) }
            }
            world.agents.filter { it.alive }.forEach { agent ->
                standing += (agent.x + agent.y) to {
                    val sx = isoX(agent.x, agent.y)
                    val sy = isoY(agent.x, agent.y)
                    when (agent.monster.rarity) {
                        MonsterRarity.MAGIC -> figures.ring(sx, sy, unit * 1.3f, Palettes.magic, time)
                        MonsterRarity.RARE -> figures.ring(sx, sy, unit * 1.5f, Palettes.rare, time)
                        MonsterRarity.NORMAL -> Unit
                    }
                    val facing = if (agent.targetX - agent.targetY >= agent.x - agent.y) 1f else -1f
                    // A rarer monster is a bigger one: the tier is read before the ring is noticed.
                    val scale = when (agent.monster.rarity) { MonsterRarity.NORMAL -> 1.5f; MonsterRarity.MAGIC -> 1.65f; MonsterRarity.RARE -> 1.85f }
                    figures.monster(agent.monster.form, sx, sy, unit * scale, facing, time)
                }
            }
            standing += heroDepth to {
                val facing = if (world.facingX - world.facingY >= 0) 1f else -1f
                figures.hero(isoX(world.heroX, world.heroY), isoY(world.heroX, world.heroY), unit * 1.6f, facing, time, world.moving)
            }
            standing.sortedBy { it.first }.forEach { it.second() }
        }
    }

    private fun touchesFloor(map: ExpeditionMap, x: Int, y: Int) =
        (-1..1).any { dy -> (-1..1).any { dx -> map.walkable(x + dx, y + dy) } }

    /** A stable, faint unevenness per tile, so the ground does not read as a printed grid. */
    private fun shade(base: Color, x: Int, y: Int, spread: Float = .08f, alpha: Float = 1f): Color {
        val noise = ((x * 73856093) xor (y * 19349663)).let { (it and 0xFF) / 255f } - .5f
        return tone(base, 1f + noise * spread * 2f, alpha = alpha)
    }

    private fun diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) =
        pen.quad(cx - halfWidth, cy, cx, cy + halfHeight, cx + halfWidth, cy, cx, cy - halfHeight)

    private fun floor(x: Int, y: Int, palette: Palette) {
        pen.color = shade(palette.floor, x, y)
        diamond(isoX(x + .5, y + .5), isoY(x + .5, y + .5), unit, unit / 2)
    }

    private fun decor(map: ExpeditionMap, x: Int, y: Int, palette: Palette) {
        val kind = map.decorAt(x, y)
        if (kind == 0) return
        val cx = isoX(x + .5, y + .5)
        val cy = isoY(x + .5, y + .5)
        val u = unit / 6
        pen.color = shade(palette.decor, x, y, .15f)
        when (kind) {
            // A stone, a tuft and a shard: which one grows where is the seed's business.
            1 -> pen.ellipse(cx - u * 1.6f, cy - u * .6f, u * 3.2f, u * 1.6f)
            2 -> for (i in -1..1) pen.triangle(cx + i * u - u * .4f, cy, cx + i * u + u * .4f, cy, cx + i * u * 1.4f, cy + u * 2.6f)
            else -> {
                pen.triangle(cx - u * .7f, cy, cx + u * .7f, cy, cx, cy + u * 3.4f)
                pen.color = palette.accent.copy(alpha = .35f + .2f * sin(time * 2f + x))
                pen.circle(cx, cy + u * 1.2f, u * 1.4f)
            }
        }
    }

    private fun wall(x: Int, y: Int, palette: Palette, alpha: Float) {
        val cx = isoX(x + .5, y + .5)
        val cy = isoY(x + .5, y + .5)
        val h = unit * .9f
        val left = cx - unit
        val right = cx + unit
        val bottom = cy - unit / 2
        pen.color = shade(palette.wallSide, x, y, alpha = alpha)
        pen.quad(left, cy, cx, bottom, cx, bottom + h, left, cy + h)
        pen.color = tone(shade(palette.wallSide, x, y), .75f, alpha = alpha)
        pen.quad(cx, bottom, right, cy, right, cy + h, cx, bottom + h)
        pen.color = shade(palette.wallTop, x, y, alpha = alpha)
        diamond(cx, cy + h, unit, unit / 2)
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

    private fun fight(scope: DrawScope, run: ExpeditionRun, battle: Battle, palette: Palette) {
        val monster = run.fightAgentOnMap?.monster ?: return
        val w = scope.size.width
        val h = scope.size.height
        val ground = h * (1 - FightLayout.GROUND_Y)
        val size = max(w, h) * .2f
        val lunge = battle.lunge()
        // A critical strike shakes the whole scene for a moment; the pen measures up from the bottom edge.
        val shake = lunge?.let { (event, progress) ->
            if (event.kind == HitKind.CRIT && event.action != Action.TICK && progress > .5) ((1 - progress) * unit * .4 * sin(progress * 70)).toFloat() else 0f
        } ?: 0f
        scope.translate(shake, h + shake / 2) {
            // The ground as a lit oval fading into the biome's dark.
            for (i in 6 downTo 1) {
                pen.color = palette.floor.copy(alpha = .16f * (7 - i) / 6f + .05f)
                pen.ellipse(w / 2 - w * .08f * i, ground - h * .03f * i, w * .16f * i, h * .06f * i)
            }
            val heroX = w * FightLayout.HERO_X
            val monsterX = w * FightLayout.MONSTER_X
            var heroShift = if (battle.retreating) -size * .25f else 0f
            var monsterShift = 0f
            var heroFlash = 0f
            var monsterFlash = 0f
            lunge?.let { (event, progress) ->
                val t = progress.toFloat()
                val swing = sin(t * PI).toFloat()
                val byHero = event.actor == Side.HERO
                val targetX = if (byHero) monsterX else heroX
                val sourceX = if (byHero) heroX else monsterX
                when (event.action) {
                    Action.ATTACK -> {
                        val reach = (monsterX - heroX) * .28f * swing
                        val flash = if (event.landed && t > .5f) (1 - t) * 2f * (if (event.kind == HitKind.CRIT) 1f else .7f) else 0f
                        val dodge = if (event.kind == HitKind.EVADED) swing * size * .25f else 0f
                        if (byHero) { heroShift += reach; monsterFlash = flash; monsterShift = dodge }
                        else { monsterShift = -reach; heroFlash = flash; heroShift -= dodge }
                    }
                    // A spell is a bolt that crosses from the caster and bursts on the target.
                    Action.SPELL -> {
                        val flight = (t * 2).coerceAtMost(1f)
                        val bx = sourceX + (targetX - sourceX) * flight
                        val by = ground + size * .55f + sin(flight * PI).toFloat() * size * .3f
                        val tint = Palettes.arcane
                        if (t < .5f) {
                            for (i in 3 downTo 1) { pen.color = tint.copy(alpha = .15f * i); pen.circle(bx, by, size * .05f * i) }
                            pen.color = Color.White.copy(alpha = .8f); pen.circle(bx, by, size * .035f)
                        } else if (event.landed) {
                            val burst = (t - .5f) * 2
                            pen.color = tint.copy(alpha = .55f * (1 - burst))
                            pen.circle(targetX, ground + size * .5f, size * (.15f + .45f * burst))
                            if (byHero) monsterFlash = (1 - burst) * .8f else heroFlash = (1 - burst) * .8f
                        }
                    }
                    // The flask: a green swell around the drinker.
                    Action.FLASK -> {
                        pen.color = Palettes.vital.copy(alpha = .45f * (1 - t))
                        pen.ellipse(heroX - size * .45f * (1 + t), ground - size * .1f, size * .9f * (1 + t), size * .35f * (1 + t))
                    }
                    Action.RETREAT, Action.TICK -> Unit
                }
                if (event.kind == HitKind.BLOCKED && t > .4f) {
                    val target = if (byHero) monsterX - size * .3f else heroX + size * .3f
                    pen.color = Palettes.steel.copy(alpha = .7f * (1 - t))
                    pen.arc(target, ground + size * .5f, size * .45f, if (byHero) 110f else -70f, 140f)
                }
                if (event.kind == HitKind.CRIT && event.action == Action.ATTACK && t > .5f) {
                    pen.color = Palettes.blood.copy(alpha = .5f * (1 - t))
                    pen.circle(targetX, ground + size * .5f, size * .6f * t)
                }
            }
            val outcome = battle.outcome
            val fade = if (outcome != null) ((battle.time - battle.duration) / ExpeditionRun.AFTERMATH).toFloat().coerceIn(0f, 1f) else 0f
            val grown = when (monster.rarity) { MonsterRarity.NORMAL -> 1f; MonsterRarity.MAGIC -> 1.1f; MonsterRarity.RARE -> 1.25f }
            when (monster.rarity) {
                MonsterRarity.MAGIC -> figures.ring(monsterX + monsterShift, ground, size * .9f * grown, Palettes.magic, time)
                MonsterRarity.RARE -> figures.ring(monsterX + monsterShift, ground, size * grown, Palettes.rare, time)
                MonsterRarity.NORMAL -> Unit
            }
            val heroSide = battle.fighter(Side.HERO)
            val monsterSide = battle.fighter(Side.MONSTER)
            // What is on a fighter colours it: frost blue, fire orange, venom green, a shock's flicker.
            figures.tinted(monsterSide.ailments.map { it.ailment }, time) {
                figures.monster(monster.form, monsterX + monsterShift, ground, size * grown, -1f, time, monsterFlash, if (outcome == Outcome.WIN) 1 - fade else 1f)
            }
            figures.tinted(heroSide.ailments.map { it.ailment }, time) {
                figures.hero(heroX + heroShift, ground, size * 1.05f, 1f, time, battle.retreating && outcome == null, heroFlash, if (outcome == Outcome.LOSS) 1 - fade else 1f)
            }
            if (outcome == null || fade < 1f) {
                figures.ailments(monsterSide.ailments.map { it.ailment }, monsterX + monsterShift, ground, size * grown, time)
                figures.ailments(heroSide.ailments.map { it.ailment }, heroX + heroShift, ground, size * 1.05f, time)
                if (heroSide.held) figures.daze(heroX + heroShift, ground + size * 1.05f, size, time)
                if (monsterSide.held) figures.daze(monsterX + monsterShift, ground + size * grown, size * grown, time)
            }
        }
    }
}
