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
import com.sperance.exileforge.core.model.trade.MerchantStock
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
 * unique jewels refused twice with `ST_022`, `atlasNodes` in the run context).
 */
const val API_REVISION = 23

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
) {
    val capabilities: ApiCapabilities get() = ApiCapabilities.of(routes)

    /** A server of another revision speaks another wire: the client refuses it rather than misreading it. */
    fun requireWorkbench() {
        require(revision == API_REVISION) { ui("cmd.stale_revision", revision, API_REVISION) }
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

    fun merge(snapshot: HeroSnapshot): HeroParts = HeroParts(heroId, snapshot.version, parts + snapshot.parts)

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
