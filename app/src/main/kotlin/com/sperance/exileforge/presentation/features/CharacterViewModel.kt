package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.GuildState
import com.sperance.exileforge.presentation.state.MarketState
import com.sperance.exileforge.presentation.state.PlayState
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.TAB_HERO
import kotlinx.coroutines.flow.update

/** The hero menu: the one place a hero is chosen, made or given up. Everything below the gate acts on `heroId`. */
class CharacterViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    /** The account's heroes; [autoEnter] skips the menu for a single hero straight after a sign-in. */
    suspend fun readCharacters(autoEnter: Boolean = false) { with(runtime) {
        val owner = state.value.account.profile?.id.orEmpty()
        val characters = if (owner.isBlank()) emptyList() else api.hero.heroesOf(owner)
        mutable.update { it.copy(account = it.account.copy(characters = characters, charactersRead = true)) }
        if (autoEnter) characters.singleOrNull()?.let { only -> entered(only.id) }
    } }

    fun refresh() { with(runtime) { read(Reads.CHARACTERS) { readCharacters() } } }

    fun enter(id: String) { with(runtime) { task(touches = setOf(Reads.HERO)) { entered(id) } } }

    private suspend fun entered(id: String) { with(runtime) {
        mutable.update { it.copy(phase = AppPhase.GAME, tab = TAB_HERO, play = PlayState(heroId = id, draftClass = it.play.draftClass, selectedOrb = it.play.selectedOrb)) }
        heroViewModel.forget()
        ensureContent(fresh = true)
        heroViewModel.readHero()
        expeditionViewModel.resume(id)
    } }

    /** Back to the menu — the only way to swap heroes. Everything the old hero owned is dropped. */
    fun leaveGame() { with(runtime) {
        if (state.value.busy) return
        cancelReads()
        expeditionViewModel.drop()
        craftsViewModel.drop()
        mutable.update { it.copy(phase = AppPhase.CHARACTERS, play = PlayState(draftClass = it.play.draftClass, selectedOrb = it.play.selectedOrb), market = MarketState(),
            building = null, guild = GuildState()) }
        read(Reads.CHARACTERS) { readCharacters() }
    } }

    /** Create a hero and start playing it; the class is chosen here and nowhere else. */
    fun create(name: String, heroClass: String) { with(runtime) { task(writing = true, touches = setOf(Reads.CHARACTERS, Reads.HERO)) {
        require(name.isNotBlank()) { ui("character.enter_name") }
        require(heroClass.isNotBlank()) { ui("character.choose_class") }
        val owner = state.value.account.profile?.id.orEmpty()
        check(owner.isNotBlank()) { ui("catalog.sign_in") }
        check(state.value.characterSlotsLeft > 0) { ui("character.limit", state.value.account.characters.size) }
        val created = api.hero.create(owner, name, "", heroClass)
        readCharacters()
        entered(created.id)
    } } }

    fun delete(id: String) { with(runtime) { task(writing = true, touches = setOf(Reads.CHARACTERS)) {
        api.hero.delete(id)
        val remaining = state.value.account.characters.filterNot { character -> character.id == id }
        mutable.update { it.copy(account = it.account.copy(characters = remaining)) }
    } } }

    /** The classes the creation form offers come with the content. */
    fun ensureClasses() { with(runtime) { read(Reads.CONTENT) { ensureContent() } } }
}
