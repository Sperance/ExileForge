package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.run.AbyssSpot
import com.sperance.exileforge.core.campaign.run.AgentMode
import com.sperance.exileforge.core.campaign.run.AilmentView
import com.sperance.exileforge.core.campaign.run.BlightSpot
import com.sperance.exileforge.core.campaign.run.Chest
import com.sperance.exileforge.core.campaign.run.CrystalSpot
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.ExpeditionWorld
import com.sperance.exileforge.core.campaign.run.FeatureSpot
import com.sperance.exileforge.core.campaign.run.Fountain
import com.sperance.exileforge.core.campaign.run.IsoGrid
import com.sperance.exileforge.core.campaign.run.MonsterAgent
import com.sperance.exileforge.core.campaign.run.TrapSpot
import com.sperance.exileforge.core.campaign.run.sealed
import com.sperance.exileforge.core.campaign.run.waiting
import com.sperance.exileforge.core.expedition.TrapJointsCache
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.ui.icons.Strokes
import com.sperance.exileforge.ui.icons.drawToken
import com.sperance.exileforge.ui.screens.expedition.arena.ailmentTint
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** How bright a cell the hero saw once but does not see now is, against one in full light. */
internal const val REMEMBERED = .32f

/** Everything the scene draws, holding the one pen it draws with. */
internal class ScenePainter {
    internal val pen = Pen()
    internal var time = 0f

    /** Half a tile's width on screen; the tile is twice as wide as it is tall. */
    internal var unit = 30f

    /** The hero's class, whose portrait is the hero's token. */
    internal var classCode: String? = null

    /** Босс этого захода запечатан. */
    private var sealed = false

    /** Анимации-декор включены (`LocalMotion`): без них декор стоит в покое ([decor]). */
    internal var motion = true

    /** Художник узлов сбора: контуры обликов строятся раз на сцену. */
    internal val nodes = NodeGlyphs()

    /** Значки материалов узлов ремесла по коду материала, их готовит сцена. */
    internal var materials: Map<String, MaterialMark> = emptyMap()

    /** Слои земли и скалы карты: их даёт сцена ([ExpeditionScene]). */
    internal val layers = MapLayers()

    /** Часы декора: при выключенных анимациях стоят на нуле. */
    internal val decor: Float get() = if (motion) time else 0f

    /** Массивы скалы и контуры вод карты: считаются раз на карту и её правку. */
    private var relief: MapRelief? = null

    /** Контуры стен карты: раз на карту, её правку, стиль и масштаб. */
    private var walls: WallRelief? = null

    /** Пещера боя: её контуры и неподвижные кисти живут со сценой, а не собираются на кадр. */
    private val stage = FightStage()

    /** Очередь стоящего (скала, жетоны, ориентиры) по глубине: одна на сцену, без новых записей на кадр. */
    private val standing = Standing()

    /** Перо кадра: одно на сцену, часы - декора. */
    private val frame = SceneFrame(pen, unit, 0f)

    /** Соседство клеток ловушек карты: считается раз на ловушку ([TrapJoints]). */
    internal val trapJoints = TrapJointsCache()

    /** Свет карты по клеткам: середина клетки героя и радиус факела - без мерцания, меняется со сменой клетки. */
    private var world: ExpeditionWorld? = null
    private var lightX = 0.0
    private var lightY = 0.0
    private var lightRadius = 1.0

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

    // Изометрия ядра: художник - та же матрица, что камера и стик; y здесь растёт вверх
    internal fun isoX(x: Double, y: Double) = (IsoGrid.across(x, y) * unit).toFloat()
    internal fun isoY(x: Double, y: Double) = (-IsoGrid.down(x, y) * unit).toFloat()

    /** Высота стены у клетки в клетках сцены; до первой раскладки стен - обычная. */
    internal fun wallRise(x: Int, y: Int): Double = (walls?.height(x, y) ?: PLAIN_WALL).toDouble()

    /**
     * Как освещена клетка: полностью у героя, к краю света тускнеет, увиденное прежде - тускло; свет считается от
     * середины клетки героя без мерцания факела, так что земля и скала перезаписываются лишь со сменой клетки.
     */
    internal fun glow(x: Int, y: Int): Float {
        val world = world ?: return 1f
        if (!world.lit(x, y)) return REMEMBERED
        val d = (hypot(x + .5 - lightX, y + .5 - lightY) / lightRadius).toFloat().coerceIn(0f, 1f)
        return 1f - (1f - REMEMBERED) * d * d
    }

