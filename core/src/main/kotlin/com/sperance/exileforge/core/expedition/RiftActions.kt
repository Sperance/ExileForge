package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.campaign.RiftArena
import com.sperance.exileforge.core.campaign.RiftStyles
import com.sperance.exileforge.core.campaign.run.FightReport
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.receivedText
import com.sperance.exileforge.core.display.shardsPerOrb
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.hero.HeroSync
import com.sperance.exileforge.core.i18n.Phrase
import com.sperance.exileforge.core.model.campaign.RiftReport
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.refusalLine
import com.sperance.exileforge.core.quests.StoryOverlay
import com.sperance.exileforge.core.session.CallState
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.PatientCall
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.rules.content.MonsterCode
import com.sperance.exileforge.rules.content.RiftMetal
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftBoard
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftOffer
import com.sperance.exileforge.rules.rift.RiftResult
import com.sperance.exileforge.rules.rift.RiftRun
import com.sperance.exileforge.rules.rift.RiftStage
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.RunContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Разлом на экране: доска недели с забегом, бой текущего узла (арена) и итог последнего забега.
 * [open] - экран Разлома открыт; [delivery] - итог кончившегося боя в пути на сервер; [lastFight] - журнал последнего
 * боя узла («Журнал боя»); [toll] - итог последнего выигранного боя для шапки; [coffers] - что лежало в открытых сундуках Владыки
 * по номеру сундука (только пока забег стоит у сундуков); [chart] - карта доски: что можно выбрать и почему нельзя; [walk] - шаг героя
 * к выбранному узлу, пока доска с ответом не легла; [recap] - итог последнего выигранного боя (лист итога), до следующего боя.
 */
data class RiftState(
    val open: Boolean = false,
    val board: RiftBoard? = null,
    val chart: RiftChart? = null,
    val walk: RiftWalk? = null,
    val arena: RiftArena? = null,
    val result: RiftResult? = null,
    val delivery: RiftDelivery? = null,
    val lastFight: FightReport? = null,
    val toll: RiftToll? = null,
    val coffers: Map<Int, Reward> = emptyMap(),
    val recap: RiftRecap? = null,
)

/**
 * Итог выигранного боя Разлома: узел вида [kind] акта [act], у стража - сам страж [guardian] ([final] - Владыка), бой [fight] (разбор и
 * журнал), Честь и взятый ключ боя [toll] из предела [honorMax], ключи к сундукам по металлам [keys] (после Владыки - сколько их ждёт),
 * счёт забега до [scoreBefore] и после [scoreAfter]. [shown] - лист открыт: он открывается сам после принятого сервером итога и
 * закрывается только кнопкой, закрытый ждёт плашкой «Итог боя» на карте до следующего боя.
 */
data class RiftRecap(
    val kind: RiftNodeKind,
    val guardian: MonsterCode?,
    val act: Int,
    val final: Boolean,
    val fight: FightReport,
    val toll: RiftToll,
    val honorMax: Double,
    val keys: Map<RiftMetal, Int>,
    val scoreBefore: Long,
    val scoreAfter: Long,
    val shown: Boolean = true,
) {
    /** Очки, что принёс бой. */
    val points: Long get() = (scoreAfter - scoreBefore).coerceAtLeast(0)
}

/** Итог выигранного боя узла для шапки забега: Честь до [before] и после [after], взятый ключ [key]. */
data class RiftToll(val before: Double, val after: Double, val key: RiftMetal?) {
    /** Сколько Чести снял бой, в целых, как её показывает шапка. */
    val lost: Int get() = (before.roundToInt() - after.roundToInt()).coerceAtLeast(0)

    companion object {
        /** Итог боя по забегу до [before] и после [after]: взятый ключ - металл, которого стало больше. */
        fun of(before: RiftRun, after: RiftRun): RiftToll = RiftToll(before.honor, after.honor, after.keys.entries.firstOrNull { (metal, count) -> count > (before.keys[metal] ?: 0) }?.key)
    }
}

/** Итог боя Разлома в пути: уходит, пока сервер не ответит, под одним ключом - дважды он не ляжет. */
sealed interface RiftDelivery {
    /** Отправка с [since] (мс стены, конец боя); [attempt] - какая по счёту (с 2 - нет связи, повтор). */
    data class Sending(val attempt: Int, val since: Long) : RiftDelivery

    /** Сервер итог не принял: [reason] - его слова; забег стоит на бою, «К карте» перечитывает доску. */
    data class Refused(val reason: String) : RiftDelivery
}

