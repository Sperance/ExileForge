package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.core.crafts.CraftCycle
import com.sperance.exileforge.core.crafts.minus
import com.sperance.exileforge.core.crafts.plus
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.crafts.job
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.rules.roll.WorkGains
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The crafts. The work runs on the server by time. A cycle that ends is thrown here from the work's seed
 * and number, its stacks land in the bag at once, and the server is asked in the background: its answer
 * counts the same cycles, and whatever it counted differently — or beyond, while the app was away — is set right then.
 */
class CraftsViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun load(silent: Boolean = false) { with(runtime) { read(Reads.CRAFTS, silent = silent) {
        val id = state.value.play.heroId
        if (id.isBlank()) return@read
        ensureContent()
        land(id, api.crafts.state(id))
    } } }

    private var cycle: Job? = null

    /** The next cycle's alarm: it lives here, so a cycle ends on every tab and during a run alike. */
    private fun armCycle() { with(runtime) {
        cycle?.cancel()
        val play = state.value.play
        val crafts = play.crafts ?: return
        val work = crafts.work ?: return
        val due = work.nextAt - (crafts.now - play.craftsAt) - System.currentTimeMillis()
        cycle = scope.launch { delay(due.coerceAtLeast(0)); cycleDue() }
    } }

    /** The cycle under way has ended: throw it here, pay it into the bag, and ask the server behind it. */
    fun cycleDue() { with(runtime) {
        val play = state.value.play
        val work = play.crafts?.work
        val profession = play.crafts?.professions?.firstOrNull { it.code == work?.profession }
        val job = work?.let { profession?.job(it.job, it.choice) }
        val bag = play.hero?.bag
        var thrown = false
        if (work != null && profession != null && job != null && bag != null) {
            val spent = CraftCycle.spent(job, work.additives)
            // A cycle the bag cannot pay for is the server's to stop: nothing is thrown for it here. Without a seed
            // (server 1.53.0 keeps the dice to itself) nothing is thrown either: the server's answer is the cycle.
            if (work.seed != 0L && spent.all { (code, amount) -> (bag[code] ?: 0L) >= amount }) {
                val gains = CraftCycle.roll(work.seed, work.cycle, job, profession.bonus, work.additives)
                mutable.update { s -> s.copy(play = s.play.copy(
                    hero = s.play.hero?.let { it.copy(bag = patched(it.bag, gains)) },
                    crafts = s.play.crafts?.copy(work = work.copy(settledAt = work.settledAt + work.cycleMillis, nextAt = work.nextAt + work.cycleMillis, cycle = work.cycle + 1)),
                    craftsTotals = s.play.craftsTotals + gains, craftsLast = gains, craftsPending = s.play.craftsPending + gains)) }
                thrown = true
            }
        }
        if (thrown) armCycle() else cycle = null
        scope.launch { delay(SETTLE_GRACE); load(silent = true) }
    } }

    fun drop() { with(runtime) {
        cycle?.cancel()
        cycle = null
        mutable.update { it.copy(play = it.play.copy(crafts = null, craftsPending = WorkGains())) }
    } }

    fun openProfession(code: String) { runtime.mutable.update { it.copy(play = it.play.copy(craftsProfession = code)) } }

    fun start(job: String, choice: String = "", additives: List<String> = emptyList()) { with(runtime) { buzz(Buzz.BUTTON); task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = state.value.play.heroId
        val before = heroViewModel.snapshots
        land(id, api.crafts.start(id, job, choice, additives), heroViewModel.snapshots != before)
    } } }

    fun stop() { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = state.value.play.heroId
        val before = heroViewModel.snapshots
        land(id, api.crafts.stop(id), heroViewModel.snapshots != before)
    } } }

    /** A tool into its profession's slot: the server's equip, and the crafts read again for the new numbers. */
    fun equipTool(itemId: String) { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS, Reads.HERO)) {
        val id = state.value.play.heroId
        api.hero.equip(id, itemId, null)
        land(id, api.crafts.state(id))
    } } }

    /**
     * The server's answer, set against the cycles thrown here: when it has counted every one of them, what
     * it counted beyond goes into the bag and the tally; when it is behind the device, the rest stays predicted.
     */
    private fun land(id: String, answer: CraftsState, bagFromServer: Boolean = false) { with(runtime) {
        mutable.update { s ->
            if (s.play.heroId != id) return@update s
            val pending = s.play.craftsPending
            val local = s.play.crafts?.work
            val remote = answer.work
            val behind = local != null && remote != null && remote.job == local.job && remote.choice == local.choice && remote.cycle < local.cycle
            if (bagFromServer) s.copy(play = s.play.copy(crafts = answer, craftsAt = System.currentTimeMillis(),
                craftsPending = WorkGains(), craftsLast = if (answer.gains.cycles > 0) answer.gains else s.play.craftsLast,
                craftsTotals = if (behind) s.play.craftsTotals else s.play.craftsTotals + (answer.gains - pending).copy(equipment = answer.gains.equipment)))
            else if (behind) s.copy(play = s.play.copy(crafts = answer.copy(work = local), craftsAt = System.currentTimeMillis(), craftsPending = pending - answer.gains))
            else {
                val beyond = answer.gains - pending
                val changed = beyond.cycles != 0 || beyond.items.isNotEmpty() || beyond.spent.isNotEmpty() || answer.gains.equipment.isNotEmpty()
                s.copy(play = s.play.copy(crafts = answer, craftsAt = System.currentTimeMillis(), craftsPending = WorkGains(),
                    hero = s.play.hero?.let { it.copy(bag = patched(it.bag, beyond)) },
                    craftsTotals = if (changed) s.play.craftsTotals + beyond.copy(equipment = answer.gains.equipment) else s.play.craftsTotals,
                    craftsLast = if (answer.gains.cycles > pending.cycles) answer.gains else s.play.craftsLast,
                    heroReadAt = if (answer.gains.equipment.isNotEmpty() || beyond.items.isNotEmpty() || beyond.spent.isNotEmpty()) 0 else s.play.heroReadAt))
            }
        }
        armCycle()
    } }

    /** The bag with a tally's stacks paid in and its spending taken out, by code. */
    private fun patched(bag: Map<String, Long>, gains: WorkGains): Map<String, Long> {
        if (gains.items.isEmpty() && gains.spent.isEmpty()) return bag
        val have = bag.toMutableMap()
        gains.items.forEach { (code, amount) -> have.merge(code, amount, Long::plus) }
        gains.spent.forEach { (code, amount) -> have.merge(code, -amount, Long::plus) }
        return have.filterValues { it > 0 }
    }

    private companion object { const val SETTLE_GRACE = 600L }
}
