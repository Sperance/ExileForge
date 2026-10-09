package com.sperance.exileforge.presentation.forge

import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.QualityForecast
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget

/** Верстак берёт модификатор только с волшебной редкости (2.51.0), как правила: у обычной нет мест аффиксов, у закреплённой они закрыты. */
private val BENCHABLE = setOf(Rarity.MAGIC, Rarity.RARE)

/** Эссенция (2.78.0) делает обычную вещь редкой или перебрасывает редкую. */
private val ESSENTIAL = setOf(Rarity.COMMON, Rarity.RARE)

/**
 * Над чем работает кузница (4.4.x) - зеркало правил [OrbTarget]: вещь или питомец. Наковальня, лоток сфер и полоса с удерживаемой
 * кнопкой одни на обе цели; что у цели своё (разделы, свои сферы, прогноз качества), говорит она сама.
 */
sealed interface ForgeTarget {
    /** Id копии вещи или питомца. */
    val id: String

    /** Та же цель для правил: что примет сфера, решает [OrbApplier] над ней. */
    val orbTarget: OrbTarget

    /** Осквернённую цель кузница не меняет. */
    val corrupted: Boolean

    /** Разделы кузницы над целью, сферы - первыми. */
    val sections: List<ForgeSection>

    /** Ключ словаря: в сумке нет сфер, что лягут на цель. */
    val noOrbsKey: String

    /** Ключ словаря: подпись гнезда цели на наковальне. */
    val socketKey: String

    /** Ключ словаря: кнопка карточки цели, что открывает выбор другой. */
    val changeKey: String

    /** Свои сферы цели сверх сфер ремесла, что на ней сейчас что-то сделают (рост питомца); у вещи своих нет. */
    fun ownOrbs(index: ContentIndex): List<String> = emptyList()

    /** Что поднимет сфера качества со знамением [omen] и сколько качества другого вида сбросится; null - прогноза нет. */
    fun quality(index: ContentIndex, omen: Omen?): QualityForecast? = null

    /** Уникалки, что может дать сфера удачи, - тот же список правил, что тянет сервер. */
    fun chanceUniques(index: ContentIndex): List<String> = emptyList()

    /** Вещь под кузницей: сферы, верстак (не у карты, с волшебной редкости) и эссенции (только снаряжение). */
    data class Gear(val view: ItemView) : ForgeTarget {
        override val id: String get() = view.id
        override val orbTarget: OrbTarget get() = OrbTarget.Gear(view.item, view.template)
        override val corrupted: Boolean get() = view.corrupted
        override val noOrbsKey: String get() = "forge.no_orbs_fit"
        override val changeKey: String get() = "forge.change_target"
        override val socketKey: String get() = "forge.socket_item"

        override val sections: List<ForgeSection>
            get() {
                val slot = view.slot
                val benchable = slot != Slot.MAP && view.rarity in BENCHABLE
                val essential = !slot.isJewelLike && !slot.isFlask && !slot.isTool && view.rarity in ESSENTIAL
                return listOfNotNull(ForgeSection.ORBS, ForgeSection.BENCH.takeIf { benchable }, ForgeSection.ESSENCES.takeIf { essential })
            }

        override fun quality(index: ContentIndex, omen: Omen?): QualityForecast? = QualityForecast.of(index, view.item, view.template, omen)

        override fun chanceUniques(index: ContentIndex): List<String> = OrbApplier(index).chanceUniques(view.item, view.template).map { it.value.code }
    }

    /** Питомец под кузницей (3.81.0): только сферы - ремесла, как у вещи, и свои сферы роста ([Menagerie.orb]). */
    data class Beast(val pet: Pet) : ForgeTarget {
        override val id: String get() = pet.id
        override val orbTarget: OrbTarget get() = OrbTarget.Beast(pet)
        override val corrupted: Boolean get() = pet.corrupted
        override val sections: List<ForgeSection> get() = listOf(ForgeSection.ORBS)
        override val noOrbsKey: String get() = "pets.no_orbs"
        override val changeKey: String get() = "forge.change_pet"
        override val socketKey: String get() = "forge.target_pet"

        override fun ownOrbs(index: ContentIndex): List<String> {
            val menagerie = Menagerie(index)
            return index.pets.orbs.keys.filter { code -> menagerie.orb(code)?.let { menagerie.apply(it, pet, Dice(0)) } != null }
        }
    }
}

/** Цель кузницы героя: выбранный питомец [pet], пока он есть, иначе вещь под кузницей; null - выбирать не из чего. */
fun GameUi.forgeTarget(pet: String): ForgeTarget? = hero?.pets?.pets?.firstOrNull { it.id == pet }?.let(ForgeTarget::Beast)
    ?: hero?.item(holding.selectedEquipment)?.let { view(it) }?.let(ForgeTarget::Gear)

/** Сфера лотка кузницы: код стопки и ждёт ли она знамения - сама цель её не примет, со знамением из сумки примет. */
data class OrbChoice(val code: String, val needsOmen: Boolean)

/**
 * Сферы сумки, что лягут на цель (4.4.x): [held] - сколько стопки в сумке, [shelf] - коды сфер ремесла в порядке полки, [omens] -
 * знамения сумки. Сфера ремесла, что цель не примет ни сама, ни со знамением, в лоток не попадает; сожаление тратится на древе.
 */
fun ForgeTarget.orbChoices(index: ContentIndex, shelf: List<String>, omens: List<Omen>, held: (String) -> Long): List<OrbChoice> {
    val applier = OrbApplier(index)
    val target = orbTarget
    val crafting = shelf.mapNotNull { code -> Orb.of(code)?.takeIf { it != Orb.ORB_OF_REGRET && held(code) > 0 } }.mapNotNull { orb ->
        when {
            applier.accepts(orb, target) -> OrbChoice(orb.name, needsOmen = false)
            omens.any { it.fits(orb) && applier.accepts(orb, target, it) } -> OrbChoice(orb.name, needsOmen = true)
            else -> null
        }
    }
    return crafting + ownOrbs(index).filter { held(it) > 0 }.map { OrbChoice(it, needsOmen = false) }
}
