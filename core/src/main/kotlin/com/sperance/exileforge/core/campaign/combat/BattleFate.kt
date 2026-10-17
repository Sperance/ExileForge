package com.sperance.exileforge.core.campaign.combat

import com.sperance.exileforge.core.campaign.KitSkill
import com.sperance.exileforge.core.campaign.Loadout
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
import kotlin.math.roundToInt

/**
 * Счёт Предначертания на весь заход (забег, испытание, Разлом): бои захода делят его, прогон шансов берёт свой, свежий.
 * [overflow] - перелив маны ([FateLever.MANA_OVERFLOW]) до конца захода; [won] - боёв, выигранных в заходе
 * ([FateLever.WIN_STREAK]): в забеге - павшие паки жетонов [killed] и [vaalKilled] (ключи `жетон × PACK_SLOTS + член`) и стражи,
 * счёт - правилом сервера [Run.fightsWon] ([recount]); в испытаниях и Разломе - выигранные бои захода ([victory]). Что
 * судьбоносные строки несут из боя в бой - [stored], [barrier], [warded].
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

    /** Запас Зеркала урона, с которым начнётся следующий бой захода ([FateKnob.STORE_KEEP]). */
    var stored: Double = 0.0
        internal set

    /** Барьер Крови за кровь, с которым начнётся следующий бой захода ([FateKnob.BARRIER_CARRY]). */
    var barrier: Double = 0.0
        internal set

    /** Счёт [won], за который неуязвимость Разгона уже дана ([FateKnob.STREAK_WARD]). */
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

    /** Бой испытания или узел Разлома выигран: у них нет жетонов, счёт [won] - их победы подряд в заходе. */
    fun victory() {
        won++
    }
}

/**
 * Состояние рычагов и судьбоносных строк одного боя: раунды боя - один бой, «в начале боя» - раз на [Battle].
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

    /** Счёт Разгона, за который в этом бою дана неуязвимость ([FateKnob.STREAK_WARD]); ноль - не дана. */
    var warded = 0

    /** Последняя прибавка урона Крови предков ([FateLever.MISSING_LIFE_DAMAGE]): журнал пишет её смену. */
    var rage = 0.0

    /** Удар мести вышел критом и ждёт, сколько снимет ([FateLever.AVENGE_CRIT]). */
    var avenged = false

    /**
     * Окно секунды недугов по герою ([FateLever.HIT_CAP]): с какого мига, сколько урона пропущено и сколько пришло, по какой
     * доле здоровья срезано.
     */
    var dotFrom = 0.0
    var dotTaken = 0.0
    var dotRaw = 0.0
    var dotShare = 0.0

    /** Свёртки частых срабатываний боя ([FateBeat.folds]). */
    val tallies = LinkedHashMap<FateBeat, FateTally>()
}

/**
 * Отклик Предначертания в бою: что рычаг дара (и судьбоносные строки) делает в каждый момент боя. Момент, в который
 * откликнуться нечем, - ничего. Отклик знает свои числа сам; новый рычаг или правило строки - новая реализация и ветка [of]; бой
 * видов откликов не проверяет. Семантика - KDoc [FateLever] и [FateKnob].
 */
internal sealed interface FateReaction {
    /** Бой открылся: что несёт заход из прошлого боя. */
    fun opens(battle: Battle) = Unit

    /** Прошло [dt] секунд боя, герой стоит. */
    fun ticks(battle: Battle, dt: Double) = Unit

    /** Бой кончился: что уходит в следующий бой захода. */
    fun ends(battle: Battle) = Unit

    /** Значок рычага дара [fate] у героя, пока рычаг действует; null - не действует или значка нет. */
    fun glow(battle: Battle, fate: String): FateGlow? = null

    /** Во сколько раз удар героя [blow] по [target] тяжелее сейчас. */
    fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = 1.0

    /** Сколько урона сверх своего несёт удар героя (до защит цели); тратится, лишь когда удар попал ([landed]). */
    fun extra(battle: Battle): Double = 0.0

    /** Этот удар героя - крит наверняка. */
    fun sure(battle: Battle): Boolean = false

    /** Во сколько раз больше множитель крита этого удара героя. */
    fun keener(battle: Battle): Double = 1.0

