package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.CampaignFile
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.sheet.StatCaps
import com.sperance.exileforge.rules.sheet.StatReading

/**
 * A figure a fight counts only up to a ceiling: a resistance under its maximum; block, critical chance and
 * physical reduction under their own. Каждый предел реестра правил `StatCaps`: потолки боя, подавление и
 * отклонение, длительности на себе, доли, скорость атаки и чар, свет и шаг карты.
 * [gathered] - сколько набрано (у стихии - с «всеми сопротивлениями», у скорости чар - со скоростью применения умений),
 * [effective] - что бой читает на самом деле, [base] - предел правил без поднятий, [raises] - что его поднимает и сколько,
 * [cap] - где он стоит, [hard] - выше чего не поднять, [floor] - ниже чего бой не читает.
 */
data class StatLimit(
    val stat: String,
    val gathered: Double,
    val effective: Double,
    val base: Double,
    val raises: List<Pair<String, Double>>,
    val cap: Double,
    val hard: Double?,
    val floor: Double? = null,
) {
    /** What lies above the cap and does nothing in a fight. */
    val over: Double get() = (gathered - cap).coerceAtLeast(0.0)

    /** Бой читает не набранное: оно выше предела или ниже дна - сводка печатает «предел; набрано N». */
    val clamped: Boolean get() = effective != gathered
}

/** The ceilings of the hero's figures, read from the rules' one registry (`StatCaps`) - exactly as the fight applies them. */
object StatLimits {
    fun of(stat: String, stats: Map<String, Double>, campaign: CampaignFile): StatLimit? {
        val cap = StatCaps.of(stat, campaign) ?: return null
        val read = { code: String -> stats[code] ?: 0.0 }
        val raises = cap.raises.map { it to read(it) }.filter { it.second != 0.0 }
        return StatLimit(stat, cap.gathered(stat, read), cap.effective(stat, read), cap.base, raises, cap.at(read), cap.hard, cap.floor)
    }

    /** Шанс уклонения героя уровня [level]: от меткости эталонного врага его уровня (`ReferenceFoe`), под пределом правил. */
    fun evasion(stats: Map<String, Double>, level: Int, index: ContentIndex): StatReading = StatCaps.evasion({ stats[it] ?: 0.0 }, level, index)

    /**
     * Защита процентом: уклонение - шанс против меткости врага уровня героя [level], броня - снижение
     * физического урона от удара эталонного врага уровня героя (`StatCaps.physical`, `ReferenceFoe.hit`). Сводка печатает его главным
     * числом, сравнение - разницей в п.п.; прочие характеристики - null.
     */
    fun defence(stat: String, stats: Map<String, Double>, level: Int, index: ContentIndex): StatReading? {
        val read = { code: String -> stats[code] ?: 0.0 }
        return when (stat) {
            EVASION -> evasion(stats, level, index)
            ARMOUR -> StatCaps.physical(read, index.referenceFoe.hit(level), index.campaign.combat)
            else -> null
        }
    }

    /** Характеристика, под которой сводка печатает шанс уклонения. */
    val EVASION: String = CoreStat.EVASION.code

    /** Броня: сводка печатает снижение физического урона. */
    val ARMOUR: String = CoreStat.ARMOR.code
}
