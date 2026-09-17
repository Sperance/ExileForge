package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.PendingBattleWrite
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.UUID
import com.sperance.exileforge.core.i18n.tr

class CombatViewModel(private val runtime: ForgeRuntime) {
    private fun scope(id: String): String = with(runtime.state.value) { "$server:${requireNotNull(profile).id}:$id" }
    fun load() = with(runtime) {
        task {
            val id = state.value.characterId
            check(id.isNotBlank() && state.value.signedIn) { tr("Выберите персонажа и войдите в аккаунт", "Choose a character and sign in") }
            val saved = store.combatPending(scope(id))?.let { WireJson.decodeFromString<PendingBattleWrite>(it) }
            mutable.update { it.copy(battlePending = saved, battleView = null, battleCharacterId = "", battleAction = null) }
            check(api.capabilities().combat) { tr("Для походов обновите ktor-bestgame до 0.11.0", "Update ktor-bestgame to 0.11.0 for expeditions") }
            val catalog = api.combatCatalog()
            val view = api.battle(id)
            val hero = api.character(id)
            mutable.update { it.copy(combatCatalog = catalog, battleView = view, battleCharacterId = id, battleAction = null, hero = hero) }
        }
    }
    fun start(zoneId: String, boss: Boolean): Unit = with(runtime) {
        val s = state.value
        if(s.busy || s.battlePending != null || s.battleCharacterId != s.characterId) return
        val view = s.battleView ?: return
        submit(PendingBattleWrite(s.characterId, "start", WireJson.encodeToJsonElement(
            StartBattleCommand(view.characterVersion, UUID.randomUUID().toString(), zoneId, boss))))
    }
    fun act(action: BattleAction): Unit = with(runtime) {
        val s = state.value
        if(s.busy || s.battlePending != null || s.battleCharacterId != s.characterId) return
        val view = s.battleView ?: return
        val battle = view.battle ?: return
        if(battle.status != BattleStatus.ACTIVE) return
        submit(PendingBattleWrite(s.characterId, "act", WireJson.encodeToJsonElement(
            BattleActionCommand(view.characterVersion, UUID.randomUUID().toString(), battle.id, action))))
    }
    fun retry() { runtime.state.value.battlePending?.let(::submit) }
    /**
     * The action the durable record already carries, so the arena can animate the right turn.
     *
     * Read back out of the payload instead of being tracked separately: the command that was sent is
     * the only thing guaranteed to survive a process death and an idempotent replay.
     */
    private fun actionOf(pending: PendingBattleWrite): BattleAction? =
        if(pending.operation != "act") null
        else runCatching { WireJson.decodeFromJsonElement<BattleActionCommand>(pending.command).action }.getOrNull()
    private fun submit(pending: PendingBattleWrite) = with(runtime) {
        task(writing = true) {
            val key = scope(pending.characterId)
            // Persist BEFORE sending; reuse exactly this payload after timeout or process death.
            store.saveCombatPending(key, WireJson.encodeToString(pending))
            mutable.update { it.copy(battlePending = pending) }
            try {
                val result = api.combatCommand(pending.characterId, pending.operation, pending.command)
                store.saveCombatPending(key, null)
                mutable.update { it.copy(battlePending = null, battleView = result, battleCharacterId = pending.characterId,
                    battleAction = actionOf(pending),
                    inventoryVersion = null, equipmentView = null, comparison = null, craftOptions = null,
                    hero = it.hero?.takeIf { hero -> hero.id == pending.characterId }?.copy(version = result.characterVersion, level = result.characterLevel, experience = result.experience, money = result.gold)) }
            } catch(e: ApiFailure) {
                if(e.status?.let { it in 400..499 && it != 401 } == true) {
                    store.saveCombatPending(key, null)
                    mutable.update { it.copy(battlePending = null, battleView = null, battleCharacterId = "", battleAction = null) }
                }
                throw e
            }
        }
    }
}
