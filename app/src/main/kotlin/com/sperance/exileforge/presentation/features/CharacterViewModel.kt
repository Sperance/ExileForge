package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppPhase
import com.sperance.exileforge.presentation.state.MAX_CHARACTERS
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
     * Nothing to choose between is not a choice: an empty account opens the creation form, and a
     * single character is entered directly. The menu appears when it has a decision to offer.
     */
    suspend fun readCharacters() { with(runtime) {
        val owner = state.value.profile?.id.orEmpty()
        val characters = if (owner.isBlank()) emptyList() else api.charactersOf(owner)
        mutable.update { it.copy(characters = characters, charactersRead = true) }
        characters.singleOrNull()?.let { only -> entered(only.id) }
    } }

    fun refresh() { with(runtime) { task { readCharacters() } } }

    /** Enter the game as one character. Every screen below reads `characterId` and nothing else. */
    fun enter(id: String) { with(runtime) { task { entered(id) } } }

    /**
     * The same step from inside a running action, because `task` refuses to nest.
     *
     * The hero is read here rather than left to the screen: the tabs open on a character that is
     * already loaded, which is what "every button is bound to the chosen one" has to mean.
     */
    private suspend fun entered(id: String) { with(runtime) {
        mutable.update { it.copy(phase = AppPhase.GAME, characterId = id, tab = 0,
            hero = null, characterOwner = "", inventoryBases = emptyMap(), selectedEquipment = "") }
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
        mutable.update { it.copy(phase = AppPhase.CHARACTERS, characterId = "", characterOwner = "",
            hero = null, inventoryBases = emptyMap(), selectedEquipment = "", selectedNode = "", nodeQuery = "",
            myLots = emptyList(), showcase = com.sperance.exileforge.core.model.auction.AuctionPage(),
            auctionTab = 0, auctionLocked = null, editorOpen = false, original = null, draft = JsonObject(emptyMap())) }
        task { readCharacters() }
    } }

    /**
     * Create a character and start playing it.
     *
     * The class is a creation field with no update route — the server has no way to move a
     * character between classes — so this form is the only place it is ever chosen.
     */
    fun create(name: String, classId: String) { with(runtime) { task(writing = true) {
        require(name.isNotBlank()) { tr("Введите имя персонажа", "Enter the character's name") }
        require(classId.isNotBlank()) { tr("Выберите класс", "Choose a class") }
        val owner = state.value.profile?.id.orEmpty()
        check(owner.isNotBlank()) { tr("Войдите в аккаунт", "Sign in to your account") }
        // The limit is the server's (CH_005); this only keeps the form honest about it.
        check(state.value.characters.size < MAX_CHARACTERS) {
            tr("Больше $MAX_CHARACTERS персонажей аккаунт не держит", "An account holds no more than $MAX_CHARACTERS characters")
        }
        val document = JsonObject(template(Catalog.CHARACTERS) + mapOf(
            "userId" to JsonPrimitive(owner), "name" to JsonPrimitive(name.trim()), "classId" to JsonPrimitive(classId)))
        val created = api.create(Catalog.CHARACTERS, document)
        mutable.update { it.copy(message = tr("Персонаж создан: ${name.trim()}", "Character created: ${name.trim()}")) }
        readCharacters()
        entered(created.entityId)
    } } }

    /** Giving a character up frees one of the account's slots; the server owns what that costs. */
    fun delete(id: String) { with(runtime) { task(writing = true) {
        api.delete(Catalog.CHARACTERS, id)
        mutable.update { it.copy(message = tr("Персонаж удалён", "The character was deleted")) }
        val remaining = state.value.characters.filterNot { character -> character.id == id }
        // Re-reading would enter the last survivor, and a deletion is not a choice to play them.
        mutable.update { it.copy(characters = remaining) }
    } } }

    /** The classes the creation form offers; they are seeded and fixed for a session. */
    fun ensureClasses() { with(runtime) { task { ensureProgression() } } }
}
