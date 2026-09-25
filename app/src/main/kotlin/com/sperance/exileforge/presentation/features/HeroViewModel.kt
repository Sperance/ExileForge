package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.character.Sheet
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.ItemStack
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
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
     * A command brings the hero back in its own answer; this is for everything that changed the
     * character somewhere else — a trade, an administrator, the same account on another device —
     * where the client has no way to be told. It is one request, and usually a 304 (server 0.48.0).
     * [FRESH_FOR] is how long a reading is trusted without asking.
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
    fun allocateNode(code: String, choice: Int? = null) { with(runtime) { characterCommand { id -> api.tree.allocate(id, code, choice) } } }
    fun refundNode(code: String) { with(runtime) { characterCommand { id -> api.tree.refund(id, code) } } }
    fun resetTree() { with(runtime) { characterCommand { id -> api.tree.reset(id) } } }

    /** Admin only: hand the character experience and let the server decide about the level. */
    fun addExperience(amount: Double) { with(runtime) { characterCommand { id ->
        check(state.value.isAdmin) { ui("hero.xp_admin_only") }
        api.hero.addExperience(id, amount)
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
     * The server sets the price and pays it; the card showed the same sum beforehand (2.46.0), by
     * the merchant's own rule, and the hero is re-read because the item is gone and the gold moved.
     */
    fun sellForGold(inventoryId: String) { with(runtime) { characterCommand { id -> api.hero.sellForGold(id, inventoryId) } } }

    /**
     * Every character command answers with the hero as the server has it now (server 0.48.0), so
     * nothing is patched from the response and nothing is read again — unless the answer came
     * without a snapshot, which [delivered] marks by setting the reading cold.
     */
    private fun characterCommand(block: suspend (String) -> Unit) { with(runtime) { task(writing = true, touches = setOf(Reads.HERO)) {
        val id = state.value.play.characterId.trim()
        check(id.isNotBlank()) { ui("auction.choose_character") }
        check(state.value.ownsCharacter || state.value.isAdmin) { ui("hero.owner_only") }
        block(id)
        if (state.value.play.heroReadAt == 0L) readHero()
        expeditionViewModel.regear()
    } } }

    /**
     * A command of the forge: its answer is the server's sentence under the item, and the previous
     * sentence goes the moment another command starts, so a refusal is never read beside the
     * success before it.
     */
    private fun forgeCommand(block: suspend (String) -> Unit) = characterCommand { id ->
        runtime.mutable.update { it.copy(play = it.play.copy(forgeLine = "")) }
        block(id)
    }

    /** The parts of the hero held here, and the character they belong to. */
    private var parts: HeroParts? = null

    /** How many snapshots have been applied; a command compares it to know one came back. */
    var snapshots = 0L
        private set

    /** Another character, or nobody: what is held no longer answers `If-None-Match`. */
    fun forget() { parts = null }

    /** What a command on [characterId] tells the server the client holds; `null` asks for nothing. */
    fun heldParts(characterId: String): String? =
        if (characterId != state.value.play.characterId.trim()) null
        else parts?.takeIf { it.characterId == characterId }?.header() ?: HeroParts(characterId).header()

    /** A command's answer: its snapshot, or none — and then the reading is cold and read again. */
    fun delivered(characterId: String, snapshot: HeroSnapshot?) {
        if (characterId != state.value.play.characterId.trim()) return
        if (snapshot == null) runtime.mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
        else apply(characterId, snapshot)
    }

    /**
     * The hero in one request (server 0.48.0): only what moved since the parts held here, or a 304
     * when nothing did. The reference tables come first — a card is half its base.
     */
    internal suspend fun readHero() { with(runtime) {
        val id = state.value.play.characterId.trim()
        check(id.isNotBlank()) { ui("auction.choose_character") }
        ensureWorld()
        val held = parts?.takeIf { it.characterId == id } ?: HeroParts(id)
        val snapshot = api.hero.view(id, held)
        when {
            snapshot != null -> apply(id, snapshot)
            state.value.play.hero == null -> apply(id, HeroSnapshot(held.version))
            else -> mutable.update { it.copy(play = it.play.copy(heroReadAt = System.currentTimeMillis(), heroSeenAt = System.currentTimeMillis())) }
        }
    } }

    /**
     * Folds a snapshot into the parts held and draws the hero from them. The sheet is added up
     * here since 2.46.0, by the server's formula and in its order; a part is replaced whole.
     */
    private fun apply(characterId: String, snapshot: HeroSnapshot) { with(runtime) {
        val merged = (parts?.takeIf { it.characterId == characterId } ?: HeroParts(characterId)).merge(snapshot)
        if (!merged.complete) { parts = null; mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }; return }
        parts = merged
        snapshots++
        val character = merged.character
        val inventory = merged.inventory
        val tree = merged.tree
        val world = state.value.world
        val sheet = Sheet.calculate(character, world.classes.firstOrNull { it.id == character.classId }, tree.nodes, inventory,
            world.inventoryBases, world.definitions, world.statTables)
        val view = HeroView(character, inventory, sheet, merged.bag, tree)
        val now = System.currentTimeMillis()
        mutable.update { it.copy(world = it.world.copy(bench = merged.bench), play = it.play.copy(hero = view, characterOwner = character.userId,
            heroReadAt = now, heroSeenAt = now, selectedEquipment = it.play.selectedEquipment.takeIf { chosen -> view.inventory.any { item -> item.id == chosen } }
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
