package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.RollKey
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.FOREVER
import com.sperance.exileforge.core.campaign.combat.Battle.Companion.NEVER
import com.sperance.exileforge.core.campaign.combat.Battle.Fighter
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FateEffect
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateKnob
import com.sperance.exileforge.rules.content.FateKnobValue
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.rules.content.FateSide
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.run.Run
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// ==================== Предначертание в бою (4.6.0) ====================

/**
 * Счёт Предначертания на весь заход (забег, испытание, Разлом): бои захода делят его, прогон шансов берёт свой, свежий.
 * [overflow] - перелив маны ([FateLever.MANA_OVERFLOW]) до конца захода; [won] - боёв, выигранных в заходе
 * ([FateLever.WIN_STREAK]): павшие паки жетонов [killed] и [vaalKilled] (ключи `жетон × PACK_SLOTS + член`) и стражи, счёт -
 * правилом сервера [Run.fightsWon] ([recount]). В испытаниях и Разломе счёта нет - он ноль. Что судьбоносные строки (4.6.2)
 * несут из боя в бой - [stored], [barrier], [warded].
 */
class FateRun(killed: Collection<Int> = emptyList(), vaalKilled: Collection<Int> = emptyList()) {
    private val killed = killed.toMutableSet()
    private val vaalKilled = vaalKilled.toMutableSet()
    private var bosses = 0

    /** Перелив маны сверх максимума, в единицах маны. */
    var overflow: Double = 0.0
        internal set

    /** Выигранных в заходе боёв на последнем [recount]. */
    var won: Int = 0
        private set

    /** Запас Зеркала урона, с которым начнётся следующий бой захода (4.6.2, [FateKnob.STORE_KEEP]). */
    var stored: Double = 0.0
        internal set

    /** Барьер Крови за кровь, с которым начнётся следующий бой захода (4.6.2, [FateKnob.BARRIER_CARRY]). */
    var barrier: Double = 0.0
        internal set

    /** Счёт [won], за который неуязвимость Разгона уже дана (4.6.2, [FateKnob.STREAK_WARD]). */
    internal var warded: Int = 0

    /** Пал член пака жетона: [key] - `жетон × PACK_SLOTS + член`, [vaal] - в Ваал-зоне. */
    fun slain(key: Int, vaal: Boolean) {
        (if (vaal) vaalKilled else killed) += key
    }

    /** Пал страж зоны захода. */
    fun bossSlain() {
        bosses++
    }

    /** Пересчёт [won] по павшим - заходом [run], как его катит сервер. */
    fun recount(run: Run) {
        won = run.fightsWon(killed, vaalKilled, bosses)
    }
}

/**
 * Состояние рычагов и судьбоносных строк одного боя (4.6.0): раунды боя - один бой, «в начале боя» - раз на [Battle].
 */
internal class FateFight {
    /** Попавших ударов героя за бой ([FateLever.EVERY_NTH_AILMENT], [FateLever.PHANTOM_ECHO]). */
    var hits = 0

    /** Заряд мести: приняв крит, следующий удар героя - крит ([FateLever.AVENGE_CRIT]). */
    var avenge = false

    /** До когда крит по герою бьёт обычным ударом ([FateKnob.CRIT_WARD]). */
    var critWardUntil = NEVER

    /** Враги, на которых перенесён недуг, по номеру ([FateKnob.SPREAD_MARK_DAMAGE]). */
    val marked = mutableSetOf<Int>()

    /**
     * Последнее применённое умение, умение перед ним и когда ([FateLever.ALTERNATE_SKILL]); [resonant] - умение, чьё применение
     * звучит сейчас; [chain] - сколько разных умений подряд звучит в окне резонанса ([FateKnob.RESONANT_CHAIN]).
     */
    var lastSkill: String? = null
    var beforeLast: String? = null
    var lastSkillAt = 0.0
    var resonant: String? = null
    var chain = 0

    /** Запас Зеркала урона ([FateLever.STORE_TAKEN]): уйдёт следующим попавшим ударом героя. */
    var stored = 0.0

    /** До когда урон по герою меньше после удара, срезанного потолком ([FateKnob.CAPPED_GUARD]). */
    var guardedUntil = NEVER

    /** Идущий удар по герою пришёлся на полное здоровье ([FateLever.FULL_LIFE_GUARD]); [reflected] - первый такой уже отражён. */
    var full = false
    var reflected = false

    /** Ослабленные питомцем враги: номер - до когда ([FateKnob.PET_WEAKEN]). */
    val weakened = mutableMapOf<Int, Double>()

    /** Счёт Разгона, за который в этом бою дана неуязвимость ([FateKnob.STREAK_WARD]); ноль - не дана. */
    var warded = 0
}

/**
 * Отклик Предначертания в бою (4.6.0): что рычаг дара (и судьбоносные строки, 4.6.2) делает в каждый момент боя. Момент, в который
 * откликнуться нечем, - ничего. Отклик знает свои числа сам; новый рычаг или правило строки - новая реализация и ветка [of]; бой
 * видов откликов не проверяет. Семантика - KDoc [FateLever] и [FateKnob].
 */
internal sealed interface FateReaction {
    /** Бой открылся: первый ход питомца, что несёт заход из прошлого боя. */
    fun opens(battle: Battle) = Unit

    /** Прошло [dt] секунд боя, герой стоит. */
    fun ticks(battle: Battle, dt: Double) = Unit

    /** Бой кончился: что уходит в следующий бой захода. */
    fun ends(battle: Battle) = Unit

    /** Во сколько раз удар героя [blow] по [target] тяжелее сейчас. */
    fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = 1.0

    /** Во сколько раз удар врага [me] легче сейчас. */
    fun weaker(battle: Battle, me: Fighter): Double = 1.0

    /** Сколько урона сверх своего несёт удар героя (до защит цели); тратится, лишь когда удар попал ([landed]). */
    fun extra(battle: Battle): Double = 0.0

    /** Этот удар героя - крит наверняка. */
    fun sure(battle: Battle): Boolean = false

    /** Во сколько раз больше множитель крита этого удара героя. */
    fun keener(battle: Battle): Double = 1.0

    /** Крит по герою сейчас бьёт обычным ударом. */
    fun warded(battle: Battle): Boolean = false

