package com.sperance.exileforge.core.sheet

import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.EffectOrigins
import com.sperance.exileforge.core.campaign.PoolReserve
import com.sperance.exileforge.core.campaign.RunSheet
import com.sperance.exileforge.core.campaign.SourceShifts
import com.sperance.exileforge.core.campaign.StatFigures
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.character.GearVerdicts
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.character.StatDelta
import com.sperance.exileforge.core.character.WearOutcome
import com.sperance.exileforge.core.character.WearPlace
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.CardSeal
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.MapCapture
import com.sperance.exileforge.core.display.NeedKind
import com.sperance.exileforge.core.display.unmetNeeds
import com.sperance.exileforge.core.model.atlas.AtlasState
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.tree.TreeState
import com.sperance.exileforge.rules.content.AtlasBonuses
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.sheet.MightReading
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.Shift

/**
 * Герой над контентом: что экраны выводят из пары «контент + герой». Одна логика для срезов экранов.
 */
internal object HeroLens {
    /** Память видов копий: одна на приложение, сбрасывается сама с новым контентом. */
    private val views = ItemViews()

    /** The view of an item over the content on screen, or null before the content has been read. */
    fun view(index: ContentIndex?, item: ItemInstance): ItemView? = index?.let { views.of(item, it) }

    /** What the merchant pays for [item], by the rules' price; null until the content has arrived. */
    fun sellPrice(index: ContentIndex?, item: ItemInstance): Long? = view(index, item)?.sellPrice()

    /**
     * What putting [item] on would change on the sheet - на место [place] или туда, что выберут правила; и Мощь
     * героя с вещью.
     */
    fun wearing(index: ContentIndex?, hero: HeroView?, item: ItemInstance, place: Slot? = null): WearOutcome {
        index ?: return WearOutcome.NONE
        hero ?: return WearOutcome.NONE
        return Sheets.wearing(index, item, hero.level, hero.heroClass, hero.tree, hero.items, hero.figures.values, place, hero.info.laws, hero.info.fate, hero.skills)
    }

    /**
     * Что даст самоцвет [jewel] в гнезде [socket] вместо того, что там стоит: сдвиги сводки и Мощь героя с заменой
     * ([Sheets.socketing]). Считается клиентом без запроса - вызывать вне главного потока.
     */
    fun socketing(index: ContentIndex?, hero: HeroView?, jewel: ItemInstance, socket: String): WearOutcome {
        index ?: return WearOutcome.NONE
        hero ?: return WearOutcome.NONE
        return Sheets.socketing(index, jewel, socket, hero.level, hero.heroClass, hero.tree, hero.items, hero.figures.values, hero.info.laws, hero.info.fate, hero.skills)
    }

    /**
     * Что изменит в листе взятие узла [code] дерева с вариантом [choice] (у узла с выбором); пусто - героя или контента нет
     * или узел уже взят. Считается клиентом без запроса - вызывать вне главного потока.
     */
    fun allocationDelta(index: ContentIndex?, hero: HeroView?, code: String, choice: Int?): List<StatDelta> {
        index ?: return emptyList()
        hero ?: return emptyList()
        if (hero.tree.any { it.code == code }) return emptyList()
        return Sheets.allocating(index, TakenNode(code, choice), hero.level, hero.heroClass, hero.tree, hero.items, hero.figures.values, hero.info.laws, hero.info.fate, hero.skills)
    }

    /** Память вердиктов: одна на приложение, сбрасывается сама с новым героем или контентом. */
    private val verdicts = GearVerdicts()

    /**
     * Урон и защита героя, если надеть [item] - на место [place], а без него или для слота с одним местом -
     * на лучшее; null - вещь не надевается, надета или не по требованиям.
     */
    fun verdict(index: ContentIndex?, hero: HeroView?, item: ItemInstance, place: Slot? = null): GearVerdict? = place?.let { wanted -> verdicts.places(index, hero, item).firstOrNull { it.slot == wanted } }?.verdict ?: verdicts.of(index, hero, item)

