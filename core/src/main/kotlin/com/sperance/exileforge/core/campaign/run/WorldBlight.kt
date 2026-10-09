package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.campaign.Cell
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Run
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// ==================== Скверна (4.0.0, сервер: MapFeature.Blight) ====================

/** Что Скверна только что сделала - для строки над картой (4.0.0). */
enum class BlightNews {
    /** Герой наступил на очаг: вокруг встали монстры Скверны. */
    AWAKE,

    /** Сундук открыт, и Скверна сдвинулась к следующей точке. */
    SPREAD,

    /** Сундук открыт, а дальше Скверне некуда: очаг иссяк. */
    END,

    /** Сундук последней точки открыт - пробуждается Матерь Скверны. */
    MOTHER,
}

/**
 * Очаг Скверны на карте (4.0.0) как его видит полоса карты: точка [point] (с нуля; Матерь - номер [points]) из не больше чем
 * [limit] точек правила; [raised] - её монстры уже встали, [left] из них ещё стоят; [chest] - качество сундука, что ждёт на
 * зачищенной точке (null - сундука нет); [news] - последнее, что сделала Скверна, под своим номером [told]: полоса объявляет
 * новость, только когда номер сменился. [over] - очаг исчерпан; [seen] - герой уже видел раскрытую точку.
 */
data class BlightView(
    val point: Int,
    val points: Int,
    val limit: Int,
    val mother: Boolean,
    val raised: Boolean,
    val left: Int,
    val chest: Rarity?,
    val news: BlightNews?,
    val told: Int,
    val over: Boolean,
    val seen: Boolean,
)

/**
 * Очаг Скверны (4.0.0) - цепь точек [cells] по порядку: где каждая встанет, решило семя при расстановке, а видна точка, лишь
 * когда до неё дошла Скверна (сундук прежней открыт). Касание активной точки поднимает её жетоны кольцом вокруг неё
 * ([FeatureAction.Rouse]); когда пали все - на точке стоит сундук, касание открывает его выбором с номером точки. Сундук
 * последней точки будит Матерь (жетон точки [MapFeature.Blight.motherChoice]) на той же клетке, её сундук - после её смерти.
 * Всё восстанавливается из журнала объектов и павших жетонов ([resume]).
 */
class BlightSpot(feature: MapFeature.Blight, val cells: List<Cell>) : FeatureSpot(feature, cells.first()) {
    val blight: MapFeature.Blight get() = feature as MapFeature.Blight

    /** Номера точек, чьи жетоны уже встали. */
    private val raised = mutableSetOf<Int>()

    /** Часы очага, секунд шага по карте: от них считается, сколько стоит кольцо ([age]). */
    var clock = 0.0
        private set
    private val risenAt = HashMap<Int, Double>()

    /** Последняя новость Скверны и её номер: полоса объявляет только новые. */
    var news: BlightNews? = null
        private set
    var told = 0
        private set

    /** Точка, чей ход сейчас: первая без сундука; null - очаг исчерпан. Матерь - номер [MapFeature.Blight.motherChoice]. */
    val active: Int? get() = (0 until blight.choices).firstOrNull { it !in taken }

    /** Точек, до которых уже дошла Скверна: активная и все прежние. */
    val revealed: Int get() = ((active ?: (blight.choices - 1)) + 1).coerceAtMost(blight.points)

    /** Клетка точки [point]: Матерь встаёт на последней. */
    fun at(point: Int): Cell = cells[point.coerceIn(0, cells.lastIndex)]

    /** Жетоны точки [point] уже встали. */
    fun raised(point: Int): Boolean = point in raised

    /** Сколько секунд стоит кольцо жетона [token] и его номер в кольце; null - жетон не очага или ещё не встал. */
    fun age(token: Int): Pair<Double, Int>? {
        val (point, n) = blight.slot(token) ?: return null
        val at = risenAt[point] ?: return null
        return clock - at to n
    }

    /** Пали все жетоны точки [point] на карте [world]. */
    fun cleared(world: ExpeditionWorld, point: Int): Boolean = raised(point) && blight.tokens(point).all(world::fell)

    /** Сколько жетонов точки [point] ещё стоит. */
    fun left(world: ExpeditionWorld, point: Int): Int = if (raised(point)) blight.tokens(point).count { !world.fell(it) } else blight.count(point)

    /** Видна точка, что Скверна уже раскрыла и герой видел. */
    override val sites: List<Cell> get() = (0 until revealed).map(::at)

    override val landmarks: List<Cell> get() = cells.take(revealed)

    override val focus: Cell get() = at(active ?: (blight.choices - 1))

    override fun touch(world: ExpeditionWorld, dt: Double): FeatureAction? {
        clock += dt
        val point = active ?: return null
        val cell = at(point)
        return when {
            // Матерь будит сундук последней точки, а не шаг героя
            !raised(point) -> if (point < blight.points && near(world, cell, world.rules.chestReach)) FeatureAction.Rouse(this, point) else null

            cleared(world, point) && near(world, cell, world.rules.chestReach) -> FeatureAction.Trigger(this, point)

            else -> null
        }
    }

