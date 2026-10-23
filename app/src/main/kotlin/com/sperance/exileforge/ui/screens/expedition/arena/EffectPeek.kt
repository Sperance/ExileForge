package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.LocalContentIndex
import com.sperance.exileforge.ui.components.TipCallout
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Выноска плитки полки, которой коснулись: ключ плитки ([Shelved.key]) - по нему выноска находит её в каждом новом снимке боя, и её
 * последний вид - им выноска говорит «Эффект закончился», когда плитки уже нет. Открыта одна выноска на полку.
 */
@Stable
internal class ShelfPeek {
    var key: String? by mutableStateOf(null)
        private set

    /** Последний вид плитки выноски: читается только при рисовании выноски, перекомпоновок не вызывает. */
    var last: Shelved? = null
        private set

    /** Место выноски на полке, пока плитка видна: туда же она встаёт, когда плитка ушла. */
    internal var slot: Int = 0

    fun open(item: Shelved) {
        last = item
        key = item.key
    }

    /** Плитка выноски в новом снимке полки [items]; ушла - null. Живая плитка становится последним видом. */
    fun find(items: List<Shelved>): Shelved? = key?.let { k -> items.firstOrNull { it.key == k } }?.also { last = it }

    fun close() {
        key = null
        last = null
    }
}

@Composable internal fun rememberShelfPeek(): ShelfPeek = remember { ShelfPeek() }

/**
 * Выноска эффекта у его плитки: имя цветом эффекта и стаки «×N», что он делает (сила), «От: <источник>», сколько осталось - числом и
 * нитью, кнопка «Все эффекты» ([onAll]: лист всех эффектов на этой строке). Плитки уже нет ([live] null) - вместо срока «Эффект
 * закончился», вид - последний ([last]). Пока выноска открыта, бой арены стоит ([ArenaHold]); касание мимо закрывает ([onClose]).
 */
@Composable internal fun EffectPeek(live: Shelved?, last: Shelved, onAll: () -> Unit, onClose: () -> Unit) {
    ArenaHold()
    val shown = live ?: last
    val row = shown.row(LocalContentIndex.current)
    TipCallout(row.tint, onClose) {
        Row(Modifier.widthIn(min = PEEK_WIDTH), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.title, color = row.tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            shown.stacks.takeIf { it > 1 }?.let { Text(ui("fight.peek.stacks", it), color = Parchment, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
        }
        row.lines.filter { it.isNotBlank() }.forEach { Text(it, color = Parchment, style = MaterialTheme.typography.bodySmall) }
        Text(ui("fight.peek.from", row.source), color = Muted, style = MaterialTheme.typography.labelMedium)
        if (live == null) {
            Text(ui("fight.peek.ended"), color = LifeRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        } else {
            PeekTimer(row, shown.share)
        }
        ForgeButton(onClick = onAll, modifier = Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, size = ButtonSize.Wide) { Text(ui("fight.peek.all")) }
    }
}

/** Срок выноски: нить остатка [share] (есть - если плитка знает долю) и справа секунды; без срока - «весь бой» или слово состояния. */
@Composable private fun PeekTimer(row: ShelfRow, share: Float?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(3.dp).background(Color(0x1FFFFFFF), BarShape)) {
            if (share != null) Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(row.tint, BarShape))
        }
        Text(
            row.state ?: row.seconds?.let { ui("fight.fact_seconds", fineNumber(it)) } ?: ui("fight.shelf.whole"),
            color = Parchment,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

/** Сколько раз эффект лёг: стаки недуга, слои проклятия, заряды; прочие - один. */
private val Shelved.stacks: Int get() = when (this) {
    is Shelved.Sick -> view.stacks
    is Shelved.Effect -> view.layers
    is Shelved.Charge -> view.count
    else -> 1
}

/** Доля срока, что ещё осталась, - у недуга и эффекта с таймером; у прочих её нет. */
private val Shelved.share: Float? get() = when (this) {
    is Shelved.Sick -> view.left
    is Shelved.Effect -> view.left.takeUnless { view.lasting || view.halted }
    else -> null
}

/** Наименьшая ширина выноски: имя и стаки в строку, кнопка не сжимается. */
private val PEEK_WIDTH = 200.dp

private val BarShape = RoundedCornerShape(2.dp)
