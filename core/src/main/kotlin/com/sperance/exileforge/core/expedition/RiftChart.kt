package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.rules.content.RiftMetal
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.RiftReward
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftNode
import com.sperance.exileforge.rules.rift.RiftRun
import com.sperance.exileforge.rules.rift.RiftStage

/** Почему на узел не шагнуть: [key] - ключ словаря причины. */
enum class RiftBar(val key: String) {
    /** До забега - сперва войти. */
    NO_RUN("rift.node_why.no_run"),

    /** Узел уже пройден. */
    WALKED("rift.node_why.walked"),

    /** Открыт выбор узла или идёт бой - сперва его закончить. */
    BUSY("rift.node_why.busy"),

    /** Дальше следующего ряда - дорога приведёт позже. */
    LATER("rift.node_why.later"),

    /** Тропа сюда осталась позади. */
    BEHIND("rift.node_why.behind"),
}

/**
 * Узел на карте забега: виден ли за обзором [seen], пройден ли [walked], стоит ли на нём герой [hero], можно ли на него шагнуть
 * сейчас [open] (забег ждёт шага, узел - следующий) и почему нельзя [bar] (null - можно); [passed] - узел не пройден, а его ряд не выше
 * ряда героя; [reward] - главная награда узла впереди ([RiftEngine.rewards]), в тумане и позади её нет.
 */
data class RiftSpot(
    val node: RiftNode,
    val seen: Boolean,
    val walked: Boolean,
    val hero: Boolean,
    val open: Boolean,
    val bar: RiftBar?,
    val passed: Boolean = false,
    val reward: RiftReward? = null,
) {
    val guardian: Boolean get() = node.kind == RiftNodeKind.GUARDIAN

    /** На карте узел есть: виден, а страж - и в тумане, силуэтом. */
    val shown: Boolean get() = seen || guardian

    /** Страж в тумане - только силуэт. */
    val silhouette: Boolean get() = guardian && !seen

    /** Ключ захода на видимом непройденном узле. */
    val key: RiftMetal? get() = node.key?.takeIf { seen && !walked }
}

/**
 * Тропа между видимыми узлами [from] (null - вход в Разлом) и [to]: пройдена ли она [walked], ведёт ли с места героя на доступный
 * узел [open] и осталась ли позади [behind] - конец не пройден, а его ряд не выше ряда героя.
 */
data class RiftTrail(val from: Int?, val to: Int, val walked: Boolean, val open: Boolean, val behind: Boolean = false)

/** Шаг героя по тропе к выбранному узлу [to] с узла [from] (null - со входа): идёт, пока сервер не ответит и шаг не дойдёт. */
data class RiftWalk(val from: Int?, val to: Int) {
    companion object {
        /** Сколько идёт шаг по тропе: доска с ответом ложится не раньше. */
        const val STEP_MS = 900
    }
}

/**
 * Карта забега Разлома правилами ([RiftEngine]): узлы [spots] по номеру узла схемы, тропы между видимыми узлами и от входа [trails],
 * узел героя [here] и край обзора [edge] - глубина последнего видимого ряда. Что можно выбрать и почему нельзя - здесь; экран только
 * рисует и шлёт выбор.
 */
class RiftChart private constructor(val spots: List<RiftSpot>, val trails: List<RiftTrail>, val here: RiftNode?, val edge: Int) {
    fun spot(id: Int): RiftSpot? = spots.getOrNull(id)?.takeIf { it.node.id == id }

    companion object {
        /** Карта схемы [engine] у забега [run]; без забега - схема недели от входа, шагнуть некуда. */
        fun of(engine: RiftEngine, run: RiftRun?): RiftChart {
            val plan = engine.plan
            val here = run?.at?.let(plan::node)
            val reach = here?.let(engine::depth) ?: -1
            val next = if (run != null && run.stage == RiftStage.MOVE) engine.next(run).toSet() else emptySet()
            val walked = run?.path.orEmpty().toSet()
            val spots = plan.nodes.map { node ->
                val depth = engine.depth(node)
                val bar = when {
                    run == null -> RiftBar.NO_RUN
                    node.id in walked -> RiftBar.WALKED
                    run.stage != RiftStage.MOVE -> RiftBar.BUSY
                    node.id in next -> null
                    depth > reach + 1 -> RiftBar.LATER
                    else -> RiftBar.BEHIND
                }
                val seen = engine.seen(run, node)
                val ahead = seen && node.id !in walked && depth > reach
                RiftSpot(
                    node,
                    seen,
                    walked = node.id in walked,
                    hero = node == here,
                    open = node.id in next,
                    bar = bar,
                    passed = node.id !in walked && depth <= reach,
                    reward = if (ahead) engine.rewards(node).firstOrNull() else null,
                )
            }

            fun trail(from: RiftNode?, to: RiftNode) = RiftTrail(
                from?.id,
                to.id,
                walked = (from == null || from.id in walked) && to.id in walked,
                open = from == here && to.id in next,
                behind = to.id !in walked && engine.depth(to) <= reach,
            )
            val entries = plan.entries.mapNotNull(plan::node).filter { spots[it.id].seen }.map { trail(null, it) }
            val links = plan.nodes.flatMap { node ->
                node.links.mapNotNull(plan::node).filter { spots[node.id].seen && spots[it.id].seen }.map { trail(node, it) }
            }
            return RiftChart(spots, entries + links, here, reach + engine.sight(run))
        }
    }
}
