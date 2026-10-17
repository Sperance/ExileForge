package com.sperance.exileforge.core.character

import com.sperance.exileforge.rules.content.CoreStat

/**
 * Важность характеристики в сравнении «было → стало»: сначала здоровье,
 * затем урон, затем защита, всё прочее - после них. Свёрнутое сравнение показывает строки в этом порядке, внутри группы -
 * по величине изменения ([Comparison][com.sperance.exileforge.core.display.Comparison]). Новая важная группа - запись
 * перечисления на своём месте, а не ветка экрана.
 */
enum class StatPriority(stats: List<CoreStat>) {
    LIFE(listOf(CoreStat.HEALTH, CoreStat.ENERGY_SHIELD)),
    DAMAGE(
        listOf(
            CoreStat.DAMAGE,
            CoreStat.ATTACK_PHYSICAL,
            CoreStat.ATTACK_MAGICAL,
            CoreStat.ATTACK_FIRE,
            CoreStat.ATTACK_COLD,
            CoreStat.ATTACK_LIGHTNING,
            CoreStat.ATTACK_CHAOS,
            CoreStat.ELEMENTAL_DAMAGE,
            CoreStat.SPELL_DAMAGE,
            CoreStat.SKILL_DAMAGE,
        ),
    ),
    DEFENCE(
        listOf(
            CoreStat.ARMOR,
            CoreStat.EVASION,
            CoreStat.BLOCK_CHANCE,
            CoreStat.SPELL_BLOCK,
            CoreStat.DEFLECTION,
            CoreStat.PHYSICAL_REDUCTION,
            CoreStat.SPELL_SUPPRESSION,
            CoreStat.ALL_RESISTANCES,
            CoreStat.ALL_ELEMENTAL_RESISTANCES,
            CoreStat.RESIST_FIRE,
            CoreStat.RESIST_COLD,
            CoreStat.RESIST_LIGHTNING,
            CoreStat.RESIST_CHAOS,
        ),
    ),
    ;

    /** Коды характеристик группы. */
    val codes: Set<String> = stats.mapTo(HashSet()) { it.code }

    companion object {
        /** Место характеристики [stat] в очереди важности: номер её группы, вне групп - после всех. */
        fun rank(stat: String): Int = entries.indexOfFirst { stat in it.codes }.let { if (it < 0) entries.size else it }
    }
}
