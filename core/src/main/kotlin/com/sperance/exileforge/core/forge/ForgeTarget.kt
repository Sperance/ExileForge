package com.sperance.exileforge.core.forge

import com.sperance.exileforge.core.app.ForgeSection
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.character.view
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.QualityForecast
import com.sperance.exileforge.core.i18n.refusalText
import com.sperance.exileforge.rules.Refusal
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.Omen
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.AtlasSeals
import com.sperance.exileforge.rules.roll.OrbApplier
import com.sperance.exileforge.rules.roll.OrbTarget

/** Верстак берёт модификатор только с волшебной редкости, как правила: у обычной нет мест аффиксов, у закреплённой они закрыты. */
private val BENCHABLE = setOf(Rarity.MAGIC, Rarity.RARE)

/** Эссенция делает обычную вещь редкой; только обычную, редкую не перековывает (`OrbApplier`). */
private val ESSENTIAL = setOf(Rarity.COMMON)

/**
 * Над чем работает кузница - зеркало правил [OrbTarget]: вещь. Наковальня, лоток сфер и полоса с удерживаемой кнопкой
 * не знают вида цели; что у цели своё (разделы, свои сферы, прогноз качества), говорит она сама.
 */
sealed interface ForgeTarget {
    /** Id копии вещи. */
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

    /** Уровень предмета копии до действия: по нему сбор золотом за ремесло (`CraftFeeRules` правил). */
    val itemLevel: Int

    /** Печати Атласа сумки, что лягут на цель: сами или со знамением сумки [omens]; лягут только на карту. */
    fun seals(index: ContentIndex, omens: List<Omen>, held: (String) -> Long): List<OrbChoice> = emptyList()

    /** Знамения сумки [held], что меняют печать [seal] на цели. */
    fun sealOmens(index: ContentIndex, seal: String, held: List<Omen>): List<Omen> = emptyList()

    /** Что поднимет сфера качества со знамением [omen] и сколько качества другого вида сбросится; null - прогноза нет. */
    fun quality(index: ContentIndex, omen: Omen?): QualityForecast? = null

    /** Уникалки, что может дать сфера удачи, - тот же список правил, что тянет сервер. */
    fun chanceUniques(index: ContentIndex): List<String> = emptyList()

    /** Пойдёт ли сфера [orb] на цель (со знамением [omen]): сфера одной строки - если хоть одну строку правила примут. */
    fun accepts(applier: OrbApplier, orb: Orb, omen: Omen? = null): Boolean = applier.accepts(orb, orbTarget, omen)

    /** Почему сфера [orb] не пойдёт на цель: отказ правил ([OrbApplier.refusal]); null - пойдёт. */
    fun refusal(applier: OrbApplier, orb: Orb): Refusal? = applier.refusal(orb, orbTarget)

    /** Строки цели для Сферы гравёра, каждая со своим отказом правил; у цели без строк - пусто. */
    fun engraving(index: ContentIndex): List<EngraveLine> = emptyList()

    /** Вещь под кузницей: сферы, печати Атласа (только карта), верстак (не у карты, с волшебной редкости) и эссенции (только снаряжение). */
    data class Gear(val view: ItemView) : ForgeTarget {
        override val id: String get() = view.id
        override val orbTarget: OrbTarget get() = OrbTarget.Gear(view.item, view.template)
        override val corrupted: Boolean get() = view.corrupted
        override val noOrbsKey: String get() = "forge.no_orbs_fit"
        override val changeKey: String get() = "forge.change_target"
        override val socketKey: String get() = "forge.socket_item"
        override val itemLevel: Int get() = view.item.level(view.template)

        override val sections: List<ForgeSection>
            get() {
                val slot = view.slot
                val benchable = slot != Slot.MAP && view.rarity in BENCHABLE
                val essential = !slot.isJewelLike && !slot.isFlask && !slot.isTool && view.rarity in ESSENTIAL
                return listOfNotNull(ForgeSection.ORBS, ForgeSection.SEALS.takeIf { slot == Slot.MAP }, ForgeSection.BENCH.takeIf { benchable }, ForgeSection.ESSENCES.takeIf { essential })
            }

        override fun seals(index: ContentIndex, omens: List<Omen>, held: (String) -> Long): List<OrbChoice> {
            if (view.slot != Slot.MAP) return emptyList()
            val seals = AtlasSeals(index)
            return index.itemsByCategory[Item.ATLAS_SEAL].orEmpty().map { it.code.value }.filter { held(it) > 0 }.mapNotNull { code ->
                when {
                    seals.accepts(code, view.item, view.template) -> OrbChoice(code, needsOmen = false)
                    sealOmens(index, code, omens).isNotEmpty() -> OrbChoice(code, needsOmen = true)
                    else -> null
                }
            }
        }

