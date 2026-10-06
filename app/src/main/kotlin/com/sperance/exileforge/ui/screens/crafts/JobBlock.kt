package com.sperance.exileforge.ui.screens.crafts

import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.text
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.JobKind

/**
 * Почему работу нельзя начать (3.89.1): одна точная причина внизу листа вместо кнопки запуска. Выбор вариантов виден
 * всегда, даже у работы выше уровня профессии; причина считается по выбранному варианту, кнопка - только без неё.
 */
internal sealed interface JobBlock {
    val text: String

    /** Работе или варианту нужен уровень профессии [level]. */
    data class Level(val level: Int, val profession: String) : JobBlock {
        override val text: String get() = ui("crafts.needs_level", level, professionTitle(profession))
    }

    /** Сгущать нечего: в сумке нет эссенций на цикл. */
    data object NothingToCondense : JobBlock {
        override val text: String get() = ui("crafts.nothing_to_condense")
    }

    /** У выбирающей работы нет ни одного варианта. */
    data object NoVariant : JobBlock {
        override val text: String get() = ui("crafts.no_variant")
    }

    /** Карта региона, чья локация ещё не открыта в кампании. */
    data object LockedMap : JobBlock {
        override val text: String get() = ui("crafts.locked_map")
    }

    /** В слоте профессии нет инструмента. */
    data object NoTool : JobBlock {
        override val text: String get() = ui("crafts.no_tool")
    }

    /** Сумке не хватает на цикл: [line] называет, чего и сколько (3.89.0). */
    data class Short(val line: String) : JobBlock {
        override val text: String get() = line
    }

    companion object {
        /**
         * Первая причина по порядку проверки сервера: уровень, вариант, регион, инструмент, расход цикла [job] с добавками
         * [additives]; null - работу можно начать. [work] - сама работа, [choices] - её варианты, что лист предлагает.
         */
        fun of(game: GameUi, profession: ProfessionView, work: JobView, choices: List<JobView>, job: JobView, additives: List<String>): JobBlock? = when {
            job.level > profession.level -> Level(job.level, profession.code)
            work.options.isNotEmpty() && choices.isEmpty() && work.kind == JobKind.CONDENSE -> NothingToCondense
            job.kind.chosen -> NoVariant
            !job.open -> LockedMap
            profession.equipped == null -> NoTool
            else -> game.shortfall(job.cycleCost(additives))?.text()?.let(::Short)
        }
    }
}
