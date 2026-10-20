package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.AbyssDepth
import com.sperance.exileforge.core.campaign.ChargeView
import com.sperance.exileforge.core.campaign.DeathHit
import com.sperance.exileforge.core.campaign.FightStats
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.RunSummary
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.FateGlow
import com.sperance.exileforge.core.campaign.combat.FlaskView
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.LifeFlow
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.PowerView
import com.sperance.exileforge.core.campaign.combat.RiftGuard
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.SkillView
import com.sperance.exileforge.core.campaign.combat.StrikeView
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.WorldKind
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.EffectSource
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.sheet.SourceKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** Where a run stands: walking, fighting, at a kill's loot, at a Vaal portal's gate, at a crystal, at a crack of the Abyss, or over one way or another. */
enum class RunPhase { MAP, FIGHT, LOOT, GATE, CRYSTAL, ABYSS, DEAD, CLEARED, LEFT, CHALLENGE }

/**
 * Что изменит смена набора в походе ([ExpeditionRun.preview]): здоровье держит число [life] под новым максимумом [newMaxLife] (было
 * [maxLife]); прибавки к добыче следующих боёв и неоткрытых сундуков, что сдвинутся ([loot]); новая фляга - [flask], null - фляги не меняются.
 */
data class GearPreview(val life: Double, val maxLife: Double, val newMaxLife: Double, val loot: List<LootShift>, val flask: FlaskSwap?)

/** Прибавка героя к добыче вида [kind] с обычного монстра, процентов: сейчас [before], с новым набором [after]. */
data class LootShift(val kind: WorldKind, val before: Double, val after: Double)

/**
 * Фляга, что встанет на место [slot] пояса (с нуля) пустой - 0 из [maxCharges] зарядов; [replaced] - id снятой фляги этого места
 * (null - место было пустым) и её заряды [lost], что пропадут.
 */
data class FlaskSwap(val slot: Int, val maxCharges: Int, val replaced: String?, val lost: Int)

/** Почему набор в походе сейчас не сменить: идёт бой, спуск в Бездну или заход окончен; подпись - `expedition.gear_lock.<вид>`. */
enum class GearLock {
    FIGHT,
    ABYSS,
    OVER,
    ;

    /** Ключ подписи причины. */
    val key: String get() = "expedition.gear_lock.${name.lowercase()}"
}

/** A number floating off a fighter, [age] seconds after the blow that made it; [foe] is the foe of the pack it was about. */
data class FloatingHit(
    val id: Int,
    val target: Side,
    val action: Action,
    val kind: HitKind,
    val amount: Int,
    val age: Double,
    val healed: Int,
    val type: DamageType?,
    val inflicted: List<Ailment>,
    val stunned: Boolean,
    val foe: Int = 0,
    /** Недуг тика: яд рисуется цветом своей плитки, а не хаосом; у удара - null. */
    val ailment: Ailment? = null,
)

/** The blow on screen right now, for the cards to act out. */
data class LungeView(
    val actor: Side,
    val action: Action,
    val kind: HitKind,
    val landed: Boolean,
    val progress: Float,
    val foe: Int = 0,
)

/** One ailment on a fighter as its tile shows it: the share of time [left], the [stacks], the [seconds] it still holds and its [strength]. */
data class AilmentView(val ailment: Ailment, val left: Float, val stacks: Int, val seconds: Double = 0.0, val strength: Double = 0.0)

/** One foe of the pack as its card prints it. */
data class FoeView(
    val index: Int,
    val monster: RolledMonster,
    val life: Int,
    val maxLife: Int,
    val shield: Int,
    val maxShield: Int,
    val swing: Float,
    val ailments: List<AilmentView>,
    val held: Boolean,
    val alive: Boolean,
    val reachable: Boolean,
    val taunt: Boolean = false,
    val effects: List<EffectView> = emptyList(),
    val mana: Int = 0,
    val maxMana: Int = 0,
    /** Its place on the field; -1 - свита босса, что ещё не звана. */
    val place: Int = index,
    /** Its buildups, null while the rules have none. */
    val buildup: BuildupView? = null,
    /** Барьер: что ещё поглотит барьер фазы босса. */
    val barrier: Int = 0,
    /** Ждёт своего раунда: ещё не на поле, но его ещё бить. */
    val waiting: Boolean = false,
) {
    /** Its card is on the field: it fights there, or fell there. */
    val onField: Boolean get() = place >= 0
}