    /** Проверка шанса [key] героя удачна: кость бросят дважды. Сама тянет свою кость - и лишь когда может сработать. */
    fun lucky(battle: Battle, key: RollKey): Boolean = false

    /** Разброс урона этого удара героя удачен: берётся больший из двух бросков. Сама тянет свою кость. */
    fun luckySpread(battle: Battle): Boolean = false

    /** Удар героя вышел - [kind] - и забрал заряды «наверняка». */
    fun swung(battle: Battle, kind: HitKind) = Unit

    /** Герой заблокировал удар (и чары). */
    fun blocked(battle: Battle) = Unit

    /**
     * Удар героя [blow] попал в [target], отдав [taken]; [raw] (4.6.3) - тот же удар по типам до защит цели. [Battle.fateFight] уже
     * счёл его.
     */
    fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) = Unit

    /** Какую долю урона удара героя [blow] он ещё похищает общим вампиризмом героя (4.6.3), сверх своего. */
    fun leech(battle: Battle, blow: Blow): Double = 0.0

    /** Удар питомца попал в [target]. */
    fun petLanded(battle: Battle, target: Fighter) = Unit

    /** По герою попал [attacker]: [kind] - как, [amount] - сколько удар снял (барьер, щит и здоровье вместе). */
    fun struck(battle: Battle, attacker: Fighter, kind: HitKind, amount: Double) = Unit

    /** Что из [amount] одного удара [attacker] по герою доходит до барьера и щита. */
    fun capped(battle: Battle, attacker: Fighter, amount: Double): Double = amount

    /** Что из [amount] одного удара по питомцу доходит до него. */
    fun petCapped(battle: Battle, amount: Double): Double = amount

    /** Во сколько раз сильнее попавший удар героя копит оглушение. */
    fun stunning(battle: Battle): Double = 1.0

    /** Недуг [fresh] ложится на героя: каким он ляжет. */
    fun ailed(battle: Battle, fresh: ActiveAilment): ActiveAilment = fresh

    /** Враг [fallen] пал с недугами [ailing]. */
    fun fell(battle: Battle, fallen: Fighter, ailing: List<ActiveAilment>) = Unit

    /** Герой применил умение [skill] - до его перезарядки и удара. */
    fun cast(battle: Battle, skill: String) = Unit

    /** Во сколько раз короче перезарядка умения [skill], что применяют сейчас. */
    fun cooldown(battle: Battle, skill: String): Double = 1.0

    /** Мана героя перелилась через максимум на [excess]. */
    fun spilled(battle: Battle, excess: Double) = Unit

    /** Сколько маны сверх запаса героя рычаг держит для умений. */
    fun spare(battle: Battle): Double = 0.0

    /** Цена [cost] умения: рычаг платит из своего, ответ - что осталось заплатить мане героя. */
    fun pay(battle: Battle, cost: Double): Double = cost

    /** Цена умения [cost] (в мане) - здоровьем: сколько; null - платит мана. */
    fun price(battle: Battle, cost: Double): Double? = null

    /** Лечение героя перелилось через полное здоровье на [excess]. */
    fun overhealed(battle: Battle, excess: Double) = Unit

    companion object {
        /**
         * Отклики боя дара героя [kit] (`kit.fate.effects`): рычаги клиента и боевые половины рычагов обеих сторон - каждый со своими
         * судьбоносными строками; правила строк, чей рычаг в бою не откликается (или его нет), - своими откликами. Рычаг и строка
         * без отклика в бою (лист, сервер) пропущены.
         */
        fun of(kit: Loadout): List<FateReaction> {
            val fate = kit.fate.effects
            val levers = fate.effects.filter { it.lever.side != FateSide.SERVER }.mapNotNull { ofLever(it, fate, kit) }
            val knobs = FateKnob.entries.mapNotNull { k -> fate.of(k)?.let { ofKnob(k, it, kit) } }
            return levers + knobs
        }

        private fun ofLever(effect: FateEffect, fate: FateEffects, kit: Loadout): FateReaction? = when (effect.lever) {
            FateLever.MISSING_LIFE_DAMAGE -> MissingLifeDamage(effect, fate)

            FateLever.AVENGE_CRIT -> AvengeCrit(effect, fate)

            FateLever.KILL_SPREAD_AILMENTS -> KillSpread(effect, fate)

            FateLever.EVERY_NTH_AILMENT -> EveryNthAilment(effect, fate)

            FateLever.KILL_BURST -> KillBurst(effect, fate)

            FateLever.EXECUTE -> Execute(effect, fate)

            FateLever.PET_FIRST -> PetFirst(effect, fate)

            FateLever.FREEZE_TO_SLOW -> FreezeToSlow(effect, fate)

            FateLever.HIT_CAP -> HitCap(effect, fate)

            FateLever.ALTERNATE_SKILL -> AlternateSkill(effect, fate)

            FateLever.MANA_OVERFLOW -> ManaOverflow(effect, fate)

            FateLever.STORE_TAKEN -> StoreTaken(effect, fate)

            FateLever.FULL_LIFE_GUARD -> FullLifeGuard(effect, fate)

            FateLever.OVERHEAL_BARRIER -> OverhealBarrier(effect, fate)

            FateLever.LUCKY_ROLLS -> LuckyRolls(effect, fate)

            FateLever.BLOOD_PRICE -> BloodPrice(effect, fate, kit.actives.filterNotNull().associateBy { it.skill.code })

            FateLever.PHANTOM_ECHO -> PhantomEcho(effect, fate)

            FateLever.WIN_STREAK -> WinStreak(effect, fate)

            // Равновесие - лист героя, прочие - сервер
            FateLever.RESIST_BALANCE, FateLever.MAP_DROP, FateLever.MAP_TIER, FateLever.TRACKER_PACK, FateLever.TROPHIES, FateLever.ORB_KEEP,
            FateLever.AFFIX_TIER, FateLever.FIGHT_BOUNTY,
            -> null
        }

        /** Правило строки, что откликается само: его рычаг в бою молчит или его нет; прочие читает отклик их рычага. */
        private fun ofKnob(knob: FateKnob, value: FateKnobValue, kit: Loadout): FateReaction? = when (knob) {
            FateKnob.OVERCAP_DAMAGE -> OvercapDamage(value)

            FateKnob.ARMOUR_AILMENTS -> ArmourAilments(value)

            FateKnob.FULL_SOCKETS_DAMAGE -> FullSocketsDamage(value, (kit.actives.filterNotNull() + kit.passives).filter { it.filled }.mapTo(HashSet()) { it.skill.code })

            // Читает отклик рычага своего дара
            FateKnob.LOW_LIFE_SPEED, FateKnob.AVENGE_CRIT_MULTI, FateKnob.CRIT_WARD, FateKnob.AVENGE_ON_BLOCK, FateKnob.SPREAD_POWER,
            FateKnob.SPREAD_MARK_DAMAGE, FateKnob.AILMENTED_DAMAGE, FateKnob.BURST_BLEED, FateKnob.BURST_HEAL, FateKnob.EXECUTE_HEAL,
            FateKnob.PET_WEAKEN, FateKnob.PET_GUARD, FateKnob.FROZEN_DAMAGE, FateKnob.CAPPED_GUARD, FateKnob.RESONANT_COOLDOWN,
            FateKnob.RESONANT_CHAIN, FateKnob.OVERFLOW_REGEN, FateKnob.OVERFLOW_SHIELD, FateKnob.STREAK_WARD, FateKnob.STORE_KEEP,
            FateKnob.STORE_STUN, FateKnob.FULL_LIFE_MARGIN, FateKnob.FULL_LIFE_REFLECT, FateKnob.BARRIER_CARRY, FateKnob.LUCKY_DAMAGE,
            FateKnob.LUCKY_AILMENT_DEFENCE, FateKnob.BLOOD_SKILL_LEECH, FateKnob.PHANTOM_AILMENTS, FateKnob.PHANTOM_SAME_TARGET,
            -> null

            // Лист героя (обе стороны) и сервер
            FateKnob.SPELL_BLOCK_ARMOUR, FateKnob.STREAK_KEEP, FateKnob.BALANCE_CHAOS, FateKnob.MAP_TIER_CHANCE, FateKnob.MAP_INFLUENCE,
            FateKnob.MAP_EXTRA_LINE, FateKnob.TRACKER_VEILS, FateKnob.TRACKER_SECOND_PACK, FateKnob.TRACKER_LEADER_ITEM, FateKnob.TROPHY_DOUBLE,
            FateKnob.TROPHY_QUALITY, FateKnob.TROPHY_GUARDS, FateKnob.WORK_TRIPLE, FateKnob.WORK_RARE_MATERIAL, FateKnob.LONG_WORK_SPEED,
            FateKnob.WORK_FREE_CYCLE, FateKnob.KEEP_OMENS, FateKnob.KEEP_ESSENCES, FateKnob.CHAOS_KEEP_BEST, FateKnob.ORB_ROLL_QUALITY,
            FateKnob.BOUNTY_RICH, FateKnob.BOUNTY_DOUBLE,
            -> null
        }
    }
}

