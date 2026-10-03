package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.GuildState
import com.sperance.exileforge.presentation.state.MarketState
import com.sperance.exileforge.presentation.state.PlayState
import com.sperance.exileforge.presentation.state.QuestState
import com.sperance.exileforge.presentation.state.TAB_HERO
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The hero menu: the one place a hero is chosen, made or given up. Everything below the gate acts on `heroId`. */
class CharacterViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    /** The account's heroes; [autoEnter] skips the menu for a single hero straight after a sign-in. */
    suspend fun readCharacters(autoEnter: Boolean = false) {
        with(runtime) {
            val owner = sessions.state.value.profile?.id.orEmpty()
            val characters = if (owner.isBlank()) emptyList() else api.hero.heroesOf(owner)
            sessions.update { it.copy(characters = characters, charactersRead = true) }
            if (autoEnter) characters.singleOrNull()?.let { only -> entered(only.id) }
        }
    }

    fun refresh() {
        with(runtime) { read(Reads.CHARACTERS) { readCharacters() } }
    }

    fun enter(id: String) {
        with(runtime) { task(touches = setOf(Reads.HERO)) { entered(id) } }
    }

    private suspend fun entered(id: String) {
        with(runtime) {
            heroes.select(id)
            boards.clear()
            expeditions.clear()
            navigator.reset(com.sperance.exileforge.presentation.nav.Route.Hero)
            mutable.update { it.copy(play = PlayState(heroId = id, draftClass = it.play.draftClass)) }
            heroSync.forget()
            // The hero the next launch opens straight into (3.30.0).
            store.saveLastHero(sessions.state.value.server, id)
            ensureContent(fresh = true)
            heroSync.readHero()
            expedition.resume(id)
        }
    }

    /** Back to the menu — the only way to swap heroes. Everything the old hero owned is dropped. */
    fun leaveGame() {
        with(runtime) {
            if (state.value.busy) return
            cancelReads()
            expedition.drop()
            trial.drop()
            crafts.drop()
            heroes.clear()
            boards.clear()
            markets.clear()
            guilds.clear()
            navigator.reset(com.sperance.exileforge.presentation.nav.Route.Characters)
            mutable.update {
                it.copy(
                    play = PlayState(draftClass = it.play.draftClass),
                    market = MarketState(),
                    guild = GuildState(),
                    quests = QuestState(),
                )
            }
            read(Reads.CHARACTERS) { readCharacters() }
            scope.launch { store.saveLastHero(sessions.state.value.server, null) }
        }
    }

    /** Create a hero and start playing it; the class is chosen here and nowhere else. */
    fun create(name: String, heroClass: String) {
        with(runtime) {
            task(writing = true, touches = setOf(Reads.CHARACTERS, Reads.HERO)) {
                require(name.isNotBlank()) { ui("character.enter_name") }
                require(heroClass.isNotBlank()) { ui("character.choose_class") }
                val owner = sessions.state.value.profile?.id.orEmpty()
                check(owner.isNotBlank()) { ui("catalog.sign_in") }
                check(state.value.characterSlotsLeft > 0) { ui("character.limit", sessions.state.value.characters.size) }
                val created = api.hero.create(owner, name, "", heroClass)
                readCharacters()
                entered(created.id)
            }
        }
    }

    fun delete(id: String) {
        with(runtime) {
            task(writing = true, touches = setOf(Reads.CHARACTERS)) {
                api.hero.delete(id)
                val remaining = sessions.state.value.characters.filterNot { character -> character.id == id }
                sessions.update { it.copy(characters = remaining) }
            }
        }
    }

    /** The classes the creation form offers come with the content. */
    fun ensureClasses() {
        with(runtime) { read(Reads.CONTENT) { ensureContent() } }
    }
}
