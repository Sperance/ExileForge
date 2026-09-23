package com.sperance.exileforge.ui.screens.expedition.gdx

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.ExpeditionRun
import com.sperance.exileforge.core.campaign.FightPlayback
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
    const val GROUND_Y = .66f
}

/** The run the scene draws. A fragment has no constructor to pass it through, so it is handed over here. */
object SceneHost {
    @Volatile var run: ExpeditionRun? = null
}

/**
 * The campaign's scene: the map in pseudo-isometry while walking, two fighters face to face while
 * fighting. Everything is a shape — rule 17, no picture is ever loaded — and nothing here is text:
 * names, bars and numbers are the Compose overlay's, in the app's own dictionary.
 *
 * The scene is also the run's clock: [ExpeditionRun.update] is called once a frame on this thread,
 * so the world is stepped and drawn by the same hand and never seen half-moved.
 */
class ExpeditionScene : ApplicationAdapter() {
    private lateinit var shapes: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var figures: Figures
    private var time = 0f
    private val colour = Color()

    override fun create() {
        shapes = ShapeRenderer()
        camera = OrthographicCamera()
        figures = Figures(shapes)
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
    }

    override fun render() {
        val dt = Gdx.graphics.deltaTime.coerceAtMost(.05f)
        time += dt
        val run = SceneHost.run
        run?.update(dt.toDouble())
        val palette = Palettes.of(run?.map?.biome.orEmpty())
        Gdx.gl.glClearColor(palette.void.r, palette.void.g, palette.void.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        if (run == null) return
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        val playback = run.fight
        if (playback != null) drawFight(run, playback, palette) else drawMap(run, palette)
    }

    override fun dispose() { if (::shapes.isInitialized) shapes.dispose() }

    // ==================== The map ====================

    /** Half a tile's width on screen; the tile is twice as wide as it is tall. */
    private val unit get() = 30f * max(1f, Gdx.graphics.density)

    private fun isoX(x: Double, y: Double) = ((x - y) * unit).toFloat()
    private fun isoY(x: Double, y: Double) = (-(x + y) * unit / 2).toFloat()

    private fun drawMap(run: ExpeditionRun, palette: Palette) {
        val world = run.world
        val map = world.map
        camera.position.set(isoX(world.heroX, world.heroY), isoY(world.heroX, world.heroY) + unit, 0f)
        camera.update()
        shapes.projectionMatrix = camera.combined
        val reach = ceil(max(Gdx.graphics.width, Gdx.graphics.height) / unit).toInt() + 2
        val hx = world.heroX.toInt()
        val hy = world.heroY.toInt()
        val xs = (hx - reach).coerceAtLeast(0)..(hx + reach).coerceAtMost(map.width - 1)
        val ys = (hy - reach).coerceAtLeast(0)..(hy + reach).coerceAtMost(map.height - 1)

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // The ground first, all of it: nothing stands below the floor.
        for (y in ys) for (x in xs) if (map.walkable(x, y)) floor(x, y, palette)
        for (y in ys) for (x in xs) if (map.walkable(x, y)) decor(map, x, y, palette)
        portal(map.exit.x + .5, map.exit.y + .5)

        // Then everything that stands, back to front: rock, monsters and the hero by x + y.
        val heroDepth = world.heroX + world.heroY
        val standing = mutableListOf<Pair<Double, () -> Unit>>()
        for (y in ys) for (x in xs) if (map.tile(x, y) == Tile.WALL && touchesFloor(map, x, y)) {
            val depth = x + y + 1.0
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
                figures.monster(agent.monster.form, sx, sy, unit * 1.5f, facing, time)
            }
        }
        standing += heroDepth to {
            val facing = if (world.facingX - world.facingY >= 0) 1f else -1f
            figures.hero(isoX(world.heroX, world.heroY), isoY(world.heroX, world.heroY), unit * 1.6f, facing, time, world.moving)
        }
        standing.sortedBy { it.first }.forEach { it.second() }
        shapes.end()
    }

    private fun touchesFloor(map: ExpeditionMap, x: Int, y: Int) =
        (-1..1).any { dy -> (-1..1).any { dx -> map.walkable(x + dx, y + dy) } }

    private fun shade(base: Color, x: Int, y: Int, spread: Float = .08f): Color {
        val noise = ((x * 73856093) xor (y * 19349663)).let { (it and 0xFF) / 255f } - .5f
        return colour.set(base).mul(1f + noise * spread * 2f, 1f + noise * spread * 2f, 1f + noise * spread * 2f, 1f)
    }

    private fun floor(x: Int, y: Int, palette: Palette) {
        val cx = isoX(x + .5, y + .5)
        val cy = isoY(x + .5, y + .5)
        shapes.color = shade(palette.floor, x, y)
        diamond(cx, cy, unit, unit / 2)
    }

