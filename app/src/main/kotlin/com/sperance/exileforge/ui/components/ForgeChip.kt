package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.OnPrimary
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Seam

private val ChipOff = SurfaceLook(Seam, SolidColor(Panel), Quiet)
private val ChipOn = SurfaceLook(Gold, SolidColor(ButtonInk.ChipOnFill), GoldBright)

/**
 * Чип фильтра или выбора игры: подпись [label] в скруглённом прямоугольнике 32 dp. Выбранный ([selected]) - зелёная окантовка и
 * галочка слева; [leading] - свой знак слева вместо галочки (замок, герб фракции, крестик снятия фильтра) 15 dp, цвет - чипа;
 * [count] - число справа в таблетке. Погашенный ([enabled] false) - штриховка.
 * Касание добирает до 44 dp сама разметка Compose.
 */
@Composable fun ForgeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    count: Int? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val look = when {
        !enabled -> OffLook
        selected -> ChipOn
        else -> ChipOff
    }
    Row(
        modifier.height(32.dp).forgeSurface(look, Unpressed)
            .selectable(selected, source, indication = null, enabled = enabled, role = Role.Checkbox, onClick = onClick)
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
        count?.let { ChipCount(it, selected, enabled) }
    }
}

/** Число чипа: таблетка, у выбранного - зелёная. */
@Composable private fun ChipCount(count: Int, selected: Boolean, enabled: Boolean) {
    val (fill, ink) = when {
        !enabled -> ButtonInk.CountOffFill to Muted
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
        modifier = Modifier.defaultMinSize(minWidth = 20.dp).background(fill, RoundedCornerShape(9.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
