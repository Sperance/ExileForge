package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.AbyssDepth
import com.sperance.exileforge.core.campaign.ChargeView
import com.sperance.exileforge.core.campaign.DeathHit
import com.sperance.exileforge.core.campaign.MapEnd
import com.sperance.exileforge.core.campaign.MapTally
import com.sperance.exileforge.core.campaign.RunSummary
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
import com.sperance.exileforge.core.campaign.combat.RiftGuard
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.SkillView
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.MapFeature
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

/** Where a run stands: walking, fighting, at a kill's loot, at a Vaal portal's gate, at a crystal, at a crack of the Abyss, or over one way or another. */
enum class RunPhase { MAP, FIGHT, LOOT, GATE, CRYSTAL, ABYSS, DEAD, CLEARED, LEFT, CHALLENGE }

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
    /** The combat pet's line (3.70.0): it struck, healed, or was struck — its card pulses with it. */
    val pet: Boolean = false,
)

/** The blow on screen right now, for the cards to act out. */
data class LungeView(
    val actor: Side,
    val action: Action,
    val kind: HitKind,
    val landed: Boolean,
    val progress: Float,
    val foe: Int = 0,
    /** The combat pet's blow, or a blow at it (3.70.0): its card acts it out, not the hero's. */
    val pet: Boolean = false,
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
    /** Its buildups (3.78.0), null while the rules have none. */
    val buildup: BuildupView? = null,
    /** Барьер (3.93.0): что ещё поглотит барьер фазы босса. */
    val barrier: Int = 0,
) {
    /** Its card is on the field: it fights there, or fell there. */
    val onField: Boolean get() = place >= 0
}

/**
 * Ярость боя (3.95.0 - в полосе стража; 4.3.0 - любого боя): [stage] ступеней уже прошло, урон врагов больше на [damage]
 * процентов, до следующей ступени [next] секунд; null - ступеней больше не будет.
 */
data class RageView(val stage: Int, val damage: Double, val next: Double?)

/**
 * Босс в бою (3.92.0): [index] - его место в стае, [phase] - шаблон фаз, [marks] - пороги шагов в процентах здоровья и
 * [passed] - какие уже сработали, [cast] - умение, что он готовит, и через сколько секунд из скольких.
 */
data class BossHud(
    val index: Int,
    val phase: String?,
    val marks: List<Double>,
    val passed: List<Boolean>,
    val cast: CastView?,
    /** Слоты вокруг босса (3.93.0): по порядку, null - пустой; первая половина слева, вторая справа. */
    val slots: List<SlotView?> = emptyList(),
    /** Страж Разлома (3.96.0): кто он; null - обычный босс, поля ниже пусты. */
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
    /** Босс Скверны (4.0.0, Матерь): осквернён - его шаги фаз рвут пуповины «Сердца Скверны». */
    val tainted: Boolean = false,
) {
    /** Сработавших шагов фаз - у Матери столько пуповин уже порвано (портрет и полоса гасят их по одной). */
    val torn: Int get() = passed.count { it }

    /** Пуповин «Сердца Скверны»: по одной на каждый порог фазы и последняя - на смерть. */
    val cords: Int get() = marks.size + 1
}

/** Кто в слоте вокруг босса (3.93.0): приспешник свиты или тотем. */
sealed interface SlotView {
    /** Приспешник: [index] - его место в стае. */
    data class Minion(val index: Int) : SlotView

    /** Тотем [code] рода [kind]: [serial] - его номер в бою, [left] секунд из [total]; [element] - стихия удара. */
    data class Totem(val serial: Int, val code: String, val kind: com.sperance.exileforge.rules.content.TotemKind, val left: Double, val total: Double, val element: String?) : SlotView {
        val share: Float get() = if (total <= 0) 0f else (left / total).toFloat().coerceIn(0f, 1f)
    }
}

/**
 * Применение умения героя на экране (4.4.0, «Жилы энергии»): умение [code] вида [type], его стихия [element] (null - без стихии),
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

/** Умение врага на подходе (3.92.0): [left] секунд до готовности из [total]. */
data class CastView(val code: String, val left: Double, val total: Double) {
    val progress: Float get() = if (total <= 0) 1f else (1 - left / total).toFloat().coerceIn(0f, 1f)
}

/** The fight as the overlay prints it: the pack as cards, the hero's pools and states, what just landed, and the blows so far, newest first. */

/**
 * A fighter's buildups as its card shows them (3.78.0): the fullest bar and its share, every bar's share by [Buildup] order
 * for its window, and which went off and holds it now.
 */
