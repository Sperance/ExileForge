package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sperance.exileforge.ForgeApplication
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.AppMode
import kotlinx.serialization.json.JsonObject

/** Lifecycle owner and compatibility facade; screen actions live in feature models. */
class ForgeViewModel(store: ServerStore, journal: RequestJournal) : ViewModel() {
    private val runtime = ForgeRuntime(store, journal)
    val state = runtime.state
    val logs = runtime.logs
    fun tab(tab: Int) = runtime.tab(tab)
    fun language(lang: Lang) = runtime.language(lang)
    fun dismissMessage() = runtime.dismissMessage()
    suspend fun referencePage(source: EntitySource, page: Int, query: String) = runtime.referencePage(source, page, query)
    suspend fun recipeDocument(id: String) = runtime.recipeDocument(id)
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
    fun loadDefinitions() = runtime.editorViewModel.loadDefinitions()
    fun reloadEditor() = runtime.editorViewModel.reloadEditor()
    fun save() = runtime.editorViewModel.save()
    fun delete() = runtime.editorViewModel.delete()
    fun editInventoryBase(id: String) = runtime.editorViewModel.editInventoryBase(id)
    fun characterId(value: String) = runtime.heroViewModel.characterId(value)
    fun selectEquipment(value: String) = runtime.heroViewModel.selectEquipment(value)
    fun showCharacterInventory(id: String) = runtime.heroViewModel.showCharacterInventory(id)
    fun loadHero() = runtime.heroViewModel.loadHero()
    fun equip(instanceId: String) = runtime.heroViewModel.equip(instanceId)
    fun unequip(instanceId: String) = runtime.heroViewModel.unequip(instanceId)
    fun grant(equipmentId: String) = runtime.heroViewModel.grant(equipmentId)
    fun grantRarity(value: String) = runtime.heroViewModel.grantRarity(value)
    fun grantSlot(value: String) = runtime.heroViewModel.grantSlot(value)
    fun grantRandom() = runtime.heroViewModel.grantRandom()
    fun adjustItems(itemId: String, amount: Long) = runtime.heroViewModel.adjustItems(itemId, amount)
    fun redeem(code: String) = runtime.heroViewModel.redeem(code)
    fun useRecipe(recipeId: String, ingredients: List<String>, amount: Long) = runtime.heroViewModel.useRecipe(recipeId, ingredients, amount)
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
