package com.sperance.exileforge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Угол действий строки списка: справа сверху - то, к чему относятся кнопки (цена [GoldCost], ключи, срок; [value], если есть),
 * под ним - кнопки строки [RowButton] по ширине подписи, главная крайняя справа, второе действие левее. Ширину угол не тянет:
 * прижать его вправо - дело строки (`fillMaxWidth` у угла или `weight` у соседа). Кнопки, что не встают в ширину (360 dp, длинные
 * подписи), переносятся рядом ниже, тоже прижатым вправо.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RowActions(
    modifier: Modifier = Modifier,
    value: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        value?.invoke()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/**
 * Кнопка в строке списка: малая [ButtonSize.Small] (30 dp, зона касания 44 dp полями) по ширине подписи в одну строку, по
 * умолчанию главная [ButtonKind.Primary] - в списке «место» - строка, и главная допустима в каждой. Своей ширины
 * (`weight`, `fillMaxWidth`) у неё нет; широкая кнопка - только подвал листа.
 */
@Composable fun RowButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    state: ButtonState = ButtonState.Enabled,
    icon: ImageVector? = null,
    accent: Color? = null,
) {
    ForgeButton(onClick = onClick, modifier = modifier, kind = kind, size = ButtonSize.Small, state = state, icon = icon, accent = accent) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
