package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.refusalText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.QualityGain
import com.sperance.exileforge.rules.roll.QualityKind

/**
 * Сфера качества над вещью словами игрока: что поднимет вид качества, который даёт знамение [omen] (катализатор или
 * никакого - база), и сколько накопленного качества другого вида сбросится. Решают правила ([QualityKind]).
 * Вид не ложится (бижутерия без базы без катализатора) - [refusal]: отказ правил словами сервера (`CR_038`/`CR_039`).
 */
class QualityForecast(val raises: String, val lost: Int, val refusal: String? = null) {
    companion object {
        fun of(index: ContentIndex, item: ItemInstance, template: ItemTemplate, omen: Omen?): QualityForecast {
            val kind = QualityKind.of(omen)
            val gain = kind.gain(item, template, index) ?: return QualityForecast("", 0, refusalText(kind.refusal(template)))
            return QualityForecast(raises(index, gain), QualityKind.lost(item, kind))
        }

        private fun raises(index: ContentIndex, gain: QualityGain): String = when (gain) {
            is QualityGain.Base -> ui("forge.quality_raises_base", titles(index, gain.lines))

            QualityGain.Flask -> ui("forge.quality_raises_flask")

            QualityGain.MapQuantity -> ui("forge.quality_raises_map")

            is QualityGain.Affixes -> {
                val kind = ui("enum.catalyst.${gain.catalyst.name}")
                if (gain.lines.isEmpty()) ui("forge.quality_raises_kind_future", kind) else ui("forge.quality_raises_kind", kind, titles(index, gain.lines))
            }
        }

        /** Строки как характеристики, что они меняют, без повторов. */
        private fun titles(index: ContentIndex, lines: List<ModifierCode>): String = lines.mapNotNull { index.modifier(it)?.effects?.firstOrNull()?.stat }
            .distinct().joinToString(", ") { statTitle(it) }
    }
}
