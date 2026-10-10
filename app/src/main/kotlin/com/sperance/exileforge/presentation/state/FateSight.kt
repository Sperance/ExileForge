package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.FateBoon
import com.sperance.exileforge.rules.content.FateBoonDef
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.text.LocaleKey
import kotlin.math.abs

/** Как действует раскрытая судьбоносная строка вещи у героя (4.6.2, Осквернение Предначертания). */
enum class BoonState {
    /** Надета, дар героя, сильнейшая из одинаковых - действует. */
    ACTIVE,

    /** Дар героя, но вещь не надета: строка своя, состояния нет. */
    KIN,

    /** Строка чужого дара - спит. */
    SLEEPING,

    /** Та же строка сильнее на другой надетой вещи - эта не действует. */
    OUTDONE,
}

/** Судьбоносная строка вещи глазами героя: строка с силой вещи [boon], её [state] и место вещи, что её перебила ([rival]). */
data class FatedLine(val boon: FateBoon, val state: BoonState, val rival: Slot? = null)

/**
 * Судьбоносные строки вещей для героя с даром [fate] и надетым [worn] (4.6.2): та же логика, что у правил - строки чужого дара
 * спят, одинаковые не складываются, действует сильнейшая (тир, затем модуль значения, `FateBoons.active`), сила вещи - через
 * `Fates.worn`.
 */
class FateSight(val index: ContentIndex, private val fate: String?, worn: Collection<ItemInstance>) {
    private val rules = index.fates.desecration
    private val held = worn.filter { it.slot in rules.slots }

    /** Раскрытая строка вещи [item]; null - вещь не осквернена или строка ещё не раскрыта. */
    fun line(item: ItemInstance): FatedLine? {
        val boon = item.desecration?.boon ?: return null
        val shown = strengthened(item, boon)
        if (boon.fate != fate) return FatedLine(shown, BoonState.SLEEPING)
        if (held.none { it.id == item.id }) return FatedLine(shown, BoonState.KIN)
        val strongest = held.mapNotNull { piece -> piece.desecration?.boon?.takeIf { it.fate == fate && it.boon == boon.boon }?.let { piece to strengthened(piece, it) } }
            .maxWithOrNull(compareBy<Pair<ItemInstance, FateBoon>>({ it.second.tier }, { abs(it.second.value) }))
        return if (strongest == null || strongest.first.id == item.id) FatedLine(shown, BoonState.ACTIVE) else FatedLine(shown, BoonState.OUTDONE, strongest.first.slot)
    }

    /** Строка [boon] с силой своей вещи [item] (уникалка Осквернения усиливает её), как её считают правила; не надета - как есть. */
    private fun strengthened(item: ItemInstance, boon: FateBoon): FateBoon = index.fates.worn(index, boon.fate, listOf(item)).firstOrNull() ?: boon

    companion object {
        /** Ни героя, ни контента: строки читаются без состояния. */
        fun of(index: ContentIndex?, fate: String?, worn: Collection<ItemInstance>): FateSight? = index?.let { FateSight(it, fate, worn) }
    }
}

/**
 * Текст судьбоносной строки [boon] словами сервера (`FateBoons.key`): `{v}` - модуль значения, `{p}` и `{c}` - постоянные правила
 * (`FateBoonDef.Knob`).
 */
fun boonText(index: ContentIndex, boon: FateBoon): String {
    val knob = index.fates.boons.def(boon.fate, boon.boon, boon.tier) as? FateBoonDef.Knob
    return loc(index.fates.boons.key(boon))
        .replace(VALUE, number(abs(boon.value)))
        .replace(PARAM, number(knob?.param ?: 0.0))
        .replace(CAP, number(knob?.cap ?: 0.0))
}

/**
 * Крупное число строки [boon] для выбора Раскрытия: значение со знаком и процентом, как их ставит шаблон вокруг `{v}`; у строки
 * «да/нет» без числа - null.
 */
fun boonFigure(index: ContentIndex, boon: FateBoon): String? = FIGURE.find(loc(index.fates.boons.key(boon)))?.let { match ->
    match.groupValues[1] + number(abs(boon.value)) + match.groupValues[2]
}

/** Имя дара [code] словами сервера. */
fun fateTitle(code: String): String = loc(LocaleKey.fateName(code))

private const val VALUE = "{v}"
private const val PARAM = "{p}"
private const val CAP = "{c}"
private val FIGURE = Regex("""([+\-−]?)\{v\}(%?)""")
