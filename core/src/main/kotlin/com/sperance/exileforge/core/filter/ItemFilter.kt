package com.sperance.exileforge.core.filter

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.display.statPercent
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.gearVerdict
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.QualityKind
import kotlinx.serialization.Serializable

/**
 * A shelf of the stash's slot chips: the slots the rules tag alike are one — both weapons, both rings,
 * the three flask bays — and every other slot is its own. Вариант «Типа» фильтра предметов ([ItemType.Group]).
 */
@Serializable
@JvmInline
value class SlotGroup(val tag: String) {
    fun title(lang: Lang): String = when (tag) {
        WEAPON, RING, FLASK, TOOL -> ui(lang, "stash.group.$tag")
        else -> Slot.entries.firstOrNull { it.tag == tag }?.let { slotTitle(it, lang) } ?: tag
    }

    /** Место группы, чей значок она носит в рейке тайника: первое место с её тегом. */
    val slot: Slot get() = Slot.entries.first { it.tag == tag }

    val isTool: Boolean get() = tag == TOOL

    companion object {
        const val WEAPON = "weapon"
        const val RING = "ring"
        const val FLASK = "flask"
        const val TOOL = "tool"
        fun of(slot: Slot) = SlotGroup(slot.tag)
    }
}

/**
 * Раздел списка шире места: вся экипировка, карты, инструменты, надетое - быстрые варианты «Типа» на полке у наковальни.
 * Новый раздел - запись здесь и в [ItemShelf.sections]. [apart] - раздел по тому, где вещь, а не что она: полка ставит его
 * отдельной кнопкой под чертой.
 */
@Serializable
enum class ItemSection(val apart: Boolean) {
    GEAR(false),
    MAPS(false),
    TOOLS(false),
    WORN(true),
    ;

    fun admits(piece: ItemView): Boolean = when (this) {
        GEAR -> piece.slot != Slot.MAP && !piece.slot.isTool
        MAPS -> piece.slot == Slot.MAP
        TOOLS -> piece.slot.isTool
        WORN -> piece.isWorn
    }
}

/** «Тип» фильтра предметов: место (группа мест рейки) или раздел шире места - одно состояние для рейки, полки и шторки. */
@Serializable
sealed interface ItemType {
    fun admits(piece: ItemView): Boolean

    @Serializable
    data class Group(val group: SlotGroup) : ItemType {
        override fun admits(piece: ItemView): Boolean = SlotGroup.of(piece.slot) == group
    }

    @Serializable
    data class Section(val section: ItemSection) : ItemType {
        override fun admits(piece: ItemView): Boolean = section.admits(piece)
    }
}

/** Качество в фильтре предметов: любое, есть хоть сколько-то, на потолке своего вида (фляги - свой), нет совсем. */
@Serializable
enum class QualityFilter {
    ANY,
    SOME,
    MAX,
    NONE,
    ;

    fun admits(piece: ItemView): Boolean {
        val quality = piece.item.quality
        return when (this) {
            ANY -> true
            SOME -> quality > 0
            MAX -> quality > 0 && quality >= piece.qualityCap
            NONE -> quality == 0
        }
    }
}

/**
 * Вилка уровня предмета в фильтре: края [min] и [max] включительно, null - без края. Заданная вилка отсеивает то, у чего уровня
 * нет (стопки хранилища гильдии); пустая пропускает всё.
 */
@Serializable
data class LevelRange(val min: Int? = null, val max: Int? = null) {
    val isAny: Boolean get() = min == null && max == null

    fun admits(level: Int?): Boolean = isAny || (level != null && (min == null || level >= min) && (max == null || level <= max))

    /** Вилка словами на языке [lang] для строки фильтра и чипа: «Ур. 27–37», с одним краем - «Ур. от 27» / «Ур. до 37»; пустая - null. */
    fun title(lang: Lang): String? = when {
        min != null && max != null -> ui(lang, "filter.level_range", min, max)
        min != null -> ui(lang, "filter.level_min", min)
        max != null -> ui(lang, "filter.level_max", max)
        else -> null
    }

    companion object {
        /** Сколько уровней в обе стороны от героя берёт «Около моего». */
        const val NEAR = 5

        /** Сколько цифр принимает поле края вилки. */
        private const val DIGITS = 3

        /** «Около моего»: уровень героя [level] ± [NEAR], не ниже первого. */
        fun near(level: Int): LevelRange = LevelRange((level - NEAR).coerceAtLeast(1), level + NEAR)

        /** Край вилки из набранного в поле [text]: только цифры, не больше трёх; пусто - края нет. */
        fun bound(text: String): Int? = text.filter(Char::isDigit).take(DIGITS).toIntOrNull()
    }
}