/** Отклик рычага: его поля [effect] уже на силе дара, судьбоносные строки дара - в [fate] ([FateEffects.of] по [FateKnob]). */
private sealed class LeverReaction(protected val effect: FateEffect, protected val fate: FateEffects) : FateReaction {
    protected val value: Double get() = effect.value
}

/** Урон героя больше за каждые `step`% недостающего здоровья, до `cap`; ниже `param`% здоровья - скорость атаки ([FateKnob.LOW_LIFE_SPEED]). */
private class MissingLifeDamage(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val speed = fate.of(FateKnob.LOW_LIFE_SPEED)

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        val hero = battle.heroFighter
        if (hero.body.maxLife <= 0) return 1.0
        val missing = (1 - hero.life / hero.body.maxLife).coerceIn(0.0, 1.0) * 100
        return 1 + min(effect.cap, floor(missing / effect.step) * value) / 100
    }

    /** Скорость атаки - благом героя, пока здоровье ниже порога: ложится и снимается на краю порога. */
    override fun ticks(battle: Battle, dt: Double) {
        val knob = speed ?: return
        val hero = battle.heroFighter
        val low = hero.life < hero.body.maxLife * knob.param / 100
        val quick = hero.effects.any { it.source == FATE_SOURCE }
        if (low && !quick) {
            battle.buff(hero, FATE_SOURCE, listOf(StatLine(CoreStat.ATTACK_SPEED.code, Op.INCREASED, knob.value)), FOREVER)
        } else if (!low && quick && hero.effects.removeAll { it.source == FATE_SOURCE }) {
            battle.remake(hero)
        }
    }
}

/**
 * Принятый крит - или удар, снявший не меньше `value`% максимума здоровья (4.6.3), - заряжает следующий попавший удар героя критом
 * (заряд один), его множитель крита - больше ([FateKnob.AVENGE_CRIT_MULTI]); крит даёт заслон от критов ([FateKnob.CRIT_WARD]),
 * блок - шанс заряда ([FateKnob.AVENGE_ON_BLOCK]).
 */
private class AvengeCrit(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun struck(battle: Battle, attacker: Fighter, kind: HitKind, amount: Double) {
        val crit = kind == HitKind.CRIT
        if (!crit && amount < battle.heroFighter.body.maxLife * value / 100) return
        val fight = battle.fateFight
        fight.avenge = true
        if (crit) fate.of(FateKnob.CRIT_WARD)?.let { fight.critWardUntil = battle.time + it.value }
    }

    override fun blocked(battle: Battle) {
        val fight = battle.fateFight
        val chance = fate.add(FateKnob.AVENGE_ON_BLOCK)
        if (!fight.avenge && chance > 0 && battle.random.nextDouble() * 100 < chance) fight.avenge = true
    }

    override fun sure(battle: Battle): Boolean = battle.fateFight.avenge

    override fun keener(battle: Battle): Double = if (battle.fateFight.avenge) 1 + fate.add(FateKnob.AVENGE_CRIT_MULTI) / 100 else 1.0

    override fun warded(battle: Battle): Boolean = battle.time < battle.fateFight.critWardUntil

    /** Заряд уходит лишь с попавшим ударом (4.6.3): уклонение и блок цели его не тратят. */
    override fun swung(battle: Battle, kind: HitKind) {
        if (!battle.fateFight.avenge || kind !in LANDED) return
        battle.fateFight.avenge = false
        if (kind == HitKind.CRIT) battle.fateNote()
    }

    private companion object {
        val LANDED = setOf(HitKind.HIT, HitKind.CRIT)
    }
}

