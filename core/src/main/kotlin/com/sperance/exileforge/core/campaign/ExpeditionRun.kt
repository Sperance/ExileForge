package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.model.campaign.LoneWolfRule
import com.sperance.exileforge.core.model.campaign.CampaignFall
import com.sperance.exileforge.core.model.campaign.CampaignMap
import com.sperance.exileforge.core.model.campaign.CampaignRarity
import com.sperance.exileforge.core.model.campaign.CampaignReward
import com.sperance.exileforge.core.model.campaign.CombatRules
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.core.model.campaign.VaalZone
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a run stands: walking, fighting, waiting on a kill's loot, at a Vaal portal's gate (2.65.0), or over one way or another. */
enum class RunPhase { MAP, FIGHT, LOOT, GATE, DEAD, CLEARED, LEFT }

/** A number floating off a fighter, [age] seconds after the blow that made it; [foe] is the foe of the pack it was about. */
data class FloatingHit(val id: Int, val target: Side, val action: Action, val kind: HitKind, val amount: Int, val age: Double, val healed: Int,
    val type: DamageType?, val inflicted: List<Ailment>, val stunned: Boolean, val foe: Int = 0)

/** The blow on screen right now, for the cards to act out: who, at or by which foe, what, whether it landed, and how far along (0..1). */
data class LungeView(val actor: Side, val action: Action, val kind: HitKind, val landed: Boolean, val progress: Float, val foe: Int = 0)

/** An ailment on a fighter as the overlay prints it: what, how much of it is left (1 fresh, 0 gone), and how many stacks. */
data class AilmentView(val ailment: Ailment, val left: Float, val stacks: Int)

/**
 * One foe of the pack as its card prints it (2.70.0): who, its row, its pools, its swing, what is
 * on it, and whether the hero's weapon reaches it right now.
 */
data class FoeView(
    val index: Int,
    val monster: RolledMonster,
    val life: Int, val maxLife: Int, val shield: Int, val maxShield: Int,
    val swing: Float,
    val ailments: List<AilmentView>,
    val held: Boolean,
    val alive: Boolean,
    val reachable: Boolean,
    /** Taunts (2.71.0): while it stands the hero must strike it, past any row. */
    val taunt: Boolean = false,
) {
    val ranged: Boolean get() = monster.ranged
}

/**
 * The fight as the overlay prints it (2.70.0, the owner's mockup B «карточки против карточек»):
 * the pack as cards — [foes] in the order they were rolled, the overlay sorts them into rows — the
 * hero's pools, swing and states, what just landed, whom the hero strikes next ([target]) and
 * whether the player singled that one out ([focus]), and the blows so far, newest first.
 *
 * Until [started] nothing moves: it is the scouting pause, the pack laid open. [paused] is the same
 * pause asked for mid-fight.
 */
data class FightHud(
    val leader: RolledMonster,
    val foes: List<FoeView>,
    val heroLife: Int, val heroShield: Int,
    val hits: List<FloatingHit>,
    val speed: Int,
    val outcome: Outcome?,
    val heroSwing: Float = 0f,
    val heroAilments: List<AilmentView> = emptyList(),
    val heroHeld: Boolean = false,
    val retreating: Boolean = false,
    val lunge: LungeView? = null,
    val events: List<CombatEvent> = emptyList(),
    val started: Boolean = true,
    val paused: Boolean = false,
    val target: Int? = null,
    val focus: Int? = null,
    /** The hero fights alone and has the «Волк-одиночка» bonus (2.71.0), and how much of it. */
    val loneWolf: LoneWolfRule? = null,
    /** The hero taunts (2.71.0): it will matter once they have a party to cover. */
    val heroTaunt: Boolean = false,
) {
    /** Nothing is moving and the pack is laid open: before «В бой», or paused. */
    val scouting: Boolean get() = outcome == null && (!started || paused)
}

/** One member of a pack fought and its own log, kept apart so a mixed pack's log names each one right. */
data class PackHit(val monster: RolledMonster, val events: List<CombatEvent>, val duration: Double)

