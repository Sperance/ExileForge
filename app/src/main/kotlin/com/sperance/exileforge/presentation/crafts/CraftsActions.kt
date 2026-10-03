package com.sperance.exileforge.presentation.crafts

import com.sperance.exileforge.core.crafts.CraftCycle
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.crafts.minus
import com.sperance.exileforge.core.crafts.plus
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.crafts.job
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
 * Ремёсла. Работа идёт на сервере по времени. Закончившийся цикл бросается здесь из семени и номера работы, его
 * стопки сразу ложатся в сумку, а сервер спрашивается в фоне: его ответ считает те же циклы, и что он сосчитал
 * иначе - или сверх, пока приложения не было, - правится тогда. Действия общие для экрана, прогрева и возврата.
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

    /** Будильник следующего цикла живёт здесь: цикл кончается на любой вкладке и во время похода. */
    private fun armCycle() {
        cycle?.cancel()
        val held = crafts
        val work = held.state?.work ?: return
        val due = work.nextAt - held.offset - System.currentTimeMillis()
        cycle = scope.launch {
            delay(due.coerceAtLeast(0))
            cycleDue()
        }
    }

    /** Текущий цикл кончился: бросить его здесь, уплатить в сумку и спросить сервер следом. */
    fun cycleDue() {
        val held = crafts
        val work = held.state?.work
        val profession = held.state?.professions?.firstOrNull { it.code == work?.profession }
        val job = work?.let { profession?.job(it.job, it.choice) }
        val bag = heroes.state.value.hero?.bag
        var thrown = false
        if (work != null && profession != null && job != null && bag != null) {
            val spent = CraftCycle.spent(job, work.additives)
            // Цикл, который сумка не оплатит, остановит сервер: здесь он не бросается. Без семени (сервер 1.53.0 держит
            // кости у себя) тоже: цикл - ответ сервера.
            if (work.seed != 0L && spent.all { (code, amount) -> (bag[code] ?: 0L) >= amount }) {
                val gains = CraftCycle.roll(work.seed, work.cycle, job, profession.bonus, work.additives)
                heroes.patch { it.copy(bag = patched(it.bag, gains)) }
                crafts { c ->
                    c.copy(
                        state = c.state?.copy(work = work.copy(settledAt = work.settledAt + work.cycleMillis, nextAt = work.nextAt + work.cycleMillis, cycle = work.cycle + 1)),
                        totals = c.totals + gains,
                        last = gains,
                        pending = c.pending + gains,
                    )
                }
                thrown = true
            }
        }
        if (thrown) armCycle() else cycle = null
        scope.launch {
            delay(SETTLE_GRACE)
            load(silent = true)
        }
    }

    /** Герой уходит с экрана: будильник снят, ремёсла забыты. */
    fun drop() {
        cycle?.cancel()
        cycle = null
        repository.clear()
    }

    fun start(job: String, choice: String = "", additives: List<String> = emptyList()) {
        buzzes.buzz(Buzz.BUTTON)
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
     * Ответ сервера против циклов, брошенных здесь: когда он сосчитал каждый, сосчитанное сверх ложится в сумку
     * и в итог; когда он отстаёт от устройства, остаток остаётся предсказанным.
     */
    private fun land(id: String, answer: CraftsState, bagFromServer: Boolean = false) {
        if (!heroes.onScreen(id)) return
        val now = System.currentTimeMillis()
        var reread = false
        crafts { c ->
            val pending = c.pending
            val local = c.state?.work
            val remote = answer.work
            val behind = local != null && remote != null && remote.job == local.job && remote.choice == local.choice && remote.cycle < local.cycle
            when {
                bagFromServer -> c.copy(
                    state = answer,
                    readAt = now,
                    pending = WorkGains(),
                    last = if (answer.gains.cycles > 0) answer.gains else c.last,
                    totals = if (behind) c.totals else c.totals + (answer.gains - pending).copy(equipment = answer.gains.equipment),
                )

                behind -> c.copy(state = answer.copy(work = local), readAt = now, pending = pending - answer.gains)

                else -> {
                    val beyond = answer.gains - pending
                    val changed = beyond.cycles != 0 || beyond.items.isNotEmpty() || beyond.spent.isNotEmpty() || answer.gains.equipment.isNotEmpty()
                    heroes.patch { it.copy(bag = patched(it.bag, beyond)) }
                    // Снаряжение или стопки сверх предсказанного: герой перечитывается, сумка и сундук берутся с сервера.
                    reread = answer.gains.equipment.isNotEmpty() || beyond.items.isNotEmpty() || beyond.spent.isNotEmpty()
                    c.copy(
                        state = answer,
                        readAt = now,
                        pending = WorkGains(),
                        totals = if (changed) c.totals + beyond.copy(equipment = answer.gains.equipment) else c.totals,
                        last = if (answer.gains.cycles > pending.cycles) answer.gains else c.last,
                    )
                }
            }
        }
        if (reread) events.heroChanged()
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
