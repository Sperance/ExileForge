package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.TesterAccount
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.server.AccountUi
import com.sperance.exileforge.presentation.session.SessionViewModel
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * The testing window (3.73.0): everything a hero earns by playing, granted to the hero in play at a tap — gold, level, points,
 * zones, items of every kind, maps, professions, recipes — and the resets. Testers and administrators only.
 */
@Composable internal fun TestingPage(account: AccountUi) {
    val heroModel: HeroViewModel = koinViewModel()
    val index = account.index
    val enabled = !account.busy && account.heroId.isNotBlank()
    if (account.heroId.isBlank()) {
        InfoCard(ui("tester.no_hero"), ui("tester.no_hero_hint"))
        return
    }
    MutedText(ui("tester.for_hero", account.heroName))
    ForgePanel {
        Engraved(ui("tester.currency"))
        NumberGrant(ui("tester.gold"), "1000000", account.inputs.number, enabled) { heroModel.testerGrant("gold", "amount" to it) }
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
        NumberGrant(ui("tester.amount"), "10", account.inputs.number, enabled && item.isNotBlank()) { heroModel.grantItem(item, it.toLong()) }
        var unique by remember { mutableStateOf("") }
        val uniques = index.templates.values.filter { it.rarity >= Rarity.UNIQUE }.associate { it.code to equipmentTitle(it.code) }
        Spinner(ui("tester.unique"), unique, uniques, enabled) { unique = it }
        GrantButton(ui("tester.give"), enabled && unique.isNotBlank()) { heroModel.grant(unique) }
        NumberGrant(ui("tester.rares"), "5", 2, enabled) { heroModel.testerGrant("rares", "count" to it) }
    }
    ForgePanel {
        Engraved(ui("tester.maps"))
        var zone by remember { mutableStateOf("") }
        var rarity by remember { mutableStateOf(Rarity.RARE) }
        Spinner(ui("tester.zone"), zone, index.zones.values.sortedBy { it.level }.associate { it.code.value to "${mapTitle(it.code)} · ${it.level}" }, enabled) { zone = it }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Rarity.COMMON, Rarity.MAGIC, Rarity.RARE).forEach { r ->
                FilterChip(selected = rarity == r, onClick = { rarity = r }, label = { Text(ui("enum.rarity.${r.name}")) })
            }
        }
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
        ForgeButton(enabled = enabled && value.isNotBlank(), onClick = { onGrant(value) }) { Text(ui("tester.give")) }
    }
}

@Composable private fun GrantButton(label: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    if (danger) {
        ForgeOutlinedButton(enabled = enabled, onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label, color = LifeRed) }
    } else {
        ForgeOutlinedButton(enabled = enabled, onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
    }
}

private val RESETS = listOf("TREE", "ATLAS", "BAG", "STASH", "CAMPAIGN")