    /** Исследована ли клетка: до первой карты - да. */
    internal fun explored(x: Int, y: Int): Boolean = world?.explored(x, y) ?: true

    /** Свет скалы: глубокая клетка светится, как самый светлый сосед, - крышка массива не темнеет пятном. */
    internal fun rockGlow(rock: WallRelief, x: Int, y: Int): Float {
        if (!rock.deep(x, y)) return glow(x, y)
        var best = Float.NEGATIVE_INFINITY
        for (dy in -1..1) for (dx in -1..1) best = max(best, glow(x + dx, y + dy))
        return best
    }

    /**
     * Узор вод и пропастей - декор, без `LocalMotion` стоит. Герой чуть ниже середины: впереди больше карты, чем позади. Земля и скала -
     * слоями, гладь течёт вживую поверх пола. Всё стоящее - сзади вперёд по x + y; выход и портал Ваал стоят по глубине, как всё стоящее.
     * Объекты появляются, когда увидены; плиты ловушек лежат на полу под всем стоящим. Жетон - круг портрета; монстр виден лишь в свете героя или
     * сквозь туман по Предначертанию. Очередь глубины одна на сцену, без копии на кадр; воздух (туман, искры, светлячки) - на часах декора.
     */
    internal fun map(scope: DrawScope, run: ExpeditionRun, palette: Palette) {
        val world = run.world
        val map = world.map
        val biome = run.zone.biome
        val style = MapStyles.of(biome)
        val shape = MapRelief.of(map, world.shoreline, relief).also { relief = it }
        val rock = WallRelief.of(map, world.walls, world.grain, shape, style, palette, unit, walls).also { walls = it }
        frame.unit = unit
        frame.time = decor
        val width = scope.size.width
        val height = scope.size.height
        val cameraX = isoX(world.heroX, world.heroY)
        val cameraY = isoY(world.heroX, world.heroY) + unit
        val hx = floor(world.heroX).toInt()
        val hy = floor(world.heroY).toInt()
        this.world = world
        lightX = hx + .5
        lightY = hy + .5
        lightRadius = world.lightRadius
        layers.refresh(this, scope, world, shape, rock, style, palette, biome, hx, hy)
        scope.translate(width / 2 - cameraX, height * SceneCamera.HERO_DOWN + cameraY) {
            layers.drawUnder(this)
            layers.drawPools(this@ScenePainter, frame, shape, style, palette, map.liquid)
            layers.drawOver(this)
            val hxs = isoX(world.heroX, world.heroY)
            val hys = -isoY(world.heroX, world.heroY)
            val warm = (world.lightRadius * unit * 1.414).toFloat()
            withTransform({
                scale(1f, .5f, Offset(hxs, hys))
                translate(hxs, hys)
                scale(warm, warm, Offset.Zero)
            }) { drawCircle(TorchWarmth, 1f, Offset.Zero) }

            for (spot in world.features) if (spot is TrapSpot && spot.shown(world)) drawTrapFloor(spot, world.clock)
            standing.clear()
            for (i in 0 until layers.wallCount) standing.add(layers.wallDepth(i), Standing.WALLS, null, i)
            if (world.explored(map.exit.x, map.exit.y)) standing.add(map.exit.x + map.exit.y + 1.0, Standing.EXIT)
            world.portal?.takeIf { world.explored(it.x, it.y) }?.let { at -> standing.add(at.x + at.y + 1.0, Standing.VAAL_PORTAL, at) }
            for (fountain in world.fountains) if (world.explored(fountain.cell.x, fountain.cell.y)) standing.add(fountain.cell.x + fountain.cell.y + 1.0, Standing.FOUNTAIN, fountain)
            for (chest in world.chests) if (world.shows(chest)) standing.add(chest.cell.x + chest.cell.y + 1.0, Standing.CHEST, chest)
            for (spot in world.crystals) if (world.explored(spot.cell.x, spot.cell.y)) standing.add(spot.cell.x + spot.cell.y + 1.0, Standing.CRYSTAL, spot)
            for (spot in world.cracks) if (world.explored(spot.cell.x, spot.cell.y)) standing.add(spot.cell.x + spot.cell.y + .5, Standing.CRACK, spot)
            var blight: BlightSpot? = null
            for (spot in world.features) {
                if (spot is BlightSpot && blight == null) blight = spot
                if (spot.shown(world)) standFeature(standing, spot, world)
            }
            for (agent in world.agents) if (agent.alive && world.shows(agent)) standing.add(agent.x + agent.y, Standing.AGENT, agent)
            standing.add(world.heroX + world.heroY, Standing.HERO)
            standing.sort()
            for (i in 0 until standing.size) stand(this, run, world, blight, i)
            standing.clear()
        }
        style.atmosphere(scope, palette, decor)
    }