    /** Места [item] с тем, что там надето, и вердиктом каждого; пусто - вещь не надеть. */
    fun wearPlaces(index: ContentIndex?, hero: HeroView?, item: ItemInstance): List<WearPlace> = verdicts.places(index, hero, item)

    /** Чего копии [item] не хватает против листа героя, словами правил ([Sheets.unmet]); пусто - можно надеть. */
    fun unmet(index: ContentIndex?, hero: HeroView?, item: ItemInstance): List<String> {
        index ?: return emptyList()
        hero ?: return emptyList()
        return Sheets.unmet(index, item, hero.level, hero.stats)
    }

    /** What the hero's auras hold of the pool [pool], as the fight holds it (лист боя); null until the hero's sheet is here. */
    fun reserve(hero: HeroView?, pool: ReservePool): PoolReserve? = hero?.combat?.reserve(pool)

    /** The orbs of the world, in the order of their price: what the forge and the auction offer. */
    fun orbs(index: ContentIndex?): List<Item> = index?.let(orbsMemo::of).orEmpty()
    private val orbsMemo = LastOf { i: ContentIndex -> i.itemsByCategory[Item.CURRENCY].orEmpty().sortedBy { it.price } }

    /** The auction's money (every orb, `AuctionRules.trades`): what a lot is priced, bought and filtered in, cheapest first. */
    fun currencies(index: ContentIndex?): List<Item> = index?.let(currenciesMemo::of).orEmpty()
    private val currenciesMemo = LastOf { i: ContentIndex -> orbs(i).filter(i.rules.auction::trades) }

    /** The bench lines the hero has found; the rest of the bench stays hidden. */
    fun bench(index: ContentIndex?, hero: HeroView?): List<BenchRecipe> = benchMemo.of(index, hero).orEmpty()
    private val benchMemo = LastPair { i, h -> i.bench.filter { it.code in h.info.recipes } }

    /** Which zones the hero has passed and which are open: derived from the hero, no request. */
    fun progress(index: ContentIndex?, hero: HeroView?): CampaignProgress? = progressMemo.of(index, hero)
    private val progressMemo = LastPair { i, h -> CampaignProgress(h.campaign.cleared.filter { it in i.zones }, i.world.unlocked(h.campaign.cleared)) }

    /** The hero's atlas as the rules count it: points earned and free. */
    fun atlasState(index: ContentIndex?, hero: HeroView?): AtlasState? = atlasMemo.of(index, hero)

    /**
     * Плашки особого блока карточки копии [item], что не строки вещи: у карты-предмета - захват влиянием ([MapCapture]) и качество
     * ([mapQuantity]) с Атласом героя; у прочих вещей - пусто.
     */
    fun itemSeals(index: ContentIndex?, hero: HeroView?, item: ItemInstance): List<CardSeal> {
        val i = index ?: return emptyList()
        if (i.template(item.template)?.slot != Slot.MAP) return emptyList()
        val atlas = AtlasBonuses.of(i.atlasGraph, hero?.info?.atlas.orEmpty(), i::modifier)
        return listOfNotNull(
            MapCapture.of(i.campaign.maps.influence, item.influence) { atlas.effects[it] ?: 0.0 }?.let(CardSeal::Capture),
            mapQuantity(i, atlas, item)?.let { CardSeal.MapQuality(item.quality, it) },
        )
    }

    /**
     * Что качество карты-предмета прибавляет к количеству добычи, проценты: с «эффектом модификаторов карт» Атласа [atlas], тем же
     * расчётом правил, что окно входа в карту ([LootRoller.mapEffects] над одним качеством копии); без качества - null.
     */
    private fun mapQuantity(index: ContentIndex, atlas: AtlasBonuses, item: ItemInstance): Double? {
        if (item.quality <= 0) return null
        return LootRoller(index).mapEffects(item.copy(rolls = emptyList(), mapTier = 0), atlas.mapEffect)[MapStat.QUANTITY.code]
    }
    private val atlasMemo = LastPair { i, h ->
        AtlasState(
            listOf(i.atlasGraph.start) + h.info.atlas,
            h.info.earned,
            AtlasPoints.total(i.atlas.points, h.info.earned, i.atlas.cap),
            AtlasPoints.available(i.atlas.points, h.info.earned, h.info.atlas, i.atlas.cap),
        )
    }

