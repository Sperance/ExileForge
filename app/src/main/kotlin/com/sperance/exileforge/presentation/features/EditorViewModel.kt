package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.diff
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.creationFields
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*

class EditorViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun draftClass(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(draftClass = value) } } }

    /**
     * A blank draft.
     *
     * A new character carries two fields the form never shows: the owner, who is whoever is signed
     * in, and the class, which decides the whole stat base and cannot be changed afterwards. The
     * classes are read here because the form needs them before it can offer a choice.
     */
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) { with(runtime) {
        if (state.value.busy || !state.value.canEdit) return
        if (state.value.catalog != Catalog.CHARACTERS) { setEditor(template(state.value.catalog, kind), null); return }
        task {
            ensureProgression()
            setEditor(JsonObject(template(Catalog.CHARACTERS) + mapOf(
                "userId" to JsonPrimitive(state.value.profile?.id.orEmpty()),
                "classId" to JsonPrimitive(state.value.draftClass))), null)
        }
    } }

    fun closeEditor() { with(runtime) { if (!state.value.busy) mutable.update { it.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap())) } } }
    fun edit(document: JsonObject) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(draft = document) } } }

    fun loadDefinitions() { with(runtime) { task {
        mutable.update { it.copy(definitions = api.modifierDefinitions()) }
        mutable.update { it.copy(message = ui("editor.modifiers_loaded", it.definitions.size)) }
    } } }

    fun reloadEditor() { with(runtime) { task {
        val original = requireNotNull(state.value.original)
        val latest = api.get(state.value.catalog, original.entityId) ?: error(ui("editor.record_deleted"))
        setEditor(latest, latest)
    } } }

    fun save() { with(runtime) { task(writing = true) {
        check(state.value.canEdit) { ui("editor.no_rights") }
        val document = state.value.draft
        val catalog = state.value.catalog
        validate(document, catalog)
        val original = state.value.original
        val saved = if (original == null) api.create(catalog, JsonObject(document.filterKeys { it in editableFields(catalog) + creationFields(catalog) }))
        else {
            val changes = diff(original, document)
            require(changes.isNotEmpty()) { ui("editor.nothing_to_save") }
            api.update(catalog, original.entityId, changes)
        }
        if (catalog == Catalog.EQUIPMENT) mutable.update { it.copy(inventoryBases = it.inventoryBases + (saved.entityId to saved)) }
        setEditor(saved, saved)
        mutable.update { it.copy(message = ui("editor.saved", saved.entityId)) }
        // List refresh failure must not imply that the successful mutation failed.
        try { loadPage(state.value.page) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = ui("editor.saved_refresh")) } }
    } } }

    fun delete() { with(runtime) { task(writing = true) {
        val original = state.value.original ?: error(ui("editor.save_first"))
        check(state.value.canEdit)
        api.delete(state.value.catalog, original.entityId)
        mutable.update { it.copy(editorOpen = false, original = null, tab = 0, items = it.items.filterNot { item -> item.entityId == original.entityId }, message = ui("editor.item_deleted")) }
        try { loadPage(0) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = ui("editor.deleted_refresh")) } }
    } } }

    /** Opens the shared template an inventory instance was rolled from. */
    fun editInventoryBase(id: String) { with(runtime) { task {
        check(!state.value.editorOpen || state.value.original?.let { diff(it, state.value.draft).isEmpty() } == true) { ui("editor.close_draft") }
        val document = api.get(Catalog.EQUIPMENT, id) ?: error(ui("editor.base_not_found"))
        ensureDefinitions()
        mutable.update { it.copy(catalog = Catalog.EQUIPMENT, items = emptyList(), total = 0, page = 0, totalPages = 0) }
        setEditor(document, document)
    } } }
}
