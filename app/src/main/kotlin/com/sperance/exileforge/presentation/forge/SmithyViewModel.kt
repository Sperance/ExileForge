package com.sperance.exileforge.presentation.forge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Что выбрано в кузнице: сфера, эссенция, предзнаменование к сфере и открытый раздел. Сфера переживает смену героя. */
data class Smithy(
    val orb: String = "",
    val essence: String = "",
    val omen: String = "",
    val section: ForgeSection = ForgeSection.ORBS,
    /** Питомец под кузницей (3.81.0): сферы питомцев тратятся только здесь; пустая строка - кузница над предметом. */
    val pet: String = "",
    /** Кузница над питомцем, а не над предметом (3.81.0). */
    val petMode: Boolean = false,
    /** Вещь, под которую выбрано знамение (4.2.1): катализатор подбирается к вещи, на другой его нет. */
    val omenItem: String = "",
)

/** Кузница (3.80.18): выбор инструмента - состояние экрана, команды - общие действия героя; предмет под кузницей - в репозитории героя. */
class SmithyViewModel(
    private val hero: HeroActions,
    private val sync: HeroSync,
    world: WorldRepository,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    private val mutable = MutableStateFlow(Smithy())

    /**
     * Выбор как он есть; пока сфера не выбрана - самая дешёвая валюта контента, когда он прочитан. Знамение держится только над
     * вещью, под которую его выбрали (4.2.1): сменилась вещь под кузницей - знамения нет.
     */
    val smithy: StateFlow<Smithy> = combine(mutable, world.state, game.map { it.holding.selectedEquipment }.distinctUntilChanged()) { chosen, w, item ->
        val omened = if (chosen.omenItem == item) chosen else chosen.copy(omen = "", omenItem = "")
        if (omened.orb.isNotBlank()) omened else omened.copy(orb = w.content?.itemsByCategory?.get(Item.CURRENCY)?.minByOrNull { it.price }?.code?.value.orEmpty())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Smithy())
    val activity: StateFlow<Activity> = commands.state

    fun ensure() = sync.ensure()

    /** Другая вещь под кузницей - знамение, выбранное под прежнюю, снимается (4.2.1). */
    fun selectEquipment(itemId: String) {
        hero.selectEquipment(itemId)
        mutable.update { if (it.omenItem == itemId) it else it.copy(omen = "", omenItem = "") }
    }

    /** Кузница над одним предметом на разделе, за которым пришёл игрок; `null` оставляет её предмет. */
    fun open(itemId: String?, section: ForgeSection) {
        itemId?.let(::selectEquipment)
        mutable.update { it.copy(section = section, petMode = false) }
    }

    /** Кузница над питомцем (3.81.0): из Зверинца кнопкой «В кузницу». */
    fun openPet(petId: String) = mutable.update { it.copy(pet = petId, petMode = true) }
    fun petMode(on: Boolean) = mutable.update { it.copy(petMode = on) }
    fun selectPet(petId: String) = mutable.update { it.copy(pet = petId) }
    fun petOrb(petId: String, orb: String, omen: String? = null) = hero.petOrb(petId, orb, omen)
    fun choosePetLine(petId: String, choice: Int) = hero.choosePetLine(petId, choice)

    fun section(section: ForgeSection) = mutable.update { it.copy(section = section) }

    /** Новая сфера - без предзнаменования: оно подбирается к сфере. */
    fun selectOrb(code: String) = mutable.update { it.copy(orb = code, omen = "", omenItem = "") }
    fun selectOmen(code: String) = mutable.update { it.copy(omen = code, omenItem = game.value.holding.selectedEquipment) }
    fun selectEssence(code: String) = mutable.update { it.copy(essence = code) }

    /** Сфера тратится вместе с предзнаменованием: следующее применение начинается без него. */
    fun applyOrb(itemId: String, orb: String) = hero.applyOrb(itemId, orb, smithy.value.omen) { mutable.update { it.copy(omen = "", omenItem = "") } }
    fun applyEssence(itemId: String, essence: String) = hero.applyEssence(itemId, essence)
    fun unveil(itemId: String, choice: Int) = hero.unveil(itemId, choice)
    fun choose(itemId: String, choice: Int) = hero.choose(itemId, choice)
    fun craft(itemId: String, recipe: String) = hero.craft(itemId, recipe)
    fun uncraft(itemId: String) = hero.uncraft(itemId)
}