/**
 * A fight that is over, as the screen after it reads it: the whole log to scroll back through and
 * what it came to — dealt and taken by blows and by ailments, how long, criticals,
 * blocks, evasions, what was inflicted.
 *
 * Since 2.54.0 a jetton can be a pack of up to three, fought all at once since 2.70.0: [pack]
 * holds one [PackHit] per foe in the fight, each with its own blows, and [monster] — for the header
 * and the portrait — is the strongest of them, the one the token showed on the map. Every figure
 * below sums over the whole pack.
 */
data class FightReport(
    val monster: RolledMonster,
    val outcome: Outcome,
    val pack: List<PackHit>,
    val duration: Double,
) {
    val events: List<CombatEvent> get() = pack.flatMap { it.events }
    val packSize: Int get() = pack.size
    private fun mine() = events.filter { it.actor == Side.HERO }
    private fun theirs() = events.filter { it.actor == Side.MONSTER }
    val dealt: Int get() = mine().sumOf { it.damage }.roundToInt()
    val taken: Int get() = theirs().sumOf { it.damage }.roundToInt()
    val crits: Int get() = mine().count { it.kind == HitKind.CRIT }
    val blocked: Int get() = mine().count { it.kind == HitKind.BLOCKED }
    val evaded: Int get() = theirs().count { it.kind == HitKind.EVADED }
    val inflicted: List<Ailment> get() = mine().flatMap { it.inflicted }.distinct()
}

/** Everything the overlay draws, as one value: it changes only when something on it does. */
data class RunHud(
    val phase: RunPhase,
    val mapCode: String,
    val heroLife: Int, val heroMaxLife: Int, val heroShield: Int, val heroMaxShield: Int,
    val alive: Int, val total: Int,
    /** The exit is sealed while the map's boss lives (since 2.34.0). */
    val sealed: Boolean = false,
    val fight: FightHud? = null,
    val reward: CampaignReward? = null,
    val rewardPending: Boolean = false,
    val rewardFailed: Boolean = false,
    val slain: RolledMonster? = null,
    /** The fight just over, kept for the screen after it — a victory's loot or a defeat. */
    val report: FightReport? = null,
    /** What the death cost, once the server has said; pending until it has. */
    val fall: CampaignFall? = null,
    val fallPending: Boolean = false,
    val gold: Long = 0, val experience: Double = 0.0, val kills: Int = 0,
    /** Chests on the map not yet opened (since 2.33.0), and what the last one opened brought. */
    val chestsLeft: Int = 0,
    val chest: CampaignReward? = null,
    val chestPending: Boolean = false,
    val chestFailed: Boolean = false,
    /** Fountains on the map not yet drunk (since 2.48.0). */
    val fountainsLeft: Int = 0,
    /** The Vaal zone behind the portal the hero stands at (2.65.0), once the server has rolled it. */
    val gate: VaalZone? = null,
    val gatePending: Boolean = false,
    val gateFailed: Boolean = false,
    /** This run is a Vaal zone (2.65.0): no way out but its guardian or a death, and a death is not the map's end. */
    val vaal: Boolean = false,
)

/** What the overlay asks of the run; applied at the start of the next step. */
sealed interface RunCommand {
    data object Continue : RunCommand
    data object Speed : RunCommand
    data object Leave : RunCommand
    /** Walk out of the fight: the monster gets its free swings first; before it began, simply walk away. */
    data object Retreat : RunCommand
    /** The fight begins (since 2.48.0): until then the pack is laid open to be studied. Mid-fight it ends a [Pause]. */
    data object Begin : RunCommand
    /** Stops the fight where it stands and lays the pack open again (2.70.0); [Begin] goes on. */
    data object Pause : RunCommand
    /** Singles out foe [index] of the pack as the hero's target (2.70.0); the same one again lets the class choose. */
    data class Focus(val index: Int) : RunCommand
    data class Reward(val reward: CampaignReward) : RunCommand
    data object RewardFailed : RunCommand
    data class Fallen(val fall: CampaignFall) : RunCommand
    data object FallFailed : RunCommand
    /** The server's count of chests on this map: they are placed once it arrives. */
    data class Chests(val count: Int) : RunCommand
    data class ChestReward(val reward: CampaignReward) : RunCommand
    data object ChestFailed : RunCommand
    /** The chest's loot panel is put away. */
    data object DismissChest : RunCommand
    /** The server says the map's boss was slain within the hour: it is not there, the exit is open. */
    data object BossAbsent : RunCommand
    /** The server rolled the Vaal zone behind the portal (2.65.0), for the gate to show. */
    data class Gate(val zone: VaalZone) : RunCommand
    data object GateFailed : RunCommand
    /** Stepping back from the gate undecided: the portal stays, and opens again when walked onto. */
    data object StepBack : RunCommand
    /** The zone was entered or refused: the portal is gone and the map goes on. */
    data object ShutGate : RunCommand
    /** Back from the Vaal zone with [life] left (2.65.0). */
    data class Returned(val life: Double) : RunCommand
    /**
     * The gear changed on the map (since 2.40.0): the server's new sheet. It lands between fights —
     * one under way keeps the fighter it began with — and life keeps its share.
     */
    data class Regear(val stats: Map<String, Double>, val level: Int, val stance: HeroStance? = null) : RunCommand
}

