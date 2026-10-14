package com.sperance.exileforge.ui.screens.redemption

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.model.command.redemptionReward
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.admin.AdminViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.reward.RedemptionKind
import com.sperance.exileforge.rules.reward.RedemptionReward
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import org.koin.compose.viewmodel.koinViewModel

/**
 * Promo codes: what exists, and what each one pays out.
 *
 * An administrator's screen, opened by a button on the administrator's tab rather than by one of
 * its own in the bottom bar — a player never lists codes, only types one in on the Account tab.
 *
 * The reward is one list of mixed rows because that is how it is thought of: some experience, a
 * little gold and three Chaos Orbs is one gift, not three fields. Nothing here decides whether a
 * code is acceptable — a blank or duplicate code, an empty reward and a non-positive amount are
 * all the server's refusals, and a refusal is shown rather than pre-empted.
 */
@Composable fun RedemptionScreen() {
    val vm = koinViewModel<AdminViewModel>()
    val game by vm.game.collectAsStateWithLifecycle()
    val admin by vm.admin.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RedemptionCode?>(null) }

    PullToRefreshBox(isRefreshing = game.refreshing(Reads.REDEMPTIONS), onRefresh = vm::loadRedemptions, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                ScreenHeader(ui("redemption.title"), ui("redemption.count", admin.redemptions.size), ForgeGlyphs.Scroll)
            }
            item { NewCodePanel(game, vm) }
            if (admin.redemptions.isEmpty()) item { InfoCard(ui("redemption.empty"), ui("redemption.empty_hint")) }
            items(admin.redemptions, key = { it.id }) { code ->
                CodeCard(game, code, onDelete = { pendingDelete = code })
            }
        }
    }

    pendingDelete?.let { doomed ->
        ConfirmSheet(
            title = ui("redemption.delete_q"),
            subtitle = doomed.code,
            danger = true,
            icon = { Icon(ForgeGlyphs.Scroll, null, tint = LifeRed, modifier = Modifier.size(40.dp)) },
            note = ui("redemption.delete_text"),
            confirm = ui("common.delete"),
            onDismiss = { pendingDelete = null },
        ) {
            vm.deleteRedemption(doomed.id)
            pendingDelete = null
        }
    }
}

/** One stored code: what it is, how many took it, and what it gives. */
@Composable private fun CodeCard(game: GameUi, code: RedemptionCode, onDelete: () -> Unit) {
    ForgePanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Engraved(code.code)
            Spacer(Modifier.weight(1f))
            IconButton(enabled = !game.busy, onClick = onDelete) { Icon(Icons.Outlined.Delete, null, tint = Muted) }
        }
        code.description?.takeIf { it.isNotBlank() }?.let {
            MutedText(it)
        }
        PropertyRow(ui("redemption.used"), code.used.toString(), Glyph.LEVEL)
        code.expiredAt?.takeIf { it.isNotBlank() }?.let { PropertyRow(ui("redemption.expires"), it, Glyph.TEXT) }
        OrnateDivider(Gold)
        code.treasure.forEach { reward -> Text(rewardLine(reward), style = MaterialTheme.typography.labelMedium) }
    }
}

/**
 * One line of a reward, read by an administrator rather than by a player.
 *
 * A stack and a template are named by code (3.0.0), and the code has a name in the server's
 * dictionary — so the line reads as the player will see the gift, kind first: «Предмет · Сфера хаоса · 3 шт.».
 */
private fun rewardLine(reward: RedemptionReward): String {
    val amount = if (reward.quantity % 1.0 == 0.0) reward.quantity.toLong().toString() else reward.quantity.toString()
    return when (reward) {
        is RedemptionReward.Experience -> ui("redemption.line_experience", amount)
        is RedemptionReward.Gold -> ui("redemption.line_gold", amount)
        is RedemptionReward.Item -> "${ui("enum.reward.ITEM")} · ${itemTitle(reward.code)} · ${ui("auction.pieces", amount)}"
        is RedemptionReward.Equipment -> "${ui("enum.reward.EQUIPMENT")} · ${equipmentTitle(reward.template)} · ${ui("auction.pieces", amount)}"
    }
}

