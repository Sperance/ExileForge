package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sperance.exileforge.ForgeApplication
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.ForgeSection
import kotlinx.serialization.json.JsonObject

/** Lifecycle owner and compatibility facade; screen actions live in feature models. */
class ForgeViewModel(store: ServerStore, journal: RequestJournal, deviceId: String) : ViewModel() {
    private val runtime = ForgeRuntime(store, journal, deviceId)
    val state = runtime.state
    val logs = runtime.logs
    fun tab(tab: Int) = runtime.tab(tab)
    /** The campaign run on screen, if any: a world the scene steps and the overlay reads. */
    val expedition = runtime.expeditionViewModel.run
    fun loadCampaign() = runtime.expeditionViewModel.loadCampaign()
    fun nextCampaignMap() = runtime.expeditionViewModel.nextMap()
    fun startRun(mapCode: String) = runtime.expeditionViewModel.start(mapCode)
    fun openLaunch(mapCode: String) = runtime.expeditionViewModel.openLaunch(mapCode)
    fun loadCrafts() = runtime.craftsViewModel.load()
    fun openProfession(code: String) = runtime.craftsViewModel.openProfession(code)
    fun startWork(job: String) = runtime.craftsViewModel.start(job)
    fun stopWork() = runtime.craftsViewModel.stop()
    fun equipTool(instanceId: String) = runtime.craftsViewModel.equipTool(instanceId)
    fun closeLaunch() = runtime.expeditionViewModel.closeLaunch()
    fun pickMap(instanceId: String?) = runtime.expeditionViewModel.pickMap(instanceId)
    fun buyTreasure(mapCode: String) = runtime.expeditionViewModel.buyTreasure(mapCode)
    fun summonGuardian(mapCode: String) = runtime.expeditionViewModel.summonGuardian(mapCode)
    fun runCommand(command: com.sperance.exileforge.core.campaign.RunCommand) = runtime.expeditionViewModel.send(command)
    fun closeRun() = runtime.expeditionViewModel.close()
    fun language(lang: Lang) = runtime.language(lang)
    /** The server's names live in its dictionary; this re-reads it without touching the session. */
    fun refreshLocale() = runtime.refreshLocale()
    /** The server's drawings live in its icon set; this re-reads it without touching the session. */
    fun refreshIcons() = runtime.refreshIcons()
    fun dismissMessage() = runtime.dismissMessage()
    suspend fun referencePage(source: EntitySource, page: Int, query: String) = runtime.referencePage(source, page, query)
    suspend fun recipeDocument(id: String) = runtime.recipeDocument(id)
    /** The template behind an instance, for a card that has only the instance — an auction lot. */
    suspend fun equipmentBase(id: String) = runtime.equipmentBase(id)
    fun query(value: String) = runtime.catalogViewModel.query(value)
    fun catalog(value: Catalog) = runtime.catalogViewModel.catalog(value)
    fun filter(value: CatalogFilter) = runtime.catalogViewModel.filter(value)
    fun applyFilters() = runtime.catalogViewModel.applyFilters()
    fun refresh(page: Int = runtime.state.value.admin.page) = runtime.catalogViewModel.refresh(page)
    fun count() = runtime.catalogViewModel.count()
    fun open(id: String) = runtime.catalogViewModel.open(id)
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) = runtime.editorViewModel.create(kind)
    fun closeEditor() = runtime.editorViewModel.closeEditor()
    fun edit(document: JsonObject) = runtime.editorViewModel.edit(document)
    fun loadDefinitions() = runtime.editorViewModel.loadDefinitions()
    fun reloadEditor() = runtime.editorViewModel.reloadEditor()
    fun save() = runtime.editorViewModel.save()
    fun delete() = runtime.editorViewModel.delete()
    fun editInventoryBase(id: String) = runtime.editorViewModel.editInventoryBase(id)
    fun selectEquipment(value: String) = runtime.heroViewModel.selectEquipment(value)
    fun loadHero() = runtime.heroViewModel.loadHero()
    /** Re-reads the hero only if what is on screen has gone cold; every character tab opens with it. */
    fun ensureHero() = runtime.heroViewModel.ensureHero()
    fun equip(instanceId: String, slot: String? = null) = runtime.heroViewModel.equip(instanceId, slot)
    fun unequip(instanceId: String) = runtime.heroViewModel.unequip(instanceId)
    fun grant(equipmentId: String) = runtime.heroViewModel.grant(equipmentId)
    fun grantRarity(value: String) = runtime.heroViewModel.grantRarity(value)
    fun grantSlot(value: String) = runtime.heroViewModel.grantSlot(value)
    fun grantRandom() = runtime.heroViewModel.grantRandom()
    fun adjustItems(itemId: String, amount: Long) = runtime.heroViewModel.adjustItems(itemId, amount)
    fun selectOrb(value: String) = runtime.heroViewModel.selectOrb(value)
    fun openForge(instanceId: String?, section: ForgeSection) = runtime.heroViewModel.openForge(instanceId, section)
    fun forgeSection(section: ForgeSection) = runtime.heroViewModel.forgeSection(section)
    fun selectNode(code: String) = runtime.heroViewModel.selectNode(code)
    fun allocateNode(code: String) = runtime.heroViewModel.allocateNode(code)
    fun refundNode(code: String) = runtime.heroViewModel.refundNode(code)
    fun resetTree() = runtime.heroViewModel.resetTree()
    fun addExperience(amount: Double) = runtime.heroViewModel.addExperience(amount)
    fun draftClass(value: String) = runtime.editorViewModel.draftClass(value)
    fun applyOrb(inventoryId: String, orbItemId: String) = runtime.heroViewModel.applyOrb(inventoryId, orbItemId)
    fun craft(inventoryId: String, recipe: String) = runtime.heroViewModel.craft(inventoryId, recipe)
    fun uncraft(inventoryId: String) = runtime.heroViewModel.uncraft(inventoryId)
    fun redeem(code: String) = runtime.heroViewModel.redeem(code)
    /** Puts a jewel into a socket on the tree, and takes it back out. */
    fun socketJewel(inventoryId: String, nodeCode: String) = runtime.heroViewModel.socketJewel(inventoryId, nodeCode)
    fun unsocketJewel(inventoryId: String) = runtime.heroViewModel.unsocketJewel(inventoryId)
    /** Sells an item to a merchant; the price and the refusal are both the server's. */
    fun sellForGold(inventoryId: String) = runtime.heroViewModel.sellForGold(inventoryId)
    fun useRecipe(recipeId: String, ingredients: List<String>, amount: Long) = runtime.heroViewModel.useRecipe(recipeId, ingredients, amount)
    fun mode(mode: AppMode) = runtime.sessionViewModel.mode(mode)
    fun serverDraft(value: String) = runtime.sessionViewModel.serverDraft(value)
    fun connect() = runtime.sessionViewModel.connect()
    fun health() = runtime.sessionViewModel.health()
    fun login(login: String, password: String) = runtime.sessionViewModel.login(login, password)
    fun playOnThisDevice() = runtime.sessionViewModel.playOnThisDevice()
    fun retryResume() = runtime.sessionViewModel.retryResume()
    fun enterCharacter(id: String) = runtime.characterViewModel.enter(id)
    fun leaveGame() = runtime.characterViewModel.leaveGame()
    fun createCharacter(name: String, classId: String) = runtime.characterViewModel.create(name, classId)
    fun deleteCharacter(id: String) = runtime.characterViewModel.delete(id)
    fun refreshCharacters() = runtime.characterViewModel.refresh()
    fun ensureClasses() = runtime.characterViewModel.ensureClasses()
    fun logout() = runtime.sessionViewModel.logout()
    fun changePassword(current: String, replacement: String) = runtime.sessionViewModel.changePassword(current, replacement)
    fun nodeQuery(value: String) = runtime.heroViewModel.nodeQuery(value)
    fun auctionTab(tab: Int) = runtime.auctionViewModel.auctionTab(tab)
    fun auctionFilter(filter: com.sperance.exileforge.core.model.auction.AuctionFilter) = runtime.auctionViewModel.auctionFilter(filter)
    fun showOwnLots(show: Boolean) = runtime.auctionViewModel.showOwnLots(show)
    fun loadAuction() = runtime.auctionViewModel.loadAuction()
    fun loadShowcase(page: Int = 0) = runtime.auctionViewModel.loadShowcase(page)
    fun loadMyLots() = runtime.auctionViewModel.loadMyLots()
    fun buyLot(lotId: String) = runtime.auctionViewModel.buy(lotId)
    fun loadMerchant() = runtime.auctionViewModel.loadMerchant()
    fun buyOffer(offerId: String) = runtime.auctionViewModel.buyOffer(offerId)
    fun buyLotSlot() = runtime.auctionViewModel.buySlot()
    fun cancelLot(lotId: String) = runtime.auctionViewModel.cancel(lotId)
    fun sellEquipment(inventoryId: String, priceOrbId: String, price: Long) = runtime.auctionViewModel.sellEquipment(inventoryId, priceOrbId, price)
    fun sellItem(itemId: String, amount: Long, priceOrbId: String, price: Long) = runtime.auctionViewModel.sellItem(itemId, amount, priceOrbId, price)
    fun runChecks() = runtime.checksViewModel.runChecks()
    fun clearLogs() = runtime.checksViewModel.clearLogs()

    fun loadRedemptions() = runtime.redemptionViewModel.load()
    fun createRedemption(code: com.sperance.exileforge.core.model.command.RedemptionCode) = runtime.redemptionViewModel.create(code)
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
