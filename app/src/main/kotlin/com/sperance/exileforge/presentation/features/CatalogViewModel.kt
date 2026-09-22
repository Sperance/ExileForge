package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.update

class CatalogViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun query(value: String) { with(runtime) { mutable.update { it.copy(query = value) } } }

    fun catalog(value: Catalog) { with(runtime) {
        // A player's catalogue is the stash and nothing else: their own character is reached
        // through the menu, and other people's are an administrator's business.
        if (state.value.busy || state.value.editorOpen || !state.value.adminTools) return
        mutable.update { it.copy(catalog = value, items = emptyList(), page = 0, total = 0, totalPages = 0, query = "") }
        task {
            val saved = store.filters(state.value.server, value.path)?.let { WireJson.decodeFromString(CatalogFilter.serializer(), it) } ?: CatalogFilter()
            mutable.update { it.copy(filter = saved, query = saved.query) }
            loadPage(0)
        }
    } }

    fun filter(value: CatalogFilter) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(filter = value, query = value.query) } } }

    fun applyFilters() { with(runtime) { task {
        store.saveFilters(state.value.server, state.value.catalog.path, WireJson.encodeToString(CatalogFilter.serializer(), state.value.filter.copy(query = state.value.query)))
        loadPage(0)
    } } }

    fun refresh(page: Int = state.value.page) { with(runtime) {
        task { if (state.value.signedIn) loadPage(page) else mutable.update { it.copy(tab = 3, message = ui("catalog.sign_in")) } }
    } }

    fun count() { with(runtime) { task {
        val result = api.count(state.value.catalog)
        mutable.update { it.copy(message = ui("catalog.count", result)) }
    } } }

    fun open(id: String) { with(runtime) { task {
        val document = api.get(state.value.catalog, id) ?: error(ui("catalog.not_found"))
        ensureDefinitions()
        setEditor(document, document)
    } } }
}