        override fun sealOmens(index: ContentIndex, seal: String, held: List<Omen>): List<Omen> {
            val omens = index.seal(seal)?.omens.orEmpty()
            val seals = AtlasSeals(index)
            return held.filter { it in omens && seals.accepts(seal, view.item, view.template, it) }
        }

        override fun quality(index: ContentIndex, omen: Omen?): QualityForecast? = QualityForecast.of(index, view.item, view.template, omen)

        override fun chanceUniques(index: ContentIndex): List<String> = OrbApplier(index).chanceUniques(view.item, view.template).map { it.value.code }

        override fun accepts(applier: OrbApplier, orb: Orb, omen: Omen?): Boolean = if (orb == Orb.ENGRAVERS_ORB) {
            view.item.rolls.indices.any { applier.accepts(orb, OrbTarget.Gear(view.item, view.template, it), omen) }
        } else {
            super.accepts(applier, orb, omen)
        }

        override fun refusal(applier: OrbApplier, orb: Orb): Refusal? = if (orb == Orb.ENGRAVERS_ORB && view.item.rolls.isNotEmpty()) {
            view.item.rolls.indices.firstNotNullOfOrNull { applier.refusal(orb, OrbTarget.Gear(view.item, view.template, it)) }
        } else {
            super.refusal(applier, orb)
        }

        override fun engraving(index: ContentIndex): List<EngraveLine> {
            val applier = OrbApplier(index)
            return view.lines.mapIndexed { i, line -> EngraveLine(i, line.text, line.range, applier.engraveRefusal(view.item, view.template, i)?.let { refusalText(it) }) }
        }
    }
}

/** Цель кузницы героя: вещь под кузницей; null - выбирать не из чего. */
fun GameUi.forgeTarget(): ForgeTarget? = hero?.item(holding.selectedEquipment)?.let { view(it) }?.let(ForgeTarget::Gear)

/**
 * Строка вещи под Сферой гравёра: номер [line] в `item.rolls`, её текст [text], вилка тира
 * [range] (печатается только здесь - карточка вещи вилок не пишет) и отказ правил словами игрока [refusal]; null - гравировать можно.
 */
data class EngraveLine(val line: Int, val text: String, val range: String?, val refusal: String?)

/** Сфера или печать лотка кузницы: код стопки и ждёт ли она знамения - сама цель её не примет, со знамением из сумки примет. */
data class OrbChoice(val code: String, val needsOmen: Boolean)

/**
 * Сферы сумки, что лягут на цель: [held] - сколько стопки в сумке, [shelf] - коды сфер ремесла в порядке полки, [omens] -
 * знамения сумки. Сфера ремесла, что цель не примет ни сама, ни со знамением, в лоток не попадает; сожаление тратится на древе.
 */
fun ForgeTarget.orbChoices(index: ContentIndex, shelf: List<String>, omens: List<Omen>, held: (String) -> Long): List<OrbChoice> {
    val applier = OrbApplier(index)
    return shelf.mapNotNull { code -> Orb.of(code)?.takeIf { it != Orb.ORB_OF_REGRET && held(code) > 0 } }.mapNotNull { orb ->
        when {
            accepts(applier, orb) -> OrbChoice(orb.name, needsOmen = false)
            omens.any { it.fits(orb) && accepts(applier, orb, it) } -> OrbChoice(orb.name, needsOmen = true)
            else -> null
        }
    }
}

/** Сфера сумки, что в лоток не легла: код [code] и точная причина [reason] - отказ правил словами сервера. */
data class HiddenOrb(val code: String, val reason: String)

/**
 * Сферы сумки, что цель не примет ни сама, ни со знамением сумки, - строка «Скрыто сфер: N» под лотком: каждая с отказом правил
 * над целью ([ForgeTarget.refusal]) словами сервера, своих причин клиент не пишет. Тот же отбор, что [orbChoices], - лоток и
 * скрытые вместе дают все сферы сумки, кроме сожаления.
 */
fun ForgeTarget.hiddenOrbs(index: ContentIndex, shelf: List<String>, omens: List<Omen>, held: (String) -> Long): List<HiddenOrb> {
    val shown = orbChoices(index, shelf, omens, held).mapTo(HashSet()) { it.code }
    val applier = OrbApplier(index)
    return shelf.mapNotNull { code -> Orb.of(code)?.takeIf { it != Orb.ORB_OF_REGRET && held(code) > 0 && it.name !in shown } }
        .map { orb -> HiddenOrb(orb.name, refusal(applier, orb)?.let { refusalText(it) }.orEmpty()) }
}
