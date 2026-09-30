package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sperance.exileforge.ForgeApplication
import com.sperance.exileforge.core.campaign.AutoPlan
import com.sperance.exileforge.core.campaign.RunCommand
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.GuildTab
import com.sperance.exileforge.presentation.state.QuestTab
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.StashSort
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot

/** Lifecycle owner and compatibility facade; screen actions live in feature models. */
class ForgeViewModel(store: ServerStore, journal: RequestJournal, deviceId: String) : ViewModel() {
    private val runtime = ForgeRuntime(store, journal, deviceId)
    val state = runtime.state
    val logs = runtime.logs
    fun tab(tab: Int) = runtime.tab(tab)
    /** The campaign run on screen, if any: a world the scene steps and the overlay reads. */
    val expedition = runtime.expeditionViewModel.run
    fun loadCampaign() = runtime.expeditionViewModel.loadCampaign()
    fun startRun(mapCode: String) = runtime.expeditionViewModel.start(mapCode)
    fun startAutoRun(mapCode: String, plan: AutoPlan) = runtime.expeditionViewModel.start(mapCode, plan)
    fun selectZone(mapCode: String) = runtime.expeditionViewModel.selectZone(mapCode)
    fun loadCrafts(silent: Boolean = false) = runtime.craftsViewModel.load(silent)
    fun openProfession(code: String) = runtime.craftsViewModel.openProfession(code)
    fun startWork(job: String, additives: List<String> = emptyList()) = runtime.craftsViewModel.start(job, additives)
    fun stopWork() = runtime.craftsViewModel.stop()
    fun equipTool(itemId: String) = runtime.craftsViewModel.equipTool(itemId)
    fun closeZone() = runtime.expeditionViewModel.closeZone()
    fun pickMap(itemId: String?) = runtime.expeditionViewModel.pickMap(itemId)
    fun runCommand(command: RunCommand) = runtime.expeditionViewModel.send(command)
    fun closeRun() = runtime.expeditionViewModel.close()
    /** The run's journal goes out now: the app leaves the foreground. */
    fun flushRun() = runtime.expeditionViewModel.flushRun()
    fun enterVaal() = runtime.expeditionViewModel.enterVaal()
    fun refuseVaal() = runtime.expeditionViewModel.refuseVaal()
    fun openAtlas() = runtime.expeditionViewModel.openAtlas()
    fun closeAtlas() = runtime.expeditionViewModel.closeAtlas()
    fun selectAtlasNode(code: String) = runtime.expeditionViewModel.selectAtlasNode(code)
    fun allocateAtlas(code: String) = runtime.expeditionViewModel.allocateAtlas(code)
    fun refundAtlas(code: String) = runtime.expeditionViewModel.refundAtlas(code)
    fun resetAtlas() = runtime.expeditionViewModel.resetAtlas()
    fun language(lang: Lang) = runtime.language(lang)
    fun refreshLocale() = runtime.refreshLocale()
    fun refreshIcons() = runtime.refreshIcons()
    fun dismissMessage() = runtime.dismissMessage()
    fun dismissNotice() = runtime.dismissNotice()
    fun selectEquipment(value: String) = runtime.heroViewModel.selectEquipment(value)
    /** An essence on one item, by the essence's item code. */
    fun applyEssence(itemId: String, essence: String) = runtime.heroViewModel.applyEssence(itemId, essence)
    fun selectEssence(value: String) = runtime.heroViewModel.selectEssence(value)
    fun learnSkill(code: String) = runtime.heroViewModel.learnSkill(code)
    fun slotSkill(kind: String, index: Int, code: String?, condition: String? = null) = runtime.heroViewModel.slotSkill(kind, index, code, condition)
    fun flaskCondition(index: Int, condition: String?) = runtime.heroViewModel.flaskCondition(index, condition)
    fun exchangeBooks(books: List<String>, code: String) = runtime.heroViewModel.exchangeBooks(books, code)
    fun loadHero() = runtime.heroViewModel.loadHero()
    fun ensureHero() = runtime.heroViewModel.ensureHero()
    fun equip(itemId: String, slot: Slot? = null) = runtime.heroViewModel.equip(itemId, slot)
    fun unequip(itemId: String) = runtime.heroViewModel.unequip(itemId)
    fun expandStash() = runtime.heroViewModel.expandStash()
    fun setTitle(title: String) = runtime.heroViewModel.setTitle(title)
    fun claimOverflow(itemId: String? = null) = runtime.heroViewModel.claimOverflow(itemId)
    fun sellOverflow(itemId: String) = runtime.heroViewModel.sellOverflow(itemId)
    fun hatchPet(egg: String) = runtime.heroViewModel.hatchPet(egg)
    fun petOrb(petId: String, orb: String) = runtime.heroViewModel.petOrb(petId, orb)
    fun activatePet(petId: String) = runtime.heroViewModel.activatePet(petId)
    fun releasePet(petId: String) = runtime.heroViewModel.releasePet(petId)
    /** Admin only: a named template, rolled by the server at [rarity] or the template's own. */
    fun grant(template: String, rarity: Rarity? = null) = runtime.heroViewModel.grant(template, rarity)
    fun grantRarity(value: String) = runtime.heroViewModel.grantRarity(value)
    fun grantSlot(value: String) = runtime.heroViewModel.grantSlot(value)
    fun grantRandom() = runtime.heroViewModel.grantRandom()
    /** Admin only: a stack into the bag, by the item's code. */
    fun grantItem(code: String, amount: Long) = runtime.heroViewModel.grantItem(code, amount)
    fun selectOrb(value: String) = runtime.heroViewModel.selectOrb(value)
    fun openForge(itemId: String?, section: ForgeSection) = runtime.heroViewModel.openForge(itemId, section)
    fun forgeSection(section: ForgeSection) = runtime.heroViewModel.forgeSection(section)
    fun selectNode(code: String) = runtime.heroViewModel.selectNode(code)
    fun allocateNode(code: String, choice: Int? = null) = runtime.heroViewModel.allocateNode(code, choice)
    fun allocatePath(code: String, choice: Int? = null) = runtime.heroViewModel.allocatePath(code, choice)
    fun refundNode(code: String) = runtime.heroViewModel.refundNode(code)
    fun rechooseNode(code: String, choice: Int) = runtime.heroViewModel.rechooseNode(code, choice)
    fun resetTree() = runtime.heroViewModel.resetTree()
    fun addExperience(amount: Double) = runtime.heroViewModel.addExperience(amount)
    fun draftClass(value: String) = runtime.mutable.value.let { runtime.mutable.value = it.copy(play = it.play.copy(draftClass = value)) }
    /** An orb on one item, by the orb's item code. */
    fun applyOrb(itemId: String, orb: String) = runtime.heroViewModel.applyOrb(itemId, orb)
    fun selectOmen(value: String) = runtime.heroViewModel.selectOmen(value)
    fun unveil(itemId: String, choice: Int) = runtime.heroViewModel.unveil(itemId, choice)
    fun craft(itemId: String, recipe: String) = runtime.heroViewModel.craft(itemId, recipe)
    fun uncraft(itemId: String) = runtime.heroViewModel.uncraft(itemId)
    fun redeem(code: String) = runtime.heroViewModel.redeem(code)
    fun socketJewel(itemId: String, nodeCode: String) = runtime.heroViewModel.socketJewel(itemId, nodeCode)
    fun unsocketJewel(itemId: String) = runtime.heroViewModel.unsocketJewel(itemId)
    fun sellForGold(itemId: String) = runtime.heroViewModel.sellForGold(itemId)
    /** The item lock (3.30.0): a locked item is never sold, listed or auto-sold. */
    fun lockItem(itemId: String, locked: Boolean) = runtime.heroViewModel.lockItem(itemId, locked)
    /** The stash's order (3.30.0), kept on the device. */
    fun stashSort(sort: StashSort) = runtime.heroViewModel.stashSort(sort)
    fun logFilter(kinds: Set<com.sperance.exileforge.core.campaign.LogKind>) = runtime.heroViewModel.logFilter(kinds)
    /** The link's probe at once (3.30.0): the offline icon tapped. */
    fun retryLink() = runtime.connectionViewModel.wake(now = true)
    fun mode(mode: AppMode) = runtime.sessionViewModel.mode(mode)
    fun serverDraft(value: String) = runtime.sessionViewModel.serverDraft(value)
    fun connect() = runtime.sessionViewModel.connect()
    fun health() = runtime.sessionViewModel.health()
    fun login(login: String, password: String) = runtime.sessionViewModel.login(login, password)
    fun playOnThisDevice() = runtime.sessionViewModel.playOnThisDevice()
    fun retryResume() = runtime.sessionViewModel.retryResume()
    fun reconnect() = runtime.sessionViewModel.reconnect()
    fun away() = runtime.sessionViewModel.away()
    fun enterCharacter(id: String) = runtime.characterViewModel.enter(id)
    fun leaveGame() = runtime.characterViewModel.leaveGame()
    fun createCharacter(name: String, heroClass: String) = runtime.characterViewModel.create(name, heroClass)
    fun deleteCharacter(id: String) = runtime.characterViewModel.delete(id)
    fun refreshCharacters() = runtime.characterViewModel.refresh()
    fun ensureClasses() = runtime.characterViewModel.ensureClasses()
    fun logout() = runtime.sessionViewModel.logout()
    fun changePassword(current: String, replacement: String) = runtime.sessionViewModel.changePassword(current, replacement)
    fun nodeQuery(value: String) = runtime.heroViewModel.nodeQuery(value)
    fun auctionTab(tab: Int) = runtime.auctionViewModel.auctionTab(tab)
    fun auctionFilter(filter: AuctionFilter) = runtime.auctionViewModel.auctionFilter(filter)
    fun showOwnLots(show: Boolean) = runtime.auctionViewModel.showOwnLots(show)
    fun loadAuction() = runtime.auctionViewModel.loadAuction()
    fun loadShowcase(page: Int = 0) = runtime.auctionViewModel.loadShowcase(page)
    fun loadMyLots(glance: Boolean = false) = runtime.auctionViewModel.loadMyLots(glance)
    fun loadMerchant() = runtime.auctionViewModel.loadMerchant()
    /** A building of the City (3.22.0), or the square for none. */
    fun building(building: Building?) = runtime.mutable.value.let { runtime.mutable.value = it.copy(building = building, message = null, error = false) }
    fun loadGuild() = runtime.guildViewModel.load()
    fun guildTab(tab: GuildTab) = runtime.guildViewModel.tab(tab)
    fun guildQuery(text: String) = runtime.guildViewModel.query(text)
    fun guildFaction(code: String) = runtime.guildViewModel.filterFaction(code)
    fun searchGuilds(page: Int = 0) = runtime.guildViewModel.search(page)
    fun createGuild(name: String, tag: String, faction: String, emblem: String, color: String, mode: GuildMode, minLevel: Int) =
        runtime.guildViewModel.create(name, tag, faction, emblem, color, mode, minLevel)
    fun joinGuild(card: GuildCard) = runtime.guildViewModel.join(card)
    fun acceptGuildInvite(guildId: String) = runtime.guildViewModel.acceptInvite(guildId)
    fun declineGuildInvite(guildId: String) = runtime.guildViewModel.declineInvite(guildId)
    fun acceptApplicant(applicantId: String) = runtime.guildViewModel.acceptApplicant(applicantId)
    fun declineApplicant(applicantId: String) = runtime.guildViewModel.declineApplicant(applicantId)
    fun inviteToGuild(name: String) = runtime.guildViewModel.invite(name)
    fun guildMember(command: MemberCommand, memberId: String) = runtime.guildViewModel.member(command, memberId)
    fun leaveGuild() = runtime.guildViewModel.leave()
    fun disbandGuild() = runtime.guildViewModel.disband()
    fun guildSettings(mode: GuildMode, minLevel: Int, emblem: String, color: String, announcement: String) =
        runtime.guildViewModel.settings(mode, minLevel, emblem, color, announcement)
    /** Gold (`GOLD`) or an orb, by its item code, into the treasury. */
    fun contribute(item: String, amount: Long) = runtime.guildViewModel.contribute(item, amount)
    fun loadGuildLog(more: Boolean = false) = runtime.guildViewModel.loadLog(more)