/**
 * Ярость боя (в полосе стража; любого боя): [stage] ступеней уже прошло, урон врагов больше на [damage]
 * процентов, до следующей ступени [next] секунд; null - ступеней больше не будет.
 */
data class RageView(val stage: Int, val damage: Double, val next: Double?)

/**
 * Босс в бою: [index] - его место в стае, [phase] - шаблон фаз, [marks] - пороги шагов в процентах здоровья и
 * [passed] - какие уже сработали, [cast] - умение, что он готовит, и через сколько секунд из скольких.
 */
data class BossHud(
    val index: Int,
    val phase: String?,
    val marks: List<Double>,
    val passed: List<Boolean>,
    val cast: CastView?,
    /** Слоты вокруг босса: по порядку, null - пустой; первая половина слева, вторая справа. */
    val slots: List<SlotView?> = emptyList(),
    /** Страж Разлома: кто он; null - обычный босс, поля ниже пусты. */
    val rift: RiftGuard? = null,
    /** Печати брони Стража Врат: стоят [seals] из [sealsMax], к следующей снятой - [sealHits] попаданий из [sealEvery]. */
    val seals: Int = 0,
    val sealsMax: Int = 0,
    val sealHits: Int = 0,
    val sealEvery: Int = 0,
    /**
     * Законы Владыки: [law] - в силе, [lawNext] - объявленные (только в знамение перед сменой), [lawIn] - секунд до смены;
     * [lawElement] - стихия «Одной стихии» в силе, [lawNextElement] - объявленной.
     */
    val law: List<RiftLaw> = emptyList(),
    val lawNext: List<RiftLaw> = emptyList(),
    val lawIn: Double = 0.0,
    val lawElement: String? = null,
    val lawNextElement: String? = null,
    /** Поглотитель эха: коды украденных даров по порядку и его сила от Эха, «больше» в процентах. */
    val stolen: List<String> = emptyList(),
    val devoured: Double = 0.0,
    /** Босс Скверны (Матерь): осквернён - его шаги фаз рвут пуповины «Сердца Скверны». */
    val tainted: Boolean = false,
) {
    /** Сработавших шагов фаз - у Матери столько пуповин уже порвано (портрет и полоса гасят их по одной). */
    val torn: Int get() = passed.count { it }

    /** Пуповин «Сердца Скверны»: по одной на каждый порог фазы и последняя - на смерть. */
    val cords: Int get() = marks.size + 1
}

/** Кто в слоте вокруг босса: приспешник свиты или тотем. */
sealed interface SlotView {
    /** Приспешник: [index] - его место в стае. */
    data class Minion(val index: Int) : SlotView

    /** Тотем [code] рода [kind]: [serial] - его номер в бою, [left] секунд из [total]; [element] - стихия удара. */
    data class Totem(val serial: Int, val code: String, val kind: com.sperance.exileforge.rules.content.TotemKind, val left: Double, val total: Double, val element: String?) : SlotView {
        val share: Float get() = if (total <= 0) 0f else (left / total).toFloat().coerceIn(0f, 1f)
    }
}

/**
 * Применение умения героя на экране («Жилы энергии»): умение [code] вида [type], его стихия [element] (null - без стихии),
 * тир в бою [tier], по кому оно легло ([foes]), на себя ли ([self]), эхо ли это ([echo]) и как далеко сцена ([progress], 0..1).
 * [serial] - номер строки лога: новая строка - новая сцена.
 */
data class HeroCastView(
    val serial: Int,
    val code: String,
    val type: SkillType,
    val element: String?,
    val tier: Int,
    val foes: List<Int>,
    val self: Boolean,
    val echo: Boolean,
    val progress: Float,
)

/**
 * Умение врага на полосе босса в состоянии [state]: применяется - [left] секунд до конца применения из [total], отсчёт - до
 * готовности; готово, но босс применить его не может ([CastState.held]) - [left] ноль.
 */
data class CastView(val code: String, val left: Double, val total: Double, val state: CastState) {
    val progress: Float get() = if (total <= 0) 1f else (1 - left / total).toFloat().coerceIn(0f, 1f)
}

/** Что умение босса делает сейчас; [held] - готово, но не применяется (подпись - словом, без отсчёта и телеграфа). */
enum class CastState(val held: Boolean) {
    CASTING(false),
    COUNTDOWN(false),
    STUNNED(true),
    FROZEN(true),
    SILENCED(true),