data class BuildupView(val leading: Buildup?, val share: Float, val bars: List<Float>, val stunned: Boolean, val frozen: Boolean, val electrocuted: Boolean)

/** The pet in a fight as its bar shows it (3.5.0). */
data class AllyView(val species: String, val life: Int, val maxLife: Int, val alive: Boolean)

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
    /** The hero's buildups (3.78.0). */
    val heroBuildup: BuildupView? = null,
    val lunge: LungeView? = null,
    val events: List<CombatEvent> = emptyList(),
    val started: Boolean = true,
    val paused: Boolean = false,
    val target: Int? = null,
    val focus: Int? = null,
    /** The pet beside the hero (3.5.0). */
    val ally: AllyView? = null,
    val heroTaunt: Boolean = false,
    /** [heroMaxMana] is the mana the auras leave free; [heroReserved] what they hold past it. */
    val heroMana: Int = 0,
    val heroMaxMana: Int = 0,
    val heroReserved: Int = 0,
    val skills: List<SkillView?> = emptyList(),
    val flasks: List<FlaskView?> = emptyList(),
    val heroEffects: List<EffectView> = emptyList(),
    /** The hero's frenzy, power and endurance charges (3.33.0), a counter per kind held. */
    val heroCharges: List<ChargeView> = emptyList(),
    val heroBarrier: Int = 0,
    /** The level the foes stand at: a depth of the Abyss stands deeper than its zone. */
    val level: Int = 0,
    /** Бой цепочки (4.2.0), с 1, из [rounds]: босс раша, бой этажа Башни, волна Разлома; у одиночного боя - 1 из 1. */
    val round: Int = 1,
    val rounds: Int = 1,
    /** Лист героя сейчас (3.92.0): по нему лист умения врага считает, сколько оно снимет. */
    val heroBody: com.sperance.exileforge.core.campaign.combat.Combatant? = null,
    /** Босс боя (3.92.0): его фазы и что он готовит; null - босса нет. */
    val boss: BossHud? = null,
    /** Ярость боя (4.3.0): ступень, прибавка урона врагов и отсчёт до следующей; null - бой кончен. */
    val rage: RageView? = null,
    /** Последнее применение умения героя (4.4.0), пока его сцена на экране; null - сцены нет. */
    val heroCast: HeroCastView? = null,
) {
    val scouting: Boolean get() = outcome == null && (!started || paused)

    /** The foes on the field, by their places. */
    val field: List<FoeView> get() = foes.filter { it.onField }.sortedBy { it.place }

    /** How many of the whole pack are still to be beaten. */
    val standing: Int get() = foes.count { it.alive }
}

/** One member of a pack fought and its own log. */
data class PackHit(
    val monster: RolledMonster,
    val events: List<CombatEvent>,
    val duration: Double,
    /** Когда начался его бой (3.91.1), секунд от начала шага испытания (этажа башни): время событий боя считается с нуля. */
    val start: Double = 0.0,
)

/** A fight that is over, as the screen after it reads it. */
data class FightReport(val monster: RolledMonster, val outcome: Outcome, val pack: List<PackHit>, val duration: Double) {
    val events: List<CombatEvent> get() = pack.flatMap { it.events }
    val packSize: Int get() = pack.size
    private fun mine() = events.filter { it.actor == Side.HERO }
    private fun theirs() = events.filter { it.actor == Side.MONSTER && !it.atPet }
    val dealt: Int get() = mine().sumOf { it.damage }.roundToInt()
    val taken: Int get() = theirs().sumOf { it.damage }.roundToInt()

