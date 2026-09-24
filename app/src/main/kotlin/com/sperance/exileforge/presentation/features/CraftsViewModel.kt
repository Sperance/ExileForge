package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.Reads
import kotlinx.coroutines.flow.update

/**
 * The crafts (since 2.41.0, server 0.37.0). The work runs on the server by time; the client asks
 * when the tab opens and again when the next cycle is due, and every answer is the whole state with
 * the cycles counted up to now — their yield already in the bag, which is why an answer that
 * brought something makes the hero go cold.
 */
class CraftsViewModel(private val runtime: ForgeRuntime) {

    fun load() { with(runtime) { read(Reads.CRAFTS) {
        val id = state.value.play.characterId
        if (id.isBlank()) return@read
        ensureMaterials(); ensureEquipment(); ensureDefinitions()
        land(id, api.crafts.state(id))
    } } }

    fun openProfession(code: String) { runtime.mutable.update { it.copy(play = it.play.copy(craftsProfession = code)) } }

    fun start(job: String, additives: List<String> = emptyList()) { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = state.value.play.characterId
        land(id, api.crafts.start(id, job, additives))
    } } }

    fun stop() { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = state.value.play.characterId
        land(id, api.crafts.stop(id))
    } } }

    /** A tool into its profession's slot: the server's equip, and the crafts read again for the new numbers. */
    fun equipTool(instanceId: String) { with(runtime) { task(writing = true, touches = setOf(Reads.CRAFTS, Reads.HERO)) {
        val id = state.value.play.characterId
        api.hero.equip(id, instanceId, null)
        land(id, api.crafts.state(id))
        mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
    } } }

    private fun land(id: String, answer: CraftsState) { with(runtime) {
        val brought = answer.gains.cycles > 0
        mutable.update { s -> if (s.play.characterId != id) s else s.copy(play = s.play.copy(
            crafts = answer, craftsAt = System.currentTimeMillis(),
            craftsLog = if (brought) (listOf(answer.gains) + s.play.craftsLog).take(LOG) else s.play.craftsLog,
            heroReadAt = if (brought) 0 else s.play.heroReadAt)) }
    } }

    private companion object { const val LOG = 30 }
}