    // Quests (3.23.0): the City's board and the guild's quests.
    fun loadQuests() = runtime.questViewModel.load()
    fun questTab(tab: QuestTab) = runtime.questViewModel.tab(tab)
    fun claimQuest(questId: String) = runtime.questViewModel.claim(questId)
    fun takeContract(offerId: String) = runtime.questViewModel.take(offerId)
    fun abandonContract(questId: String) = runtime.questViewModel.abandon(questId)
    fun loadGuildQuests() = runtime.questViewModel.loadGuild()
    fun claimGuildQuest(questId: String? = null, goal: String? = null) = runtime.questViewModel.claimGuild(questId, goal)
    fun buyLot(lotId: String) = runtime.auctionViewModel.buy(lotId)
    fun buyOffer(offerId: String) = runtime.auctionViewModel.buyOffer(offerId)
    fun buyOrb(code: String) = runtime.auctionViewModel.buyOrb(code)
    fun buyLotSlot() = runtime.auctionViewModel.buySlot()
    fun cancelLot(lotId: String) = runtime.auctionViewModel.cancel(lotId)
    /** Lists a copy for [price] of the orb [priceOrb] (an item code). */
    fun sellEquipment(itemId: String, priceOrb: String, price: Long) = runtime.auctionViewModel.sellEquipment(itemId, priceOrb, price)
    /** Lists [amount] of the stack [code]. */
    fun sellItem(code: String, amount: Long, priceOrb: String, price: Long) = runtime.auctionViewModel.sellItem(code, amount, priceOrb, price)
    fun clearLogs() = runtime.journal.clear()
    fun loadRedemptions() = runtime.redemptionViewModel.load()
    fun createRedemption(code: RedemptionCode) = runtime.redemptionViewModel.create(code)
    fun deleteRedemption(id: String) = runtime.redemptionViewModel.delete(id)
    override fun onCleared() { runtime.close() }
    class Factory(private val app: ForgeApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ForgeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ForgeViewModel(app.serverStore, app.journal, app.deviceId) as T
        }
    }
}
