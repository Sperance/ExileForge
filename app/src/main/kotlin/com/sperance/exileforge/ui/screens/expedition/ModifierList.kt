package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.ModifierSource
import com.sperance.exileforge.core.campaign.run.ModifierSum
import com.sperance.exileforge.core.campaign.run.ModifierWindow
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.MapLines
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ModifierLine
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SourcedSumRow
import com.sperance.exileforge.ui.components.SumShare

/**
 * Модификаторы карты или зоны Ваал одним списком ([ModifierWindow]): заголовок «Модификаторы N» ([titled]) и строка-сумма на каждую
 * характеристику - фраза сервера по сумме, под ней источники ([SourcedSumRow]); доля «Зона Ваал» - цветом портала, со сноской силы
 * Атласа. Пустое окно - «модификаторов нет». Строки собираются раз на окно и язык.
 */
@Composable
internal fun ModifierList(window: ModifierWindow, index: ContentIndex, modifier: Modifier = Modifier, titled: Boolean = true) {
    val lang = uiLanguage
    val rows = remember(window, index, lang) { window.sums.map { it to shares(it, window, index) } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (titled) Engraved(ui("run.mods.title", rows.size))
        if (rows.isEmpty()) MutedText(ui("map.no_modifiers"))
        rows.forEach { (sum, shares) ->
            key(sum.stat) {
                SourcedSumRow(shares) {
                    ModifierLine(effectText(sum.stat, sum.total, index), Glyph.ofStat(sum.stat), stats = listOf(sum.stat), index = index)
                }
            }
        }
    }
}

/** Доли суммы [sum] строками подсписка: имя источника, доля числом; у строк Ваал - сноска силы Атласа окна [window]. */
private fun shares(sum: ModifierSum, window: ModifierWindow, index: ContentIndex): List<SumShare> = sum.shares.map { share ->
    val vaal = share.source == ModifierSource.VAAL
    SumShare(
        ui(share.source.key),
        SkillText.statShare(sum.stat, share.value, index),
        note = if (vaal) MapLines.vaalAtlasNote(window.vaalAtlasEffect) else null,
        accent = if (vaal) PortalTint else null,
    )
}
