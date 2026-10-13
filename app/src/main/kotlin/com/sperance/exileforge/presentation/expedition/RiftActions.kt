package com.sperance.exileforge.presentation.expedition

import com.sperance.exileforge.core.campaign.RiftArena
import com.sperance.exileforge.core.campaign.RiftStyles
import com.sperance.exileforge.core.campaign.run.FightReport
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.receivedText
import com.sperance.exileforge.core.display.shardsPerOrb
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.campaign.RiftReport
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.speedPresses
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftBoard
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftResult
import com.sperance.exileforge.rules.rift.RiftStage
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

/**
 * Разлом на экране (3.96.0): доска недели с забегом, бой текущего узла (арена) и итог последнего забега.
 * [open] - экран Разлома открыт; [delivery] - итог кончившегося боя в пути на сервер (4.7.0); [lastFight] - журнал последнего
 * боя узла (4.8.0, «Журнал боя»).
 */
data class RiftState(
    val open: Boolean = false,
    val board: RiftBoard? = null,
    val arena: RiftArena? = null,
    val result: RiftResult? = null,
    val delivery: RiftDelivery? = null,
    val lastFight: FightReport? = null,
)

/** Итог боя Разлома в пути (4.7.0): уходит, пока сервер не ответит, под одним ключом - дважды он не ляжет. */
sealed interface RiftDelivery {
    /** Отправка; [attempt] - какая по счёту (с 2 - нет связи, повтор). */
    data class Sending(val attempt: Int) : RiftDelivery

    /** Сервер итог не принял: [reason] - его слова; забег стоит на бою, «К карте» перечитывает доску. */
    data class Refused(val reason: String) : RiftDelivery
}

/**
 * Разлом недели (3.96.0, сервер 1.83.0): доска с сервера, каждый шаг забега - действие [RiftAct], которое сервер применяет
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
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api
    private val mutable = MutableStateFlow(RiftState())
    val state: StateFlow<RiftState> = mutable.asStateFlow()

    private val speedSteps: Int get() = speedPresses(world.state.value.content, prefs.settings.value.fightSpeed)

    /** «Разлом недели»: экран открывается, доска читается. */
    fun open() {
        mutable.value = RiftState(open = true)
        refresh()
    }

    /** Доска заново - и бой, если забег стоит на бою. */
    fun refresh() {
        commands.task(touches = setOf(Reads.HERO)) { show(api.trials.riftBoard(heroes.heroId)) }
    }

    /** Экран закрыт: бой посреди не бросается - забег ждёт на сервере, вернуться можно в любой момент недели. */
    fun close() {
        mutable.value.arena?.takeIf { it.hud.value.phase == com.sperance.exileforge.core.campaign.TrialPhase.FIGHT }?.let { return }
        mutable.value = RiftState()
        heroSync.ensure()
    }

    /** Новый забег: стиль героя - по его умениям и листу. */
    fun start() {
        val gear = expedition.gear() ?: return
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            show(api.trials.riftStart(heroes.heroId, RiftStyles.of(gear)))
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
        }
    }

    /** Шаг забега [act] по нажатию игрока: ответ - новая доска, а у конца - итог и сундук. */
    fun act(act: RiftAct) {
        val run = mutable.value.board?.progress?.run ?: return
        commands.task(writing = true, touches = setOf(Reads.HERO)) { land(api.trials.riftAct(heroes.heroId, run.id, act)) }
    }

    /**
     * Итог боя (4.7.0): не через раннер команд - тот отказывает, пока занят, и итог пропадал, а арена ждала вечно. Итог уходит
     * сам, под одним ключом, с повтором при сбое сети; отказ сервера - на экран, с выходом к карте.
     */
    private fun deliver(end: RiftAct.Fight) {
        val run = mutable.value.board?.progress?.run ?: return
        val heroId = heroes.heroId
        val key = UUID.randomUUID().toString()
        // Журнал боя (4.8.0): арена уже сложила его, итог уходит
        mutable.update { it.copy(lastFight = it.arena?.report ?: it.lastFight) }
        sending?.cancel()
        sending = scope.launch {
            var attempt = 1
            while (true) {
                mutable.update { it.copy(delivery = RiftDelivery.Sending(attempt)) }
                try {
                    val report = api.trials.riftAct(heroId, run.id, end, key)
                    mutable.update { it.copy(delivery = null) }
                    land(report)
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiFailure) {
                    if (!CommandQueue.transient(e.status)) {
                        mutable.update { it.copy(delivery = RiftDelivery.Refused(e.message.orEmpty())) }
                        return@launch
                    }
                } catch (_: Exception) {
                    // Сеть: тот же ключ позже
                }
                delay(RETRY_FIRST shl (attempt - 1).coerceAtMost(RETRY_DOUBLINGS))
                attempt++
            }
        }
    }

    /** «К карте» после отказа: арена снимается, доска перечитывается - сервер скажет, где стоит забег. */
    fun leaveRefused() {
        mutable.update { it.copy(arena = null, delivery = null) }
        refresh()
    }

    /** Ответ на шаг: тосты переполнения, итог забега, новая доска; герой перечитывается, только если ответ его не принёс. */
    private suspend fun land(report: RiftReport) {
        report.received.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { notices.toast(receivedText(it, world.state.value.content.shardsPerOrb)) }
        report.result?.let { result -> mutable.update { it.copy(result = result) } }
        show(report.board)
        // Итог забега, как и всякий ответ со снимком, уже освежил героя; перечитывается лишь заведомо устаревший
        if (report.result != null && heroes.state.value.readAt == 0L) heroSync.readHero()
    }

    /** Итог прочитан. */
    fun dismissResult() = mutable.update { it.copy(result = null) }

    fun send(command: RunCommand) {
        mutable.value.arena?.send(command)
    }

    /** Доска легла: стоит бой - встаёт его арена (одна на узел), нет - арены нет. */
    private fun show(board: RiftBoard) {
        val fight = board.fight
        val run = board.progress.run
        val current = mutable.value.arena
        val arena = when {
            fight == null || run == null || run.stage != RiftStage.FIGHT -> null

            current != null && current.hud.value.phase == com.sperance.exileforge.core.campaign.TrialPhase.FIGHT -> current

            // Итог ещё в пути: доска до него стоит на том же бою, арена с итогом остаётся
            current != null && mutable.value.delivery != null -> current

            else -> arena(board)
        }
        mutable.update { it.copy(board = board, arena = arena) }
    }

    private var sending: Job? = null

    /** Счёт Предначертания забега (4.6.0) по id забега: арены одного забега делят «Второе дыхание». */
    private var fateRun: Pair<String, com.sperance.exileforge.core.campaign.combat.FateRun>? = null

    private fun arena(board: RiftBoard): RiftArena? {
        val index = world.state.value.content ?: return null
        val rules: RiftRules = index.campaign.trials?.rift ?: return null
        val hero = heroes.state.value.hero ?: return null
        val gear = expedition.gear() ?: return null
        val run = board.progress.run ?: return null
        val fight = board.fight ?: return null
        val engine = RiftEngine(rules, index.campaign, board.plan, index.rules.fight)
        val fate = fateRun?.takeIf { it.first == run.id }?.second ?: com.sperance.exileforge.core.campaign.combat.FateRun().also { fateRun = run.id to it }
        return RiftArena(index, rules, engine, run, fight, RunContext(hero.heroClass, hero.level), gear, hero.pets.pet(hero.pets.combat), fate) { end -> deliver(end) }
            .also { a -> repeat(speedSteps) { a.send(RunCommand.Speed) } }
    }

    private companion object {
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 3
    }
}
