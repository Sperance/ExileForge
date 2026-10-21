package com.sperance.exileforge.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.DamageNumbers
import com.sperance.exileforge.core.campaign.run.FightReport
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.screens.expedition.arena.FightLogSheet
import com.sperance.exileforge.ui.screens.server.ChoiceRow
import com.sperance.exileforge.ui.screens.server.SettingsViewModel
import com.sperance.exileforge.ui.screens.server.SwitchRow
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import org.koin.compose.viewmodel.koinViewModel

/**
 * Системные листы поверх всего: отчёт об ошибке, настройки и лист отчётов. Их открывает и оболочка, и забег, накрывший её; пока
 * открыт отчёт об ошибке или настройки, забег под ним держится ([RunMenu]). Новый лист - поле здесь и его хозяин в `ForgeApp`.
 */
@Stable
class SystemSheets {
    var bug by mutableStateOf(false)
    var settings by mutableStateOf(false)

    /** Лист отчётов: отчёт, к которому он прокручен; null - закрыт. */
    var reports by mutableStateOf<ReportsView?>(null)

    val open: Boolean get() = bug || settings
}

val LocalSystemSheets = staticCompositionLocalOf<SystemSheets?> { null }

/**
 * Журнал последнего боя забега: режим (поход, Разлом, башня, раш) кладёт свой отчёт, а [RunMenu] - где бы оно ни
 * стояло, на карте или посреди боя - сам добавляет пункт «Журнал боя»; null - боя ещё не было, пункта нет.
 */
val LocalFightReport = compositionLocalOf<FightReport?> { null }

/**
 * Краски меню забега и строк настроек в нём: лист [sheet], заголовок и знаки пунктов [title], знаки, переключатель и выбранный сегмент
 * настроек [accent] (слова на нём - [onAccent]), подложка сегментов [well], слова [text], сноски [muted], опасный пункт [danger],
 * разделитель [line]. По умолчанию - краски Кузницы; Разлом даёт свои.
 */
@Immutable
data class MenuPalette(
    val sheet: Color = PanelRaised,
    val title: Color = GoldBright,
    val accent: Color = Gold,
    val onAccent: Color = Ink,
    val well: Color = PanelRaised,
    val text: Color = Parchment,
    val muted: Color = Muted,
    val danger: Color = LifeRed,
    val line: Color = Bronze.copy(alpha = .5f),
)

/** Краски строк настроек ([MenuPalette]): в меню забега - краски режима, вне его - Кузницы. */
val LocalMenuPalette = staticCompositionLocalOf { MenuPalette() }

/** Пункт меню забега, свой у каждого режима: выйти из захода, мутаторы Разлома, закрыть Разлом. */
data class RunMenuItem(
    val icon: ImageVector,
    val label: String,
    val note: String? = null,
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * Меню забега - одно на поход, Разлом и испытания: быстрые настройки боя, «Все
 * настройки» поверх забега, «Журнал боя» последнего боя ([LocalFightReport]) и пункты режима [items]; [extra] - свой блок режима над ними (мутаторы),
 * [badge] - свои данные справа от заголовка (срок недели). [palette] - краски листа, строк и пунктов.
 * Пока меню или открытый из него системный лист на экране, [hold] держит забег. Сразу за кнопкой меню ([trigger]) - кружок ждущего
 * жёсткого обновления ([UpdateMark]): режимы его не ставят.
 */
@Composable fun RunMenu(
    items: List<RunMenuItem>,
    hold: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    palette: MenuPalette = MenuPalette(),
    trigger: @Composable (onClick: () -> Unit) -> Unit = { RunMenuButton(modifier, it) },
) {
    var open by remember { mutableStateOf(false) }
    var journal by remember { mutableStateOf(false) }
    val sheets = LocalSystemSheets.current
    val report = LocalFightReport.current
    trigger { open = true }
    UpdateMark()
    if (open || journal || sheets?.open == true) hold()
    report?.takeIf { journal }?.let { FightLogSheet(it) { journal = false } }
    if (!open) return
    ForgeSheet(onDismissRequest = { open = false }, containerColor = palette.sheet) {
        CompositionLocalProvider(LocalMenuPalette provides palette) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ui("run.menu"), color = palette.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    badge?.invoke()
                }
                QuickSettings()
                HorizontalDivider(color = palette.line)
                extra?.let {
                    it()
                    HorizontalDivider(color = palette.line)
                }
                MenuItems(items, report, sheets, close = { open = false }, onJournal = { journal = true })
            }
        }
    }
}

/** Общие пункты меню («Все настройки», «Журнал боя») и пункты режима [items]; каждый сперва закрывает меню ([close]). */
@Composable private fun MenuItems(items: List<RunMenuItem>, report: FightReport?, sheets: SystemSheets?, close: () -> Unit, onJournal: () -> Unit) {
    fun closing(action: () -> Unit): () -> Unit = {
        close()
        action()
    }
    sheets?.let {
        MenuRow(RunMenuItem(Icons.Outlined.Settings, ui("run.menu.settings"), ui("run.menu.settings_note"), onClick = closing { it.settings = true }))
    }
    report?.let { MenuRow(RunMenuItem(Icons.AutoMirrored.Outlined.ReceiptLong, ui("run.menu.log"), ui("run.menu.log_note"), onClick = closing(onJournal))) }
    items.forEach { item -> MenuRow(item.copy(onClick = closing(item.onClick))) }
}

/** ☰ меню забега: круглая тональная иконка-кнопка, как кнопки карты. */
@Composable fun RunMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    ForgeIconButton(Icons.Outlined.Menu, ui("run.menu"), onClick = onClick, modifier = modifier, kind = ButtonKind.Secondary, shape = IconButtonShape.Circle)
}

/** Настройки, что нужны посреди боя: анимации и цифры урона - те же строки, что на экране настроек. */
@Composable private fun QuickSettings() {
    val model = koinViewModel<SettingsViewModel>()
    val set by model.settings.collectAsStateWithLifecycle()
    SwitchRow(Icons.Outlined.Animation, ui("settings.animations"), null, set.animations) { on -> model.change { copy(animations = on) } }
    SwitchRow(Icons.Outlined.DirectionsRun, ui("settings.swift"), null, set.swiftAfterBoss) { on -> model.change { copy(swiftAfterBoss = on) } }
    ChoiceRow(Icons.Outlined.Whatshot, ui("settings.damage"), DamageNumbers.entries, set.damageNumbers, { ui("settings.damage.${it.name}") }) {
        model.change { copy(damageNumbers = it) }
    }
}

@Composable private fun MenuRow(item: RunMenuItem) {
    val palette = LocalMenuPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(enabled = item.enabled, onClick = item.onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val tint = when {
            !item.enabled -> palette.muted
            item.danger -> palette.danger
            else -> palette.title
        }
        Icon(item.icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label, color = if (item.danger) palette.danger else palette.text, style = MaterialTheme.typography.bodyMedium)
            item.note?.let { Text(it, color = palette.muted, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
