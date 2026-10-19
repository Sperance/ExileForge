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
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
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

    /** Лист отчётов: вкладка и отчёт, к которому он прокручен; null - закрыт. */
    var reports by mutableStateOf<ReportsView?>(null)

    val open: Boolean get() = bug || settings
}

val LocalSystemSheets = staticCompositionLocalOf<SystemSheets?> { null }

/**
 * Журнал последнего боя забега: режим (поход, Разлом, башня, раш) кладёт свой отчёт, а [RunMenu] - где бы оно ни
 * стояло, на карте или посреди боя - сам добавляет пункт «Журнал боя»; null - боя ещё не было, пункта нет.
 */
val LocalFightReport = compositionLocalOf<FightReport?> { null }

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
 * настройки» поверх забега, «Журнал боя» последнего боя ([LocalFightReport]) и пункты режима [items]; [extra] - свой блок режима над ними (мутаторы).
 * Пока меню или открытый из него системный лист на экране, [hold] держит забег.
 */
@Composable fun RunMenu(
    items: List<RunMenuItem>,
    hold: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
    trigger: @Composable (onClick: () -> Unit) -> Unit = { RunMenuButton(modifier, it) },
) {
    var open by remember { mutableStateOf(false) }
    var journal by remember { mutableStateOf(false) }
    val sheets = LocalSystemSheets.current
    val report = LocalFightReport.current
    trigger { open = true }
    if (open || journal || sheets?.open == true) hold()
    report?.takeIf { journal }?.let { FightLogSheet(it) { journal = false } }
    if (!open) return
    ForgeSheet(onDismissRequest = { open = false }) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui("run.menu"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            QuickSettings()
            HorizontalDivider(color = Bronze.copy(alpha = .5f))
            extra?.invoke(this)
            fun closing(action: () -> Unit): () -> Unit = {
                open = false
                action()
            }
            sheets?.let {
                MenuRow(RunMenuItem(Icons.Outlined.Settings, ui("run.menu.settings"), ui("run.menu.settings_note"), onClick = closing { it.settings = true }))
            }
            report?.let { MenuRow(RunMenuItem(Icons.AutoMirrored.Outlined.ReceiptLong, ui("run.menu.log"), ui("run.menu.log_note"), onClick = closing { journal = true })) }
            items.forEach { item -> MenuRow(item.copy(onClick = closing(item.onClick))) }
        }
    }
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
    ChoiceRow(Icons.Outlined.Whatshot, ui("settings.damage"), DamageNumbers.entries, set.damageNumbers, { ui("settings.damage.${it.name}") }) {
        model.change { copy(damageNumbers = it) }
    }
}

@Composable private fun MenuRow(item: RunMenuItem) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = item.enabled, onClick = item.onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val tint = when {
            !item.enabled -> Muted
            item.danger -> LifeRed
            else -> GoldBright
        }
        Icon(item.icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(item.label, color = if (item.danger) LifeRed else Parchment, style = MaterialTheme.typography.bodyMedium)
            item.note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