    /** Повода нет: лечиться не с чего, благо или проклятие уже лежит, у героя нет маны. */
    WAITING(true),
}

/**
 * A fighter's buildups as its card shows them: the fullest bar and its share, every bar's share by [Buildup] order
 * for its window, and which went off and holds it now.
 */
data class BuildupView(val leading: Buildup?, val share: Float, val bars: List<Float>, val stunned: Boolean, val frozen: Boolean, val electrocuted: Boolean)

/**
 * Провокация героя для подсказки: шанс [chance]%, на [seconds] секунд, урон спровоцированного по герою
 * меньше на [cut]% (боссов - [bossCut]%), повторно - через [immunity] секунд (боссов - [bossImmunity]).
 */
data class ProvokeView(val chance: Double, val seconds: Double, val cut: Double, val bossCut: Double, val immunity: Double, val bossImmunity: Double)

data class FightHud(
    val leader: RolledMonster,
    val foes: List<FoeView>,
    val heroLife: Int,
    val heroShield: Int,
    val hits: List<FloatingHit>,
    val speed: Int,
    val outcome: Outcome?,
    val heroSwing: Float = 0f,
    val heroAilments: List<AilmentView> = emptyList(),
    val heroHeld: Boolean = false,
    /** The hero's buildups. */
    val heroBuildup: BuildupView? = null,
    val lunge: LungeView? = null,
    val events: List<CombatEvent> = emptyList(),
    val started: Boolean = true,
    val paused: Boolean = false,
    val target: Int? = null,
    val focus: Int? = null,
    /** Провокация героя: её числа для подсказки; null - героя без провокации. */
    val heroProvoke: ProvokeView? = null,
    /** [heroMaxMana] is the mana the auras leave free; [heroReserved] what they hold past it. */
    val heroMana: Int = 0,
    val heroMaxMana: Int = 0,
    val heroReserved: Int = 0,
    val skills: List<SkillView?> = emptyList(),
    val flasks: List<FlaskView?> = emptyList(),
    val heroEffects: List<EffectView> = emptyList(),
    /** The hero's frenzy, power and endurance charges, a counter per kind held. */
    val heroCharges: List<ChargeView> = emptyList(),
    /** Действующие рычаги Предначертания героя: значок ✦ среди его состояний. */
    val heroFate: List<FateGlow> = emptyList(),
    val heroBarrier: Int = 0,
    /** Максимум здоровья и щита боевого тела героя: шкала полос [heroLife] и [heroShield] в бою. */
    val heroMaxLife: Int = 0,
    val heroMaxShield: Int = 0,
    /** The level the foes stand at: a depth of the Abyss stands deeper than its zone. */
    val level: Int = 0,
    /** Бой цепочки, с 1, из [rounds]: босс раша, бой этажа Башни, волна Разлома, раунд собранных стай; у одиночного боя - 1 из 1. */
    val round: Int = 1,
    val rounds: Int = 1,
    /** Лист героя сейчас: по нему лист умения врага считает, сколько оно снимет. */
    val heroBody: com.sperance.exileforge.core.campaign.combat.Combatant? = null,
    /** Босс боя: его фазы и что он готовит; null - босса нет. */
    val boss: BossHud? = null,
    /** Ярость боя: ступень, прибавка урона врагов и отсчёт до следующей; null - бой кончен. */
    val rage: RageView? = null,
    /** Последнее применение умения героя, пока его сцена на экране; null - сцены нет. */
    val heroCast: HeroCastView? = null,
    /** Все удары на экране: одновременные - каждый своей сценой, не только последний ([lunge]). */
    val strikes: List<StrikeView> = emptyList(),
    /** Итог здоровья героя за последнюю секунду: плитка «−12/с» и её разбор. */
    val heroFlow: LifeFlow = LifeFlow.NONE,
    /** Пассивные умения героя на весь бой: ауры и бонусы на полке «на весь бой». */
    val heroPassives: List<EffectView> = emptyList(),
    /** Силы уникальных предметов героя: постоянные - на рельсе, силы-события - на временной полке со вспышкой, счётом и перезарядкой. */
    val heroPowers: List<PowerView> = emptyList(),
    /** Секунды боя, когда снят этот снимок: кадр двигает от них сцены ударов ([com.sperance.exileforge.core.campaign.FightPulse]). */
    val time: Double = 0.0,
    /** Выпад удара правил ([CombatRules.lunge]), секунды: мера сцен ударов, применения и выпада. */
    val beat: Double = 0.0,
) {
    val scouting: Boolean get() = outcome == null && (!started || paused)

    /** The foes on the field, by their places. */
    val field: List<FoeView> get() = foes.filter { it.onField }.sortedBy { it.place }

    /** How many of the whole pack are still to be beaten: those on the field and those waiting for their round. */
    val standing: Int get() = foes.count { it.alive || it.waiting }
}

