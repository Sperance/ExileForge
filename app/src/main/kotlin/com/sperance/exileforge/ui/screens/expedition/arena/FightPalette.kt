package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.*
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

/** Each damage type's colour, on a number and in the log alike. */
internal fun damageTint(type: DamageType?, onHero: Boolean = false): Color = when (type) {
    DamageType.FIRE -> Ember
    DamageType.COLD -> ShieldCyan
    DamageType.LIGHTNING -> Color(0xFFFFD34A)
    DamageType.CHAOS -> Elder
    else -> if (onHero) LifeRed else Parchment
}

/** Each ailment's colour: the damage that brings it, or the state it leaves. */
internal fun ailmentTint(ailment: Ailment): Color = when (ailment) {
    Ailment.BURNING -> Ember
    Ailment.CHILLED -> ShieldCyan
    Ailment.FROZEN -> Shaper
    Ailment.SHOCKED -> Color(0xFFFFD34A)
    Ailment.POISONED -> Vital
    Ailment.BLEEDING -> LifeRed
}

internal fun Ailment.key() = "enum.ailment.$name"
internal fun DamageType.key() = "enum.damage.$name"

/**
 * What each effect's window says (2.72.0, a window growing out of the icon since 2.73.0): its name,
 * the rule in words, and its figures — strength, seconds left, stacks.
 */
internal fun ailmentTip(view: AilmentView) = Tip(
    ui(view.ailment.key()),
    ui("fight.effect.${view.ailment.name}"),
    ailmentTint(view.ailment),
    listOfNotNull(
        view.strength.takeIf { it > 0 && view.ailment != Ailment.FROZEN }?.let {
            ui("fight.fact_strength") to (if (view.ailment.hurts) ui("fight.fact_dps", fineNumber(it)) else ui("fight.fact_percent", fineNumber(it)))
        },
        (ui("fight.fact_left") to ui("fight.fact_seconds", fineNumber(view.seconds))).takeIf { view.seconds > 0 },
        (ui("fight.fact_stacks") to view.stacks.toString()).takeIf { view.stacks > 1 },
    ),
)

internal fun stunTip() = Tip(ui("expedition.stunned"), ui("fight.effect.STUN"), GoldBright)

/** How strongly an ailment washes a portrait: a freeze is the whole face, a bleed a tint. */
internal fun washAmount(ailment: Ailment): Float = when (ailment) {
    Ailment.FROZEN -> .55f
    Ailment.BURNING -> .35f
    Ailment.CHILLED, Ailment.POISONED, Ailment.SHOCKED -> .3f
    Ailment.BLEEDING -> .25f
}

/** A clock in seconds for the portraits' idle motion, ticking once a frame while the frame is on screen. */
@Composable internal fun rememberClock(): State<Float> {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) clock.floatValue += ((now - last) / 1e9f).coerceAtMost(.05f)
                last = now
            }
        }
    }
    return clock
}

internal fun outcomeColour(outcome: Outcome) = when (outcome) {
    Outcome.WIN -> Vital
    Outcome.LOSS -> LifeRed
    Outcome.RETREAT -> Muted
}
