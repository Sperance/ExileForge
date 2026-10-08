package com.sperance.exileforge.presentation.expedition

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.campaign.Expedition
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.run.AutoPlan
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.MapCode
import kotlinx.coroutines.flow.StateFlow

/** Экраны похода (3.80.21): карта мира, карточка зоны, бег похода, испытания и атлас - над общими действиями. */
class ExpeditionViewModel(
    private val expedition: ExpeditionActions,
    private val trial: TrialActions,
    private val rift: RiftActions,
    private val hero: HeroActions,
    repository: ExpeditionRepository,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val state: StateFlow<Expedition> = repository.state
    val run: StateFlow<ExpeditionRun?> = expedition.run

    /** Добыча основной карты под зоной Ваал (3.94.0). */
    fun outerLoot(): List<String> = expedition.outerLoot()
    val arena: StateFlow<TrialArena?> = trial.arena
    val activity: StateFlow<Activity> = commands.state

    /** Босс на сервере героя (3.92.0): сколько героев с ним дрались и сколько победили. */
    suspend fun bossRecord(code: String) = expedition.bossRecord(code)

    fun loadCampaign() = expedition.loadCampaign()
    fun selectZone(mapCode: String) = expedition.selectZone(mapCode)
    fun closeZone() = expedition.closeZone()
    fun pickMap(itemId: String?) = expedition.pickMap(itemId)
    fun pickPotion(code: String?) = expedition.pickPotion(code)
    fun toggleScarab(code: String, add: Boolean) = expedition.toggleScarab(code, add)

    /** Заход в зону; [toBoss] (3.92.0, тестировщик и выше) - сразу бой со стражем. */
    fun startRun(mapCode: String, toBoss: Boolean = false) = expedition.start(MapCode(mapCode), toBoss = toBoss && game.value.isTester)
    fun startAutoRun(mapCode: String) = expedition.start(MapCode(mapCode), AutoPlan)
    fun continueUnfinished() = expedition.continueUnfinished()
    fun abandonUnfinished() = expedition.abandonUnfinished()
    fun runCommand(command: RunCommand) = expedition.send(command)
    fun flushRun() = expedition.flushRun()
    fun toggleSaleMark(itemId: String) = expedition.toggleSaleMark(itemId)
    fun enterVaal() = expedition.enterVaal()
    fun closeRun() = expedition.close()

    /** «Продать и вернуться» (3.90.4): заход закрывается и на сервере, затем отмеченная добыча продаётся пачкой. */
    fun closeRunSelling(itemIds: Collection<String>) = expedition.close { id -> hero.sellBatch(id, itemIds) }
    fun openAtlas() = expedition.openAtlas()
    fun closeAtlas() = expedition.closeAtlas()
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

    /** Разлом недели (3.96.0). */
    val riftState: StateFlow<RiftState> = rift.state
    fun openRift() = rift.open()
    fun closeRift() = rift.close()
    fun refreshRift() = rift.refresh()
    fun startRift() = rift.start()
    fun riftAct(act: com.sperance.exileforge.rules.rift.RiftAct) = rift.act(act)
    fun riftCommand(command: RunCommand) = rift.send(command)
    fun dismissRiftResult() = rift.dismissResult()

    /** Таблица испытаний (3.96.0); null - закрыть. */
    fun trialTable(board: com.sperance.exileforge.rules.content.TrialBoard?, scope: String = "", league: Int? = null) = rift.table(board, scope, league)
}
