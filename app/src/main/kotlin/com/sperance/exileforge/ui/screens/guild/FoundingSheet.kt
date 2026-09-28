package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.nameLength
import com.sperance.exileforge.core.model.guild.tagLength
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.GuildPatron
import com.sperance.exileforge.rules.content.GuildRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** What the founding form hands over: name, tag, patron, emblem, colour, mode and the level to join. */
internal typealias Founding = (String, String, String, String, String, GuildMode, Int) -> Unit

/**
 * Founding a guild: its name and tag, the patron — chosen for good, so each shows what it gives at the first level and
 * at the last — the arms, and the way in. The price and the level stand at the bottom; short of either, or with a name
 * the rules' lengths refuse, the button stays off and says why. It is held, as every purchase of gold is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun FoundingSheet(s: ForgeState, onDismiss: () -> Unit, onFound: Founding) {
    val rules = s.index?.guilds ?: GuildRules()
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var patron by remember { mutableStateOf(rules.patrons.firstOrNull()?.code.orEmpty()) }
    var emblem by remember { mutableStateOf(rules.emblems.firstOrNull().orEmpty()) }
    var color by remember { mutableStateOf(rules.colors.firstOrNull().orEmpty()) }
    var mode by remember { mutableStateOf(GuildMode.OPEN) }
    var minLevel by remember { mutableStateOf("1") }
    val money = s.hero?.money
    val refusal = when {
        s.heroLevel < rules.create.level -> ui("guild.found_level", rules.create.level)
        money != null && money < rules.create.gold -> ui("guild.found_gold", number(rules.create.gold.toDouble()))
        name.trim().length !in rules.nameLength -> ui("guild.found_name", rules.nameLength.first, rules.nameLength.last)
        tag.length !in rules.tagLength -> ui("guild.found_tag", rules.tagLength.first, rules.tagLength.last)
        patron.isBlank() -> ui("guild.api.patron")
        else -> null
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Engraved(ui("guild.found_title"))
            OutlinedTextField(name, { name = it.take(rules.nameLength.last) }, label = { Text(ui("guild.name")) }, singleLine = true,
                supportingText = { Text("${name.trim().length}/${rules.nameLength.last}") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(tag, { tag = it.filter(Char::isLetterOrDigit).uppercase().take(rules.tagLength.last) }, label = { Text(ui("guild.tag")) },
                singleLine = true, supportingText = { Text(ui("guild.tag_hint", rules.tagLength.first, rules.tagLength.last)) }, modifier = Modifier.fillMaxWidth())
            Engraved(ui("guild.patron"))
            MutedText(ui("guild.patron_forever"))
            rules.patrons.forEach { PatronCard(it, rules, chosen = it.code == patron) { patron = it.code } }
            Engraved(ui("guild.arms"))
            ArmsPicker(rules.emblems, rules.colors, emblem, color, enabled = true, onEmblem = { emblem = it }, onColor = { color = it })
            Engraved(ui("guild.entry"))
            ModePicker(mode, enabled = true) { mode = it }
            MinLevelField(minLevel, enabled = true) { minLevel = it }
            OrnateDivider()
            PropertyRow(ui("guild.found_price"), number(rules.create.gold.toDouble()), Glyph.CURRENCY)
            PropertyRow(ui("guild.found_level_row"), rules.create.level.toString(), Glyph.LEVEL)
            money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), Glyph.CURRENCY) }
            refusal?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
            HoldButton(ui("guild.found_for", number(rules.create.gold.toDouble())), Gold, Modifier.fillMaxWidth(), enabled = !s.busy && refusal == null,
                icon = ForgeGlyphs.Banner) {
                onFound(name.trim(), tag, patron, emblem, color, mode, (minLevel.toIntOrNull() ?: 1).coerceAtLeast(1))
            }
        }
    }
}

/** One patron to choose: name and theme, and its lines at the guild's first and last level. */
@Composable private fun PatronCard(patron: GuildPatron, rules: GuildRules, chosen: Boolean, onChoose: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().background(if (chosen) Gold.copy(alpha = .12f) else Color.Transparent, shape)
        .border(1.dp, if (chosen) Gold else Bronze, shape).clickable(onClick = onChoose).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PatronIcon(patron.icon, Modifier.size(22.dp))
            Text(GuildText.patron(patron.code), color = if (chosen) GoldBright else Parchment, style = MaterialTheme.typography.titleSmall)
        }
        GuildText.patronTheme(patron.code).takeIf { it.isNotBlank() }?.let { MutedText(it) }
        MutedText(ui("guild.bonus_at", 1))
        BonusLines(rules.bonus(patron.code, 1, 0))
        MutedText(ui("guild.bonus_at", rules.maxLevel))
        BonusLines(rules.bonus(patron.code, rules.maxLevel, 0), Vital)
    }
}
