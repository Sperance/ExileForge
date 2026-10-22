package com.sperance.exileforge.core.display

import com.sperance.exileforge.rules.content.CoreAttribute
import com.sperance.exileforge.rules.content.Rarity

/**
 * Вариант общего выбора из списка - чистые данные, без отрисовки.
 *
 * [key] уходит вызывающему при выборе (null - вариант «любое»/«все»), [title] - имя варианта, [subtitle] - подназвание под именем в
 * шторке (переносится целиком), [brief] - короткое подназвание под значением на чипе (нет - на чипе [subtitle]), [glyph] - знак по
 * смыслу, [accent] - цвет полосы слева, [data] - короткое значение ровной колонкой справа, [group] - заголовок группы: варианты одной
 * группы идут подряд; [terms] - слова, по которым вариант тоже находит поиск, но которые не печатаются (текст строк узла древа).
 * [locked] - почему вариант закрыт («с ур. 58»): он не выбирается, в шторке - тусклый, с замком и этой подписью справа вместо данных;
 * мапперы ставят закрытые в конец группы.
 */
data class PickOption<out K>(
    val key: K?,
    val title: String,
    val subtitle: String? = null,
    val brief: String? = null,
    val glyph: PickGlyph? = null,
    val accent: PickAccent? = null,
    val data: PickData? = null,
    val group: String? = null,
    val terms: List<String> = emptyList(),
    val locked: String? = null,
) {
    /** Строка под значением на чипе: [brief], а без него - [subtitle]. */
    val chipLine: String? get() = brief ?: subtitle
}

/** Данные справа от варианта: [value] крупно, [caption] мелко под ним («6» и «в ранге»); [lacking] - чего-то нет («0 в сумке»), число красным. */
data class PickData(val value: String, val caption: String? = null, val lacking: Boolean = false)

/** Знак варианта: значок смысла ([Sign]) или эмблема вида вещи ([Emblem]); рисует их `:app`. */
sealed interface PickGlyph {
    /** Значок смысла [glyph]. */
    data class Sign(val glyph: Glyph) : PickGlyph

    /** Эмблема вида вещи [kind]: база шаблона, полка стопки. */
    data class Emblem(val kind: ItemVisualKind) : PickGlyph
}

/** Цвет полосы варианта по смыслу; краску подбирает `:app`. */
sealed interface PickAccent {
    /** Цвет редкости [rarity]. */
    data class OfRarity(val rarity: Rarity) : PickAccent

    /** Ступень [step] лестницы из [steps] ступеней (ранги): от тихого цвета к яркому. */
    data class Step(val step: Int, val steps: Int) : PickAccent

    /** Полоса [band] из чередующихся цветов групп: регион, полка сумки. */
    data class Band(val band: Int) : PickAccent

    /** Цвет атрибута [attribute]: выбор кузнеца. */
    data class OfAttribute(val attribute: CoreAttribute) : PickAccent
}

/** Группа шторки выбора: заголовок [title] (null - без заголовка) и её варианты [options]. */
data class PickSection<K>(val title: String?, val options: List<PickOption<K>>)

/**
 * Список выбора: [options] в порядке показа - общий поиск по вариантам всех выборов игры (шторка выбора, «по моду», дары модерации,
 * узлы древа). Шторка ищет с [SEARCH_FROM] вариантов: запрос ищется в имени, подназвании и [PickOption.terms] без учёта регистра, «ё» и
 * лишних пробелов (свёртка поиска игры [ItemSearch.fold]). Группы - подряд идущие варианты с одним [PickOption.group].
 */
class PickList<K>(val options: List<PickOption<K>>) {
    /** Свёрнутые слова поиска каждого варианта: сворачиваются один раз на список. */
    private val words: List<List<String>> = options.map { option -> (listOfNotNull(option.title, option.subtitle) + option.terms).map(ItemSearch::fold) }

    /** Нужно ли шторке поле поиска. */
    val searchable: Boolean get() = options.size >= SEARCH_FROM

    /** Вариант с ключом [key] (null - вариант «любое», если он есть); нет такого - null. */
    fun option(key: K?): PickOption<K>? = options.firstOrNull { it.key == key }

    /** Варианты, что находит запрос [query], подряд; пустой запрос - все. */
    fun found(query: String): List<PickOption<K>> {
        val wanted = ItemSearch.fold(query)
        return if (wanted.isEmpty()) options else options.filterIndexed { at, _ -> words[at].any { it.contains(wanted) } }
    }

    /** Варианты, что находит запрос [query], по группам; пустой запрос - все. Ничего не нашлось - пустой список. */
    fun sections(query: String): List<PickSection<K>> {
        val runs = mutableListOf<Pair<String?, MutableList<PickOption<K>>>>()
        found(query).forEach { option -> runs.lastOrNull()?.takeIf { it.first == option.group }?.second?.add(option) ?: runs.add(option.group to mutableListOf(option)) }
        return runs.map { (title, run) -> PickSection(title, run) }
    }

    /** Строка варианта [key] в шторке без запроса - заголовки групп тоже строки; нет такого варианта - 0. */
    fun row(key: K?): Int {
        var row = 0
        sections("").forEach { section ->
            if (section.title != null) row++
            val at = section.options.indexOfFirst { it.key == key }
            if (at >= 0) return row + at
            row += section.options.size
        }
        return 0
    }

    companion object {
        /** С какого числа вариантов шторка ищет. */
        const val SEARCH_FROM = 8
    }
}
