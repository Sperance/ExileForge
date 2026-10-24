package com.sperance.exileforge.ui.screens.expedition

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.combat.FightPart
import com.sperance.exileforge.core.campaign.combat.FightRung
import com.sperance.exileforge.core.display.RowFigure
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.RowShare
import com.sperance.exileforge.core.sheet.RowShift
import com.sperance.exileforge.core.sheet.RunSteps
import com.sperance.exileforge.core.sheet.ShareOrigin
import com.sperance.exileforge.core.sheet.StatExplainer
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.StatSpread
import com.sperance.exileforge.ui.components.SumShare
import com.sperance.exileforge.ui.screens.expedition.arena.effectTint
import com.sperance.exileforge.ui.screens.expedition.arena.effectTitle
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.Vital

/**
 * Вклады в сдвинутую строку листа похода строками раскрытия ([SumShare]) из ступеней [steps] героя [game]: «Герой без похода» первым
 * (у брони и уклонения - сноской рейтинг), затем вклады по порядку ([RunSteps]) - число со знаком в единице строки, зелёным рост,
 * красным спад; под эффектом - его фраза, под зарядами - сколько их по виду ([charges]), под «Сверх предела» - предел.
 */
internal class StatShares(private val game: GameUi, private val steps: RunSteps, private val charges: Map<ChargeKind, Int>) {
    private val explainer = StatExplainer(game)

    /** Строки раскрытия цифры [key]; без контента - пусто. */
    fun of(key: String): List<SumShare> {
        val index = game.index ?: return emptyList()
        val shift = steps.of(key, game.hero?.level, index)
        return listOf(base(key, shift, index)) + shift.shares.map { share(key, it, shift.base, index) }
    }

    private fun base(key: String, shift: RowShift, index: ContentIndex): SumShare {
        val rating = if (shift.base.defence) ui("stat.compare.rating.$key", number(shift.mine.values[key] ?: 0.0)) else null
        return SumShare(ui("stat.compare.base"), shift.base.text(index, shift.mine.stats), rating, Parchment)
    }

    private fun share(key: String, share: RowShare, base: RowFigure, index: ContentIndex): SumShare {
        val text = base.deltaText(share.delta, index)
        val tint = if (share.delta > 0) Vital else LossText
        return when (val origin = share.origin) {
            is ShareOrigin.Run -> SumShare(explainer.runSourceTitle(origin.source), text, accent = Rune, tint = tint)
            is ShareOrigin.RunWhole -> SumShare(ui(if (origin.map) "stat.src.map" else "stat.compare.run"), text, accent = Rune, tint = tint)
            is ShareOrigin.Fight -> fight(key, origin.rung, text, index).copy(tint = tint)
            is ShareOrigin.OverCap -> SumShare(ui("stat.compare.over_cap"), text, ui("stat.compare.cap", number(origin.cap)), Caution, tint)
        }
    }

    private fun fight(key: String, rung: FightRung, text: String, index: ContentIndex): SumShare = when (rung.part) {
        FightPart.EFFECT -> rung.effect?.let { view -> SumShare(effectTitle(view), text, phrase(key, rung, index), effectTint(view)) } ?: SumShare(ui("fight.src.buff"), text, phrase(key, rung, index))
        FightPart.CHARGES -> SumShare(ui("stat.compare.charges"), text, chargeNote())
        FightPart.POWERS -> SumShare(ui("stat.compare.powers"), text)
        FightPart.CONDITIONS -> SumShare(ui("stat.compare.conditions"), text)
        FightPart.LOW_LIFE -> SumShare(ui("stat.compare.low_life"), text)
        FightPart.AURAS -> SumShare(ui("stat.compare.auras"), text)
    }

    /** Фраза строк эффекта, что двигают цифру [key]; не нашлось - все его строки. */
    private fun phrase(key: String, rung: FightRung, index: ContentIndex): String {
        val own = rung.lines.filter { it.stat == key || StatSpread.touches(it.stat, it.op, key) }.ifEmpty { rung.lines }
        return own.joinToString("; ") { SkillText.statLine(it.stat, it.op, it.value, index) }
    }

    /** Заряды по виду: «3 заряда выносливости, 1 заряд ярости»; без зарядов - null. */
    private fun chargeNote(): String? = charges.filterValues { it > 0 }.entries
        .joinToString(", ") { (kind, count) -> ui(pluralKey("stat.compare.charge.${kind.name}", count), count) }
        .ifEmpty { null }
}