    /**
     * Запись [i] очереди стоящего.
     *
     * Открытый сундук без ответа сервера приоткрыт и ждёт. Узел, что герой собирает, стоит на кольце доли сбора ([drawGather]): узел и доля - из одного кадра пульса.
     */
    private fun stand(scope: DrawScope, run: ExpeditionRun, world: ExpeditionWorld, blight: BlightSpot?, i: Int) {
        val ref = standing.ref(i)
        when (standing.kind(i)) {
            Standing.WALLS -> layers.drawWalls(scope, standing.part(i))

            Standing.EXIT -> portal(world.map.exit.x + .5, world.map.exit.y + .5, world.sealed)

            Standing.VAAL_PORTAL -> (ref as Cell).let { vaalPortal(it.x + .5, it.y + .5, glow(it.x, it.y)) }

            Standing.FOUNTAIN -> (ref as Fountain).let { drawFountain(it.cell.x + .5, it.cell.y + .5, it.used, glow(it.cell.x, it.cell.y)) }

            Standing.CHEST -> (ref as Chest).let { drawChest(it.cell.x + .5, it.cell.y + .5, it.opened, glow(it.cell.x, it.cell.y), run.waiting(it)) }

            Standing.CRYSTAL -> (ref as CrystalSpot).let { drawCrystal(it.cell.x + .5, it.cell.y + .5, it.freed, it.crystal.stronger, glow(it.cell.x, it.cell.y)) }

            Standing.CRACK -> (ref as AbyssSpot).let { drawCrack(it.cell.x + .5, it.cell.y + .5, it.opened, it.id, glow(it.cell.x, it.cell.y)) }

            Standing.FEATURE -> (ref as FeatureSpot).let { spot ->
                if (spot === run.pulse.gathering) run.pulse.action?.let { drawGather(spot.cell.x + .5, spot.cell.y + .5, it, glow(spot.cell.x, spot.cell.y)) }
                drawFeature(spot, standing.part(i), world)
            }

            Standing.AGENT -> agent(ref as MonsterAgent, world, blight)

            else -> hero(run, world)
        }
    }

    /**
     * Жетон монстра [agent]: размер по редкости, качание по повадке, метка очага Скверны, печать стража.
     *
     * Жетон кольца очага Скверны «выпрыгивает» по очереди. Редкий монстр - крупнее; спящий сидит, засадный низко, охотник покачивается
     * быстрее. Запечатанный босс - под фиолетовой пеленой.
     */
    private fun agent(agent: MonsterAgent, world: ExpeditionWorld, blight: BlightSpot?) {
        val pop = blight?.age(agent.id)?.let { (age, n) -> blightPop(age, n) } ?: 1f
        if (pop <= 0f) return
        val mark = blight.decorOf(agent.id)
        val monster = agent.monster
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
        val bob = when (agent.mode) {
            AgentMode.ASLEEP, AgentMode.LURKING -> 0f
            AgentMode.CHASING, AgentMode.HUNTING -> abs(sin(time * 8f + agent.id)) * unit * .12f
            else -> abs(sin(time * 3f + agent.id)) * unit * .08f
        }
        val boss = sealed && agent === world.boss
        token(isoX(agent.x, agent.y), isoY(agent.x, agent.y), size, ring, bob, mark) {
            Portraits.monster(this, monster.code.value, monster.form, time)
            if (agent.mode == AgentMode.ASLEEP) drawRect(Asleep)
            if (boss) {
                drawRect(SealVeil.copy(alpha = .55f + .1f * sin(time * 2f)))
                drawArc(SealArc, time * 90f, 270f, false, style = Strokes.of(drawContext.size.minDimension * .06f))
            }
        }
    }