    /** Крит по герою сейчас бьёт обычным ударом; спрашивают лишь о выпавшем крите - ответ «да» гасит его. */
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
     * Удар героя [blow] попал в [target], отдав [taken]; [raw] - тот же удар по типам до защит цели. [Battle.fateFight] уже
     * счёл его.
     */
    fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) = Unit

    /** Какую долю урона удара героя [blow] он ещё похищает общим вампиризмом героя, сверх своего. */
    fun leech(battle: Battle, blow: Blow): Double = 0.0

    /** По герою попал [attacker]: [kind] - как, [amount] - сколько удар снял (барьер, щит и здоровье вместе). */
    fun struck(battle: Battle, attacker: Fighter, kind: HitKind, amount: Double) = Unit

    /** Что из [amount] одного удара [attacker] по герою доходит до барьера и щита. */
    fun capped(battle: Battle, attacker: Fighter, amount: Double): Double = amount

    /**
     * Что из [amount] урона недугов по герою за шаг доходит до барьера и щита: [source] - кто наложил недуг; null - не враг.
     */
    fun dotCapped(battle: Battle, source: Fighter?, amount: Double): Double = amount

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

    /** Цена умения [cost] (в мане) - здоровьем: сколько; null - платит мана. Только вопрос: платит [bled]. */
    fun price(battle: Battle, cost: Double): Double? = null

    /** Умение [skill] оплачено здоровьем: [life]. */
    fun bled(battle: Battle, life: Double, skill: String) = Unit

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

        /** Равновесие - лист героя, прочие - сервер. */
        private fun ofLever(effect: FateEffect, fate: FateEffects, kit: Loadout): FateReaction? = when (effect.lever) {
            FateLever.MISSING_LIFE_DAMAGE -> MissingLifeDamage(effect, fate)

            FateLever.AVENGE_CRIT -> AvengeCrit(effect, fate)

            FateLever.KILL_SPREAD_AILMENTS -> KillSpread(effect, fate)

            FateLever.EVERY_NTH_AILMENT -> EveryNthAilment(effect, fate)

            FateLever.KILL_BURST -> KillBurst(effect, fate)

            FateLever.EXECUTE -> Execute(effect, fate)

            FateLever.KILL_STACKS -> KillStacks(effect, fate)

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

            FateLever.RESIST_BALANCE, FateLever.MAP_DROP, FateLever.MAP_TIER, FateLever.TRACKER_PACK, FateLever.TROPHIES, FateLever.ORB_KEEP,
            FateLever.AFFIX_TIER, FateLever.FIGHT_BOUNTY,
            -> null
        }

        /**
         * Правило строки, что откликается само: его рычаг в бою молчит или его нет; прочие читает отклик их рычага.
         *
         * Отклик рычага своего дара; рычаги листа героя (обе стороны) и сервера - без отклика.
         */
        private fun ofKnob(knob: FateKnob, value: FateKnobValue, kit: Loadout): FateReaction? = when (knob) {
            FateKnob.OVERCAP_DAMAGE -> OvercapDamage(value)

            FateKnob.ARMOUR_AILMENTS -> ArmourAilments(value)

            FateKnob.FULL_SOCKETS_DAMAGE -> FullSocketsDamage(value, (kit.actives.filterNotNull() + kit.passives).filter { it.filled }.mapTo(HashSet()) { it.skill.code })

            FateKnob.LOW_LIFE_SPEED, FateKnob.AVENGE_CRIT_MULTI, FateKnob.CRIT_WARD, FateKnob.AVENGE_ON_BLOCK, FateKnob.SPREAD_POWER,
            FateKnob.SPREAD_MARK_DAMAGE, FateKnob.AILMENTED_DAMAGE, FateKnob.BURST_BLEED, FateKnob.BURST_HEAL, FateKnob.EXECUTE_HEAL,
            FateKnob.FROZEN_DAMAGE, FateKnob.CAPPED_GUARD, FateKnob.RESONANT_COOLDOWN,
            FateKnob.RESONANT_CHAIN, FateKnob.OVERFLOW_REGEN, FateKnob.OVERFLOW_SHIELD, FateKnob.STREAK_WARD, FateKnob.STORE_KEEP,
            FateKnob.STORE_STUN, FateKnob.FULL_LIFE_MARGIN, FateKnob.FULL_LIFE_REFLECT, FateKnob.BARRIER_CARRY, FateKnob.LUCKY_DAMAGE,
            FateKnob.LUCKY_AILMENT_DEFENCE, FateKnob.BLOOD_SKILL_LEECH, FateKnob.PHANTOM_AILMENTS, FateKnob.PHANTOM_SAME_TARGET,
            -> null

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

    /** Значок этого рычага дара [code]. */
    protected fun glowing(code: String, value: Double, count: Int = 0, left: Float = 1f) = FateGlow(code, effect.lever, value, count, left)
}

/**
 * Урон героя больше за каждые `step`% недостающего здоровья, до `cap`; ниже `param`% здоровья - скорость атаки ([FateKnob.LOW_LIFE_SPEED]).
 * Журнал - смена ступени.
 */
private class MissingLifeDamage(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val speed = fate.of(FateKnob.LOW_LIFE_SPEED)

    /** Недостающее здоровье героя, в процентах. */
    private fun missing(battle: Battle): Double {
        val hero = battle.heroFighter
        if (hero.body.maxLife <= 0) return 0.0
        return (1 - hero.life / hero.body.maxLife).coerceIn(0.0, 1.0) * 100
    }

    private fun bonus(battle: Battle): Double = min(effect.cap, floor(missing(battle) / effect.step) * value)

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        val bonus = bonus(battle)
        val fight = battle.fateFight
        if (bonus != fight.rage) {
            fight.rage = bonus
            if (bonus > 0) battle.fateBeat(FateBeat.RAGE, bonus, missing(battle))
        }
        return 1 + bonus / 100
    }

    /** Скорость атаки - благом героя, пока здоровье ниже порога: ложится и снимается на краю порога. */
    override fun ticks(battle: Battle, dt: Double) {
        val knob = speed ?: return
        val hero = battle.heroFighter
        val low = hero.life < hero.body.maxLife * knob.param / 100
        val quick = hero.effects.any { it.source == FATE_SOURCE }
        if (low && !quick) {
            battle.buff(hero, FATE_SOURCE, listOf(StatLine(CoreStat.ATTACK_SPEED.code, Op.INCREASED, knob.value)), FOREVER)
            battle.fateBeat(FateBeat.RAGE_SPEED, knob.value, knob.param)
        } else if (!low && quick && hero.effects.removeAll { it.source == FATE_SOURCE }) {
            battle.remake(hero)
        }
    }

    override fun glow(battle: Battle, fate: String): FateGlow? = bonus(battle).takeIf { it > 0 }?.let { glowing(fate, it) }
}