    /** What the combat pet took (3.70.0), apart from the hero's [taken]. */
    val petTaken: Int get() = events.filter { it.atPet }.sumOf { it.damage }.roundToInt()
    val crits: Int get() = mine().count { it.kind == HitKind.CRIT }
    val blocked: Int get() = mine().count { it.kind == HitKind.BLOCKED }
    val inflicted: List<Ailment> get() = mine().flatMap { it.inflicted }.distinct()
}

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
     * What the fight just won brought, as the server's answers bring it (server 1.30.0): nothing is rolled here,
     * and [rewardAwaiting] of its events are still to be answered — the report opens at once and fills in.
     */
    val reward: Reward? = null,
    val rewardAwaiting: Int = 0,
    /** A first win over this guardian (3.81.0): the hero's place among all who beat it. */
    val rank: Long? = null,
    /** The levels the answered experience raised the hero by and the report has not told of yet (3.81.0). */
    val levelUp: LevelUp? = null,
    val slain: RolledMonster? = null,
    val report: FightReport? = null,
    /** What the death cost, by the rules' price until the server's answer replaces it. */
    val fall: Double? = null,
    /** The run's gold and experience as the server's answers granted them; [awaiting] of its rewarding events are not answered yet. */
    val gold: Long = 0,
    val experience: Double = 0.0,
    val kills: Int = 0,
    /** The run's figures (3.47.0) and the blows that ended it. */
    val summary: RunSummary = RunSummary(),
    val recap: List<DeathHit> = emptyList(),
    val awaiting: Int = 0,
    val chestsLeft: Int = 0,
    val chest: Reward? = null,
    val chestAwaiting: Boolean = false,
    val fountainsLeft: Int = 0,
    /** The fountain offered (3.70.0): the map waits for the player's word — drink, or step away and leave it standing. */
    val fountain: FountainView? = null,
    /** The Vaal zone behind the portal the hero stands at. */
    val gate: VaalZone? = null,
    /** This run is a Vaal zone: no way out but its guardian or a death, and a death is not the map's end. */
    val vaal: Boolean = false,
    /** [heroMaxMana] is the mana the auras leave free; [heroReserved] what they hold past it. */
    val heroMana: Int = 0,
    val heroMaxMana: Int = 0,
    val heroReserved: Int = 0,
    val flasks: List<FlaskView?> = emptyList(),
    val crystal: CrystalView? = null,
    val crystalsLeft: Int = 0,
    val abyss: AbyssView? = null,
    val cracksLeft: Int = 0,
    /** The guardian is slain and not yet back. */
    val bossDown: Boolean = false,
    /** Секунды до возвращения отдыхающего стража (3.95.2); null - он не отдыхает: стоит или повержен в этом заходе. */
    val guardianRest: Int? = null,
    /** The autorun under way, and what it has gathered (3.2.0). */
    val auto: AutoHud? = null,
    val autoReward: Reward? = null,
    val autoAwaiting: Int = 0,
    /** Счёт боевых заданий за этот заход (3.95.0): счётчик летописи - сколько прибавит. */
    val questTally: Map<String, Long> = emptyMap(),
    /** Events of the journal the server has not taken yet, the number of the oldest of them, and the ones it refused. */
    val pending: Int = 0,
    val applied: Int = 0,
    val rejected: Int = 0,
    /** What the map came to so far: its summary before the camp. */
    val tally: MapTally = MapTally(),
    /** Лист объекта карты (3.90.0): алтарь, торговец или узел, к которому подошёл герой. */
    val feature: FeatureView? = null,
    /** Последний удар ловушки (3.90.0), пока он висит на полосе карты. */
    /** Числа урона карты над героем (3.95.3): удары ловушек и тики их эффектов. */
    val hazards: List<HazardView> = emptyList(),
    /** Эффекты ловушек на герое (3.95.3): горение, яд - для чипа под полосой жизни. */
    val afflictions: List<AfflictionView> = emptyList(),
    /** Доля простоя у трещины тайной комнаты (3.90.0); null - герой не стоит у неё. */
    val opening: Float? = null,
    /** Экран-вызов перед стражем (3.92.0); null - его нет. */
    val challenge: ChallengeView? = null,
    /** Очаг Скверны карты (4.0.0); null - очага нет. */
    val blight: BlightView? = null,
) {
    /**
     * Итог ведёт обратно на карту под зоной (3.95.2): зона Ваал, из которой вышли живыми. Гибель в зоне заканчивает весь заход -
     * по фазе этого не понять: отчёт о гибели уже закрыт («Дальше» переводит DEAD в LEFT), а конец захода остаётся `FELL`.
     */
    val returnsToMap: Boolean get() = vaal && tally.end != MapEnd.FELL
}

/** Страж на экране-вызове (3.92.0): его ролл, уровень, полное здоровье, шаблон фаз с порогами; [vaal] - страж Ваал-зоны. */
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
 * Объект карты, чей лист открыт (3.90.0, сервер 1.81.3): объект правил - что в нём, - выборы, уже сделанные на нём, и доля
 * идущего сбора узла (null - сбор не идёт).
 */
data class FeatureView(val feature: MapFeature, val taken: List<Int>, val gathering: Float? = null)

/** A fountain as its offer reads: which one, and the share of life and mana it gives back. */
data class FountainView(val id: Int, val heal: Double)

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

/** A rise from level [from] to [to] (3.81.0): one screen even for several levels at once. */
data class LevelUp(val from: Int, val to: Int) {
    val levels: Int get() = to - from
}
