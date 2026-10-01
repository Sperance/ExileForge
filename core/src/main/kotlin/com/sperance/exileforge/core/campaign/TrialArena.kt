package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.FightTally
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.RushPlan
import com.sperance.exileforge.rules.content.TowerFloor
import com.sperance.exileforge.rules.content.TowerMod
import com.sperance.exileforge.rules.content.TrialEvent
import com.sperance.exileforge.rules.content.TrialEventKind
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.rules.content.TrialRun
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a trial stands: fighting (or in the pause before the next fight), fallen, or over — finished or left. */
enum class TrialPhase { FIGHT, DEAD, DONE }

/**
 * The trial as its screen prints it: the fight for the arena's cards, which boss of how many or which floor, the level
 * the foes stand at, the seconds since the entry and the rush's limit for its bonus, the floor's lines, and what the
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
)

/**
 * One trial (3.49.0, server 1.47.0) — an arena with no map: the boss rush of a cleared region, its bosses one after
 * another at the hero's level, or the endless tower, a wave of the Abyss a floor, stronger each floor. The screen calls
 * [update] once a frame and sends [RunCommand]s, the arena's own; nothing here talks to the server: a boss down, a floor
 * cleared and the end are events for [onEvent], and the server's answers come back as [settle].
 *
 * Between two fights the next one stands laid open for [BREAK] seconds, or the player's word: a rush gives back a share
 * of life and a flask charge each; walking away there ends the trial. Mid-fight there is no way out.
 */
