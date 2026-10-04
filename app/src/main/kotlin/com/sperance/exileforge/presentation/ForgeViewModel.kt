package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.campaign.run.AutoPlan
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.Feature
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.presentation.state.StashSort
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_SETTINGS
import com.sperance.exileforge.presentation.state.unlocked
import com.sperance.exileforge.rules.content.GuildMode
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lifecycle owner and compatibility facade; screen actions live in feature models. */
class ForgeViewModel(private val runtime: ForgeRuntime) : ViewModel() {
    init {
        runtime.start()
    }

    /** What the phone buzzes for (3.77.0), already filtered by the settings. */
    val buzzes: kotlinx.coroutines.flow.SharedFlow<Buzz> = runtime.buzzes
    fun buzz(kind: Buzz) = runtime.buzz(kind)
    val logs = runtime.logs

    /** Вкладка по прежнему номеру; закрытую уровнем героя навигатор не откроет и скажет, с какого. */
    fun tab(tab: Int) = runtime.tab(tab)

    /** «Настройки» (3.77.0) поверх открытой вкладки; закрытие возвращает на неё. */
    fun openSettings() {
        runtime.commands.dismissMessage()
        runtime.navigator.open(com.sperance.exileforge.presentation.nav.Route.Settings)
    }
    fun closeSettings() = runtime.navigator.back()

    /** A line in the toasts, from the screens (3.76.0: a place opened by the level). */
    fun announce(text: String) = runtime.toast(text)

    /** The campaign run on screen, if any: a world the scene steps and the overlay reads. */
    val expedition = runtime.expedition.run
    fun loadCampaign() = runtime.expedition.loadCampaign()
    fun startRun(mapCode: String) = runtime.expedition.start(mapCode)
    fun startAutoRun(mapCode: String, plan: AutoPlan) = runtime.expedition.start(mapCode, plan)
    fun selectZone(mapCode: String) = runtime.expedition.selectZone(mapCode)
    fun loadCrafts(silent: Boolean = false) = runtime.crafts.load(silent)

    /** A bug report from the beetle (3.48.0): sent at once, whether signed in or not. */
    fun reportBug(report: com.sperance.exileforge.core.model.command.BugReportRequest, onSent: suspend () -> Unit = {}) = runtime.reportBug(report, onSent)
    fun planTree(nodes: List<com.sperance.exileforge.rules.content.TakenNode>) = runtime.hero.planTree(nodes)
    fun autoSell(rarity: com.sperance.exileforge.rules.content.Rarity, groups: Set<com.sperance.exileforge.rules.content.SlotGroup>) = runtime.hero.autoSell(rarity, groups)
    fun closeZone() = runtime.expedition.closeZone()
    fun pickMap(itemId: String?) = runtime.expedition.pickMap(itemId)
    fun pickPotion(code: String?) = runtime.expedition.pickPotion(code)
    fun toggleScarab(code: String, add: Boolean) = runtime.expedition.toggleScarab(code, add)

    /** The trial on screen (3.49.0), if any: an arena the screen steps and reads. */
    val trial = runtime.trial.arena

    /** The warm-up on entering a hero (3.54.0). */
    fun warmUp() = runtime.warmupViewModel.start()
    fun enterRush(region: String) = runtime.trial.rush(region)
    fun enterTower() = runtime.trial.tower()
    fun forgeRushKey() = runtime.trial.forgeKey()
    fun abandonTrial() = runtime.trial.abandon()

    /** The run's journal goes out now: the app leaves the foreground. */
    fun flushRun() = runtime.expedition.flushRun()
    fun openAtlas() = runtime.expedition.openAtlas()
    fun language(lang: Lang) = runtime.language(lang)
    fun refreshLocale() = runtime.refreshLocale()
    fun refreshIcons() = runtime.refreshIcons()
    fun dismissMessage() = runtime.dismissMessage()
    fun dismissNotice() = runtime.dismissNotice()
    fun selectEquipment(value: String) = runtime.heroes.selectEquipment(value)

    fun learnSkill(code: String) = runtime.hero.learnSkill(code)
    fun openChest(code: String) = runtime.hero.openChest(code)
    fun dismissChest() = runtime.hero.dismissChest()
    fun slotSkill(kind: String, index: Int, code: String?, condition: String? = null) = runtime.hero.slotSkill(kind, index, code, condition)
    fun flaskCondition(index: Int, condition: String?) = runtime.hero.flaskCondition(index, condition)
    fun exchangeBooks(books: List<String>, code: String) = runtime.hero.exchangeBooks(books, code)
    fun ensureHero() = runtime.heroSync.ensure()
    fun equip(itemId: String, slot: Slot? = null) = runtime.hero.equip(itemId, slot)
    fun unequip(itemId: String) = runtime.hero.unequip(itemId)
    fun expandStash() = runtime.hero.expandStash()
    fun claimPath() = runtime.hero.claimPath()
    fun temper(itemId: String) = runtime.hero.temper(itemId)
    fun setTitle(title: String) = runtime.hero.setTitle(title)

