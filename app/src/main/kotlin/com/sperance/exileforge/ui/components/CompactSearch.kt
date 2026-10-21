package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital

/**
 * Плотное поле поиска (40dp) строки поиска [SearchBar]: лупа, ввод одной строкой и подсказка [placeholder], пока
 * пусто; басальт в нити `Bronze`. Экраны берут [SearchBar], а не это поле. [onSearch] - поиск с клавиатуры (поиск у
 * сервера); null - список сужается на месте по [onValue]. [found] - сколько нашлось, мелко справа; null - не пишется. [onClear] -
 * крестик очистки справа; null - крестика нет.
 */
@Composable internal fun CompactSearchField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    found: Int? = null,
    onClear: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    val style = MaterialTheme.typography.bodyMedium
    BasicTextField(
        value,
        onValue,
        modifier.height(40.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = if (onSearch != null) ImeAction.Search else ImeAction.Default),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        textStyle = style.copy(color = Parchment),
        cursorBrush = SolidColor(Gold),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().background(Panel, shape).border(1.dp, Bronze, shape).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Search, null, tint = Muted, modifier = Modifier.size(18.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = Muted, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    field()
                }
                found?.let { Text(it.toString(), color = Vital, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                onClear?.let { ForgeIconButton(Icons.Outlined.Close, ui("common.clear"), onClick = it, kind = ButtonKind.Text, size = ButtonSize.Compact) }
            }
        },
    )
}
