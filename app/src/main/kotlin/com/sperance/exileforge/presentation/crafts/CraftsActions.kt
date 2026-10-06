package com.sperance.exileforge.presentation.crafts

import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.crafts.Harvest
import com.sperance.exileforge.core.crafts.plus
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.rules.roll.WorkGains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ремёсла. Работа идёт на сервере по времени, кости цикла - у него (сервер 1.53.0): полоса цикла идёт здесь по кругу,
 * а на каждой границе цикла сервер тихо спрашивается заново (3.90.0), и его сбор ложится в сумку. Действия общие для
 * экрана, прогрева и возврата.
 */
class CraftsActions(
    private val repository: CraftsRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val events: GameEvents,
    private val content: ContentLoader,
    private val buzzes: Buzzes,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api
    private val crafts: Crafts get() = repository.state.value

    private fun crafts(transform: (Crafts) -> Crafts) = repository.update(transform)

    fun load(silent: Boolean = false) = commands.read(Reads.CRAFTS, silent = silent) {
        val id = heroes.heroId
        if (id.isBlank()) return@read
        content.ensure()
        land(id, api.crafts.state(id))
    }

    private var cycle: Job? = null

    /** Граница цикла на часах сервера, под которую уже стоит пересчёт (3.90.0): один запрос на границу. */
    private var armedAt = 0L

    /**
     * Будильник границы цикла живёт здесь: цикл кончается на любой вкладке и во время похода. На каждой границе (3.90.0) -
     * один тихий пересчёт с сервера, без спиннера; граница считается по кругу от `settledAt` работы, так что ответ,
     * ещё не сдвинувший работу, ставит будильник на следующую границу, а не повторяет запрос.
     */
    private fun armCycle() {
        val held = crafts
        val work = held.state?.work?.takeIf { it.cycleMillis > 0 }
        if (work == null) {
            cycle?.cancel()
            cycle = null
            armedAt = 0L
            return
        }
        val boundary = work.nextBoundary(System.currentTimeMillis() + held.offset)
        if (boundary == armedAt && cycle?.isActive == true) return
        cycle?.cancel()
        armedAt = boundary
        cycle = scope.launch {
            // Сервер считает цикл по своим часам: пересчёт - чуть позже границы, чтобы он её уже прошёл.
            delay((boundary - held.offset - System.currentTimeMillis()).coerceAtLeast(0L) + SETTLE_GRACE)
            cycle = null
            load(silent = true)
            armCycle()
        }
    }

    /** Герой уходит с экрана: будильник снят, ремёсла забыты. */
    fun drop() {
        cycle?.cancel()
        cycle = null
        armedAt = 0L
        repository.clear()
    }

    fun start(job: String, choice: String = "", additives: List<String> = emptyList()) {
        buzzes.buzz(Buzz.BUTTON)
        // Another work begins (3.81.0): «За сессию» counts it from zero, not on top of the one it replaces.
        val running = crafts.state?.work
        if (running == null || running.job != job || running.choice != choice) crafts { it.copy(totals = WorkGains()) }
        commands.task(writing = true, touches = setOf(Reads.CRAFTS)) {
            val id = heroes.heroId
            val before = heroes.version
            land(id, api.crafts.start(id, job, choice, additives), heroes.version != before)
        }
    }

    fun stop() = commands.task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = heroes.heroId
        val before = heroes.version
        land(id, api.crafts.stop(id), heroes.version != before)
    }

    /** Инструмент в слот профессии: экипировка сервером, и ремёсла прочитаны заново ради новых чисел. */
    fun equipTool(itemId: String) = commands.task(writing = true, touches = setOf(Reads.CRAFTS, Reads.HERO)) {
        val id = heroes.heroId
        api.hero.equip(id, itemId, null)
        land(id, api.crafts.state(id))
    }

    /**
     * Ответ сервера: работа как он её пересчитал, его сбор с прошлого ответа - в сумку (если сумку не прислал сам сервер)
     * и в итог сеанса; сбор с циклами - [Crafts.last] для всплывашки у полосы цикла (3.90.0).
     */
    private fun land(id: String, answer: CraftsState, bagFromServer: Boolean = false) {
        if (!heroes.onScreen(id)) return
        val gains = answer.gains
        val stacks = gains.items.isNotEmpty() || gains.spent.isNotEmpty()
        if (!bagFromServer) heroes.patch { it.copy(bag = patched(it.bag, gains)) }
        crafts { c ->
            c.copy(
                state = answer,
                readAt = System.currentTimeMillis(),
                totals = c.totals + gains,
                last = if (gains.cycles > 0) Harvest(gains, (c.last?.seq ?: 0L) + 1) else c.last,
            )
        }
        // Снаряжение или стопки: герой перечитывается, сумка и сундук берутся с сервера.
        if (!bagFromServer && (gains.equipment.isNotEmpty() || stacks)) events.heroChanged()
        armCycle()
    }

    /** Сумка с уплаченными стопками итога и вычтенной тратой, по кодам. */
    private fun patched(bag: Map<String, Long>, gains: WorkGains): Map<String, Long> {
        if (gains.items.isEmpty() && gains.spent.isEmpty()) return bag
        val have = bag.toMutableMap()
        gains.items.forEach { (code, amount) -> have.merge(code, amount, Long::plus) }
        gains.spent.forEach { (code, amount) -> have.merge(code, -amount, Long::plus) }
        return have.filterValues { it > 0 }
    }

    private companion object {
        const val SETTLE_GRACE = 600L
    }
}