/** One member of a pack fought and its own log. */
data class PackHit(
    val monster: RolledMonster,
    val events: List<CombatEvent>,
    val duration: Double,
    /** Когда начался его бой, секунд от начала шага испытания (этажа башни): время событий боя считается с нуля. */
    val start: Double = 0.0,
)

/** A fight that is over, as the screen after it reads it. */
data class FightReport(val monster: RolledMonster, val outcome: Outcome, val pack: List<PackHit>, val duration: Double) {
    val events: List<CombatEvent> get() = pack.flatMap { it.events }
    val packSize: Int get() = pack.size
    private fun mine() = events.filter { it.actor == Side.HERO }
    private fun theirs() = events.filter { it.actor == Side.MONSTER }
    val dealt: Int get() = mine().sumOf { it.damage }.roundToInt()
    val taken: Int get() = theirs().sumOf { it.damage }.roundToInt()
    val crits: Int get() = mine().count { it.kind == HitKind.CRIT }
    val blocked: Int get() = mine().count { it.kind == HitKind.BLOCKED }
    val inflicted: List<Ailment> get() = mine().flatMap { it.inflicted }.distinct()

    /** Урон героя в секунду за весь бой; бой без времени - ноль. */
    val perSecond: Int get() = if (duration > 0) (dealt / duration).roundToInt() else 0

    /** Пройденные пороги фаз по журналу боя, в порядке срабатывания: шаблон фаз и порог в процентах здоровья. */
    val phases: List<PhaseMark> by lazy {
        events.mapNotNull { event -> (event.trace as? NoteTrace)?.takeIf { it.kind == NoteKind.PHASE }?.let { PhaseMark(it.ref, it.value) } }
    }

    /** Разбор этого боя: лист «Разбор» итога боя, считается один раз. */
    val stats: FightStats by lazy { FightStats.of(pack, duration) }
}

/** Сработавший порог фаз босса: шаблон фаз [phase] и порог [at] в процентах здоровья. */
data class PhaseMark(val phase: String, val at: Double)

