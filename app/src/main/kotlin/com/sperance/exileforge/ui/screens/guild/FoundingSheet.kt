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
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildFaction
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.GuildRules
import com.sperance.exileforge.rules.text.NameCharset
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.quests.weekText
import com.sperance.exileforge.ui.theme.*
import java.util.concurrent.TimeUnit

/** What the founding form hands over: name, tag, faction, emblem, colour, mode and the level to join. */
internal typealias Founding = (String, String, String, String, String, GuildMode, Int) -> Unit

/**
 * Founding a guild: its name and tag, the faction — chosen for good, a banner with no bonus, each in its own colour —
 * the arms, and the way in. The price and the level stand at the bottom; short of either, or with a name
 * the rules' lengths refuse, the button stays off and says why. It is held, as every purchase of gold is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoundingSheet(game: GameUi, onDismiss: () -> Unit, onFound: Founding) {
    val rules = game.index?.guilds ?: GuildRules()
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var faction by remember { mutableStateOf(rules.factions.firstOrNull()?.code.orEmpty()) }
    var emblem by remember { mutableStateOf(rules.emblems.firstOrNull().orEmpty()) }
    var color by remember { mutableStateOf(rules.colors.firstOrNull().orEmpty()) }
    var mode by remember { mutableStateOf(GuildMode.OPEN) }
    var minLevel by remember { mutableStateOf("1") }
    val money = game.hero?.money
    val price = rules.create.price.of(game.heroLevel)
    // Возраст героя (4.2.0): с создания должно пройти `create.heroDays` дней; тестировщика уровень и возраст не держат
    val readyAt = game.heroRow?.createdAtMs?.let { it + TimeUnit.DAYS.toMillis(rules.create.heroDays.toLong()) }
    val refusal = when {
        !game.isTester && game.heroLevel < rules.create.level -> ui("guild.found_level", rules.create.level)
        !game.isTester && readyAt != null && readyAt > System.currentTimeMillis() -> ui("guild.found_age", rules.create.heroDays, weekText(readyAt))
        money != null && money < price -> ui("guild.found_gold", number(price.toDouble()))
        name.trim().length !in rules.nameLength -> ui("guild.found_name", rules.nameLength.first, rules.nameLength.last)
        tag.length !in rules.tagLength -> ui("guild.found_tag", rules.tagLength.first, rules.tagLength.last)
        faction.isBlank() -> ui("guild.api.faction")
        else -> null
    }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Engraved(ui("guild.found_title"))
            OutlinedTextField(
                name,
                { name = NameCharset.filter(it).take(rules.nameLength.last) },
                label = { Text(ui("guild.name")) },
                singleLine = true,
                supportingText = { Text(ui("common.name_charset") + " · ${name.trim().length}/${rules.nameLength.last}") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                tag,
                { tag = it.filter(Char::isLetterOrDigit).uppercase().take(rules.tagLength.last) },
                label = { Text(ui("guild.tag")) },
                singleLine = true,
                supportingText = { Text(ui("guild.tag_hint", rules.tagLength.first, rules.tagLength.last)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Engraved(ui("guild.faction"))
            MutedText(ui("guild.faction_forever"))
            rules.factions.forEach { FactionCard(it, rules, chosen = it.code == faction) { faction = it.code } }
            Engraved(ui("guild.arms"))
            ArmsPicker(rules.emblems, rules.colors, emblem, color, enabled = true, onEmblem = { emblem = it }, onColor = { color = it })
            Engraved(ui("guild.entry"))
            ModePicker(mode, enabled = true) { mode = it }
            MinLevelField(minLevel, enabled = true) { minLevel = it }
            OrnateDivider()
            PropertyRow(ui("guild.found_price"), number(price.toDouble()), Glyph.CURRENCY)
            PropertyRow(ui("guild.found_level_row"), rules.create.level.toString(), Glyph.LEVEL)
            money?.let { PropertyRow(ui("merchant.gold"), number(it.toDouble()), Glyph.CURRENCY) }
            refusal?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
            HoldButton(
                ui("guild.found_for", number(price.toDouble())),
                Gold,
                Modifier.fillMaxWidth(),
                enabled = !game.busy && refusal == null,
                icon = ForgeGlyphs.Banner,
            ) {
                onFound(name.trim(), tag, faction, emblem, color, mode, (minLevel.toIntOrNull() ?: 1).coerceAtLeast(1))
            }
        }
    }
}

/** One faction to choose: its sign, name and word, the card lit in the faction's own colour when chosen. */
@Composable private fun FactionCard(faction: GuildFaction, rules: GuildRules, chosen: Boolean, onChoose: () -> Unit) {
    val tint = factionColor(faction)
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().background(if (chosen) tint.copy(alpha = .14f) else Color.Transparent, shape)
            .border(if (chosen) 2.dp else 1.dp, if (chosen) tint else tint.copy(alpha = .45f), shape)
            .clickable(onClickLabel = GuildText.faction(faction.code), onClick = onChoose).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FactionIcon(faction.code, rules, Modifier.size(28.dp))
            Text(GuildText.faction(faction.code), color = tint, style = MaterialTheme.typography.titleSmall)
        }
        GuildText.factionLore(faction.code).takeIf { it.isNotBlank() }?.let { MutedText(it) }
    }
}