/**
 * Недуги павшего - следующему живому врагу боя, с остатком срока и силой (сильнее - [FateKnob.SPREAD_POWER]); по нему урон героя
 * больше до конца боя ([FateKnob.SPREAD_MARK_DAMAGE]).
 */
private class KillSpread(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun fell(battle: Battle, fallen: Fighter, ailing: List<ActiveAilment>) {
        if (ailing.isEmpty()) return
        val next = battle.target() ?: battle.foeFighters.firstOrNull { it.alive } ?: return
        val power = 1 + fate.add(FateKnob.SPREAD_POWER) / 100
        ailing.filter { it.until > battle.time }.forEach { active ->
            battle.place(next, if (power == 1.0) active else active.copy(magnitude = active.magnitude * power), battle.ruleOf[active.ailment]?.first?.stacks == true)
        }
        if (fate.has(FateKnob.SPREAD_MARK_DAMAGE)) battle.fateFight.marked += next.index
        battle.fateNote()
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = if (target.side == Side.MONSTER && target.index in battle.fateFight.marked) 1 + fate.add(FateKnob.SPREAD_MARK_DAMAGE) / 100 else 1.0
}

/**
 * Каждый N-й попавший удар героя вешает случайный недуг из списка - как от доли удара его стихией, по общему правилу наложения:
 * сила недуга без урона - от доли к `step`% здоровья цели, до `cap`% силы правила ([AilmentWeight]); по врагу хотя бы с `param`
 * разными недугами урон героя больше ([FateKnob.AILMENTED_DAMAGE]).
 */
private class EveryNthAilment(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val ailed = fate.of(FateKnob.AILMENTED_DAMAGE)
    private val weight = AilmentWeight(effect.step, effect.cap)

    override fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
        if (battle.fateFight.hits % effect.every != 0 || !target.alive) return
        val ailments = effect.ailments.mapNotNull { Ailment.of(it) }
        if (ailments.isEmpty()) return
        val ailment = ailments[battle.random.nextInt(ailments.size)]
        val type = battle.ruleOf[ailment]?.second ?: return
        val share = taken.values.sum() * value / 100
        if (share > 0 && battle.afflict(battle.heroFighter, target, ailment, mapOf(type to share), weight = weight) != null) battle.fateNote()
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        val knob = ailed ?: return 1.0
        return if (target.ailments.mapTo(HashSet()) { it.ailment }.size >= knob.param) 1 + knob.value / 100 else 1.0
    }
}

/**
 * Павший взрывается: всем прочим живым врагам - доля его здоровья физическим уроном, броня режет; взрыв вешает кровотечение
 * ([FateKnob.BURST_BLEED]) и лечит героя ([FateKnob.BURST_HEAL]).
 */
private class KillBurst(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun fell(battle: Battle, fallen: Fighter, ailing: List<ActiveAilment>) {
        val amount = fallen.body.maxLife * value / 100
        val hit = battle.foeFighters.filter { it.alive && it !== fallen && !it.invulnerable }
        if (amount <= 0 || hit.isEmpty()) return
        battle.fateNote(amount)
        val bleed = fate.has(FateKnob.BURST_BLEED)
        hit.forEach { foe ->
            val dealt = battle.fateHit(foe, mapOf(DamageType.PHYSICAL to amount))
            if (bleed && foe.alive && dealt.isNotEmpty()) battle.afflict(battle.heroFighter, foe, Ailment.BLEEDING, dealt)
        }
        fate.of(FateKnob.BURST_HEAL)?.let { battle.restore(battle.heroFighter.body.maxLife * it.value / 100) }
    }
}

/** Попавший удар героя добивает врага ниже порога здоровья (у босса и стража - своего) и лечит героя ([FateKnob.EXECUTE_HEAL]). */
private class Execute(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
        if (!target.alive || target.invulnerable || target.side != Side.MONSTER) return
        val threshold = if (battle.towering(target)) effect.boss else value
        if (target.life >= target.body.maxLife * threshold / 100) return
        target.life = 0.0
        target.shield = 0.0
        battle.fateNote()
        fate.of(FateKnob.EXECUTE_HEAL)?.let { battle.restore(battle.heroFighter.body.maxLife * it.value / 100) }
    }
}

/**
 * Боевой питомец бьёт первым: его первая атака - с начала боя. Его попавший удар ослабляет врага ([FateKnob.PET_WEAKEN]), урон по
 * нему меньше ([FateKnob.PET_GUARD]).
 */
private class PetFirst(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val weaken = fate.of(FateKnob.PET_WEAKEN)

    override fun opens(battle: Battle) {
        battle.pets.forEach { it.nextAttack = 0.0 }
    }

    override fun petLanded(battle: Battle, target: Fighter) {
        val knob = weaken ?: return
        if (target.side == Side.MONSTER && target.alive) battle.fateFight.weakened[target.index] = battle.time + knob.param
    }

    override fun weaker(battle: Battle, me: Fighter): Double {
        val knob = weaken ?: return 1.0
        return if ((battle.fateFight.weakened[me.index] ?: NEVER) > battle.time) max(0.0, 1 - knob.value / 100) else 1.0
    }

    override fun petCapped(battle: Battle, amount: Double): Double = amount * max(0.0, 1 - fate.add(FateKnob.PET_GUARD) / 100)
}

/** Заморозка на героя - замедление на тот же срок; по замороженному врагу урон героя больше ([FateKnob.FROZEN_DAMAGE]). */
private class FreezeToSlow(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun ailed(battle: Battle, fresh: ActiveAilment): ActiveAilment = if (fresh.ailment != Ailment.FROZEN) fresh else fresh.copy(ailment = Ailment.CHILLED, magnitude = value)

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = if (target.frozen()) 1 + fate.add(FateKnob.FROZEN_DAMAGE) / 100 else 1.0
}

/**
 * Один удар снимает не больше доли максимума здоровья героя - у босса и стража не ниже `boss` (4.6.3); срезанный удар на `param` с
 * делает урон меньше ([FateKnob.CAPPED_GUARD]).
 */
