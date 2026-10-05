package com.sperance.exileforge.presentation.app

import com.sperance.exileforge.core.i18n.Phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.refusalLine
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.Actions
import com.sperance.exileforge.presentation.Repositories
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.MAX_CHARACTERS
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.world.WorldLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The hero menu: the one place a hero is chosen, made or given up. Everything below the gate acts on `heroId`. */
class CharacterActions(
    repositories: Repositories,
    actions: Actions,
    commands: CommandRunner,
    connection: ServerConnection,
    store: ServerStore,
    scope: CoroutineScope,
    private val navigator: Navigator,
    private val loader: WorldLoader,
    private val lazyWarmup: Lazy<WarmupActions>,
    private val trace: StartupTrace,
) : AppService(repositories, actions, commands, connection, store, scope) {
    private val warmupActions: WarmupActions get() = lazyWarmup.value

    /** The account's heroes; [autoEnter] skips the menu for a single hero straight after a sign-in. */
    suspend fun readCharacters(autoEnter: Boolean = false) {
        run {
            val owner = sessions.state.value.profile?.id.orEmpty()
            val characters = if (owner.isBlank()) emptyList() else api.hero.heroesOf(owner)
            sessions.update { it.copy(characters = characters, charactersRead = true) }
            if (autoEnter) characters.singleOrNull()?.let { only -> entered(only.id) }
        }
    }

    fun refresh() {
        run { read(Reads.CHARACTERS) { readCharacters() } }
    }

    fun enter(id: String) {
        run { task(touches = setOf(Reads.HERO)) { entered(id) } }
    }

    private suspend fun entered(id: String) {
        run {
            heroes.select(id)
            boards.clear()
            expeditions.clear()
            navigator.reset(com.sperance.exileforge.presentation.nav.Route.Hero)
            warmupActions.clear()
            heroSync.forget()
            // The hero the next launch opens straight into (3.30.0).
            store.saveLastHero(sessions.state.value.server, id)
            loader.ensureContent(fresh = true)
            trace.step(StartStage.SESSION, "start.step.hero") { heroSync.readHero() }
            expedition.resume(id)
        }
    }

    /** Back to the menu — the only way to swap heroes. Everything the old hero owned is dropped. */
    fun leaveGame() {
        run {
            if (commands.state.value.busy) return
            cancelReads()
            expedition.drop()
            trial.drop()
            crafts.drop()
            heroes.clear()
            boards.clear()
            markets.clear()
            guilds.clear()
            navigator.reset(com.sperance.exileforge.presentation.nav.Route.Characters)
            warmupActions.clear()
            read(Reads.CHARACTERS) { readCharacters() }
            scope.launch { store.saveLastHero(sessions.state.value.server, null) }
        }
    }

    /**
     * Сервер отказал активному герою как заблокированному (`CH_034`, 3.88.0): когда команда, встретившая отказ, кончится,
     * игрок уходит к выбору героя, и там - причина словами сервера. Отказ про другого героя ничего не меняет.
     */
    fun blocked(heroId: String, failure: ApiFailure) {
        scope.launch {
            commands.state.first { !it.busy }
            if (heroes.heroId != heroId) return@launch
            leaveGame()
            commands.refuse(Phrase { refusalLine(failure) })
        }
    }

    /** Create a hero and start playing it; the class is chosen here and nowhere else. */
    fun create(name: String, heroClass: String) {
        run {
            task(writing = true, touches = setOf(Reads.CHARACTERS, Reads.HERO)) {
                require(name.isNotBlank()) { ui("character.enter_name") }
                require(heroClass.isNotBlank()) { ui("character.choose_class") }
                val owner = sessions.state.value.profile?.id.orEmpty()
                check(owner.isNotBlank()) { ui("catalog.sign_in") }
                check(characterSlotsLeft() > 0) { ui("character.limit", sessions.state.value.characters.size) }
                val created = api.hero.create(owner, name, "", heroClass)
                readCharacters()
                entered(created.id)
            }
        }
    }

    fun delete(id: String) {
        run {
            task(writing = true, touches = setOf(Reads.CHARACTERS)) {
                api.hero.delete(id)
                val remaining = sessions.state.value.characters.filterNot { character -> character.id == id }
                sessions.update { it.copy(characters = remaining) }
            }
        }
    }

    /** The classes the creation form offers come with the content. */
    fun ensureClasses() {
        run { read(Reads.CONTENT) { loader.ensureContent() } }
    }

    /** Сколько героев ещё можно создать: по правилам контента, пока их не прочли - по умолчанию. */
    private fun characterSlotsLeft(): Int = ((world.state.value.content?.rules?.maxCharacters ?: MAX_CHARACTERS) - sessions.state.value.characters.size).coerceAtLeast(0)
}
