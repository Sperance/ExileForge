package com.sperance.exileforge.core.model.sync

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.display.PortraitManifest
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.core.model.command.ApiCapabilities
import com.sperance.exileforge.core.model.command.RouteInfo
import com.sperance.exileforge.core.model.crafts.WorkState
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.PetState
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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

/**
 * The API revision this client is written against (server 1.30.0: run rewards rolled by the server alone and answered per event
 * by `campaign/events`, auction lots that expire; 23 - server 1.31.0: the powers' `STAGE_CLEAR`, `MOMENTUM`, `ECHO` and `RETALIATE`,
 * unique jewels refused twice with `ST_022`, `atlasNodes` in the run context; 24 - server 1.32.0: frenzy, power and endurance charges,
 * the pet's power events, slot powers and the sheet's worn counts, rules version 4); 30 - server 1.47.0: the trials; 31 - server 1.48.0: the rush key; 32 - server 1.49.0: fight tallies and the hero's statistics; 33 - server 1.50.0: influenced maps and the fanned atlas; 34 - server 1.52.0: quests without zones, the branch refund; 35 - server 1.53.0: replayed commands answer without `data`, journals of at most 64 events, 503 when the database is away, the crafting seed withheld, the tower's last floor; 36 - server 1.62.0: the auction showcase by cursor, `rules` in the manifest, the merchant's resale cap; 42 - server 1.69.0: monster traits, the tester role and grants, suggestions, mail, the auction history.; 43 - server 1.75.0: sealed run and trial events, lot goods, profession jobs and redemption rewards with the `kind` discriminator; 44 - server 1.77.0: без Пути изгнанника, сборки приложения с сервера `/app/latest.json`; 45 - server 1.78.0: веса размеров стаи `rules.run.packWeights`, отпечаток устройства `hardwareId` во входе; 46 - server 1.79.0: без раздачи сборок `/app`, сборки с GitHub Releases.; 47 - server 1.80.0: блок героя `CH_034`, привязка логина `user/bind`, отчёты с устройством и версией, лут боя по `ENGAGE`; 48 - server 1.80.8: санкции модерации - `AUTH_006`/`CH_034` с id санкции, экран `notice` и апелляция, устройство во входе, окно `admin/moderation`, `hero/sanctions`; 49 - server 1.80.11: без плана дерева `hero/plan`, бой без отступления, уровни разделов `rules.unlocks`.; 50 - server 1.81.0: незаконченный заход `campaign/abandon`, открытые сундуки захода, простой захода `rules.run.idleMinutes`, алфавит имён `CH_035`/`GU_039`.; 51 - server 1.81.3: объекты карты `FEATURE` (алтари, торговец, ловушки, тайники, узлы ремёсел), общее значение эффекта модификатора.; 52 - server 1.81.7: находки уникальных вещей `hero/uniques`, без пути дерева `skilltree/path`, питомцы в заходе не меняются (`CH_036`).
 */
const val API_REVISION = 52

/** `static/index.json` → `content`: the fingerprint of the whole world and of each of its chunks, by file name. */
@Serializable data class ContentManifest(val hash: String = "", val chunks: Map<String, String> = emptyMap())

/** `static/index.json`: everything the client checks at start — the routes it decides features by and every fingerprint it keeps. */
@Serializable data class StaticManifest(
    val version: String = "",
    val revision: Int = 0,
    val routes: List<RouteInfo> = emptyList(),
    val locale: LocaleManifest = LocaleManifest(),
    val icons: IconManifest = IconManifest(),
    val portraits: PortraitManifest = PortraitManifest(),
    val content: ContentManifest = ContentManifest(),
    /** The server's rules version (server 1.62.0): other rules roll runs differently, so the client needs a new build, not new content. */
    val rules: Int = 0,
    /** Коммит работающего сервера (server 1.80.5): экран «Контракт сервера» показывает его рядом с закреплённым. */
    val commit: String = "",
) {
    val capabilities: ApiCapabilities get() = ApiCapabilities.of(routes)

    /** The server speaks this client's wire and rules; another one may be mid-deploy. */
    val matchesClient: Boolean get() = revision == API_REVISION && rules == RULES_VERSION

    /** A server of another revision speaks another wire: the client refuses it rather than misreading it. */
    fun requireWorkbench() {
        require(revision == API_REVISION) { ui("cmd.stale_revision", revision, API_REVISION) }
        require(rules == RULES_VERSION) { ui("cmd.stale_rules", rules, RULES_VERSION) }
        capabilities.requireWorkbench()
    }
}

