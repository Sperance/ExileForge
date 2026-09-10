package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import kotlinx.serialization.json.*

@Composable internal fun CatalogSwitch(s: ForgeState, vm: ForgeViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Catalog.entries.forEach { catalog -> FilterChip(selected = s.catalog == catalog,
            enabled = !s.busy && !s.editorOpen, onClick = { vm.catalog(catalog) }, label = { Text(catalog.title) }) }
    }
}
