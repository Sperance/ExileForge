package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FateEffect
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.rules.content.FateSide
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// ==================== Предначертание в бою (4.6.0) ====================

/**
 * Счёт Предначертания на весь заход (забег, испытание, Разлом): сколько раз «Второе дыхание» ([FateLever.CHEAT_DEATH]) уже
 * спасло героя. Один на заход - бои захода делят его; прогон шансов берёт свой, свежий.
 */
class FateRun {
    var cheated: Int = 0
        internal set
}

/**
 * Состояние рычагов одного боя (4.6.0): раунды боя - один бой, «в начале боя» и «первый удар» - раз на [Battle].
 */
internal class FateFight {
    /** Попавших ударов героя за бой ([FateLever.EVERY_NTH_AILMENT]). */
    var hits = 0

    /** Заряд мести: приняв крит, следующий удар героя - крит ([FateLever.AVENGE_CRIT]). */
    var avenge = false

    /** Первый удар героя уже был ([FateLever.OPENING_CRIT]); [opening] - он идёт сейчас и вешает свои недуги, попав. */
    var struck = false
    var opening = false

    /** Первое умение боя ещё бесплатно ([FateLever.FIRST_SKILL_FREE]). */
    var freeSkill = true
}

/** Рычаг клиента [effect] с его откликом [reaction] (4.6.0): числа уже на силе дара ([FateEffects]). */
internal class FatedLever(val effect: FateEffect, val reaction: FateReaction) {
    val value: Double get() = effect.value

    companion object {
        /** Рычаги боя дара [fate]: только клиентские, у каждого - свой отклик; рычаг без отклика в бою (обзор карты) пропущен. */
        fun of(fate: FateEffects): List<FatedLever> = fate.effects.filter { it.lever.side == FateSide.CLIENT }.mapNotNull { e -> FateReaction.of(e.lever)?.let { FatedLever(e, it) } }
    }
}

/**
 * Отклик рычага Предначертания (4.6.0): что рычаг делает в каждый момент боя. Момент, в который рычагу нечего делать, -
 * ничего. Новый рычаг - новая реализация и ветка [of]; бой видов рычагов не проверяет. Семантика - KDoc [FateLever].
 */
internal sealed interface FateReaction {
    /** Бой открылся: барьер, уклонение, первый ход питомца. */
    fun opens(battle: Battle, at: FatedLever) = Unit

    /** Во сколько раз удар героя тяжелее сейчас. */
    fun heavier(battle: Battle, at: FatedLever): Double = 1.0

    /** Этот удар героя - крит наверняка. */
    fun sure(battle: Battle, at: FatedLever): Boolean = false

    /** Удар героя вышел - [kind] - и забрал заряды «наверняка». */
    fun swung(battle: Battle, at: FatedLever, kind: HitKind) = Unit

    /** Удар героя попал в [target], отдав [taken]. */
    fun landed(battle: Battle, at: FatedLever, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind) = Unit

    /** По герою попали: [kind] - как. */
    fun struck(battle: Battle, at: FatedLever, kind: HitKind) = Unit

    /** Что из [amount] одного удара по герою доходит до барьера и щита. */
    fun capped(battle: Battle, at: FatedLever, amount: Double): Double = amount

    /** Недуг [fresh] ложится на героя: каким он ляжет. */
    fun ailed(battle: Battle, at: FatedLever, fresh: ActiveAilment): ActiveAilment = fresh

    /** Героя оглушают на [seconds]: true - рычаг ответил вместо оглушения. */
    fun stunned(battle: Battle, at: FatedLever, seconds: Double): Boolean = false

    /** Герой пал: true - рычаг поднял его. */
    fun dies(battle: Battle, at: FatedLever): Boolean = false

    /** Враг [fallen] пал с недугами [ailing]. */
    fun fell(battle: Battle, at: FatedLever, fallen: Fighter, ailing: List<ActiveAilment>) = Unit

    /** Умение героя сейчас бесплатно и без перезарядки; [use] - его применяют. */
    fun free(battle: Battle, at: FatedLever, use: Boolean): Boolean = false

