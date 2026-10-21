package com.sperance.exileforge.core.model.sync

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.display.PortraitManifest
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.core.model.campaign.RunState
import com.sperance.exileforge.core.model.command.ApiCapabilities
import com.sperance.exileforge.core.model.command.RouteInfo
import com.sperance.exileforge.core.model.crafts.WorkState
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.core.model.trade.MerchantStock
import com.sperance.exileforge.rules.content.RULES_VERSION
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ItemBuckets
import com.sperance.exileforge.rules.roll.ItemInstance
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Ревизия API, против которой написан клиент: совпадает с `API_REVISION` закреплённого сервера (сабмодуль `backend/`) и меняется
 * вместе с ним. История ревизий - `audit/versions.md`.
 */
const val API_REVISION = 99

/** `static/index.json` → `content`: the fingerprint of the whole world and of each of its chunks, by file name. */
@Serializable data class ContentManifest(val hash: String = "", val chunks: Map<String, String> = emptyMap())

/**
 * `static/index.json`: everything the client checks at start — the routes it decides features by and every fingerprint it keeps.
 * Маршруты - только те, что вызывающий может звать: без входа - открытые.
 */
@Serializable data class StaticManifest(
    val version: String = "",
    val revision: Int = 0,
    val routes: List<RouteInfo> = emptyList(),
    val locale: LocaleManifest = LocaleManifest(),
    val icons: IconManifest = IconManifest(),
    val portraits: PortraitManifest = PortraitManifest(),
    val content: ContentManifest = ContentManifest(),
    /** The server's rules version: other rules roll runs differently, so the client needs a new build, not new content. */
    val rules: Int = 0,
    /** Коммит работающего сервера: экран «Контракт сервера» показывает его рядом с закреплённым. */
    val commit: String = "",
) {
    val capabilities: ApiCapabilities get() = ApiCapabilities.of(routes)

    /** The server speaks this client's wire and rules; another one may be mid-deploy. */
    val matchesClient: Boolean get() = revision == API_REVISION && rules == RULES_VERSION

    /**
     * A server of another revision speaks another wire: the client refuses it rather than misreading it. Маршруты - после входа
     * : без сессии манифест несёт только открытые ([com.sperance.exileforge.core.network.GameApi.requireRoutes]).
     */
    fun requireWorkbench() {
        require(revision == API_REVISION) { ui("cmd.stale_revision", revision, API_REVISION) }
        require(rules == RULES_VERSION) { ui("cmd.stale_rules", rules, RULES_VERSION) }
    }
}

/** One part of the hero and its fingerprint (sixteen hex digits of the part's JSON); [data] без значения - `null` части (закрытый заход). */
@Serializable data class HeroPart(val version: String, val data: JsonElement = JsonNull)

/** The hero as the server answers it: only the parts whose fingerprints the client did not hold. */
@Serializable data class HeroSnapshot(val version: String, val parts: Map<String, HeroPart> = emptyMap(), val unread: Int = 0, val now: Long = 0)

/**
 * The parts of one hero the client holds. Their fingerprints travel back in `X-Hero-Parts` with every
 * command and every read, so the server sends only what moved; [version] is the document's own and
 * answers `If-None-Match`. The items come in [ItemBuckets] by id and their order as a part of its own: a new drop moves one bucket and the order, not the whole stash.
 */
class HeroParts(val heroId: String, val version: String = "", private val parts: Map<String, HeroPart> = emptyMap(), private val memo: PartMemo = PartMemo()) {
    val complete: Boolean get() = NAMES.all { it in parts }

    /**
     * Можно ли спросить героя по версии (`If-None-Match`): все части на руках и ни одна часть, что сервер считает по часам
     * ([CLOCKED]), к [now] не дошла до своего срока - иначе 304 прежней версии оставил бы её устаревшей.
     */
    fun conditional(now: Long): Boolean = complete && version.isNotBlank() && CLOCKED.none { (name, due) -> name in parts && due(this) in 1..now }

    /** `hero=<hash>,items=<hash>,…`, or `none`: the header is how a client asks for a snapshot at all. */
    fun header(): String = parts.entries.joinToString(",") { "${it.key}=${it.value.version}" }.ifEmpty { "none" }