    private fun diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) {
        shapes.triangle(cx - halfWidth, cy, cx, cy + halfHeight, cx + halfWidth, cy)
        shapes.triangle(cx - halfWidth, cy, cx, cy - halfHeight, cx + halfWidth, cy)
    }

    private fun decor(map: ExpeditionMap, x: Int, y: Int, palette: Palette) {
        val kind = map.decorAt(x, y)
        if (kind == 0) return
        val cx = isoX(x + .5, y + .5)
        val cy = isoY(x + .5, y + .5)
        val u = unit / 6
        shapes.color = shade(palette.decor, x, y, .15f)
        when (kind) {
            // A stone, a tuft and a shard: which one grows where is the seed's business.
            1 -> shapes.ellipse(cx - u * 1.6f, cy - u * .6f, u * 3.2f, u * 1.6f)
            2 -> for (i in -1..1) shapes.triangle(cx + i * u - u * .4f, cy, cx + i * u + u * .4f, cy, cx + i * u * 1.4f, cy + u * 2.6f)
            else -> {
                shapes.triangle(cx - u * .7f, cy, cx + u * .7f, cy, cx, cy + u * 3.4f)
                shapes.color = colour.set(palette.accent).also { it.a = .35f + .2f * sin(time * 2f + x) }
                shapes.circle(cx, cy + u * 1.2f, u * 1.4f, 10)
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
        shapes.color = shade(palette.wallSide, x, y).also { it.a = alpha }
        shapes.triangle(left, cy, cx, bottom, cx, bottom + h)
        shapes.triangle(left, cy, left, cy + h, cx, bottom + h)
        shapes.color = shade(palette.wallSide, x, y).mul(.75f, .75f, .75f, 1f).also { it.a = alpha }
        shapes.triangle(cx, bottom, right, cy, right, cy + h)
        shapes.triangle(cx, bottom, cx, bottom + h, right, cy + h)
        shapes.color = shade(palette.wallTop, x, y).also { it.a = alpha }
        diamond(cx, cy + h, unit, unit / 2)
    }

    private fun portal(x: Double, y: Double) {
        val cx = isoX(x, y)
        val cy = isoY(x, y)
        val pulse = .5f + .5f * sin(time * 3f)
        for (i in 3 downTo 1) {
            shapes.color = colour.set(Palettes.portal).also { it.a = .12f + .1f * i * pulse }
            shapes.ellipse(cx - unit * .35f * i, cy - unit * .15f * i + unit * .6f, unit * .7f * i, unit * .5f * i)
        }
        shapes.color = colour.set(Color.WHITE).also { it.a = .5f + .4f * pulse }
        shapes.ellipse(cx - unit * .25f, cy + unit * .75f, unit * .5f, unit * .8f)
    }

    // ==================== The fight ====================

    private fun drawFight(run: ExpeditionRun, playback: FightPlayback, palette: Palette) {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        camera.position.set(w / 2, h / 2, 0f)
        camera.update()
        shapes.projectionMatrix = camera.combined
        val ground = h * (1 - FightLayout.GROUND_Y)
        val size = max(w, h) * .2f

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        // The ground as a lit oval fading into the biome's dark.
        for (i in 6 downTo 1) {
            shapes.color = colour.set(palette.floor).also { it.a = .16f * (7 - i) / 6f + .05f }
            shapes.ellipse(w / 2 - w * .08f * i, ground - h * .03f * i, w * .16f * i, h * .06f * i)
        }
        val heroX = w * FightLayout.HERO_X
        val monsterX = w * FightLayout.MONSTER_X
        var heroShift = 0f
        var monsterShift = 0f
        var heroFlash = 0f
        var monsterFlash = 0f
        playback.lunge()?.let { (event, t) ->
            val swing = sin(t * PI).toFloat()
            val reach = (monsterX - heroX) * .28f * swing
            val landed = event.kind == HitKind.HIT || event.kind == HitKind.CRIT
            val flash = if (landed && t > .5f) (1 - t.toFloat()) * 2f * (if (event.kind == HitKind.CRIT) 1f else .7f) else 0f
            val dodge = if (event.kind == HitKind.EVADED) swing * size * .25f else 0f
            if (event.attacker == Side.HERO) { heroShift = reach; monsterFlash = flash; monsterShift = dodge }
            else { monsterShift = -reach; heroFlash = flash; heroShift = -dodge }
            if (event.kind == HitKind.BLOCKED && t > .4f) {
                val target = if (event.attacker == Side.HERO) monsterX - size * .3f else heroX + size * .3f
                shapes.color = colour.set(Palettes.steel).also { it.a = .7f * (1 - t.toFloat()) }
                shapes.arc(target, ground + size * .5f, size * .45f, if (event.attacker == Side.HERO) 110f else -70f, 140f, 16)
            }
            if (event.kind == HitKind.CRIT && t > .5f) {
                val target = if (event.attacker == Side.HERO) monsterX else heroX
                shapes.color = colour.set(Palettes.blood).also { it.a = .5f * (1 - t.toFloat()) }
                shapes.circle(target, ground + size * .5f, size * .6f * t.toFloat(), 24)
            }
        }
        val outcome = playback.log.outcome.takeIf { playback.clock >= playback.log.duration }
        val fade = ((playback.clock - playback.log.duration) / ExpeditionRun.AFTERMATH).toFloat().coerceIn(0f, 1f)
        val monster = playback.agent.monster
        when (monster.rarity) {
            MonsterRarity.MAGIC -> figures.ring(monsterX + monsterShift, ground, size * .9f, Palettes.magic, time)
            MonsterRarity.RARE -> figures.ring(monsterX + monsterShift, ground, size, Palettes.rare, time)
            MonsterRarity.NORMAL -> Unit
        }
        figures.monster(monster.form, monsterX + monsterShift, ground, size, -1f, time, monsterFlash,
            if (outcome == Outcome.WIN) 1 - fade else 1f)
        figures.hero(heroX + heroShift, ground, size * 1.05f, 1f, time, false, heroFlash,
            if (outcome == Outcome.LOSS) 1 - fade else 1f)
        shapes.end()
    }
}
