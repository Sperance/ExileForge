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
    private val hero: HeroActions,
    private val sync: HeroSync,
    repository: ExpeditionRepository,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val state: StateFlow<Expedition> = repository.state
    val run: StateFlow<ExpeditionRun?> = expedition.run
    val arena: StateFlow<TrialArena?> = trial.arena
    val activity: StateFlow<Activity> = commands.state

    fun ensureHero() = sync.ensure()
    fun loadCampaign() = expedition.loadCampaign()
    fun selectZone(mapCode: String) = expedition.selectZone(mapCode)
    fun closeZone() = expedition.closeZone()
    fun pickMap(itemId: String?) = expedition.pickMap(itemId)
    fun pickPotion(code: String?) = expedition.pickPotion(code)
    fun toggleScarab(code: String, add: Boolean) = expedition.toggleScarab(code, add)
    fun startRun(mapCode: String) = expedition.start(MapCode(mapCode))
    fun startAutoRun(mapCode: String, plan: AutoPlan) = expedition.start(MapCode(mapCode), plan)
    fun continueUnfinished() = expedition.continueUnfinished()
    fun abandonUnfinished() = expedition.abandonUnfinished()
    fun runCommand(command: RunCommand) = expedition.send(command)
    fun flushRun() = expedition.flushRun()
    fun enterVaal() = expedition.enterVaal()
    fun closeRun() = expedition.close()
    fun openAtlas() = expedition.openAtlas()
    fun closeAtlas() = expedition.closeAtlas()
    fun selectAtlasNode(code: String) = expedition.selectAtlasNode(code)
    fun allocateAtlas(code: String) = expedition.allocateAtlas(code)
    fun refundAtlas(code: String, regret: Boolean = false) = expedition.refundAtlas(code, regret)
    fun resetAtlas(regret: Boolean = false) = expedition.resetAtlas(regret)
    fun enterRush(region: String) = trial.rush(region)
    fun enterTower() = trial.tower()
    fun forgeRushKey() = trial.forgeKey()
    fun abandonTrial() = trial.abandon()
    fun trialCommand(command: RunCommand) = trial.send(command)
    fun closeTrial() = trial.close()
    fun sellForGold(itemId: String) = hero.sellForGold(itemId)
}
