package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnPrimary
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Seam

/** Как чип выбирает - и как его называет чтение с экрана. */
enum class ChipMode {
    /** Фильтр: каждый включается сам по себе (флажок). */
    Filter,

    /** Выбор одного из ряда (переключатель): «Все», порог, фракция, вкладка тайника. */
    Choice,
}

private val ChipOff = SurfaceLook(Seam, SolidColor(Panel), Quiet)
private val ChipOn = SurfaceLook(Gold, SolidColor(ButtonInk.ChipOnFill), GoldBright)

/**
 * Чип фильтра или выбора игры: подпись [label] в скруглённом прямоугольнике 32 dp, зона касания - не меньше 44 dp. Оси - как у
 * [ForgeButton]: [state] (погашенный - штриховка), [accent] - цвет места из темы (сторона голоса, фракция, редкость): окантовка
 * и слова акцентом, у выбранного - и фон его тоном. [mode] - фильтр (флажок) или выбор одного (переключатель). Выбранный
 * ([selected]) - галочка слева; [leading] - свой знак вместо неё (замок, герб, крестик снятия фильтра), 15 dp, цвет - чипа.
 * [count] - число справа в таблетке, для чтения с экрана - в описании состояния; [contentDescription] - своё имя вместо подписи
 * («Нравится, 3» вместо «+3»).
 */
@Composable fun ForgeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mode: ChipMode = ChipMode.Filter,
    state: ButtonState = ButtonState.Enabled,
    accent: Color? = null,
    count: Int? = null,
    leading: (@Composable () -> Unit)? = null,
    contentDescription: String? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val enabled = state == ButtonState.Enabled
    val look = remember(enabled, selected, accent) { chipLook(enabled, selected, accent) }
    val pick = when (mode) {
        ChipMode.Filter -> Modifier.toggleable(selected, source, indication = null, enabled = enabled, role = Role.Checkbox) { onClick() }
        ChipMode.Choice -> Modifier.selectable(selected, source, indication = null, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    }
    val counted = count?.let { ui("button.count", it) }
    Row(
        modifier.then(pick)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                counted?.let { stateDescription = it }
            }
            .touchZone()
            .height(32.dp)
            .pressShift(pressed)
            .forgeSurface(look, pressed)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides look.ink) {
            when {
                leading != null -> Box(Modifier.size(15.dp), contentAlignment = Alignment.Center) { leading() }
                selected -> Icon(Icons.Outlined.Check, null, Modifier.size(15.dp))
            }
        }
        Text(label, color = look.ink, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        count?.let { ChipCount(it, selected, enabled, accent) }
    }
}

/** Окраска чипа: погашенный - штриховка; с акцентом - окантовка и слова его цветом, выбранный - и фон его тоном. */
private fun chipLook(enabled: Boolean, selected: Boolean, accent: Color?): SurfaceLook = when {
    !enabled -> OffLook
    accent == null -> if (selected) ChipOn else ChipOff
    selected -> SurfaceLook(accent, SolidColor(accent.onPanel(.2f)), accent)
    else -> SurfaceLook(accent.copy(alpha = .45f), SolidColor(Panel), accent)
}

/** Число чипа: таблетка, у выбранного - зелёная или цвета акцента; читает его описание состояния чипа. */
@Composable private fun ChipCount(count: Int, selected: Boolean, enabled: Boolean, accent: Color?) {
    val (fill, ink) = when {
        !enabled -> ButtonInk.CountOffFill to Muted
        selected && accent != null -> accent to inkOn(accent)
        selected -> Gold to OnPrimary
        else -> ButtonInk.CountFill to Parchment
    }
    Text(
        count.toString(),
        color = ink,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier.clearAndSetSemantics {}.defaultMinSize(minWidth = 20.dp).background(fill, RoundedCornerShape(9.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