    companion object {
        fun of(lever: FateLever): FateReaction? = when (lever) {
            FateLever.MISSING_LIFE_DAMAGE -> MissingLifeDamage

            FateLever.AVENGE_CRIT -> AvengeCrit

            FateLever.KILL_SPREAD_AILMENTS -> KillSpread

            FateLever.EVERY_NTH_AILMENT -> EveryNthAilment

            FateLever.KILL_BURST -> KillBurst

            FateLever.OPENING_CRIT -> OpeningCrit

            FateLever.EXECUTE -> Execute

            FateLever.PET_FIRST -> PetFirst

            FateLever.SECOND_PET -> null

            FateLever.CHEAT_DEATH -> CheatDeath

            FateLever.OPENING_EVASION -> OpeningEvasion

            FateLever.FREEZE_TO_SLOW -> FreezeToSlow

            FateLever.HIT_CAP -> HitCap

            FateLever.STUN_TO_SLOW -> StunToSlow

            FateLever.OPENING_BARRIER -> OpeningBarrier

            FateLever.FIRST_SKILL_FREE -> FirstSkillFree

            // Обзор карты - забег (`FateSight`), второй питомец - состав боя ([Battle.helper]), прочие - сервер
            FateLever.FOG_SIGHT, FateLever.HERO_EXPERIENCE, FateLever.SKILL_EXPERIENCE, FateLever.GOLD, FateLever.DEATH_PENALTY,
            FateLever.MAP_DROP, FateLever.MAP_TIER, FateLever.MAP_RARITY, FateLever.MAP_LINES, FateLever.MAP_KEEP, FateLever.MAP_CHEST_STEP,
            FateLever.BOSS_MAP, FateLever.LINEAGE, FateLever.COLLECTOR, FateLever.ATLAS_POINTS, FateLever.TRACKER_PACK, FateLever.ABYSS_DEPTH,
            FateLever.RIFT_GIFT, FateLever.TROPHIES, FateLever.ORB_KEEP, FateLever.AFFIX_TIER, FateLever.MERCHANT_OFFERS,
            FateLever.MERCHANT_DISCOUNT, FateLever.AUCTION_FEE, FateLever.AUCTION_SLOTS,
            -> null
        }
    }
}

/** Урон героя больше за каждые `step`% недостающего здоровья, до `cap`. */
private data object MissingLifeDamage : FateReaction {
    override fun heavier(battle: Battle, at: FatedLever): Double {
        val hero = battle.heroFighter
        if (hero.body.maxLife <= 0) return 1.0
        val missing = (1 - hero.life / hero.body.maxLife).coerceIn(0.0, 1.0) * 100
        return 1 + min(at.effect.cap, floor(missing / at.effect.step) * at.value) / 100
    }
}

/** Принятый крит заряжает следующий удар героя критом; заряд один. */
private data object AvengeCrit : FateReaction {
    override fun struck(battle: Battle, at: FatedLever, kind: HitKind) {
        if (kind == HitKind.CRIT) battle.fateFight.avenge = true
    }

    override fun sure(battle: Battle, at: FatedLever): Boolean = battle.fateFight.avenge

    override fun swung(battle: Battle, at: FatedLever, kind: HitKind) {
        if (!battle.fateFight.avenge) return
        battle.fateFight.avenge = false
        if (kind == HitKind.CRIT) battle.fateNote()
    }
}

/** Недуги павшего - следующему живому врагу боя, с остатком срока и силой. */
private data object KillSpread : FateReaction {
    override fun fell(battle: Battle, at: FatedLever, fallen: Fighter, ailing: List<ActiveAilment>) {
        if (ailing.isEmpty()) return
        val next = battle.target() ?: battle.foeFighters.firstOrNull { it.alive } ?: return
        ailing.filter { it.until > battle.time }.forEach { active -> battle.place(next, active, battle.ruleOf[active.ailment]?.first?.stacks == true) }
        battle.fateNote()
    }
}

/** Каждый N-й попавший удар героя вешает случайный недуг из списка - как от доли удара его стихией. */
private data object EveryNthAilment : FateReaction {
    override fun landed(battle: Battle, at: FatedLever, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind) {
        val fight = battle.fateFight
        fight.hits++
        if (fight.hits % at.effect.every != 0 || !target.alive) return
        val ailments = at.effect.ailments.mapNotNull { Ailment.of(it) }
        if (ailments.isEmpty()) return
        val ailment = ailments[battle.random.nextInt(ailments.size)]
        val type = battle.ruleOf[ailment]?.second ?: return
        val share = taken.values.sum() * at.value / 100
        if (share > 0 && battle.afflict(battle.heroFighter, target, ailment, mapOf(type to share)) != null) battle.fateNote()
    }
}