private class HitCap(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun capped(battle: Battle, attacker: Fighter, amount: Double): Double {
        val fight = battle.fateFight
        val guarded = if (battle.time < fight.guardedUntil) amount * max(0.0, 1 - fate.add(FateKnob.CAPPED_GUARD) / 100) else amount
        val share = if (battle.towering(attacker)) max(value, effect.boss) else value
        val ceiling = battle.heroFighter.body.maxLife * share / 100
        if (guarded <= ceiling) return guarded
        fate.of(FateKnob.CAPPED_GUARD)?.let { fight.guardedUntil = battle.time + it.param }
        return ceiling
    }
}

/**
 * Резонанс: умение вслед иному в пределах `duration` - «больше» на `value`% и короче перезарядка ([FateKnob.RESONANT_COOLDOWN]); то же
 * умение подряд цепочку не продолжает; третье подряд разное - ещё «больше» ([FateKnob.RESONANT_CHAIN]).
 */
private class AlternateSkill(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun cast(battle: Battle, skill: String) {
        val fight = battle.fateFight
        val last = fight.lastSkill
        val resonant = last != null && last != skill && battle.time - fight.lastSkillAt <= effect.duration
        fight.resonant = skill.takeIf { resonant }
        fight.chain = when {
            !resonant -> 1
            skill != fight.beforeLast -> fight.chain + 1
            else -> 2
        }
        if (resonant) battle.fateNote(value)
        fight.beforeLast = last
        fight.lastSkill = skill
        fight.lastSkillAt = battle.time
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        val fight = battle.fateFight
        if (blow.skill == null || blow.skill != fight.resonant) return 1.0
        val chained = if (fight.chain >= CHAIN) 1 + fate.add(FateKnob.RESONANT_CHAIN) / 100 else 1.0
        return (1 + value / 100) * chained
    }

    override fun cooldown(battle: Battle, skill: String): Double = if (skill == battle.fateFight.resonant) max(0.0, 1 - fate.add(FateKnob.RESONANT_COOLDOWN) / 100) else 1.0

    private companion object {
        /** Какое по счёту разное умение подряд берёт прибавку цепочки - контракт [FateKnob.RESONANT_CHAIN] («третье»). */
        const val CHAIN = 3
    }
}

/**
 * Переполнение: избыток восстановления маны (не регенерации, [manaBack]) копится переливом до `pool`% максимума на весь заход и тает
 * на `decay`% в секунду (4.6.3), цена умений - сперва из него; умения «больше» на `value`% за каждые полные `step`% максимума в
 * переливе, до `cap`. Перелив отдаёт ману каждую секунду ([FateKnob.OVERFLOW_REGEN]) и принимает долю урона по герою
 * ([FateKnob.OVERFLOW_SHIELD]).
 */
private class ManaOverflow(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val regen = fate.of(FateKnob.OVERFLOW_REGEN)

    override fun spilled(battle: Battle, excess: Double) {
        val run = battle.fateRun
        run.overflow = min(battle.heroFighter.body.maxMana * effect.pool / 100, run.overflow + excess).coerceAtLeast(run.overflow)
    }

    override fun spare(battle: Battle): Double = battle.fateRun.overflow

    override fun pay(battle: Battle, cost: Double): Double {
        val run = battle.fateRun
        val paid = min(run.overflow, cost)
        run.overflow -= paid
        return cost - paid
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        val max = battle.heroFighter.body.maxMana
        if (blow.action != Action.SKILL || blow.skill == null || max <= 0 || battle.fateRun.overflow <= 0) return 1.0
        val steps = floor(battle.fateRun.overflow / max * 100 / effect.step)
        return 1 + min(effect.cap, steps * value) / 100
    }

    override fun ticks(battle: Battle, dt: Double) {
        val run = battle.fateRun
        if (run.overflow > 0) run.overflow *= max(0.0, 1 - effect.decay / 100 * dt)
        val knob = regen ?: return
        val hero = battle.heroFighter
        if (run.overflow <= 0 || battle.manaless(hero)) return
        val flow = minOf(run.overflow, hero.body.maxMana * knob.value / 100 * dt, battle.manaCap() - hero.mana)
        if (flow <= 0) return
        hero.mana += flow
        run.overflow -= flow
    }

    override fun capped(battle: Battle, attacker: Fighter, amount: Double): Double {
        val share = fate.add(FateKnob.OVERFLOW_SHIELD)
        val run = battle.fateRun
        if (share <= 0 || run.overflow <= 0) return amount
        val soaked = min(run.overflow, amount * share / 100)
        run.overflow -= soaked
        return amount - soaked
    }
}

/**
 * Зеркало урона: доля снятого с героя - в запас, запас уходит следующим попавшим ударом героя и копит им оглушение сильнее
 * ([FateKnob.STORE_STUN]); в конце боя сгорает - или переходит в следующий бой захода ([FateKnob.STORE_KEEP]).
 */
private class StoreTaken(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val keep = fate.has(FateKnob.STORE_KEEP)

    override fun opens(battle: Battle) {
        if (keep) battle.fateFight.stored = battle.fateRun.stored
    }

    override fun ends(battle: Battle) {
        if (keep) battle.fateRun.stored = battle.fateFight.stored
    }

    override fun struck(battle: Battle, attacker: Fighter, kind: HitKind, amount: Double) {
        if (amount > 0) battle.fateFight.stored += amount * value / 100
    }

    override fun extra(battle: Battle): Double = battle.fateFight.stored

    override fun stunning(battle: Battle): Double = if (battle.fateFight.stored > 0) 1 + fate.add(FateKnob.STORE_STUN) / 100 else 1.0

    override fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
        val fight = battle.fateFight
        if (fight.stored <= 0 || taken.values.sum() <= 0) return
        battle.fateNote(fight.stored)
        fight.stored = 0.0
    }
}

/**
 * Живая крепость: пока здоровье героя полное (с запасом [FateKnob.FULL_LIFE_MARGIN]), удар по нему «меньше» на `value`%; первый такой
 * удар за бой - доля снятого ударившему физическим уроном ([FateKnob.FULL_LIFE_REFLECT]).
 */
