package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.crafts.CraftCycle
import com.sperance.exileforge.core.crafts.minus
import com.sperance.exileforge.core.crafts.plus
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.crafts.WorkGains
import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.Reads
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The crafts (since 2.41.0, server 0.37.0). The work runs on the server by time. Since 2.47.0 a
 * cycle that ends is thrown here from the work's seed and number (`CraftCycle`), its stacks land in
 * the bag at once, and the server is asked in the background: its answer counts the same cycles,
 * and whatever it counted differently — or beyond, while the app was away — is set right then.
 * Gear and maps a cycle makes are the server's alone and arrive with its answer.
 */
class CraftsViewModel(private val runtime: ForgeRuntime) {

    fun load() { with(runtime) { read(Reads.CRAFTS) {
        val id = state.value.play.characterId
        if (id.isBlank()) return@read
        ensureWorld()
        land(id, api.crafts.state(id))
    } } }

    /** The cycle under way has ended: throw it here, pay it into the bag, and ask the server behind it. */
    fun cycleDue() { with(runtime) {
        val play = state.value.play
        val work = play.crafts?.work
        val profession = play.crafts?.professions?.firstOrNull { it.code == work?.profession }
        val job = profession?.jobs?.firstOrNull { it.code == work?.job }
        val bag = play.hero?.bag
        if (work != null && profession != null && job != null && bag != null) {
            val spent = CraftCycle.spent(job, work.additives)
            // A cycle the bag cannot pay for is the server's to stop: nothing is thrown for it here.
            if (spent.all { (code, amount) -> amountOf(state.value, bag, code) >= amount }) {
                val gains = CraftCycle.roll(work.seed, work.cycle, job, profession.bonus, work.additives)
                mutable.update { s -> s.copy(play = s.play.copy(
                    hero = s.play.hero?.let { it.copy(bag = patched(s, it.bag, gains)) },
                    crafts = s.play.crafts?.copy(work = work.copy(settledAt = work.settledAt + work.cycleMillis,
                        nextAt = work.nextAt + work.cycleMillis, cycle = work.cycle + 1)),
                    craftsTotals = s.play.craftsTotals + gains, craftsLast = gains, craftsPending = s.play.craftsPending + gains)) }
            }
        }
        // The server is asked a moment later, so its clock has reached the cycle too.
        scope.launch { delay(SETTLE_GRACE); load() }
    } }

    fun openProfession(code: String) { runtime.mutable.update { it.copy(play = it.play.copy(craftsProfession = code)) } }

    fun start(job: String, additives: List<String> = emptyList()) { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = state.value.play.characterId
        val before = heroViewModel.snapshots
        land(id, api.crafts.start(id, job, additives), heroViewModel.snapshots != before)
    } } }

    fun stop() { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = state.value.play.characterId
        val before = heroViewModel.snapshots
        land(id, api.crafts.stop(id), heroViewModel.snapshots != before)
    } } }

    /** A tool into its profession's slot: the server's equip, and the crafts read again for the new numbers. */
    fun equipTool(instanceId: String) { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS, Reads.HERO)) {
        val id = state.value.play.characterId
        api.hero.equip(id, instanceId, null)
        land(id, api.crafts.state(id))
    } } }

    /**
     * The server's answer, set against the cycles thrown here. When it has counted every one of them,
     * what it counted beyond or otherwise goes into the bag and the tally, and the prediction is done
     * with; when it is behind the device (its clock has not reached them yet), it confirms what it
     * counted and the rest stays predicted until the next answer.
     */
    private fun land(id: String, answer: CraftsState, bagFromServer: Boolean = false) { with(runtime) {
        mutable.update { s ->
            if (s.play.characterId != id) return@update s
            val pending = s.play.craftsPending
            val local = s.play.crafts?.work
            val behind = local != null && answer.work != null && answer.work!!.job == local.job && answer.work!!.cycle < local.cycle
            // A command's snapshot already holds the bag the server settled, predictions and all.
            if (bagFromServer) s.copy(play = s.play.copy(crafts = answer, craftsAt = System.currentTimeMillis(),
                craftsPending = WorkGains(), craftsLast = if (answer.gains.cycles > 0) answer.gains else s.play.craftsLast,
                craftsTotals = if (behind) s.play.craftsTotals else s.play.craftsTotals + (answer.gains - pending).copy(equipment = answer.gains.equipment)))
            else if (behind) s.copy(play = s.play.copy(crafts = answer.copy(work = local), craftsAt = System.currentTimeMillis(),
                craftsPending = pending - answer.gains))
            else {
                val beyond = answer.gains - pending
                val changed = beyond.cycles != 0 || beyond.items.isNotEmpty() || beyond.spent.isNotEmpty() || answer.gains.equipment.isNotEmpty()
                s.copy(play = s.play.copy(crafts = answer, craftsAt = System.currentTimeMillis(), craftsPending = WorkGains(),
                    hero = s.play.hero?.let { it.copy(bag = patched(s, it.bag, beyond)) },
                    craftsTotals = if (changed) s.play.craftsTotals + beyond.copy(equipment = answer.gains.equipment) else s.play.craftsTotals,
                    craftsLast = if (answer.gains.cycles > pending.cycles) answer.gains else s.play.craftsLast,
                    // Gear and maps come from the server alone; a bag counted otherwise is read again when the hero is next needed.
                    heroReadAt = if (answer.gains.equipment.isNotEmpty() || beyond.items.isNotEmpty() || beyond.spent.isNotEmpty()) 0 else s.play.heroReadAt))
            }
        }
    } }

    /** The bag with a tally's stacks paid in and its spending taken out, by the items' codes. */
    private fun patched(s: ForgeState, bag: List<CharacterItem>, gains: WorkGains): List<CharacterItem> {
        val change = mutableMapOf<String, Long>()
        gains.items.forEach { (code, amount) -> idOf(s, code)?.let { change.merge(it, amount, Long::plus) } }
        gains.spent.forEach { (code, amount) -> idOf(s, code)?.let { change.merge(it, -amount, Long::plus) } }
        if (change.isEmpty()) return bag
        val have = bag.associate { it.itemId to it.amount }.toMutableMap()
        change.forEach { (id, amount) -> have.merge(id, amount, Long::plus) }
        val order = bag.map { it.itemId } + change.keys.filter { key -> bag.none { it.itemId == key } }
        return order.mapNotNull { id -> have[id]?.takeIf { it > 0 }?.let { CharacterItem(id, it) } }
    }

    private fun idOf(s: ForgeState, code: String): String? =
        s.world.materials.firstOrNull { it.code == code }?.id ?: s.world.orbs.firstOrNull { it.code == code }?.id

    private fun amountOf(s: ForgeState, bag: List<CharacterItem>, code: String): Long =
        idOf(s, code)?.let { id -> bag.firstOrNull { it.itemId == id }?.amount } ?: 0

    private companion object { const val SETTLE_GRACE = 600L }
}
