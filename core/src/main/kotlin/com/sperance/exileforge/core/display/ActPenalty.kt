package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.CampaignFile
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.MapStat

/**
 * Штраф сопротивлений героя по акту, что сводка показывает поверх листа: [percent] - на сколько ниже сопротивления, [act] - номер
 * региона с единицы ([CampaignFile.act]), [region] - его код; [atlas] - штраф карт Атласа (последний), региона у него нет. Число - одна
 * функция правил ([CampaignFile.resistPenalty]), что кладёт его в бой похода.
 */
data class ActPenalty(val act: Int, val atlas: Boolean, val percent: Double, val region: String?) {
    /** Номер акта римскими цифрами - так его называет плашка сводки. */
    val numeral: String get() = roman(act)

    companion object {
        /**
         * Штраф героя [hero]: в заходе - по зоне захода (на карте Атласа - последний), вне захода - по самому дальнему региону, где у героя
         * открыта зона ([progress]). Штрафа нет (регион 1) или не из чего его взять - null: сводка печатает лист как есть.
         */
        fun of(hero: HeroView, progress: CampaignProgress?, campaign: CampaignFile): ActPenalty? {
            val run = hero.campaign.run
            val onMap = run?.context?.active != null
            val place = run?.zone?.let(::MapCode)?.takeIf { onMap || campaign.act(it) > 0 } ?: farthest(progress, campaign) ?: return null
            val percent = campaign.resistPenalty(place, onMap)
            if (percent <= 0) return null
            val act = campaign.act(place)
            return ActPenalty(act, onMap, percent, campaign.regions.getOrNull(act - 1)?.code?.takeUnless { onMap })
        }

        /** Зона самого дальнего региона среди открытых и пройденных героем ([progress]); ничего не открыто - null. */
        private fun farthest(progress: CampaignProgress?, campaign: CampaignFile): MapCode? =
            progress?.let { it.unlocked + it.cleared }?.maxByOrNull(campaign::act)?.takeIf { campaign.act(it) > 0 }

        private fun roman(number: Int): String {
            var rest = number
            return buildString {
                NUMERALS.forEach { (value, glyph) ->
                    while (rest >= value) {
                        append(glyph)
                        rest -= value
                    }
                }
            }
        }

        private val NUMERALS = listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    }
}

/**
 * Сопротивление под штрафом акта: [sheet] - набранное по листу, [penalized] - после штрафа, [effective] - что читает бой (предел и пол
 * правил, [StatLimits]).
 */
data class PenalizedResist(val sheet: Double, val penalized: Double, val effective: Double) {
    /** Сколько после штрафа лежит выше предела и в бою не работает. */
    val over: Double get() = (penalized - effective).coerceAtLeast(0.0)

    /** Сколько пол правил поднял сопротивление, ушедшее под него штрафом. */
    val under: Double get() = (effective - penalized).coerceAtLeast(0.0)

    /** Бой читает не то, что осталось после штрафа: оно выше предела или ниже пола. */
    val clamped: Boolean get() = effective != penalized
}

/**
 * Сопротивления листа [stats] под штрафом [penalty]: лист под штрафом - та же функция, что строки карты в бою ([MapEffects.hero] со
 * строками [MapStat.resistPenaltyLines]), пределы - [StatLimits]. Считается один раз на лист.
 */
class ActResists(val penalty: ActPenalty, private val stats: Map<String, Double>, private val campaign: CampaignFile) {
    private val penalized = MapEffects.hero(stats, MapStat.resistPenaltyLines(penalty.percent))

    /** Характеристика [stat] под штрафом; штраф её не трогает (не сопротивление) - null. */
    fun of(stat: String): PenalizedResist? {
        val sheet = StatLimits.of(stat, stats, campaign) ?: return null
        val acting = StatLimits.of(stat, penalized, campaign) ?: return null
        if (acting.gathered == sheet.gathered) return null
        return PenalizedResist(sheet.gathered, acting.gathered, acting.effective)
    }
}
