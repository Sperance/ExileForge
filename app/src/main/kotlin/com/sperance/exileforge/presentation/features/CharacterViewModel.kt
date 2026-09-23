package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.characterDocument
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.MAX_CHARACTERS
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject

/**
 * The character menu: the one place a character is chosen, made or given up.
 *
 * Everything below the gate acts on `characterId`, so nothing else may write it. That is the whole
 * point of the menu — a tab cannot quietly move the player onto a different hero halfway through.
 */
class CharacterViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    /**
     * The account's characters, and where the player lands after reading them.
     *
     * [autoEnter] is the whole difference between arriving and coming back. Straight after a
     * sign-in a single character is not a decision, so the menu is skipped; but the menu is also
     * where a player goes *to leave* that character, and entering them again there would make the
     * screen unreachable for anyone who owns exactly one — which is most people.
     */
    suspend fun readCharacters(autoEnter: Boolean = false) { with(runtime) {
        val owner = state.value.account.profile?.id.orEmpty()
        val characters = if (owner.isBlank()) emptyList() else api.hero.charactersOf(owner)
        mutable.update { it.copy(account = it.account.copy(characters = characters, charactersRead = true)) }
        if (autoEnter) characters.singleOrNull()?.let { only -> entered(only.id) }
    } }

    fun refresh() { with(runtime) { read(Reads.CHARACTERS) { readCharacters() } } }

    /** Enter the game as one character. Every screen below reads `characterId` and nothing else. */
    fun enter(id: String) { with(runtime) { task(touches = setOf(Reads.HERO, Reads.CATALOG)) { entered(id) } } }

    /**
     * The same step from inside a running action, because `task` refuses to nest.
     *
     * The hero is read here rather than left to the screen: the tabs open on a character that is
     * already loaded, which is what "every button is bound to the chosen one" has to mean.
     */
    private suspend fun entered(id: String) { with(runtime) {
        mutable.update { it.copy(phase = AppPhase.GAME, tab = 0, play = it.play.copy(characterId = id, hero = null, characterOwner = "", selectedEquipment = "", forgeLine = ""), world = it.world.copy(inventoryBases = emptyMap())) }
        heroViewModel.readHero()
        // Tab 0 is the catalogue, and this is where it becomes the open one.
        loadPage(0)
    } }

    /**
     * Back to the menu — the only way to swap characters.
     *
     * Everything the old character owned is dropped rather than carried across: an inventory, a
     * showcase page or a tree left on screen would belong to somebody else.
     */
    fun leaveGame() { with(runtime) {
        if (state.value.busy) return
        // What was on its way belonged to the character being left.
        cancelReads()
        mutable.update { it.copy(phase = AppPhase.CHARACTERS, play = it.play.copy(characterId = "", characterOwner = "", hero = null, selectedEquipment = "", forgeLine = "", selectedNode = "", nodeQuery = ""), world = it.world.copy(inventoryBases = emptyMap()), market = it.market.copy(myLots = emptyList(), showcase = com.sperance.exileforge.core.model.auction.AuctionPage(), tab = 0, locked = null), admin = it.admin.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap()))) }
        read(Reads.CHARACTERS) { readCharacters() }
    } }

    /**
     * Create a character and start playing it.
     *
     * The class is a creation field with no update route — the server has no way to move a
     * character between classes — so this form is the only place it is ever chosen.
     */
    fun create(name: String, classId: String) { with(runtime) { task(writing = true, touches = setOf(Reads.CHARACTERS, Reads.HERO, Reads.CATALOG)) {
        require(name.isNotBlank()) { ui("character.enter_name") }
        require(classId.isNotBlank()) { ui("character.choose_class") }
        val owner = state.value.account.profile?.id.orEmpty()
        check(owner.isNotBlank()) { ui("catalog.sign_in") }
        // The limit is the server's (CH_005); this only keeps the form honest about it.
        check(state.value.account.characters.size < MAX_CHARACTERS) {
            ui("character.limit", MAX_CHARACTERS)
        }
        val created = api.catalog.create(Catalog.CHARACTERS, characterDocument(owner, name, classId))
        mutable.update { it.copy(message = ui("character.created", name.trim())) }
        readCharacters()
        entered(created.entityId)
    } } }

    /** Giving a character up frees one of the account's slots; the server owns what that costs. */
    fun delete(id: String) { with(runtime) { task(writing = true, touches = setOf(Reads.CHARACTERS)) {
        api.catalog.delete(Catalog.CHARACTERS, id)
        mutable.update { it.copy(message = ui("character.deleted")) }
        val remaining = state.value.account.characters.filterNot { character -> character.id == id }
        // Re-reading would enter the last survivor, and a deletion is not a choice to play them.
        mutable.update { it.copy(account = it.account.copy(characters = remaining)) }
    } } }

    /** The classes the creation form offers; they are seeded and fixed for a session. */
    fun ensureClasses() { with(runtime) { read(Reads.PROGRESSION) { ensureProgression() } } }
}