/** Павший взрывается: всем прочим живым врагам - доля его здоровья физическим уроном, броня режет. */
private data object KillBurst : FateReaction {
    override fun fell(battle: Battle, at: FatedLever, fallen: Fighter, ailing: List<ActiveAilment>) {
        val amount = fallen.body.maxLife * at.value / 100
        val hit = battle.foeFighters.filter { it.alive && it !== fallen && !it.invulnerable }
        if (amount <= 0 || hit.isEmpty()) return
        battle.fateNote(amount)
        hit.forEach { battle.fateBurst(it, amount) }
    }
}

/** Первый удар героя в бою - крит наверняка; попав, вешает недуги рычага. */
private data object OpeningCrit : FateReaction {
    override fun sure(battle: Battle, at: FatedLever): Boolean = !battle.fateFight.struck

    override fun swung(battle: Battle, at: FatedLever, kind: HitKind) {
        val fight = battle.fateFight
        if (fight.struck) return
        fight.struck = true
        fight.opening = kind == HitKind.CRIT || kind == HitKind.HIT
    }

    override fun landed(battle: Battle, at: FatedLever, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind) {
        val fight = battle.fateFight
        if (!fight.opening) return
        fight.opening = false
        if (target.alive) at.effect.ailments.mapNotNull { Ailment.of(it) }.forEach { battle.afflict(battle.heroFighter, target, it, taken) }
        battle.fateNote()
    }
}

/** Попавший удар героя добивает врага ниже порога здоровья; у босса и стража - ниже своего порога. */
private data object Execute : FateReaction {
    override fun landed(battle: Battle, at: FatedLever, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind) {
        if (!target.alive || target.invulnerable || target.side != Side.MONSTER) return
        val foe = battle.foes[target.index]
        val threshold = if (foe.rarity == MonsterRarity.UNIQUE || foe.guards) at.effect.boss else at.value
        if (target.life >= target.body.maxLife * threshold / 100) return
        target.life = 0.0
        target.shield = 0.0
        battle.fateNote()
    }
}

/** Боевой питомец (и второй) бьёт первым: его первая атака - с начала боя. */
private data object PetFirst : FateReaction {
    override fun opens(battle: Battle, at: FatedLever) {
        listOfNotNull(battle.allyFighter, battle.helperFighter).forEach { it.nextAttack = 0.0 }
    }
}

/** Смертельный удар оставляет немного здоровья и неуязвимость; заряды - на заход. */
private data object CheatDeath : FateReaction {
    override fun dies(battle: Battle, at: FatedLever): Boolean {
        if (battle.fateRun.cheated >= at.effect.charges) return false
        battle.fateRun.cheated++
        val hero = battle.heroFighter
        hero.life = min(hero.body.maxLife, max(1.0, at.value))
        hero.invulnerableUntil = max(hero.invulnerableUntil, battle.time + at.effect.duration)
        battle.fateNote(at.value)
        return true
    }
}

/** Первые секунды боя уклонение героя выше. */
private data object OpeningEvasion : FateReaction {
    override fun opens(battle: Battle, at: FatedLever) {
        battle.buff(battle.heroFighter, FATE_SOURCE, listOf(StatLine(CoreStat.EVASION.code, Op.INCREASED, at.value)), at.effect.duration)
    }
}

/** Заморозка на героя - замедление на тот же срок. */
private data object FreezeToSlow : FateReaction {
    override fun ailed(battle: Battle, at: FatedLever, fresh: ActiveAilment): ActiveAilment = if (fresh.ailment != Ailment.FROZEN) fresh else fresh.copy(ailment = Ailment.CHILLED, magnitude = at.value)
}

/** Один удар снимает не больше доли максимума здоровья героя. */
private data object HitCap : FateReaction {
    override fun capped(battle: Battle, at: FatedLever, amount: Double): Double = min(amount, battle.heroFighter.body.maxLife * at.value / 100)
}

/** Оглушение героя - замедление на тот же срок. */
private data object StunToSlow : FateReaction {
    override fun stunned(battle: Battle, at: FatedLever, seconds: Double): Boolean {
        val hero = battle.heroFighter
        battle.place(hero, ActiveAilment(Ailment.CHILLED, battle.time + seconds, at.value, seconds, Side.MONSTER, 0, false), false)
        return true
    }
}

/** Барьер в начале боя: принимает урон первым и не восстанавливается. */
private data object OpeningBarrier : FateReaction {
    override fun opens(battle: Battle, at: FatedLever) {
        val hero = battle.heroFighter
        hero.barrier += hero.body.maxLife * at.value / 100
        hero.barrierUntil = FOREVER
    }
}

