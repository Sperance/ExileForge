package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.TesterAccount
import com.sperance.exileforge.rules.content.MAP_TEMPLATE
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.hero.HeroViewModel
import com.sperance.exileforge.ui.screens.session.SessionViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The testing window: everything a hero earns by playing, granted to the hero in play at a tap — gold, experience, level,
 * points, zones, items of every kind, maps, professions, recipes — and the resets. Testers and administrators only.
 *
 * Стопка за раз - не больше `moderation.grantStack`; редкости выдачи - по шаблону (`ItemFactory.grantable`). Случайная вещь - шаблон
 * выбранных редкости и места (пусто - любые), вещь по шаблону - на выбранной редкости, если шаблону дано больше одной.
 * Герою-тестировщику сверху - переключатель «Свободный прогресс» (`hero/tester/freeProgress`, не выдача: только сам тестировщик, и от
 * администратора тоже): включён - замки уровня и прогресса открыты в клиенте и на сервере, выключен (по умолчанию) - тестировщик идёт
 * по прогрессу, как игрок.
 */
@Composable internal fun TestingPage(account: AccountUi) {
    val heroModel: HeroViewModel = koinViewModel()
    val index = account.index
    val enabled = !account.busy && account.heroId.isNotBlank()
    if (account.heroId.isBlank()) {
        InfoCard(ui("tester.no_hero"), ui("tester.no_hero_hint"))
        return
    }
    TrailData(account.heroName, Tip("", ui("tester.for_hero", account.heroName), Muted))
    if (account.isTester) FreeProgress(account.freeProgress, enabled) { heroModel.freeProgress(it) }
    ForgePanel {
        Engraved(ui("tester.currency"))
        NumberGrant(ui("tester.gold"), "1000000", account.inputs.number, enabled) { heroModel.testerGrant("gold", "amount" to it) }
        NumberGrant(ui("tester.experience"), "100000", account.inputs.number, enabled) { heroModel.addExperience(it.toDouble()) }
        NumberGrant(ui("tester.level"), "100", 3, enabled) { heroModel.testerGrant("level", "level" to it) }
        NumberGrant(ui("tester.skill_points"), "10", 4, enabled) { heroModel.testerGrant("skillPoints", "amount" to it) }
        GrantButton(ui("tester.atlas_points"), enabled) { heroModel.testerGrant("atlasPoints") }
        GrantButton(ui("tester.zones"), enabled) { heroModel.testerGrant("zones") }
    }
    if (index == null) return
    ForgePanel {
        Engraved(ui("tester.items"))
        var item by remember { mutableStateOf("") }
        Spinner(ui("tester.item"), item, index.items.keys.associate { it.value to itemTitle(it.value) }, enabled) { item = it }
        NumberGrant(ui("tester.amount_cap", index.rules.moderation.grantStack), "10", account.inputs.number, enabled && item.isNotBlank()) { heroModel.grantItem(item, it.toLong()) }
        NumberGrant(ui("tester.rares"), "5", 2, enabled) { heroModel.testerGrant("rares", "count" to it) }
    }
    ForgePanel {
        Engraved(ui("tester.random_item"))
        val any = ui("tester.any")
        var rarity by remember { mutableStateOf("") }
        var slot by remember { mutableStateOf("") }
        Spinner(ui("common.rarity"), rarity, mapOf("" to any) + Rarity.entries.associate { it.name to rarityTitle(it, account.lang) }, enabled) { rarity = it }
        Spinner(ui("tester.slot"), slot, mapOf("" to any) + Slot.entries.associate { it.name to slotTitle(it, account.lang) }, enabled) { slot = it }
        GrantButton(ui("tester.give"), enabled) { heroModel.grantRandom(rarity, slot) }
    }
    ForgePanel {
        Engraved(ui("tester.template"))
        var template by remember { mutableStateOf("") }
        val templates = remember(index, account.lang) { index.templates.values.sortedBy { it.code }.associate { it.code to "${equipmentTitle(it.code)} · ${it.code}" } }
        Spinner(ui("tester.template_item"), template, templates, enabled) { template = it }
        val rarities = remember(index, template) { index.template(template)?.let { ItemFactory(index).grantable(it) }.orEmpty() }
        var picked by remember(template) { mutableStateOf<Rarity?>(null) }
        val rarity = picked?.takeIf { it in rarities } ?: rarities.lastOrNull()
        if (rarities.size > 1) RarityChips(rarities, rarity) { picked = it }
        GrantButton(ui("tester.give"), enabled && rarity != null) { heroModel.grant(template, rarity) }
    }
    ForgePanel {
        Engraved(ui("tester.maps"))
        var zone by remember { mutableStateOf("") }
        var rarity by remember { mutableStateOf(Rarity.RARE) }
        val rarities = remember(index) { index.template(MAP_TEMPLATE)?.let { ItemFactory(index).grantable(it) }.orEmpty() }
        Spinner(ui("tester.zone"), zone, index.zones.values.sortedBy { it.level }.associate { it.code.value to "${mapTitle(it.code)} · ${it.level}" }, enabled) { zone = it }
        RarityChips(rarities, rarity) { rarity = it }
        GrantButton(ui("tester.give_map"), enabled && zone.isNotBlank()) { heroModel.testerGrant("map", "zone" to zone, "rarity" to rarity.name) }
    }
    ForgePanel {
        Engraved(ui("tester.crafts"))
        var profession by remember { mutableStateOf("") }
        Spinner(
            ui("tester.profession"),
            profession,
            mapOf("" to ui("tester.all_professions")) +
                index.professions.professions.associate { it.code to professionTitle(it.code) },
            enabled,
        ) { profession = it }
        NumberGrant(ui("tester.profession_level"), index.professions.rules.maxLevel.toString(), 3, enabled) {
            heroModel.testerGrant("profession", "code" to profession, "level" to it)
        }
        GrantButton(ui("tester.recipes"), enabled) { heroModel.testerGrant("recipes") }
    }
    ForgePanel {
        Engraved(ui("tester.resets"))
        var reset by remember { mutableStateOf<String?>(null) }
        RESETS.forEach { what -> GrantButton(ui("tester.reset_${what.lowercase()}"), enabled, danger = true) { reset = what } }
        reset?.let { what ->
            ConfirmSheet(ui("tester.reset_${what.lowercase()}"), ui("tester.reset_confirm"), onDismiss = { reset = null }, danger = true) {
                reset = null
                heroModel.testerGrant("reset", "what" to what)
            }
        }
    }
}

