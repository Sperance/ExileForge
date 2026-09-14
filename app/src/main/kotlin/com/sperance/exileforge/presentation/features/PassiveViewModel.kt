package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.PendingPassiveWrite
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

class PassiveViewModel(private val runtime: ForgeRuntime) {
    private fun scope(id: String): String = with(runtime.state.value) { "$server:${requireNotNull(profile).id}:$id" }
    fun load() = with(runtime) {
        task {
            val id = state.value.characterId
            check(state.value.signedIn && id.isNotBlank()) { "Войдите в аккаунт и выберите персонажа" }
            val pending = store.passivePending(scope(id))?.let { WireJson.decodeFromString<PendingPassiveWrite>(it) }
            mutable.update { it.copy(passivePending = pending, passiveState = null, passiveTree = null, passiveCharacterId = "") }
            check(api.capabilities().passiveTree) { "Для дерева навыков обновите сервер до 0.12.0" }
            val result = api.passiveState(id)
            val tree = api.passiveTree(result.treeRevision)
            mutable.update { it.copy(passiveState = result, passiveTree = tree, passiveCharacterId = id) }
        }
    }
    fun change(action: PassiveAction, nodeId: String?): Unit = with(runtime) {
        val s = state.value
        if(s.busy || s.passivePending != null || s.passiveCharacterId != s.characterId) return
        val passives = s.passiveState ?: return
        if(passives.lockedReason != null) return
        submit(PendingPassiveWrite(s.characterId, PassiveCommand(passives.characterVersion, passives.treeRevision,
            UUID.randomUUID().toString(), action, nodeId)))
    }
    fun retry() { runtime.state.value.passivePending?.let(::submit) }
    private fun submit(pending: PendingPassiveWrite) = with(runtime) {
        task(writing = true) {
            val key = scope(pending.characterId)
            store.savePassivePending(key, WireJson.encodeToString(pending))
            mutable.update { it.copy(passivePending = pending) }
            try {
                val result = api.changePassives(pending.characterId, pending.command)
                store.savePassivePending(key, null)
                mutable.update { it.copy(passivePending = null, passiveState = result, passiveCharacterId = pending.characterId,
                    inventoryVersion = null, equipmentView = null, comparison = null, craftOptions = null, battleView = null, battleCharacterId = "") }
            } catch(e: ApiFailure) {
                if(e.status?.let { it in 400..499 && it != 401 } == true) {
                    store.savePassivePending(key, null)
                    mutable.update { it.copy(passivePending = null, passiveState = null, passiveCharacterId = "") }
                }
                throw e
            }
        }
    }
}