    /** Герой наступил на точку [group]: её жетоны встают, Скверна пробудилась; Матерь будит сундук последней точки. */
    override fun rouse(run: ExpeditionRun, group: Int) {
        if (raised(group)) return
        raise(run, group, clock)
        tell(if (group == blight.motherChoice) BlightNews.MOTHER else BlightNews.AWAKE)
    }

    /** Жетоны точки [point] встают кольцом вокруг неё (Матерь - одна, на самой точке), кольцо стоит с [since]. */
    private fun raise(run: ExpeditionRun, point: Int, since: Double) {
        raised += point
        risenAt[point] = since
        val buffs = MapEffects.buffs(run.baseEffects)
        val tokens = blight.tokens(point)
        val places = if (point == blight.motherChoice) listOf(at(point)) else run.world.ring(at(point), tokens.size)
        tokens.zip(places).forEach { (token, cell) -> run.world.summonAt(token, run.spawns.summoned(token, buffs), cell) }
    }

    /** Сундук точки открыт: добыча - панелью сундука; затем Скверна расползается, будит Матерь или иссякает. */
    override fun resolve(run: ExpeditionRun, choice: Int) {
        run.showLoot(run.recordFeature(this, choice))
        val next = active
        when {
            next == null -> tell(BlightNews.END)
            next == blight.motherChoice -> rouse(run, next)
            else -> tell(BlightNews.SPREAD)
        }
    }

    /**
     * Вход в заход снова: Матерь, если её черёд, встаёт сразу; активная точка - если из её жетонов кто-то уже пал (герой
     * начинал бой). Павших положит `ExpeditionWorld.restore` следом.
     */
    override fun resume(run: ExpeditionRun, killed: Collection<Int>) {
        val point = active ?: return
        val started = point == blight.motherChoice || blight.tokens(point).any { it * Run.PACK_SLOTS in killed }
        if (started) raise(run, point, clock - RESTORED)
    }

    private fun tell(what: BlightNews) {
        news = what
        told++
    }

    /** Очаг для полосы карты. */
    fun view(world: ExpeditionWorld): BlightView {
        val point = active
        val shownPoint = point ?: (blight.choices - 1)
        val raised = point != null && raised(point)
        val chest = point?.takeIf { cleared(world, it) }?.let(blight::quality)
        return BlightView(shownPoint, blight.points, blight.rule.points.size, blight.mother, raised, point?.let { left(world, it) } ?: 0, chest, news, told, point == null, shown(world))
    }

    private companion object {
        /** Кольцо, восстановленное при входе, уже стоит: «выпрыгивать» ему незачем. */
        const val RESTORED = 1e6
    }
}

/**
 * Клетки точек очага Скверны (4.0.0): первая - из [pool] вдали от прочих объектов, каждая следующая - на пути в
 * `chestSteps`..2×`chestSteps` шагов от прежней (иначе - дальняя доступная), все на полу и врозь. Броски - [random] расстановки.
 */
internal fun ExpeditionWorld.blightCells(points: Int, first: Cell, pool: List<Cell>, taken: MutableSet<Cell>, random: Random): List<Cell> {
    val cells = mutableListOf(first)
    val roomy = pool.toSet()
    repeat(points - 1) {
        val from = cells.last()
        val reach = distances(from, rules.chestSteps * 2).filterKeys { it !in taken && it in roomy }
        val near = reach.filterValues { it >= rules.chestSteps }.keys.shuffled(random)
        val cell = near.firstOrNull() ?: reach.maxByOrNull { it.value }?.key ?: from
        taken += cell
        cells += cell
    }
    return cells
}

/**
 * Кольцо из [count] клеток вокруг [center] (4.0.0): по кругу радиуса [RING_RADIUS] клеток, каждая - ближайший к своему месту
 * на круге пол, куда от центра можно дойти, сам центр и занятое кольцом - мимо.
 */
internal fun ExpeditionWorld.ring(center: Cell, count: Int): List<Cell> {
    val free = distances(center, RING_STEPS).keys.filterTo(HashSet()) { it != center && map.walkable(it.x, it.y) }
    return List(count) { k ->
        val angle = 2 * PI * k / count - PI / 2
        val x = center.x + .5 + cos(angle) * RING_RADIUS
        val y = center.y + .5 + sin(angle) * RING_RADIUS
        free.minByOrNull { hypot(it.x + .5 - x, it.y + .5 - y) }?.also(free::remove) ?: center
    }
}

/** Радиус кольца жетонов очага, в клетках: за краем лужи очага. */
private const val RING_RADIUS = 2.5

/** Как далеко от точки ищется пол под кольцо, в шагах. */
private const val RING_STEPS = 5