/**
 * One run of a campaign map: the world, the hero's life across it and the fights on
 * the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the overlay reads [hud] and
 * sends [RunCommand]s, and the stick writes [stickX]/[stickY] in screen axes. Nothing here talks to
 * the server: a won fight calls [onKill], a lost one [onFallen] and the map's exit [onCleared], and
 * whoever listens reports them and hands the answer back as a command.
 *
 * The hero's life carries from fight to fight and **does not return while walking** (since
 * 2.29.0): life comes back only in a fight — by regeneration or leech — or once from each
 * fountain the map holds (since 2.48.0). The shield is whole
 * again after every fight. A lost fight ends the run and keeps everything already looted; a fight the hero walked out
 * of, or that ran out of time, leaves the monster standing and calm for a while.
 */
class ExpeditionRun(
    val map: CampaignMap,
    val world: ExpeditionWorld,
    hero: Combatant,
    val rules: CombatRules,
    private val seed: Long,
    private val onKill: (RolledMonster) -> Unit,
    private val onCleared: () -> Unit,
    private val onFallen: () -> Unit = {},
    private val onChest: () -> Unit = {},
    /** The entered map's effects, laid again over a sheet that changes on the way (since 2.40.0); a Vaal zone inherits them (2.65.1). */
    val mapEffects: Map<String, Double> = emptyMap(),
    /** The hero reached the Vaal portal (2.65.0): whoever listens asks the server for its zone. */
    private val onPortal: () -> Unit = {},
    /** The life the hero walks in with; a Vaal zone (2.65.0) is entered with what the map left, a map at full. */
    startLife: Double? = null,
    /** Whom the hero picks and how far their weapon reaches (2.70.0). */
    stance: HeroStance = HeroStance(),
) {
    /** Whom the hero picks and how far their weapon reaches; a change of weapon on the map changes it between fights. */
    var stance: HeroStance = stance
        private set
    /** The hero as the sheet has them; a change of gear on the map replaces them between fights. */
    var hero: Combatant = hero
        private set
    @Volatile var stickX = 0.0
    @Volatile var stickY = 0.0
    private val commands = ConcurrentLinkedQueue<RunCommand>()
    private var phase = RunPhase.MAP
    private var life = startLife?.coerceIn(0.0, hero.maxLife) ?: hero.maxLife
    /** The hero's life right now, between fights: what a Vaal zone is entered with. */
    val heroLife: Double get() = life
    private var gate: VaalZone? = null
    private var gatePending = false
    private var gateFailed = false
    private var started = false
    private var paused = false
    private var fights = 0
    private var speed = 1
    private var reward: CampaignReward? = null
    /** In-flight `kill` calls (since 2.54.0, one per pack member): the loot screen waits for all of them. */
    private var pendingRewards = 0
    private var rewardFailed = false
    private var slain: RolledMonster? = null
    private var report: FightReport? = null
    /** The pack members in the fight, by their place in the pack; the battle numbers them in this order. */
    private var members: List<Int> = emptyList()
    /** How many of the battle's fallen were reported already. */
    private var reported = 0
    private var fall: CampaignFall? = null
    private var fallPending = false
    private var gold = 0L
    private var experience = 0.0
    private var kills = 0
    private var chest: CampaignReward? = null
    private var chestPending = false
    private var chestFailed = false
    private var fightAgent: MonsterAgent? = null

    private var pendingGear: RunCommand.Regear? = null

    private fun regear(gear: RunCommand.Regear) {
        val stats = MapEffects.hero(gear.stats, mapEffects)
        val next = Combatant(stats, gear.level, rules)
        life = if (hero.maxLife > 0) life / hero.maxLife * next.maxLife else next.maxLife
        hero = next
        gear.stance?.let { stance = it }
        world.regear(ExpeditionWorld.heroSpeed(stats), ExpeditionWorld.lightRadius(stats, map.light))
    }

    /** The fight being played, for the scene: the battle itself, alive, with its clock and its log. */
    var fight: Battle? = null
        private set
    /** Whose fight it is on the map. */
    val fightAgentOnMap: MonsterAgent? get() = fightAgent

    private val state = MutableStateFlow(snapshot())
    val hud: StateFlow<RunHud> = state.asStateFlow()

    fun send(command: RunCommand) { commands.add(command) }

    fun update(dt: Double) {
        while (true) apply(commands.poll() ?: break)
        when (phase) {
            RunPhase.MAP -> walk(dt)
            RunPhase.FIGHT -> play(dt)
            else -> Unit
        }
        state.value = snapshot()
    }

    private fun apply(command: RunCommand) {
        when (command) {
            RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2
            RunCommand.Leave -> if (phase == RunPhase.MAP || phase == RunPhase.DEAD || phase == RunPhase.CLEARED) phase = RunPhase.LEFT
            RunCommand.Retreat -> if (fight != null && !started) walkAway() else { paused = false; fight?.retreat() }
            RunCommand.Begin -> if (fight != null) { started = true; paused = false }
            RunCommand.Pause -> if (fight != null && started && fight?.outcome == null) paused = !paused
            is RunCommand.Focus -> fight?.focus(command.index)
            RunCommand.Continue -> when (phase) {
                RunPhase.LOOT -> if (pendingRewards == 0) { phase = RunPhase.MAP; reward = null; slain = null; report = null; rewardFailed = false }
                RunPhase.DEAD -> if (!fallPending) phase = RunPhase.LEFT
                RunPhase.CLEARED -> phase = RunPhase.LEFT
                else -> Unit
            }
            // A pack (2.54.0) reports one kill per foe, so this can land more than once per
            // encounter — merged rather than replaced, gold and experience summed as always.
            is RunCommand.Reward -> {
                val incoming = command.reward
                reward = reward?.copy(
                    experience = reward!!.experience + incoming.experience, gold = reward!!.gold + incoming.gold,
                    items = mergeStacks(reward!!.items, incoming.items), equipment = reward!!.equipment + incoming.equipment,
                    level = incoming.level, totalExperience = incoming.totalExperience, money = incoming.money,
                    recipeFound = reward!!.recipeFound ?: incoming.recipeFound,
                ) ?: incoming
                pendingRewards--
                gold += incoming.gold
                experience += incoming.experience
            }
            RunCommand.RewardFailed -> { pendingRewards--; rewardFailed = true }
            is RunCommand.Fallen -> { fall = command.fall; fallPending = false }
            RunCommand.FallFailed -> fallPending = false
            is RunCommand.Chests -> world.placeChests(command.count)
            is RunCommand.Regear -> if (phase == RunPhase.FIGHT) pendingGear = command else regear(command)
            is RunCommand.ChestReward -> {
                chest = command.reward
                chestPending = false
                gold += command.reward.gold
            }
            RunCommand.ChestFailed -> { chestPending = false; chestFailed = true }
            RunCommand.DismissChest -> if (!chestPending) { chest = null; chestFailed = false }
            RunCommand.BossAbsent -> if (fightAgent !== world.boss) world.bossAbsent()
            is RunCommand.Gate -> if (phase == RunPhase.GATE) { gate = command.zone; gatePending = false; gateFailed = false }
            RunCommand.GateFailed -> { gatePending = false; gateFailed = true }
            RunCommand.StepBack -> if (phase == RunPhase.GATE) closeGate()
            RunCommand.ShutGate -> { world.closePortal(); closeGate() }
            is RunCommand.Returned -> { life = command.life.coerceIn(0.0, hero.maxLife); phase = RunPhase.MAP }
        }
    }

    private fun closeGate() {
        gate = null; gatePending = false; gateFailed = false
        if (phase == RunPhase.GATE) phase = RunPhase.MAP
    }

    /** Two orb stacks merged by item (2.54.0): a pack's rewards are summed, not replaced. */
    private fun mergeStacks(a: List<com.sperance.exileforge.core.model.hero.CharacterItem>, b: List<com.sperance.exileforge.core.model.hero.CharacterItem>) =
        (a + b).groupingBy { it.itemId }.fold(0L) { total, item -> total + item.amount }
            .map { (itemId, amount) -> com.sperance.exileforge.core.model.hero.CharacterItem(itemId, amount) }

    /** Turning away before the fight began: nothing was struck, and the monster stays calm a while. */
    private fun walkAway() {
        fightAgent?.let(world::retreatFrom)
        fight = null
        fightAgent = null
        phase = RunPhase.MAP
    }

    private fun walk(dt: Double) {
        val (x, y) = ExpeditionWorld.screenToWorld(stickX, stickY)
        when (val event = world.step(dt, x, y)) {
            is WorldEvent.Encounter -> {
                // The whole pack still standing at once (2.70.0), melee in front and ranged behind.
                val agent = event.agent
                members = agent.standing
                reported = 0
                fightAgent = agent
                fight = Battle(hero, members.map { Foe(Combatant(agent.pack[it].stats, map.level, rules), agent.pack[it].ranged) },
                    rules, life, Random(seed * 31 + fights++), stance)
                started = false
                paused = false
                phase = RunPhase.FIGHT
            }
            WorldEvent.Exit -> { phase = RunPhase.CLEARED; onCleared() }
            is WorldEvent.Opened -> { chest = null; chestFailed = false; chestPending = true; onChest() }
            is WorldEvent.Drank -> life = (life + hero.maxLife * event.fountain.heal / 100).coerceAtMost(hero.maxLife)
            WorldEvent.Portal -> { phase = RunPhase.GATE; gate = null; gateFailed = false; gatePending = true; onPortal() }
            null -> Unit
        }
    }

    private fun play(dt: Double) {
        val battle = fight ?: return
        val agent = fightAgent ?: return
        if (!started || paused) return
        battle.advance(dt * speed)
        // Every foe is a kill of its own, reported the moment it falls (2.54.0), the fight still going.
        while (reported < battle.fallen.size) {
            val member = members[battle.fallen[reported++]]
            agent.fallen += member
            kills++
            pendingRewards++
            onKill(agent.pack[member])
        }
        val outcome = battle.outcome ?: return
        if (battle.time < battle.duration + AFTERMATH) return
        life = battle.heroLife
        val pack = members.mapIndexed { index, member -> PackHit(agent.pack[member], battle.events.filter { it.foe == index }, battle.duration) }
        when (outcome) {
            Outcome.WIN -> {
                agent.alive = false
                slain = agent.monster
                report = FightReport(agent.monster, Outcome.WIN, pack, battle.duration)
                phase = RunPhase.LOOT
            }
            Outcome.LOSS -> {
                report = FightReport(agent.monster, Outcome.LOSS, pack, battle.duration)
                life = 0.0; fallPending = true; phase = RunPhase.DEAD; onFallen()
            }
            // Nothing already looted is lost — every foe that fell was reported as it fell — but
            // there is no report for a fight cut short, and the rest of the pack stays standing.
            Outcome.RETREAT -> { world.retreatFrom(agent); report = null; phase = RunPhase.MAP }
        }
        fight = null
        fightAgent = null
        // Gear changed while the fight went on lands now, on the life the fight left.
        if (phase != RunPhase.DEAD) pendingGear?.let(::regear)
        pendingGear = null
    }

    private fun snapshot(): RunHud {
        val battle = fight
        return RunHud(
            phase = phase, mapCode = map.code,
            heroLife = (battle?.heroLife ?: life).roundToInt(), heroMaxLife = hero.maxLife.roundToInt(),
            heroShield = (battle?.heroFighter?.shield ?: hero.maxShield).roundToInt(), heroMaxShield = hero.maxShield.roundToInt(),
            alive = world.alive, total = world.total, sealed = world.sealed,
            fight = battle?.let { b -> fightAgent?.let { fightHud(b, it) } },
            reward = reward, rewardPending = pendingRewards > 0, rewardFailed = rewardFailed, slain = slain, report = report,
            fall = fall, fallPending = fallPending,
            gold = gold, experience = experience, kills = kills,
            chestsLeft = world.chests.count { !it.opened }, chest = chest, chestPending = chestPending, chestFailed = chestFailed,
            fountainsLeft = world.fountains.count { !it.used },
            gate = gate, gatePending = gatePending, gateFailed = gateFailed, vaal = VaalZones.isZone(map),
        )
    }

    private fun fightHud(battle: Battle, agent: MonsterAgent): FightHud {
        val h = battle.heroFighter
        val hits = battle.events.withIndex()
            .filter { (_, event) -> event.time <= battle.time && battle.time - event.time < HIT_LIFETIME && event.action != Action.RETREAT }
            .map { (index, event) ->
                FloatingHit(index, event.target, event.action, event.kind, event.damage.roundToInt(), battle.time - event.time, event.healed.roundToInt(),
                    event.type, event.inflicted, event.stunned, event.foe)
            }
        fun ailments(f: Battle.Fighter) = f.ailments.groupBy { it.ailment }.map { (ailment, active) ->
            AilmentView(ailment, ((active.maxOf { it.until } - battle.time) / active.first().duration).toFloat().coerceIn(0f, 1f), active.size)
        }
        val foes = battle.foeFighters.map { f ->
            FoeView(f.index, agent.pack[members[f.index]], f.life.roundToInt(), f.body.maxLife.roundToInt(), f.shield.roundToInt(), f.body.maxShield.roundToInt(),
                battle.swing(f), ailments(f), f.held, f.alive, battle.reachable(f.index), f.body.taunt)
        }
        return FightHud(
            leader = agent.monster, foes = foes,
            heroLife = h.life.roundToInt(), heroShield = h.shield.roundToInt(),
            hits = hits, speed = speed,
            outcome = battle.outcome,
            heroSwing = battle.swing(h), heroAilments = ailments(h), heroHeld = h.held,
            retreating = battle.retreating,
            lunge = battle.lunge()?.let { (event, progress) -> LungeView(event.actor, event.action, event.kind, event.landed, progress.toFloat(), event.foe) },
            events = battle.events.toList().asReversed(),
            started = started, paused = paused,
            target = battle.target()?.index, focus = battle.focus,
            loneWolf = rules.loneWolf.takeIf { battle.loneWolf }, heroTaunt = hero.taunt,
        )
    }

    companion object {
        /** How long the fight's last blow hangs before the scene moves on. */
        const val AFTERMATH = 0.8
        const val HIT_LIFETIME = 1.0

        /** A run of [map]; [mapEffects] are the summed effects of the map item it was entered with (since 2.37.0), see [MapEffects]. */
        fun start(map: CampaignMap, rarities: List<CampaignRarity>, heroStats: Map<String, Double>, heroLevel: Int, seed: Long,
                  onKill: (RolledMonster) -> Unit, onCleared: () -> Unit, rules: CombatRules = CombatRules(), onFallen: () -> Unit = {},
                  onChest: () -> Unit = {}, mapEffects: Map<String, Double> = emptyMap(),
                  fountains: com.sperance.exileforge.core.model.campaign.FountainRule = com.sperance.exileforge.core.model.campaign.FountainRule(),
                  portalChance: Double = 0.0, onPortal: () -> Unit = {}, startLife: Double? = null, stance: HeroStance = HeroStance()): ExpeditionRun {
            val stats = MapEffects.hero(heroStats, mapEffects)
            val world = ExpeditionWorld.create(MapEffects.map(map, mapEffects), MapEffects.rarities(rarities, mapEffects), stats, seed, MapEffects.buffs(mapEffects), portalChance)
            world.placeFountains(fountains.count.getOrElse(0) { 0 }, fountains.count.getOrElse(1) { 0 }, fountains.heal)
            return ExpeditionRun(map, world, Combatant(stats, heroLevel, rules), rules, seed, onKill, onCleared, onFallen, onChest, mapEffects, onPortal, startLife, stance)
        }
    }
}
