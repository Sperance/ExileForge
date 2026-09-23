package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.icons.propertyIcon
import com.sperance.exileforge.ui.theme.Gold

@Composable internal fun CatalogSwitch(s: ForgeState, vm: ForgeViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Catalog.entries.forEach { catalog -> FilterChip(selected = s.admin.catalog == catalog,
            enabled = !s.busy && !s.admin.editorOpen, onClick = { vm.catalog(catalog) },
            leadingIcon = { Icon(propertyIcon(catalog.path), null, tint = Gold, modifier = Modifier.size(16.dp)) },
            label = { Text(catalog.title(s.lang)) }) }
    }
}