/** Первое умение боя - без маны и перезарядки. */
private data object FirstSkillFree : FateReaction {
    override fun free(battle: Battle, at: FatedLever, use: Boolean): Boolean {
        val fight = battle.fateFight
        if (!fight.freeSkill) return false
        if (use) {
            fight.freeSkill = false
            battle.fateNote()
        }
        return true
    }
}

/** Источник строк и эффектов Предначертания на герое. */
const val FATE_SOURCE = "FATE"

// ==================== Моменты боя ====================

/** Бой открылся: рычаги начала боя. */
internal fun Battle.fateOpens() = fated.forEach { it.reaction.opens(this, it) }

/** Во сколько раз удар [me] тяжелее по Предначертанию: только удар героя. */
internal fun Battle.fateHeavier(me: Fighter): Double = if (me !== heroFighter || fated.isEmpty()) 1.0 else fated.fold(1.0) { k, at -> k * at.reaction.heavier(this, at) }

/** Удар героя - крит наверняка по Предначертанию. */
internal fun Battle.fateSure(me: Fighter): Boolean = me === heroFighter && fated.any { it.reaction.sure(this, it) }

/** Удар [me] вышел: у героя - заряды «наверняка» уходят. */
internal fun Battle.fateSwung(me: Fighter, kind: HitKind) {
    if (me === heroFighter) fated.forEach { it.reaction.swung(this, it, kind) }
}

/** Удар [me] попал в [target]: удар героя - рычагам героя, удар по герою - рычагам защиты. */
internal fun Battle.fateLanded(me: Fighter, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind) {
    if (fated.isEmpty()) return
    if (me === heroFighter) fated.forEach { it.reaction.landed(this, it, target, taken, kind) }
    if (target === heroFighter) fated.forEach { it.reaction.struck(this, it, kind) }
}

/** Удар по [target] с потолком Предначертания: у героя - не больше доли максимума, урон по типам - в той же доле. */
internal fun Battle.fateCapped(target: Fighter, taken: Map<DamageType, Double>): Map<DamageType, Double> {
    if (target !== heroFighter || fated.isEmpty()) return taken
    val total = taken.values.sum()
    val capped = fated.fold(total) { amount, at -> at.reaction.capped(this, at, amount) }
    if (capped >= total || total <= 0) return taken
    return taken.mapValues { it.value * capped / total }
}

/** Недуг [fresh] на [target]: на герое - каким его сделают рычаги. */
internal fun Battle.fateAiled(target: Fighter, fresh: ActiveAilment): ActiveAilment = if (target !== heroFighter || fated.isEmpty()) fresh else fated.fold(fresh) { a, at -> at.reaction.ailed(this, at, a) }

/**
 * Оглушение [target] на [seconds] (4.6.0): героя рычаг может замедлить вместо него. True - оглушён.
 */
internal fun Battle.stun(target: Fighter, seconds: Double): Boolean {
    if (target === heroFighter && fated.any { it.reaction.stunned(this, it, seconds) }) return false
    target.heldUntil = max(target.heldUntil, time + seconds)
    return true
}

/** Герой пал: рычаг может поднять его. True - поднят. */
internal fun Battle.fateDies(): Boolean = !heroFighter.alive && fated.any { it.reaction.dies(this, it) }

/** Враг пал: его недуги и взрыв. */
internal fun Battle.fateFell(fallen: Fighter, ailing: List<ActiveAilment>) = fated.forEach { it.reaction.fell(this, it, fallen, ailing) }

/** Умение сейчас бесплатно по Предначертанию; [use] - его применяют, заряд уходит. */
internal fun Battle.fateFree(use: Boolean): Boolean = fated.any { it.reaction.free(this, it, use) }

/** Строка лога: Предначертание героя сработало. */
internal fun Battle.fateNote(value: Double = 0.0) = note(heroFighter, NoteKind.FATE, kit.fate.effects.fate?.code.orEmpty(), value)

/** Взрыв павшего по [target]: физический урон [amount], броня режет, затем щит и здоровье. */
private fun Battle.fateBurst(target: Fighter, amount: Double) {
    val dealt = amount * (1 - target.body.physicalMitigation(amount, rules.armour.factor)) * target.body.damageTaken(DamageType.PHYSICAL)
    if (dealt <= 0) return
    var rest = dealt
    if (target.barrier > 0) {
        val soaked = min(target.barrier, rest)
        target.barrier -= soaked
        rest -= soaked
    }
    val absorbed = if (shieldless(target)) 0.0 else min(target.shield, rest)
    target.shield -= absorbed
    target.life = max(0.0, target.life - (rest - absorbed))
    target.lastHit = time
    if (!target.alive) fell(target, killer = heroFighter)
}
