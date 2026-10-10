package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.surrender
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.OddsPlan
import com.sperance.exileforge.core.campaign.run.PackHit
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.campaign.run.RunPhase
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.RushPlan
import com.sperance.exileforge.rules.content.TowerFloor
import com.sperance.exileforge.rules.content.TowerMod
import com.sperance.exileforge.rules.content.TrialEvent
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.rules.content.TrialRun
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt
import kotlin.random.Random

/** Where a trial stands: fighting (or in the pause before the next fight), fallen, or over — finished or left. */
enum class TrialPhase { FIGHT, DEAD, DONE }

/**
 * The trial as its screen prints it: the fight for the arena's cards, which boss of how many or which floor, the level
 * the foes stand at, the seconds of fighting so far (4.2.0) and the rush's limit for its bonus, the floor's lines, and what the
 * server's answers brought — the tower's hoards as they come, the rush's chest at the end.
 */
data class TrialHud(
    val kind: TrialKind,
    val phase: TrialPhase,
    val run: RunHud,
    val fight: FightHud?,
    val step: Int,
    val steps: Int,
    val level: Int,
    val elapsed: Double,
    val limit: Double,
    val mods: List<TowerMod> = emptyList(),
    val cleared: Int = 0,
    val gained: Reward = Reward.NONE,
    val awaiting: Int = 0,
    val lastHoard: Reward? = null,
    val summary: RunSummary = RunSummary(),
    /** Варианты строки десятка башни, пока герой выбирает (3.96.0); null - выбора нет. */
    val choice: List<TowerMod>? = null,
    /** Ступень раша (3.96.0), с 0. */
    val tier: Int = 0,
)

/**
 * One trial (3.49.0, server 1.47.0) — an arena with no map: the boss rush of a cleared region, its bosses one after
 * another at the hero's level, or the endless tower, its floor a wave of its own cut into fights, a level higher each floor. The screen calls
 * [update] once a frame and sends [RunCommand]s, the arena's own; nothing here talks to the server: a boss down, a floor
 * cleared and the end are events for [onEvent], and the server's answers come back as [settle].
 *
 * Between two fights the next one stands laid open until the player's «В бой»: a rush gives back a share of life and a flask
 * charge each; walking away there ends the trial. Mid-fight there is no way out.
 */
