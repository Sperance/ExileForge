package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.MonsterSkill
import com.sperance.exileforge.rules.content.MonsterTrait
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.sheet.FlaskKind

/**
 * An ailment on a fighter: what, until when, how hard, and who put it there — the side and, for a
 * monster's, which foe of the pack ([foe], since 2.70.0).
 */
data class ActiveAilment(
    val ailment: Ailment,
    val until: Double,
    val magnitude: Double,
    val duration: Double,
    val source: Side,
    val foe: Int = 0,
    /** Put there by a spell (2.78.0): a kill by it is a spell's kill. */
    val spell: Boolean = false,
    /** Burns as chaos (3.33.0): an ignite of a striker whose ignites deal chaos goes around the shield. */
    val chaos: Boolean = false,
)

/**
 * One foe of the pack as the fight takes it (2.70.0): its sheet; every foe on screen strikes from the first
 * second and can be struck by any weapon — there are no rows. Since 2.78.0 its [rarity] tells a skill waiting for a rare or a boss, and its [skills] are what it casts
 * for its mana — a boss's own, a caster's spell of its element, one a mad essence borrowed.
 */

/**
 * A combat pet in a fight (3.5.0): [code] its species, [body] its sheet, a [tank] holds every blow on itself,
 * [heal] mends the hero so many percent of their life a second, [drawFire] the share of blows it draws otherwise.
 */
class Ally(val code: String, val body: Combatant, val tank: Boolean, val heal: Double, val drawFire: Double)

/** The traits of this roll (3.73.0), as the content names them. */
fun com.sperance.exileforge.rules.roll.RolledMonster.traitsIn(index: com.sperance.exileforge.rules.content.ContentIndex): List<MonsterTrait> = traits.mapNotNull(index.campaign.traits.byCode::get)

data class Foe(
    val body: Combatant,
    val rarity: MonsterRarity = MonsterRarity.NORMAL,
    val skills: List<MonsterSkill> = emptyList(),
    /** Its roll at the fight's [level] (3.37.0): what the log's card lays its stats out with. */
    val origin: com.sperance.exileforge.rules.roll.RolledMonster? = null,
    val level: Int = 0,
    /** Its traits (3.73.0) and their strength for its rarity: their lines are on [body] already, their answers the fight plays. */
    val traits: List<MonsterTrait> = emptyList(),
    val traitPower: Double = 1.0,
    /** Фазы босса (3.92.0): шаблон [phase] и его шаги по убыванию порога; пусто - фаз нет. */
    val phase: String? = null,
    val phases: List<FoePhase> = emptyList(),
    /** Свита (3.92.0): номер босса, чья фаза её зовёт; в бой она встаёт только по зову, добычи не даёт и падает с ним. */
    val summonOf: Int? = null,
    /**
     * Тотемы (3.93.0): свои тотемы босса [totems] - ставит случайный раз в [totemEvery] секунд, первый через [totemFirst];
     * [slots] - слоты вокруг него для свиты и тотемов, 0 - у врага их нет.
     */
    val totems: List<FoeTotem> = emptyList(),
    val totemEvery: Double = 0.0,
    val totemFirst: Double = 0.0,
    val slots: Int = 0,
    /** Осквернён (4.0.0): несёт печать Скверны - цель условия `VS_BLIGHTED`, а герой со снижением от Скверны получает от него меньше. */
    val tainted: Boolean = false,
) {
    val summoned: Boolean get() = summonOf != null
}

/** What lies on a fighter for a while (2.78.0). */
enum class EffectKind { BUFF, CURSE, FLASK }

/**
 * A buff, a curse or a flask's draught on a fighter (2.78.0): its [lines] until [until], named by its
 * [source] — a skill's code or a flask's. [counter] strikes back at a blocked blow with that percent of
 * the weapon; [slot] is a flask's place on the belt.
 */
data class TimedEffect(
    val kind: EffectKind,
    val source: String,
    val lines: List<StatLine>,
    val until: Double,
    val duration: Double,
    val counter: Double = 0.0,
    val slot: Int = -1,
)

/**
 * What the hero carries from fight to fight (2.78.0): life, mana, each flask's charges and how long
 * its draught still runs, in seconds.
 */
data class HeroPools(
    val life: Double,
    val mana: Double,
    val charges: List<Double> = emptyList(),
    val flaskLeft: List<Double> = emptyList(),
    val rates: List<DraughtRate> = emptyList(),
)

/**
 * How much life and mana a running draught still gives each second (2.81.0). It rides with the draught's
 * time left, so a flask drunk at the end of a fight keeps healing on the map and in the next fight.
 */
data class DraughtRate(val life: Double = 0.0, val mana: Double = 0.0) {
    val flows: Boolean get() = life > 0 || mana > 0
}

/** An active slot as the fight's buttons draw it (2.78.0): how far it has recovered (1 ready), and whether the mana is there. */
data class SkillView(
    val slot: Int,
    val code: String,
    val icon: String,
    val level: Int,
    val cost: Int,
    val ready: Float,
    val affordable: Boolean,
    val condition: SlotCondition,
    val seconds: Double = 0.0,
    /** Сколько ещё секунд слот заперт «Запечатыванием» Стража Врат (3.96.0); 0 - свободен. */
    val locked: Double = 0.0,
)

/** A flask of the belt as its button draws it (2.78.0): charges, the price of a draught, and how much of one is left (0 none). */
data class FlaskView(
    val slot: Int,
    val code: String,
    val kind: FlaskKind,
    val charges: Int,
    val maxCharges: Int,
    val perUse: Int,
    val active: Float,
    val condition: SlotCondition,
    /** Сколько ещё секунд флакон заперт «Запечатыванием» Стража Врат (3.96.0); 0 - свободен. */
    val locked: Double = 0.0,
) {
    val usable: Boolean get() = charges >= perUse && active <= 0f && locked <= 0.0
}

/** A buff or a curse on a fighter as its tile shows it (2.78.0): what, of which kind, and how much of it is left. */
data class EffectView(val source: String, val kind: EffectKind, val left: Float, val seconds: Double, val icon: String = "", val buff: BuffKind? = null)

/**
 * Whom the hero strikes when the player has not said (2.70.0), by class — the owner's table: a
 * Marauder, a Duelist and a Ranger go for the one that presses hardest, a Shadow finishes the
 * weakest, a Witch the one least resistant to her leading element, a Templar answers whoever
 * struck last, a Scion finishes the weakest while healthy and answers when hurt.
 */
enum class TargetRule {
    THREAT,
    WEAKEST,
    EXPOSED,
    AVENGE,
    ADAPTIVE,
    ;

    companion object {
        fun of(classCode: String?): TargetRule = when (classCode) {
            "SHADOW" -> WEAKEST
            "WITCH" -> EXPOSED
            "TEMPLAR" -> AVENGE
            "SCION" -> ADAPTIVE
            else -> THREAT
        }
    }
}

/**
 * The hero's side of a fight beyond the sheet (2.70.0): whom they pick by [rule]. Any weapon reaches every
 * foe on screen — the fight has no rows.
 */
data class HeroStance(val rule: TargetRule = TargetRule.THREAT) {
    companion object {
        fun of(classCode: String?) = HeroStance(TargetRule.of(classCode))
    }
}