/** Everything the overlay draws, as one value: it changes only when something on it does. */
data class RunHud(
    val phase: RunPhase,
    val mapCode: MapCode,
    val heroLife: Int,
    val heroMaxLife: Int,
    val heroShield: Int,
    val heroMaxShield: Int,
    val alive: Int,
    val total: Int,
    /** The exit is sealed while the zone's boss lives. */
    val sealed: Boolean = false,
    val fight: FightHud? = null,
    /**
     * What the fight just won brought, as the server's answers bring it: nothing is rolled here,
     * and [rewardAwaiting] of its events are still to be answered — the report opens at once and fills in.
     */
    val reward: Reward? = null,
    val rewardAwaiting: Int = 0,
    /** A first win over this guardian: the hero's place among all who beat it. */
    val rank: Long? = null,
    /** Ключи очков Атласа, что принёс выигранный бой: строкой в окне уровня, если он поднял уровень, иначе плиткой отчёта. */
    val atlas: List<String> = emptyList(),
    /** The levels the answered experience raised the hero by and the report has not told of yet. */
    val levelUp: LevelUp? = null,
    val slain: RolledMonster? = null,
    val report: FightReport? = null,
    /** Журнал последнего боя захода - пункт «Журнал боя» меню забега; null - боя ещё не было. */
    val lastFight: FightReport? = null,
    /** What the death cost, by the rules' price until the server's answer replaces it. */
    val fall: Double? = null,
    /** Уровень героя, с которого гибель стоит опыта, пока герой ниже него; null - цена уже действует. */
    val fallFrom: Int? = null,
    /** The run's gold and experience as the server's answers granted them; [awaiting] of its rewarding events are not answered yet. */
    val gold: Long = 0,
    val experience: Double = 0.0,
    val kills: Int = 0,
    /** The run's figures and the blows that ended it. */
    val summary: RunSummary = RunSummary(),
    val recap: List<DeathHit> = emptyList(),
    val awaiting: Int = 0,
    val chestsLeft: Int = 0,
    /** Добыча сундуков и объектов захода по порядку: ответ на каждый - в сумку, ждущие - с «…». */
    val drops: List<MapDrop> = emptyList(),
    /** Вся добыча захода по событиям, новая сверху («Сумка захода»): бои, сундуки, объекты, Ваал-зона. */
    val bag: List<BagEntry> = emptyList(),
    val fountainsLeft: Int = 0,
    /** The fountain offered: the map waits for the player's word — drink, or step away and leave it standing. */
    val fountain: FountainView? = null,
    /** Уход у открытого выхода предложен: карта ждёт слова игрока - завершить карту (из зоны Ваал - вернуться на неё) или отойти. */
    val exitOffered: Boolean = false,
    /** Объекты карты, которых герой ещё не тронул ([ExpeditionWorld.untouched]). */
    val featuresLeft: Int = 0,
    /** The Vaal zone behind the portal the hero stands at. */
    val gate: VaalZone? = null,
    /** This run is a Vaal zone: no way out but its guardian or a death, and a death is not the map's end. */
    val vaal: Boolean = false,
    /** [heroMaxMana] is the mana the auras leave free; [heroReserved] what they hold past it. */
    val heroMana: Int = 0,
    val heroMaxMana: Int = 0,
    val heroReserved: Int = 0,
    /** Здоровье, что держат ауры сверх [heroMaxLife]: хвост полосы здоровья. */
    val heroLifeReserved: Int = 0,
    val flasks: List<FlaskView?> = emptyList(),
    val crystal: CrystalView? = null,
    val crystalsLeft: Int = 0,
    val abyss: AbyssView? = null,
    val cracksLeft: Int = 0,
    /** The guardian is slain and not yet back. */
    val bossDown: Boolean = false,
    /** Секунды до возвращения отдыхающего стража; null - он не отдыхает: стоит или повержен в этом заходе. */
    val guardianRest: Int? = null,
    /** Призыв поверженного стража; null - звать некого: страж на посту или это зона Ваал. */
    val summon: SummonView? = null,
    /** Босс карты убит в этом заходе: герой идёт быстрее - след и значок над ним. */
    val swift: Boolean = false,
    /** Счёт боевых заданий за этот заход: счётчик летописи - сколько прибавит. */
    val questTally: Map<String, Long> = emptyMap(),
    /** Выигранных в заходе боёв Разгона ([com.sperance.exileforge.core.campaign.combat.FateRun.won]); без рычага - 0. */
    val fateWon: Int = 0,
    /** Events of the journal the server has not taken yet, the number of the oldest of them, and the ones it refused. */
    val pending: Int = 0,
    val applied: Int = 0,
    val rejected: Int = 0,
    /** What the map came to so far: its summary before the camp. */
    val tally: MapTally = MapTally(),
    /** Лист объекта карты: алтарь, торговец или узел, к которому подошёл герой. */
    val feature: FeatureView? = null,
    /** Последний удар ловушки, пока он висит на полосе карты. */
    /** Числа урона карты над героем: удары ловушек и тики их эффектов. */
    val hazards: List<HazardView> = emptyList(),
    /**
     * Что герой несёт вне боя ([com.sperance.exileforge.core.campaign.combat.HeroCarry]): недуги (из боя и от ловушек), баффы и
     * проклятия, заряды - плитками полки под полосами героя, как в бою.
     */
    val heroAilments: List<AilmentView> = emptyList(),
    val heroEffects: List<EffectView> = emptyList(),
    val heroCharges: List<ChargeView> = emptyList(),
    /** Итог здоровья на карте в секунду: регенерация и отдых против дегена и недугов; null - в бою и не на карте. */
    val lifeFlow: LifeFlow? = null,
    /** Действие героя у объекта карты (вскрытие трещины, сбор узла, запертая дверь); null - героя ничто не держит. */
    val action: ObjectAction? = null,
    /** Экран-вызов перед стражем; null - его нет. */
    val challenge: ChallengeView? = null,
    /** Очаг Скверны карты; null - очага нет. */
    val blight: BlightView? = null,
    /** Почему набор сейчас не сменить; null - между боями, находку захода можно надеть. */
    val gearLock: GearLock? = null,
    /** Набор, что поход носит сейчас: id вещи по месту. */
    val worn: Map<Slot, String> = emptyMap(),
) {
    /**
     * Итог ведёт обратно на карту под зоной: зона Ваал, из которой вышли живыми. Гибель в зоне заканчивает весь заход -
     * по фазе этого не понять: отчёт о гибели уже закрыт («Дальше» переводит DEAD в LEFT), а конец захода остаётся `FELL`.
     */
    val returnsToMap: Boolean get() = vaal && tally.end != MapEnd.FELL
}

