package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.character.WearOutcome
import com.sperance.exileforge.core.display.Comparison
import com.sperance.exileforge.core.display.LostLines
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.sheet.MightReading

/**
 * Основа примерки героя: всё, из чего правила складывают лист героя с другой вещью ([Sheets.wearing]). Равные основы дают равный
 * итог - новый срез с тем же героем новой ссылкой (сброс похода, ответ сервера без перемен в снаряжении) примерку не пересчитывает.
 */
data class WearBasis(
    val level: Int,
    val heroClass: String,
    val tree: List<TakenNode>,
    val items: List<ItemInstance>,
    val figures: Map<String, Double>,
    val laws: List<String>,
    val fate: String?,
    val skills: HeroSkills,
) {
    /** Лист героя с [item] на месте [place] (без места - куда выберут правила) над контентом [index]. */
    fun wearing(index: ContentIndex, item: ItemInstance, place: Slot?): WearOutcome =
        Sheets.wearing(index, item, level, heroClass, tree, items, figures, place, laws, fate, skills)
}

/** Основа примерки этого героя ([WearBasis]). */
val HeroView.wearBasis: WearBasis get() = WearBasis(level, heroClass, tree, items, figures.values, info.laws, info.fate, skills)

/**
 * Итог «Если надеть» карточки вещи: сравнение листа героя «было → стало» ([comparison]; null - вещь листа героя не меняет, как фляга
 * и инструмент), Мощь героя с вещью ([might]) и «Теряешь» ([lost], [LostLines]). Складывается целиком за раз и тяжёл - строится вне
 * главного потока.
 */
class WearSums private constructor(val comparison: Comparison?, val might: MightReading?, val lost: List<String>) {
    companion object {
        /** Итог вещи [item], что меняет лист героя с основой [basis]: надевается на место [place] и снимает [removed]. */
        fun worn(index: ContentIndex, basis: WearBasis, item: ItemInstance, place: Slot?, removed: List<ItemInstance>): WearSums {
            val outcome = basis.wearing(index, item, place)
            val comparison = Comparison.of(outcome.delta, index, sheet = basis.figures, level = basis.level)
            return WearSums(comparison, outcome.might, LostLines.of(index, item, removed, basis.items, outcome.stats))
        }

        /** Итог фляги или инструмента [item]: лист героя не меняется, теряются только строки снятого [removed]. */
        fun apart(index: ContentIndex, item: ItemInstance, removed: List<ItemInstance>): WearSums = WearSums(null, null, LostLines.of(index, item, removed))
    }
}
