package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.display.BagCategory
import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.filter.Narrowing
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Orb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Стопка сумки: код предмета [code] и сколько их у героя [amount]. */
data class BagStack(val code: String, val amount: Long)

/** Что стопка сумки делает из своего листа: в кузницу (сфера, эссенция, оттиск), прочитать книгу, открыть сундук. */
enum class StackUse { ORB, ESSENCE, SEAL, BOOK, CHEST }

/** Действия стопки - одни для кнопок листа стопки и фильтра сумки «Можно применить». */
object StackUses {
    /**
     * Что делает стопка [code] у героя класса [heroClass]: сфера, что ложится на вещь (Сожаление - на древо, не в кузницу),
     * эссенция и оттиск - в кузницу, книга умения своего класса или общего - читается, сундук-добыча - открывается.
     */
    fun of(index: ContentIndex?, code: String, heroClass: String?): Set<StackUse> = buildSet {
        val orb = Orb.of(code)
        if (orb != null && orb != Orb.ORB_OF_REGRET) add(StackUse.ORB)
        index ?: return@buildSet
        if (index.essence(code) != null) add(StackUse.ESSENCE)
        if (index.seal(code) != null) add(StackUse.SEAL)
        if (heroClass != null && index.skills.byBook(code)?.fits(heroClass) == true) add(StackUse.BOOK)
        if (index.item(ItemCode(code))?.category == Item.CHEST) add(StackUse.CHEST)
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
 * порядок пишутся именами и читаются терпимо ([ShelvesByName], [BagOrderByName]): переименованная полка или порядок не сбрасывают
 * остальные настройки устройства.
 */
@Serializable
data class BagFilter(
    @Serializable(with = ShelvesByName::class) val shelves: Set<BagCategory> = emptySet(),
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

/** Полки отбора сумки на устройстве - именами; имя, которого больше нет среди [BagCategory], при чтении пропускается. */
object ShelvesByName : KSerializer<Set<BagCategory>> {
    private val names = SetSerializer(String.serializer())
    override val descriptor: SerialDescriptor = names.descriptor

    override fun serialize(encoder: Encoder, value: Set<BagCategory>) = encoder.encodeSerializableValue(names, value.mapTo(LinkedHashSet()) { it.name })

    override fun deserialize(decoder: Decoder): Set<BagCategory> = decoder.decodeSerializableValue(names).mapNotNullTo(LinkedHashSet()) { name -> BagCategory.entries.firstOrNull { it.name == name } }
}

/** Порядок сумки на устройстве - именем; незнакомое имя при чтении - [BagOrder.VALUE]. */
object BagOrderByName : KSerializer<BagOrder> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BagOrder", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BagOrder) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): BagOrder = decoder.decodeString().let { name -> BagOrder.entries.firstOrNull { it.name == name } } ?: BagOrder.VALUE
}

/** Полка сумки на экране: её стопки [stacks] в порядке отбора. */
data class BagSection(val category: BagCategory, val stacks: List<BagStack>)

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
            val byCategory = seen.filter { it.fits(filter, query) }.sortedWith(SeenStack.order(filter.order)).groupBy({ it.category }, { it.stack })
            val sections = BagCategory.entries.mapNotNull { category -> byCategory[category]?.let { BagSection(category, it) } }
            return BagShelf(sections, seen.groupingBy { it.category }.eachCount(), seen)
        }
    }
}

/** Что сумка знает о стопке [stack], посчитанное раз: полка [category], годность к действию [usable] ([StackUses]), имя [name] и цена [worth]. */
private class SeenStack(val stack: BagStack, val category: BagCategory, val usable: Boolean, val name: String, val worth: Long) {
    fun fits(filter: BagFilter, query: String): Boolean =
        (filter.shelves.isEmpty() || category in filter.shelves) && (!filter.applicable || usable) && ItemSearch.matches(stack.code, query)

    companion object {
        fun of(index: ContentIndex?, heroClass: String?, stack: BagStack): SeenStack {
            val item = index?.item(ItemCode(stack.code))
            return SeenStack(stack, BagCategory.of(item), StackUses.of(index, stack.code, heroClass).isNotEmpty(), itemTitle(stack.code), item?.price ?: 0L)
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
