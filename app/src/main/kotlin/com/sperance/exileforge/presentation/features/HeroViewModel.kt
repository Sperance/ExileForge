package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.command.ItemStack
import com.sperance.exileforge.core.model.command.UseRecipeCommand
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.ForgeRuntime
import kotlinx.coroutines.flow.update

class HeroViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun selectEquipment(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(selectedEquipment = value) } } }

    fun loadHero() { with(runtime) { task { readHero() } } }

    /**
     * The hero, if what is on screen has gone cold.
     *
     * Reading it whole is five requests, so a tab does not ask for one every time it is opened. A
     * command already re-reads what it changed; this is for everything that changed the character
     * somewhere else — a trade, an administrator, the same account on another device — where the
     * client has no way to be told. [FRESH_FOR] is how long a reading is trusted without asking.
     */
    fun ensureHero() { with(runtime) {
        val now = System.currentTimeMillis()
        if (state.value.characterId.isBlank()) return
        if (state.value.hero != null && now - state.value.heroReadAt < FRESH_FOR) return
        task { readHero() }
    } }

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
        val name = equipmentTitle(template)
        mutable.update { it.copy(message = tr("Выпало: $name", "Rolled: $name")) }
    } } }

    fun adjustItems(itemId: String, amount: Long) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { tr("Изменение сумки доступно администратору", "Changing the bag is available to administrators") }
        api.adjustItems(id, listOf(ItemStack(itemId, amount)))
    } } }

    fun selectOrb(value: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(selectedOrb = value) } } }

    /**
     * Spends one orb on one item of the inventory.
     *
     * Whether the orb applies at all, what it rerolls and what it leaves alone is the server's rule;
     * the client only names the pair and prints the sentence that comes back.
     */
    fun applyOrb(inventoryId: String, orbItemId: String) { with(runtime) { characterCommand { id ->
        val outcome = api.applyOrb(id, inventoryId, orbItemId)
        mutable.update { it.copy(selectedEquipment = outcome.created?.id ?: outcome.item.id, message = outcome.message) }
    } } }

    fun selectNode(code: String) { with(runtime) { if (!state.value.busy) mutable.update { it.copy(selectedNode = code) } } }
    fun nodeQuery(value: String) { with(runtime) { mutable.update { it.copy(nodeQuery = value) } } }

    /**
     * Skill tree: take a node, give it back, or drop the whole tree.
     *
     * Every rule is the server's — which node is reachable, what it costs, whether a refund would
     * leave the rest of the tree hanging in the air — so the client names a node and reports back.
     */
    fun allocateNode(code: String) { with(runtime) { characterCommand { id -> treeChanged(api.allocateNode(id, code)) } } }
    fun refundNode(code: String) { with(runtime) { characterCommand { id -> treeChanged(api.refundNode(id, code)) } } }
    fun resetTree() { with(runtime) { characterCommand { id -> treeChanged(api.resetTree(id)) } } }
    private fun treeChanged(state: com.sperance.exileforge.core.model.skilltree.SkillTreeState) { with(runtime) {
        mutable.update { it.copy(message = tr("Очков осталось: ${state.available} из ${state.total}", "${state.available} of ${state.total} points left")) }
    } }

    /** Admin only: hand the character experience and let the server decide about the level. */
    fun addExperience(amount: Double) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { tr("Начисление опыта доступно администратору", "Granting experience is available to administrators") }
        val character = api.addExperience(id, amount)
        mutable.update { it.copy(message = tr("Уровень ${character.level}, опыт ${character.experience}", "Level ${character.level}, experience ${character.experience}")) }
    } } }

    fun redeem(code: String) { with(runtime) { characterCommand { id -> api.redeem(id, code) } } }

    /**
     * Puts a jewel into a socket, and takes it back out.
     *
     * Which socket exists, whether the character took it and whether it is free are all the
     * server's to say; the client names the pair and prints the refusal.
     */
    fun socketJewel(inventoryId: String, nodeCode: String) { with(runtime) { characterCommand { id ->
        api.socketJewel(id, inventoryId, nodeCode)
    } } }

    fun unsocketJewel(inventoryId: String) { with(runtime) { characterCommand { id ->
        api.unsocketJewel(id, inventoryId)
    } } }

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

    internal suspend fun readHero() { with(runtime) {
        val id = state.value.characterId.trim()
        check(id.isNotBlank()) { tr("Выберите персонажа", "Choose a character") }
        ensureDefinitions()
        ensureOrbs()
        ensureProgression()
        // The catalogue is half of every card now that an instance keeps only its rolls,
        // so it is read before the hero rather than chased afterwards.
        ensureEquipment()
        val character = api.character(id)
        val view = HeroView(character, api.inventory(id), api.stats(id), api.bag(id), api.characterTree(id))
        mutable.update { it.copy(hero = view, characterOwner = character.userId, heroReadAt = System.currentTimeMillis(),
            selectedEquipment = it.selectedEquipment.takeIf { chosen -> view.inventory.any { item -> item.id == chosen } }
                ?: view.inventory.firstOrNull()?.id.orEmpty()) }
    } }
}

/**
 * How long a reading of the hero is trusted without asking again.
 *
 * Long enough that walking between tabs costs nothing, short enough that a purchase made on
 * another device is not still invisible by the time the player looks for it.
 */
private const val FRESH_FOR = 30_000L