    /** The hero's statistics (3.51.0), read when the chronicle opens; null when the read failed. */
    suspend fun heroStats(heroId: String): Map<String, Long>? = runCatching { runtime.api.hero.stats(heroId).values }.getOrNull()
    fun claimOverflow(itemId: String? = null) = runtime.hero.claimOverflow(itemId)
    fun sellOverflow(itemId: String) = runtime.hero.sellOverflow(itemId)
    fun incubatePet(egg: String, slot: Int? = null) = runtime.hero.incubatePet(egg, slot)
    fun collectPet(slot: Int) = runtime.hero.collectPet(slot)
    fun petOrb(petId: String, orb: String, omen: String? = null) = runtime.hero.petOrb(petId, orb, omen)
    fun choosePetLine(petId: String, choice: Int) = runtime.hero.choosePetLine(petId, choice)
    fun activatePet(petId: String) = runtime.hero.activatePet(petId)
    fun releasePet(petId: String) = runtime.hero.releasePet(petId)
    fun breedPets(first: String, second: String) = runtime.hero.breedPets(first, second)

    /** Admin only: a named template, rolled by the server at [rarity] or the template's own. */
    fun grant(template: String, rarity: Rarity? = null) = runtime.hero.grant(template, rarity)

    /** Случайная вещь администратору: редкость и слот с панели, пусто - любые. */
    fun grantRandom(rarity: String, slot: String) = runtime.hero.grantRandom(rarity, slot)

    /** Admin only: a stack into the bag, by the item's code. */
    fun grantItem(code: String, amount: Long) = runtime.hero.grantItem(code, amount)
    fun allocateNode(code: String, choice: Int? = null) = runtime.hero.allocateNode(code, choice)
    fun allocatePath(code: String, choice: Int? = null) = runtime.hero.allocatePath(code, choice)
    fun refundNode(code: String) = runtime.hero.refundNode(code)
    fun refundBranch(code: String) = runtime.hero.refundBranch(code)
    fun rechooseNode(code: String, choice: Int) = runtime.hero.rechooseNode(code, choice)
    fun resetTree() = runtime.hero.resetTree()
    fun addExperience(amount: Double) = runtime.hero.addExperience(amount)

    fun redeem(code: String) = runtime.hero.redeem(code)
    fun socketJewel(itemId: String, nodeCode: String) = runtime.hero.socketJewel(itemId, nodeCode)
    fun unsocketJewel(itemId: String) = runtime.hero.unsocketJewel(itemId)
    fun sellForGold(itemId: String) = runtime.hero.sellForGold(itemId)

    /** The item lock (3.30.0): a locked item is never sold, listed or auto-sold. */
    fun lockItem(itemId: String, locked: Boolean) = runtime.hero.lockItem(itemId, locked)

    /** «Пока вас не было» (3.69.0): the last catch-up shown for a hero, and marking one shown. */
    suspend fun craftsAwaySeen(heroId: String): Long = runtime.store.craftsAwaySeen(runtime.sessions.state.value.server, heroId)
    fun markCraftsAwaySeen(heroId: String, until: Long) {
        val server = runtime.sessions.state.value.server
        runtime.scope.launch { runtime.store.saveCraftsAwaySeen(server, heroId, until) }
    }

    /** The link's probe at once (3.30.0): the offline icon tapped. */
    fun retryLink() = runtime.connectionViewModel.wake(now = true)
    fun mode(mode: AppMode) = runtime.sessionViewModel.mode(mode)
    fun loadTesters() = runtime.sessionViewModel.loadTesters()
    fun createTester(login: String) = runtime.sessionViewModel.createTester(login)
    fun resetTester(id: String) = runtime.sessionViewModel.resetTester(id)
    fun setTesterActive(id: String, active: Boolean) = runtime.sessionViewModel.setTesterActive(id, active)
    fun closeShownTester() = runtime.sessionViewModel.closeShownTester()
    fun testerGrant(what: String, vararg params: Pair<String, String?>) = runtime.hero.testerGrant(what, *params)
    fun resetServer() = runtime.sessionViewModel.resetServer()
    fun health() = runtime.sessionViewModel.health()
    fun login(login: String, password: String) = runtime.sessionViewModel.login(login, password)
    fun playOnThisDevice() = runtime.sessionViewModel.playOnThisDevice()
    fun retryResume() = runtime.sessionViewModel.retryResume()

    /** The live server's manifest for the update check (3.72.0); null while no server answers. */
    suspend fun serverManifest(): com.sperance.exileforge.core.model.sync.StaticManifest? = try {
        // 3.74.0: after the app has its server, and as that server serves it now - a kept manifest of an older deploy hid every update.
        kotlinx.coroutines.withTimeoutOrNull(API_WAIT_MS) { runtime.apiReady.await() }
        runtime.api.liveManifest()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    /** A sign-in met a newer server (3.74.0). */
    val newerServer: kotlinx.coroutines.flow.Flow<Unit> get() = runtime.newerServer
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

    /** A building of the City (3.22.0), or the square for none. */
    fun building(building: Building?) {
        runtime.commands.dismissMessage()
        runtime.navigator.tab(com.sperance.exileforge.presentation.nav.Route.ofBuilding(building))
    }

    fun clearLogs() = runtime.journal.clear()
    fun loadRedemptions() = runtime.redemptionViewModel.load()
    fun createRedemption(code: RedemptionCode) = runtime.redemptionViewModel.create(code)
    fun deleteRedemption(id: String) = runtime.redemptionViewModel.delete(id)
    override fun onCleared() {
        runtime.close()
    }
}

/** How long the update check waits for the app to have its server. */
private const val API_WAIT_MS = 10_000L
