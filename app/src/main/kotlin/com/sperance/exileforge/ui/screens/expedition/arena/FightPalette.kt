package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.Rate
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

internal fun MonsterRarity.key() = "enum.monster_rarity.$name"

internal fun rarityTint(rarity: MonsterRarity) = when (rarity) {
    MonsterRarity.NORMAL -> Parchment
    MonsterRarity.MAGIC -> Color(0xFF8888FF)
    MonsterRarity.RARE -> Color(0xFFFFFF77)
    MonsterRarity.UNIQUE -> Color(0xFFAF6025)
}

/**
 * Цвет урона тика недуга [ailment]: яд - цветом своей плитки (кислотным), а не хаосом, как удар; прочие - цветом
 * вида урона [type]. Тот же цвет у тика в бою и у тика ловушки на карте.
 */
internal fun tickTint(ailment: Ailment?, type: DamageType?, onHero: Boolean = false): Color = if (ailment == Ailment.POISONED) ailmentTint(ailment) else damageTint(type, onHero)

/** Значок стихии у числа удара по герою: текстом, не эмодзи; у физики - без значка. */
internal fun damageGlyph(type: DamageType): String? = when (type) {
    DamageType.FIRE -> "✹"
    DamageType.COLD -> "❄\uFE0E"
    DamageType.LIGHTNING -> "ϟ"
    DamageType.CHAOS -> "☣\uFE0E"
    else -> null
}

/** Each ailment's colour: the damage that brings it, or the state it leaves. */
internal fun ailmentTint(ailment: Ailment): Color = when (ailment) {
    Ailment.BURNING -> Ember
    Ailment.CHILLED -> ShieldCyan
    Ailment.FROZEN -> Shaper
    Ailment.SHOCKED -> Color(0xFFFFD34A)
    Ailment.POISONED -> Acid
    Ailment.BLEEDING -> LifeRed
    Ailment.WOUNDED -> Color(0xFF8A3324)
    Ailment.PARCHED -> Color(0xFFC9A05A)
    Ailment.DECAYED -> Elder
    Ailment.DRAINED -> Color(0xFF8E5BC8)
}

internal fun Ailment.key() = "enum.ailment.$name"
internal fun DamageType.key() = "enum.damage.$name"

/**
 * What each effect's window says (a window growing out of the icon): its name,
 * the rule in words, and its figures — strength, seconds left, stacks.
 */
internal fun ailmentTip(view: AilmentView) = Tip(
    ui(view.ailment.key()),
    ui("fight.effect.${view.ailment.name}"),
    ailmentTint(view.ailment),
    listOfNotNull(
        view.strength.takeIf { it > 0 && view.ailment != Ailment.FROZEN }?.let { ui("fight.fact_strength") to strengthFact(view.ailment, it) },
        (ui("fight.fact_left") to ui("fight.fact_seconds", fineNumber(view.seconds))).takeIf { view.seconds > 0 },
        (ui("fight.fact_stacks") to view.stacks.toString()).takeIf { view.stacks > 1 },
    ),
)

/** Как окно недуга [ailment] пишет его силу [value]: урон в секунду, доля или срезанное восстановление запаса. */
private fun strengthFact(ailment: Ailment, value: Double): String = when (ailment) {
    Ailment.BURNING, Ailment.POISONED, Ailment.BLEEDING, Ailment.DRAINED -> ui("fight.fact_dps", Rate.perSecond(value))
    Ailment.CHILLED, Ailment.FROZEN, Ailment.SHOCKED, Ailment.DECAYED -> ui("fight.fact_percent", fineNumber(value))
    Ailment.WOUNDED -> ui("fight.fact_curb_life", fineNumber(value))
    Ailment.PARCHED -> ui("fight.fact_curb_mana", fineNumber(value))
}

internal fun stunTip() = Tip(ui("expedition.stunned"), ui("fight.effect.STUN"), GoldBright)

/** How strongly an ailment washes a portrait: a freeze is the whole face, a bleed a tint. */
internal fun washAmount(ailment: Ailment): Float = when (ailment) {
    Ailment.FROZEN -> .55f
    Ailment.BURNING -> .35f
    Ailment.CHILLED, Ailment.POISONED, Ailment.SHOCKED -> .3f
    Ailment.BLEEDING, Ailment.WOUNDED, Ailment.DRAINED -> .25f
    Ailment.PARCHED, Ailment.DECAYED -> .2f
}

internal fun outcomeColour(outcome: Outcome) = when (outcome) {
    Outcome.WIN -> Vital
    Outcome.LOSS -> LifeRed
}
