package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.ItemStack
import com.sperance.exileforge.core.model.command.UseRecipeCommand
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import kotlinx.coroutines.flow.update

class HeroViewModel(private val runtime: ForgeRuntime) {
    private val state get() = runtime.state

    fun selectEquipment(value: String) { with(runtime) { mutable.update { it.copy(play = it.play.copy(selectedEquipment = value,
        forgeLine = if (value == it.play.selectedEquipment) it.play.forgeLine else "")) } } }

    /** The forge over one item, on the section the player came for; `null` keeps the item it had. */
    fun openForge(instanceId: String?, section: ForgeSection) { with(runtime) {
        instanceId?.let { selectEquipment(it) }
        mutable.update { it.copy(play = it.play.copy(forgeSection = section)) }
        tab(TAB_CRAFT)
    } }
    fun forgeSection(section: ForgeSection) { with(runtime) { mutable.update { it.copy(play = it.play.copy(forgeSection = section)) } } }

    fun loadHero() { with(runtime) { read(Reads.HERO) { readHero() } } }

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
        if (state.value.play.characterId.isBlank()) return
        if (state.value.play.hero != null && now - state.value.play.heroReadAt < FRESH_FOR) return
        read(Reads.HERO) { readHero() }
    } }

    fun equip(instanceId: String, slot: String? = null) { with(runtime) { characterCommand { id -> api.hero.equip(id, instanceId, slot) } } }
    fun unequip(instanceId: String) { with(runtime) { characterCommand { id -> api.hero.unequip(id, instanceId) } } }

    /** Admin only: hand the character a named template, rolled by the server. */
    fun grant(equipmentId: String) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { ui("hero.grant_admin_only") }
        api.hero.grant(id, equipmentId)
    } } }

    fun grantRarity(value: String) { with(runtime) { mutable.update { it.copy(play = it.play.copy(grantRarity = value)) } } }
    fun grantSlot(value: String) { with(runtime) { mutable.update { it.copy(play = it.play.copy(grantSlot = value)) } } }

    /**
     * Admin only: a random template of the chosen rarity and category, with server-rolled modifiers.
     *
     * The client picks the base and nothing else — prefixes, suffixes, tiers and values all come
     * back from `itemToInventory`.
     */
    fun grantRandom() { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { ui("hero.grant_admin_only") }
        val template = api.catalog.randomTemplate(state.value.play.grantRarity, state.value.play.grantSlot)
        api.hero.grant(id, template.entityId)
        val name = equipmentTitle(template)
        mutable.update { it.copy(message = ui("hero.rolled", name)) }
    } } }

    fun adjustItems(itemId: String, amount: Long) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { ui("hero.bag_admin_only") }
        api.hero.adjustItems(id, listOf(ItemStack(itemId, amount)))
    } } }

    fun selectOrb(value: String) { with(runtime) { mutable.update { it.copy(play = it.play.copy(selectedOrb = value)) } } }

    /**
     * Spends one orb on one item of the inventory.
     *
     * Whether the orb applies at all, what it rerolls and what it leaves alone is the server's rule;
     * the client only names the pair and prints the sentence that comes back.
     */
    fun applyOrb(inventoryId: String, orbItemId: String) { with(runtime) { forgeCommand { id ->
        val outcome = api.hero.applyOrb(id, inventoryId, orbItemId)
        mutable.update { it.copy(play = it.play.copy(forgeLine = outcome.message, selectedEquipment = outcome.created?.id ?: outcome.item.id)) }
    } } }

    /**
     * The crafting bench: one crafted modifier placed, or taken back off.
     *
     * Every rule and the price are the server's; the answer is an orb's — the item and a sentence —
     * and the hero is re-read after it like after any command.
     */
    fun craft(inventoryId: String, recipe: String) { with(runtime) { forgeCommand { id ->
        val outcome = api.hero.craft(id, inventoryId, recipe)
        mutable.update { it.copy(play = it.play.copy(forgeLine = outcome.message)) }
    } } }
    fun uncraft(inventoryId: String) { with(runtime) { forgeCommand { id ->
        val outcome = api.hero.uncraft(id, inventoryId)
        mutable.update { it.copy(play = it.play.copy(forgeLine = outcome.message)) }
    } } }

    fun selectNode(code: String) { with(runtime) { mutable.update { it.copy(play = it.play.copy(selectedNode = code)) } } }
    fun nodeQuery(value: String) { with(runtime) { mutable.update { it.copy(play = it.play.copy(nodeQuery = value)) } } }

    /**
     * Skill tree: take a node, give it back, or drop the whole tree.
     *
     * Every rule is the server's — which node is reachable, what it costs, whether a refund would
     * leave the rest of the tree hanging in the air — so the client names a node and reports back.
     */
    fun allocateNode(code: String) { with(runtime) { characterCommand { id -> treeChanged(api.tree.allocate(id, code)) } } }
    fun refundNode(code: String) { with(runtime) { characterCommand { id -> treeChanged(api.tree.refund(id, code)) } } }
    fun resetTree() { with(runtime) { characterCommand { id -> treeChanged(api.tree.reset(id)) } } }
    private fun treeChanged(state: com.sperance.exileforge.core.model.skilltree.SkillTreeState) { with(runtime) {
        mutable.update { it.copy(message = ui("hero.points_left", state.available, state.total)) }
    } }

    /** Admin only: hand the character experience and let the server decide about the level. */
    fun addExperience(amount: Double) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { ui("hero.xp_admin_only") }
        val character = api.hero.addExperience(id, amount)
        mutable.update { it.copy(message = ui("hero.level_and_xp", character.level, character.experience)) }
    } } }

    fun redeem(code: String) { with(runtime) { characterCommand { id -> api.hero.redeem(id, code) } } }

    /**
     * Puts a jewel into a socket, and takes it back out.
     *
     * Which socket exists, whether the character took it and whether it is free are all the
     * server's to say; the client names the pair and prints the refusal.
     */
    fun socketJewel(inventoryId: String, nodeCode: String) { with(runtime) { characterCommand { id ->
        api.hero.socket(id, inventoryId, nodeCode)
    } } }

    fun unsocketJewel(inventoryId: String) { with(runtime) { characterCommand { id ->
        api.hero.unsocket(id, inventoryId)
    } } }

    /**
     * Sells an item to a merchant.
     *
     * The price is the server's — it answers with what was paid and what the purse holds now —
     * and the hero is re-read because the item is gone and the gold is not where it was.
     */
    fun sellForGold(inventoryId: String) { with(runtime) { characterCommand { id ->
        val outcome = api.hero.sellForGold(id, inventoryId)
        // What it fetched is the whole point of the command, so it is said rather than left to
        // the generic "saved" — characterCommand keeps a message that is already there.
        mutable.update { it.copy(message = ui("hero.sold_for", outcome.gold, outcome.money)) }
    } } }

    fun useRecipe(recipeId: String, ingredients: List<String>, amount: Long) { with(runtime) { characterCommand { id ->
        api.hero.useRecipe(id, recipeId, UseRecipeCommand(ingredients, amount))
    } } }

    /**
     * Every character command is a write the server may have applied even when the answer is lost,
     * so the hero is always re-read afterwards rather than patched from the response.
     */
    private fun characterCommand(announce: Boolean = true, block: suspend (String) -> Unit) { with(runtime) { task(writing = true, touches = setOf(Reads.HERO)) {
        val id = state.value.play.characterId.trim()
        check(id.isNotBlank()) { ui("auction.choose_character") }
        check(state.value.ownsCharacter || state.value.isAdmin) { ui("hero.owner_only") }
        block(id)
        readHero()
        if (announce) mutable.update { it.copy(message = it.message ?: ui("hero.changes_saved")) }
    } } }

    /**
     * A command of the forge: its answer is the server's sentence under the item rather than a
     * snackbar, and the previous sentence goes the moment another command starts, so a refusal is
     * never read beside the success before it.
     */
    private fun forgeCommand(block: suspend (String) -> Unit) = characterCommand(announce = false) { id ->
        runtime.mutable.update { it.copy(play = it.play.copy(forgeLine = "")) }
        block(id)
    }

    internal suspend fun readHero() { with(runtime) {
        val id = state.value.play.characterId.trim()
        check(id.isNotBlank()) { ui("auction.choose_character") }
        ensureDefinitions()
        ensureOrbs()
        ensureBench()
        ensureProgression()
        // The catalogue is half of every card now that an instance keeps only its rolls,
        // so it is read before the hero rather than chased afterwards.
        ensureEquipment()
        val character = api.hero.character(id)
        val view = HeroView(character, api.hero.inventory(id), api.hero.stats(id), api.hero.bag(id), api.tree.state(id))
        mutable.update { it.copy(play = it.play.copy(hero = view, characterOwner = character.userId, heroReadAt = System.currentTimeMillis(), selectedEquipment = it.play.selectedEquipment.takeIf { chosen -> view.inventory.any { item -> item.id == chosen } }
                ?: view.inventory.firstOrNull()?.id.orEmpty())) }
    } }
}

/**
 * How long a reading of the hero is trusted without asking again.
 *
 * Long enough that walking between tabs costs nothing, short enough that a purchase made on
 * another device is not still invisible by the time the player looks for it.
 */
private const val FRESH_FOR = 30_000L
