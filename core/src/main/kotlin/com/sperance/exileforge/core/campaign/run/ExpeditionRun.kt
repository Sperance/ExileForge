package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.AbyssWaves
import com.sperance.exileforge.core.campaign.DeathHit
import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.MapStats
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.PetAllies
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.RunStats
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.StageCarry
import com.sperance.exileforge.core.campaign.VaalZones
import com.sperance.exileforge.core.campaign.ZoneShare
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Ally
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.FlaskView
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.SkillView
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.campaign.hud
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.FeatureUse
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * One run of a zone: the world, the hero's life across it and the fights on the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the overlay reads [hud] and sends
 * [RunCommand]s. Nothing here talks to the server: every kill, chest, boss and descent is an event of the
 * [journal]. Its reward is the server's alone (1.30.0): it comes back with the answer as [RunCommand.Settled],
 * and until then the screens say it is on its way — as do the Vaal zone behind a portal and a crystal's Vaal
 * orb, read from the campaign the answer brings ([RunCommand.Campaign]). The hero's life carries from fight to
 * fight and does not return while walking; mana comes back on the road, flasks fill with kills.
 */
class ExpeditionRun(
    val index: ContentIndex,
    /** The zone walked: the location, or its Vaal zone. */
    val zone: Zone,
    run: Run,
    val journal: RunJournal,
    val world: ExpeditionWorld,
    build: HeroBuild,
    val rules: CombatRules,
    internal val seed: Long,
    /** The map's summed effects, the atlas's share in them; a Vaal zone adds its own lines. Без сделок алтаря (3.90.0): с ними встали стаи на входе. */
    val baseEffects: Map<String, Double>,
    /** This run is the Vaal zone behind the portal. */
    val vaal: Boolean,
    startPools: HeroPools?,
    internal val heroExperience: Double,
    internal val heroLevel: Int,
    /** Vaal orbs at hand, the ones already spent on this run's journal counted out. */
    internal val vaalOrbs: () -> Long,
    internal var corruptionOpened: Boolean,
    internal var vaalZone: VaalZone?,
    internal var bossDown: Boolean,
    internal val onRecorded: (RunEvent) -> Unit,
    internal val onCleared: () -> Unit,
    internal val onFallen: () -> Unit,
    /** The autorun that drives this run instead of the stick (3.2.0); null walks by hand. */
    internal var autopilot: AutoPilot? = null,
    /** The combat pet at work (3.5.0), read again as each fight begins (3.70.0): one put to work mid-run joins the next. */
    internal val pet: () -> Pet? = { null },
) {
    /** The run as the rules roll it; it takes the Vaal zone's context once the portal opens. */
    var run: Run = run
        internal set

    /** Сделки алтарей захода (3.90.0, сервер 1.81.3), сложенные по характеристике: герой и бои несут их до конца карты. */
    var pacts: Map<String, Double> = emptyMap()
        internal set

    /** The map's summed effects with the altars' bargains (3.90.0): the hero's sheet and the map's window read these. */
    var mapEffects: Map<String, Double> = baseEffects
        internal set
    internal val spawns get() = Spawns(index, run)
    var build: HeroBuild = build
        internal set
    val stance: HeroStance get() = build.gear.stance
    var hero: Combatant = build.body
        internal set

    @Volatile var stickX = 0.0

    @Volatile var stickY = 0.0
    internal val commands = ConcurrentLinkedQueue<RunCommand>()
    internal var phase = RunPhase.MAP

    /** Страж на экране-вызове (3.92.0): мир ждёт «В бой»; null - вызова нет. */
    internal var challenge: MonsterAgent? = null
    internal var life = startPools?.life?.coerceIn(0.0, hero.maxLife) ?: hero.maxLife
    val heroLife: Double get() = life
    internal val kit: Loadout get() = build.gear.kit

    /** Темп похода из контента (3.80.31): паузы боя, шаг автозабега. */
    internal val pace: ExpeditionRules get() = index.campaign.expedition
    internal var mana = startPools?.mana?.coerceIn(0.0, manaCap()) ?: manaCap()
    internal var charges: List<Double> = kit.flasks.mapIndexed { i, flask -> flask?.let { startPools?.charges?.getOrNull(i)?.coerceIn(0.0, it.maxCharges) ?: it.maxCharges } ?: 0.0 }
    internal var flaskLeft: List<Double> = kit.flasks.indices.map { startPools?.flaskLeft?.getOrNull(it) ?: 0.0 }
    internal var rates: List<DraughtRate> = kit.flasks.indices.map { startPools?.rates?.getOrNull(it) ?: DraughtRate() }
    val pools: HeroPools get() = HeroPools(life, mana, charges, flaskLeft, rates)
    internal var gate: VaalZone? = null
    internal var crystal: CrystalSpot? = null

    /** The fountain offered (3.70.0), until it is drunk or turned down. */
    internal var fountain: Fountain? = null

    /** Объект карты, чей лист открыт (3.90.0): забег ждёт ответа игрока, как у фонтана. */
    internal var offer: FeatureSpot? = null

    /** Сбор узла ремесла (3.90.0): какой узел и сколько секунд уже прошло; забег стоит, пока сбор идёт. */
    internal var channel: Channel? = null

    /** Горение и яд ловушек на герое (3.90.0): тикают на карте, могут убить. */
    internal val burns = mutableListOf<Burn>()

    /** Последний удар ловушки на экране (3.90.0) и сколько секунд он ещё висит. */
    internal var hazard: HazardView? = null
    internal var hazardLeft = 0.0

    /** Выборы объектов карты по номеру события (3.90.0): отклонённый сервером объект становится прежним. */
    internal val featureEvents = HashMap<Int, Pair<FeatureSpot, Int>>()
    internal var crystalOutcome: String? = null
    internal var rift: AbyssSpot? = null
    internal var descent: Descent? = null
    internal var abyssFight = false
    internal var fightLevel = 0
    internal var started = false
    internal var paused = false
    internal var holds = 0
    internal var fights = 0
    internal var speed = 1

    /** How far the server has answered the journal, what it granted by event number, and what it refused (server 1.30.0). */
    internal var answered = 0
    internal val earned = HashMap<Int, Reward>()
    internal val refused = HashSet<Int>()

    /** The rewarding events this run recorded, and what the answers brought for them all told. */
    internal val mine = HashSet<Int>()
    internal var granted = Reward.NONE

    /**
     * Добыча начатых боёв (3.88.0, server 1.80.0): бой - по номеру его ENGAGE, добыча боя по членам, когда сервер её ответил,
     * убийство - по своему номеру, и убийства, чья добыча уже показана до ответа на них самих.
     */
    internal val engaged = HashMap<Int, FightKey>()
    internal val pendingLoot = HashMap<FightKey, Map<Int, Reward>>()
    internal val killsOf = HashMap<Int, Pair<FightKey, Int>>()
    internal val previewed = HashSet<Int>()

    /** The events of the fight on the report, and what they brought so far; null before the fight's first kill. */
    internal val fightEvents = mutableListOf<Int>()
    internal var reward: Reward? = null

    /** Every bonus of the hero's atlas on this run (3.81.0), for the map's window: the ones the fight wears and the ones the rolls do. */
    var atlas: Map<String, Double> = emptyMap()
        internal set

    /** The fight's guardian fell to this hero for the first time (3.81.0): their place among all who beat it. */
    internal var rank: Long? = null

    /** The last level the level-up screen told of (3.81.0); the hero's level at the run's start until then. */
    internal var levelShown: Int = heroLevel

    /**
     * Счёт боевых заданий по ходу захода (3.95.0): что записанные события прибавят счётчикам летописи, когда сервер их примет, -
     * для листа «Задания» на карте. Сам заход уже сосчитан входом.
     */
    internal val questTally = hashMapOf(com.sperance.exileforge.rules.content.Counter.RUNS to 1L)

    /** What an autorun has gathered, fight by fight: its report at the end. */
    internal val autoEvents = HashSet<Int>()
    internal var autoReward: Reward? = null

    /** The chest on screen, by its event. */
    internal var chestEvent: Int? = null

    /** The hero's campaign as the last answer brought it. */
    internal var campaign: CampaignState? = null

    /** The portal's opening, until the server tells the Vaal zone behind it. */
    internal var gateEvent: Int? = null

    /** Vaal orbs on crystals, by event, until the server tells what they did. */
    internal val vaalings = HashMap<Int, Vaaling>()

    /** The crystals the server's answers said Vaal orbs made, by event, until their orbs are resolved. */
    internal val vaaled = HashMap<Int, Crystal>()
    internal var fallEvent: Int? = null

    /** The pet as a fighter (3.5.0), made again only when the hero's sheet that reaches it changes. */
    internal val allies = PetAllies(index, rules)

    /** The pet of the fight under way, taken as it began: a change mid-fight waits for the next. */
    internal var fightPet: Pet? = null
    internal fun ally(): Ally? = allies.of(hero.stats, fightPet)

    /**
     * The hero's degeneration on the road (3.4.0): the fight burns it in its own beat, the walk did not.
     * Off a fight it wounds to the last point: only a fight ends a run.
     */
    internal fun wound(dt: Double) {
        val share = hero.lifeDegenShare
        if (share > 0 && life > 1) life = (life - hero.maxLife * share * dt).coerceAtLeast(1.0)
    }
    internal var slain: RolledMonster? = null
    internal var report: FightReport? = null

    /** The foes of the stage in the battle's order, each with the pack it walked with. */
    internal var members: List<FightMember> = emptyList()

    /** Every pack the fight drew in: the engaged one first, then the rest by their distance to it. */
    internal var fightAgents: List<MonsterAgent> = emptyList()

    /** [fightAgents] as the stages they are fought in: small packs in a row merge into one (3.70.0). */
    internal var fightStages: List<List<MonsterAgent>> = emptyList()

    /** The stage under way, from 1 (3.28.0): the packs of [fightStages] it fights. */
    internal var stage = 0

    /** Seconds of the pause before a later stage begins on its own; null outside that pause. */
    internal var interlude: Double? = null

    /** What the stages already won leave to the one report: every foe's log, and the seconds they took. */
    internal var stageHits: List<PackHit> = emptyList()
    internal var stageTime = 0.0

    /**
     * What the stage won last hands the one under way (3.32.0): its STAGE_CLEAR powers and the momentum; null for a first stage.
     * Held by the journal (3.32.1), so a run entered again after a restart mid-fight hands it to its next fight.
     */
    internal var stageCarry: StageCarry?
        get() = journal.carry
        set(value) {
            journal.carry = value
        }

    /** The strongest of every stage: it stands for the whole fight in the report. */
    internal var fightStrongest: RolledMonster? = null

    /** The dice stream of the stage's battle: a draught in the pause builds the battle again on the same dice. */
    internal var fightStream = 0L
    internal var reported = 0
    internal var fall: Double? = null
    internal var kills = 0

    /** The map's summary (see [MapTally]): how it ended, the seconds on it, the guardians slain and the deaths. */
    internal var end: MapEnd? = null
    internal var seconds = 0.0
    internal var bosses = 0
    internal var deaths = 0

    /** The run's figures (3.47.0) and, after a fall, its last blows. */
    internal val stats = RunStats()
    internal var recap: List<DeathHit> = emptyList()
    internal var fightAgent: MonsterAgent? = null
    internal val waves get() = AbyssWaves(index, run)
    internal val abyssRule get() = index.campaign.abyss

    internal fun manaCap(): Double = hero.maxMana * (1 - kit.reserved(hero) / 100)

    internal fun regear(gear: HeroGear) {
        val next = HeroBuild(gear, mapEffects, rules)
        val before = hero
        build = next
        rebody()
        life = if (before.maxLife > 0) life / before.maxLife * hero.maxLife else hero.maxLife
        charges = next.gear.kit.flasks.mapIndexed { i, flask -> flask?.let { (charges.getOrNull(i) ?: 0.0).coerceIn(0.0, it.maxCharges) } ?: 0.0 }
        flaskLeft = next.gear.kit.flasks.indices.map { i -> if (next.gear.kit.flasks[i] != null) flaskLeft.getOrNull(i) ?: 0.0 else 0.0 }
        rates = next.gear.kit.flasks.indices.map { i -> if (next.gear.kit.flasks[i] != null) rates.getOrNull(i) ?: DraughtRate() else DraughtRate() }
        rebody()
    }

    /** The hero between fights made again: the sheet with the draughts still running, and the pace and sight they give. */
    internal fun rebody() {
        val lines = kit.flasks.withIndex().filter { (i, flask) -> flask != null && (flaskLeft.getOrNull(i) ?: 0.0) > 0 }
            .flatMap { (_, flask) -> flask!!.draught(build.body, life, 0.0).lines }
        hero = if (lines.isEmpty()) build.body else build.body(lines)
        mana = mana.coerceIn(0.0, manaCap())
        world.regear(ExpeditionWorld.heroSpeed(pace, hero.stats), ExpeditionWorld.lightRadius(pace, hero.stats, zone.light))
    }

    var fight: Battle? = null
        internal set

    internal val state = MutableStateFlow(snapshot())
    val hud: StateFlow<RunHud> = state.asStateFlow()

    fun send(command: RunCommand) {
        commands.add(command)
    }

    fun update(dt: Double) {
        while (true) handle(commands.poll() ?: break)
        if (phase != RunPhase.DEAD && phase != RunPhase.CLEARED && phase != RunPhase.LEFT) seconds += dt
        hazardLeft = (hazardLeft - dt).coerceAtLeast(0.0)
        if (hazardLeft <= 0) hazard = null
        if (holds == 0) {
            when (phase) {
                // A fountain offered holds the walk until the player answers; так же лист объекта карты и сбор узла (3.90.0).
                RunPhase.MAP -> autopilot?.let { drive(it, dt) } ?: run {
                    channel?.let { gather(it, dt) }
                    if (fountain == null && offer == null && channel == null) walk(dt)
                }

                RunPhase.FIGHT -> play(dt)

                else -> Unit
            }
        }
        state.value = snapshot()
    }

    /** One event of the journal, and the listener told. */
    internal fun record(event: (n: Int) -> RunEvent): RunEvent? = journal.record(event)?.also(onRecorded)?.also(::counted)

    private fun counted(event: RunEvent) {
        fun add(counter: String) = questTally.merge(counter, 1L, Long::plus)
        when (event) {
            is RunEvent.Kill -> {
                add(com.sperance.exileforge.rules.content.Counter.KILLS)
                when (run.spawn(event.i, event.vaal).pack.getOrNull(event.m)?.rarity) {
                    com.sperance.exileforge.rules.content.MonsterRarity.MAGIC -> add(com.sperance.exileforge.rules.content.Counter.KILLS_MAGIC)
                    com.sperance.exileforge.rules.content.MonsterRarity.RARE -> add(com.sperance.exileforge.rules.content.Counter.KILLS_RARE)
                    else -> Unit
                }
            }

            is RunEvent.Chest -> add(com.sperance.exileforge.rules.content.Counter.CHESTS)

            is RunEvent.Boss -> add(com.sperance.exileforge.rules.content.Counter.BOSSES)

            is RunEvent.Crystal -> add(com.sperance.exileforge.rules.content.Counter.CRYSTALS)

            else -> Unit
        }
    }

    /** A rewarding event recorded: what it brings comes with the server's answer, into the run's count, the autorun's and, [fought], the fight's report. */
    internal fun rewarding(event: RunEvent?, fought: Boolean = false): RunEvent? = event?.also {
        mine += it.n
        if (autopilot != null) {
            autoEvents += it.n
            autoReward = autoReward ?: Reward.NONE
        }
        if (fought) {
            fightEvents += it.n
            reward = reward ?: Reward.NONE
        }
    }

    internal fun clearSpoils() {
        reward = null
        rank = null
        fightEvents.clear()
    }

    /** The server's answer: every reward of this run's events lands where it was waited for. */

    /** What this run hands the map it was entered from, a Vaal zone over: its rewarding events and its own counts. */
    fun share(): ZoneShare = ZoneShare(mine.associateWith { earned[it] }, seconds, kills, bosses, deaths, stats.summary(kills))

    companion object {
        /** How long the fight's last blow hangs before the scene moves on. */
        const val HIT_LIFETIME = 1.0

        /** Seconds between the stages of a fight before the next begins on its own. */

        /** The agents of the Abyss's waves are numbered down from here, out of the way of the map's and the crystals'. */
        internal const val ABYSS_AGENT = -10_000
        internal val FIGHT_STREAM = "fight".hashCode().toLong()

        /** Поток свиты фаз боссов (3.92.0): свой, чтобы бой без фаз катился на прежних костях. */
        internal val RETINUE_STREAM = "retinue".hashCode().toLong()

        /**
         * A run of [location] as the seed rolls it — or, [vaal], of the Vaal zone behind its portal, entered with
         * the pools the map left. [campaign] is the hero's campaign as the server holds it after the entry: the
         * windows of chests, crystals and cracks, the boss's return, the map and the Vaal zone rolled; [killed],
         * what of it the server already counted when the run is entered again.
         */
        fun start(
            index: ContentIndex,
            location: Zone,
            run: Run,
            journal: RunJournal,
            gear: HeroGear,
            campaign: CampaignState,
            now: Long,
            heroExperience: Double,
            heroLevel: Int,
            vaalOrbs: () -> Long,
            onRecorded: (RunEvent) -> Unit = {},
            vaal: Boolean = false,
            startPools: HeroPools? = null,
            onCleared: () -> Unit = {},
            onFallen: () -> Unit = {},
            /** Tokens `i*[Run.PACK_SLOTS]+m` the server already counts as killed: a run entered again keeps its dead dead. */
            killed: Collection<Int> = emptyList(),
            /** Chests of the run the server already counts as opened (3.89.0): a run entered again shows them open in their places. */
            opened: Collection<Int> = emptyList(),
            /** Журнал объектов карты (3.90.0): вернувшийся герой видит их использованными, сделки алтарей действуют. */
            features: Collection<FeatureUse> = emptyList(),
            /** Сделки алтарей карты, из которой вошли в Ваал-зону (3.90.0): действуют до конца карты и там. */
            inherited: Map<String, Double> = emptyMap(),
            /** An autorun instead of the stick (3.2.0). */
            auto: AutoPlan? = null,
            /** The combat pet at work (3.5.0): it fights every fight at the hero's side, read again as each begins (3.70.0). */
            pet: () -> Pet? = { null },
        ): ExpeditionRun {
            val zone = if (vaal) VaalZones.zone(location) ?: location else location
            val context = run.context
            val base = AtlasEffects.map(context.active?.effects.orEmpty(), context.atlas)
            // The act's resistance penalty (3.18.0, server 1.16.0) rides with the map's own "less resistances".
            val penalty = index.campaign.resistPenalty(location.code, onMap = context.active != null)
            val acted = if (penalty > 0) MapEffects.sum(base, mapOf(MapStats.HERO_RESIST to penalty)) else base
            val effects = if (vaal) MapEffects.sum(acted, context.vaal?.effects.orEmpty()) else acted
            val rules = index.campaign.combat
            val build = HeroBuild(gear, effects, rules)
            val stats = build.body.stats
            val spawns = Spawns(index, run)
            val buffs = MapEffects.buffs(effects)
            val packs = spawns.packs(vaal, buffs)
            val boss = spawns.boss(zone, buffs, MapEffects.bossBuffs(effects))
            val bossDown = !vaal && campaign.bossDown(location.code, now)
            val vaalZone = campaign.vaalZone?.takeIf { it.mapCode == location.code }
            val portal = !vaal && !campaign.corruptionOpened && (vaalZone != null || run.portal)
            val world = ExpeditionWorld.create(index.campaign.expedition, zone, packs, stats, if (vaal) run.seed xor VAAL_SALT else run.seed, boss, portal)
            if (bossDown) world.bossAbsent()
            val fountains = AtlasEffects.fountains(index.campaign.fountains, context.atlas)
            val extraFountains = MapEffects.fountains(effects)
            world.placeFountains(fountains.count.getOrElse(0) { 0 } + extraFountains, fountains.count.getOrElse(1) { fountains.count.getOrElse(0) { 0 } } + extraFountains, fountains.heal)
            if (!vaal) {
                // Открытые сундуки (3.89.0) стоят на своих местах: всего их столько, сколько осталось, и открытые
                world.placeChests((campaign.chests[location.code.value]?.left ?: 0) + opened.size, opened)
                world.placeCrystals(campaign.crystals[location.code.value]?.crystals.orEmpty())
                world.placeCracks(campaign.abyss[location.code.value]?.cracks.orEmpty())
                // Объекты карты (3.90.0) - после прочих: их места не сдвигают ни сундуков, ни кристаллов
                world.placeFeatures(run.features.all, features, index.campaign.features?.traps?.reach ?: 0.0)
            }
            val pilot = auto?.let { AutoPilot.of(index.campaign.expedition, world, if (vaal) run.seed xor VAAL_SALT else run.seed, bossStands = world.boss?.alive == true) }
            return ExpeditionRun(
                index, zone, run, journal, world, build, rules, run.seed, effects, vaal, startPools, heroExperience, heroLevel, vaalOrbs,
                campaign.corruptionOpened, vaalZone, bossDown, onRecorded, onCleared, onFallen, pilot, pet,
            ).also {
                it.atlas = context.atlas.filterValues { value -> value != 0.0 }
                // Сделки алтарей - до павших: стаи подкрепления встают, и уже убитые из них остаются лежать
                it.bargain(if (vaal) inherited else run.features.bonuses(features))
                // Стражи сокровищ (3.90.0, строка карты) - у комнат и узлов, тоже до павших
                if (!vaal) it.guard()
                world.restore(killed, Run.PACK_SLOTS)
            }
        }

        internal const val VAAL_SALT = 0x5661616C5A6F6E65L
    }
}

/** One foe of a fight: the pack it walked with and its place there. */
internal class FightMember(val agent: MonsterAgent, val index: Int) {
    val monster: RolledMonster get() = agent.pack[index]
}

/** A Vaal orb on a crystal whose outcome is still the server's to tell: the spot and its place among the standing ones. */
internal class Vaaling(val spot: CrystalSpot, val place: Int)

/** Бой, как его называет сервер (3.88.0): пак жетона [pack] ([vaal] - в Ваал-зоне) или босс зоны. */
internal data class FightKey(val pack: Int, val vaal: Boolean, val boss: Boolean) {
    companion object {
        val BOSS = FightKey(0, vaal = false, boss = true)
    }
}
