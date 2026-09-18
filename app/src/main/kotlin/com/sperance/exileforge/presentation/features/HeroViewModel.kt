package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.command.ItemStack
import com.sperance.exileforge.core.model.command.UseRecipeCommand
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class HeroViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun characterId(value: String) { with(runtime) {
        if (state.value.busy) return
        mutable.update { it.copy(characterId = value, hero = null, characterOwner = "", inventoryBases = emptyMap(), selectedEquipment = "") }
    } }

    fun selectEquipment(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(selectedEquipment = value) } } }

    fun showCharacterInventory(id: String) { with(runtime) { task {
        mutable.update { it.copy(tab = 4, characterId = id, hero = null, characterOwner = "", selectedEquipment = "") }
        readHero()
    } } }

    fun loadHero() { with(runtime) { task { readHero() } } }

    fun equip(instanceId: String) { with(runtime) { characterCommand { id -> api.equip(id, instanceId) } } }
    fun unequip(instanceId: String) { with(runtime) { characterCommand { id -> api.unequip(id, instanceId) } } }

    /** Admin only: hand the character a named template, rolled by the server. */
    fun grant(equipmentId: String) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { tr("Выдача предметов доступна администратору", "Granting items is available to administrators") }
        api.grant(id, equipmentId)
    } } }

    fun grantRarity(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(grantRarity = value) } } }
    fun grantSlot(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(grantSlot = value) } } }

    /**
     * Admin only: a random template of the chosen rarity and category, with server-rolled modifiers.
     *
     * The client picks the base and nothing else — prefixes, suffixes, tiers and values all come
     * back from `itemToInventory`.
     */
    fun grantRandom() { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { tr("Выдача предметов доступна администратору", "Granting items is available to administrators") }
        val template = api.randomTemplate(state.value.grantRarity, state.value.grantSlot)
        api.grant(id, template.entityId)
        mutable.update { it.copy(message = tr("Выпало: ${template.text("name")}", "Rolled: ${template.text("name")}")) }
    } } }

    fun adjustItems(itemId: String, amount: Long) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { tr("Изменение сумки доступно администратору", "Changing the bag is available to administrators") }
        api.adjustItems(id, listOf(ItemStack(itemId, amount)))
    } } }

    fun redeem(code: String) { with(runtime) { characterCommand { id -> api.redeem(id, code) } } }

    fun useRecipe(recipeId: String, ingredients: List<String>, amount: Long) { with(runtime) { characterCommand { id ->
        api.useRecipe(id, recipeId, UseRecipeCommand(ingredients, amount))
    } } }

    /**
     * Every character command is a write the server may have applied even when the answer is lost,
     * so the hero is always re-read afterwards rather than patched from the response.
     */
    private fun characterCommand(block: suspend (String) -> Unit) { with(runtime) { task(writing = true) {
        val id = state.value.characterId.trim()
        check(id.isNotBlank()) { tr("Выберите персонажа", "Choose a character") }
        check(state.value.ownsCharacter || state.value.isAdmin) { tr("Операция доступна владельцу персонажа", "The operation is available to the character's owner") }
        block(id)
        readHero()
        mutable.update { it.copy(message = it.message ?: tr("Изменения сохранены", "Changes saved")) }
    } } }

    private suspend fun readHero() { with(runtime) {
        val id = state.value.characterId.trim()
        check(id.isNotBlank()) { tr("Выберите персонажа", "Choose a character") }
        ensureDefinitions()
        val character = api.character(id)
        val view = HeroView(character, api.inventory(id), api.stats(id), api.bag(id))
        mutable.update { it.copy(hero = view, characterOwner = character.userId,
            selectedEquipment = it.selectedEquipment.takeIf { chosen -> view.inventory.any { item -> item.id == chosen } }
                ?: view.inventory.firstOrNull()?.id.orEmpty()) }
        loadTemplates()
    } }

    /**
     * An instance stores only its rolls, so the shared template carries the name, slot and picture.
     * Templates are fetched in the background: a card falls back to its emblem until one arrives.
     */
    private fun loadTemplates() { with(runtime) {
        metadataJob?.cancel()
        val snapshot = state.value
        val currentApi = api
        metadataJob = scope.launch {
            val limit = Semaphore(4)
            snapshot.hero?.inventory.orEmpty().map { it.equipmentId }.distinct().filter { it !in snapshot.inventoryBases }.map { id ->
                async {
                    limit.withPermit {
                        try {
                            val base = currentApi.get(Catalog.EQUIPMENT, id) ?: return@withPermit
                            mutable.update { if (it.server == snapshot.server && it.characterId == snapshot.characterId) it.copy(inventoryBases = it.inventoryBases + (id to base)) else it }
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) { /* The emblem and the id stay on screen; a refresh retries. */ }
                    }
                }
            }.awaitAll()
        }
    } }
}