/** Потолок качества вещи - потолок её вида качества по правилам ([QualityKind]); вида нет - общий. */
private val ItemView.qualityCap: Int get() = QualityKind.of(catalyst).gain(item, template, index)?.cap(index.rules) ?: index.rules.quality.max

/** Порядки списка предметов (общие для всех списков): новые первыми, по редкости, уровню предмета или цене списка. */
@Serializable
enum class ItemSort {
    NEWEST,
    RARITY,
    LEVEL,
    PRICE,
    ;

    /** Имя порядка на языке [lang]: подпись кнопки порядка и пункт её меню. */
    fun title(lang: Lang): String = ui(lang, "filter.sort.$name")

    /**
     * Список в этом порядке. Сервер держит вещи старыми первыми, так что новые - список наоборот; прочие порядки среди равных
     * - тоже новые первыми. [price] - цена вещи в этом списке (торговцу, у торговца), когда известна.
     */
    fun order(pieces: List<ItemView>, price: (ItemView) -> Long?): List<ItemView> {
        val newest = pieces.asReversed()
        return when (this) {
            NEWEST -> newest
            RARITY -> newest.sortedByDescending { it.rarity.ordinal }
            LEVEL -> newest.sortedByDescending { it.level }
            PRICE -> newest.sortedByDescending { price(it) ?: -1L }
        }
    }
}

/**
 * Что умеет сузить фильтр списка: каждая грань - строка шторки или переключатель; уровень - вилка в шапке шторки, порядок - кнопка
 * строки поиска.
 */
enum class FilterFacet { TYPE, RARITY, LEVEL, QUALITY, MOD, MINE, CRAFTED, WEARABLE, BETTER, HIDE_WORN, SORT }

/**
 * Список предметов приложения с фильтром: какие грани фильтра в нём есть ([facets]), какие порядки ([sorts]) и с чего он
 * начинается ([defaults]). [rail] - «Тип» выбирает рейка рядом со списком: в шторке и в ряду сужений его нет, сброс его не трогает.
 * Выбор фильтра запоминается на список, на устройстве ([ItemFilters]). Новый список - запись здесь.
 */
enum class ItemShelf(val facets: Set<FilterFacet>, val sorts: List<ItemSort>, val defaults: ItemFilter, val sections: List<ItemSection> = emptyList(), val rail: Boolean = false) {
    /** Тайник героя: тип - рейка, цена - торговца за копию; надетое скрыто, пока игрок его не покажет. */
    STASH(FilterFacet.entries.toSet(), ItemSort.entries, ItemFilter(hideWorn = true), rail = true),

    /** Выбор вещи в кузнице: сферам всё равно, надета ли вещь и лучше ли она надетого, поэтому надетое видно. */
    FORGE(FilterFacet.entries.toSet() - FilterFacet.WEARABLE - FilterFacet.BETTER, ItemSort.entries, ItemFilter(sort = ItemSort.RARITY), ItemSection.entries),

    /** Товар торговца: его вещи не сделаны героем и не надеты; цена - торговца. */
    MERCHANT(
        setOf(FilterFacet.TYPE, FilterFacet.RARITY, FilterFacet.LEVEL, FilterFacet.QUALITY, FilterFacet.MOD, FilterFacet.WEARABLE, FilterFacet.BETTER, FilterFacet.SORT),
        ItemSort.entries,
        ItemFilter(),
    ),

    /** Вещи окна вклада в хранилище гильдии: надетого там нет, тип - вкладки окна (снаряжение и карты), цены нет. */
    GUILD_DEPOSIT(
        setOf(FilterFacet.RARITY, FilterFacet.LEVEL, FilterFacet.QUALITY, FilterFacet.MOD, FilterFacet.MINE, FilterFacet.CRAFTED, FilterFacet.SORT),
        listOf(ItemSort.NEWEST, ItemSort.RARITY, ItemSort.LEVEL),
        ItemFilter(),
    ),
}

/**
 * Что фильтр предметов спрашивает о герое на экране: [heroName] - для «сделано мной», [heroLevel] - для «Около моего» вилки уровня,
 * [unmet] - чего копии не хватает до «могу надеть», [better] - улучшит ли вещь героя на своём месте («лучше надетого»,
 * [GearVerdict.better][com.sperance.exileforge.core.character.GearVerdict.better]).
 */
interface ItemJudge {
    val heroName: String

