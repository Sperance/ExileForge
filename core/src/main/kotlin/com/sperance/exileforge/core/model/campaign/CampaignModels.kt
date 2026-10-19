package com.sperance.exileforge.core.model.campaign

import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.SummonLog
import com.sperance.exileforge.rules.content.TrialEventKind
import com.sperance.exileforge.rules.content.TrialProgress
import com.sperance.exileforge.rules.content.TrialRun
import com.sperance.exileforge.rules.roll.AbyssRun
import com.sperance.exileforge.rules.roll.AbyssWindow
import com.sperance.exileforge.rules.roll.ActiveMap
import com.sperance.exileforge.rules.roll.ChestWindow
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.CrystalWindow
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.ChestStock
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.run.RunEventKind
import com.sperance.exileforge.rules.run.RunTally
import kotlinx.serialization.Serializable

/**
 * The hero's campaign as the server keeps it: zones passed, the windows of chests, crystals and cracks
 * per zone, when each boss returns (epoch millis), the map entered with, the Vaal zone rolled, the
 * descent under way and the open [run].
 */
@Serializable data class CampaignState(
    val cleared: List<MapCode> = emptyList(),
    val chests: Map<String, ChestWindow> = emptyMap(),
    val bosses: Map<String, Long> = emptyMap(),
    val crystals: Map<String, CrystalWindow> = emptyMap(),
    val abyss: Map<String, AbyssWindow> = emptyMap(),
    val activeMap: ActiveMap? = null,
    val vaalZone: VaalZone? = null,
    val abyssRun: AbyssRun? = null,
    val recipeRolled: Boolean = false,
    val corruptionOpened: Boolean = false,
    val run: RunState? = null,
    /** The trials: the one open, the tower's record, the rush's best times and cleared regions. */
    val trials: TrialProgress = TrialProgress(),
    /** How many times each zone was passed — its guardian slain, by zone code. */
    val completions: Map<String, Int> = emptyMap(),
    /** Призывы стража героем в окне: от них растёт цена следующего ([SummonLog.price]). */
    val summons: SummonLog = SummonLog(),
    /** Когда герой последний раз получил новое семя захода, мс эпохи сервера: следующее - по `RunRules.seedReadyAt`. */
    val seededAt: Long = 0,
) {
    /** The boss of [mapCode] is slain and not yet back. */
    fun bossDown(mapCode: MapCode, now: Long): Boolean = (bosses[mapCode.value] ?: 0L) > now
}

/** The open run as the server holds it: the seed and the frozen context, and how far the journal was applied. */
@Serializable data class RunState(
    val id: String,
    val seed: Long,
    val zone: String,
    val context: RunContext,
    val startedAt: Long = 0,
    val applied: Int = 0,
    val killed: List<Int> = emptyList(),
    val vaalKilled: List<Int> = emptyList(),
    /** Счёт наград захода (и призывов стража - их не больше `services.summonsPerRun`). */
    val tally: RunTally = RunTally(),
)

/** Which zones the hero has passed — slain their boss — and which are open to them. */
@Serializable data class CampaignProgress(val cleared: List<MapCode> = emptyList(), val unlocked: List<MapCode> = emptyList())

/** What a batch of events brought, as the server counted it. */
@Serializable data class RewardView(
    val experience: Double = 0.0,
    val gold: Long = 0,
    val items: Map<String, Long> = emptyMap(),
    val equipment: List<ItemInstance> = emptyList(),
    val recipe: String? = null,
) {
    /** The same reward as the rules count it. */
    fun toReward(): Reward = Reward(experience, gold, items, equipment, recipe)
}

/**
 * What one accepted event [n] of the journal brought: the server alone rolls rewards, the client shows them.
 * [crystal] is what an event that changes a crystal rather than takes it (a Vaal orb) made of it; null otherwise.
 * [rank]: a hero's first win over a guardian - how many heroes had won it before, counting this one.
 */