/**
 * Принятый крит - или удар, снявший не меньше `value`% максимума здоровья, - заряжает следующий попавший удар героя критом
 * (заряд один), его множитель крита - больше ([FateKnob.AVENGE_CRIT_MULTI]); крит даёт заслон от критов ([FateKnob.CRIT_WARD]),
 * блок - шанс заряда ([FateKnob.AVENGE_ON_BLOCK]). Заряд уходит лишь с критом: запрет крита его не тратит.
 */
private class AvengeCrit(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun struck(battle: Battle, attacker: Fighter, kind: HitKind, amount: Double) {
        val crit = kind == HitKind.CRIT
        if (!crit && amount < battle.heroFighter.body.maxLife * value / 100) return
        charge(battle)
        if (crit) fate.of(FateKnob.CRIT_WARD)?.let { battle.fateFight.critWardUntil = battle.time + it.value }
    }

    override fun blocked(battle: Battle) {
        val fight = battle.fateFight
        val chance = fate.add(FateKnob.AVENGE_ON_BLOCK)
        if (!fight.avenge && chance > 0 && battle.random.nextDouble() * 100 < chance) charge(battle)
    }

    private fun charge(battle: Battle) {
        val fight = battle.fateFight
        if (!fight.avenge) battle.fateBeat(FateBeat.AVENGE_READY)
        fight.avenge = true
    }

    override fun sure(battle: Battle): Boolean = battle.fateFight.avenge

    override fun keener(battle: Battle): Double = if (battle.fateFight.avenge) 1 + fate.add(FateKnob.AVENGE_CRIT_MULTI) / 100 else 1.0

    override fun warded(battle: Battle): Boolean {
        val left = battle.fateFight.critWardUntil - battle.time
        if (left <= 0) return false
        battle.fateBeat(FateBeat.CRIT_WARD, left)
        return true
    }

    /** Заряд уходит лишь с попавшим критом: уклонение и блок цели и запрет крита его не тратят. */
    override fun swung(battle: Battle, kind: HitKind) {
        val fight = battle.fateFight
        if (!fight.avenge || kind != HitKind.CRIT) return
        fight.avenge = false
        fight.avenged = true
    }

    override fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
        val fight = battle.fateFight
        if (!fight.avenged) return
        fight.avenged = false
        battle.fateBeat(FateBeat.AVENGE, taken.values.sum())
    }

    override fun glow(battle: Battle, fate: String): FateGlow? = if (battle.fateFight.avenge) glowing(fate, this.fate.add(FateKnob.AVENGE_CRIT_MULTI)) else null
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
        val moved = ailing.filter { it.until > battle.time }
        moved.forEach { active ->
            battle.place(next, if (power == 1.0) active else active.copy(magnitude = active.magnitude * power), battle.ruleOf[active.ailment]?.first)
        }
        if (fate.has(FateKnob.SPREAD_MARK_DAMAGE)) battle.fateFight.marked += next.index
        battle.fateBeat(FateBeat.SPREAD, moved.size.toDouble())
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

    /** Недуг без вида урона несёт доля всего удара, по его видам. */
    override fun landed(battle: Battle, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
        if (battle.fateFight.hits % effect.every != 0 || !target.alive) return
        val ailments = effect.ailments.mapNotNull { Ailment.of(it) }
        if (ailments.isEmpty()) return
        val ailment = ailments[battle.random.nextInt(ailments.size)]
        val (_, type) = battle.ruleOf[ailment] ?: return
        val share = taken.values.sum() * value / 100
        val dealt = type?.let { mapOf(it to share) } ?: taken.mapValues { it.value * value / 100 }
        if (share > 0 && battle.afflict(battle.heroFighter, target, ailment, AilingHit.of(dealt), weight = weight) != null) {
            battle.fateBeat(FateBeat.AILMENT, effect.every.toDouble(), share, ref = ailment.name)
        }
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        val knob = ailed ?: return 1.0
        return if (target.ailments.filter { it.ailment.ailing }.mapTo(HashSet()) { it.ailment }.size >= knob.param) 1 + knob.value / 100 else 1.0
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
        battle.fateBeat(FateBeat.BURST, amount, hit.size.toDouble())
        val bleed = fate.has(FateKnob.BURST_BLEED)
        hit.forEach { foe ->
            val dealt = battle.fateHit(foe, mapOf(DamageType.PHYSICAL to amount))
            if (bleed && foe.alive && dealt.isNotEmpty()) battle.afflict(battle.heroFighter, foe, Ailment.BLEEDING, AilingHit.of(dealt))
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
        battle.fateBeat(FateBeat.EXECUTE, threshold)
        fate.of(FateKnob.EXECUTE_HEAL)?.let { battle.restore(battle.heroFighter.body.maxLife * it.value / 100) }
    }
}

/**
 * Кровь вожака: убийство даёт стопку на `duration` с, до `charges` стопок; новое убийство добавляет стопку и освежает
 * срок. За стопку скорость атаки и сотворения больше на `value`%, вампиризм здоровья - на `leech`% (общий вампиризм героя, под
 * его потолком). Стопки - бафф героя [FATE_STACKS] со счётом в [TimedEffect.counter]: так они уходят на карту и в следующий бой
 * захода остатком срока ([HeroCarry]), как заряды.
 */
