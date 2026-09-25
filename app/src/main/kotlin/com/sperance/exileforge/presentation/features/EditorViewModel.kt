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
import com.sperance.exileforge.presentation.state.Reads
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*

class EditorViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun draftClass(value: String) = update { it.copy(play = it.play.copy(draftClass = value)) }

    /**
     * A blank draft.
     *
     * A new character carries two fields the form never shows: the owner, who is whoever is signed
     * in, and the class, which decides the whole stat base and cannot be changed afterwards. The
     * classes are read here because the form needs them before it can offer a choice.
     */
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) { with(runtime) {
        if (state.value.busy || !state.value.canEdit) return
        if (state.value.admin.catalog != Catalog.CHARACTERS) { setEditor(template(state.value.admin.catalog, kind), null); return }
        task(touches = setOf(Reads.PROGRESSION)) {
            ensureWorld()
            setEditor(JsonObject(template(Catalog.CHARACTERS) + mapOf(
                "userId" to JsonPrimitive(state.value.account.profile?.id.orEmpty()),
                "classId" to JsonPrimitive(state.value.play.draftClass))), null)
        }
    } }

    fun closeEditor() { with(runtime) { if (!state.value.busy) mutable.update { it.copy(admin = it.admin.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap()))) } } }
    fun edit(document: JsonObject) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(admin = it.admin.copy(draft = document)) } } }

    fun loadDefinitions() { with(runtime) { read(Reads.DEFINITIONS) { ensureWorld(fresh = true) } } }

    fun reloadEditor() { with(runtime) { task(touches = setOf(Reads.CATALOG)) {
        val original = requireNotNull(state.value.admin.original)
        val latest = api.catalog.get(state.value.admin.catalog, original.entityId) ?: error(ui("editor.record_deleted"))
        setEditor(latest, latest)
    } } }

    fun save() { with(runtime) { task(writing = true, touches = setOf(Reads.CATALOG)) {
        check(state.value.canEdit) { ui("editor.no_rights") }
        val document = state.value.admin.draft
        val catalog = state.value.admin.catalog
        validate(document, catalog)
        val original = state.value.admin.original
        val saved = if (original == null) api.catalog.create(catalog, JsonObject(document.filterKeys { it in editableFields(catalog) + creationFields(catalog) }))
        else {
            val changes = diff(original, document)
            require(changes.isNotEmpty()) { ui("editor.nothing_to_save") }
            api.catalog.update(catalog, original.entityId, changes)
        }
        staleWorld()
        if (catalog == Catalog.EQUIPMENT) mutable.update { it.copy(world = it.world.copy(inventoryBases = it.world.inventoryBases + (saved.entityId to saved))) }
        setEditor(saved, saved)
        // List refresh failure must not imply that the successful mutation failed.
        try { loadPage(state.value.admin.page) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = ui("editor.saved_refresh"), error = true) } }
    } } }

    fun delete() { with(runtime) { task(writing = true, touches = setOf(Reads.CATALOG)) {
        val original = state.value.admin.original ?: error(ui("editor.save_first"))
        check(state.value.canEdit)
        api.catalog.delete(state.value.admin.catalog, original.entityId)
        staleWorld()
        mutable.update { it.copy(tab = 0, admin = it.admin.copy(editorOpen = false, original = null, items = it.admin.items.filterNot { item -> item.entityId == original.entityId })) }
        try { loadPage(0) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = ui("editor.deleted_refresh"), error = true) } }
    } } }

    /** Opens the shared template an inventory instance was rolled from. */
    fun editInventoryBase(id: String) { with(runtime) { task(touches = setOf(Reads.CATALOG)) {
        check(!state.value.admin.editorOpen || state.value.admin.original?.let { diff(it, state.value.admin.draft).isEmpty() } == true) { ui("editor.close_draft") }
        val document = api.catalog.get(Catalog.EQUIPMENT, id) ?: error(ui("editor.base_not_found"))
        ensureWorld()
        mutable.update { it.copy(admin = it.admin.copy(catalog = Catalog.EQUIPMENT, items = emptyList(), total = 0, page = 0, totalPages = 0)) }
        setEditor(document, document)
    } } }
}
