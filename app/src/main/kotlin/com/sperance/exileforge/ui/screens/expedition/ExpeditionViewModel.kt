package com.sperance.exileforge.ui.screens.expedition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.WorldView
import com.sperance.exileforge.core.campaign.Expedition
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.GearPreview
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.expedition.ExpeditionActions
import com.sperance.exileforge.core.expedition.RiftActions
import com.sperance.exileforge.core.expedition.RiftState
import com.sperance.exileforge.core.expedition.RiftStep
import com.sperance.exileforge.core.expedition.TrialActions
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CallState
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.WorldPoint
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Экраны похода: карта мира, карточка зоны, бег похода, испытания и атлас - над общими действиями. */
class ExpeditionViewModel(
    private val expedition: ExpeditionActions,
    private val trial: TrialActions,
    private val rift: RiftActions,
    repository: ExpeditionRepository,
    commands: CommandRunner,
    slice: GameSlice,
    private val prefs: PreferencesRepository,
    private val navigator: Navigator,
) : ViewModel() {
    /** Где стояла камера карты у героя [heroId]; не стояла - null, карта откроется на фронтире. */
    fun worldView(heroId: String): WorldView? = prefs.worldView(heroId)

    /** Запомнить камеру карты героя [heroId] на устройстве. */
    fun keepWorldView(heroId: String, view: WorldView) {
        viewModelScope.launch { prefs.keepWorldView(heroId, view) }
    }

    /** Точка героя на карте [world]: жетон его зоны ([ExpeditionActions.heroZone]), нет зоны или жетона - граница. */
    fun heroPoint(world: WorldMap): WorldPoint = expedition.heroZone()?.let(world::point) ?: world.frontier()

    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val state: StateFlow<Expedition> = repository.state
    val run: StateFlow<ExpeditionRun?> = expedition.run

    /** Герой в наборе, что носит поход по карте ([ExpeditionActions.runHero]); null - похода нет. */
    val runHero: StateFlow<HeroView?> = expedition.runHero

    /** Добыча основной карты под зоной Ваал. */
    fun outerLoot(): List<String> = expedition.outerLoot()
    val arena: StateFlow<TrialArena?> = trial.arena
    val activity: StateFlow<Activity> = commands.state

    /** Босс на сервере героя: сколько героев с ним дрались и сколько победили. */
    suspend fun bossRecord(code: String) = expedition.bossRecord(code)

    fun loadCampaign() = expedition.loadCampaign()
    fun selectZone(mapCode: String) = expedition.selectZone(mapCode)
    fun closeZone() = expedition.closeZone()
    fun pickMap(itemId: String?) = expedition.pickMap(itemId)
    fun pickPotion(code: String?) = expedition.pickPotion(code)
    fun toggleScarab(code: String, add: Boolean) = expedition.toggleScarab(code, add)

    /** Насильные механики захода - только тестировщику и выше. */
    fun toggleForced(mechanic: MapMechanic) = expedition.toggleForced(mechanic)
    fun forceAll(on: Boolean) = expedition.forceAll(on)

    /** Заход в зону; [toBoss] (тестировщик и выше) - сразу бой со стражем. */
    fun startRun(mapCode: String, toBoss: Boolean = false) = expedition.startRun(mapCode, toBoss)
    fun continueUnfinished() = expedition.continueUnfinished()
    fun abandonUnfinished() = expedition.abandonUnfinished()
    fun runCommand(command: RunCommand) = expedition.send(command)
    fun flushRun() = expedition.flushRun()
    fun toggleSaleMark(itemId: String) = expedition.toggleSaleMark(itemId)

    /** Надеть между боями вещь, что в заходе можно надеть ([ExpeditionActions.wear]). */
    fun wear(itemId: String, slot: Slot? = null) = expedition.wear(itemId, slot)

    /** Что изменит надетая в походе вещь ([ExpeditionActions.wearPreview]); вызывать вне главного потока. */
    fun wearPreview(itemId: String, slot: Slot? = null): GearPreview? = expedition.wearPreview(itemId, slot)
    fun enterVaal() = expedition.enterVaal()

    /** Выход из итогов захода: отмеченное [sell] продаётся, отложенная добыча со снятой отметкой [keep] остаётся. */
    fun closeRun(sell: Collection<String>, keep: Collection<String>) = expedition.closeSettling(sell, keep)
    fun openAtlas() = expedition.openAtlas()

    /** Страница «Похода» экраном под вкладкой; Атлас открывает поход - он прежде готовит своё окно. */
    fun openPage(route: Route) = if (route == Route.Atlas) openAtlas() else navigator.tab(route)
    fun selectAtlasNode(code: String) = expedition.selectAtlasNode(code)
    fun allocateAtlas(code: String) = expedition.allocateAtlas(code)
    fun refundAtlas(code: String, regret: Boolean = false) = expedition.refundAtlas(code, regret)
    fun resetAtlas(regret: Boolean = false) = expedition.resetAtlas(regret)
    fun enterRush(region: String, tier: Int = 0) = trial.rush(region, tier)
    fun enterTower() = trial.tower()
    fun abandonTrial() = trial.abandon()
    fun trialCommand(command: RunCommand) = trial.send(command)
    fun closeTrial() = trial.close()

    /** Разлом недели. */
    val riftState: StateFlow<RiftState> = rift.state

    /** Шаг Разлома в пути: ожидание, «нет связи», «связь восстановлена». */
    val riftStep: StateFlow<CallState<RiftStep>> = rift.step

    /** Итог боя Разлома в пути. */
    val riftDelivery: StateFlow<CallState<RiftAct.Fight>> = rift.delivery

    /** Кнопки забега живы ([RiftActions.canAct]). */
    val riftCanAct: StateFlow<Boolean> = rift.canAct
    fun retryRiftStep() = rift.retryStep()
    fun openRift() = rift.open()
    fun closeRift() = rift.closeGate()
    fun startRift() = rift.start()
    fun riftAct(act: RiftAct) = rift.act(act)
    fun riftGo(node: Int) = rift.go(node)
    fun leaveRift() = rift.leave()
    fun riftCommand(command: RunCommand) = rift.send(command)
    fun dismissRiftResult() = rift.dismissResult()
    fun dismissRiftToll() = rift.dismissToll()
    fun openRiftRecap() = rift.openRecap()
    fun closeRiftRecap() = rift.closeRecap()

    fun leaveRiftRefused() = rift.leaveRefused()
}
