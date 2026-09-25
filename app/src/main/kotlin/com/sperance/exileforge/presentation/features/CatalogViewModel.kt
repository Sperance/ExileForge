package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.Reads
import kotlinx.coroutines.flow.update

class CatalogViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun query(value: String) = update { it.copy(admin = it.admin.copy(query = value)) }

    fun catalog(value: Catalog) { with(runtime) {
        // A player's catalogue is the stash and nothing else: their own character is reached
        // through the menu, and other people's are an administrator's business.
        if (state.value.busy || state.value.admin.editorOpen || !state.value.adminTools) return
        mutable.update { it.copy(admin = it.admin.copy(catalog = value, items = emptyList(), page = 0, total = 0, totalPages = 0, query = "")) }
        read(Reads.CATALOG, restart = true) {
            val saved = store.filters(state.value.account.server, value.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
            mutable.update { it.copy(admin = it.admin.copy(filter = saved, query = saved.query)) }
            loadPage(0)
        }
    } }

    fun filter(value: CatalogFilter) = update { it.copy(admin = it.admin.copy(filter = value, query = value.query)) }

    fun applyFilters() { with(runtime) { read(Reads.CATALOG, restart = true) {
        store.saveFilters(state.value.account.server, state.value.admin.catalog.path, WireJson.encodeToString(CatalogFilter.serializer(), state.value.admin.filter.copy(query = state.value.admin.query)))
        loadPage(0)
    } } }

    fun refresh(page: Int = state.value.admin.page) { with(runtime) {
        read(Reads.CATALOG, restart = true) { if (state.value.account.signedIn) loadPage(page) else mutable.update { it.copy(tab = 3, message = ui("catalog.sign_in"), error = true) } }
    } }

    fun count() { with(runtime) { read("${Reads.CATALOG}.count") {
        val result = api.catalog.count(state.value.admin.catalog)
        // The server answers the count as a bare number; anything else leaves the last total standing.
        val total = ((result as? kotlinx.serialization.json.JsonPrimitive)?.content ?: result.toString()).toDoubleOrNull()?.toLong()
        mutable.update { it.copy(admin = it.admin.copy(total = total ?: it.admin.total)) }
    } } }

    fun open(id: String) { with(runtime) { task(touches = setOf(Reads.CATALOG)) {
        val document = api.catalog.get(state.value.admin.catalog, id) ?: error(ui("catalog.not_found"))
        ensureWorld()
        setEditor(document, document)
    } } }
}