    /**
     * [fresh] - the snapshot is the server's answer just now, not the copy kept on the device.
     *
     * Свежий ответ несёт часы сервера ([HeroSnapshot.now]) - единственный миг, когда можно измерить их сдвиг.
     */
    fun merge(snapshot: HeroSnapshot, fresh: Boolean = true): HeroParts {
        if (fresh) ServerClock.heard(snapshot.now)
        return HeroParts(heroId, snapshot.version, parts + snapshot.parts, memo)
    }

    /** Every part held, as one snapshot: what the device keeps of the hero for the next launch. */
    fun snapshot(): HeroSnapshot = HeroSnapshot(version, parts)

    /**
     * Отпечаток листа героя: части, из которых он складывается, - герой, дерево, вещи и их порядок. Совпал с
     * прошлым - лист не пересчитывается (пришли только сумка, заход, ремёсла или торговец).
     */
    fun sheetKey(): String = (listOf(HERO, TREE, ItemBuckets.ORDER) + ItemBuckets.names).joinToString(",") { parts[it]?.version.orEmpty() }

    val hero: HeroInfo get() = decode(HERO, HeroInfo.serializer())
    val items: List<ItemInstance> get() = memo.derived(ITEMS, sheetKey()) {
        val byId = ItemBuckets.names.flatMap { decode(it, ListSerializer(ItemInstance.serializer())) }.associateBy { it.id }
        decode(ItemBuckets.ORDER, ListSerializer(String.serializer())).mapNotNull(byId::get)
    }
    val overflow: List<ItemInstance> get() = decode(OVERFLOW, ListSerializer(ItemInstance.serializer()))
    val bag: Map<String, Long> get() = decode(BAG, MapSerializer(String.serializer(), Long.serializer()))
    val tree: List<TakenNode> get() = decode(TREE, ListSerializer(TakenNode.serializer()))
    /** Кампания с открытым заходом: часть [CAMPAIGN] без захода и часть [RUN] - заход (null - закрыт) - сложены в одно состояние. */
    val campaign: CampaignState get() = memo.derived(CAMPAIGN_RUN, parts.getValue(CAMPAIGN).version + ":" + parts.getValue(RUN).version) {
        decode(CAMPAIGN, CampaignState.serializer()).copy(run = decode(RUN, RunState.serializer().nullable))
    }
    val crafts: WorkState get() = decode(CRAFTS, WorkState.serializer())
    val merchant: MerchantStock get() = decode(MERCHANT, MerchantStock.serializer())

    /** Часть [name] разобранной: та же часть (отпечаток) разбирается один раз на героя. */
    private fun <T> decode(name: String, serializer: KSerializer<T>): T {
        val part = parts.getValue(name)
        return memo.derived(name, part.version) { WireJson.decodeFromJsonElement(serializer, part.data) }
    }

    companion object {
        const val HEADER = "X-Hero-Parts"
        const val HERO = "hero"
        const val OVERFLOW = "overflow"
        const val BAG = "bag"
        const val TREE = "tree"
        const val CAMPAIGN = "campaign"

        /** Открытый заход (`RunState?`, null - закрыт): сервер шлёт его отдельно от кампании, клиент кладёт в `campaign.run`. */
        const val RUN = "run"
        const val CRAFTS = "crafts"
        const val MERCHANT = "merchant"
        val NAMES = listOf(HERO, BAG, TREE, CAMPAIGN, RUN, CRAFTS, MERCHANT, OVERFLOW, ItemBuckets.ORDER) + ItemBuckets.names

        /**
         * Части, что сервер считает по часам, а не по версии героя, и миг (часы сервера, мс), когда каждая устареет сама;
         * 0 - срока нет.
         */
        private val CLOCKED: Map<String, (HeroParts) -> Long> = mapOf(MERCHANT to { it.merchant.refreshAt })

        /** Ключи памяти собранных вещей и кампании с заходом (не части сервера). */
        private const val ITEMS = "#items"
        private const val CAMPAIGN_RUN = "#campaign"
    }
}

/**
 * Память разобранных частей героя: по имени - отпечаток и значение. Слияние снимка несёт её дальше, так что
 * часть, чей отпечаток не сдвинулся, не разбирается заново; сдвинулась - разбирается и заменяет прежнее.
 */
class PartMemo {
    private val values = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Any?>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> derived(name: String, version: String, make: () -> T): T {
        values[name]?.takeIf { it.first == version }?.let { return it.second as T }
        return make().also { values[name] = version to it }
    }
}
