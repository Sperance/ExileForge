package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.StoryChapter
import com.sperance.exileforge.rules.content.ThroneLaw
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/*
 * Законы тронов (сюжет «Девять тронов»): за финал главы герой навсегда берёт один из двух законов её трона. Карточка закона
 * показывает обе стороны до выбора - строки героя и цену для монстров дальше трона и на Атласе; взятие - через шторку
 * подтверждения с удержанием, перевыбора нет.
 */

/** Трон пройденной главы [chapter] ждёт выбора: эпилог главы и две карточки законов; [onTake] - взять закон навсегда. */
@Composable internal fun LawChoice(index: ContentIndex, chapter: StoryChapter, busy: Boolean, onTake: (String) -> Unit) {
    var asked by remember { mutableStateOf<ThroneLaw?>(null) }
    ForgePanel(accent = GoldBright) {
        Engraved(ui("story.law.title"), accent = GoldBright)
        Text(loc(LocaleKey.chapterOutro(chapter.region)), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        MutedText(ui("story.law.choose", loc(LocaleKey.chapterTitle(chapter.region))))
    }
    chapter.laws.forEach { law ->
        LawCard(index, law) {
            ForgeButton({ asked = law }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(ui("story.law.take")) }
        }
    }
    asked?.let { law ->
        ConfirmSheet(
            title = ui("story.law.confirm_title", loc(LocaleKey.lawName(law.code))),
            confirm = ui("story.law.confirm"),
            onDismiss = { asked = null },
            ledger = ledger(index, law),
            warning = ui("story.law.warning"),
            danger = true,
        ) {
            asked = null
            onTake(law.code)
        }
    }
}

/** Взятые законы: имя, строки героя и цена для монстров - на экране Сюжета под главой. */
@Composable internal fun TakenLaws(index: ContentIndex, laws: List<ThroneLaw>) {
    if (laws.isEmpty()) return
    Engraved(ui("story.laws.title"))
    laws.forEach { LawCard(index, it) }
}

/** Карточка закона: имя, что он даёт герою и чем платят - монстры регионов дальше трона и карт Атласа; [action] - под ними. */
@Composable private fun LawCard(index: ContentIndex, law: ThroneLaw, action: (@Composable ColumnScope.() -> Unit)? = null) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).depthPanel(shape, accent = Gold).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(loc(LocaleKey.lawName(law.code)), color = GoldBright, style = MaterialTheme.typography.titleSmall)
        MutedText(ui("story.law.hero"))
        law.hero.forEach { Text("+ " + lineText(index, it), color = Vital, style = MaterialTheme.typography.bodyMedium) }
        MutedText(ui("story.law.cost"))
        law.monsters.forEach { Text("− " + lineText(index, it), color = LifeRed, style = MaterialTheme.typography.bodyMedium) }
        action?.invoke(this)
    }
}

/** Строки закона в шторке подтверждения: что получит герой и что получат монстры дальше. */
private fun ledger(index: ContentIndex, law: ThroneLaw): List<LedgerLine> = law.hero.map { LedgerLine(ui("story.law.ledger_hero"), lineText(index, it), Tone.GAIN) } +
    law.monsters.map { LedgerLine(ui("story.law.ledger_monsters"), lineText(index, it), Tone.SPEND) }