class TrialArena(
    val index: ContentIndex,
    val trial: TrialRun,
    context: RunContext,
    gear: HeroGear,
    private val pet: Pet?,
    /** Строки башни, выбранные героем по десяткам (3.96.0). */
    picks: List<Int>,
    private val onEvent: (TrialEvent) -> Unit,
) {
    val rules = index.campaign.combat
    private val trials: TrialRules = index.campaign.trials ?: TrialRules()
    private val run = Run(index, TrialRules.arena(index.campaign, trial.heroLevel), trial.seed, context)
    private val spawns = Spawns(index, run)
    private val waves = AbyssWaves(index, run)
    private val atlas = AtlasEffects.map(emptyMap(), context.atlas)
    private val plan = trial.region.takeIf { trial.kind == TrialKind.RUSH }?.let { code -> index.campaign.regions.firstOrNull { it.code == code } }
        ?.let { RushPlan(it, trial.seed, trials.rush.bosses) }
    private val tier = trials.rush.tier(trial.tier)
    private val picks = picks.toMutableList()
    private var choice: List<TowerMod>? = null
    private val allies = PetAllies(index, rules)

    /** Счёт Предначертания испытания (4.6.0): перелив маны живёт до конца испытания. */
    private val fateRun = com.sperance.exileforge.core.campaign.combat.FateRun()
    private val commands = ConcurrentLinkedQueue<RunCommand>()

    private val gear = gear
    private var build = HeroBuild(gear, atlas, rules)
    private val hero: Combatant get() = build.body
    private val kit: Loadout get() = gear.kit
    val stance: HeroStance get() = gear.stance
    private var pools = HeroPools(hero.maxLife, manaCap(), kit.flasks.map { it?.sheet?.maxCharges ?: 0.0 }, kit.flasks.map { 0.0 }, kit.flasks.map { DraughtRate() })

    private var phase = TrialPhase.FIGHT

    /**
     * Время боёв испытания (4.2.0), секунд: часы испытания и лимит раша идут только в боях, как их считает сервер
     * ([TrialRun.fought]); вход в начатое испытание продолжает с его счёта.
     */
    private var fightSeconds = trial.fought / 1000.0

    /** Прошлый бой испытания выигран (4.2.0): следующий открывается силами `FIGHT_CLEAR`. */
    private var wonLast = false

    /** The rush's boss under way (from 0), or the tower's floor. */
    private var step = if (trial.kind == TrialKind.RUSH) trial.killed else trial.floor
    private var floor: TowerFloor? = null

    /** The fights of the floor still to come, the one under way first; a rush's boss is a fight of one. */
    private var fights: List<List<RolledMonster>> = emptyList()
    private var round = 0
    private var rounds = 0
    private var level = trial.heroLevel
    private var battle: Battle? = null
    private var monsters: List<RolledMonster> = emptyList()
    private var started = false
    private var paused = false
    private var speed = 1
    private var reported = 0
    private var fought = 0L
    private var cleared = 0

    /** Бои шага (босса раша или этажа башни) до сих пор: шаг - одно событие боя с общим журналом и временем. */
    private var stepHits: List<PackHit> = emptyList()
    private var stepTime = 0.0
    private val stats = RunStats()
    private var kills = 0

    /** The journal: the next number, how far the server answered, what it granted by number. */
    private var next = trial.applied
    private var answered = trial.applied
    private val earned = HashMap<Int, Reward>()
    private var gained = Reward.NONE
    private var lastHoard: Reward? = null
    private val hoards = HashSet<Int>()

    private val state = MutableStateFlow(snapshot())
    val hud: StateFlow<TrialHud> = state.asStateFlow()

    init {
        stand()
    }

    fun send(command: RunCommand) {
        commands.add(command)
    }

    fun update(dt: Double) {
        while (true) apply(commands.poll() ?: break)
        if (phase == TrialPhase.FIGHT) play(dt)
        state.value = snapshot()
    }

    /** The server answered the journal up to [applied]: what each event brought, by its number. */
    fun settle(applied: Int, rewards: Map<Int, Reward>) {
        answered = maxOf(answered, applied)
        rewards.forEach { (n, reward) ->
            if (n in earned) return@forEach
            earned[n] = reward
            gained += reward
            if (n in hoards) lastHoard = reward
        }
    }

    /** Records the event [event] builds for the next number and returns that number. */
    private fun record(event: (n: Int) -> TrialEvent): Int {
        val n = next++
        onEvent(event(n))
        return n
    }

    private fun apply(command: RunCommand) {
        val fight = battle
        when (command) {
            RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2

            RunCommand.Begin -> if (fight != null && phase == TrialPhase.FIGHT) {
                started = true
                paused = false
            }

            RunCommand.Pause -> if (fight != null && started && fight.outcome == null) paused = !paused

            RunCommand.Surrender -> if (fight != null && fight.outcome == null) {
                fight.surrender()
                paused = false
            }

            is RunCommand.Focus -> fight?.focus(command.index)

            is RunCommand.Cast -> fight?.useSkill(command.slot)

            is RunCommand.Drink -> if (started) fight?.useFlask(command.slot)

            is RunCommand.PickLine -> choice?.takeIf { command.option in it.indices }?.let {
                val decade = picks.size + 1
                record { n -> TrialEvent.Pick(n, decade, command.option) }
                picks += command.option
                choice = null
                stand()
            }

            // Walking away is only between two fights: the trial ends there, what it brought kept.
            RunCommand.Leave -> if (phase == TrialPhase.FIGHT && !started) finish(fallen = false)

            else -> Unit
        }
    }

    /** The next fight stands up, laid open: the rush's next boss, or the floor's next wave — a new floor first when the last is done. */
    private fun stand() {
        if (fights.isEmpty()) {
            when (trial.kind) {
                TrialKind.RUSH -> {
                    val zone = plan?.zones?.getOrNull(step) ?: return finish(fallen = false)
                    level = trial.heroLevel
                    // Ступень раша (3.96.0): боссы сильнее и с лишними строками
                    fights = listOfNotNull(spawns.boss(zone.copy(level = level), growth(tier.power), emptyList(), tier.mods)?.let(::listOf))
                    if (fights.isEmpty()) return finish(fallen = false)
                }

                TrialKind.TOWER -> {
                    val abyss = index.campaign.abyss ?: return finish(fallen = false)
                    // Новый десяток (3.96.0): сначала герой выбирает его строку угрозы
                    if (!trials.tower.ready(step, picks)) {
                        choice = trials.tower.options(picks.size + 1)
                        battle = null
                        return
                    }
                    val next = trials.tower.floor(abyss, trial.heroLevel, step, picks)
                    if (next.mods != floor?.mods) {
                        floor = next
                        rebuild()
                    }
                    floor = next
                    level = next.level
                    fights = waves.fights(abyss, next.wave, next.level, run.streams.of("towerFloor", next.floor), effects(), run.context.extraRareMods, growth(next.power))
                    if (fights.isEmpty()) return finish(fallen = false)
                }
            }
            rounds = fights.size
            round = 0
            stepHits = emptyList()
            stepTime = 0.0
        }
        monsters = fights.first()
        fights = fights.drop(1)
        round++
        reported = 0
        battle = battle(monsters)
        started = false
        paused = false
    }

    private fun battle(foes: List<RolledMonster>): Battle {
        val phases = PhaseFoes(index, rules)
        val stream = fought++
        return Battle(
            hero,
            // Фазы и свита боссов (3.92.0): свита - на своём потоке
            phases.withRetinue(foes.map { phases.foe(it, level) }, Dice(Streams.mix(trial.seed, RETINUE_STREAM, stream))),
            rules, index.rules.fight, pools.life, Random(Streams.mix(trial.seed, FIGHT_STREAM, stream)), gear.stance, kit = kit, model = build, pools = pools,
            percent = gear.percent, ally = allies.of(hero.stats, pet), cleared = wonLast, kind = trial.kind.fight,
            fateRun = fateRun,
        )
    }

    /**
     * Снимок боя с боссом шага для прогона (3.92.0; 4.2.0 - снимок): герой, каков он сейчас, против стаи боя; null - босса в стае
     * нет. Снимать в потоке арены; прогон [OddsPlan.run] - вне главного потока.
     */
    fun oddsPlan(): OddsPlan? {
        if (monsters.none { it.rarity == MonsterRarity.UNIQUE }) return null
        val phases = PhaseFoes(index, rules)
        return OddsPlan(hero, monsters.map { phases.foe(it, level) }, rules, index.rules.fight, pools, gear.stance, kit, build, gear.percent, allies.of(hero.stats, pet), phases, wonLast, trial.kind.fight)
    }

    /** The floor's lines and the atlas over the hero and the monsters, as a map's. */
    private fun effects(): Map<String, Double> = MapEffects.sum(atlas, floor?.mods.orEmpty().groupBy { it.stat }.mapValues { (_, lines) -> lines.sumOf { it.value } })

    /** The hero made again on the floor's lines; life keeps its share. */
    private fun rebuild() {
        val before = hero.maxLife
        build = HeroBuild(gear, effects(), rules)
        pools = pools.copy(life = if (before > 0) pools.life / before * hero.maxLife else hero.maxLife, mana = pools.mana.coerceAtMost(manaCap()))
    }

    private fun manaCap(): Double = hero.maxMana * (1 - kit.reserved(hero) / 100)

    /** The floor's growth: so many percent more life and damage on every monster of it. */
    private fun growth(power: Double): List<MonsterEffect> = if (power <= 0) emptyList() else (listOf(CoreStat.HEALTH.code) + DamageType.entries.map { it.attack }).map { MonsterEffect(it, Op.MORE, power) }

    private fun play(dt: Double) {
        val fight = battle ?: return
        if (!started || paused) return
        fight.advance(dt * speed)
        while (reported < fight.fallen.size) {
            reported++
            kills++
        }
        val outcome = fight.outcome ?: return
        if (fight.time < fight.duration + index.campaign.expedition.aftermath) return
        pools = fight.pools()
        val retinue = fight.foes.withIndex().drop(monsters.size).mapNotNull { (i, foe) -> foe.origin?.let { PackHit(it, fight.events.filter { e -> e.foe == i }, fight.duration, stepTime) } }
        val pack = stepHits + monsters.mapIndexed { i, monster -> PackHit(monster, fight.events.filter { it.foe == i }, fight.duration, stepTime) } + retinue
        stepHits = pack
        stepTime += fight.duration
        fightSeconds += fight.duration
        wonLast = outcome == Outcome.WIN
        if (outcome != Outcome.WIN) {
            stats.add(pack, stepTime)
            record { TrialEvent.Fight(it, FightFigures.of(pack, stepTime, trial.kind == TrialKind.RUSH, won = false)) }
            battle = null
            finish(fallen = true)
            return
        }
        if (fights.isEmpty()) won(pack)
        if (phase == TrialPhase.FIGHT) stand()
    }

    /** A boss or a whole floor won: its event, the rush's breath between bosses, and the next one — or the end of the rush. */
    private fun won(pack: List<PackHit>) {
        stats.add(pack, stepTime)
        record { TrialEvent.Fight(it, FightFigures.of(pack, stepTime, trial.kind == TrialKind.RUSH, won = true)) }
        cleared++
        when (trial.kind) {
            TrialKind.RUSH -> {
                record { TrialEvent.Boss(it, step) }
                step++
                breathe(trials.rush.life, trials.rush.flaskCharges)
                if (step >= (plan?.size ?: 0)) finish(fallen = false)
            }

            TrialKind.TOWER -> {
                val n = record { TrialEvent.Floor(it, step) }
                if (floor?.hoard == true) hoards += n
                step++
                // Передышка между этажами (4.2.0), как между боссами раша
                breathe(trials.tower.life, trials.tower.flaskCharges)
                // The tower's last floor won (3.71.0): the server closes the trial with it, so no end is sent after it
                if (step > trials.tower.maxFloor) finish(fallen = false, ended = true)
            }
        }
    }

    /** Передышка после босса раша или этажа башни: [life]% максимума здоровья (не выше него) и [charges] зарядов каждой фляге. */
    private fun breathe(life: Double, charges: Double) {
        pools = pools.copy(
            life = (pools.life + hero.maxLife * life / 100).coerceAtMost(hero.maxLife),
            charges = pools.charges.mapIndexed { i, held -> kit.flasks.getOrNull(i)?.let { (held + charges).coerceAtMost(it.sheet.maxCharges) } ?: held },
        )
    }

    /** The trial over; [ended] - the server closed it already, and no end of it is recorded. */
    private fun finish(fallen: Boolean, ended: Boolean = false) {
        if (phase != TrialPhase.FIGHT) return
        if (!ended) record { TrialEvent.End(it, fallen) }
        battle = null
        phase = if (fallen) TrialPhase.DEAD else TrialPhase.DONE
    }

    private fun snapshot(): TrialHud {
        val fight = battle
        val life = (fight?.heroLife ?: pools.life).roundToInt()
        val runHud = RunHud(
            RunPhase.FIGHT, MapCode.NONE, life, hero.maxLife.roundToInt(), (fight?.heroFighter?.shield ?: hero.maxShield).roundToInt(), hero.maxShield.roundToInt(),
            alive = 0, total = 0, heroMana = (fight?.heroMana ?: pools.mana).roundToInt(), heroMaxMana = manaCap().roundToInt(),
            heroReserved = (hero.maxMana - manaCap()).roundToInt(), kills = kills,
        )
        val leader = monsters.maxByOrNull { it.rarity.ordinal }
        val fightHud = if (fight != null && leader != null) {
            fight.hud(
                monsters, leader, speed, started, paused, hero.taunt, level,
                round = if (trial.kind == TrialKind.RUSH) step + 1 else round, rounds = if (trial.kind == TrialKind.RUSH) plan?.size ?: 1 else rounds,
            )
        } else {
            null
        }
        return TrialHud(
            kind = trial.kind, phase = phase, run = runHud, fight = fightHud,
            step = if (trial.kind == TrialKind.RUSH) (step + 1).coerceAtMost(plan?.size ?: 0) else step, steps = plan?.size ?: 0,
            level = level, elapsed = fightSeconds + (fight?.takeIf { started }?.let { it.outcome?.let { _ -> it.duration } ?: it.time } ?: 0.0), limit = plan?.let { trials.rush.seconds * it.size } ?: 0.0,
            mods = floor?.mods.orEmpty(), cleared = cleared, gained = gained, awaiting = (next - answered).coerceAtLeast(0), lastHoard = lastHoard,
            summary = stats.summary(kills),
            choice = choice,
            tier = trial.tier,
        )
    }

    companion object {
        private val FIGHT_STREAM = "trial".hashCode().toLong()

        /** Поток свиты фаз боссов (3.92.0). */
        private val RETINUE_STREAM = "trialRetinue".hashCode().toLong()
    }
}