private class KillStacks(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private fun held(battle: Battle): TimedEffect? = battle.heroFighter.effects.firstOrNull { it.source == FATE_STACKS && it.until > battle.time }

    private fun stacks(battle: Battle): Int = held(battle)?.counter?.toInt() ?: 0

    private fun lines(count: Int): List<StatLine> = listOf(
        StatLine(CoreStat.ATTACK_SPEED.code, Op.INCREASED, value * count),
        StatLine(CoreStat.CAST_SPEED.code, Op.INCREASED, value * count),
    )

    override fun fell(battle: Battle, fallen: Fighter, ailing: List<ActiveAilment>) {
        if (!battle.heroFighter.alive) return
        val count = min(effect.charges, stacks(battle) + 1)
        battle.buff(battle.heroFighter, FATE_STACKS, lines(count), effect.duration, count.toDouble())
        battle.fateBeat(FateBeat.KILL_STACKS, count.toDouble(), value * count, effect.leech * count)
    }

    override fun leech(battle: Battle, blow: Blow): Double = stacks(battle) * effect.leech / 100

    override fun glow(battle: Battle, fate: String): FateGlow? = held(battle)?.let {
        val count = it.counter.toInt()
        glowing(fate, value * count, count, ((it.until - battle.time) / it.duration).toFloat().coerceIn(0f, 1f))
    }
}

/** Заморозка на героя - замедление на тот же срок; по замороженному врагу урон героя больше ([FateKnob.FROZEN_DAMAGE]). */
private class FreezeToSlow(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun ailed(battle: Battle, fresh: ActiveAilment): ActiveAilment {
        if (fresh.ailment != Ailment.FROZEN) return fresh
        battle.fateBeat(FateBeat.THAW, value, fresh.duration)
        return fresh.copy(ailment = Ailment.CHILLED, magnitude = value)
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = if (target.frozen()) 1 + fate.add(FateKnob.FROZEN_DAMAGE) / 100 else 1.0
}

/**
 * Один удар снимает не больше доли максимума здоровья героя - у босса и стража не ниже `boss`; срезанный удар на `param` с
 * делает урон меньше ([FateKnob.CAPPED_GUARD]). Недуги - тем же потолком на каждую секунду: урон всех недугов по герою
 * за секунду - как один удар, доля - по сильнейшему наложившему.
 */
private class HitCap(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val guard = fate.of(FateKnob.CAPPED_GUARD)

    /** Доля потолка удара [attacker], в процентах здоровья: у босса и стража - не ниже своей. */
    private fun share(battle: Battle, attacker: Fighter?): Double = if (attacker != null && battle.towering(attacker)) max(value, effect.boss) else value

    override fun capped(battle: Battle, attacker: Fighter, amount: Double): Double {
        val fight = battle.fateFight
        val guarded = if (battle.time < fight.guardedUntil) amount * max(0.0, 1 - fate.add(FateKnob.CAPPED_GUARD) / 100) else amount
        val share = share(battle, attacker)
        val ceiling = battle.heroFighter.body.maxLife * share / 100
        if (guarded <= ceiling) return guarded
        guard?.let { fight.guardedUntil = battle.time + it.param }
        battle.fateBeat(FateBeat.CAP, share, guarded, ceiling)
        return ceiling
    }

    override fun dotCapped(battle: Battle, source: Fighter?, amount: Double): Double {
        val fight = battle.fateFight
        if (fight.dotRaw <= 0) fight.dotFrom = battle.time
        fight.dotShare = max(fight.dotShare, share(battle, source))
        val room = max(0.0, battle.heroFighter.body.maxLife * fight.dotShare / 100 - fight.dotTaken)
        val passed = min(amount, room)
        fight.dotTaken += passed
        fight.dotRaw += amount
        return passed
    }

    /** Секунда недугов закрылась: срезанная - строкой журнала, окно - заново. */
    override fun ticks(battle: Battle, dt: Double) {
        val fight = battle.fateFight
        if (fight.dotRaw <= 0 || battle.time - fight.dotFrom < Battle.TICK - 1e-9) return
        if (fight.dotRaw > fight.dotTaken + 1e-9) battle.fateBeat(FateBeat.CAP_DOT, fight.dotShare, fight.dotRaw, fight.dotTaken)
        fight.dotRaw = 0.0
        fight.dotTaken = 0.0
        fight.dotShare = 0.0
    }

    override fun glow(battle: Battle, fate: String): FateGlow? {
        val knob = guard ?: return null
        val left = battle.fateFight.guardedUntil - battle.time
        return if (left > 0 && knob.param > 0) glowing(fate, knob.value, left = (left / knob.param).toFloat()) else null
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
        if (resonant) battle.fateBeat(FateBeat.RESONANCE, bonus(fight), fight.chain.toDouble(), ref = skill)
        fight.beforeLast = last
        fight.lastSkill = skill
        fight.lastSkillAt = battle.time
    }

    /** Множитель цепочки: третье подряд разное умение. */
    private fun chained(fight: FateFight): Double = if (fight.chain >= CHAIN) 1 + fate.add(FateKnob.RESONANT_CHAIN) / 100 else 1.0

    /** Прибавка урона звучащего умения, в процентах: с цепочкой. */
    private fun bonus(fight: FateFight): Double = ((1 + value / 100) * chained(fight) - 1) * 100

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        val fight = battle.fateFight
        if (blow.skill == null || blow.skill != fight.resonant) return 1.0
        return (1 + value / 100) * chained(fight)
    }

    override fun cooldown(battle: Battle, skill: String): Double = if (skill == battle.fateFight.resonant) max(0.0, 1 - fate.add(FateKnob.RESONANT_COOLDOWN) / 100) else 1.0

    override fun glow(battle: Battle, fate: String): FateGlow? {
        val fight = battle.fateFight
        val left = effect.duration - (battle.time - fight.lastSkillAt)
        if (fight.resonant == null || left <= 0 || effect.duration <= 0) return null
        return glowing(fate, bonus(fight), fight.chain, (left / effect.duration).toFloat())
    }

    private companion object {
        /** Какое по счёту разное умение подряд берёт прибавку цепочки - контракт [FateKnob.RESONANT_CHAIN] («третье»). */
        const val CHAIN = 3
    }
}

