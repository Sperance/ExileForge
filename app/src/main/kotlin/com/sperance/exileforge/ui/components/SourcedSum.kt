package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Quiet

/**
 * Доля одного источника в строке-сумме ([SourcedSumRow]): имя источника [source], доля текстом [share] (со знаком и единицей), сноска
 * под ней [note] (null - без сноски); [accent] - цвет имени источника, null - приглушённый.
 */
@Immutable
data class SumShare(val source: String, val share: String, val note: String? = null, val accent: Color? = null)

/**
 * Строка-сумма с источниками: [sum] - сама сумма (фраза модификатора, число характеристики), [shares] - откуда она сложилась, по порядку.
 * Две доли и больше - стрелка справа раскрывает под строкой подсписок «источник … доля» с отступом и линией слева; одна доля - без
 * стрелки, источник мелкой подписью под суммой; без долей - только сумма. Строка свёрнута сначала и помнит раскрытие, пока жив экран.
 * Касание суммы остаётся её содержимому (пояснение характеристики): раскрывает только стрелка.
 */
@Composable
fun SourcedSumRow(shares: List<SumShare>, modifier: Modifier = Modifier, sum: @Composable RowScope.() -> Unit) {
    val folds = shares.size > 1
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = sum)
            if (folds) {
                Box(
                    Modifier.size(SourcedSumMetrics.toggle).foldToggle(open, ui(if (open) "common.collapse" else "common.expand")) { open = !open },
                    contentAlignment = Alignment.Center,
                ) { FoldChevron(open) }
            }
        }
        if (!folds) {
            shares.singleOrNull()?.let { only -> ShareCaption(only) }
            return@Column
        }
        FoldBody(open) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = SourcedSumMetrics.indent, bottom = 8.dp)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(Bronze))
                Column(Modifier.weight(1f).padding(start = 10.dp, top = 2.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    shares.forEach { SumShareLine(it) }
                }
            }
        }
    }
}

/** Доля строки-суммы в раскрытом подсписке: источник слева, доля справа, сноска строкой ниже; её же печатает раскрытие сводки героя. */
@Composable fun SumShareLine(share: SumShare) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(share.source, color = share.accent ?: Quiet, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(share.share, color = Parchment, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        share.note?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
    }
}

/** Единственный источник строки - мелкой подписью под суммой, со сноской через точку. */
@Composable private fun ShareCaption(share: SumShare) {
    Text(
        listOfNotNull(share.source, share.note).joinToString(" · "),
        color = share.accent ?: Muted,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(start = SourcedSumMetrics.indent, bottom = 4.dp),
    )
}

/** Размеры строки-суммы: зона касания стрелки и отступ долей под значком суммы. */
private object SourcedSumMetrics {
    val toggle = 44.dp
    val indent = 26.dp
}