/** Страж на экране-вызове: его ролл, уровень, полное здоровье, шаблон фаз с порогами; [vaal] - страж Ваал-зоны. */
data class ChallengeView(
    val boss: RolledMonster,
    val level: Int,
    val maxLife: Double,
    val phase: String?,
    val marks: List<Double>,
    val vaal: Boolean,
)

/** The Abyss as its sheet shows it: how many depths the crack leads down, how many are cleared, every depth's wave and hoard, and the share a fall keeps. */
data class AbyssView(
    val depth: Int,
    val cleared: Int,
    val open: Boolean,
    val depths: List<AbyssDepth>,
    val hoard: Reward? = null,
    val fallen: Boolean = false,
    val hoardAwaiting: Boolean = false,
) {
    val current: AbyssDepth? get() = depths.getOrNull(cleared - 1)
    val next: AbyssDepth? get() = if (cleared < depth) depths.getOrNull(cleared) else null
}

/**
 * Объект карты, чей лист открыт: объект правил - что в нём, - выборы, уже сделанные на нём, доля
 * идущего сбора узла (null - сбор не идёт) и стоят ли его стражи сокровищ.
 */
data class FeatureView(val feature: MapFeature, val taken: List<Int>, val guarded: Boolean = false)

/** Фонтан, как его читает предложение: какой, доля здоровья и маны и доля ёмкости зарядов фляг, что он даёт. */
data class FountainView(val id: Int, val heal: Double, val flasks: Double)

/**
 * Призыв стража: [price] - цена сейчас (`SummonLog.price` правил по району захода), [spent] - заход исчерпал
 * свои [perRun] призывов. Хватает ли золота, решает счёт кнопки.
 */
data class SummonView(val price: Long, val spent: Boolean, val perRun: Int)

/** A crystal of essences as its sheet shows it, what a Vaal orb on it did, and whether that orb's outcome is still the server's to tell. */
data class CrystalView(
    val id: Int,
    val essences: List<String>,
    val guardian: MonsterCode,
    val stronger: Boolean,
    val vaal: Boolean,
    val outcome: String? = null,
    val awaiting: Boolean = false,
)

/** A rise from level [from] to [to]: one screen even for several levels at once. */
data class LevelUp(val from: Int, val to: Int) {
    val levels: Int get() = to - from
}

/**
 * Действие героя у объекта карты - одна нижняя панель «действие у объекта» для всех: объект [feature] и сделанная доля [progress]
 * (0..1) - простой у трещины тайной комнаты или сбор узла ремесла; null доли - делать нечего, только подсказка (дверь заперта, нужен
 * рычаг).
 */
data class ObjectAction(val feature: MapFeature, val progress: Float?)

/**
 * Раздел окна карты ([ExpeditionRun.windowLines]): строки самой карты, скарабеи, влияние, атлас, древо гильдии, зелье; [title] - ключ
 * заголовка, [empty] - ключ строки пустого раздела (null - пустой не показывается).
 */
enum class MapSection(val title: String, val empty: String? = null) {
    MAP("map.modifiers", "map.no_modifiers"),
    SCARAB("map.scarabs"),
    INFLUENCE("map.influence"),
    ATLAS("map.atlas"),
    GUILD("map.guild"),
    POTION("map.potion"),
    ;

    companion object {
        /** Раздел доли с источником [kind]: гильдия и зелье - свои, остаток атласа - атлас. */
        fun of(kind: SourceKind): MapSection = when (kind) {
            SourceKind.GUILD -> GUILD
            SourceKind.POTION -> POTION
            else -> ATLAS
        }

        /** Раздел строк карты-предмета источника [source]. */
        fun of(source: EffectSource): MapSection = when (source) {
            EffectSource.MAP -> MAP
            EffectSource.ATLAS -> ATLAS
            EffectSource.SCARAB -> SCARAB
            EffectSource.INFLUENCE -> INFLUENCE
        }
    }
}
