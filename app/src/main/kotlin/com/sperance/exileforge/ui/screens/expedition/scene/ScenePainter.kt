package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.MapGenerator
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.run.AgentMode
import com.sperance.exileforge.core.campaign.run.AilmentView
import com.sperance.exileforge.core.campaign.run.BlightSpot
import com.sperance.exileforge.core.campaign.run.EntranceCue
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.sealed
import com.sperance.exileforge.core.campaign.run.waiting
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.ui.icons.drawToken
import com.sperance.exileforge.ui.screens.expedition.arena.ailmentTint
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

    /** Анимации-декор включены (`LocalMotion`): без них декор стоит в покое ([decor]). */
    internal var motion = true

    /** Значки материалов узлов ремесла по коду материала (4.7.0), их готовит сцена. */
    internal var materials: Map<String, MaterialMark> = emptyMap()

    /** Часы декора: при выключенных анимациях стоят на нуле. */
    internal val decor: Float get() = if (motion) time else 0f

    /** Массивы скалы и контуры вод карты (4.2.0): считаются раз на карту и её правку. */
    private var relief: MapRelief? = null

    /** Контуры стен карты: раз на карту, её правку, стиль и масштаб. */
    private var walls: WallRelief? = null

    /** Пещера боя: её контуры и неподвижные кисти живут со сценой, а не собираются на кадр (4.9.0). */
    private val stage = FightStage()

    /** Очередь стоящего (скала, жетоны, ориентиры) по глубине: одна на сцену, без нового списка на кадр (4.9.0). */
    private val standing = ArrayList<Pair<Double, () -> Unit>>()

    /** Нить кольца простоя у трещины (4.9.0): пересоздаётся лишь со сменой масштаба, не на кадр. */
    private var ringStroke: Stroke? = null

    /** Кисть ореола недугов: одна, пока мера сцены та же. */
    private var haloStroke: Stroke? = null

    fun draw(scope: DrawScope, run: ExpeditionRun, time: Float, classCode: String?) {
        this.time = time
        sealed = run.sealed
        this.classCode = classCode
        pen.scope = scope
        unit = with(scope) { SCENE_UNIT.toPx() }
        val palette = Palettes.of(run.zone.biome)
        scope.drawRect(palette.void)
        if (run.fight != null) stage.draw(scope, palette, time) else map(scope, run, palette)
    }

    // ==================== The map ====================

    internal fun isoX(x: Double, y: Double) = ((x - y) * unit).toFloat()
    internal fun isoY(x: Double, y: Double) = (-(x + y) * unit / 2).toFloat()

    internal fun map(scope: DrawScope, run: ExpeditionRun, palette: Palette) {
        val world = run.world
        val map = world.map
        val biome = run.zone.biome
        val style = MapStyles.of(biome)
        val shape = MapRelief.of(map, world.shoreline, relief).also { relief = it }
        val rock = WallRelief.of(map, shape, style, MapGenerator.styleOf(biome), palette, unit, walls).also { walls = it }
        // Узор земли, скалы и воды - декор (4.2.0): без `LocalMotion` стоит
        val frame = SceneFrame(pen, unit, decor)
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
        val radius = (world.lightRadius * (1 + .03 * sin(decor * 7f) + .02 * sin(decor * 13f + 1f))).toFloat()

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

        // Глубокую скалу герой не видит, но край массива каймы заходит в неё: он виден вместе с соседней клеткой
        fun shown(x: Int, y: Int) = visible(x, y) || rock.deep(x, y) && nearby { dx, dy -> visible(x + dx, y + dy) }

        // Свет скалы: глубокая клетка светится, как самый светлый сосед, - крышка массива не темнеет пятном
        fun rockGlow(x: Int, y: Int): Float {
            if (!rock.deep(x, y)) return glow(x, y)
            var best = Float.NEGATIVE_INFINITY
            for (dy in -1..1) for (dx in -1..1) best = max(best, glow(x + dx, y + dy))
            return best
        }
        scope.translate(width / 2 - cameraX, height * .55f + cameraY) {
            // The ground first, all of it: nothing stands below the floor.
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) style.floor(frame, spot(map, x, y), palette, glow(x, y))
            // Земля под скалой: подложка массива, где нет пола
            for (y in ys) for (x in xs) rock.cell(x, y)?.takeIf { it.plate != null && shown(x, y) }?.let { style.wallGround(frame, it, rock.tones, rockGlow(x, y)) }
            // Chasms (3.91.0) sink into the ground: drawn with it, under everything that stands. Вода (3.95.0): реки и озёра биома -
            // вровень с полом. С 4.2.0 и то и другое - одна гладь по контуру карты, неувиденное под ней закрыто тьмой
            fun cells(tile: Tile, near: Boolean) = buildList {
                for (y in ys) {
                    for (x in xs) {
                        if (!visible(x, y)) continue
                        val hit = map.tile(x, y) == tile || near && nearby { dx, dy -> map.tile(x + dx, y + dy) == tile }
                        if (hit) add(PoolCell(spot(map, x, y), glow(x, y)))
                    }
                }
            }
            fun hide(tile: Tile) {
                pen.color = palette.void
                for (y in ys) for (x in xs) if (!world.explored(x, y) && nearby { dx, dy -> map.tile(x + dx, y + dy) == tile }) diamond(isoX(x + .5, y + .5), isoY(x + .5, y + .5), unit, unit / 2)
            }
            listOfNotNull(Tile.CHASM, Tile.WATER.takeIf { map.liquid != null }).forEach { tile ->
                val pool = cells(tile, near = false)
                if (pool.isEmpty()) return@forEach
                val outline = shape.outline(tile, unit)
                when (tile) {
                    Tile.WATER -> style.water(frame, pool, cells(tile, near = true), outline, palette, checkNotNull(map.liquid))
                    else -> style.chasm(frame, pool, cells(tile, near = true), outline, palette)
                }
                hide(tile)
            }
            for (y in ys) for (x in xs) if (map.walkable(x, y) && visible(x, y)) decor(map, x, y, palette, biome, glow(x, y))
            // Мягкая тень у подножия скалы ложится на пол и его мелочь
            for (y in ys) for (x in xs) rock.cell(x, y)?.takeIf { shown(x, y) }?.let { style.wallShadow(frame, it) }
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
            standing.clear()
            // Rock between the hero and the player is see-through, or a corridor would hide them. С 4.2.0 - круглое «окно» вокруг
            // героя, монстров, что гонятся за ним, и закрытых скалой ориентиров (3.95.3): скала перед ними прозрачна к центру
            val windows = buildList {
                add(Window(world.heroX, world.heroY))
                world.agents.filter { it.alive && it.mode in ENGAGED && world.lit(floor(it.x).toInt(), floor(it.y).toInt()) }.forEach { add(Window(it.x, it.y)) }
                world.screened().forEach { add(Window(it.x + .5, it.y + .5)) }
            }
            // Скала - кусками массива по клеткам (контур один на массив): кусок встаёт в очередь с глубиной своей клетки, как
            // прежний блок, так что жетоны перед скалой и за ней сортируются как раньше
            for (y in ys) {
                for (x in xs) {
                    val piece = rock.cell(x, y)?.piece ?: continue
                    if (!shown(x, y)) continue
                    val alpha = windows.minOf { it.alpha(x, y) }
                    standing += piece.depth to { style.wall(frame, piece, x, y, rock.tones, palette, alpha, rockGlow(x, y)) }
                }
            }
            // Выход и портал Ваал стоят по глубине, как всё стоящее (3.95.3): прежде их рисовала земля, и любая скала за ними
            // ложилась поверх овала
            if (world.explored(map.exit.x, map.exit.y)) standing += (map.exit.x + map.exit.y + 1.0) to { portal(map.exit.x + .5, map.exit.y + .5, world.sealed) }
            world.portal?.takeIf { world.explored(it.x, it.y) }?.let { at -> standing += (at.x + at.y + 1.0) to { vaalPortal(at.x + .5, at.y + .5, glow(at.x, at.y)) } }
            // A chest stands once the hero has seen its place (2.33.0); an opened one stays, open.
            // A fountain stands once seen (2.48.0): brimming until drunk, dry after.
            world.fountains.filter { world.explored(it.cell.x, it.cell.y) }.forEach { fountain ->
                standing += (fountain.cell.x + fountain.cell.y + 1.0) to { drawFountain(fountain.cell.x + .5, fountain.cell.y + .5, fountain.used, glow(fountain.cell.x, fountain.cell.y)) }
            }
            world.chests.filter { world.shows(it) }.forEach { chest ->
                // Открытый сундук без ответа сервера (4.10.0) приоткрыт и ждёт
                standing += (chest.cell.x + chest.cell.y + 1.0) to { drawChest(chest.cell.x + .5, chest.cell.y + .5, chest.opened, glow(chest.cell.x, chest.cell.y), run.waiting(chest)) }
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
            world.features.filter { it.shown(world) }.forEach { spot -> standing += featureParts(spot, world, { x, y -> glow(x, y) }, { x, y -> world.explored(x, y) }) }
            val blight = world.features.firstNotNullOfOrNull { it as? BlightSpot }
            // Since 2.31.0 whoever walks the map is a round token cut from their portrait's face: the
            // class's for the hero, the monster's own or its form's for a monster, ringed by what it is.
            // Since 2.32.0 a monster is drawn only where the hero's light reaches - или сквозь туман по Предначертанию (4.6.0).
            world.agents.filter { it.alive && world.shows(it) }.forEach { agent ->
                // Жетон кольца очага Скверны «выпрыгивает» по очереди (4.0.0); ещё не вставший не виден
                val pop = blight?.age(agent.id)?.let { (age, n) -> blightPop(age, n) } ?: 1f
                if (pop <= 0f) return@forEach
                val mark = blight.decorOf(agent.id)
                standing += (agent.x + agent.y) to {
                    val monster = agent.monster
                    // A rarer monster is a bigger one: the tier is read before the ring is noticed.
                    val size = pop * unit * when (monster.rarity) {
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
                    token(isoX(agent.x, agent.y), isoY(agent.x, agent.y), size, ring, bob, mark) {
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
            // Простой у трещины (4.9.0, макет «Ломание трещины», вариант B): кольцо вокруг жетона героя, пока трещина поддаётся
            val crack = world.features.firstNotNullOfOrNull { it.cue(world) as? EntranceCue.Crack }?.progress?.takeIf { it > 0f }
            standing += heroDepth to {
                val bob = if (world.moving) abs(sin(time * 9f)) * unit * .14f else 0f
                // След скорости (4.10.0): босс карты пал - за идущим героем тянутся штрихи; декор, без `LocalMotion` его нет
                if (run.swift && world.moving && motion) swiftTrail(isoX(world.heroX, world.heroY), isoY(world.heroX, world.heroY) + unit * HERO_TOKEN * TOKEN_MID + bob, world.facingX, world.facingY)
                // Ореол ранящих недугов (макет «Эффекты героя на карте», вариант C): яд, горение, кровотечение - кольцо их цвета; декор
                ailmentHalo(Offset(isoX(world.heroX, world.heroY), -isoY(world.heroX, world.heroY) - unit * HERO_TOKEN * TOKEN_MID - bob), run.hud.value.heroAilments)
                token(isoX(world.heroX, world.heroY), isoY(world.heroX, world.heroY), unit * HERO_TOKEN, Palettes.hero, bob) {
                    Portraits.hero(this, classCode, time)
                }
                crack?.let { crackRing(Offset(isoX(world.heroX, world.heroY), -isoY(world.heroX, world.heroY) - unit * HERO_TOKEN * TOKEN_MID - bob), it) }
            }
            // По глубине, устойчиво и на месте (4.9.0): очередь одна на сцену, без копии на кадр
            standing.sortBy { it.first }
            for (i in standing.indices) standing[i].second()
            standing.clear()
        }
        // The air over everything (2.64.0): fog, sparks or fireflies, by the biome's style - на часах декора (4.2.0).
        style.atmosphere(scope, palette, decor)
    }

    /**
     * След скорости за героем с серединой жетона ([x], [y] в мере пера), идущим по [facingX], [facingY] клеток: три штриха назад,
     * мерцают на часах декора.
     */
    private fun swiftTrail(x: Float, y: Float, facingX: Double, facingY: Double) {
        // Направление назад на экране: изометрия клетки, как у [isoX] и [isoY]
        val backX = -(facingX - facingY).toFloat()
        val backY = (facingX + facingY).toFloat() / 2
        val length = hypot(backX, backY).takeIf { it > 0f } ?: return
        val dx = backX / length * unit
        val dy = backY / length * unit
        for (i in 0 until SWIFT_STREAKS) {
            val side = (i - (SWIFT_STREAKS - 1) / 2f) * unit * .22f
            val flicker = .5f + .5f * sin(decor * 14f + i * 2.1f)
            pen.color = SwiftHue.copy(alpha = .22f + .3f * flicker)
            val sx = x - dy / unit * side + dx * .5f
            val sy = y + dx / unit * side + dy * .5f
            pen.line(sx, sy, sx + dx * (1.1f + .4f * flicker), sy + dy * (1.1f + .4f * flicker), unit * .05f)
        }
    }

    /**
     * Ореол ранящих недугов [ailments] вокруг жетона героя с серединой [centre] в мере холста: по кольцу на недуг, его цветом
     * ([ailmentTint]), мягкое свечение снаружи. Декор: дышит на часах декора, без `LocalMotion` стоит в покое.
     */
    private fun ailmentHalo(centre: Offset, ailments: List<AilmentView>) {
        var ring = 0
        for (view in ailments) {
            if (!view.ailment.hurts) continue
            val tint = ailmentTint(view.ailment)
            val breath = if (motion) .5f + .5f * sin(decor * HALO_BREATH + ring) else .5f
            val radius = unit * HERO_TOKEN * (HALO_SPAN + ring * HALO_STEP)
            pen.scope.drawCircle(tint.copy(alpha = .14f + .1f * breath), radius * 1.18f, centre)
            val stroke = haloStroke?.takeIf { it.width == unit * HALO_WIDTH } ?: Stroke(unit * HALO_WIDTH).also { haloStroke = it }
            pen.scope.drawCircle(tint.copy(alpha = .45f + .3f * breath), radius, centre, style = stroke)
            ring++
        }
    }

    /** Кольцо простоя [progress] (0..1) вокруг жетона героя с серединой [centre] в мере холста: дорожка и дуга от верха по часовой. */
    private fun crackRing(centre: Offset, progress: Float) {
        val stroke = ringStroke?.takeIf { it.width == unit * RING_WIDTH } ?: Stroke(unit * RING_WIDTH, cap = StrokeCap.Round).also { ringStroke = it }
        val radius = unit * HERO_TOKEN * RING_SPAN
        val corner = Offset(centre.x - radius, centre.y - radius)
        val size = Size(radius * 2, radius * 2)
        pen.scope.drawArc(CrackTrack, 0f, 360f, false, corner, size, style = stroke)
        pen.scope.drawArc(CrackHue, -90f, 360f * progress.coerceIn(0f, 1f), false, corner, size, style = stroke)
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
     * «Окно» в скале вокруг точки карты ([x], [y]) (4.2.0): скала перед ней по ходу камеры прозрачна до [WINDOW_ALPHA] в центре
     * и плавно плотнеет к краю круга в [WINDOW_CELLS] клеток на экране; скала за точкой не трогается.
     */
    private inner class Window(val x: Double, val y: Double) {
        fun alpha(cx: Int, cy: Int): Float {
            if (cx + cy + 1.0 <= x + y) return 1f
            // Расстояние на экране: середина скалы по высоте против середины жетона
            val dx = isoX(cx + .5, cy + .5) - isoX(x, y)
            val dy = isoY(cx + .5, cy + .5) + unit * WALL_MID - (isoY(x, y) + unit * TOKEN_MID)
            val d = (hypot(dx, dy) / (unit * WINDOW_CELLS * CELL_STEP)).coerceIn(0f, 1f)
            val eased = d * d * (3f - 2f * d)
            return WINDOW_ALPHA + (1f - WINDOW_ALPHA) * eased
        }
    }

    /**
     * A token standing on its feet at ([x], [y]) in the pen's upward measure: a shadow on the floor
     * and the disc above it, lifted by [bob]. The pen turns `y` over; a token is drawn on the scope
     * itself, so it turns it over here. [decor] - метка жетона слоем под ним и над ним (4.3.0), на часах декора.
     */
    internal fun token(x: Float, y: Float, radius: Float, ring: Color, bob: Float, decor: TokenDecor? = null, draw: DrawScope.() -> Unit) {
        val scope = pen.scope
        val feet = -y
        val centre = Offset(x, feet - radius * TOKEN_MID - bob)
        val clock = this.decor
        scope.drawOval(Color.Black.copy(alpha = .45f), Offset(x - radius * .8f, feet - radius * .22f), Size(radius * 1.6f, radius * .44f))
        decor?.run { scope.under(centre, radius, clock) }
        scope.drawToken(centre, radius, ring, draw)
        decor?.run { scope.over(centre, radius, clock) }
    }

    /** A stable, faint unevenness per tile, so the ground does not read as a printed grid; [light] darkens it. */
    internal fun shade(base: Color, x: Int, y: Int, spread: Float = .08f, alpha: Float = 1f, light: Float = 1f): Color {
        val noise = ((x * 73856093) xor (y * 19349663)).let { (it and 0xFF) / 255f } - .5f
        return tone(base, (1f + noise * spread * 2f) * light, alpha = alpha)
    }

    internal fun diamond(cx: Float, cy: Float, halfWidth: Float, halfHeight: Float) = pen.quad(cx - halfWidth, cy, cx, cy + halfHeight, cx + halfWidth, cy, cx, cy - halfHeight)
}

/** Прозрачность скалы в центре «окна» (4.2.0; 4.7.0 - почти насквозь). */
private const val WINDOW_ALPHA = .08f

/** Радиус «окна» в клетках (4.2.0; 4.7.0 - шире: стены, закрывающие героя и ямы). */
private const val WINDOW_CELLS = 5f

/** Шаг клетки на экране в полуширинах клетки: сдвиг на клетку по x - полуширина вбок и четверть ширины вниз. */
private const val CELL_STEP = 1.118f

/** Середина скалы и жетона над землёй, в полуширинах клетки. */
private const val WALL_MID = .75f
private const val TOKEN_MID = 1.1f

/** Монстры, что гонятся за героем: вокруг них в скале тоже «окно» (4.2.0). */
private val ENGAGED = setOf(AgentMode.CHASING, AgentMode.HUNTING)

/** Жетон героя: радиус в полуширинах клетки. */
private const val HERO_TOKEN = .85f

/** Кольцо простоя у трещины (4.9.0): радиус в радиусах жетона и толщина нити в полуширинах клетки. */
private const val RING_SPAN = 1.35f
private const val RING_WIDTH = .2f

/** Ореол недугов героя: первое кольцо в долях жетона, шаг следующих, толщина в мере сцены и темп дыхания. */
private const val HALO_SPAN = .78f
private const val HALO_STEP = .14f
private const val HALO_WIDTH = .09f
private const val HALO_BREATH = 3.2f

/** Цвета трещины (4.9.0, макет «Ломание трещины»): дорожка кольца, дуга простоя и подпись над героем. */
private val CrackTrack = Color(0xFF1F2C35)
private val CrackHue = Color(0xFFA26BFF)
internal val CrackText = Color(0xFFCDB6FF)

/** След скорости после босса (4.10.0): сколько штрихов и их цвет - руна, как значок «быстрее» над картой. */
private const val SWIFT_STREAKS = 3
internal val SwiftHue = Color(0xFF8FD3FF)

/** Есть ли среди клетки и восьми её соседей ([dx], [dy] в -1..1) такая, что [test] (4.9.0: без диапазонов на клетку кадра). */
private inline fun nearby(test: (dx: Int, dy: Int) -> Boolean): Boolean {
    for (dy in -1..1) for (dx in -1..1) if (test(dx, dy)) return true
    return false
}