    val heroLevel: Int

    fun unmet(item: ItemInstance): List<String>

    fun better(piece: ItemView): Boolean
}

/**
 * Единый фильтр предметов всех списков: тип ([type], null - всё), редкости (пусто - все), вилка уровня предмета ([level]), качество,
 * характеристика строки ([mod], null - любая: «по моду»), «сделано мной» (поле `maker` копии - имя героя), «сделано ремеслом» (метка
 * происхождения копии `origin` с сервера), «могу надеть», «лучше надетого» ([better]), «скрыть надетое» и порядок. Хранится на
 * устройстве для своего списка ([ItemShelf]), у новых граней - значения по умолчанию, так что сохранённый раньше фильтр читается.
 * Поиск строки - отдельно ([ItemQuery]): он не запоминается.
 */
@Serializable
data class ItemFilter(
    val type: ItemType? = null,
    val rarities: Set<Rarity> = emptySet(),
    val level: LevelRange = LevelRange(),
    val quality: QualityFilter = QualityFilter.ANY,
    val mod: String? = null,
    val mine: Boolean = false,
    val crafted: Boolean = false,
    val wearable: Boolean = false,
    val better: Boolean = false,
    val hideWorn: Boolean = false,
    val sort: ItemSort = ItemSort.NEWEST,
) {
    fun toggle(rarity: Rarity) = copy(rarities = if (rarity in rarities) rarities - rarity else rarities + rarity)

    /**
     * Включённые сужения списка [shelf] чипами под строкой поиска, каждое - с фильтром без него: грани, что отличаются от
     * обычных списка; редкость - чипом на каждую; подписи - на языке [lang]. Порядок и поиск сужениями не считаются, тип рейки
     * ([ItemShelf.rail]) - тоже. Их число - на кнопке фильтра.
     */
    fun narrowings(shelf: ItemShelf, lang: Lang): List<Narrowing<ItemFilter>> {
        val base = shelf.defaults
        return buildList {
            fun put(facet: FilterFacet, changed: Boolean, title: () -> String?, without: () -> ItemFilter) {
                if (facet in shelf.facets && changed) title()?.let { add(Narrowing(it, without())) }
            }
            put(FilterFacet.TYPE, !shelf.rail && type != base.type, { type?.let { typeTitle(it, lang) } }, { copy(type = base.type) })
            rarities.sortedByDescending { it.ordinal }.forEach { rarity -> put(FilterFacet.RARITY, rarity !in base.rarities, { rarityTitle(rarity, lang) }, { toggle(rarity) }) }
            put(FilterFacet.LEVEL, level != base.level, { level.title(lang) }, { copy(level = base.level) })
            put(FilterFacet.QUALITY, quality != base.quality, { ui(lang, "filter.quality_chip", ui(lang, "filter.quality.${quality.name}")) }, { copy(quality = base.quality) })
            put(FilterFacet.MOD, mod != base.mod, { mod?.let { ui(lang, "filter.mod_chip", modTitle(it, lang)) } }, { copy(mod = base.mod) })
            put(FilterFacet.MINE, mine != base.mine, { ui(lang, "filter.mine") }, { copy(mine = base.mine) })
            put(FilterFacet.CRAFTED, crafted != base.crafted, { ui(lang, "filter.crafted") }, { copy(crafted = base.crafted) })
            put(FilterFacet.WEARABLE, wearable != base.wearable, { ui(lang, "filter.wearable") }, { copy(wearable = base.wearable) })
            put(FilterFacet.BETTER, better != base.better, { ui(lang, "filter.better") }, { copy(better = base.better) })
            put(FilterFacet.HIDE_WORN, hideWorn != base.hideWorn, { ui(lang, if (hideWorn) "filter.hide_worn" else "filter.show_worn") }, { copy(hideWorn = base.hideWorn) })
        }
    }

    /** Все сужения списка [shelf] сняты: обычный фильтр списка с прежними порядком и типом рейки ([ItemShelf.rail]). */
    fun cleared(shelf: ItemShelf): ItemFilter = shelf.defaults.copy(type = if (shelf.rail) type else shelf.defaults.type, sort = sort)

    /** Прячет ли «скрыть надетое» списка [shelf] вещь [piece] - правило снаряжения: инструмент виден всегда. */
    fun hides(piece: ItemView, shelf: ItemShelf): Boolean = FilterFacet.HIDE_WORN in shelf.facets && hideWorn && piece.isWorn && !piece.slot.isTool

    /**
     * Проходит ли [piece]: о герое спрашивает [judge]. Грани, которых нет у списка [shelf], не сужают его; «скрыть надетое» - [hides].
     * «Лучше надетого» меряет вещь, только когда грань включена, и последней.
     */
    fun admits(piece: ItemView, shelf: ItemShelf, judge: ItemJudge): Boolean {
        fun on(facet: FilterFacet) = facet in shelf.facets
        return (!on(FilterFacet.TYPE) || type?.admits(piece) != false) &&
            (!on(FilterFacet.RARITY) || rarities.isEmpty() || piece.rarity in rarities) &&
            (!on(FilterFacet.LEVEL) || level.admits(piece.level)) &&
            (!on(FilterFacet.QUALITY) || quality.admits(piece)) &&
            (!on(FilterFacet.MOD) || mod == null || piece.hasModStat(mod)) &&
            (!on(FilterFacet.MINE) || !mine || piece.item.maker?.hero == judge.heroName) &&
            (!on(FilterFacet.CRAFTED) || !crafted || piece.item.origin != null) &&
            (!on(FilterFacet.WEARABLE) || !wearable || judge.unmet(piece.item).isEmpty()) &&
            !hides(piece, shelf) &&
            (!on(FilterFacet.BETTER) || !better || judge.better(piece))
    }

    /** Порядок списка [shelf]: свой, если список сортируется, иначе обычный списка. */
    fun order(shelf: ItemShelf): ItemSort = if (FilterFacet.SORT in shelf.facets) sort else shelf.defaults.sort
}

