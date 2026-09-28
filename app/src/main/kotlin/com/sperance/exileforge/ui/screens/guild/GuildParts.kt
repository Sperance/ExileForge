package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.GuildBonus
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * A guild's arms are a code of the rules' dozen and a colour of its eight; the client draws each code with a glyph of its
 * own, the same on every device. A code the client does not know yet takes a glyph by its hash.
 */
private val EmblemGlyphs = mapOf("SWORD" to ForgeGlyphs.Swords, "SHIELD" to ForgeGlyphs.Kite, "AXE" to ForgeGlyphs.Shard, "HAMMER" to ForgeGlyphs.Anvil,
    "CROWN" to ForgeGlyphs.Gem, "SKULL" to ForgeGlyphs.Skull, "WOLF" to ForgeGlyphs.Exile, "EAGLE" to ForgeGlyphs.Sigil, "TOWER" to ForgeGlyphs.Keep,
    "FLAME" to ForgeGlyphs.Rift, "SUN" to ForgeGlyphs.Orb, "COIN" to ForgeGlyphs.Coins)

internal fun emblemGlyph(code: String): ImageVector =
    EmblemGlyphs[code] ?: EmblemGlyphs.values.elementAt(abs(code.hashCode()) % EmblemGlyphs.size)

/** A `#RRGGBB` of the rules; anything unreadable is the app's own gold. */
internal fun guildColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Gold)

@Composable internal fun GuildEmblem(emblem: String, color: String, size: Dp = 44.dp) {
    val tint = guildColor(color)
    val shape = RoundedCornerShape(size / 4)
    Box(Modifier.size(size).background(Brush.verticalGradient(listOf(tint.copy(alpha = .32f), Panel)), shape).border(1.dp, tint, shape),
        contentAlignment = Alignment.Center) {
        Icon(emblemGlyph(emblem), GuildText.emblem(emblem), tint = tint, modifier = Modifier.size(size * .6f))
    }
}

/** The arms being chosen: the dozen emblems in the colour picked, and the eight colours under them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun ArmsPicker(emblems: List<String>, colors: List<String>, emblem: String, color: String, enabled: Boolean,
    onEmblem: (String) -> Unit, onColor: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        emblems.forEach { code ->
            Box(Modifier.border(2.dp, if (code == emblem) GoldBright else Color.Transparent, RoundedCornerShape(12.dp)).padding(2.dp)
                .clickable(enabled = enabled, onClickLabel = GuildText.emblem(code)) { onEmblem(code) }) { GuildEmblem(code, color, 40.dp) }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { hex ->
            Box(Modifier.size(30.dp).border(2.dp, if (hex == color) GoldBright else Bronze, CircleShape).padding(4.dp)
                .background(guildColor(hex), CircleShape).clickable(enabled = enabled) { onColor(hex) })
        }
    }
}

/** A patron's sign: its icon is a key of the server's icon set (`stat.<power>`), a bundled glyph where the set has none. */
@Composable internal fun PatronIcon(icon: String, modifier: Modifier) = StatIcon(icon.removePrefix("stat."), Gold, modifier)

/** How a hero gets in, one chip each, and a line under them saying what the chosen one means. */
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun ModePicker(mode: GuildMode, enabled: Boolean, onChange: (GuildMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GuildMode.entries.forEach { FilterChip(selected = it == mode, enabled = enabled, onClick = { onChange(it) }, label = { Text(GuildText.mode(it)) }) }
    }
    Text(modeHint(mode), color = Muted, style = MaterialTheme.typography.bodySmall)
}

private fun modeHint(mode: GuildMode): String = when (mode) {
    GuildMode.OPEN -> ui("guild.mode_hint_open")
    GuildMode.APPLY -> ui("guild.mode_hint_apply")
    GuildMode.INVITE -> ui("guild.mode_hint_invite")
}

/** The level a hero needs to join, typed as digits. */
@Composable internal fun MinLevelField(value: String, enabled: Boolean, onChange: (String) -> Unit) =
    OutlinedTextField(value, { onChange(it.filter(Char::isDigit).take(3)) }, label = { Text(ui("guild.min_level")) }, enabled = enabled,
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

/** A patron's bonus as sentences — its lines in the server's own words where its dictionary has them, and the trade discount. */
@Composable internal fun BonusLines(bonus: GuildBonus, tint: Color = Parchment) {
    if (bonus.lines.isEmpty() && bonus.discount <= 0) Text(ui("guild.bonus_none"), color = Muted, style = MaterialTheme.typography.bodySmall)
    bonus.lines.forEach { Text("• " + SkillText.statLine(it.stat, it.op, it.value), color = tint, style = MaterialTheme.typography.bodyMedium) }
    if (bonus.discount > 0) Text("• " + ui("guild.discount_line", number(bonus.discount)), color = tint, style = MaterialTheme.typography.bodyMedium)
}

/** When a member was last seen: «в игре», minutes, hours or days ago. */
internal fun seenText(at: Long): String {
    if (at <= 0) return ui("guild.seen_never")
    val minutes = ((System.currentTimeMillis() - at) / 60_000).coerceAtLeast(0)
    return when {
        minutes < 5 -> ui("guild.seen_now")
        minutes < 60 -> ui("guild.seen_minutes", minutes)
        minutes < 24 * 60 -> ui("guild.seen_hours", minutes / 60)
        else -> ui("guild.seen_days", minutes / (24 * 60))
    }
}

/** An epoch-millisecond moment by the device's clock, «dd.MM HH:mm». */
internal fun clockText(at: Long): String =
    if (at <= 0) "" else Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))
