package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.AbyssWaves
import com.sperance.exileforge.core.campaign.DeathHit
import com.sperance.exileforge.core.campaign.EffectOrigins
import com.sperance.exileforge.core.campaign.FightPulse
import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.HudCadence
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.RunStats
import com.sperance.exileforge.core.campaign.SimWake
import com.sperance.exileforge.core.campaign.Simulation
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.VaalZones
import com.sperance.exileforge.core.campaign.ZoneShare
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.AilmentLaw
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.FateRun
import com.sperance.exileforge.core.campaign.combat.FlaskView
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroCarry
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
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.ExpeditionRules
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.SummonLog
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * One run of a zone: the world, the hero's life across it and the fights on the way.
 *
 * The scene calls [update] once a frame and draws [world] and [fight]; the overlay reads [hud] and sends
 * [RunCommand]s. Nothing here talks to the server: every kill, chest, boss and descent is an event of the
 * [journal]. Its reward is the server's alone: it comes back with the answer as [RunCommand.Settled],
 * and until then the screens say it is on its way — as do the Vaal zone behind a portal and a crystal's Vaal
 * orb, read from the campaign the answer brings ([RunCommand.Campaign]). The hero's life carries from fight to
 * fight; на карте она восстанавливается по [RoadLife], mana comes back on the road, flasks fill with kills.
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
    /** The map's summed effects, the atlas's share in them; a Vaal zone adds its own lines. Без сделок алтаря: с ними встали стаи на входе. */
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
    /**
     * Счёт Предначертания захода: бои захода и его Ваал-зоны делят его - перелив маны, выигранные бои Разгона
     * ([tallyWins]).
     */
    val fateRun: FateRun = FateRun(),
) : Simulation {
    /** The run as the rules roll it; it takes the Vaal zone's context once the portal opens. */
    var run: Run = run
        internal set

    /**
     * Когда отдыхающий страж вернётся на пост, мс часов сервера; null - он не отдыхает. Отдых, что кончился посреди
     * захода, ставит стража на пост: выход снова запечатан.
     */
    internal var guardianBack: Long? = null

    /** Призывы стража героем в окне: с призывом этого захода - от них цена следующего. */
    internal var summons: SummonLog = SummonLog()

    /** Сколько раз стража призвали в этом заходе: не больше `services.summonsPerRun`. */
    internal var summoned: Int = 0

    /** Часы сервера: по ним отдых стража сверяется с тем, что примет сервер. */
    internal var clock: () -> Long = { System.currentTimeMillis() + ServerClock.offset }

    /** Сейчас по часам захода: от них считается «N мин назад» порций «Сумки захода» ([BagEntry.at]). */
    fun now(): Long = clock()

    /** Сделки алтарей захода, сложенные по характеристике: герой и бои несут их до конца карты. */
    var pacts: Map<String, Double> = emptyMap()
        internal set

    /** The map's summed effects with the altars' bargains: the hero's sheet and the map's window read these. */
    var mapEffects: Map<String, Double> = baseEffects
        internal set

    /** Источники долей [mapEffects]: древо гильдии и зелье захода - свои подписи в листе, разборе и полке боя. */
    val origins: EffectOrigins = EffectOrigins.of(run.context, index)
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

    /** Страж на экране-вызове: мир ждёт «В бой»; null - вызова нет. */
    internal var challenge: MonsterAgent? = null
    internal var life = startPools?.life?.coerceIn(0.0, hero.maxLife) ?: hero.maxLife
    val heroLife: Double get() = life

    /**
     * Энергощит на карте: ловушки и недуги бьют сначала в него, как удары в бою, - яд и хаос мимо; он перезаряжается по
     * тем же правилам. Бой его наследует в обе стороны ([HeroCarry.shield]): каким бой его оставил, таким он и на карте, и в
     * следующем бою.
     */
    internal var shield = startPools?.carry?.shield?.coerceIn(0.0, hero.maxShield) ?: hero.maxShield

    /**
     * Что герой несёт между боями ([HeroCarry]): недуги (и ловушек карты), временные баффы и проклятия, заряды - тают на карте по
     * часам дороги ([ailing]), входят в следующий бой и в зону Ваал; щит - отдельно, [shield].
     */
    internal var carry: HeroCarry = startPools?.carry?.copy(shield = null) ?: HeroCarry.NONE

    /** Законы недугов вне боя - те же, что в бою ([AilmentLaw]). */
    internal val law = AilmentLaw(rules)

    /** Урон недугов вне боя, что копится до тика на экране, по недугу. */
    internal val ticks = HashMap<Ailment, AilmentTick>()

    /** Секунда захода, когда карта ранила героя в последний раз: с неё отсчитывается задержка перезарядки щита. */
    internal var hitAt = Double.NEGATIVE_INFINITY
    internal val kit: Loadout get() = build.gear.kit

    /** Темп похода из контента: паузы боя. */
    internal val pace: ExpeditionRules get() = index.campaign.expedition
    internal var mana = startPools?.mana?.coerceIn(0.0, manaCap()) ?: manaCap()
    internal var charges: List<Double> = kit.flasks.mapIndexed { i, flask -> flask?.sheet?.let { startPools?.charges?.getOrNull(i)?.coerceIn(0.0, it.maxCharges) ?: it.maxCharges } ?: 0.0 }
    internal var flaskLeft: List<Double> = kit.flasks.indices.map { startPools?.flaskLeft?.getOrNull(it) ?: 0.0 }
    internal var rates: List<DraughtRate> = kit.flasks.indices.map { startPools?.rates?.getOrNull(it) ?: DraughtRate() }
    val pools: HeroPools get() = HeroPools(life, mana, charges, flaskLeft, rates, carry.copy(shield = shield))
    internal var gate: VaalZone? = null
    internal var crystal: CrystalSpot? = null

    /** The fountain offered, until it is drunk or turned down. */
    internal var fountain: Fountain? = null

    /** Объект карты, чей лист открыт: забег ждёт ответа игрока, как у фонтана. */
    internal var offer: FeatureSpot? = null

    /** Сбор узла ремесла: какой узел и сколько секунд уже прошло; забег стоит, пока сбор идёт. */
    internal var channel: Channel? = null

    /** Числа урона карты на экране (удары и тики эффектов, каждое своё) и номер следующего. */
    internal val hazards = mutableListOf<ShownHazard>()
    internal var nextHazard = 0

    /** Выборы объектов карты по номеру события: отклонённый сервером объект становится прежним. */
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

    /** How far the server has answered the journal, what it granted by event number, and what it refused. */
    internal var answered = 0
    internal val earned = HashMap<Int, Reward>()
    internal val refused = HashSet<Int>()

    /** The rewarding events this run recorded, and what the answers brought for them all told. */
    internal val mine = HashSet<Int>()
    internal var granted = Reward.NONE

    /** Начатые бои: бой - по номеру его ENGAGE; одно ENGAGE на бой. */
    internal val engaged = HashMap<Int, FightKey>()

    /** The events of the fight on the report, and what they brought so far; null before the fight's first kill. */
    internal val fightEvents = mutableListOf<Int>()
    internal var reward: Reward? = null

    /** Every bonus of the hero's atlas on this run, for the map's window: the ones the fight wears and the ones the rolls do. */
    var atlas: Map<String, Double> = emptyMap()
        internal set

    /**
     * Строки окна карты по разделам ([MapSection]), каждая доля один раз: свои строки карты (предмет, зона Ваал, штраф акта, сделки
     * алтарей) - [mapEffects] без того, что в них положил атлас захода ([AtlasEffects.extras]); [atlas] - по долям [origins].
     */
    val windowLines: Map<MapSection, Map<String, Double>>
        get() {
            val laid = AtlasEffects.extras(run.context.atlas)
            val own = (mapEffects.keys + laid.keys).associateWith { (mapEffects[it] ?: 0.0) - (laid[it] ?: 0.0) }.filterValues { abs(it) > MapSection.EPSILON }
            val shares = atlas.flatMap { (code, value) -> origins.parts(code, value).map { (source, share) -> Triple(MapSection.of(source.kind), code, share) } }
                .groupBy({ it.first }) { it.second to it.third }
                .mapValues { (_, lines) -> lines.groupBy({ it.first }, { it.second }).mapValues { (_, parts) -> parts.sum() } }
            return mapOf(MapSection.MAP to own) + shares
        }

    /**
     * Босс основной карты убит в этом заходе: шаг героя по карте × `bossSlainSpeed` ([ExpeditionWorld.heroSpeed]). Не у зоны
     * Ваал - её страж не босс карты; Бездна по карте не ходит.
     */
    var swift = false
        private set

    /** Босс карты пал в этом заходе: герой ускоряется до конца захода. */
    internal fun bossSlain() {
        if (vaal || swift) return
        swift = true
        rebody()
    }

    /** The fight's guardian fell to this hero for the first time: their place among all who beat it. */
    internal var rank: Long? = null

    /** The last level the level-up screen told of; the hero's level at the run's start until then. */
    internal var levelShown: Int = heroLevel

    /**
     * Счёт боевых заданий по ходу захода: что записанные события прибавят счётчикам летописи, когда сервер их примет, -
     * для листа «Задания» на карте. Сам заход уже сосчитан входом.
     */
    internal val questTally = hashMapOf(com.sperance.exileforge.rules.content.Counter.RUNS to 1L)

    /** Копия [questTally] для снимка экрана: одна и та же, пока счёт не сдвинулся, - не новая карта на кадр. */
    internal var questTallyShown: Map<String, Long> = questTally.toMap()

    /** Добыча, что вылетает на карте: события сундуков и объектов захода по порядку. */
    internal val drops = mutableListOf<Int>()

    /** Копия [drops] с их добычей для снимка: меняется только с новым сундуком или ответом, не на кадр. */
    internal var dropsShown: List<MapDrop> = emptyList()

    /** Сундук или объект с добычей открыт событием [n]: его добыча придёт с ответом сервера. */
    internal fun dropped(n: Int) {
        drops += n
        showDrops()
    }

    internal fun showDrops() {
        dropsShown = drops.map { n -> MapDrop(n, earned[n] ?: Reward.NONE.takeIf { n < answered }) }
        bagShown = bag.map { (n, tag) -> BagEntry(n, tag.first, tag.second, earned[n] ?: Reward.NONE.takeIf { n < answered }) }.filter { it.shown }.asReversed()
    }

    /** Все события захода с добычей: источник и когда записано - для «Сумки захода»; ответы - [earned]. */
    internal val bag = LinkedHashMap<Int, Pair<LootFrom, Long>>()

    /** Порции [bag] с ответами, новые сверху: меняется только с ответом сервера, не на кадр. */
    internal var bagShown: List<BagEntry> = emptyList()

    /** The hero's campaign as the last answer brought it. */
    internal var campaign: CampaignState? = null

    /** The portal's opening, until the server tells the Vaal zone behind it. */
    internal var gateEvent: Int? = null

    /** Vaal orbs on crystals, by event, until the server tells what they did. */
    internal val vaalings = HashMap<Int, Vaaling>()

    /** The crystals the server's answers said Vaal orbs made, by event, until their orbs are resolved. */
    internal val vaaled = HashMap<Int, Crystal>()
    internal var fallEvent: Int? = null

    internal var slain: RolledMonster? = null
    internal var report: FightReport? = null

    /** Журнал последнего боя захода, любого - и Бездны: его открывает «Журнал боя» меню забега. */
    internal var lastFight: FightReport? = null

    /** Прошлый бой захода выигран: следующий бой открывается силами `FIGHT_CLEAR`. */
    internal var wonLast = false

    /** The foes of the fight in the battle's order, each with the pack it walked with. */
    internal var members: List<FightMember> = emptyList()

    /** The dice stream of the fight's battle. */
    internal var fightStream = 0L
    internal var reported = 0
    internal var fall: Double? = null
    internal var kills = 0

    /** The map's summary (see [MapTally]): how it ended, the seconds on it, the guardians slain and the deaths. */
    internal var end: MapEnd? = null
    internal var seconds = 0.0
    internal var bosses = 0

    /** Страж этой карты (или зоны Ваал) пал в этом заходе: итог ухода - «пройдена» ([MapEnd]). */
    internal var guardianSlain = false
    internal var deaths = 0

    /** The run's figures and, after a fall, its last blows. */
    internal val stats = RunStats()
    internal var recap: List<DeathHit> = emptyList()
    internal var fightAgent: MonsterAgent? = null

    /** Все стаи боя: вступившая первой, затем собранные ею по раундам. */
    internal var fightAgents: List<MonsterAgent> = emptyList()
    internal val waves get() = AbyssWaves(index, run)
    internal val abyssRule get() = index.campaign.abyss

    internal fun manaCap(): Double = hero.maxMana * (1 - kit.reserved(hero) / 100)

    internal fun regear(gear: HeroGear) {
        val next = HeroBuild(gear, mapEffects, rules, origins = origins)
        val before = hero
        build = next
        rebody()
        life = if (before.maxLife > 0) life / before.maxLife * hero.maxLife else hero.maxLife
        charges = next.gear.kit.flasks.mapIndexed { i, flask -> flask?.sheet?.let { (charges.getOrNull(i) ?: 0.0).coerceIn(0.0, it.maxCharges) } ?: 0.0 }
        flaskLeft = next.gear.kit.flasks.indices.map { i -> if (next.gear.kit.flasks[i] != null) flaskLeft.getOrNull(i) ?: 0.0 else 0.0 }
        rates = next.gear.kit.flasks.indices.map { i -> if (next.gear.kit.flasks[i] != null) rates.getOrNull(i) ?: DraughtRate() else DraughtRate() }
        rebody()
    }

    /**
     * The hero between fights made again: the sheet with the draughts still running, what the hero carries from the fight (баффы,
     * проклятия и заряды [carry]), and the pace and sight they give.
     */
    internal fun rebody() {
        val lines = kit.flasks.withIndex().filter { (i, flask) -> flask != null && (flaskLeft.getOrNull(i) ?: 0.0) > 0 }
            .flatMap { (_, flask) -> flask!!.sheet.draught(build.body, life, 0.0).lines } + carry.lines(kit.charges)
        hero = if (lines.isEmpty()) build.body else build.body(lines)
        mana = mana.coerceIn(0.0, manaCap())
        shield = shield.coerceIn(0.0, hero.maxShield)
        world.regear(ExpeditionWorld.heroSpeed(pace, hero.stats, swift), ExpeditionWorld.lightRadius(pace, hero.stats, zone.light))
    }

    var fight: Battle? = null
        internal set

    internal val state = MutableStateFlow(snapshot())
    val hud: StateFlow<RunHud> = state.asStateFlow()

    /** Пульс боя на кадр: часы и замахи; снимок ([hud]) выходит реже ([cadence]). */
    val pulse = FightPulse()
    private val cadence = HudCadence()

    /** Кадр нужен, пока заход идёт ([active]) или ждёт команда: на паузе боя, под листом и в итогах часы спят. */
    private val wake = SimWake()
    override val awake: StateFlow<Boolean> = wake.awake

    /** Заход идёт: не кончен, не в итогах боя, не под листом и не на паузе или в ожидании начала боя. */
    private val active: Boolean get() = phase !in RESTING && holds == 0 && !(phase == RunPhase.FIGHT && (!started || paused))

    fun send(command: RunCommand) {
        commands.add(command)
        wake.poke()
    }

    /** Предложенный фонтан, лист объекта карты и сбор узла держат шаг, пока игрок не ответит. */
    override fun update(dt: Double) {
        val poked = wake.took()
        var commanded = false
        while (true) {
            handle(commands.poll() ?: break)
            commanded = true
        }
        if (phase == RunPhase.MAP) guardianBack?.takeIf { clock() >= it + GUARDIAN_GRACE_MS }?.let { guardianReturns() }
        if (phase != RunPhase.DEAD && phase != RunPhase.CLEARED && phase != RunPhase.LEFT) seconds += dt
        hazards.forEach { it.left -= dt }
        hazards.removeAll { it.left <= 0 }
        if (holds == 0) {
            when (phase) {
                RunPhase.MAP -> {
                    channel?.let { gather(it, dt) }
                    if (fountain == null && offer == null && channel == null) walk(dt)
                }

                RunPhase.FIGHT -> play(dt)

                else -> Unit
            }
        }
        pulse.capture(fight)
        pulse.gather(channel?.elapsed, channel?.spot?.node?.seconds ?: 0.0)
        if (cadence.due(dt, commanded || poked, leaps(), fight)) state.value = snapshot()
        wake.rest(active)
    }

    /** Что меняется скачком вне боя: снимок с этим выходит сразу, а не со следующим тактом ([HudCadence]). */
    private fun leaps(): Long = HudCadence.key(
        phase.ordinal.toLong(), if (world.sealed) 1 else 0, if (bossDown) 1 else 0, guardianBack ?: -1, (fountain?.id ?: -1).toLong(), (offer?.id ?: -1).toLong(),
        kills.toLong(), mine.size.toLong(), world.alive.toLong(), (crystal?.id ?: -1).toLong(), if (channel == null) 0 else 1,
    )

    /**
     * Призыв стража до конца его отдыха: страж повержен (раньше или в этом заходе), не в зоне Ваал и не больше
     * `services.summonsPerRun` за заход; цену ([summonPrice]) спишет сервер, событие журнала - [RunEvent.Summon]. Хватает ли золота,
     * решает кнопка: правило клиента - мелкая проверка доступа.
     */
    internal fun summon() {
        if (!summonable) return
        record(RunEvent::Summon) ?: return
        summons = summons.after(index.campaign.services, clock())
        summoned++
        guardianReturns()
    }

    /** Стража можно призвать сейчас: он повержен, это карта, а не зона Ваал, и заход ещё не исчерпал призывы. */
    internal val summonable: Boolean get() = phase == RunPhase.MAP && !vaal && bossDown && summoned < index.campaign.services.summonsPerRun

    /** Цена призыва стража сейчас: одна функция правил по уровню района захода и призывам окна ([SummonLog.price]). */
    internal fun summonPrice(): Long = summons.price(index.campaign.services, run.areaLevel, clock())

    /** Отдых стража кончился: он снова на посту. */
    private fun guardianReturns() {
        guardianBack = null
        bossDown = false
        world.bossReturns()
    }

    /** One event of the journal, and the listener told. */
    internal fun record(event: (n: Int) -> RunEvent): RunEvent? = journal.record(event)?.also(onRecorded)?.also(::counted)

    private fun counted(event: RunEvent) {
        fun add(counter: String) {
            questTally.merge(counter, 1L, Long::plus)
            questTallyShown = questTally.toMap()
        }
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

    /** A rewarding event recorded: what it brings comes with the server's answer, into the run's count and, [fought], the fight's report. */
    internal fun rewarding(event: RunEvent?, fought: Boolean = false): RunEvent? = event?.also {
        mine += it.n
        bag[it.n] = LootFrom.of(it, run) to clock()
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

    /** What this run hands the map it was entered from, a Vaal zone over: its rewarding events and its own counts. */
    fun share(): ZoneShare = ZoneShare(mine.associateWith { earned[it] }, seconds, kills, bosses, deaths, stats.summary(kills))

    companion object {
        /** Фазы, где заход не шагает сам: итоги боя, гибель, пройденная и покинутая карта - их будит только команда. */
        private val RESTING = setOf(RunPhase.LOOT, RunPhase.DEAD, RunPhase.CLEARED, RunPhase.LEFT)

        /** How long the fight's last blow hangs before the scene moves on. */
        const val HIT_LIFETIME = 1.0

        /** The agents of the Abyss's waves are numbered down from here, out of the way of the map's and the crystals'. */
        internal const val ABYSS_AGENT = -10_000
        internal val FIGHT_STREAM = "fight".hashCode().toLong()

        /** Поток свиты фаз боссов: свой, чтобы бой без фаз катился на своих костях. */
        internal val RETINUE_STREAM = "retinue".hashCode().toLong()

        /**
         * A run of [location] as the seed rolls it — or, [vaal], of the Vaal zone behind its portal, entered with
         * the pools the map left. [campaign] is the hero's campaign as the server holds it after the entry: the
         * windows of chests, crystals and cracks, the boss's return, the map and the Vaal zone rolled; [killed],
         * what of it the server already counted when the run is entered again.
         *
         * Заход, войденный снова, держит павших павшими, открытые сундуки открытыми на своих местах, объекты карты использованными, сделки
         * алтарей действующими (и в Ваал-зоне); призывы стража - счётом сервера. Автопробег - вместо стика. Штраф сопротивлений акта едет с
         * «меньше сопротивлений» карты. Порядок расстановки: объекты карты - после прочих (их места не сдвигают сундуков и кристаллов);
         * сделки алтарей, стражи сокровищ, стаи Следопыта и монстры стоявших объектов - до павших. Принесённое из основной карты в
         * Ваал-зону - на листе героя сразу.
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
            killed: Collection<Int> = emptyList(),
            opened: Collection<Int> = emptyList(),
            features: Collection<FeatureUse> = emptyList(),
            inherited: Map<String, Double> = emptyMap(),
            summoned: Int = 0,
            fateRun: FateRun = if (vaal) FateRun(vaalKilled = killed) else FateRun(killed),
        ): ExpeditionRun {
            val zone = if (vaal) VaalZones.zone(location, index.campaign.vaal) ?: location else location
            val context = run.context
            val base = AtlasEffects.map(context.active?.effects.orEmpty(), context.atlas)
            val penalty = index.campaign.resistPenalty(location.code, onMap = context.active != null)
            val acted = if (penalty > 0) MapEffects.sum(base, mapOf(MapStat.HERO_RESIST.code to penalty)) else base
            val effects = if (vaal) MapEffects.sum(acted, context.vaal?.effects.orEmpty()) else acted
            val rules = index.campaign.combat
            val build = HeroBuild(gear, effects, rules, origins = EffectOrigins.of(context, index))
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
            world.placeFountains(fountains.count.getOrElse(0) { 0 }, fountains.count.getOrElse(1) { fountains.count.getOrElse(0) { 0 } }, fountains.heal)
            if (!vaal) {
                world.placeChests((campaign.chests[location.code.value]?.left ?: 0) + opened.size, opened)
                world.placeCrystals(campaign.crystals[location.code.value]?.crystals.orEmpty())
                world.placeCracks(campaign.abyss[location.code.value]?.cracks.orEmpty())
                world.placeFeatures(run.features.all, features, index.campaign.features?.traps)
            }
            return ExpeditionRun(
                index, zone, run, journal, world, build, rules, run.seed, effects, vaal, startPools, heroExperience, heroLevel, vaalOrbs,
                campaign.corruptionOpened, vaalZone, bossDown, onRecorded, onCleared, onFallen, fateRun,
            ).also {
                if (!it.carry.bare) it.rebody()
                it.tallyWins()
                it.atlas = context.atlas.filterValues { value -> value != 0.0 }
                it.bargain(if (vaal) inherited else run.features.bonuses(features))
                if (!vaal) it.guard()
                if (!vaal) it.track()
                if (!vaal) world.features.forEach { spot -> spot.resume(it, killed) }
                world.restore(killed, Run.PACK_SLOTS)
                if (bossDown) it.guardianBack = campaign.bosses[location.code.value]
                it.summons = campaign.summons
                it.summoned = summoned
            }
        }

        internal const val VAAL_SALT = 0x5661616C5A6F6E65L

        /** Запас к концу отдыха стража, мс: часы клиента и сервера сверены с точностью до задержки ответа. */
        internal const val GUARDIAN_GRACE_MS = 2_000L
    }
}

/**
 * Член [index] стаи [agent] в бою; [round] - раунд, в котором он выходит на поле.
 *
 * Враг боя: стая, с которой он шёл, и его место в ней.
 */
internal class FightMember(val agent: MonsterAgent, val index: Int, val round: Int = 0) {
    val monster: RolledMonster get() = agent.pack[index]
}

/** A Vaal orb on a crystal whose outcome is still the server's to tell: the spot and its place among the standing ones. */
internal class Vaaling(val spot: CrystalSpot, val place: Int)

/** Бой, как его называет сервер: пак жетона [pack] ([vaal] - в Ваал-зоне) или босс зоны. */
internal data class FightKey(val pack: Int, val vaal: Boolean, val boss: Boolean) {
    companion object {
        val BOSS = FightKey(0, vaal = false, boss = true)
    }
}