/** Есть ли у копии строка, что действует на характеристику [stat] («по моду»): обход строк с ранним выходом. */
fun ItemView.hasModStat(stat: String): Boolean = item.rolls.any { roll -> index.modifier(roll.code)?.effects?.any { it.stat == stat } == true }

/** Характеристики «по моду», что есть у вещей списка [pieces], по имени на языке [lang] ([modTitle]): из них выбирают в шторке. */
fun modChoices(pieces: List<ItemView>, lang: Lang): List<String> = buildSet {
    pieces.forEach { piece -> piece.item.rolls.forEach { roll -> piece.index.modifier(roll.code)?.effects?.forEach { add(it.stat) } } }
}.sortedBy { modTitle(it, lang) }

/** Имя характеристики «по моду»: процентная - с «%», чтобы «Здоровье» и «Здоровье, %» различались. */
fun modTitle(stat: String, lang: Lang): String = statTitle(stat, lang).let { if (statPercent(stat)) ui(lang, "filter.mod_percent", it) else it }

/** Имя варианта «Типа»: раздел - своим словом, группа мест - как в рейке. */
fun typeTitle(type: ItemType, lang: Lang): String = when (type) {
    is ItemType.Group -> type.group.title(lang)
    is ItemType.Section -> ui(lang, "filter.section.${type.section.name}")
}

/** Запомненные фильтры списков на устройстве: у списка без записи - его обычный. */
@Serializable
data class ItemFilters(val shelves: Map<ItemShelf, ItemFilter> = emptyMap()) {
    operator fun get(shelf: ItemShelf): ItemFilter = shelves[shelf] ?: shelf.defaults

    fun with(shelf: ItemShelf, filter: ItemFilter): ItemFilters = copy(shelves = shelves + (shelf to filter))
}

/**
 * Пункты рейки тайника: группы мест в порядке мест тела, инструменты - в конце, со счётом вещей [pieces];
 * группа без вещей пункта не получает. И варианты «Типа» в шторке фильтра.
 */
fun railGroups(pieces: List<ItemView>): List<Pair<SlotGroup, Int>> {
    val counts = pieces.groupingBy { SlotGroup.of(it.slot) }.eachCount()
    return Slot.entries.map(SlotGroup::of).distinct().sortedBy { it.isTool }.mapNotNull { group -> counts[group]?.let { group to it } }
}

/**
 * Герой на экране глазами фильтра предметов - снимок героя и контента сейчас: «сделано мной» - по его имени, «могу надеть» - по
 * его листу, «лучше надетого» - по вердикту вещи ([gearVerdict]) на её лучшем месте. Меряет вещи и вне главного потока.
 */
val GameUi.itemJudge: ItemJudge get() {
    val game = this
    return object : ItemJudge {
        override val heroName: String = game.hero?.info?.name.orEmpty()

        override val heroLevel: Int = game.heroLevel

        override fun unmet(item: ItemInstance): List<String> = game.unmetFor(item)

        override fun better(piece: ItemView): Boolean = game.gearVerdict(piece.item)?.better == true
    }
}