/**
 * What a reward of [kind] may name, out of the content on screen: every stacking item by category,
 * or every template by slot and level, keyed by code. Empty for the kinds that name nothing, and
 * before the content has been read.
 */
private fun rewardOptions(game: GameUi, kind: RedemptionKind): Map<String, String> {
    val index = game.index ?: return emptyMap()
    return when (kind) {
        RedemptionKind.ITEM -> index.items.values.sortedWith(compareBy({ it.category }, { itemTitle(it.code.value) }))
            .associate { it.code.value to itemTitle(it.code.value) }

        RedemptionKind.EQUIPMENT -> index.templates.values.sortedWith(compareBy({ it.slot }, { it.level }))
            .associate { it.code to "${equipmentTitle(it.code)} · ${slotTitle(it.slot, game.lang)} · ${it.level}" }

        RedemptionKind.EXPERIENCE, RedemptionKind.GOLD -> emptyMap()
    }
}

/**
 * The form that makes a code.
 *
 * The reward is built row by row and held here until the code is sent: a half-written list is not
 * worth a request, and the server takes the whole thing in one document anyway.
 */
@Composable private fun NewCodePanel(game: GameUi, vm: AdminViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var rewards by remember { mutableStateOf(emptyList<RedemptionReward>()) }

    var kind by remember { mutableStateOf(RedemptionKind.ITEM) }
    var item by remember { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("1") }

    val needsCode = kind == RedemptionKind.ITEM || kind == RedemptionKind.EQUIPMENT

    ForgePanel {
        Engraved(ui("redemption.new"))
        OutlinedTextField(
            code,
            { code = it },
            label = { Text(ui("account.code")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            description,
            { description = it },
            label = { Text(ui("form.description")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OrnateDivider(Gold)
        Engraved(ui("redemption.reward"))
        Spinner(
            ui("redemption.kind"),
            kind.name,
            RedemptionKind.entries.associate { it.name to ui("enum.reward.${it.name}") },
            !game.busy,
            glyph = Glyph.CURRENCY,
        ) {
            kind = RedemptionKind.valueOf(it)
            item = ""
        }
        // The goods are picked from the content the hero reads (3.0.0): a stack of the bag or a template, by code.
        if (needsCode) Spinner(ui("enum.reward.${kind.name}"), item, rewardOptions(game, kind), !game.busy, glyph = Glyph.ITEM) { item = it }
        OutlinedTextField(
            amount,
            { amount = it },
            label = { Text(ui("auction.amount")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        ForgeOutlinedButton(
            enabled = !game.busy && (!needsCode || item.isNotBlank()) && amount.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                rewards = rewards + redemptionReward(kind, item, amount.toDoubleOrNull() ?: 0.0)
                item = ""
                amount = "1"
            },
        ) {
            Icon(Icons.Outlined.Add, null)
            Text(ui("redemption.add_reward"))
        }

        if (rewards.isEmpty()) MutedText(ui("redemption.no_rewards"))
        rewards.forEachIndexed { index, reward ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(rewardLine(reward), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { rewards = rewards.filterIndexed { at, _ -> at != index } }) {
                    Icon(Icons.Outlined.Delete, null, tint = Muted)
                }
            }
        }

        ForgeButton(
            enabled = !game.busy && code.isNotBlank() && rewards.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                vm.createRedemption(
                    RedemptionCode(
                        code = code.trim(),
                        description = description.takeIf { it.isNotBlank() },
                        treasure = rewards,
                    ),
                )
                code = ""
                description = ""
                rewards = emptyList()
            },
        ) {
            Text(ui("redemption.create"))
        }
    }
}
