package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.key
import com.sperance.exileforge.core.filter.Narrowing
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.rules.content.BagCategoriesByName
import com.sperance.exileforge.rules.content.BagCategory
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Orb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Стопка сумки: код предмета [code] и сколько их у героя [amount]. */
data class BagStack(val code: String, val amount: Long)

/** Что стопка сумки делает из своего листа: в кузницу (сфера, эссенция, оттиск), прочитать книгу, открыть сундук, «Использовать» ([USE]). */
enum class StackUse { ORB, ESSENCE, SEAL, BOOK, CHEST, USE }

/** Действия стопки - одни для кнопок листа стопки и фильтра сумки «Можно применить». */
object StackUses {
    /**
     * Что делает стопка [code] у героя класса [heroClass]: сфера, что ложится на вещь (Сожаление - на древо, не в кузницу),
     * эссенция и оттиск - в кузницу, книга умения своего класса или общего - читается, сундук-добыча - открывается, стопка с действием
     * контента (`items.json` → `use`: «Лист Угнетения», «Колода Рока») - используется.
     */
    fun of(index: ContentIndex?, code: String, heroClass: String?): Set<StackUse> = buildSet {
        val orb = Orb.of(code)
        if (orb != null && orb != Orb.ORB_OF_REGRET) add(StackUse.ORB)
        index ?: return@buildSet
        if (index.essence(code) != null) add(StackUse.ESSENCE)
        if (index.seal(code) != null) add(StackUse.SEAL)
        if (heroClass != null && index.skills.byBook(code)?.fits(heroClass) == true) add(StackUse.BOOK)
        if (index.item(ItemCode(code))?.category == Item.CHEST) add(StackUse.CHEST)
        if (index.item(ItemCode(code))?.use != null) add(StackUse.USE)
    }
}

/** Порядок стопок внутри полки сумки: ценность (дороже - раньше), имя, количество (больше - раньше). */
@Serializable(with = BagOrderByName::class)
enum class BagOrder {
    VALUE,
    NAME,
    AMOUNT,
    ;

    fun title(lang: Lang = uiLanguage): String = ui(lang, "bag.order.$name")
}

/**
 * Отбор сумки, что помнится на устройстве: полки множеством [shelves] (пусто - все), только то, что можно применить
 * ([applicable], [StackUses]), и порядок внутри полки [order]. Поиск по имени - не грань и не помнится: его держит экран. Полки и
 * порядок пишутся именами и читаются терпимо ([BagCategoriesByName], [BagOrderByName]): переименованная полка или порядок не сбрасывают
 * остальные настройки устройства.
 */
@Serializable
data class BagFilter(
    @Serializable(with = BagCategoriesByName::class) val shelves: Set<BagCategory> = emptySet(),
    val applicable: Boolean = false,
    val order: BagOrder = BagOrder.VALUE,
) {
    /** Сужен ли список: выбрана полка или «Можно применить»; порядок не сужает. */
    val narrowed: Boolean get() = shelves.isNotEmpty() || applicable

    /** Включённые сужения чипами под строкой поиска, каждое - с отбором без него; полки - в порядке полок. */
    fun narrowings(lang: Lang): List<Narrowing<BagFilter>> = BagCategory.entries.filter { it in shelves }.map { Narrowing(ui(lang, it.key), copy(shelves = shelves - it)) } +
        listOfNotNull(Narrowing(ui(lang, "bag.filter.applicable"), copy(applicable = false)).takeIf { applicable })

    /** Все сужения сняты, порядок остаётся. */
    fun cleared(): BagFilter = BagFilter(order = order)
}

/** Порядок сумки на устройстве - именем; незнакомое имя при чтении - [BagOrder.VALUE]. */
object BagOrderByName : KSerializer<BagOrder> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BagOrder", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BagOrder) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): BagOrder = decoder.decodeString().let { name -> BagOrder.entries.firstOrNull { it.name == name } } ?: BagOrder.VALUE
}

/** Подзаголовок части полки «Книги»: книги, что читает герой (своего класса и общие), - под именем его класса, прочие - «Другие классы». */
sealed interface BagHeading {
    fun title(): String

    data class OwnClass(val heroClass: String) : BagHeading {
        override fun title(): String = classTitle(heroClass)
    }

    data object OtherClasses : BagHeading {
        override fun title(): String = ui("bag.books_other")
    }
}

/** Часть полки под подзаголовком [heading] (null - без него): её стопки [stacks] в порядке отбора. */
data class BagGroup(val heading: BagHeading?, val stacks: List<BagStack>)