    /** The hero's tree as the rules count it: the point balance and what the taken nodes give. */
    fun treeState(index: ContentIndex?, hero: HeroView?): TreeState? = treeMemo.of(index, hero)

    /**
     * Дерево раскрывается раз на героя и контент: нижняя навигация спрашивает его на каждый срез игры.
     *
     * Вклад «за каждые N» - от листа героя, как у сервера.
     */
    private val treeMemo = LastPair { i, h ->
        val total = i.classes.pointsTotal(h.level) + h.info.bonusPoints
        val spent = i.tree.spent(h.tree)
        val calculator = SheetCalculator(i)
        TreeState(total, spent, total - spent, h.tree, calculator.totals(calculator.expand(i.tree.lines(h.tree))) { h.stats[it] ?: 0.0 })
    }
}

fun GameUi.view(item: ItemInstance): ItemView? = HeroLens.view(index, item)

/** Плашки особого блока карточки копии [item], что не строки вещи ([HeroLens.itemSeals]). */
fun GameUi.itemSeals(item: ItemInstance): List<CardSeal> = HeroLens.itemSeals(index, hero, item)
fun GameUi.sellPrice(item: ItemInstance): Long? = HeroLens.sellPrice(index, item)

/** Свободных мест самого тайника героя по правилу сервера (`StashRules.free`), без переполнения; нет героя или правил - null. */
fun GameUi.stashFree(): Int? = hero?.let { hero -> index?.rules?.stash?.free(hero.items.size, hero.info.stashSlots) }
fun GameUi.wearing(item: ItemInstance, place: Slot? = null): WearOutcome = HeroLens.wearing(index, hero, item, place)
fun GameUi.socketing(jewel: ItemInstance, socket: String): WearOutcome = HeroLens.socketing(index, hero, jewel, socket)

/** Что даст самоцвет [jewel] в гнезде [socket] этому герою над контентом [index] ([HeroLens.socketing]) - расчёт над захваченным героем. */
fun HeroView.socketing(index: ContentIndex, jewel: ItemInstance, socket: String): WearOutcome = HeroLens.socketing(index, this, jewel, socket)
fun GameUi.allocationDelta(code: String, choice: Int?): List<StatDelta> = HeroLens.allocationDelta(index, hero, code, choice)
fun GameUi.gearVerdict(item: ItemInstance, place: Slot? = null): GearVerdict? = HeroLens.verdict(index, hero, item, place)
fun GameUi.wearPlaces(item: ItemInstance): List<WearPlace> = HeroLens.wearPlaces(index, hero, item)
fun GameUi.unmetFor(item: ItemInstance): List<String> = HeroLens.unmet(index, hero, item)

/** Вещи тайника героя, что встают на место [place]: не в гнёздах, слотом под это место; пусто - нечем заменить. */
fun GameUi.placeCandidates(place: BodyPlace): List<ItemView> = hero?.stash.orEmpty().filter { !it.socketed }.mapNotNull { view(it) }.filter { place.takes(it.slot) }

/** Какие требования копии [item] герой не выполняет - по чипу карточки; героя или шаблона нет - null (без отметок). */
fun GameUi.unmetNeeds(item: ItemInstance): Set<NeedKind>? {
    val hero = hero ?: return null
    val template = index?.template(item.template) ?: return null
    return unmetNeeds(template, hero.heroClass, unmetFor(item))
}
fun GameUi.reserve(pool: ReservePool): PoolReserve? = HeroLens.reserve(hero, pool)

/**
 * Цифры сводки героя: лист боя с пассивными умениями и Предначертанием ([CombatSheet]); до него - лист как есть. Сводка,
 * разбор и сравнения вещи и узла дерева читают одни и те же цифры.
 */
val HeroView.figures: StatFigures get() = combat?.figures ?: StatFigures(stats)

/**
 * Лист героя под эффектами захода [effects] ([RunSheet]): лист боя сводки с зельем и гильдией в своде и картой поверх; до листа
 * боя - над цифрами сводки как есть.
 */