/**
 * Переполнение: избыток восстановления маны (не регенерации, [manaBack]) копится переливом до `pool`% максимума на весь заход и тает
 * на `decay`% в секунду, цена умений - сперва из него; умения «больше» на `value`% за каждые полные `step`% максимума в
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
        if (paid > 0) battle.fateBeat(FateBeat.OVERFLOW_PAY, paid)
        return cost - paid
    }

    /** Прибавка урона умений от перелива, в процентах. */
    private fun bonus(battle: Battle): Double {
        val max = battle.heroFighter.body.maxMana
        if (max <= 0 || battle.fateRun.overflow <= 0) return 0.0
        return min(effect.cap, floor(battle.fateRun.overflow / max * 100 / effect.step) * value)
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double {
        if (blow.action != Action.SKILL || blow.skill == null) return 1.0
        return 1 + bonus(battle) / 100
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
        if (soaked > 0) battle.fateBeat(FateBeat.OVERFLOW_SOAK, soaked)
        return amount - soaked
    }

    override fun glow(battle: Battle, fate: String): FateGlow? {
        val overflow = battle.fateRun.overflow
        if (overflow < 1) return null
        val pool = battle.heroFighter.body.maxMana * effect.pool / 100
        return glowing(fate, overflow, bonus(battle).roundToInt(), if (pool > 0) (overflow / pool).toFloat() else 1f)
    }
}

/**
 * Зеркало урона: доля снятого с героя - в запас, запас уходит следующим попавшим ударом героя и копит им оглушение сильнее
 * ([FateKnob.STORE_STUN]); в конце боя сгорает - или переходит в следующий бой захода ([FateKnob.STORE_KEEP]).
 */
private class StoreTaken(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private val keep = fate.has(FateKnob.STORE_KEEP)

    override fun opens(battle: Battle) {
        if (!keep) return
        battle.fateFight.stored = battle.fateRun.stored
        if (battle.fateFight.stored >= 1) battle.fateBeat(FateBeat.MIRROR_KEPT, battle.fateFight.stored)
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
        battle.fateBeat(FateBeat.MIRROR, fight.stored)
        fight.stored = 0.0
    }

    override fun glow(battle: Battle, fate: String): FateGlow? = battle.fateFight.stored.takeIf { it >= 1 }?.let { glowing(fate, it) }
}

/**
 * Живая крепость: пока здоровье героя полное (с запасом [FateKnob.FULL_LIFE_MARGIN]), удар по нему «меньше» на `value`%; первый такой
 * удар за бой - доля снятого ударившему физическим уроном ([FateKnob.FULL_LIFE_REFLECT]).
 */
private class FullLifeGuard(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    private fun full(battle: Battle): Boolean {
        val hero = battle.heroFighter
        return hero.life >= hero.body.maxLife * (1 - fate.add(FateKnob.FULL_LIFE_MARGIN) / 100)
    }

    override fun capped(battle: Battle, attacker: Fighter, amount: Double): Double {
        val full = full(battle)
        battle.fateFight.full = full
        if (!full) return amount
        val eased = amount * max(0.0, 1 - value / 100)
        if (amount > 0) battle.fateBeat(FateBeat.GUARD, value, amount, eased)
        return eased
    }

    override fun struck(battle: Battle, attacker: Fighter, kind: HitKind, amount: Double) {
        val fight = battle.fateFight
        if (!fight.full) return
        fight.full = false
        val share = fate.add(FateKnob.FULL_LIFE_REFLECT)
        if (share <= 0 || fight.reflected) return
        fight.reflected = true
        val back = battle.fateHit(attacker, mapOf(DamageType.PHYSICAL to amount * share / 100))
        if (back.isNotEmpty()) battle.fateBeat(FateBeat.REFLECT, back.values.sum())
    }

    override fun glow(battle: Battle, fate: String): FateGlow? = if (battle.heroFighter.alive && full(battle)) glowing(fate, value) else null
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
        val before = hero.barrier
        hero.barrier = min(ceiling, hero.barrier + excess)
        hero.barrierUntil = FOREVER
        battle.fateBeat(FateBeat.BARRIER, hero.barrier - before)
    }

    override fun opens(battle: Battle) {
        val carried = battle.fateRun.barrier
        if (carry == null || carried <= 0) return
        val hero = battle.heroFighter
        hero.barrier = max(hero.barrier, min(carried, ceiling(battle)))
        hero.barrierUntil = FOREVER
        battle.fateBeat(FateBeat.BARRIER_KEPT, hero.barrier)
    }

    /** Барьер дара - тот, что стоит до конца боя; барьер умения короче и не переходит. */
    override fun ends(battle: Battle) {
        val knob = carry ?: return
        val hero = battle.heroFighter
        val kept = if (hero.barrierUntil >= FOREVER) min(hero.barrier, ceiling(battle)) else 0.0
        battle.fateRun.barrier = kept * max(0.0, 1 - knob.param / 100)
    }

    override fun glow(battle: Battle, fate: String): FateGlow? {
        val hero = battle.heroFighter
        val ceiling = ceiling(battle)
        if (hero.barrierUntil < FOREVER || hero.barrier < 1 || ceiling <= 0) return null
        return glowing(fate, hero.barrier, left = (hero.barrier / ceiling).toFloat())
    }
}

