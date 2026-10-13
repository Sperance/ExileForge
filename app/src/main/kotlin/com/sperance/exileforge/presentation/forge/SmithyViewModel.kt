package com.sperance.exileforge.presentation.forge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Что выбрано в кузнице: сфера, печать Атласа, эссенция, предзнаменование к сфере или печати и открытый раздел. Сфера переживает смену героя. */
data class Smithy(
    val orb: String = "",
    /** Печать Атласа (4.6.4) - раздел печатей карты. */
    val seal: String = "",
    val essence: String = "",
    val omen: String = "",
    val section: ForgeSection = ForgeSection.ORBS,
    /** Цель, под которую выбрано знамение (4.3.0): знамение подбирается к вещи, на другой его нет. */
    val omenTarget: String = "",
)

/** Кузница (3.80.18): выбор инструмента - состояние экрана, команды - общие действия героя; предмет под кузницей - в репозитории героя. */
class SmithyViewModel(
    private val hero: HeroActions,
    private val sync: HeroSync,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    private val mutable = MutableStateFlow(Smithy())

    /** Выбор как он есть. Знамение держится только над целью, под которую его выбрали (4.3.0): сменилась цель - знамения нет. */
    val smithy: StateFlow<Smithy> = combine(mutable, game.map { it.holding.selectedEquipment }.distinctUntilChanged()) { chosen, item ->
        if (chosen.omenTarget == item) chosen else chosen.copy(omen = "", omenTarget = "")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Smithy())
    val activity: StateFlow<Activity> = commands.state

    fun ensure() = sync.ensure()

    /** Другая цель под кузницей (4.4.x); прежняя фраза наковальни уходит вместе с прежней целью. */
    fun select(target: ForgeTarget) {
        if (target.id == game.value.holding.selectedEquipment) return
        hero.clearForgeLine()
        selectEquipment(target.id)
    }

    /** Вещь под кузницей; знамение, выбранное под прежнюю цель, снимается (4.3.0). */
    fun selectEquipment(itemId: String) = hero.selectEquipment(itemId)

    /** Кузница над одним предметом на разделе, за которым пришёл игрок; `null` оставляет её предмет. */
    fun open(itemId: String?, section: ForgeSection) {
        itemId?.let(::selectEquipment)
        mutable.update { it.copy(section = section) }
    }

    /** Другой раздел: знамение подбиралось к валюте прежнего раздела - оно снимается. */
    fun section(section: ForgeSection) = mutable.update { if (it.section == section) it else it.copy(section = section, omen = "", omenTarget = "") }

    /** Новая сфера - без предзнаменования: оно подбирается к сфере. */
    fun selectOrb(code: String) = mutable.update { it.copy(orb = code, omen = "", omenTarget = "") }
    fun selectSeal(code: String) = mutable.update { it.copy(seal = code, omen = "", omenTarget = "") }
    fun selectOmen(code: String) = mutable.update { it.copy(omen = code, omenTarget = game.value.holding.selectedEquipment) }
    fun selectEssence(code: String) = mutable.update { it.copy(essence = code) }

    /** Сфера тратится вместе с предзнаменованием: следующее применение начинается без него. [line] - строка Сферы гравёра (4.11.x). */
    fun applyOrb(target: ForgeTarget, orb: String, line: Int? = null) = applyOrb(target.id, orb, line)

    /** Сфера на вещь [itemId] (и с панели администратора, где цель - любая вещь героя). */
    fun applyOrb(itemId: String, orb: String, line: Int? = null) = hero.applyOrb(itemId, orb, smithy.value.omen, ::omenSpent, line)

    /** Печать Атласа на карту [itemId] (4.6.4): знамение тратится вместе с ней. */
    fun applySeal(itemId: String, seal: String) = hero.applySeal(itemId, seal, smithy.value.omen, ::omenSpent)

    private fun omenSpent() = mutable.update { it.copy(omen = "", omenTarget = "") }
    fun applyEssence(itemId: String, essence: String) = hero.applyEssence(itemId, essence)
    fun unveil(itemId: String, choice: Int) = hero.unveil(itemId, choice)
    fun choose(itemId: String, choice: Int) = hero.choose(itemId, choice)
    fun craft(itemId: String, recipe: String) = hero.craft(itemId, recipe)
    fun uncraft(itemId: String) = hero.uncraft(itemId)
}
