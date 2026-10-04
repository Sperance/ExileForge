package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.AdminHeroRow
import com.sperance.exileforge.core.network.AdminHeroSort
import com.sperance.exileforge.presentation.admin.AdminHeroesViewModel
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The administrator's heroes (3.81.0, server 1.76.0): every hero of every account — name, class, level, when it was made and the
 * account's login —, a search by name, the order by level, date or name, and a page at a time. A tap on a row opens its block:
 * a reason is required to block, and the block takes hold at once; lifting it needs none.
 */
@Composable internal fun HeroesAdminPage(account: AccountUi) {
    val vm = koinViewModel<AdminHeroesViewModel>()
    val state by vm.heroes.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load(0) }
    OutlinedTextField(
        state.query,
        vm::search,
        label = { Text(ui("admin.heroes_search")) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { vm.load(0) }),
        trailingIcon = { ForgeTextButton(onClick = { vm.load(0) }, enabled = !account.busy) { Text(ui("admin.heroes_find")) } },
        modifier = Modifier.fillMaxWidth(),
    )
    PillTabs(AdminHeroSort.entries.map { ui("admin.heroes_sort_${it.name.lowercase()}") }, state.sort.ordinal, { vm.sort(AdminHeroSort.entries[it]) }, segmented = true)
    val page = state.page
    MutedText(ui("admin.heroes_total", page.total))
    if (page.heroes.isEmpty()) MutedText(ui("admin.heroes_none"))
    page.heroes.forEach { HeroAdminRow(account, vm, it) }
    val pages = if (page.size > 0) ((page.total + page.size - 1) / page.size).toInt() else 1
    if (pages > 1) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ForgeTextButton(onClick = { vm.load(page.page - 1) }, enabled = !account.busy && page.page > 0) { Text("‹") }
            Text(ui("admin.heroes_page", page.page + 1, pages), color = Muted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            ForgeTextButton(onClick = { vm.load(page.page + 1) }, enabled = !account.busy && page.page + 1 < pages) { Text("›") }
        }
    }
}

@Composable private fun HeroAdminRow(account: AccountUi, vm: AdminHeroesViewModel, hero: AdminHeroRow) {
    var open by remember(hero.id) { mutableStateOf(false) }
    var reason by remember(hero.id, hero.blockReason) { mutableStateOf(hero.blockReason) }
    ForgePanel(Modifier.clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(hero.name, color = if (hero.blocked) LifeRed else GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(ui("pets.level", hero.level), color = Gold, style = MaterialTheme.typography.labelMedium)
        }
        MutedText(listOf(classTitle(hero.heroClass), hero.login, hero.createdAt.replace('T', ' ').take(16)).filter { it.isNotBlank() }.joinToString(" · "))
        if (hero.blocked) Text(ui("admin.heroes_blocked_line", hero.blockReason), color = LifeRed, style = MaterialTheme.typography.bodySmall)
        if (open) {
            if (!hero.blocked) {
                OutlinedTextField(
                    reason,
                    { reason = it.take(REASON) },
                    label = { Text(ui("admin.heroes_reason")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ForgeButton(
                enabled = !account.busy && (hero.blocked || reason.isNotBlank()),
                onClick = { vm.block(hero.id, !hero.blocked, reason) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(ui(if (hero.blocked) "admin.heroes_unblock" else "admin.heroes_block")) }
        }
    }
}

/** The longest reason a block keeps. */
private const val REASON = 300
