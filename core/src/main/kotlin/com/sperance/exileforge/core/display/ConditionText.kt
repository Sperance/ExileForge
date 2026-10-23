package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.SlotAilment
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.SlotConditionGroup
import com.sperance.exileforge.rules.content.SlotConditionKind
import com.sperance.exileforge.rules.content.SlotParam

/**
 * Слова условия слота умения и места пояса ([SlotCondition]): имя вида в листе выбора, краткий итог для ячейки и кнопки
 * («Здоровье ≤ 50%», «Щит пробит», «Яд +2»), число параметра крупно, пояснение вида. Ноль щита - «пробит», ноль времени - «сразу».
 */
object ConditionText {
    /** Имя вида в листе выбора: «Здоровье», «Число врагов». */
    fun kind(kind: SlotConditionKind): String = ui("skills.condition.${kind.name}")

    /** Подпись группы видов: «Герой», «Враги», «Бой». */
    fun group(group: SlotConditionGroup): String = ui("skills.condition.group.${group.name}")

    /** Что вид проверяет - строка над его параметром. */
    fun note(kind: SlotConditionKind): String = ui("skills.condition.note.${kind.name}", number(SlotConditionKind.BURST_SECONDS))

    /** Ступень редкости врага в выборе: «любой», «редкий+», «босс». */
    fun rarity(rarity: MonsterRarity): String = ui("skills.condition.rarity.${rarity.name}")

    /** Недуг «Под недугом»: «Горение», «Холод». */
    fun ailment(ailment: SlotAilment): String = ui("skills.condition.ailment.${ailment.name}")

    /** Краткий итог условия (до ~16 знаков): ячейка Гримуара, место пояса, кнопка «Сохранить». */
    fun short(condition: SlotCondition): String {
        val kind = condition.kind
        return when {
            kind.param == SlotParam.AILMENTS -> ailments(condition.ailments)
            kind.param == SlotParam.RARITY -> ui("skills.condition.short.${kind.name}", rarity(condition.rarity))
            zero(condition) -> ui("skills.condition.short.${kind.name}.zero")
            kind.param.numeric -> ui("skills.condition.short.${kind.name}", condition.value)
            else -> ui("skills.condition.short.${kind.name}")
        }
    }

    /** Число параметра крупно: «50%», «12 с», «пробит», «сразу»; у вида без числа - пусто. */
    fun value(condition: SlotCondition): String = when {
        zero(condition) -> ui("skills.condition.zero.${condition.kind.name}")
        condition.kind.param == SlotParam.PERCENT || condition.kind.param == SlotParam.SECONDS -> ui("skills.condition.value.${condition.kind.param.name}", condition.value)
        condition.kind.param.numeric -> condition.value.toString()
        else -> ""
    }

    /** Ноль с особым словом: пробитый щит и «сразу» в начале боя. */
    private fun zero(condition: SlotCondition): Boolean = condition.value == 0 && condition.kind in ZERO_KINDS

    /** Набор недугов кратко: один - его имя, все - «Любой недуг», иначе первый и «+N». */
    private fun ailments(set: Set<SlotAilment>): String {
        val chosen = SlotAilment.entries.filter { it in set }
        return when (chosen.size) {
            SlotAilment.entries.size -> ui("skills.condition.short.AILING.all")
            1 -> ailment(chosen.first())
            else -> ui("skills.condition.short.AILING.some", ailment(chosen.first()), chosen.size - 1)
        }
    }

    /** Виды, у которых ноль - своё слово. */
    val ZERO_KINDS: Set<SlotConditionKind> = setOf(SlotConditionKind.SHIELD, SlotConditionKind.TIME)
}