private class FullLifeGuard(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun capped(battle: Battle, attacker: Fighter, amount: Double): Double {
        val hero = battle.heroFighter
        val full = hero.life >= hero.body.maxLife * (1 - fate.add(FateKnob.FULL_LIFE_MARGIN) / 100)
        battle.fateFight.full = full
        return if (full) amount * max(0.0, 1 - value / 100) else amount
    }

    override fun struck(battle: Battle, attacker: Fighter, kind: HitKind, amount: Double) {
        val fight = battle.fateFight
        if (!fight.full) return
        fight.full = false
        val share = fate.add(FateKnob.FULL_LIFE_REFLECT)
        if (share <= 0 || fight.reflected) return
        fight.reflected = true
        val back = battle.fateHit(attacker, mapOf(DamageType.PHYSICAL to amount * share / 100))
        if (back.isNotEmpty()) battle.fateNote(back.values.sum())
    }
}

/**
 * Кровь за кровь: лечение сверх полного здоровья - барьер до `value`% максимума здоровья, до конца боя; со строкой
 * [FateKnob.BARRIER_CARRY] он переходит в следующий бой захода, теряя `param`%.
 */
private class OverhealBarrier(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val carry = fate.of(FateKnob.BARRIER_CARRY)

    private fun ceiling(battle: Battle): Double = battle.heroFighter.body.maxLife * value / 100

    override fun overhealed(battle: Battle, excess: Double) {
        val hero = battle.heroFighter
        val ceiling = ceiling(battle)
        if (hero.barrier >= ceiling) return
        hero.barrier = min(ceiling, hero.barrier + excess)
        hero.barrierUntil = FOREVER
    }

    override fun opens(battle: Battle) {
        val carried = battle.fateRun.barrier
        if (carry == null || carried <= 0) return
        val hero = battle.heroFighter
        hero.barrier = max(hero.barrier, min(carried, ceiling(battle)))
        hero.barrierUntil = FOREVER
    }

    /** Барьер дара - тот, что стоит до конца боя; барьер умения короче и не переходит. */
    override fun ends(battle: Battle) {
        val knob = carry ?: return
        val hero = battle.heroFighter
        val kept = if (hero.barrierUntil >= FOREVER) min(hero.barrier, ceiling(battle)) else 0.0
        battle.fateRun.barrier = kept * max(0.0, 1 - knob.param / 100)
    }
}

/**
 * Удача рода: проверка шанса героя с шансом `value`% удачна - своей костью боя до самой проверки; избежание недугов - лишь со строкой
 * [FateKnob.LUCKY_AILMENT_DEFENCE], разброс урона - со строкой [FateKnob.LUCKY_DAMAGE].
 */
private class LuckyRolls(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun lucky(battle: Battle, key: RollKey): Boolean = (key != RollKey.AVOID || fate.has(FateKnob.LUCKY_AILMENT_DEFENCE)) && roll(battle)

    override fun luckySpread(battle: Battle): Boolean = fate.has(FateKnob.LUCKY_DAMAGE) && roll(battle)

    private fun roll(battle: Battle): Boolean = battle.random.nextDouble() * 100 < value
}

/**
 * Обет крови: умения героя ([paid] - умения слотов по коду) стоят здоровья, не маны, - той же доли максимума, что их цена в мане от
 * максимума маны (4.6.3), × `pool`%; умения с ценой больше нуля «больше» на `value`% и похищают долю нанесённого общим
 * вампиризмом ([FateKnob.BLOOD_SKILL_LEECH]).
 */
private class BloodPrice(effect: FateEffect, fate: FateEffects, private val paid: Map<String, KitSkill>) : LeverReaction(effect, fate) {
    override fun price(battle: Battle, cost: Double): Double {
        val body = battle.heroFighter.body
        val life = if (body.maxMana > 0) cost / body.maxMana * body.maxLife else cost
        return life * effect.pool / 100
    }

    /** Удар умения, что стоит здоровья: умение слота с ценой больше нуля. */
    private fun bloody(battle: Battle, blow: Blow): Boolean = blow.skill?.let(paid::get)?.let { battle.cost(it, it.level(battle.heroFighter.body)) > 0 } == true

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = if (bloody(battle, blow)) 1 + value / 100 else 1.0

    override fun leech(battle: Battle, blow: Blow): Double = if (bloody(battle, blow)) fate.add(FateKnob.BLOOD_SKILL_LEECH) / 100 else 0.0
}

/**
 * Призрачный клинок: каждый `every`-й попавший удар героя за бой призрак повторяет по другому живому врагу (по порядку целей героя) -
 * `value`% удара до защит (4.6.3) теми же типами, защиты цели призрака режут его один раз. Без другого врага - ту же цель на `solo`%
 * (4.6.3), сильнее со строкой [FateKnob.PHANTOM_SAME_TARGET]; недуги - лишь со строкой [FateKnob.PHANTOM_AILMENTS].
 */
private class PhantomEcho(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
        if (battle.fateFight.hits % effect.every != 0) return
        val other = battle.targets(0).firstOrNull { it !== target && !it.invulnerable }
        val (aim, share) = when {
            other != null -> other to value / 100
            target.alive -> target to effect.solo / 100 * (1 + fate.add(FateKnob.PHANTOM_SAME_TARGET) / 100)
            else -> return
        }
        val dealt = battle.fateHit(aim, raw.mapValues { it.value * share })
        if (dealt.isEmpty()) return
        battle.fateNote(dealt.values.sum())
        if (fate.has(FateKnob.PHANTOM_AILMENTS) && aim.alive) battle.inflict(battle.heroFighter, aim, dealt, blow.ailments, blow.spell)
    }
}

/**
 * Разгон: урон героя «больше» за каждый выигранный в заходе бой - тем же резолвером, что количество добычи сервера; каждые `param`
 * выигранных боёв захода следующий бой начинается с неуязвимостью героя ([FateKnob.STREAK_WARD]).
 */
private class WinStreak(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = 1 + fate.streak(battle.fateRun.won) / 100

    override fun opens(battle: Battle) {
        val knob = fate.of(FateKnob.STREAK_WARD) ?: return
        val run = battle.fateRun
        val every = knob.param.toInt()
        if (every <= 0 || run.won <= 0 || run.won % every != 0 || run.warded == run.won) return
        val hero = battle.heroFighter
        hero.invulnerableUntil = max(hero.invulnerableUntil, battle.time + knob.value)
        battle.fateFight.warded = run.won
        battle.fateNote(knob.value)
    }

    /** Неуязвимость за счёт засчитана, лишь когда её бой сыгран: бой, собранный и брошенный, её не тратит. */
    override fun ends(battle: Battle) {
        battle.fateFight.warded.takeIf { it > 0 }?.let { battle.fateRun.warded = it }
    }
}

