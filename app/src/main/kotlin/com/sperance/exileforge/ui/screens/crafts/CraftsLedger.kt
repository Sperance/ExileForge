package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.crafts.LedgerKind
import com.sperance.exileforge.core.crafts.LedgerRow
import com.sperance.exileforge.core.crafts.LedgerStep
import com.sperance.exileforge.core.crafts.StepKind
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Brass
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.ModBlue
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Seam
import com.sperance.exileforge.ui.theme.Vital

/**
 * Поля листа работы списком строк ([LedgerRow]): значок, подпись с меткой «⚒ инструмент», число справа. Касание строки раскрывает под ней
 * лесенку разбора по источникам, раскрыть можно несколько; смена работы [work] сворачивает всё. «Подробнее» - [onHelp] с ключом справки.
 */
@Composable internal fun JobFields(rows: List<LedgerRow>, work: String, onHelp: (String) -> Unit) {
    var open by remember(work) { mutableStateOf(emptySet<String>()) }
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(Abyss).border(1.dp, Bronze, shape)) {
        rows.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = Bronze)
            val expanded = row.key in open
            LedgerLine(row, expanded) { open = if (expanded) open - row.key else open + row.key }
            if (expanded) LedgerFall(row) { onHelp(row.kind.help) }
        }
    }
}

/** Значок и цвет поля по его виду. */
private class LedgerLook(val icon: ImageVector, val tint: Color)

private fun look(kind: LedgerKind): LedgerLook = when (kind) {
    LedgerKind.CYCLE -> LedgerLook(Icons.Outlined.Schedule, Parchment)
    LedgerKind.HOUR -> LedgerLook(Icons.Outlined.TrendingUp, Vital)
    LedgerKind.WASTE -> LedgerLook(Icons.Outlined.Block, LifeRed)
    LedgerKind.SURE -> LedgerLook(Icons.Outlined.Check, Vital)
    LedgerKind.YIELD -> LedgerLook(Icons.Outlined.Add, Parchment)
    LedgerKind.DOUBLE, LedgerKind.TRIPLE -> LedgerLook(Icons.Outlined.AutoAwesome, Vital)
    LedgerKind.SAVE -> LedgerLook(Icons.Outlined.Replay, Vital)
    LedgerKind.FREE -> LedgerLook(Icons.Outlined.Redeem, Vital)
    LedgerKind.EXPERIENCE -> LedgerLook(Icons.Outlined.StarOutline, Rune)
    LedgerKind.FIND -> LedgerLook(Icons.Outlined.Add, Brass)
}

/** Строка поля: у раскрытой - золотая черта слева и повёрнутая стрелка. */
@Composable private fun LedgerLine(row: LedgerRow, expanded: Boolean, onToggle: () -> Unit) {
    val look = look(row.kind)
    val label = ui("crafts.ledger.open", row.caption, row.value)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .drawBehind { if (expanded) drawRect(Gold, size = Size(3.dp.toPx(), size.height)) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(28.dp).background(PanelRaised, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
            if (row.kind == LedgerKind.FIND) BagIcon(row.item, Modifier.size(20.dp)) else Icon(look.icon, null, tint = look.tint, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.caption, color = Parchment, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (row.tool) ToolMark()
            }
            if (row.note.isNotEmpty()) Text(row.note, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(row.value, color = look.tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp).rotate(if (expanded) 90f else 0f))
    }
}

/** Метка поля, которое сдвигает инструмент. */
@Composable private fun ToolMark() {
    Text(
        ui("crafts.ledger.tool_mark"),
        color = Brass,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier.background(Brass.copy(alpha = .14f), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Лесенка разбора поля [row]: ступени с полосами и «Подробнее» в справку. */
@Composable private fun LedgerFall(row: LedgerRow, onHelp: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(PanelRaised).padding(start = 12.dp, end = 12.dp, top = 4.dp)) {
        row.steps.forEach { LedgerStepLine(it) }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onHelp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = ModBlue, modifier = Modifier.size(18.dp))
            Text(ui("crafts.ledger.more"), color = ModBlue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Ступень лесенки: кто и сдвиг слева, мера после ступени справа, под ними полоса. */
@Composable private fun LedgerStepLine(step: LedgerStep) {
    val total = step.kind == StepKind.TOTAL
    val tone = when (step.kind) {
        StepKind.BASE -> Parchment
        StepKind.SOURCE -> if (step.tool) Brass else Vital
        StepKind.NO_TOOL -> Muted
        StepKind.TOTAL -> GoldBright
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (step.note.isEmpty()) step.label else "${step.label} · ${step.note}",
                color = when (step.kind) {
                    StepKind.NO_TOOL -> Muted
                    StepKind.TOTAL -> GoldBright
                    else -> Parchment
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (total) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(step.value, color = tone, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.width(72.dp))
        }
        if (step.kind != StepKind.NO_TOOL) StepBar(step, if (step.kind == StepKind.SOURCE) tone else null, if (total) Gold else Seam)
    }
}

/** Полоса ступени: [fill] до начала сдвига (у базы и итога - вся мера), сдвиг цветом [shift]. */
@Composable private fun StepBar(step: LedgerStep, shift: Color?, fill: Color) {
    Canvas(Modifier.fillMaxWidth().padding(top = 3.dp).height(6.dp)) {
        val radius = CornerRadius(3.dp.toPx())
        drawRoundRect(Abyss, cornerRadius = radius)
        val reach = if (shift == null) step.high else step.low
        if (reach > 0f) drawRoundRect(fill, size = Size(size.width * reach, size.height), cornerRadius = radius)
        if (shift != null) {
            val width = (size.width * (step.high - step.low)).coerceAtLeast(2.dp.toPx())
            drawRoundRect(shift.copy(alpha = .85f), topLeft = Offset(size.width * step.low, 0f), size = Size(width, size.height), cornerRadius = radius)
        }
    }
}
