package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.verification.CheckResult
import kotlinx.serialization.json.JsonObject

enum class AppMode { PLAYER, ADMIN }

data class ForgeState(
    val lang: Lang = uiLanguage,
    val mode: AppMode = AppMode.PLAYER,
    val failure: FailureState? = null,

    val profile: UserProfile? = null, val signedIn: Boolean = false, val sessionEpoch: Int = 0,
    val server: String = "http://10.0.2.2:8080/", val serverDraft: String = "http://10.0.2.2:8080/",
    val busy: Boolean = true, val message: String? = null, val error: Boolean = false,

    val tab: Int = 3, val catalog: Catalog = Catalog.EQUIPMENT,
    val filter: CatalogFilter = CatalogFilter(), val query: String = "",
    val items: List<JsonObject> = emptyList(), val page: Int = 0, val totalPages: Int = 0, val total: Long = 0,

    val original: JsonObject? = null, val editorOpen: Boolean = false, val draft: JsonObject = JsonObject(emptyMap()),
    /** The shared modifier catalogue, read once per session so rolled values can be named. */
    val definitions: List<ModifierDefinition> = emptyList(),

    val characterId: String = "", val characterOwner: String = "", val hero: HeroView? = null,
    /** Templates of the instances on screen, keyed by `equipmentId`; an instance carries only rolls. */
    val inventoryBases: Map<String, JsonObject> = emptyMap(),
    val selectedEquipment: String = "",
    /** What the admin's random grant asks the server for. Blank means "any". */
    val grantRarity: String = "", val grantSlot: String = "",
    /** The currency catalogue, read once per session; `selectedOrb` is the `items` id of one orb. */
    val orbs: List<CurrencyItem> = emptyList(), val selectedOrb: String = "",
    /** The world's reference tables, read once per session: classes and the shared skill tree. */
    val classes: List<CharacterClass> = emptyList(), val treeNodes: List<SkillTreeNode> = emptyList(),
    /** The class a new character is being created with, and the tree node under the cursor. */
    val draftClass: String = "", val selectedNode: String = "",
    /** What the tree search box holds; a match moves the map to that node. */
    val nodeQuery: String = "",

    /** Which of the auction's own tabs is open: 0 showcase, 1 my lots, 2 sell. */
    val auctionTab: Int = 0,
    /** The showcase as the server paged it, and the filter it was asked for. */
    val showcase: AuctionPage = AuctionPage(), val auctionFilter: AuctionFilter = AuctionFilter(),
    /** Own lots are dropped from the showcase by default: they cannot be bought anyway. */
    val showOwnLots: Boolean = false,
    val myLots: List<AuctionLot> = emptyList(),
    /**
     * Why the auction is closed to this character, in the server's own words.
     *
     * The level it opens at is the server's constant; the client never carries a copy of it and
     * learns the gate only by being refused.
     */
    val auctionLocked: String? = null,

    /**
     * The server's dictionary for the current language, and how many strings it holds.
     *
     * Since 0.14.0 no document carries text: an entity stores a code and the name lives here. The
     * bundle itself is global (`serverLocale`) because `core` renders from it without a state
     * object; what is kept here is only what the screens need to report — which language is loaded
     * and whether it arrived at all.
     */
    val localeLanguage: String = "", val localeStrings: Int = 0,

    val checks: List<CheckResult> = emptyList(),
    val health: String = tr("Соединение ещё не проверено", "The connection has not been checked yet"),
) {
    val isAdmin: Boolean get() = signedIn && profile?.role == "ADMIN"
    /** Lots the character may act on: the showcase hides their own unless asked not to. */
    val ownLots: List<AuctionLot> get() = myLots.filter { it.onSale }
    /** The class the shown hero belongs to; the server owns the base it hands out. */
    val heroClass: CharacterClass? get() = hero?.let { view -> classes.firstOrNull { it.id == view.character.classId } }
    val adminTools: Boolean get() = isAdmin && mode == AppMode.ADMIN
    val canEdit: Boolean get() = signedIn && (catalog == Catalog.CHARACTERS || adminTools)
    val ownsCharacter: Boolean get() = signedIn && profile?.id == characterOwner
}