@Serializable data class EventReward(
    val n: Int,
    val kind: RunEventKind,
    val reward: RewardView = RewardView(),
    val crystal: CrystalOutcome? = null,
    val rank: Long? = null,
    /** Ключи очков Атласа `<вид>:<карта>`, впервые заработанные событием: о новом очке - без чтения героя. */
    val atlas: List<String> = emptyList(),
    /** Весь запас сундуков захода после события, сменившего сделки алтарей: неоткрытые сундуки перекатаны; у прочих событий null. */
    val chestStock: ChestStock? = null,
)

/** A crystal after an event: its [index] in the zone's window, as the event named it, and the [crystal] it became. */
@Serializable data class CrystalOutcome(val index: Int = 0, val crystal: Crystal)

/**
 * The server's answer to a batch of run events: how far the journal is applied now, the numbers it
 * refused, what the batch brought and cost, where the hero stands, whether the run is still open, and
 * where the items went.
 */
@Serializable data class RunReport(
    val applied: Int = 0,
    val rejected: List<Int> = emptyList(),
    val reward: RewardView = RewardView(),
    val lost: Double = 0.0,
    val level: Int = 1,
    val experience: Double = 0.0,
    val money: Long = 0,
    val progress: CampaignProgress = CampaignProgress(),
    val open: Boolean = true,
    /** Where the batch's items went: the stash, its overflow, or sold for gold past both. */
    val received: Received = Received(),
    /** What each accepted event of the batch brought, by ascending number: the only source of the run's loot. */
    val rewards: List<EventReward> = emptyList(),
    /** The run the report is about. */
    val runId: String = "",
    /** Задания, сданные сервером при конце захода: ответ, что закрыл заход; null - заход открыт или сдача не прошла. */
    val turnIns: com.sperance.exileforge.rules.content.QuestClaimAll? = null,
    /** Задания гильдии после той сдачи; null - вне гильдии или заход открыт. */
    val guildQuests: com.sperance.exileforge.rules.content.GuildQuests? = null,
)

/** Продажа торговцу сразу при получении: [count] вещей за [gold] золота и [shards] осколков. */
@Serializable data class Sale(val count: Int = 0, val gold: Long = 0, val shards: List<com.sperance.exileforge.rules.content.ShardYield> = emptyList())

/**
 * Куда легли пришедшие герою вещи: в тайник, в переполнение или торговцу.
 *
 * @property overflowSold вещи, проданные торговцу из-за полных тайника и переполнения, с выручкой только с них.
 * @property autoSold добыча, проданная фильтром автопродажи героя сразу: вне захода - при любом числе свободных мест, в заходе - не влезшая в тайник.
 */
@Serializable data class Received(
    val stashed: Int = 0,
    val overflowed: Int = 0,
    val overflowSold: Sale = Sale(),
    val autoSold: Sale = Sale(),
)

/** A trial entered: the trial itself and the hero's context frozen on entry, the monsters stood up by it. */
@Serializable data class TrialStart(val run: TrialRun, val context: RunContext)

/** Ответ на действие в Разломе: доска после него, итог конца забега и что из сундука не влезло. */
@Serializable data class RiftReport(val board: com.sperance.exileforge.rules.rift.RiftBoard, val result: com.sperance.exileforge.rules.rift.RiftResult? = null, val received: Received = Received())

/** What one accepted event of a trial brought; [atlas] - ключи очков Атласа, впервые заработанные событием: рубеж башни, первая зачистка раша. */
@Serializable data class TrialReward(val n: Int, val kind: TrialEventKind, val reward: RewardView = RewardView(), val atlas: List<String> = emptyList())

/** The server's answer to a trial's events: how far applied, what refused, what each brought, the hero's trials and where the hero stands. */
@Serializable data class TrialReport(
    val applied: Int = 0,
    val rejected: List<Int> = emptyList(),
    val rewards: List<TrialReward> = emptyList(),
    val trials: TrialProgress = TrialProgress(),
    val level: Int = 1,
    val experience: Double = 0.0,
    val money: Long = 0,
    val received: Received = Received(),
    /** The trial the report is about. */
    val runId: String = "",
)