/** Отклик судьбоносной строки, что действует сама (её рычаг в бою молчит или его нет): число и постоянные - [knob]. */
private sealed class KnobReaction(protected val knob: FateKnobValue) : FateReaction

/** Равновесие ([FateKnob.OVERCAP_DAMAGE]): урон героя «больше» на `value`% за каждые `param`% стихийных сопротивлений сверх потолка, до `cap`%. */
private class OvercapDamage(knob: FateKnobValue) : KnobReaction(knob) {
    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        if (knob.param <= 0) return 1.0
        val over = DamageType.ELEMENTS.sumOf { battle.heroFighter.body.overcap(it) }
        return 1 + min(knob.cap, floor(over / knob.param) * knob.value) / 100
    }
}

/**
 * Проклятый металл ([FateKnob.ARMOUR_AILMENTS]): урон недуга по герою «меньше» на `value`% доли, что броня героя снимает с удара
 * размером во весь урон недуга.
 */
private class ArmourAilments(knob: FateKnobValue) : KnobReaction(knob) {
    override fun ailed(battle: Battle, fresh: ActiveAilment): ActiveAilment {
        if (!fresh.ailment.hurts || fresh.magnitude <= 0) return fresh
        val share = battle.heroFighter.body.physicalMitigation(fresh.magnitude * fresh.duration, battle.rules.armour.factor)
        return fresh.copy(magnitude = fresh.magnitude * max(0.0, 1 - knob.value / 100 * share))
    }
}

/** Рунный круг ([FateKnob.FULL_SOCKETS_DAMAGE]): умения [filled], у которых заняты все гнёзда рун, «больше» на `value`%. */
private class FullSocketsDamage(knob: FateKnobValue, private val filled: Set<String>) : KnobReaction(knob) {
    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = if (blow.skill in filled) 1 + knob.value / 100 else 1.0
}

/** Источник строк и эффектов Предначертания на герое. */
const val FATE_SOURCE = "FATE"

// ==================== Моменты боя ====================

/** Бой открылся: рычаги начала боя. */
internal fun Battle.fateOpens() = fated.forEach { it.opens(this) }

/** Прошёл шаг [dt]: рычаги, что живут временем, пока герой стоит. */
internal fun Battle.fateTicks(dt: Double) {
    if (fated.isNotEmpty() && heroFighter.alive) fated.forEach { it.ticks(this, dt) }
}

/** Бой кончился: что Предначертание несёт в следующий бой захода. */
internal fun Battle.fateEnds() = fated.forEach { it.ends(this) }

/** Во сколько раз удар [me] по [target] тяжелее по Предначертанию: удар героя - рычагам героя, удар врага - легче ([FateReaction.weaker]). */
internal fun Battle.fateHeavier(me: Fighter, target: Fighter, blow: Blow): Double = when {
    fated.isEmpty() -> 1.0
    me === heroFighter -> fated.fold(1.0) { k, it -> k * it.heavier(this, blow, target) }
    me.side == Side.MONSTER -> fated.fold(1.0) { k, it -> k * it.weaker(this, me) }
    else -> 1.0
}

/** Урон сверх своего у удара [me] по Предначертанию (4.6.1, до защит цели): только удар героя. */
internal fun Battle.fateExtra(me: Fighter): Double = if (me !== heroFighter || fated.isEmpty()) 0.0 else fated.sumOf { it.extra(this) }

/**
 * Кость проверки шанса [key] бойца [owner] (4.6.1, Удача рода): у героя удачная проверка - лучший из двух бросков; кость удачи
 * тянется раньше самой проверки и лишь при рычаге. Успех проверки - бросок меньше шанса.
 */
internal fun Battle.checkDraw(owner: Fighter, key: RollKey, chance: Double, ailment: Ailment? = null): Double {
    val lucky = owner === heroFighter && fated.isNotEmpty() && fated.any { it.lucky(this, key) }
    val first = draw(key, chance, ailment)
    return if (lucky) min(first, draw(key, chance, ailment)) else first
}

/** Разброс урона удара [me] удачен (4.6.2): берётся больший из двух бросков; только удар героя. */
internal fun Battle.fateLuckySpread(me: Fighter): Boolean = me === heroFighter && fated.isNotEmpty() && fated.any { it.luckySpread(this) }

/** Удар героя - крит наверняка по Предначертанию. */
internal fun Battle.fateSure(me: Fighter): Boolean = me === heroFighter && fated.any { it.sure(this) }

/** Во сколько раз больше множитель крита удара [me] по Предначертанию (4.6.2); читается до [fateSwung]. */
internal fun Battle.fateKeener(me: Fighter): Double = if (me !== heroFighter || fated.isEmpty()) 1.0 else fated.fold(1.0) { k, it -> k * it.keener(this) }

/** Крит по [target] гаснет по Предначертанию (4.6.2): по герою - пока стоит его заслон. */
internal fun Battle.fateWarded(target: Fighter): Boolean = target === heroFighter && fated.any { it.warded(this) }

/** Удар [me] вышел: у героя - заряды «наверняка» уходят. */
internal fun Battle.fateSwung(me: Fighter, kind: HitKind) {
    if (me === heroFighter) fated.forEach { it.swung(this, kind) }
}

/** [target] заблокировал удар: у героя - рычагам защиты. */
internal fun Battle.fateBlocked(target: Fighter) {
    if (target === heroFighter) fated.forEach { it.blocked(this) }
}

/**
 * Удар [me] попал в [target], отдав [taken] ([raw] - до защит цели): удар героя - рычагам героя (счёт попаданий боя - до них), удар
 * питомца и удар по герою - своим.
 */
internal fun Battle.fateLanded(me: Fighter, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
    if (fated.isEmpty()) return
    if (me === heroFighter) {
        fateFight.hits++
        fated.forEach { it.landed(this, target, taken, kind, blow, raw) }
    }
    if (isPet(me)) fated.forEach { it.petLanded(this, target) }
    if (target === heroFighter) taken.values.sum().let { amount -> fated.forEach { it.struck(this, me, kind, amount) } }
}

