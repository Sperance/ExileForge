package com.sperance.exileforge.presentation.expedition

import com.sperance.exileforge.core.campaign.RiftArena
import com.sperance.exileforge.core.campaign.RiftStyles
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.receivedText
import com.sperance.exileforge.core.display.shardsPerOrb
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftBoard
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftResult
import com.sperance.exileforge.rules.rift.RiftStage
import com.sperance.exileforge.rules.run.RunContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Разлом на экране (3.96.0): доска недели с забегом, бой текущего узла (арена) и итог последнего забега.
 * [open] - экран Разлома открыт.
 */
data class RiftState(
    val open: Boolean = false,
    val board: RiftBoard? = null,
    val arena: RiftArena? = null,
    val result: RiftResult? = null,
)

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
) {
    private val api: GameApi get() = connection.api
    private val mutable = MutableStateFlow(RiftState())
    val state: StateFlow<RiftState> = mutable.asStateFlow()

    private val speedSteps: Int get() = GameSettings.SPEEDS.indexOf(prefs.settings.value.fightSpeed).coerceAtLeast(0)

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

    /** Шаг забега [act]: ответ - новая доска, а у конца - итог и сундук. */
    fun act(act: RiftAct) {
        val run = mutable.value.board?.progress?.run ?: return
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val report = api.trials.riftAct(heroes.heroId, run.id, act)
            report.received.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { notices.toast(receivedText(it, world.state.value.content.shardsPerOrb)) }
            report.result?.let { result -> mutable.update { it.copy(result = result) } }
            show(report.board)
            if (report.result != null) heroSync.readHero()
        }
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
            else -> arena(board)
        }
        mutable.update { it.copy(board = board, arena = arena) }
    }

    private fun arena(board: RiftBoard): RiftArena? {
        val index = world.state.value.content ?: return null
        val rules: RiftRules = index.campaign.trials?.rift ?: return null
        val hero = heroes.state.value.hero ?: return null
        val gear = expedition.gear() ?: return null
        val run = board.progress.run ?: return null
        val fight = board.fight ?: return null
        val engine = RiftEngine(rules, index.campaign, board.plan, index.rules.fight)
        return RiftArena(index, rules, engine, run, fight, RunContext(hero.heroClass, hero.level), gear, hero.pets.pet(hero.pets.combat)) { end -> act(end) }
            .also { a -> repeat(speedSteps) { a.send(RunCommand.Speed) } }
    }
}
