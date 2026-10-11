package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import org.koin.compose.koinInject

/**
 * Короткий номер плашкой (4.7.0): подпись [label], шесть цифр [code] разрядкой и копирование касанием - номер героя в карточке
 * и листе героя, код вступления гильдии. Пустой номер (сервер старше) не рисуется.
 */
@Composable fun ShortCodeChip(label: String, code: String, modifier: Modifier = Modifier) {
    if (code.isBlank()) return
    val clipboard = LocalClipboardManager.current
    val notices = koinInject<Notices>()
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.background(PanelRaised, shape).border(1.dp, Bronze, shape).clickable {
            clipboard.setText(AnnotatedString(code))
            notices.toast(ui("code.copied", code))
        }.padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(code.chunked(3).joinToString(" "), color = GoldBright, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        Icon(Icons.Outlined.ContentCopy, ui("code.copy"), tint = Muted, modifier = Modifier.size(14.dp))
    }
}
