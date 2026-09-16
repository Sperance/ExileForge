package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.editor.conflict.ThreeWayMerge
import com.sperance.exileforge.core.contract.entityVersion
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.diff
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.core.i18n.tr

class EditorViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) { with(runtime) {

        if (state.value.busy || !state.value.canEdit) return
        setEditor(template(state.value.catalog, kind), null)

    } }
    fun closeEditor() { with(runtime) {
 if (!state.value.busy) mutable.update { it.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap())) }
    } }
    fun edit(document: JsonObject) { with(runtime) {
 if (!state.value.busy) mutable.update { it.copy(draft = document) }
    } }
    fun publishDefinition(document: JsonObject, expectedRevision: Int) { with(runtime) {
task(writing = true) {
        validateForm("definition", document)
        val saved = api.publishDefinition(document, expectedRevision)
        mutable.update { it.copy(definitions = (listOf(saved) + it.definitions).distinctBy(::definitionKey), message = tr("Опубликовано ${saved.text("id")} v${saved.text("revision")}", "Published ${saved.text("id")} v${saved.text("revision")}")) }
    }
    } }
    fun definitionQuery(value: String) { with(runtime) {
 mutable.update { it.copy(definitionQuery = value) }
    } }
    fun loadDefinitions(page: Int = 0) { with(runtime) {
task {
        val result = api.definitions(state.value.definitionQuery.trim(), page)
        val definitions = (pinnedDefinitions(state.value.draft) + result.getValue("items").jsonArray.map { it.jsonObject }).distinctBy(::definitionKey)
        mutable.update { it.copy(definitions = definitions, definitionPage = page, definitionTotal = result.getValue("total").jsonPrimitive.int) }
    }
    } }
    fun reviewConflict() { with(runtime) {
task {
        val base = requireNotNull(state.value.original)
        val remote = api.get(state.value.catalog, base.entityId) ?: error(tr("Запись удалена", "The record was deleted"))
        val review = ThreeWayMerge.review(base, state.value.draft, remote)
        mutable.update { it.copy(mergeReview = review, mergeRemote = remote) }
    }
    } }
    fun resolveConflict(choices: Map<String, Boolean>) { with(runtime) {
task {
        val review = requireNotNull(state.value.mergeReview)
        val remote = requireNotNull(state.value.mergeRemote)
        val merged = ThreeWayMerge.resolve(review, choices)
        setEditor(merged, remote)
        mutable.update { it.copy(message = tr("Черновик объединён. Проверьте его и нажмите «Сохранить».", "The draft was merged. Review it and press Save.")) }
    }
    } }
    fun reloadEditor() { with(runtime) {
task {
        val original = requireNotNull(state.value.original)
        val latest = api.get(state.value.catalog, original.entityId) ?: error(tr("Запись удалена", "The record was deleted"))
        setEditor(latest, latest)
    }
    } }
    fun save() { with(runtime) {
task(writing = true) {
        check(state.value.canEdit) { tr("Недостаточно прав", "Not enough permissions") }
        check(!state.value.conflict) { tr("Сначала обновите запись после конфликта", "Refresh the record after the conflict first") }
        val document = state.value.draft
        val catalog = state.value.catalog
        validate(document, catalog)
        val original = state.value.original
        val saved = if (original == null) api.create(catalog, JsonObject(document.filterKeys { it in editableFields(catalog) || it == "type" })) else {
            val changes = diff(original, document)
            require(catalog != Catalog.CHARACTERS || "userId" !in changes) { tr("Владельца существующего персонажа менять нельзя", "The owner of an existing character cannot be changed") }
            require(changes.isNotEmpty()) { tr("Нет изменений для сохранения", "Nothing to save") }
            api.update(catalog, original.entityId, changes, original.entityVersion)
        }
        if(catalog == Catalog.EQUIPMENT) mutable.update { it.copy(inventoryBases = it.inventoryBases + (saved.entityId to saved)) }
        setEditor(saved, saved)
        mutable.update { it.copy(message = tr("Сохранено: ${saved.entityId}", "Saved: ${saved.entityId}")) }
        // List refresh failure must not imply that the successful mutation failed.
        try { loadPage(state.value.page) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = tr("Сохранено. Обновите список вручную.", "Saved. Refresh the list by hand.")) } }
    }
    } }
    fun delete() { with(runtime) {
task(writing = true) {
        val original = state.value.original ?: error(tr("Сначала сохраните предмет", "Save the item first"))
        check(state.value.canEdit && !state.value.conflict)
        api.delete(state.value.catalog, original.entityId, original.entityVersion)
        mutable.update { it.copy(editorOpen = false, original = null, tab = 0, items = it.items.filterNot { item -> item.entityId == original.entityId }, message = tr("Предмет удалён", "The item was deleted")) }
        try { loadPage(0) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = tr("Удалено. Обновите список вручную.", "Deleted. Refresh the list by hand.")) } }
    }
    } }
    fun editInventoryBase(id: String) { with(runtime) {
task {
        check(!state.value.editorOpen || state.value.original?.let { diff(it, state.value.draft).isEmpty() } == true) { tr("Сохраните или закройте текущий черновик", "Save or close the current draft") }
        val doc = api.get(Catalog.EQUIPMENT, id) ?: error(tr("База предмета не найдена", "The item base was not found"))
        val definitions = pinnedDefinitions(doc)
        mutable.update { it.copy(catalog = Catalog.EQUIPMENT, items = emptyList(), total = 0, page = 0, totalPages = 0, definitions = definitions) }
        setEditor(doc, doc)
    }
    } }
}
