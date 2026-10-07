package com.sperance.exileforge.presentation.app

import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.Actions
import com.sperance.exileforge.presentation.Repositories
import kotlinx.coroutines.CoroutineScope

/**
 * Общее у сервисов приложения (3.80.44, вместо `ForgeRuntime` и `FeatureViewModel`): репозитории и действия игры,
 * команды, текущий сервер, хранилище устройства и область приложения. Сервис пишет репозитории; экран читает их
 * через свою модель.
 */
abstract class AppService(
    protected val repositories: Repositories,
    protected val actions: Actions,
    protected val commands: CommandRunner,
    protected val connection: ServerConnection,
    protected val store: ServerStore,
    protected val scope: CoroutineScope,
) {
    protected val sessions get() = repositories.sessions
    protected val world get() = repositories.world
    protected val heroes get() = repositories.heroes
    protected val boards get() = repositories.boards
    protected val markets get() = repositories.markets
    protected val guilds get() = repositories.guilds
    protected val feedbacks get() = repositories.feedbacks
    protected val expeditions get() = repositories.expeditions
    protected val languages get() = repositories.languages
    protected val links get() = repositories.links
    protected val admins get() = repositories.admins
    protected val modes get() = repositories.modes
    protected val quests get() = actions.quests
    protected val market get() = actions.market
    protected val guild get() = actions.guild
    protected val crafts get() = actions.crafts
    protected val hero get() = actions.hero
    protected val heroSync get() = actions.heroSync
    protected val expedition get() = actions.expedition
    protected val trial get() = actions.trial

    /** Текущий сервер: заменяется при смене адреса и при входе. */
    protected var api: GameApi
        get() = connection.api
        set(value) = connection.set(value)

    /** Герой на экране, как его называет каждый маршрут героя. */
    protected val heroId: String get() = heroes.heroId

    /** Ответ про другого героя не рисуется. */
    protected fun onScreen(id: String): Boolean = heroes.onScreen(id)

    protected fun task(writing: Boolean = false, touches: Set<String> = emptySet(), block: suspend () -> Unit) = commands.task(writing, touches, block)
    protected fun read(key: String, restart: Boolean = false, silent: Boolean = false, block: suspend () -> Unit) = commands.read(key, restart, silent, block = block)
    protected fun cancelReads() = commands.cancelReads()
}