/** Переключатель свободного прогресса тестировщика: [on] - замки прогресса открыты; ответ сервера приходит снимком героя. */
@Composable private fun FreeProgress(on: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    ForgePanel {
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(ui("tester.free_progress"), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                Text(ui("tester.free_progress_hint"), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Switch(checked = on, enabled = enabled, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Elder))
        }
    }
}

/** A number and the button that grants it; [initial] stands in the field until changed. */
@Composable private fun NumberGrant(label: String, initial: String, limit: Int, enabled: Boolean, onGrant: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value,
            { value = it.filter(Char::isDigit).take(limit) },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        ForgeButton(state = ButtonState.of(enabled && value.isNotBlank()), onClick = { onGrant(value) }) { Text(ui("tester.give")) }
    }
}

/** Выбор редкости выдачи из [rarities], что даёт шаблон. */
@Composable private fun RarityChips(rarities: List<Rarity>, selected: Rarity?, onSelect: (Rarity) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        rarities.forEach { r -> ForgeChip(ui("enum.rarity.${r.name}"), selected == r, { onSelect(r) }, mode = ChipMode.Choice) }
    }
}

@Composable private fun GrantButton(label: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    if (danger) {
        ForgeButton(state = ButtonState.of(enabled), onClick = onClick, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Danger, icon = Icons.Outlined.Close) { Text(label) }
    } else {
        ForgeButton(state = ButtonState.of(enabled), onClick = onClick, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary) { Text(label) }
    }
}

private val RESETS = listOf("TREE", "ATLAS", "BAG", "STASH", "CAMPAIGN")