/**
 * Удача рода: проверка шанса героя с шансом `value`% удачна - своей костью боя до самой проверки; избежание недугов - лишь со строкой
 * [FateKnob.LUCKY_AILMENT_DEFENCE], разброс урона - со строкой [FateKnob.LUCKY_DAMAGE].
 */
private class LuckyRolls(effect: FateEffect, fate: FateEffects) : LeverReaction(effect, fate) {
    override fun lucky(battle: Battle, key: RollKey): Boolean = (key != RollKey.AVOID || fate.has(FateKnob.LUCKY_AILMENT_DEFENCE)) && roll(battle)

    override fun luckySpread(battle: Battle): Boolean = fate.has(FateKnob.LUCKY_DAMAGE) && roll(battle)

    private fun roll(battle: Battle): Boolean = (battle.random.nextDouble() * 100 < value).also { if (it) battle.fateBeat(FateBeat.LUCKY, value) }
}

/**
 * Обет крови: умения героя ([paid] - умения слотов по коду) стоят здоровья, не маны, - той же доли максимума, что их цена в мане от
 * максимума маны, × `pool`%; умения с ценой больше нуля «больше» на `value`% и похищают долю нанесённого общим
 * вампиризмом ([FateKnob.BLOOD_SKILL_LEECH]).
 */
private class BloodPrice(effect: FateEffect, fate: FateEffects, private val paid: Map<String, KitSkill>) : LeverReaction(effect, fate) {
    override fun price(battle: Battle, cost: Double): Double {
        val body = battle.heroFighter.body
        val life = if (body.maxMana > 0) cost / body.maxMana * body.maxLife else cost
        return life * effect.pool / 100
    }

    override fun bled(battle: Battle, life: Double, skill: String) {
        if (life > 0) battle.fateBeat(FateBeat.BLOOD, life, ref = skill)
    }

    /** Удар умения, что стоит здоровья: умение слота с ценой больше нуля. */
    private fun bloody(battle: Battle, blow: Blow): Boolean = blow.skill?.let(paid::get)?.let { battle.cost(it, it.level(battle.heroFighter.body)) > 0 } == true

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = if (bloody(battle, blow)) 1 + value / 100 else 1.0

    override fun leech(battle: Battle, blow: Blow): Double = if (bloody(battle, blow)) fate.add(FateKnob.BLOOD_SKILL_LEECH) / 100 else 0.0
}

/**
 * Призрачный клинок: каждый `every`-й попавший удар героя за бой призрак повторяет по другому живому врагу (по порядку целей героя) -
 * `value`% удара до защит теми же типами, защиты цели призрака режут его один раз. Без другого врага - ту же цель на `solo`%, сильнее со строкой [FateKnob.PHANTOM_SAME_TARGET]; недуги - лишь со строкой [FateKnob.PHANTOM_AILMENTS].
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
        battle.fateBeat(FateBeat.PHANTOM, dealt.values.sum())
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
        val run = battle.fateRun
        val bonus = fate.streak(run.won)
        if (bonus > 0) battle.fateBeat(FateBeat.STREAK, (run.won + fate.carry).toDouble(), bonus)
        val knob = fate.of(FateKnob.STREAK_WARD) ?: return
        val every = knob.param.toInt()
        if (every <= 0 || run.won <= 0 || run.won % every != 0 || run.warded == run.won) return
        val hero = battle.heroFighter
        hero.invulnerableUntil = max(hero.invulnerableUntil, battle.time + knob.value)
        battle.fateFight.warded = run.won
        battle.fateBeat(FateBeat.STREAK_WARD, knob.value, run.won.toDouble())
    }

    /** Неуязвимость за счёт засчитана, лишь когда её бой сыгран: бой, собранный и брошенный, её не тратит. */
    override fun ends(battle: Battle) {
        battle.fateFight.warded.takeIf { it > 0 }?.let { battle.fateRun.warded = it }
    }

    override fun glow(battle: Battle, fate: String): FateGlow? {
        val bonus = this.fate.streak(battle.fateRun.won)
        return if (bonus > 0) glowing(fate, bonus, battle.fateRun.won + this.fate.carry) else null
    }
}

/** Отклик судьбоносной строки, что действует сама (её рычаг в бою молчит или его нет): число и постоянные - [knob]. */
private sealed class KnobReaction(protected val knob: FateKnobValue) : FateReaction

/** Равновесие ([FateKnob.OVERCAP_DAMAGE]): урон героя «больше» на `value`% за каждые `param`% стихийных сопротивлений сверх потолка, до `cap`%. */
private class OvercapDamage(knob: FateKnobValue) : KnobReaction(knob) {
    private fun bonus(battle: Battle): Double {
        if (knob.param <= 0) return 0.0
        val over = DamageType.ELEMENTS.sumOf { battle.heroFighter.body.overcap(it) }
        return min(knob.cap, floor(over / knob.param) * knob.value)
    }

    override fun opens(battle: Battle) {
        bonus(battle).takeIf { it > 0 }?.let { battle.fateBeat(FateBeat.OVERCAP, it) }
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = 1 + bonus(battle) / 100
}

/**
 * Проклятый металл ([FateKnob.ARMOUR_AILMENTS]): урон недуга по герою «меньше» на `value`% доли, что броня героя снимает с удара
 * размером во весь урон недуга.
 */
private class ArmourAilments(knob: FateKnobValue) : KnobReaction(knob) {
    override fun ailed(battle: Battle, fresh: ActiveAilment): ActiveAilment {
        if (!fresh.ailment.hurts || fresh.magnitude <= 0) return fresh
        val share = battle.heroFighter.body.physicalMitigation(fresh.magnitude * fresh.duration, battle.rules.armour.factor)
        val less = knob.value * share
        if (less > 0) battle.fateBeat(FateBeat.ARMOUR_AILMENT, less, ref = fresh.ailment.name)
        return fresh.copy(magnitude = fresh.magnitude * max(0.0, 1 - less / 100))
    }
}

/** Рунный круг ([FateKnob.FULL_SOCKETS_DAMAGE]): умения [filled], у которых заняты все гнёзда рун, «больше» на `value`%. */
private class FullSocketsDamage(knob: FateKnobValue, private val filled: Set<String>) : KnobReaction(knob) {
    override fun opens(battle: Battle) {
        if (filled.isNotEmpty()) battle.fateBeat(FateBeat.FULL_SOCKETS, knob.value)
    }