class TrialArena(
    val index: ContentIndex,
    val trial: TrialRun,
    context: RunContext,
    gear: HeroGear,
    private val pet: Pet?,
    private val onEvent: (TrialEvent) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val rules = index.campaign.combat
    private val trials: TrialRules = index.campaign.trials ?: TrialRules()
    private val run = Run(index, TrialRules.arena(index.campaign, trial.heroLevel), trial.seed, context)
    private val spawns = Spawns(index, run)
    private val waves = AbyssWaves(index, run)
    private val atlas = AtlasEffects.map(emptyMap(), context.atlas)
    private val plan = trial.region.takeIf { trial.kind == TrialKind.RUSH }?.let { code -> index.campaign.regions.firstOrNull { it.code == code } }?.let(::RushPlan)
    private val allies = PetAllies(index, rules)
    private val commands = ConcurrentLinkedQueue<RunCommand>()

    private var gear = gear
    private var build = HeroBuild(gear, atlas, rules)
    private val hero: Combatant get() = build.body
    private val kit: Loadout get() = gear.kit
    val stance: HeroStance get() = gear.stance
    private var pools = HeroPools(hero.maxLife, manaCap(), kit.flasks.map { it?.maxCharges ?: 0.0 }, kit.flasks.map { 0.0 }, kit.flasks.map { DraughtRate() })

    private var phase = TrialPhase.FIGHT
    /** When the trial ended, by [clock]: the clock stops there, and the ending prints a time that no longer runs. */
    private var endedAt: Long? = null
    /** The rush's boss under way (from 0), or the tower's floor. */
    private var step = if (trial.kind == TrialKind.RUSH) trial.killed else trial.floor
    private var floor: TowerFloor? = null
    /** The fights of the floor still to come, the one under way first; a rush's boss is a fight of one. */
    private var fights: List<List<RolledMonster>> = emptyList()
    private var stage = 0
    private var stages = 0
    private var level = trial.heroLevel
    private var battle: Battle? = null
    private var monsters: List<RolledMonster> = emptyList()
    private var carry: StageCarry? = null
    private var started = false
    private var paused = false
    private var interlude: Double? = null
    private var speed = 1
    private var reported = 0
    private var fought = 0L
    private var cleared = 0
    private var stageHits: List<PackHit> = emptyList()
    private var stageTime = 0.0
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

    init { stand() }

    fun send(command: RunCommand) { commands.add(command) }

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

    private fun record(kind: TrialEventKind, index: Int = 0, fallen: Boolean = false, fight: FightTally? = null): Int {
        val n = next++
        onEvent(TrialEvent(n, kind, index, fallen, fight))
        return n
    }

    private fun apply(command: RunCommand) {
        val fight = battle
        when (command) {
            RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2
            RunCommand.Begin -> if (fight != null && phase == TrialPhase.FIGHT) { started = true; paused = false; interlude = null }
            RunCommand.Pause -> if (fight != null && started && fight.outcome == null) paused = !paused
            is RunCommand.Focus -> fight?.focus(command.index)
            is RunCommand.Cast -> fight?.useSkill(command.slot)
            is RunCommand.Drink -> if (started) fight?.useFlask(command.slot)
            // Walking away is only between two fights: the trial ends there, what it brought kept.
            RunCommand.Retreat, RunCommand.Leave -> if (phase == TrialPhase.FIGHT && !started) finish(fallen = false)
            is RunCommand.Regear -> if (!started) { gear = command.gear; rebuild() }
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
                    fights = listOfNotNull(spawns.boss(zone.copy(level = level), emptyList(), emptyList())?.let(::listOf))
                    if (fights.isEmpty()) return finish(fallen = false)
                }
                TrialKind.TOWER -> {
                    val abyss = index.campaign.abyss ?: return finish(fallen = false)
                    val next = trials.tower.floor(abyss, trial.heroLevel, step)
                    if (next.mods != floor?.mods) { floor = next; rebuild() }
                    floor = next
                    level = next.level
                    fights = waves.fights(abyss, next.wave, next.level, run.streams.of("towerFloor", next.floor), effects(), run.context.extraRareMods, growth(next.power))
                    if (fights.isEmpty()) return finish(fallen = false)
                }
            }
            stages = fights.size
            stage = 0
            stageHits = emptyList()
            stageTime = 0.0
        }
        monsters = fights.first()
        fights = fights.drop(1)
        stage++
        reported = 0
        battle = battle(monsters)
        started = false
        paused = false
        interlude = BREAK
    }

    private fun battle(foes: List<RolledMonster>): Battle = Battle(hero, foes.map { monster ->
        Foe(Combatant(monster.stats, level, rules), monster.rarity, monster.skills.mapNotNull(index.skills.monsterByCode::get), monster, level)
    }, rules, pools.life, Random(Streams.mix(trial.seed, FIGHT_STREAM, fought++)), gear.stance, kit = kit, model = build, pools = pools,
        percent = gear.percent, ally = allies.of(hero.stats, pet), stage = carry)

    /** The floor's lines and the atlas over the hero and the monsters, as a map's. */
    private fun effects(): Map<String, Double> =
        MapEffects.sum(atlas, floor?.mods.orEmpty().groupBy { it.stat }.mapValues { (_, lines) -> lines.sumOf { it.value } })

    /** The hero made again on the floor's lines; life keeps its share. */
    private fun rebuild() {
        val before = hero.maxLife
        build = HeroBuild(gear, effects(), rules)
        pools = pools.copy(life = if (before > 0) pools.life / before * hero.maxLife else hero.maxLife, mana = pools.mana.coerceAtMost(manaCap()))
    }

    private fun manaCap(): Double = hero.maxMana * (1 - kit.reserved(hero) / 100)

    /** The floor's growth: so many percent more life and damage on every monster of it. */
    private fun growth(power: Double): List<MonsterEffect> =
        if (power <= 0) emptyList() else (listOf("STOCK_HEALTH") + DamageType.entries.map { it.attack }).map { MonsterEffect(it, Op.MORE, power) }

    private fun play(dt: Double) {
        val fight = battle ?: return
        if (!started) interlude?.let { left -> if (left > dt) interlude = left - dt else { interlude = null; started = true } }
        if (!started || paused) return
        fight.advance(dt * speed)
        while (reported < fight.fallen.size) { reported++; kills++ }
        val outcome = fight.outcome ?: return
        if (fight.time < fight.duration + ExpeditionRun.AFTERMATH) return
        pools = fight.pools()
        val pack = stageHits + monsters.mapIndexed { i, monster -> PackHit(monster, fight.events.filter { it.foe == i }, fight.duration) }
        stageHits = pack
        stageTime += fight.duration
        if (outcome != Outcome.WIN) {
            stats.add(pack, stageTime)
            record(TrialEventKind.FIGHT, fight = FightFigures.of(pack, stageTime, trial.kind == TrialKind.RUSH, won = false))
            battle = null
            finish(fallen = true)
            return
        }
        carry = fight.carry()
        if (fights.isEmpty()) won(pack)
        if (phase == TrialPhase.FIGHT) stand()
    }

    /** A boss or a whole floor won: its event, the rush's breath between bosses, and the next one — or the end of the rush. */
    private fun won(pack: List<PackHit>) {
        stats.add(pack, stageTime)
        record(TrialEventKind.FIGHT, fight = FightFigures.of(pack, stageTime, trial.kind == TrialKind.RUSH, won = true))
        cleared++
        when (trial.kind) {
            TrialKind.RUSH -> {
                record(TrialEventKind.BOSS, step)
                step++
                val rush = trials.rush
                pools = pools.copy(life = (pools.life + hero.maxLife * rush.life / 100).coerceAtMost(hero.maxLife),
                    charges = pools.charges.mapIndexed { i, held -> kit.flasks.getOrNull(i)?.let { (held + rush.flaskCharges).coerceAtMost(it.maxCharges) } ?: held })
                if (step >= (plan?.size ?: 0)) finish(fallen = false)
            }
            TrialKind.TOWER -> {
                val n = record(TrialEventKind.FLOOR, step)
                if (floor?.hoard == true) hoards += n
                step++
                // The tower's last floor won (3.71.0): the server closes the trial with it, so no end is sent after it
                if (step > trials.tower.maxFloor) finish(fallen = false, ended = true)
            }
        }
    }

    /** The trial over; [ended] - the server closed it already, and no end of it is recorded. */
    private fun finish(fallen: Boolean, ended: Boolean = false) {
        if (phase != TrialPhase.FIGHT) return
        if (!ended) record(TrialEventKind.END, fallen = fallen)
        endedAt = clock()
        battle = null
        phase = if (fallen) TrialPhase.DEAD else TrialPhase.DONE
    }

    private fun snapshot(): TrialHud {
        val fight = battle
        val life = (fight?.heroLife ?: pools.life).roundToInt()
        val runHud = RunHud(RunPhase.FIGHT, "", life, hero.maxLife.roundToInt(), (fight?.heroFighter?.shield ?: hero.maxShield).roundToInt(), hero.maxShield.roundToInt(),
            alive = 0, total = 0, heroMana = (fight?.heroMana ?: pools.mana).roundToInt(), heroMaxMana = manaCap().roundToInt(),
            heroReserved = (hero.maxMana - manaCap()).roundToInt(), kills = kills)
        val leader = monsters.maxByOrNull { it.rarity.ordinal }
        val fightHud = if (fight != null && leader != null) fight.hud(monsters, leader, speed, started, paused, hero.taunt, level, escape = !started,
            stage = if (trial.kind == TrialKind.RUSH) step + 1 else stage, stages = if (trial.kind == TrialKind.RUSH) plan?.size ?: 1 else stages,
            interlude = interlude) else null
        return TrialHud(
            kind = trial.kind, phase = phase, run = runHud, fight = fightHud,
            step = if (trial.kind == TrialKind.RUSH) (step + 1).coerceAtMost(plan?.size ?: 0) else step, steps = plan?.size ?: 0,
            level = level, elapsed = ((endedAt ?: clock()) - trial.startedAt) / 1000.0, limit = plan?.let { trials.rush.seconds * it.size } ?: 0.0,
            mods = floor?.mods.orEmpty(), cleared = cleared, gained = gained, awaiting = (next - answered).coerceAtLeast(0), lastHoard = lastHoard,
            summary = stats.summary(kills),
        )
    }

    companion object {
        /** Seconds the next fight stands laid open before it begins by itself. */
        const val BREAK = 3.0
        private val FIGHT_STREAM = "trial".hashCode().toLong()
    }
}
