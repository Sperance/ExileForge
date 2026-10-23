package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.Strength
import com.sperance.exileforge.rules.content.Weakness
import com.sperance.exileforge.rules.roll.StrengthReading
import kotlin.math.abs
import kotlin.math.round

/** Сила вида на чипе: [strength], слово недуга [ailment] у силы недугов (по нему цвет и имя), название [title] и пояснение с числом [hint]. */
data class StrengthChip(val strength: Strength, val ailment: String?, val title: String, val hint: String)

/** Открытая слабость вида на чипе: [weakness], название [title] и пояснение с числом [hint]. */
data class WeaknessChip(val weakness: Weakness, val title: String, val hint: String)

/**
 * Силы и слабости вида для экрана: силы [strengths] видны всегда, слабости [weaknesses] - открытые; [hidden] - слабости ещё скрыты
 * (чип «? слабость» с подсказкой [hiddenHint], как их открыть). У вида без слабостей скрытого чипа нет.
 */
data class MonsterMarkView(val strengths: List<StrengthChip>, val weaknesses: List<WeaknessChip>, val hiddenHint: String?) {
    val hidden: Boolean get() = hiddenHint != null
    val empty: Boolean get() = strengths.isEmpty() && weaknesses.isEmpty() && !hidden

    companion object {
        val NONE = MonsterMarkView(emptyList(), emptyList(), null)

        /**
         * Силы и слабости вида [code] по контенту [index]: силы - с числом против медианы главы ([com.sperance.exileforge.rules.roll.MonsterMarks]),
         * слабости открыты, когда убийств вида [kills] (счёт Бестиария) не меньше `marks.reveal`, у босса - после первой победы;
         * не прочитан счёт - null, слабости скрыты. Вида нет в контенте - [NONE].
         */
        fun of(index: ContentIndex, code: MonsterCode, kills: Long?): MonsterMarkView {
            val monster = index.monster(code) ?: return NONE
            val low = index.campaign.combat.passiveLowLife * PERCENT
            val strengths = index.marks.strengths(monster).map { reading ->
                val key = reading.strength.key(reading.ailment)
                StrengthChip(reading.strength, reading.ailment, strengthTitle(key), strengthText(key, figure(reading), low))
            }
            val need = index.campaign.marks.revealAt(monster.boss)
            val open = (kills ?: 0L) >= need
            val weaknesses = if (open) monster.weaknesses.map { chip(index, it, low) } else emptyList()
            val hint = when {
                open || monster.weaknesses.isEmpty() -> null
                monster.boss -> ui("marks.hidden_boss")
                else -> ui("marks.hidden_kills", need - (kills ?: 0L))
            }
            return MonsterMarkView(strengths, weaknesses, hint)
        }

        private fun chip(index: ContentIndex, weakness: Weakness, low: Double): WeaknessChip {
            val value = index.campaign.marks.weaknesses[weakness]?.firstOrNull()?.value ?: 0.0
            return WeaknessChip(weakness, weaknessTitle(weakness.name), weaknessText(weakness.name, abs(value), low))
        }

        /** Число силы на чипе: доля к среднему главы - десятыми («1.6»), своё число - как есть. */
        private fun figure(reading: StrengthReading): String = reading.ratio?.let { fineNumber(round(it * TENTHS) / TENTHS) } ?: fineNumber(reading.value)

        private const val PERCENT = 100.0
        private const val TENTHS = 10.0
    }
}