    override fun heavier(battle: Battle, blow: Blow, target: Fighter): Double = if (blow.skill in filled) 1 + knob.value / 100 else 1.0
}

/** Источник строк и эффектов Предначертания на герое. */
const val FATE_SOURCE = "FATE"

/** Источник стопок Крови вожака ([FateLever.KILL_STACKS]): бафф героя, что уходит на карту и в следующий бой. */
const val FATE_STACKS = "FATE_STACKS"

/** Эффект на герое от Предначертания: благо рычага или стопки Крови вожака - лист эффектов зовёт его даром. */
fun fateSourced(source: String): Boolean = source == FATE_SOURCE || source == FATE_STACKS

/** Бой открылся: рычаги начала боя. */
internal fun Battle.fateOpens() = fated.forEach { it.opens(this) }

/** Прошёл шаг [dt]: рычаги, что живут временем, пока герой стоит. */
internal fun Battle.fateTicks(dt: Double) {
    if (fated.isNotEmpty() && heroFighter.alive) fated.forEach { it.ticks(this, dt) }
}

/** Бой кончился: что Предначертание несёт в следующий бой захода, и свёртки частых срабатываний - в журнал. */
internal fun Battle.fateEnds() {
    fated.forEach { it.ends(this) }
    fateFolds()
}

/** Во сколько раз удар [me] по [target] тяжелее по Предначертанию: удар героя - рычагам героя. */
internal fun Battle.fateHeavier(me: Fighter, target: Fighter, blow: Blow): Double = when {
    fated.isEmpty() -> 1.0
    me === heroFighter -> fated.fold(1.0) { k, it -> k * it.heavier(this, blow, target) }
    else -> 1.0
}

/** Урон сверх своего у удара [me] по Предначертанию (до защит цели): только удар героя. */
internal fun Battle.fateExtra(me: Fighter): Double = if (me !== heroFighter || fated.isEmpty()) 0.0 else fated.sumOf { it.extra(this) }

/**
 * Кость проверки шанса [key] бойца [owner] (Удача рода): у героя удачная проверка - лучший из двух бросков; кость удачи
 * тянется раньше самой проверки и лишь при рычаге. Успех проверки - бросок меньше шанса.
 */
internal fun Battle.checkDraw(owner: Fighter, key: RollKey): Double {
    val lucky = owner === heroFighter && fated.isNotEmpty() && fated.any { it.lucky(this, key) }
    val first = draw()
    return if (lucky) min(first, draw()) else first
}

/** Разброс урона удара [me] удачен: берётся больший из двух бросков; только удар героя. */
internal fun Battle.fateLuckySpread(me: Fighter): Boolean = me === heroFighter && fated.isNotEmpty() && fated.any { it.luckySpread(this) }

/** Удар героя - крит наверняка по Предначертанию. */
internal fun Battle.fateSure(me: Fighter): Boolean = me === heroFighter && fated.any { it.sure(this) }

/** Во сколько раз больше множитель крита удара [me] по Предначертанию; читается до [fateSwung]. */
internal fun Battle.fateKeener(me: Fighter): Double = if (me !== heroFighter || fated.isEmpty()) 1.0 else fated.fold(1.0) { k, it -> k * it.keener(this) }

/** Крит по [target] гаснет по Предначертанию: по герою - пока стоит его заслон. */
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
 * по герою - своим.
 */
internal fun Battle.fateLanded(me: Fighter, target: Fighter, taken: Map<DamageType, Double>, kind: HitKind, blow: Blow, raw: Map<DamageType, Double>) {
    if (fated.isEmpty()) return
    if (me === heroFighter) {
        fateFight.hits++
        fated.forEach { it.landed(this, target, taken, kind, blow, raw) }
    }
    if (target === heroFighter) taken.values.sum().let { amount -> fated.forEach { it.struck(this, me, kind, amount) } }
}

/** Удар [me] по [target] с потолком Предначертания: у героя - что рычаги пропустят, урон по типам - в той же доле. */
internal fun Battle.fateCapped(me: Fighter, target: Fighter, taken: Map<DamageType, Double>): Map<DamageType, Double> {
    if (fated.isEmpty()) return taken
    val total = taken.values.sum()
    val capped = when {
        target === heroFighter -> fated.fold(total) { amount, it -> it.capped(this, me, amount) }
        else -> return taken
    }
    if (capped >= total || total <= 0) return taken
    return taken.mapValues { it.value * capped / total }
}

/**
 * Урон недугов [amount] по [target] за шаг: у героя - что пропустят рычаги ([FateReaction.dotCapped]); [source] - наложивший.
 */
internal fun Battle.fateDot(target: Fighter, source: Fighter?, amount: Double): Double = if (target !== heroFighter || fated.isEmpty() || amount <= 0) amount else fated.fold(amount) { left, it -> it.dotCapped(this, source, left) }

/** Доля урона удара [me] ([blow]), что Предначертание похищает общим вампиризмом: только удар героя. */
internal fun Battle.fateLeech(me: Fighter, blow: Blow): Double = if (me !== heroFighter || fated.isEmpty()) 0.0 else fated.sumOf { it.leech(this, blow) }

/** Враг [f] - босс или страж: у них свои пороги Предначертания (добивание, потолок удара). */
internal fun Battle.towering(f: Fighter): Boolean = f.side == Side.MONSTER && foes[f.index].let { it.rarity == MonsterRarity.UNIQUE || it.guards }

/** Во сколько раз сильнее попавший удар [me] копит оглушение по Предначертанию: только удар героя. */
internal fun Battle.fateStunning(me: Fighter): Double = if (me !== heroFighter || fated.isEmpty()) 1.0 else fated.fold(1.0) { k, it -> k * it.stunning(this) }

/** Недуг [fresh] на [target]: на герое - каким его сделают рычаги. */
internal fun Battle.fateAiled(target: Fighter, fresh: ActiveAilment): ActiveAilment = if (target !== heroFighter || fated.isEmpty()) fresh else fated.fold(fresh) { a, it -> it.ailed(this, a) }

/** Враг пал: его недуги и взрыв. */
internal fun Battle.fateFell(fallen: Fighter, ailing: List<ActiveAilment>) = fated.forEach { it.fell(this, fallen, ailing) }

/** Герой применил умение [skill]. */
internal fun Battle.fateCast(skill: String) = fated.forEach { it.cast(this, skill) }

/** Во сколько раз короче перезарядка умения [skill], что применяют сейчас. */
internal fun Battle.fateCooldown(skill: String): Double = fated.fold(1.0) { k, it -> k * it.cooldown(this, skill) }

/** Мана сверх запаса героя, что держит Предначертание для умений (перелив). */
internal fun Battle.fateSpare(): Double = if (fated.isEmpty()) 0.0 else fated.sumOf { it.spare(this) }

/** Цена умения [cost] в мане - здоровьем по Предначертанию (Обет крови); null - платит мана. */
private fun Battle.fatePrice(cost: Double): Double? = fated.firstNotNullOfOrNull { it.price(this, cost) }

/** Умение ценой [cost] маны по карману герою: здоровьем - цена меньше текущего здоровья, иначе маной с переливом. */
internal fun Battle.affords(cost: Double): Boolean = fatePrice(cost)?.let { it < heroFighter.life } ?: (heroFighter.mana + fateSpare() + 1e-9 >= cost)

/** Герой платит за умение [skill] ценой [cost] маны: здоровьем или маной - сперва из запаса Предначертания. */
internal fun Battle.payFor(cost: Double, skill: String) {
    val hero = heroFighter
    val price = fatePrice(cost)
    if (price != null) {
        hero.life = max(0.0, hero.life - price)
        fated.forEach { it.bled(this, price, skill) }
        return
    }
    val rest = fated.fold(cost) { left, it -> if (left <= 0) 0.0 else it.pay(this, left) }
    hero.mana = max(0.0, hero.mana - rest)
}

/**
 * Мана [amount] бойцу [me]: все пути восстановления маны идут через неё - не выше запаса; излишек героя - Предначертанию
 * (перелив), если [spill]: регенерация и возврат цены умения не переливаются.
 */
internal fun Battle.manaBack(me: Fighter, amount: Double, spill: Boolean = true) {
    val cap = manaCap(me)
    val sum = me.mana + amount
    me.mana = min(cap, sum)
    if (spill && me === heroFighter && sum > cap && fated.isNotEmpty()) fated.forEach { it.spilled(this, sum - cap) }
}

/** Лечение героя сверх полного здоровья [excess] (Кровь за кровь): похищение, за удар и убийство, фляги. */
internal fun Battle.fateOverheal(excess: Double) {
    if (excess > 0 && fated.isNotEmpty()) fated.forEach { it.overhealed(this, excess) }
}

/**
 * Удар Предначертания по [target] (взрыв павшего, призрак, отражение): урон [damage] по типам режут защиты цели - физический
 * броня, прочие сопротивления - урон, что она получает, и печати и законы Разлома; затем барьер, щит ([EnergyShield]) и здоровье. Ответ - что дошло, по типам;
 * пусто - ничего.
 *
 * Печати и законы Разлома - как у удара и отражения.
 */
internal fun Battle.fateHit(target: Fighter, damage: Map<DamageType, Double>): Map<DamageType, Double> {
    if (!target.alive || target.invulnerable) return emptyMap()
    val dealt = damage.mapValues { (type, raw) ->
        val defence = if (type == DamageType.PHYSICAL) target.body.physicalMitigation(raw, rules.armour.factor) else target.body.resist(type)
        (raw * (1 - defence).coerceAtLeast(0.0) * target.body.damageTaken(type) * sealed(target) * lawTaken(target, type)).takeIf { it.isFinite() } ?: 0.0
    }.filterValues { it > 0 }
    val total = dealt.values.sum()
    if (total <= 0) return emptyMap()
    var rest = total
    if (target.barrier > 0) {
        val soaked = min(target.barrier, rest)
        target.barrier -= soaked
        rest -= soaked
    }
    val absorbed = if (shieldless(target)) 0.0 else EnergyShield.absorbed(target.shield, dealt.mapValues { it.value * rest / total })
    target.shield -= absorbed
    target.life = max(0.0, target.life - (rest - absorbed))
    target.lastHit = time
    if (!target.alive) fell(target, killer = heroFighter)
    return dealt
}
