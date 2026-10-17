package com.sperance.exileforge.ui.screens.expedition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.WorldView
import com.sperance.exileforge.core.campaign.Expedition
import com.sperance.exileforge.core.campaign.ExpeditionActions
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.campaign.RiftActions
import com.sperance.exileforge.core.campaign.RiftState
import com.sperance.exileforge.core.campaign.TrialActions
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.rules.content.MapCode
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
    private val hero: HeroActions,
    repository: ExpeditionRepository,
    commands: CommandRunner,
    slice: GameSlice,
    private val prefs: PreferencesRepository,
    private val navigator: Navigator,
) : ViewModel() {
    /** Где стояла камера карты у героя [heroId]; не стояла - null, карта откроется на фронтире. */
    fun worldView(heroId: String): WorldView? = prefs.settings.value.worldViews[heroId]

    /** Запомнить камеру карты героя [heroId] на устройстве: только настройки, сервер не знает. */
    fun keepWorldView(heroId: String, view: WorldView) {
        if (heroId.isBlank()) return
        viewModelScope.launch { prefs.editSettings { copy(worldViews = worldViews + (heroId to view)) } }
    }

    /** Срез «игра» для экранов этой модели. */
    val game: StateFlow<GameUi> = slice.ui
    val state: StateFlow<Expedition> = repository.state
    val run: StateFlow<ExpeditionRun?> = expedition.run

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
    fun toggleForced(mechanic: MapMechanic) {
        if (game.value.isTester) expedition.toggleForced(mechanic)
    }

    fun forceAll(on: Boolean) {
        if (game.value.isTester) expedition.forceAll(on)
    }

    /** Заход в зону; [toBoss] (тестировщик и выше) - сразу бой со стражем. */
    fun startRun(mapCode: String, toBoss: Boolean = false) = expedition.start(MapCode(mapCode), toBoss = toBoss && game.value.isTester)
    fun continueUnfinished() = expedition.continueUnfinished()
    fun abandonUnfinished() = expedition.abandonUnfinished()
    fun runCommand(command: RunCommand) = expedition.send(command)
    fun flushRun() = expedition.flushRun()
    fun toggleSaleMark(itemId: String) = expedition.toggleSaleMark(itemId)
    fun enterVaal() = expedition.enterVaal()
    fun closeRun() = expedition.close()

    /** «Продать и вернуться»: заход закрывается и на сервере, затем отмеченная добыча продаётся пачкой. */
    fun closeRunSelling(itemIds: Collection<String>) = expedition.close { id -> hero.sellBatch(id, itemIds) }
    fun openAtlas() = expedition.openAtlas()

    /** Страница «Похода» экраном под вкладкой; Атлас открывает поход - он прежде готовит своё окно. */
    fun openPage(route: Route) = if (route == Route.Atlas) openAtlas() else navigator.tab(route)
    fun selectAtlasNode(code: String) = expedition.selectAtlasNode(code)
    fun allocateAtlas(code: String) = expedition.allocateAtlas(code)
    fun refundAtlas(code: String, regret: Boolean = false) = expedition.refundAtlas(code, regret)
    fun resetAtlas(regret: Boolean = false) = expedition.resetAtlas(regret)
    fun enterRush(region: String, tier: Int = 0) = trial.rush(region, tier)
    fun enterTower() = trial.tower()
    fun forgeRushKey() = trial.forgeKey()
    fun abandonTrial() = trial.abandon()
    fun trialCommand(command: RunCommand) = trial.send(command)
    fun closeTrial() = trial.close()

    /** Разлом недели. */
    val riftState: StateFlow<RiftState> = rift.state
    fun openRift() = rift.open()
    fun closeRift() = rift.close()
    fun refreshRift() = rift.refresh()
    fun startRift() = rift.start()
    fun riftAct(act: com.sperance.exileforge.rules.rift.RiftAct) = rift.act(act)
    fun riftCommand(command: RunCommand) = rift.send(command)
    fun dismissRiftResult() = rift.dismissResult()

    fun leaveRiftRefused() = rift.leaveRefused()
}