    /**
     * Жетон героя: след скорости, ореол недугов.
     *
     * След скорости и ореол ранящих недугов - декор.
     */
    private fun hero(run: ExpeditionRun, world: ExpeditionWorld) {
        val bob = if (world.moving) abs(sin(time * 9f)) * unit * .14f else 0f
        val x = isoX(world.heroX, world.heroY)
        val y = isoY(world.heroX, world.heroY)
        if (run.hastened && world.moving && motion) swiftTrail(x, y + unit * HERO_TOKEN * TOKEN_MID + bob, world.facingX, world.facingY)
        val centre = Offset(x, -y - unit * HERO_TOKEN * TOKEN_MID - bob)
        ailmentHalo(centre, run.hud.value.heroAilments)
        val code = classCode
        token(x, y, unit * HERO_TOKEN, Palettes.hero, bob) { Portraits.hero(this, code, time) }
    }

    /**
     * След скорости за героем с серединой жетона ([x], [y] в мере пера), идущим по [facingX], [facingY] клеток: три штриха назад,
     * мерцают на часах декора.
     */
    private fun swiftTrail(x: Float, y: Float, facingX: Double, facingY: Double) {
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
     * Ореол ранящих недугов и холода [ailments] вокруг жетона героя с серединой [centre] в мере холста: по кольцу на недуг,
     * его цветом ([ailmentTint]), мягкое свечение снаружи; у холода под жетоном - подпись «−X%» шага (`ChillMark`). Декор:
     * дышит на часах декора, без `LocalMotion` стоит в покое.
     */
    private fun ailmentHalo(centre: Offset, ailments: List<AilmentView>) {
        var ring = 0
        for (view in ailments) {
            if (!view.ailment.hurts && view.ailment != Ailment.CHILLED) continue
            val tint = ailmentTint(view.ailment)
            val breath = if (motion) .5f + .5f * sin(decor * HALO_BREATH + ring) else .5f
            val radius = unit * HERO_TOKEN * (HALO_SPAN + ring * HALO_STEP)
            pen.scope.drawCircle(tint.copy(alpha = .14f + .1f * breath), radius * 1.18f, centre)
            pen.scope.drawCircle(tint.copy(alpha = .45f + .3f * breath), radius, centre, style = Strokes.of(unit * HALO_WIDTH))
            ring++
        }
    }

    /** A cell as the style reads it: where its centre falls and how much rock borders it. */
    internal fun spot(map: ExpeditionMap, x: Int, y: Int) = TileSpot(
        x,
        y,
        isoX(x + .5, y + .5),
        isoY(x + .5, y + .5),
        (if (map.walkable(x + 1, y)) 0 else 1) + (if (map.walkable(x - 1, y)) 0 else 1) + (if (map.walkable(x, y + 1)) 0 else 1) + (if (map.walkable(x, y - 1)) 0 else 1),
    )

    /**
     * «Окно» в скале вокруг точки карты ([x], [y]): скала перед ней по ходу камеры прозрачна до [WINDOW_ALPHA] в центре
     * и плавно плотнеет к краю круга в [WINDOW_CELLS] клеток на экране; скала за точкой не трогается.
     */
    internal inner class Window(val x: Double, val y: Double) {
        fun alpha(cx: Int, cy: Int): Float {
            if (cx + cy + 1.0 <= x + y) return 1f
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
     * itself, so it turns it over here. [decor] - метка жетона слоем под ним и над ним, на часах декора.
     */
    internal inline fun token(x: Float, y: Float, radius: Float, ring: Color, bob: Float, decor: TokenDecor? = null, draw: DrawScope.() -> Unit) {
        val scope = pen.scope
        val feet = -y
        val centre = Offset(x, feet - radius * TOKEN_MID - bob)
        val clock = this.decor
        scope.drawOval(TokenShadow, Offset(x - radius * .8f, feet - radius * .22f), Size(radius * 1.6f, radius * .44f))
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

/** Прозрачность скалы в центре «окна» (почти насквозь). */
private const val WINDOW_ALPHA = .08f

/** Радиус «окна» в клетках (шире: стены, закрывающие героя и ямы). */
private const val WINDOW_CELLS = 5f

/** Шаг клетки на экране в полуширинах клетки: сдвиг на клетку по x - полуширина вбок и четверть ширины вниз. */
private const val CELL_STEP = 1.118f

/** Середина скалы и жетона над землёй, в полуширинах клетки. */
private const val WALL_MID = .75f
internal const val TOKEN_MID = 1.1f

/** Тень жетона на полу. */
internal val TokenShadow = Color.Black.copy(alpha = .45f)

/** Пелена спящего монстра и печать запечатанного стража. */
private val Asleep = Color.Black.copy(alpha = .35f)
private val SealVeil = Color(0xFF2A1040)
private val SealArc = Color(0xFFB07CFF)

/** Тепло факела на земле: круг радиуса 1, растянутый на свет героя. */
private val TorchWarmth = Brush.radialGradient(listOf(Palettes.torch.copy(alpha = .16f), Palettes.torch.copy(alpha = .05f), Color.Transparent), Offset.Zero, 1f)

/** Монстры, что гонятся за героем: вокруг них в скале тоже «окно». */
internal val ENGAGED = setOf(AgentMode.CHASING, AgentMode.HUNTING)

/** Жетон героя: радиус в полуширинах клетки. */
private const val HERO_TOKEN = .85f

/** Ореол недугов героя: первое кольцо в долях жетона, шаг следующих, толщина в мере сцены и темп дыхания. */
private const val HALO_SPAN = .78f
private const val HALO_STEP = .14f
private const val HALO_WIDTH = .09f
private const val HALO_BREATH = 3.2f

/** След скорости после босса: сколько штрихов и их цвет - руна, как значок «быстрее» над картой. */
private const val SWIFT_STREAKS = 3
internal val SwiftHue = Color(0xFF8FD3FF)

/** Есть ли среди клетки и восьми её соседей ([dx], [dy] в -1..1) такая, что [test] (без диапазонов на клетку кадра). */
internal inline fun nearby(test: (dx: Int, dy: Int) -> Boolean): Boolean {
    for (dy in -1..1) for (dx in -1..1) if (test(dx, dy)) return true
    return false
}

/** Высота стены, пока раскладки стен ещё нет. */
private const val PLAIN_WALL = 1.5f

/**
 * Очередь стоящего на карте по глубине (скала, ориентиры, объекты, жетоны): запись - глубина, вид, ссылка и число, без лямбд и
 * пар на кадр. Массивы растут один раз и живут со сценой; порядок - устойчивая вставка: при равной глубине первым рисуется
 * поставленный раньше, как у `sortBy`.
 */
internal class Standing {
    var size = 0
        private set
    private var depths = DoubleArray(CAPACITY)
    private var kinds = IntArray(CAPACITY)
    private var parts = IntArray(CAPACITY)
    private var refs = arrayOfNulls<Any>(CAPACITY)

    /** Порядок рисования: номера записей по глубине. */
    private var order = IntArray(CAPACITY)

    fun add(depth: Double, kind: Int, ref: Any? = null, part: Int = 0) {
        if (size == depths.size) grow()
        depths[size] = depth
        kinds[size] = kind
        parts[size] = part
        refs[size] = ref
        order[size] = size
        size++
    }

    /** Порядок по глубине вставкой: очередь почти упорядочена (скала идёт по глубине), так что это почти линейный проход. */
    fun sort() {
        for (i in 1 until size) {
            val at = order[i]
            val depth = depths[at]
            var j = i - 1
            while (j >= 0 && depths[order[j]] > depth) {
                order[j + 1] = order[j]
                j--
            }
            order[j + 1] = at
        }
    }

    fun kind(i: Int): Int = kinds[order[i]]
    fun part(i: Int): Int = parts[order[i]]
    fun ref(i: Int): Any? = refs[order[i]]

    fun clear() {
        java.util.Arrays.fill(refs, 0, size, null)
        size = 0
    }

    private fun grow() {
        val next = depths.size * 2
        depths = depths.copyOf(next)
        kinds = kinds.copyOf(next)
        parts = parts.copyOf(next)
        refs = refs.copyOf(next)
        order = order.copyOf(next)
    }

    companion object {
        private const val CAPACITY = 256

        /** Кусок скалы - слой [MapLayers] номер `part`. */
        const val WALLS = 0
        const val EXIT = 1
        const val VAAL_PORTAL = 2
        const val FOUNTAIN = 3
        const val CHEST = 4
        const val CRYSTAL = 5
        const val CRACK = 6

        /** Объект карты: часть `part` ([standFeature]). */
        const val FEATURE = 7
        const val AGENT = 8
        const val HERO = 9
    }
}