/** Удар [me] по [target] с потолком Предначертания: у героя и питомца - что рычаги пропустят, урон по типам - в той же доле. */
internal fun Battle.fateCapped(me: Fighter, target: Fighter, taken: Map<DamageType, Double>): Map<DamageType, Double> {
    if (fated.isEmpty()) return taken
    val total = taken.values.sum()
    val capped = when {
        target === heroFighter -> fated.fold(total) { amount, it -> it.capped(this, me, amount) }
        isPet(target) -> fated.fold(total) { amount, it -> it.petCapped(this, amount) }
        else -> return taken
    }
    if (capped >= total || total <= 0) return taken
    return taken.mapValues { it.value * capped / total }
}

/** Доля урона удара [me] ([blow]), что Предначертание похищает общим вампиризмом (4.6.3): только удар героя. */
internal fun Battle.fateLeech(me: Fighter, blow: Blow): Double = if (me !== heroFighter || fated.isEmpty()) 0.0 else fated.sumOf { it.leech(this, blow) }

/** Враг [f] - босс или страж: у них свои пороги Предначертания (добивание, потолок удара). */
internal fun Battle.towering(f: Fighter): Boolean = f.side == Side.MONSTER && foes[f.index].let { it.rarity == MonsterRarity.UNIQUE || it.guards }

/** Во сколько раз сильнее попавший удар [me] копит оглушение по Предначертанию (4.6.2): только удар героя. */
internal fun Battle.fateStunning(me: Fighter): Double = if (me !== heroFighter || fated.isEmpty()) 1.0 else fated.fold(1.0) { k, it -> k * it.stunning(this) }

/** Недуг [fresh] на [target]: на герое - каким его сделают рычаги. */
internal fun Battle.fateAiled(target: Fighter, fresh: ActiveAilment): ActiveAilment = if (target !== heroFighter || fated.isEmpty()) fresh else fated.fold(fresh) { a, it -> it.ailed(this, a) }

/** Враг пал: его недуги и взрыв. */
internal fun Battle.fateFell(fallen: Fighter, ailing: List<ActiveAilment>) = fated.forEach { it.fell(this, fallen, ailing) }

/** Герой применил умение [skill] (4.6.1). */
internal fun Battle.fateCast(skill: String) = fated.forEach { it.cast(this, skill) }

/** Во сколько раз короче перезарядка умения [skill], что применяют сейчас (4.6.2). */
internal fun Battle.fateCooldown(skill: String): Double = fated.fold(1.0) { k, it -> k * it.cooldown(this, skill) }

/** Мана сверх запаса героя, что держит Предначертание для умений (4.6.1, перелив). */
internal fun Battle.fateSpare(): Double = if (fated.isEmpty()) 0.0 else fated.sumOf { it.spare(this) }

/** Цена умения [cost] в мане - здоровьем по Предначертанию (4.6.2, Обет крови); null - платит мана. */
private fun Battle.fatePrice(cost: Double): Double? = fated.firstNotNullOfOrNull { it.price(this, cost) }

/** Умение ценой [cost] маны по карману герою: здоровьем - цена меньше текущего здоровья (4.6.2), иначе маной с переливом. */
internal fun Battle.affords(cost: Double): Boolean = fatePrice(cost)?.let { it < heroFighter.life } ?: (heroFighter.mana + fateSpare() + 1e-9 >= cost)

/** Герой платит за умение ценой [cost] маны: здоровьем (4.6.2) или маной - сперва из запаса Предначертания (4.6.1). */
internal fun Battle.payFor(cost: Double) {
    val hero = heroFighter
    val price = fatePrice(cost)
    if (price != null) {
        hero.life = max(0.0, hero.life - price)
        return
    }
    val rest = fated.fold(cost) { left, it -> if (left <= 0) 0.0 else it.pay(this, left) }
    hero.mana = max(0.0, hero.mana - rest)
}

/**
 * Мана [amount] бойцу [me] (4.6.1): все пути восстановления маны идут через неё - не выше запаса; излишек героя - Предначертанию
 * (перелив), если [spill]: регенерация и возврат цены умения (4.6.3) не переливаются.
 */
internal fun Battle.manaBack(me: Fighter, amount: Double, spill: Boolean = true) {
    val cap = manaCap(me)
    val sum = me.mana + amount
    me.mana = min(cap, sum)
    if (spill && me === heroFighter && sum > cap && fated.isNotEmpty()) fated.forEach { it.spilled(this, sum - cap) }
}

/** Лечение героя сверх полного здоровья [excess] (4.6.1, Кровь за кровь): похищение, за удар и убийство, фляги. */
internal fun Battle.fateOverheal(excess: Double) {
    if (excess > 0 && fated.isNotEmpty()) fated.forEach { it.overhealed(this, excess) }
}

/** Строка лога: Предначертание героя сработало. */
internal fun Battle.fateNote(value: Double = 0.0) = note(heroFighter, NoteKind.FATE, kit.fate.effects.fate?.code.orEmpty(), value)

/**
 * Удар Предначертания по [target] (взрыв павшего, призрак, отражение): урон [damage] по типам режут защиты цели - физический
 * броня, прочие сопротивления - и урон, что она получает; затем барьер, щит (хаос мимо) и здоровье. Ответ - что дошло, по типам;
 * пусто - ничего.
 */
internal fun Battle.fateHit(target: Fighter, damage: Map<DamageType, Double>): Map<DamageType, Double> {
    if (!target.alive || target.invulnerable) return emptyMap()
    val dealt = damage.mapValues { (type, raw) ->
        val defence = if (type == DamageType.PHYSICAL) target.body.physicalMitigation(raw, rules.armour.factor) else target.body.resist(type)
        raw * (1 - defence).coerceAtLeast(0.0) * target.body.damageTaken(type)
    }.filterValues { it > 0 }
    val total = dealt.values.sum()
    if (total <= 0) return emptyMap()
    var rest = total
    if (target.barrier > 0) {
        val soaked = min(target.barrier, rest)
        target.barrier -= soaked
        rest -= soaked
    }
    val chaos = (dealt[DamageType.CHAOS] ?: 0.0) * rest / total
    val absorbed = if (shieldless(target)) 0.0 else EnergyShield.absorbed(target.shield, rest, chaos)
    target.shield -= absorbed
    target.life = max(0.0, target.life - (rest - absorbed))
    target.lastHit = time
    if (!target.alive) fell(target, killer = heroFighter)
    return dealt
}
