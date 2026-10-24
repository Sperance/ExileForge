package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.display.ActPenalty
import com.sperance.exileforge.core.display.ActResists
import com.sperance.exileforge.core.display.PenalizedResist
import com.sperance.exileforge.core.display.RowFigure
import com.sperance.exileforge.core.display.StatLimits
import com.sperance.exileforge.core.display.groupedStats
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.display.rowValue
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.text.LocaleKey

/**
 * Число сопротивления под штрафом акта словами строки: [sheet] - по листу, [penalized] - после штрафа (null - бой читает его же, предел
 * и пол не тронули), [effective] - действующее; [below] - действующее ниже нуля, [over] - упёрлось в предел; [spoken] - вся строка для
 * чтения с экрана.
 */
data class PenaltyFigure(val sheet: String, val penalized: String?, val effective: String, val below: Boolean, val over: Boolean, val spoken: String)

/**
 * Строка сводки характеристики [stat] со значением листа [value]: имя [title] и число строки [figure] ([RowFigure]: процент защиты,
 * действующее под пределом, иначе цифра) - текстом [text], упёршееся в предел или дно - [clamped]. Под штрафом акта - [penalty]
 * («по листу → действует»), без штрафа - null. [aura] и [fated] - цифру сдвинули ауры и Предначертание.
 */
data class SheetLine(
    val stat: String,
    val value: Double,
    val title: String,
    val text: String,
    val clamped: Boolean,
    val penalty: PenaltyFigure?,
    val aura: Boolean,
    val fated: Boolean,
)

/** Штраф акта в разделе сопротивлений: плашка шапки [chip] («Акт III · −30%») и сноска под строками [note]. */
data class SectionPenalty(val chip: String, val note: String)

/**
 * Раздел сводки [section] с именем [title]: строки [lines] и строки, что поднимают пределы ([StatLimits.raisers]), - [limits], свёрнутые
 * одной строкой «Пределы · N упёрлись» ([reached] строк раздела дошли до предела). [penalty] - штраф акта раздела, без него - null.
 */
data class SheetSection(
    val section: StatSection,
    val title: String,
    val lines: List<SheetLine>,
    val limits: List<SheetLine>,
    val reached: Int,
    val penalty: SectionPenalty?,
)

/** Строка раздела «С условием» [row]: имя характеристики [stat], условие [condition] и вклад [text] словами. */
data class ConditionalLine(val row: ConditionalStat, val stat: String, val condition: String, val text: String)

/**
 * Сводка листа [figures], готовая к показу строками: разделы [sections] по порядку контента и раздел «С условием» [conditional]
 * ([ConditionalStats]). Всё, что строка печатает, посчитано здесь, и считается вне главного потока (вкладка - [com.sperance.exileforge.core.hero.HeroSheetModel]); экран лишь раскладывает строки. Лист
 * героя и лист похода - одна модель. [resists] - лист под штрафом акта, им же читает разбор строки; лист, где штраф уже лёг, - null.
 */
class StatSheetModel(
    val figures: StatFigures,
    val resists: ActResists?,
    val sections: List<SheetSection>,
    val conditional: List<ConditionalLine>,
) {
    /** В листе нет ни одной цифры. */
    val empty: Boolean get() = figures.values.isEmpty()

    companion object {
        /**
         * Сводка листа [figures] героя уровня [level] (null - процента защиты нет) над контентом [index] словами языка [lang]: строки с
         * условием [conditional] - их вклад печатает [explainer]; [resists] - штраф акта, без него - null.
         */
        fun of(
            figures: StatFigures,
            index: ContentIndex,
            level: Int?,
            lang: Lang,
            conditional: List<ConditionalStat>,
            resists: ActResists?,
            explainer: StatExplainer,
        ): StatSheetModel {
            val raisers = StatLimits.raisers(index)
            val sections = groupedStats(figures.values, index.stats).map { (section, rows) ->
                val (limits, plain) = rows.partition { it.first in raisers }
                val lines = plain.map { (stat, value) -> line(stat, value, figures, index, level, lang, resists) }
                val reached = plain.count { (stat) -> StatLimits.of(stat, figures.stats, index.campaign)?.reached == true }
                val penalty = resists?.takeIf { lines.any { it.penalty != null } }?.let { sectionPenalty(it.penalty, index) }
                SheetSection(section, section.title(lang), lines, limits.map { (stat, value) -> line(stat, value, figures, index, level, lang, null) }, reached, penalty)
            }
            val conditionalLines = conditional.map { row ->
                ConditionalLine(row, statTitle(row.stat, lang), loc(LocaleKey.condition(row.condition)), explainer.fmt(row.stat, row.value, row.op, index.stats.isPercent(row.stat)))
            }
            return StatSheetModel(figures, resists, sections, conditionalLines)
        }

        /**
         * Сводка героя игры [game]: его лист боя, свойства с условием и штраф акта ([ActPenalty]) - то, что печатает вкладка «Герой»;
         * героя или контента нет - null.
         */
        fun hero(game: GameUi): StatSheetModel? {
            val hero = game.hero ?: return null
            val index = game.index ?: return null
            val figures = hero.figures
            val resists = ActPenalty.of(hero, game.progress, index.campaign)?.let { ActResists(it, figures.stats, index.campaign) }
            return of(figures, index, hero.level, game.lang, ConditionalStats.of(hero, index), resists, StatExplainer(game))
        }

        private fun line(stat: String, value: Double, figures: StatFigures, index: ContentIndex, level: Int?, lang: Lang, resists: ActResists?): SheetLine {
            val row = RowFigure.of(stat, figures, level, index)
            val penalty = resists?.of(stat)?.let { penaltyFigure(stat, it, index) }
            return SheetLine(stat, value, statTitle(stat, lang), row.text(index, figures.stats), row.clamped, penalty, stat in figures.skilled, stat in figures.fated)
        }

        private fun penaltyFigure(stat: String, resist: PenalizedResist, index: ContentIndex): PenaltyFigure {
            val effective = rowValue(stat, resist.effective, index)
            return PenaltyFigure(
                number(resist.sheet),
                number(resist.penalized).takeIf { resist.clamped },
                effective,
                below = resist.effective < 0,
                over = resist.over > 0,
                spoken = ui("stat.penalty.reading", rowValue(stat, resist.sheet, index), effective),
            )
        }

        private fun sectionPenalty(penalty: ActPenalty, index: ContentIndex): SectionPenalty {
            val percent = number(penalty.percent)
            val chip = if (penalty.atlas) ui("stat.penalty.chip_atlas", percent) else ui("stat.penalty.chip", penalty.numeral, percent)
            val place = penalty.region?.let(::regionTitle) ?: ui("stat.penalty.atlas")
            return SectionPenalty(chip, ui("stat.penalty.note", place, percent, number(index.campaign.combat.resistCap)))
        }
    }
}