/** One part of the hero and its fingerprint (sixteen hex digits of the part's JSON). */
@Serializable data class HeroPart(val version: String, val data: JsonElement)

/** The hero as the server answers it: only the parts whose fingerprints the client did not hold. */
@Serializable data class HeroSnapshot(val version: String, val parts: Map<String, HeroPart> = emptyMap())

/**
 * The parts of one hero the client holds. Their fingerprints travel back in `X-Hero-Parts` with every
 * command and every read, so the server sends only what moved; [version] is the document's own and
 * answers `If-None-Match`. The items come in [ItemBuckets] by id and their order as a part of its own
 * (1.1.0): a new drop moves one bucket and the order, not the whole stash.
 */
class HeroParts(val heroId: String, val version: String = "", private val parts: Map<String, HeroPart> = emptyMap()) {
    val complete: Boolean get() = NAMES.all { it in parts }

    /** `hero=<hash>,items=<hash>,…`, or `none`: the header is how a client asks for a snapshot at all. */
    fun header(): String = parts.entries.joinToString(",") { "${it.key}=${it.value.version}" }.ifEmpty { "none" }

    /** [fresh] - the snapshot is the server's answer just now, not the copy kept on the device. */
    fun merge(snapshot: HeroSnapshot, fresh: Boolean = true): HeroParts {
        // A fresh pets part brings the server's clock: the one moment its offset can be measured.
        if (fresh) snapshot.parts[PETS]?.let { part -> runCatching { WireJson.decodeFromJsonElement(PetState.serializer(), part.data) }.getOrNull()?.incubator?.now?.let(ServerClock::heard) }
        return HeroParts(heroId, snapshot.version, parts + snapshot.parts)
    }

    /** Every part held, as one snapshot: what the device keeps of the hero for the next launch (3.30.0). */
    fun snapshot(): HeroSnapshot = HeroSnapshot(version, parts)

    val hero: HeroInfo get() = decode(HERO, HeroInfo.serializer())
    val items: List<ItemInstance> get() {
        val byId = ItemBuckets.names.flatMap { decode(it, ListSerializer(ItemInstance.serializer())) }.associateBy { it.id }
        return decode(ItemBuckets.ORDER, ListSerializer(String.serializer())).mapNotNull(byId::get)
    }
    val overflow: List<ItemInstance> get() = decode(OVERFLOW, ListSerializer(ItemInstance.serializer()))
    val bag: Map<String, Long> get() = decode(BAG, MapSerializer(String.serializer(), Long.serializer()))
    val tree: List<TakenNode> get() = decode(TREE, ListSerializer(TakenNode.serializer()))
    val campaign: CampaignState get() = decode(CAMPAIGN, CampaignState.serializer())
    val crafts: WorkState get() = decode(CRAFTS, WorkState.serializer())
    val merchant: MerchantStock get() = decode(MERCHANT, MerchantStock.serializer())
    val pets: PetState get() = decode(PETS, PetState.serializer())

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = WireJson.decodeFromJsonElement(serializer, parts.getValue(name).data)

    companion object {
        const val HEADER = "X-Hero-Parts"
        const val HERO = "hero"
        const val OVERFLOW = "overflow"
        const val BAG = "bag"
        const val TREE = "tree"
        const val CAMPAIGN = "campaign"
        const val CRAFTS = "crafts"
        const val MERCHANT = "merchant"
        const val PETS = "pets"
        val NAMES = listOf(HERO, BAG, TREE, CAMPAIGN, CRAFTS, MERCHANT, OVERFLOW, PETS, ItemBuckets.ORDER) + ItemBuckets.names
    }
}
