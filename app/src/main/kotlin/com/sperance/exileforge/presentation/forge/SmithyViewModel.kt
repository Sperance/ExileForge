package com.sperance.exileforge.presentation.forge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.rules.content.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Что выбрано в кузнице: сфера, эссенция, предзнаменование к сфере и открытый раздел. Сфера переживает смену героя. */
data class Smithy(
    val orb: String = "",
    val essence: String = "",
    val omen: String = "",
    val section: ForgeSection = ForgeSection.ORBS,
)

/** Кузница (3.80.18): выбор инструмента - состояние экрана, команды - общие действия героя; предмет под кузницей - в репозитории героя. */
class SmithyViewModel(
    private val hero: HeroActions,
    private val sync: HeroSync,
    world: WorldRepository,
    commands: CommandRunner,
) : ViewModel() {
    private val mutable = MutableStateFlow(Smithy())

    /** Выбор как он есть; пока сфера не выбрана - самая дешёвая валюта контента, когда он прочитан. */
    val smithy: StateFlow<Smithy> = combine(mutable, world.state) { chosen, w ->
        if (chosen.orb.isNotBlank()) chosen else chosen.copy(orb = w.content?.itemsByCategory?.get(Item.CURRENCY)?.minByOrNull { it.price }?.code.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Smithy())
    val activity: StateFlow<Activity> = commands.state

    fun ensure() = sync.ensure()
    fun selectEquipment(itemId: String) = hero.selectEquipment(itemId)

    /** Кузница над одним предметом на разделе, за которым пришёл игрок; `null` оставляет её предмет. */
    fun open(itemId: String?, section: ForgeSection) {
        itemId?.let(hero::selectEquipment)
        section(section)
    }

    fun section(section: ForgeSection) = mutable.update { it.copy(section = section) }

    /** Новая сфера - без предзнаменования: оно подбирается к сфере. */
    fun selectOrb(code: String) = mutable.update { it.copy(orb = code, omen = "") }
    fun selectOmen(code: String) = mutable.update { it.copy(omen = code) }
    fun selectEssence(code: String) = mutable.update { it.copy(essence = code) }

    /** Сфера тратится вместе с предзнаменованием: следующее применение начинается без него. */
    fun applyOrb(itemId: String, orb: String) = hero.applyOrb(itemId, orb, smithy.value.omen) { mutable.update { it.copy(omen = "") } }
    fun applyEssence(itemId: String, essence: String) = hero.applyEssence(itemId, essence)
    fun unveil(itemId: String, choice: Int) = hero.unveil(itemId, choice)
    fun choose(itemId: String, choice: Int) = hero.choose(itemId, choice)
    fun craft(itemId: String, recipe: String) = hero.craft(itemId, recipe)
    fun uncraft(itemId: String) = hero.uncraft(itemId)
}
