package com.sperance.exileforge.core.guild

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.ItemSearch
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.filter.ItemQuery
import com.sperance.exileforge.core.filter.ItemSection
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.filter.ItemShelfView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.guild.GuildDepositParcel
import com.sperance.exileforge.core.model.guild.GuildStashView
import com.sperance.exileforge.core.pick.PickPreset
import com.sperance.exileforge.core.pick.Pickable
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.DepositBar
import com.sperance.exileforge.rules.content.GuildDeposit
import com.sperance.exileforge.rules.content.Rarity

/** Вкладка-чип окна вклада в хранилище гильдии: снаряжение (с инструментами), стопки сумки, карты; подпись - `guild.deposit_kind.<код>`. */
enum class DepositKind { GEAR, STACKS, MAPS }

/**
 * Посылка окна вклада: копия тайника [piece] или стопка сумки [code] (в сумке [owned]); [bar] - почему её не положить, по правилу
 * сервера ([GuildDeposit.bar]): с запретом её не отметить, причина - в строке.
 */
data class DepositLot(val kind: DepositKind, val piece: ItemView? = null, val code: String = "", val owned: Long = 0, val bar: DepositBar? = null) : Pickable {
    override val id: String get() = piece?.id ?: "$STACK_ID$code"

    /** Сколько стопки уйдёт при набранном в поле [typed]: число в пределах сумки; пусто или не число - вся стопка. */
    fun amountOf(typed: String?): Long = typed?.toLongOrNull()?.coerceIn(1, owned.coerceAtLeast(1)) ?: owned

    /** Число стопки в строке: «× 37», а часть стопки - «× 10 из 37». */
    fun countText(typed: String?): String = amountOf(typed).let { if (it < owned) ui("guild.deposit_stack_part", number(it.toDouble()), number(owned.toDouble())) else ui("guild.deposit_stack_all", number(owned.toDouble())) }

    /** Посылка запроса: копия - по id, стопка - кодом и количеством из набранного [typed]. */
    fun parcel(typed: String?): GuildDepositParcel = piece?.let { GuildDepositParcel(itemId = it.id) } ?: GuildDepositParcel(code = code, amount = amountOf(typed))
}

/** Приставка id стопки в выборе: id копий и коды стопок не пересекаются. */
private const val STACK_ID = "stack:"

/** Быстрый набор окна вклада: всё видимое или одна редкость видимых вещей; запрещённого наборы не берут - его нет среди лотов выбора. */
sealed interface DepositPreset : PickPreset<DepositLot> {
    data object Visible : DepositPreset {
        override fun takes(lot: DepositLot) = true
    }

    data class OfRarity(val rarity: Rarity) : DepositPreset {
        override fun takes(lot: DepositLot) = lot.piece?.rarity == rarity
    }

    companion object {
        val ALL: List<DepositPreset> = listOf(Visible) + Rarity.entries.map(::OfRarity)
    }
}

/**
 * Что герой может положить во вкладку хранилища: копии тайника, кроме надетого и вставленного (их сперва снимают), и стопки сумки -
 * по вкладкам окна ([DepositKind]); запертое и привязанное к герою - с причиной. [free] - свободных мест вкладки, [size] - всех.
 */
class DepositBag(val lots: List<DepositLot>, val free: Int, val size: Int) {
    /** Что можно отметить: без запрета. */
    val pickable: List<DepositLot> = lots.filter { it.bar == null }

    fun of(kind: DepositKind): List<DepositLot> = lots.filter { it.kind == kind }

    /** Вещи вкладки [kind] для витрины ([ItemShelf.GUILD_DEPOSIT]): старыми первыми, как их ждёт фильтр предметов; у стопок - пусто. */
    fun pieces(kind: DepositKind): List<ItemView> = of(kind).mapNotNull { it.piece }.asReversed()

    /**
     * Видимое на вкладке [kind]: вещи - в порядке витрины [shelf] этой вкладки (пока она считается или собрана для другой - только
     * её вещи вкладки), стопки - поиском [query] по имени.
     */
    fun shown(kind: DepositKind, shelf: ItemShelfView?, query: ItemQuery): List<DepositLot> {
        val lots = of(kind)
        if (kind == DepositKind.STACKS) return lots.filter { ItemSearch.matches(it.code, query.text) }
        val byId = lots.associateBy { it.id }
        return shelf?.visible.orEmpty().mapNotNull { byId[it.id] }
    }

    /** Посылки запроса из отмеченного [picked] в его порядке; [typed] - набранные количества стопок по коду. */
    fun parcels(picked: List<DepositLot>, typed: Map<String, String>): List<GuildDepositParcel> = picked.map { it.parcel(typed[it.code]) }

    /** Сколько из отмеченного [picked] ляжет во вкладку - по правилу сервера, по порядку. */
    fun fits(picked: List<DepositLot>): Int = GuildDeposit.plan(picked, free) { it.bar }.placed.size

    /** Подпись вкладки-чипа [kind]: имя и число посылок, а с отмеченным ([chosen]) - «отмечено/всего». */
    fun tab(kind: DepositKind, chosen: (String) -> Boolean): String {
        val all = of(kind)
        val marked = all.count { chosen(it.id) }
        return ui("guild.deposit_kind_count", ui("guild.deposit_kind.${kind.name}"), if (marked > 0) "$marked/${all.size}" else all.size.toString())
    }
}

/**
 * Окно вклада героя на экране во вкладку [tab] хранилища [stash]: снаряжение и карты - новые первыми, как тайник, стопки - по имени;
 * в этом порядке посылки и уходят на сервер. Героя или контента нет - null.
 */
fun GameUi.depositBag(stash: GuildStashView, tab: Int): DepositBag? {
    val hero = hero ?: return null
    val index = index ?: return null
    val pieces = hero.items.asReversed().filter { !it.equipped && !it.socketed }.mapNotNull { item ->
        view(item)?.let { DepositLot(if (ItemSection.MAPS.admits(it)) DepositKind.MAPS else DepositKind.GEAR, piece = it, bar = GuildDeposit.bar(item)) }
    }
    val stacks = hero.bag.filter { it.value > 0 }.entries.sortedBy { itemTitle(it.key) }.map { (code, amount) -> DepositLot(DepositKind.STACKS, code = code, owned = amount, bar = GuildDeposit.bar(index, code)) }
    val lots = (pieces + stacks).sortedBy { it.kind.ordinal }
    return DepositBag(lots, (stash.tabSize - stash.entries.count { it.tab == tab }).coerceAtLeast(0), stash.tabSize)
}
