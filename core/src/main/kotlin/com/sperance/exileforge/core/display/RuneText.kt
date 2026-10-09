package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.RuneDefinition
import com.sperance.exileforge.rules.content.RuneOp
import com.sperance.exileforge.rules.text.LocaleKey
import kotlin.math.floor

/** Тир умения римской цифрой (4.4.0) - из словаря: «I», «II», «III». */
fun roman(tier: Int): String = ui("skills.tier_mark.$tier")

/**
 * Руна умения словами игрока (4.4.0): семейство и действие - словарь сервера (`EnumRuneFamily`, `rune.op.*`), виды умений и
 * стихийное действие - словарь клиента. Числа руны растут на её силу [power] (`1 + STOCK_RUNE_EFFECT / 100`), как в бою.
 */
object RuneText {
    fun family(rune: RuneDefinition): String = locOr(LocaleKey.enumLabel("EnumRuneFamily", rune.family.name), displayName(rune.family.name))

    /** Виды умений, в которые руна встаёт, через запятую. */
    fun types(rune: RuneDefinition): String = rune.types.joinToString(", ") { ui("skills.type.${it.name}") }

    /** Что руна делает с умением, одной строкой. */
    fun effect(rune: RuneDefinition, power: Double = 1.0): String {
        val value = rune.value * power
        val count = floor(value).toInt().toString()
        return when (rune.op) {
            RuneOp.SPLIT, RuneOp.FORK, RuneOp.CHARGE -> op(rune, count)
            RuneOp.CHAIN -> op(rune, count, number(rune.extra))
            RuneOp.ECHO, RuneOp.BARRIER -> op(rune, number(value), number(rune.extra))
            RuneOp.COOLDOWN, RuneOp.MANA, RuneOp.PREPARE, RuneOp.DURATION, RuneOp.LEECH, RuneOp.EFFECT -> op(rune, number(value))
            RuneOp.INFUSE -> infuse(rune, value, rune.extra * power)
        }
    }

    private fun op(rune: RuneDefinition, vararg args: String): String = loc("rune.op.${rune.op.name}", args.toList())

    /** Стихия руны: какую стихию она даёт удару и шанс какого состояния прибавляет; руна без стихии - только шанс. */
    private fun infuse(rune: RuneDefinition, share: Double, chance: Double): String {
        val ailment = SkillText.ailment(rune.ailment.orEmpty())
        val element = rune.element ?: return ui("runes.effect_ailment", number(chance), ailment)
        return ui("runes.effect_infuse", SkillText.element(element), number(share), number(chance), ailment)
    }
}
