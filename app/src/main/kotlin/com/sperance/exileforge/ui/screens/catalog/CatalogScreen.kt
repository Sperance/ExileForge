package com.sperance.exileforge.ui.screens.catalog

import androidx.compose.foundation.layout.*
import com.sperance.exileforge.presentation.state.Reads
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs

@Composable internal fun CatalogScreen(s: ForgeState, vm: ForgeViewModel) {
    // The gesture replaced the Refresh button that used to sit beside Create: one way to do one
    // thing, and paging stays a pair of buttons because a page is a place, not a reload.
    PullToRefreshBox(isRefreshing = s.refreshing(Reads.CATALOG), onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader(if (s.admin.catalog == Catalog.CHARACTERS) ui("catalog.characters") else ui("catalog.stash"),
                ui("catalog.page_info", s.admin.total, s.admin.page + 1, maxOf(1, s.admin.totalPages)),
                if (s.admin.catalog == Catalog.CHARACTERS) ForgeGlyphs.Exile else ForgeGlyphs.Stash)
            if (s.adminTools) CatalogSwitch(s, vm)
            if (s.admin.editorOpen) MutedText(ui("catalog.editor_busy"))
        }
        item {
            ForgePanel {
                Engraved(ui("common.search"))
                OutlinedTextField(s.admin.query, vm::query, label = { Text(ui("common.search_catalog")) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (s.admin.catalog == Catalog.EQUIPMENT) CatalogFilters(s, vm)
                Button(enabled = !s.busy && s.account.signedIn, onClick = vm::applyFilters, modifier = Modifier.fillMaxWidth()) { Text(ui("common.find")) }
                EntitySpinner(ui("catalog.open_record"), "", when (s.admin.catalog) {
                    Catalog.CHARACTERS -> EntitySource.CHARACTER
                    Catalog.EQUIPMENT -> EntitySource.EQUIPMENT
                    Catalog.ITEMS -> EntitySource.ITEM
                }, !s.busy && !s.admin.editorOpen && s.account.signedIn, vm::open)
                Button(enabled = !s.busy && !s.admin.editorOpen && s.canEdit, onClick = { vm.create() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null); Text(ui("common.create"))
                }
            }
        }
        val visible = s.admin.items
        if (visible.isEmpty()) item { InfoCard(if (s.refreshing(Reads.CATALOG)) ui("common.loading") else ui("catalog.empty"), ui("catalog.empty_hint")) }
        items(visible, key = { it.entityId }) { doc ->
            ItemCard(doc, enabled = !s.busy && !s.admin.editorOpen, definitions = s.world.definitions) { vm.open(doc.entityId) }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(enabled = !s.busy && s.admin.page > 0, onClick = { vm.refresh(s.admin.page - 1) }) { Text(ui("common.back")) }
                OutlinedButton(enabled = !s.busy && s.admin.page + 1 < s.admin.totalPages, onClick = { vm.refresh(s.admin.page + 1) }) { Text(ui("common.next")) }
            }
        }
    }
    }
}
