package com.sperance.exileforge.presentation.features

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.Reads
import com.sperance.exileforge.presentation.state.StashSort
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.sync.API_REVISION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class HeroViewModel(runtime: ForgeRuntime) : FeatureViewModel(runtime) {

    fun selectEquipment(value: String) { with(runtime) { mutable.update { it.copy(play = it.play.copy(selectedEquipment = value,
        forgeLine = if (value == it.play.selectedEquipment) it.play.forgeLine else "")) } } }

    /** The forge over one item, on the section the player came for; `null` keeps the item it had. */
    fun openForge(itemId: String?, section: ForgeSection) { with(runtime) {
        itemId?.let { selectEquipment(it) }
        mutable.update { it.copy(play = it.play.copy(forgeSection = section)) }
        tab(TAB_CRAFT)
    } }
    fun forgeSection(section: ForgeSection) = update { it.copy(play = it.play.copy(forgeSection = section)) }

    fun loadHero() { with(runtime) { read(Reads.HERO) { readHero() } } }

    /** The hero, if what is on screen has gone cold: one request, and usually a 304. */
    fun ensureHero() { with(runtime) {
        val now = System.currentTimeMillis()
        if (state.value.play.heroId.isBlank()) return
        if (state.value.play.hero != null && now - state.value.play.heroReadAt < FRESH_FOR) return
        read(Reads.HERO) { readHero() }
    } }

    fun equip(itemId: String, slot: Slot? = null) { with(runtime) { heroCommand { id -> api.hero.equip(id, itemId, slot) } } }
    fun unequip(itemId: String) { with(runtime) { heroCommand { id -> api.hero.unequip(id, itemId) } } }
    /** The title beside the name (1.3.0): one the chronicle opened, or none. */
    fun setTitle(title: String) { with(runtime) { heroCommand { id -> api.hero.setTitle(id, title) } } }
    /** Locks or unlocks an item (3.30.0): a locked one is never sold, listed or auto-sold; the snapshot carries the flag. */
    fun stashSort(sort: StashSort) { with(runtime) {
        update { it.copy(stashSort = sort) }
        scope.launch { store.saveStashSort(sort.name) }
    } }

    fun lockItem(itemId: String, locked: Boolean) { with(runtime) { heroCommand { id -> api.hero.lock(id, itemId, locked) } } }
    /** One more pack of stash places for gold (1.1.0); the snapshot with the answer carries the new count. */
    fun expandStash() { with(runtime) { heroCommand { id -> api.hero.expandStash(id) } } }
    /** From the overflow into the stash: [itemId], or as many as fit. */
    fun claimOverflow(itemId: String? = null) { with(runtime) { heroCommand { id -> api.hero.claimOverflow(id, itemId) } } }
    fun sellOverflow(itemId: String) { with(runtime) { heroCommand { id -> api.hero.sellOverflow(id, itemId) } } }

    // The menagerie (3.5.0): the snapshot with each answer carries the pets and the bag.
    fun hatchPet(egg: String) { with(runtime) { heroCommand { id -> api.hero.hatchPet(id, egg) } } }
    fun petOrb(petId: String, orb: String) { with(runtime) { heroCommand { id -> api.hero.petOrb(id, petId, orb) } } }
    fun activatePet(petId: String) { with(runtime) { heroCommand { id -> api.hero.activatePet(id, petId) } } }
    fun releasePet(petId: String) { with(runtime) { heroCommand { id -> api.hero.releasePet(id, petId) } } }

    /** Admin only: hand the hero a named template, rolled by the server. */
    fun grant(template: String, rarity: Rarity? = null) { with(runtime) { heroCommand { id ->
        check(state.value.isAdmin) { ui("hero.grant_admin_only") }
        api.hero.grantEquipment(id, template, rarity)
    } } }

    fun grantRarity(value: String) = update { it.copy(play = it.play.copy(grantRarity = value)) }
    fun grantSlot(value: String) = update { it.copy(play = it.play.copy(grantSlot = value)) }

    /** Admin only: a random template of the chosen rarity and slot — picked here from the content, rolled by the server. */
    fun grantRandom() { with(runtime) { heroCommand { id ->
        check(state.value.isAdmin) { ui("hero.grant_admin_only") }
        val index = state.value.index ?: error(ui("runtime.request_failed"))
        val rarity = Rarity.of(state.value.play.grantRarity)
        val slot = Slot.of(state.value.play.grantSlot)
        val candidates = index.templates.values.filter { (slot == null || it.slot == slot) && (rarity == null || rarity.fixed == it.unique) }
        check(candidates.isNotEmpty()) { ui("hero.no_template") }
        api.hero.grantEquipment(id, Dice.system().pick(candidates).code, rarity)
    } } }

    /** Admin only: a stack into the bag. */
    fun grantItem(code: String, amount: Long) { with(runtime) { heroCommand { id ->
        check(state.value.isAdmin) { ui("hero.bag_admin_only") }
        api.hero.grantItem(id, code, amount)
    } } }

    fun selectOrb(value: String) = update { it.copy(play = it.play.copy(selectedOrb = value)) }
    fun selectEssence(value: String) = update { it.copy(play = it.play.copy(selectedEssence = value)) }

    /** Spends one orb on one item: whether it applies and what it rerolls is the rules'; the sentence comes back. */
    fun applyOrb(itemId: String, orb: String) { with(runtime) { forgeCommand { id ->
        val outcome = api.hero.applyOrb(id, itemId, orb)
        mutable.update { it.copy(play = it.play.copy(forgeLine = outcome.message, selectedEquipment = outcome.created?.id ?: outcome.item.id)) }
    } } }

    fun craft(itemId: String, recipe: String) { with(runtime) { forgeCommand { id ->
        val outcome = api.hero.craft(id, itemId, recipe)
        mutable.update { it.copy(play = it.play.copy(forgeLine = outcome.message)) }
    } } }
    fun uncraft(itemId: String) { with(runtime) { forgeCommand { id ->
        val outcome = api.hero.uncraft(id, itemId)
        mutable.update { it.copy(play = it.play.copy(forgeLine = outcome.message)) }
    } } }

    fun applyEssence(itemId: String, essence: String) { with(runtime) { forgeCommand { id ->
        val outcome = api.hero.applyEssence(id, itemId, essence)
        mutable.update { it.copy(play = it.play.copy(forgeLine = outcome.message, selectedEquipment = outcome.item.id)) }
    } } }

    /** The grimoire: a book read, a skill slotted or taken out, a slot's or a flask's condition, books traded for one. */
    fun learnSkill(code: String) { with(runtime) { heroCommand { id -> api.hero.learnSkill(id, code) } } }
    fun slotSkill(kind: String, index: Int, code: String?, condition: String? = null) { with(runtime) { heroCommand { id -> api.hero.slotSkill(id, kind, index, code, condition) } } }
    fun flaskCondition(index: Int, condition: String?) { with(runtime) { heroCommand { id -> api.hero.flaskCondition(id, index, condition) } } }
    fun exchangeBooks(books: List<String>, code: String) { with(runtime) { heroCommand { id -> api.hero.exchangeBooks(id, books, code) } } }

    fun selectNode(code: String) = update { it.copy(play = it.play.copy(selectedNode = code)) }
    fun nodeQuery(value: String) = update { it.copy(play = it.play.copy(nodeQuery = value)) }

    /** Skill tree: take a node, give it back, or drop the whole tree. Every rule is the server's. */
    fun allocateNode(code: String, choice: Int? = null) { with(runtime) { heroCommand { id -> api.tree.allocate(id, code, choice) } } }
    fun refundNode(code: String) { with(runtime) { heroCommand { id -> api.tree.refund(id, code) } } }
    fun rechooseNode(code: String, choice: Int) { with(runtime) { heroCommand { id -> api.tree.rechoose(id, code, choice) } } }
    fun resetTree() { with(runtime) { heroCommand { id -> api.tree.reset(id) } } }

    /** Admin only: hand the hero experience and let the server decide about the level. */
    fun addExperience(amount: Double) { with(runtime) { heroCommand { id ->
        check(state.value.isAdmin) { ui("hero.xp_admin_only") }
        api.hero.grantExperience(id, amount)
    } } }

    fun redeem(code: String) { with(runtime) { heroCommand { id -> toast(ui("redemption.redeemed")); api.hero.redeem(id, code) } } }

    fun socketJewel(itemId: String, nodeCode: String) { with(runtime) { heroCommand { id -> api.hero.socket(id, itemId, nodeCode) } } }
    fun unsocketJewel(itemId: String) { with(runtime) { heroCommand { id -> api.hero.unsocket(id, itemId) } } }

    /** Sells an item to a merchant: the server sets the price and pays it; the card showed the same sum beforehand. */
    fun sellForGold(itemId: String) { with(runtime) { heroCommand { id -> toast(ui("toast.sold", api.hero.sell(id, itemId).gold)) } } }

    /**
     * Every hero command answers with the hero as the server has it now, so nothing is patched from the
     * response — unless the answer came without a snapshot, which [delivered] marks by setting the reading cold.
     */
    private fun heroCommand(block: suspend (String) -> Unit) { with(runtime) { task(writing = true, touches = setOf(Reads.HERO)) {
        val id = heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        check(state.value.ownsCharacter || state.value.isAdmin) { ui("hero.owner_only") }
        block(id)
        if (state.value.play.heroReadAt == 0L) readHero()
        expeditionViewModel.regear()
    } } }

    /** A command of the forge: the previous sentence goes the moment another command starts. */
    private fun forgeCommand(block: suspend (String) -> Unit) = heroCommand { id ->
        runtime.mutable.update { it.copy(play = it.play.copy(forgeLine = "")) }
        block(id)
    }

    private var parts: HeroParts? = null

    /** How many snapshots have been applied; a command compares it to know one came back. */
    var snapshots = 0L
        private set

    fun forget() { parts = null; copyJob?.cancel() }

    /** The hero as the device kept them (3.30.0): drawn at once by the fast start, read again once the session is confirmed. */
    internal fun restore(heroId: String, snapshot: HeroSnapshot) { parts = null; apply(heroId, snapshot, keep = false) }

    private var copyJob: Job? = null

    /** The hero on the device for the next launch, a moment after the last snapshot so a burst of commands is one write. */
    private fun keepCopy(heroId: String, held: HeroParts) { with(runtime) {
        val profile = state.value.account.profile ?: return
        val server = state.value.account.server
        copyJob?.cancel()
        copyJob = scope.launch {
            delay(COPY_AFTER)
            val text = withContext(Dispatchers.Default) { WireJson.encodeToString(HeroCopy.serializer(), HeroCopy(API_REVISION, profile, held.snapshot())) }
            store.saveHeroCopy(server, heroId, text)
        }
    } }

    /** What a command on [heroId] tells the server the client holds; `null` asks for nothing. */
    fun heldParts(heroId: String): String? =
        if (!onScreen(heroId)) null else parts?.takeIf { it.heroId == heroId }?.header() ?: HeroParts(heroId).header()

    /** A command's answer: its snapshot, or none — and then the reading is cold and read again. */
    fun delivered(heroId: String, snapshot: HeroSnapshot?) {
        if (!onScreen(heroId)) return
        if (snapshot == null) runtime.mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }
        else apply(heroId, snapshot)
    }

    /** The hero in one request: only what moved since the parts held here, or a 304 when nothing did. The content comes first. */
    internal suspend fun readHero() { with(runtime) {
        val id = heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        ensureContent()
        val held = parts?.takeIf { it.heroId == id } ?: HeroParts(id)
        val snapshot = api.hero.view(id, held)
        when {
            snapshot != null -> apply(id, snapshot)
            state.value.play.hero == null -> apply(id, HeroSnapshot(held.version))
            else -> mutable.update { it.copy(play = it.play.copy(heroReadAt = System.currentTimeMillis(), heroSeenAt = System.currentTimeMillis())) }
        }
    } }

    /** Folds a snapshot into the parts held and draws the hero from them; the sheet is added up here by the rules. */
    private fun apply(heroId: String, snapshot: HeroSnapshot, keep: Boolean = true) { with(runtime) {
        val merged = (parts?.takeIf { it.heroId == heroId } ?: HeroParts(heroId)).merge(snapshot)
        if (!merged.complete) { parts = null; mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }; return }
        val index = state.value.index ?: run { mutable.update { it.copy(play = it.play.copy(heroReadAt = 0)) }; return }
        parts = merged
        snapshots++
        if (keep) keepCopy(heroId, merged)
        val info = merged.hero
        val items = merged.items
        val tree = merged.tree
        val pets = merged.pets
        val sheet = Sheets.calculate(index, info.level, info.heroClass, tree, items, pets.active)
        val view = HeroView(info, items, merged.overflow, merged.bag, tree, merged.campaign, merged.crafts, merged.merchant, sheet, pets)
        val now = System.currentTimeMillis()
        mutable.update { it.copy(play = it.play.copy(hero = view, heroOwner = info.userId, heroReadAt = now, heroSeenAt = now,
            selectedEquipment = it.play.selectedEquipment.takeIf { chosen -> view.items.any { item -> item.id == chosen } } ?: view.items.firstOrNull()?.id.orEmpty()),
            market = it.market.copy(merchant = view.merchant)) }
        expeditionViewModel.heroChanged(view)
    } }
}

/** How long a reading of the hero is trusted without asking again. */
private const val FRESH_FOR = 30_000L

/** How long the hero's copy waits for the next snapshot before it is written. */
private const val COPY_AFTER = 1_500L

/** The hero kept on the device (3.30.0): who played them, under which API revision, and every part as last read. */
@Serializable
data class HeroCopy(val revision: Int, val account: UserProfile, val snapshot: HeroSnapshot)
