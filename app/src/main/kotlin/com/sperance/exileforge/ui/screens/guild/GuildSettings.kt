package com.sperance.exileforge.ui.screens.guild

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.guildEntryLevel
import com.sperance.exileforge.core.display.GuildText
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildMember
import com.sperance.exileforge.core.model.guild.GuildView
import com.sperance.exileforge.rules.content.GuildAction
import com.sperance.exileforge.rules.content.GuildPolicy
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** Выход из гильдии: участник уходит, глава распускает её (уйти главе, пока есть другие, нельзя - сначала передать главенство). */
private enum class Exit(val label: String, val question: String) {
    LEAVE("guild.leave", "guild.leave_q"),
    DISBAND("guild.disband", "guild.disband_q"),
}

/**
 * Меню «⋮» в шапке зала гильдии: выход - покинуть гильдию или, тому, кому таблица [GuildPolicy] даёт роспуск,
 * распустить её; каждое спрашивается ещё раз ([ConfirmSheet]).
 */
@Composable internal fun GuildExitMenu(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    me ?: return
    val rules = game.index?.guilds
    val way = if (GuildPolicy.can(me.role, GuildAction.DISBAND)) Exit.DISBAND else Exit.LEAVE
    var open by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    Box {
        ForgeIconButton(Icons.Outlined.MoreVert, ui("common.more"), onClick = { open = true })
        ForgeMenu(open, onDismissRequest = { open = false }, containerColor = PanelRaised) {
            DropdownMenuItem(
                text = { Text(ui(way.label), color = LifeRed) },
                enabled = !game.busy,
                onClick = {
                    open = false
                    asking = true
                },
            )
        }
    }
    if (asking) {
        ConfirmSheet(
            ui(way.question),
            ui(way.label),
            onDismiss = { asking = false },
            subtitle = GuildText.title(guild.name, guild.tag),
            note = when (way) {
                Exit.LEAVE -> ui("guild.leave_note", rules?.rejoinHours ?: 24)
                Exit.DISBAND -> ui("guild.disband_note")
            },
            danger = true,
            busy = game.busy,
            onConfirm = {
                when (way) {
                    Exit.LEAVE -> vm.leave()
                    Exit.DISBAND -> vm.disband()
                }
            },
        )
    }
}

/**
 * Настройки гильдии (раздел только главы - [com.sperance.exileforge.core.guild.GuildTab.SETTINGS]) одной панелью: режим приёма,
 * уровень героя степпером не ниже порога открытия гильдий и объявление; герб и цвет задаются только при основании. Правки
 * сохраняет одна кнопка подвала ([SaveBar]) одним запросом; правленое и не сохранённое помечено. Выход - в меню шапки зала
 * ([GuildExitMenu]).
 */
@Composable internal fun SettingsTab(game: GameUi, vm: GuildViewModel, guild: GuildView, me: GuildMember?) {
    val leader = me != null && GuildPolicy.can(me.role, GuildAction.SETTINGS)
    val floor = guildEntryLevel(game.index?.rules)
    val savedLevel = guild.minLevel.coerceAtLeast(floor)
    val limit = game.index?.guilds?.announcement ?: 200
    var mode by remember(guild.mode) { mutableStateOf(guild.mode) }
    var minLevel by remember(savedLevel) { mutableIntStateOf(savedLevel) }
    var announcement by remember(guild.announcement) { mutableStateOf(guild.announcement) }
    val modeChanged = mode != guild.mode
    val levelChanged = minLevel != savedLevel
    val textChanged = announcement.trim() != guild.announcement
    val changes = listOf(modeChanged, levelChanged, textChanged).count { it }
    val editable = leader && !game.busy
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(vertical = 10.dp)) {
            ForgePanel {
                Caption(ui("guild.entry"), modeChanged)
                ModePicker(mode, editable) { mode = it }
                OrnateDivider()
                MinLevelStepper(minLevel, floor, editable, changed = levelChanged) { minLevel = it }
                OrnateDivider()
                Caption(ui("guild.announcement"), textChanged)
                OutlinedTextField(
                    announcement,
                    { announcement = it.take(limit) },
                    enabled = editable,
                    minLines = 2,
                    maxLines = 5,
                    supportingText = { Text("${announcement.length}/$limit", Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (leader) SaveBar(changes, game.busy, scroll) { vm.settings(mode, minLevel, announcement.trim()) }
    }
}

/** Подпись группы настроек; [changed] - справа «• изменено», пока правка не сохранена. */
@Composable private fun Caption(text: String, changed: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Engraved(text, modifier = Modifier.weight(1f))
        if (changed) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ChangedDot()
                Text(ui("guild.changed"), color = Gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Подвал настроек, прижатый к низу раздела над нижней панелью игры: «Сохранить · N изменений» шлёт все правки одним запросом;
 * без правок кнопка погашена - «Всё сохранено». Черта сверху - пока под подвалом есть ещё форма ([ScrollState.canScrollForward],
 * читается в рисовании).
 */
@Composable private fun SaveBar(changes: Int, busy: Boolean, scroll: ScrollState, onSave: () -> Unit) {
    val dirty = changes > 0
    Box(
        Modifier.fillMaxWidth()
            .drawBehind { if (scroll.canScrollForward) drawLine(Bronze, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(top = 8.dp, bottom = 10.dp),
    ) {
        ForgeButton(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            state = ButtonState.of(dirty, busy = busy && dirty),
            icon = Icons.Outlined.Check.takeUnless { dirty },
        ) { Text(if (dirty) ui("guild.save_n", changes, plural("guild.change", changes)) else ui("guild.saved_all")) }
    }
}