/** Полка сумки на экране: её части [groups] - одна без подзаголовка, а у «Книг» героя - свои сверху, чужие ниже. */
data class BagSection(val category: BagCategory, val groups: List<BagGroup>)

/**
 * Сумка глазами экрана - полки по порядку [BagCategory], без пустых, под отбором [BagFilter] и поиском по имени ([ItemSearch]);
 * [counts] - стопок на каждой полке всей сумки (числа фишек шторки фильтра).
 */
class BagShelf private constructor(val sections: List<BagSection>, val counts: Map<BagCategory, Int>, private val seen: List<SeenStack>) {
    /** Сколько стопок покажет отбор [filter] с поиском [query] - «Показать N» шторки, пока отбор в черновике. */
    fun count(filter: BagFilter, query: String): Int = seen.count { it.fits(filter, query) }

    /** Почему под отбором [filter] и поиском [query] пусто, словами для игрока: полки с запросом, один запрос или одни фильтры. */
    fun emptyReason(filter: BagFilter, query: String, lang: Lang): String {
        val shelves = BagCategory.entries.filter { it in filter.shelves }.joinToString(", ") { ui(lang, it.key) }
        return when {
            query.isNotBlank() && shelves.isNotEmpty() -> ui(lang, "bag.none_shelves_query", shelves, query.trim())
            query.isNotBlank() -> ui(lang, "bag.none_query", query.trim())
            else -> ui(lang, "filter.empty")
        }
    }

    companion object {
        /**
         * Сумка [bag] героя класса [heroClass] над контентом [index] под отбором [filter] и поиском [query]: стопки с числом больше
         * нуля, по полкам, внутри полки - порядком [BagFilter.order] (равные - по имени). Полка, годность, имя и цена стопки
         * считаются раз на стопку ([SeenStack]).
         */
        fun of(index: ContentIndex?, bag: Map<String, Long>, heroClass: String?, filter: BagFilter, query: String): BagShelf {
            val seen = bag.filterValues { it > 0 }.map { (code, amount) -> SeenStack.of(index, heroClass, BagStack(code, amount)) }
            val byCategory = seen.filter { it.fits(filter, query) }.sortedWith(SeenStack.order(filter.order)).groupBy { it.category }
            val sections = BagCategory.entries.mapNotNull { category -> byCategory[category]?.let { BagSection(category, groups(category, it, heroClass)) } }
            return BagShelf(sections, seen.groupingBy { it.category }.eachCount(), seen)
        }

        /**
         * Части полки [category] из стопок [stacks]: у «Книг» героя класса [heroClass] - сначала те, что он читает ([StackUse.BOOK]: своего
         * класса и общие), затем «Другие классы»; пустая часть не показывается. Прочие полки - одной частью.
         */
        private fun groups(category: BagCategory, stacks: List<SeenStack>, heroClass: String?): List<BagGroup> {
            if (category != BagCategory.BOOKS || heroClass == null) return listOf(BagGroup(null, stacks.map { it.stack }))
            val (own, other) = stacks.partition { StackUse.BOOK in it.uses }
            return listOf(BagGroup(BagHeading.OwnClass(heroClass), own.map { it.stack }), BagGroup(BagHeading.OtherClasses, other.map { it.stack })).filter { it.stacks.isNotEmpty() }
        }
    }
}

/** Что сумка знает о стопке [stack], посчитанное раз: полка [category], действия [uses] ([StackUses]), имя [name] и цена [worth]. */
private class SeenStack(val stack: BagStack, val category: BagCategory, val uses: Set<StackUse>, val name: String, val worth: Long) {
    val usable: Boolean get() = uses.isNotEmpty()

    fun fits(filter: BagFilter, query: String): Boolean = (filter.shelves.isEmpty() || category in filter.shelves) && (!filter.applicable || usable) && ItemSearch.matches(stack.code, query)

    companion object {
        fun of(index: ContentIndex?, heroClass: String?, stack: BagStack): SeenStack {
            val item = index?.item(ItemCode(stack.code))
            return SeenStack(stack, BagCategory.of(item), StackUses.of(index, stack.code, heroClass), itemTitle(stack.code), item?.price ?: 0L)
        }

        /** Порядок [order] внутри полки; равные - по имени. */
        fun order(order: BagOrder): Comparator<SeenStack> {
            val byName = compareBy<SeenStack> { it.name }
            return when (order) {
                BagOrder.VALUE -> compareByDescending<SeenStack> { it.worth }.then(byName)
                BagOrder.NAME -> byName
                BagOrder.AMOUNT -> compareByDescending<SeenStack> { it.stack.amount }.then(byName)
            }
        }
    }
}