/**
 * Разлом недели: доска с сервера, каждый шаг забега - действие [RiftAct], которое сервер применяет
 * правилами и отвечает новой доской. Бой узла считает [RiftArena] здесь; его итог уходит действием сам. Один на приложение.
 */
class RiftActions(
    private val expedition: ExpeditionActions,
    private val heroes: HeroRepository,
    private val heroSync: HeroSync,
    private val world: WorldRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val prefs: PreferencesRepository,
    /** Окно сюжета поверх игры: уход в заход его закрывает - окно считается показанным. */
    private val story: StoryOverlay,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api
    private val mutable = MutableStateFlow(RiftState())
    val state: StateFlow<RiftState> = mutable.asStateFlow()

    /**
     * Шаг забега в пути ([act], [go]): уходит под одним ключом, сам повторяется при сбое связи, после попыток ждёт «Повторить»
     * ([retryStep]); прошёл после сбоя - доска перечитывается. Отказ сервера - строка отказа и свежая доска: сервер мог принять
     * прежний шаг, чей ответ потерялся.
     */
    private val steps = PatientCall(scope)
    val step: StateFlow<CallState> = steps.state

    /** «Разлом недели»: экран открывается, доска читается. */
    fun open() {
        story.clear()
        steps.drop()
        mutable.value = RiftState(open = true)
        refresh()
    }

    /**
     * Вход в героя: открытый забег Разлома открывает Разлом сразу - закрытое приложение и обрыв связи забег не кончают, кончает его
     * только «Покинуть Разлом».
     */
    fun resume() {
        if (heroes.state.value.hero?.campaign?.trials?.rift?.run != null && !mutable.value.open) open()
    }

    /** Доска заново - и бой, если забег стоит на бою. */
    fun refresh() {
        commands.task(touches = setOf(Reads.HERO)) { show(api.trials.riftBoard(heroes.heroId)) }
    }

    /**
     * Экран закрыт - только без забега (Врата): с открытым забегом экран не закрывается, выход из него - «Покинуть Разлом»
     * ([RiftAct.End]).
     */
    fun close() {
        if (mutable.value.board?.progress?.run != null) return
        steps.drop()
        mutable.value = RiftState()
        heroSync.ensure()
    }

    /** Новый забег шагом [steps] (ключ Разлома не тратится дважды): стиль героя - по его умениям и листу. */
    fun start() {
        val gear = expedition.gear() ?: return
        val heroId = heroes.heroId
        val styles = RiftStyles.of(gear)
        sendStep(START) { key ->
            show(api.trials.riftStart(heroId, styles, key))
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
        }
    }

    /**
     * «Покинуть Разлом»: забег кончается ([RiftAct.End]) и счёт остаётся. Шаг, что висит в полёте или ждёт повтора, бросается - выход его
     * не ждёт (сервер мог его и принять, конец забега идёт после него); выход сам повторяется, как шаг, и без связи ждёт «Повторить».
     */
    fun leave() {
        val run = mutable.value.board?.progress?.run ?: return
        val heroId = heroes.heroId
        mutable.update { it.copy(walk = null) }
        steps.supersede(request(RiftAct.End) { key -> land(api.trials.riftAct(heroId, run.id, RiftAct.End, key)) })
    }

    /** Шаг забега [act] по нажатию игрока: ответ - новая доска, у сундука Владыки - его содержимое, а у конца - итог и сундук. */
    fun act(act: RiftAct) {
        val run = mutable.value.board?.progress?.run ?: return
        val heroId = heroes.heroId
        sendStep(act) { key ->
            val report = api.trials.riftAct(heroId, run.id, act, key)
            val opened = (act as? RiftAct.Open)?.coffer
            val coffer = report.coffer
            if (opened != null && coffer != null) mutable.update { it.copy(coffers = it.coffers + (opened to coffer)) }
            land(report)
        }
    }

    /** «Повторить» после «Нет связи»: потерянный шаг уходит снова тем же ключом. */
    fun retryStep() = steps.retry()

    /** Шаг [tag] через [steps]; пока идёт команда раннера (чтение доски, вход), шаг не начинается. */
    private fun sendStep(tag: Any?, send: suspend (String) -> Unit): Boolean {
        if (commands.busy) return false
        return steps.send(request(tag, send))
    }

    /** Шаг Разлома [tag]: отказ - строка отказа и свежая доска, прошёл после сбоя - свежая доска. */
    private fun request(tag: Any?, send: suspend (String) -> Unit) = PatientCall.Request(tag, refused = ::refused, restored = ::refresh, send = send)

    /** Сервер отказал шагу: строка отказа, доска - заново. */
    private fun refused(failure: ApiFailure) {
        commands.refuse(Phrase { refusalLine(failure) })
        refresh()
    }

    /**
     * Шаг на узел [node], выбранный на карте: только если карта его пускает ([RiftSpot.bar]). С анимациями герой сразу идёт по тропе
     * ([RiftState.walk]), доска с ответом ложится, когда шаг дойдёт ([RiftWalk.STEP_MS]); без анимаций шага нет - герой встаёт на узел с
     * доской ответа, без ожидания. Отказ или сбой - герой остаётся на месте.
     */
    fun go(node: Int) {
        val state = mutable.value
        val chart = state.chart ?: return
        val run = state.board?.progress?.run ?: return
        if (chart.spot(node)?.let { it.bar == null } != true) return
        val started = TimeSource.Monotonic.markNow()
        val walk = RiftWalk(chart.here?.id, node).takeIf { prefs.settings.value.animations }
        walk?.let { step -> mutable.update { it.copy(walk = step) } }
        val heroId = heroes.heroId
        val sent = sendStep(RiftAct.Move(node)) { key ->
            try {
                val report = api.trials.riftAct(heroId, run.id, RiftAct.Move(node), key)
                if (walk != null) delay(RiftWalk.STEP_MS.milliseconds - started.elapsedNow())
                land(report)
            } finally {
                mutable.update { it.copy(walk = null) }
            }
        }
        if (!sent) mutable.update { it.copy(walk = null) }
    }

    /** Итог боя в шапке прочитан. */
    fun dismissToll() = mutable.update { it.copy(toll = null) }

    /** Лист итога боя снова открыт с карты. */
    fun openRecap() = mutable.update { state -> state.copy(recap = state.recap?.copy(shown = true)) }

    /** Лист итога боя закрыт кнопкой: итог ждёт плашкой на карте до следующего боя. */
    fun closeRecap() = mutable.update { state -> state.copy(recap = state.recap?.copy(shown = false)) }

    /**
     * Итог боя: не через раннер команд - тот отказывает, пока занят, и итог пропадал, а арена ждала вечно. Итог уходит
     * сам, под одним ключом, с повтором при сбое сети; отказ сервера - на экран, с выходом к карте.
     *
     * Журнал боя арена уже сложила - итог уходит; сеть - тот же ключ позже.
     */
    private fun deliver(end: RiftAct.Fight) {
        val run = mutable.value.board?.progress?.run ?: return
        val heroId = heroes.heroId
        val key = UUID.randomUUID().toString()
        mutable.update { it.copy(lastFight = it.arena?.report ?: it.lastFight) }
        sending?.cancel()
        sending = scope.launch {
            val since = System.currentTimeMillis()
            var attempt = 1
            while (true) {
                mutable.update { it.copy(delivery = RiftDelivery.Sending(attempt, since)) }
                try {
                    val report = api.trials.riftAct(heroId, run.id, end, key)
                    val after = report.board.progress.run?.takeIf { it.id == run.id && end.won }
                    val recap = recap(run, end, report)
                    mutable.update { it.copy(delivery = null, toll = after?.let { next -> RiftToll.of(run, next) }, recap = recap ?: it.recap) }
                    land(report)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiFailure) {
                    if (!CommandQueue.transient(e.status)) {
                        mutable.update { it.copy(delivery = RiftDelivery.Refused(refusalLine(e))) }
                        return@launch
                    }
                } catch (_: Exception) {
                }
                delay(RETRY_FIRST shl (attempt - 1).coerceAtMost(RETRY_DOUBLINGS))
                attempt++
            }
        }
    }

    /**
     * Итог выигранного боя [end] забега [before], принятый ответом [report]: бой - журнал арены, Честь, ключ и счёт - до и после
     * (кончился ли забег с ним - из итога забега). Проигрыш, нет журнала, контента или забега после боя - null.
     */
    private fun recap(before: RiftRun, end: RiftAct.Fight, report: RiftReport): RiftRecap? {
        val board = mutable.value.board ?: return null
        val node = before.at?.let(board.plan::node)?.takeIf { end.won } ?: return null
        val guardian = node.kind == RiftNodeKind.GUARDIAN
        val fight = mutable.value.lastFight ?: return null
        val rules = world.state.value.content?.campaign?.trials?.rift ?: return null
        val engine = engine(board) ?: return null
        val after = report.board.progress.run?.takeIf { it.id == before.id }
        val result = report.result
        val toll = after?.let { RiftToll.of(before, it) } ?: result?.let { RiftToll(before.honor, it.honor.toDouble(), null) } ?: return null
        return RiftRecap(
            kind = node.kind,
            guardian = rules.guardian(node.act).takeIf { guardian },
            act = node.act,
            final = guardian && rules.isFinal(node.act),
            fight = fight,
            toll = toll,
            honorMax = engine.maxHonor(after ?: before),
            keys = after?.keys.orEmpty(),
            scoreBefore = engine.score(before),
            scoreAfter = after?.let(engine::score) ?: result?.score ?: return null,
        )
    }

    /** «К карте» после отказа: арена снимается, доска перечитывается - сервер скажет, где стоит забег. */
    fun leaveRefused() {
        mutable.update { it.copy(arena = null, delivery = null) }
        refresh()
    }

    /**
     * Ответ на шаг: тосты переполнения, итог забега, новая доска; герой перечитывается, только если ответ его не принёс.
     *
     * Итог забега, как всякий ответ со снимком, уже освежил героя; перечитывается лишь заведомо устаревший.
     */
    private suspend fun land(report: RiftReport) {
        receivedText(report.received, world.state.value.content.shardsPerOrb)?.let { notices.toast(it) }
        report.result?.let { result -> mutable.update { it.copy(result = result) } }
        show(report.board)
        if (report.result != null && heroes.state.value.readAt == 0L) heroSync.readHero()
    }

    /** Итог прочитан. */
    fun dismissResult() = mutable.update { it.copy(result = null) }

    fun send(command: RunCommand) {
        mutable.value.arena?.send(command)
    }

    /**
     * Доска легла: стоит бой - встаёт его арена (одна на узел), нет - арены нет.
     *
     * Итог ещё в пути - доска стоит на том же бою, арена с итогом остаётся. Новая арена - следующий бой: итог прошлого стража ([RiftState.recap])
     * снимается.
     */
    private fun show(board: RiftBoard) {
        val fight = board.fight
        val run = board.progress.run
        val current = mutable.value.arena
        val arena = when {
            fight == null || run == null || run.stage != RiftStage.FIGHT -> null
            current != null && current.hud.value.phase == com.sperance.exileforge.core.campaign.TrialPhase.FIGHT -> current
            current != null && mutable.value.delivery != null -> current
            else -> arena(board)
        }
        val coffers = run?.offer is RiftOffer.Coffers
        val chart = engine(board)?.let { RiftChart.of(it, run) }
        val fresh = arena != null && arena !== current
        mutable.update { it.copy(board = board, chart = chart, arena = arena, coffers = if (coffers) it.coffers else emptyMap(), recap = if (fresh) null else it.recap) }
    }

    private var sending: Job? = null

    /** Счёт Предначертания забега по id забега: арены одного забега делят «Второе дыхание». */
    private var fateRun: Pair<String, com.sperance.exileforge.core.campaign.combat.FateRun>? = null

    /** Правила схемы доски [board]; без загруженного контента - null. */
    private fun engine(board: RiftBoard): RiftEngine? {
        val index = world.state.value.content ?: return null
        val rules = index.campaign.trials?.rift ?: return null
        return RiftEngine(rules, index.campaign, board.plan, index.rules.fight)
    }

    private fun arena(board: RiftBoard): RiftArena? {
        val index = world.state.value.content ?: return null
        val rules: RiftRules = index.campaign.trials?.rift ?: return null
        val hero = heroes.state.value.hero ?: return null
        val gear = expedition.gear() ?: return null
        val run = board.progress.run ?: return null
        val fight = board.fight ?: return null
        val engine = engine(board) ?: return null
        val fate = fateRun?.takeIf { it.first == run.id }?.second ?: com.sperance.exileforge.core.campaign.combat.FateRun().also { fateRun = run.id to it }
        return RiftArena(index, rules, engine, run, fight, RunContext(hero.heroClass, hero.level), gear, fate, prefs.fightSpeed(index.rules.fight)) { end -> deliver(end) }
    }

    companion object {
        /** Метка шага «Войти» на Вратах: по ней крутится спиннер кнопки входа. */
        const val START = "rift.start"

        private const val RETRY_FIRST = 2_000L
        private const val RETRY_DOUBLINGS = 3
    }
}