fun HeroView.runSheet(effects: Map<String, Double>, origins: EffectOrigins): RunSheet = combat?.runSheet(effects, origins) ?: RunSheet.over(figures.stats, effects, origins)

/**
 * Откуда цифры сводки [stats] под сдвигами захода [shifts] ([RunSheet.shifts]): по цифре - её сдвиги, сложенные по источнику; лист боя
 * добавляет сдвиги того, что читается вместе с цифрой ([CombatSheet.sources]), до листа боя - только свои сдвиги цифры.
 */
fun HeroView.runSources(stats: Collection<String>, shifts: Map<String, List<Shift>>): Map<String, List<Shift>> =
    combat?.sources(stats, shifts) ?: stats.associateWith { SourceShifts.bySource(shifts[it].orEmpty()) }.filterValues { it.isNotEmpty() }

/** Мощь героя - из того же листа боя без карты, что сводка; null - листа боя ещё нет. */
val HeroView.might: MightReading? get() = combat?.might

/**
 * Последний ответ по контенту: тот же [ContentIndex] (по ссылке) - тот же ответ; срез игры пересоздаётся часто, а
 * контент и герой в нём - те же объекты, пока не пришли новые.
 */
private class LastOf<R>(private val compute: (ContentIndex) -> R) {
    @Volatile private var last: Pair<ContentIndex, R>? = null

    fun of(index: ContentIndex): R {
        last?.takeIf { it.first === index }?.let { return it.second }
        return compute(index).also { last = index to it }
    }
}

/**
 * Память видов копий ([ItemView]) по содержимому копии: та же копия над тем же контентом даёт тот же вид, и его ленивые строки
 * считаются раз, а не при каждой перерисовке списка. Копия - только по значению: ключ ([Key]) - id и хеш её полей, посчитанный
 * один раз на запрос, совпавший ключ сверяется с самой запомненной копией; мутаторы правил на запомненной копии не зовутся,
 * изменённая копия даёт другой хеш и другой вид. Помнится и отсутствие вида (шаблона нет в контенте) - неизвестная копия не ищется
 * в контенте каждый раз. Вид одной копии и одного контента одинаков - память не меняет отображение. Держит не больше [LIMIT] видов,
 * давно не спрошенные уходят первыми; новый контент сбрасывает всё. Потокобезопасна: виды спрашивают и вне главного потока
 * (фильтр вещей, примерка, ремёсла).
 */
private class ItemViews(private val limit: Int = LIMIT) {
    private var index: ContentIndex? = null
    private val known = object : LinkedHashMap<Key, Entry>(limit * 4 / 3 + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Entry>): Boolean = size > limit
    }

    /** Ключ памяти: id копии и хеш её полей. */
    private data class Key(val id: String, val hash: Int)

    /** Запомненная копия и её вид; null - шаблона копии в контенте нет. */
    private class Entry(val item: ItemInstance, val view: ItemView?)

    /** Вид копии [item] над контентом [index] из памяти или заново; null - шаблона копии в контенте нет. */
    fun of(item: ItemInstance, index: ContentIndex): ItemView? = synchronized(this) {
        if (this.index !== index) {
            this.index = index
            known.clear()
        }
        val key = Key(item.id, item.hashCode())
        known[key]?.takeIf { it.item == item }?.let { return it.view }
        ItemView.of(item, index).also { known[key] = Entry(item, it) }
    }

    private companion object {
        /** Сколько видов держит память: с запасом на тайник, лут похода и витрину разом. */
        const val LIMIT = 512
    }
}

/** Последний ответ по паре «контент + герой», по ссылкам; без контента или героя - null. */
private class LastPair<R : Any>(private val compute: (ContentIndex, HeroView) -> R) {
    @Volatile private var last: Triple<ContentIndex, HeroView, R>? = null

    fun of(index: ContentIndex?, hero: HeroView?): R? {
        index ?: return null
        hero ?: return null
        last?.takeIf { it.first === index && it.second === hero }?.let { return it.third }
        return compute(index, hero).also { last = Triple(index, hero, it) }
    }
}
