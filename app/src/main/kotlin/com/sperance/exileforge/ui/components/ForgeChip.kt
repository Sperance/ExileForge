package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnPrimary
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Seam
import com.sperance.exileforge.ui.theme.rarityColor

/** Как чип выбирает - и как его называет чтение с экрана. */
enum class ChipMode {
    /** Фильтр: каждый включается сам по себе (флажок). */
    Filter,

    /**
     * Выбор одного из ряда (переключатель): «Все», порог, фракция, вкладка тайника. Выбранный - без галочки: заливка и окантовка
     * [ChoiceRim] вдвое толще, так что ширина чипа от выбора не зависит и ряд не перекладывается.
     */
    Choice,
}

/**
 * Размер чипа: видимая высота [height], зона касания [zone], поля по бокам [pad]. [Regular] - чипы экранов, [Dense] - плотные фишки
 * шторки фильтра (28 dp, касание 40 dp).
 */
enum class ChipSize(val height: Dp, val zone: Dp, val pad: Dp) {
    Regular(32.dp, 44.dp, 12.dp),
    Dense(28.dp, 40.dp, 10.dp),
}

/** Окантовка выбранного чипа выбора: выбор виден толщиной, а не только цветом. */
private val ChoiceRim = 2.dp

private val ChipOff = SurfaceLook(Seam, SolidColor(Panel), Quiet)
private val ChipOn = SurfaceLook(Gold, SolidColor(ButtonInk.ChipOnFill), GoldBright)
private val ChoiceOn = SurfaceLook(Gold, SolidColor(ButtonInk.ChipOnFill), GoldBright, rimWidth = ChoiceRim)
private val ChoiceOff = SurfaceLook(Muted, null, Muted, hatched = true, rimWidth = ChoiceRim)

/**
 * Чип фильтра или выбора игры: подпись [label] в скруглённом прямоугольнике [size] (32 dp, касание 44 dp; плотный - 28 и 40 dp). Оси - как у
 * [ForgeButton]: [state] (погашенный - штриховка), [accent] - цвет места из темы (сторона голоса, фракция, редкость): окантовка
 * и слова акцентом, у выбранного - и фон его тоном. [mode] - фильтр (флажок) или выбор одного (переключатель). Выбранный
 * ([selected]) фильтр - галочка слева, выбор - толстая окантовка без галочки ([ChipMode.Choice]); [leading] - свой знак (замок,
 * герб, крестик снятия фильтра) на месте галочки, 15 dp, цвет - чипа.
 * [count] - число справа в таблетке, для чтения с экрана - в описании состояния; [contentDescription] - своё имя вместо подписи
 * («Нравится, 3» вместо «+3»).
 */
@Composable fun ForgeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mode: ChipMode = ChipMode.Filter,
    size: ChipSize = ChipSize.Regular,
    state: ButtonState = ButtonState.Enabled,
    accent: Color? = null,
    count: Int? = null,
    leading: (@Composable () -> Unit)? = null,
    contentDescription: String? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val enabled = state == ButtonState.Enabled
    val look = remember(enabled, selected, accent, mode) { chipLook(enabled, selected, accent, mode) }
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
            .touchZone(size.zone)
            .height(size.height)
            .pressShift(pressed)
            .forgeSurface(look, pressed)
            .padding(horizontal = size.pad),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides look.ink) {
            when {
                leading != null -> Box(Modifier.size(15.dp), contentAlignment = Alignment.Center) { leading() }
                selected && mode == ChipMode.Filter -> Icon(Icons.Outlined.Check, null, Modifier.size(15.dp))
            }
        }
        Text(label, color = look.ink, style = if (size == ChipSize.Dense) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        count?.let { ChipCount(it, selected, enabled, accent) }
    }
}

/**
 * Окраска чипа: погашенный - штриховка; с акцентом - окантовка и слова его цветом, выбранный - и фон его тоном. Выбранный чип
 * выбора [ChipMode.Choice] - с окантовкой [ChoiceRim], и погашенный тоже: галочки у него нет, выбор виден рамкой.
 */
private fun chipLook(enabled: Boolean, selected: Boolean, accent: Color?, mode: ChipMode): SurfaceLook {
    val choice = selected && mode == ChipMode.Choice
    return when {
        !enabled -> if (choice) ChoiceOff else OffLook
        !selected -> accent?.let { SurfaceLook(it.copy(alpha = .45f), SolidColor(Panel), it) } ?: ChipOff
        accent == null -> if (choice) ChoiceOn else ChipOn
        else -> SurfaceLook(accent, SolidColor(accent.onPanel(.2f)), accent, rimWidth = if (choice) ChoiceRim else 1.dp)
    }
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

/**
 * Выбор редкости фишками в её цвете ([rarityColor]) - один на все места: шторки фильтров, выдача тестировщика, вложения письма.
 * [rarities] по порядку показа, [selected] - выбрана ли редкость (null - фишка «любая» [any], она первой, если задана); касание -
 * [onPick]. [mode] - выбор одной ([ChipMode.Choice]) или флажки ([ChipMode.Filter]), [size] - плотные фишки шторки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RarityChoice(
    rarities: List<Rarity>,
    selected: (Rarity?) -> Boolean,
    onPick: (Rarity?) -> Unit,
    mode: ChipMode = ChipMode.Choice,
    size: ChipSize = ChipSize.Regular,
    any: String? = null,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        any?.let { ForgeChip(it, selected(null), { onPick(null) }, mode = mode, size = size) }
        rarities.forEach { rarity -> ForgeChip(rarityTitle(rarity), selected(rarity), { onPick(rarity) }, mode = mode, size = size, accent = rarityColor(rarity.name)) }
    }
}
