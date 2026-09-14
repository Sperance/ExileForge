package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.ForgeApplication
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.editor.conflict.ThreeWayMerge
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.contract.entityVersion
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.diff
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.referenceKey
import com.sperance.exileforge.core.contract.starterModifier
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.generation.modifierFromDefinition
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.core.verification.CrudScenario
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.PendingInventoryAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*

/** Lifecycle owner and compatibility facade; screen actions live in feature models. */
class ForgeViewModel(store: ServerStore, journal: RequestJournal) : ViewModel() {
    private val runtime = ForgeRuntime(store, journal)
    val state = runtime.state
    val logs = runtime.logs
    fun tab(tab: Int) = runtime.tab(tab)
    fun dismissMessage() = runtime.dismissMessage()
    suspend fun referencePage(source: EntitySource, page: Int, query: String) = runtime.referencePage(source, page, query)
    suspend fun recipe(id: String) = runtime.recipe(id)
    fun query(value: String) = runtime.catalogViewModel.query(value)
    fun catalog(value: Catalog) = runtime.catalogViewModel.catalog(value)
    fun filter(value: CatalogFilter) = runtime.catalogViewModel.filter(value)
    fun applyFilters() = runtime.catalogViewModel.applyFilters()
    fun refresh(page: Int = runtime.state.value.page) = runtime.catalogViewModel.refresh(page)
    fun count() = runtime.catalogViewModel.count()
    fun open(id: String) = runtime.catalogViewModel.open(id)
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) = runtime.editorViewModel.create(kind)
    fun closeEditor() = runtime.editorViewModel.closeEditor()
    fun edit(document: JsonObject) = runtime.editorViewModel.edit(document)
    fun publishDefinition(document: JsonObject, expectedRevision: Int) = runtime.editorViewModel.publishDefinition(document, expectedRevision)
    fun definitionQuery(value: String) = runtime.editorViewModel.definitionQuery(value)
    fun loadDefinitions(page: Int = 0) = runtime.editorViewModel.loadDefinitions(page)
    fun reviewConflict() = runtime.editorViewModel.reviewConflict()
    fun resolveConflict(choices: Map<String, Boolean>) = runtime.editorViewModel.resolveConflict(choices)
    fun reloadEditor() = runtime.editorViewModel.reloadEditor()
    fun save() = runtime.editorViewModel.save()
    fun delete() = runtime.editorViewModel.delete()
    fun editInventoryBase(id: String) = runtime.editorViewModel.editInventoryBase(id)
    fun characterId(value: String) = runtime.heroViewModel.characterId(value)
    fun selectEquipment(value: String) = runtime.heroViewModel.selectEquipment(value)
    fun showCharacterInventory(id: String) = runtime.heroViewModel.showCharacterInventory(id)
    fun selectCurrency(value: String) = runtime.heroViewModel.selectCurrency(value)
    fun compareEquipment(uuid: String, slot: EquipmentSlot) = runtime.heroViewModel.compareEquipment(uuid, slot)
    fun dismissComparison() = runtime.heroViewModel.dismissComparison()
    fun equipCompared() = runtime.heroViewModel.equipCompared()
    fun equip(uuid: String, slot: EquipmentSlot) = runtime.heroViewModel.equip(uuid, slot)
    fun unequip(slot: EquipmentSlot) = runtime.heroViewModel.unequip(slot)
    fun grant(equipmentId: String) = runtime.heroViewModel.grant(equipmentId)
    fun adjustItems(itemId: String, amount: Long) = runtime.heroViewModel.adjustItems(itemId, amount)
    fun redeem(code: String) = runtime.heroViewModel.redeem(code)
    fun useRecipe(recipe: JsonObject, ingredients: List<String>, amount: Long) = runtime.heroViewModel.useRecipe(recipe, ingredients, amount)
    fun loadInventory() = runtime.heroViewModel.loadInventory()
    fun randomItem() = runtime.heroViewModel.randomItem()
    fun inventoryAction(operation: String) = runtime.heroViewModel.inventoryAction(operation)
    fun retryInventoryAction() = runtime.heroViewModel.retryInventoryAction()
    fun mode(mode: AppMode) = runtime.sessionViewModel.mode(mode)
    fun serverDraft(value: String) = runtime.sessionViewModel.serverDraft(value)
    fun connect() = runtime.sessionViewModel.connect()
    fun health() = runtime.sessionViewModel.health()
    fun login(login: String, password: String) = runtime.sessionViewModel.login(login, password)
    fun logout() = runtime.sessionViewModel.logout()
    fun changePassword(current: String, replacement: String) = runtime.sessionViewModel.changePassword(current, replacement)
    fun runChecks() = runtime.checksViewModel.runChecks()
    fun clearLogs() = runtime.checksViewModel.clearLogs()
    override fun onCleared() { runtime.close() }
    class Factory(private val app: ForgeApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ForgeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ForgeViewModel(app.serverStore, app.journal) as T
        }
    }
}
