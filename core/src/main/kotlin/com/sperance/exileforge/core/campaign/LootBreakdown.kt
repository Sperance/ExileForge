package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.display.shareText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.LootFalloffRule
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.run.LootOrigin
import com.sperance.exileforge.rules.run.Run

/**
 * Откуда прибавка к золоту или опыту захода: лист героя, карта-предмет, зона Ваал, сделки алтарей, зелье, сила монстров от уровня героя
 * ([Run.heroScale], только опыт), срез добычи у героя выше района ([Run.lootFalloff], только золото, доля отрицательная), атлас с гильдией.
 */
enum class BoostSource { HERO, MAP, VAAL, PACT, POTION, LEVEL, FALLOFF, ATLAS }

/**
 * Срез добычи монстров у героя уровня [heroLevel] выше района уровня [areaLevel] ([LootFalloffRule.share]): на сколько уровней герой
 * выше ([above]) и доля добычи [share]; в льготе среза нет ([of] - null).
 */
data class LootFalloffView(val above: Int, val share: Double) {
    /** Строка листа стража: «Герой выше района на N ур. · добыча −X%», процент - как в сводке героя ([shareText]). */
    val text: String get() = ui("zone.loot_falloff", above, shareText((1 - share) * 100))

    companion object {
        /** Срез по правилу [rule]; null - доля 1. */
        fun of(rule: LootFalloffRule, heroLevel: Int, areaLevel: Int): LootFalloffView? = rule.share(heroLevel, areaLevel).takeIf { it < 1 }?.let { LootFalloffView(heroLevel - areaLevel, it) }
    }
}

/** Доля источника [source] в прибавке: [percent] процентов к стоку. */
data class BoostPart(val source: BoostSource, val percent: Double)

/**
 * Разбор золота или опыта карты: итог сервера [total] - сток × (1 + Σ[parts] / 100).
 * Сток выводится из итога, а не считается заново, - разбор всегда сходится с тем, что выдал сервер. Прибавки - те же числа, что
 * берут правила сервера на заходе: золото - [Run.boost], опыт - бонусы опыта контекста захода.
 */
data class LootBreakdown(val total: Double, val parts: List<BoostPart>) {
    /** Во сколько раз прибавки умножили сток; золото, как у сервера, не уходит в минус. */
    val multiplier: Double get() = (1 + parts.sumOf { it.percent } / 100).coerceAtLeast(0.0)

    /** Сток - итог без прибавок героя. */
    val stock: Double get() = if (multiplier > 0) total / multiplier else total

    /** Сколько итога принёс источник [part]: его доля стока. */
    fun share(part: BoostPart): Double = stock * part.percent / 100

    companion object {
        /** Источник, что держит прибавку, когда её не на что разложить. */
        private val REST = BoostSource.ATLAS

        /**
         * Золото [total] захода [run] со сделками алтарей [pacts]: прибавка - [Run.boost] обычного монстра, как её берёт сервер;
         * [potion] - строки выпитого на входе зелья, они в атласе контекста. Срез у героя выше района ([LootBoost.share]) множит золото
         * поверх прибавок: его доля - та же доля в процентах стока, (1 + прибавки) × (доля − 1), отрицательная.
         */
        fun gold(run: Run, pacts: Map<String, Double>, potion: Map<String, Double>, total: Double): LootBreakdown {
            val context = run.context.copy(pacts = pacts)
            val boost = Run(run.index, run.zone, run.seed, context).boost(MonsterRarity.NORMAL, origin = LootOrigin.MONSTER)
            val falloff = (100 + boost.gold) * (boost.share - 1)
            return of(
                total,
                boost.gold + falloff,
                BoostSource.HERO to context.bonus(MonsterRarity.NORMAL).gold,
                BoostSource.MAP to (context.active?.effects?.get(MapStat.GOLD.code) ?: 0.0),
                BoostSource.PACT to (pacts[MapStat.GOLD.code] ?: 0.0) + (pacts[AtlasStat.GOLD.code] ?: 0.0),
                BoostSource.POTION to (potion[AtlasStat.GOLD.code] ?: 0.0),
                BoostSource.FALLOFF to falloff,
            )
        }

        /**
         * Опыт [total] захода [run]: лист героя, [vaal] - зона Ваал захода, атлас с гильдией и зельем, сделки - как их складывает
         * опыт за монстра на сервере; карта-предмет опыта не прибавляет. Сила монстров от уровня героя ([Run.heroScale]) множит опыт
         * поверх прибавок: её доля - тот же множитель в процентах стока, (1 + прибавки) × (множитель − 1).
         */
        fun experience(run: Run, pacts: Map<String, Double>, potion: Map<String, Double>, total: Double, vaal: Boolean): LootBreakdown {
            val context = run.context.copy(pacts = pacts)
            val hero = context.bonus(MonsterRarity.NORMAL).experience
            val zone = if (vaal) context.vaal?.experience ?: 0.0 else 0.0
            val added = hero + zone + context[AtlasStat.EXPERIENCE.code]
            val level = (100 + added) * (run.heroScale.experience - 1)
            return of(
                total,
                added + level,
                BoostSource.HERO to hero,
                BoostSource.VAAL to zone,
                BoostSource.PACT to (pacts[AtlasStat.EXPERIENCE.code] ?: 0.0),
                BoostSource.POTION to (potion[AtlasStat.EXPERIENCE.code] ?: 0.0),
                BoostSource.LEVEL to level,
            )
        }

        /** Разбор прибавки [all] на названные доли [named]; остаток - атлас с гильдией ([REST]). Нулевые доли не показываются. */
        private fun of(total: Double, all: Double, vararg named: Pair<BoostSource, Double>): LootBreakdown {
            val rest = all - named.sumOf { it.second }
            val parts = (named.toList() + (REST to rest)).filter { (_, percent) -> kotlin.math.abs(percent) > EPSILON }.map { (source, percent) -> BoostPart(source, percent) }
            return LootBreakdown(total, parts)
        }

        /** Меньше этого процента доля не показывается: остаток сложения дробей. */
        private const val EPSILON = 1e-6
    }
}
