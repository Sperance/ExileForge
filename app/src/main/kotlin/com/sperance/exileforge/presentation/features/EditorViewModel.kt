package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.diff
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.creationFields
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*

class EditorViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun create(kind: EquipmentKind = EquipmentKind.Weapon) { with(runtime) {
        if (state.value.busy || !state.value.canEdit) return
        val blank = template(state.value.catalog, kind)
        // A new character belongs to whoever is signed in; the server checks the per-account limit.
        val seeded = if (state.value.catalog == Catalog.CHARACTERS)
            JsonObject(blank + ("userId" to JsonPrimitive(state.value.profile?.id.orEmpty()))) else blank
        setEditor(seeded, null)
    } }

    fun closeEditor() { with(runtime) { if (!state.value.busy) mutable.update { it.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap())) } } }
    fun edit(document: JsonObject) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(draft = document) } } }

    fun loadDefinitions() { with(runtime) { task {
        mutable.update { it.copy(definitions = api.modifierDefinitions()) }
        mutable.update { it.copy(message = tr("Загружено модификаторов: ${it.definitions.size}", "Modifiers loaded: ${it.definitions.size}")) }
    } } }

    fun reloadEditor() { with(runtime) { task {
        val original = requireNotNull(state.value.original)
        val latest = api.get(state.value.catalog, original.entityId) ?: error(tr("Запись удалена", "The record was deleted"))
        setEditor(latest, latest)
    } } }

    fun save() { with(runtime) { task(writing = true) {
        check(state.value.canEdit) { tr("Недостаточно прав", "Not enough permissions") }
        val document = state.value.draft
        val catalog = state.value.catalog
        validate(document, catalog)
        val original = state.value.original
        val saved = if (original == null) api.create(catalog, JsonObject(document.filterKeys { it in editableFields(catalog) + creationFields(catalog) }))
        else {
            val changes = diff(original, document)
            require(changes.isNotEmpty()) { tr("Нет изменений для сохранения", "Nothing to save") }
            api.update(catalog, original.entityId, changes)
        }
        if (catalog == Catalog.EQUIPMENT) mutable.update { it.copy(inventoryBases = it.inventoryBases + (saved.entityId to saved)) }
        setEditor(saved, saved)
        mutable.update { it.copy(message = tr("Сохранено: ${saved.entityId}", "Saved: ${saved.entityId}")) }
        // List refresh failure must not imply that the successful mutation failed.
        try { loadPage(state.value.page) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = tr("Сохранено. Обновите список вручную.", "Saved. Refresh the list by hand.")) } }
    } } }

    fun delete() { with(runtime) { task(writing = true) {
        val original = state.value.original ?: error(tr("Сначала сохраните предмет", "Save the item first"))
        check(state.value.canEdit)
        api.delete(state.value.catalog, original.entityId)
        mutable.update { it.copy(editorOpen = false, original = null, tab = 0, items = it.items.filterNot { item -> item.entityId == original.entityId }, message = tr("Предмет удалён", "The item was deleted")) }
        try { loadPage(0) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = tr("Удалено. Обновите список вручную.", "Deleted. Refresh the list by hand.")) } }
    } } }

    /** Opens the shared template an inventory instance was rolled from. */
    fun editInventoryBase(id: String) { with(runtime) { task {
        check(!state.value.editorOpen || state.value.original?.let { diff(it, state.value.draft).isEmpty() } == true) { tr("Сохраните или закройте текущий черновик", "Save or close the current draft") }
        val document = api.get(Catalog.EQUIPMENT, id) ?: error(tr("База предмета не найдена", "The item base was not found"))
        ensureDefinitions()
        mutable.update { it.copy(catalog = Catalog.EQUIPMENT, items = emptyList(), total = 0, page = 0, totalPages = 0) }
        setEditor(document, document)
    } } }
}
