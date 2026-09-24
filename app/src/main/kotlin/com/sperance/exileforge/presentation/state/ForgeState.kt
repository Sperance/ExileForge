package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.CampaignView
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.CharacterSummary
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.modifier.BenchRecipe
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.verification.CheckResult
import kotlinx.serialization.json.JsonObject

enum class AppMode { PLAYER, ADMIN }

/**
 * Which of the three screens the app is on, above the tabs.
 *
 * The tabs only make sense once there is an account *and* a character: every command below them
 * names a character, so the gate is what lets them stop asking which one. [CHARACTERS] is also
 * the only way back — a character is swapped by leaving the game, never from inside it.
 */
enum class AppPhase { AUTH, CHARACTERS, GAME }

/**
 * The whole app, as one immutable value.
 *
 * What every screen needs — where the app is, the language, the lanes of work in flight and the
 * last word the server said — sits at the top. Everything else is grouped by whose it is, so a
 * change to one part is written as a copy of that part and reads as such:
 * [account] is the session and the characters it owns, [world] the reference tables read once
 * per session, [play] the character being played, [market] the auction, and [admin] the
 * administrator's catalogue, editor, promo codes and checks.
 */
data class ForgeState(
    val phase: AppPhase = AppPhase.AUTH,
    val lang: Lang = uiLanguage,
    val mode: AppMode = AppMode.PLAYER,
    val failure: FailureState? = null,
    /**
     * [busy] is a command in flight — the one thing that disables controls, because two writes at
     * once could spend the same orb twice. [loading] names the reads in flight: they run beside a
     * command and beside each other, and only say that something is on its way.
     */
    val busy: Boolean = true, val loading: Set<String> = emptySet(), val message: String? = null, val error: Boolean = false,
    val tab: Int = 3,
    val account: AccountState = AccountState(),
    val world: WorldState = WorldState(),
    val play: PlayState = PlayState(),
    val market: MarketState = MarketState(),
    val admin: AdminState = AdminState(),
) {
    val isAdmin: Boolean get() = account.signedIn && account.profile?.role == "ADMIN"
    val reading: Boolean get() = loading.isNotEmpty()
    /** Whether a pull on this list is still being answered, by a read of its own or by a command. */
    fun refreshing(read: String): Boolean = busy || read in loading
    /**
     * The character every command on every tab acts on; the gate guarantees there is one.
     *
     * The loaded hero wins over the menu's snapshot, which was taken before any experience was
     * earned — otherwise the banner would keep showing the level the player came in on.
     */
    val character: CharacterSummary? get() = play.hero?.character?.takeIf { it.id == play.characterId }
        ?: account.characters.firstOrNull { it.id == play.characterId }
    /** The server refuses a fourth, so the button that would ask for one is not offered. */
    val characterSlotsLeft: Int get() = (MAX_CHARACTERS - account.characters.size).coerceAtLeast(0)
    /** An account nobody named: a device registration leaves `name` and `login` empty. */
    val accountTitle: String get() = account.profile?.name?.takeIf { it.isNotBlank() }
        ?: account.profile?.login?.takeIf { it.isNotBlank() }
        ?: ui("session.guest") + " · …${account.deviceId.takeLast(6)}"
    /** Lots the character may act on: the showcase hides their own unless asked not to. */
    val ownLots: List<AuctionLot> get() = market.myLots.filter { it.onSale }
    /** The class the shown hero belongs to; the server owns the base it hands out. */
    val heroClass: CharacterClass? get() = play.hero?.let { view -> world.classes.firstOrNull { it.id == view.character.classId } }
    val adminTools: Boolean get() = isAdmin && mode == AppMode.ADMIN
    /** The editor is an administrator's tool now that a character is made from the menu. */
    val canEdit: Boolean get() = account.signedIn && adminTools
    val ownsCharacter: Boolean get() = account.signedIn && account.profile?.id == play.characterOwner
    /**
     * How many of one stacking item the hero holds, or null while the hero has not been read.
     *
     * A confirmation prints what a purchase or a refund leaves behind; before the bag is known it
     * prints nothing rather than a guess of zero.
     */
    /** The refusal to print where the action was taken; a success is never shown (2.46.0). */
    val refusal: String? get() = message.takeIf { error }
    fun bagAmount(itemId: String): Long? = play.hero?.let { view -> view.bag.firstOrNull { it.itemId == itemId }?.amount ?: 0L }
    /** The bag's orb of the given kind — the document a spent orb is counted by. */
    fun orbOf(kind: com.sperance.exileforge.core.model.currency.CurrencyOrb) = world.orbs.firstOrNull { it.orb == kind }
}

/** The session: who is signed in, to which server, and which characters they own. */
data class AccountState(
    val profile: UserProfile? = null, val signedIn: Boolean = false, val sessionEpoch: Int = 0,
    /** A kept session the server could not be reached to confirm: the sign-in screen offers to try again. */
    val resumable: Boolean = false,
    val server: String = "http://10.0.2.2:8080/", val serverDraft: String = "http://10.0.2.2:8080/",
    /** The identifier this device registers under; shown so a support log can name the account. */
    val deviceId: String = "",
    /**
     * The account's characters, as the server narrowed them, and whether they have been read yet.
     *
     * [charactersRead] is not `characters.isNotEmpty()`: an account with none is a real answer and
     * the one that opens the creation form, so "no characters" and "not asked yet" must differ.
     */
    val characters: List<CharacterSummary> = emptyList(), val charactersRead: Boolean = false,
    val health: String = ui("runtime.not_checked"),
)

/** The world's reference tables: seeded, fixed for a session, and read once. */
data class WorldState(
    /** The shared modifier catalogue, read once per session so rolled values can be named. */
    val definitions: List<ModifierDefinition> = emptyList(),
    /** The currency catalogue; an orb is an `items` document of category `CURRENCY`. */
    val orbs: List<CurrencyItem> = emptyList(),
    /** The materials the crafts gather (2.41.0), so the bag can name them. */
    val materials: List<com.sperance.exileforge.core.model.crafts.MaterialItem> = emptyList(),
    /** The crafting bench: crafted modifiers by tier and their price in orbs. Fixed per server. */
    val bench: List<BenchRecipe> = emptyList(),
    /** Classes and the shared skill tree; a class carries the stat base, the tree its graph. */
    val classes: List<CharacterClass> = emptyList(), val treeNodes: List<SkillTreeNode> = emptyList(),
    /** The level table, read with the classes: it says what the next level costs. */
    val levels: List<ExperienceLevel> = emptyList(),
    /** Templates of the instances on screen, keyed by `equipmentId`; an instance carries only rolls. */
    val inventoryBases: Map<String, JsonObject> = emptyMap(),
    /** The stat order, slot order and price rule the sheet is added up by here (server 0.41.0). */
    val statTables: com.sperance.exileforge.core.character.StatTables = com.sperance.exileforge.core.character.StatTables(),
    /**
     * The server's dictionary for the current language, and how many strings it holds.
     *
     * Since 0.14.0 no document carries text: an entity stores a code and the name lives there. The
     * bundle itself is global (`serverLocale`) because `core` renders from it without a state
     * object; what is kept here is only what the screens need to report — which language is loaded
     * and whether it arrived at all.
     */
    val localeLanguage: String = "", val localeStrings: Int = 0,
    /**
     * Which languages the player may choose from.
     *
     * The list is the server's, not the client's: `locale/index.json` says which dictionaries
     * exist, and a language the server cannot name items in would be half a translation - the
     * interface in one tongue and every item in another. Until a manifest arrives it is the two
     * every server has always served, so the picker is never empty.
     */
    val languages: List<Lang> = listOf(Lang.RU, Lang.EN),
    /**
     * How much of the server's icon set arrived: codes covered and drawings behind them.
     *
     * The set is global (`serverIcons`) for the same reason the dictionary is — `core` draws from
     * it without a state object. These two numbers exist so a missing set is reportable rather
     * than merely invisible: every hole falls back to a bundled emblem and looks deliberate.
     */
    val iconKeys: Int = 0, val iconSprites: Int = 0,
    /** How many of the server's portraits are drawn from (since 2.31.0); a screen reads it to redraw when they arrive. */
    val portraits: Int = 0,
    /** The campaign's chapters, monsters and rarities, read once per session (server 0.26.0). */
    val campaign: CampaignView? = null,
)

/** The character being played, and what the player has picked on its screens. */
data class PlayState(
    val characterId: String = "", val characterOwner: String = "", val hero: HeroView? = null,
    /**
     * When the hero was last read whole, as epoch millis; 0 means "never, or known to be stale".
     *
     * Reading the hero is five requests, so it is not done on every glance at a tab. A command
     * re-reads it because the command changed it; anything that changed it *elsewhere* — a trade,
     * another device, an administrator — is caught by this stamp going cold.
     */
    val heroReadAt: Long = 0,
    /** When the hero was last actually read (2.45.0); unlike [heroReadAt] it is never set cold. */
    val heroSeenAt: Long = 0,
    val selectedEquipment: String = "",
    /** The `items` id of the orb picked in the forge. */
    val selectedOrb: String = "",
    /** Which of the forge's three sections is open, and the server's sentence about the last thing it did. */
    val forgeSection: ForgeSection = ForgeSection.ORBS, val forgeLine: String = "",
    /** What the admin's random grant asks the server for. Blank means "any". */
    val grantRarity: String = "", val grantSlot: String = "",
    /** The class a new character is being created with, and the tree node under the cursor. */
    val draftClass: String = "", val selectedNode: String = "",
    /** What the tree search box holds; a match moves the map to that node. */
    val nodeQuery: String = "",
    /** Which campaign maps this character has cleared and which are open. */
    val campaign: CampaignProgress? = null,
    /** The location whose launch window is open (since 2.37.0): its chests, its boss and the map picked for it. */
    val launch: MapLaunchState? = null,
    /**
     * The crafts (2.41.0) as the server last answered, with the device's clock at that moment so a
     * cycle's bar can run between answers; the profession whose window is open ("" is the tiles);
     * and what the answers of this session brought, newest first.
     */
    val crafts: com.sperance.exileforge.core.model.crafts.CraftsState? = null,
    val craftsAt: Long = 0,
    val craftsProfession: String = "",
    val craftsLog: List<com.sperance.exileforge.core.model.crafts.WorkGains> = emptyList(),
    /** The gear this run brought (2.45.0), with when each piece landed, for the gear sheet's «Новый лут». */
    val runLoot: List<LootEntry> = emptyList(),
)

/** A piece a run brought and the moment it landed: a hero read after that moment says whether it is still loose. */
data class LootEntry(val item: com.sperance.exileforge.core.model.hero.EquipmentInstance, val at: Long)

/**
 * One location's launch window: its chests and boss as the server last said (null until it has),
 * and the stash map picked to enter it with — null enters without one.
 */
data class MapLaunchState(
    val mapCode: String,
    val chests: com.sperance.exileforge.core.model.campaign.ChestState? = null,
    val boss: com.sperance.exileforge.core.model.campaign.BossState? = null,
    val picked: String? = null,
)

/** The forge's sections: orbs and the bench work on one item, a recipe on the bag. */
enum class ForgeSection { ORBS, BENCH, RECIPES }

/** The auction, as this character sees it. */
data class MarketState(
    /** Which of the auction's own tabs is open: 0 showcase, 1 my lots, 2 sell. */
    val tab: Int = 0,
    /** The showcase as the server paged it, and the filter it was asked for. */
    val showcase: AuctionPage = AuctionPage(), val filter: AuctionFilter = AuctionFilter(),
    /** Own lots are dropped from the showcase by default: they cannot be bought anyway. */
    val showOwnLots: Boolean = false,
    val myLots: List<AuctionLot> = emptyList(),
    /**
     * Why the auction is closed to this character, in the server's own words.
     *
     * The level it opens at is the server's constant; the client never carries a copy of it and
     * learns the gate only by being refused.
     */
    val locked: String? = null,
    /** The merchant's shelf for this hero (0.34.0) and the hero's lot places. */
    val merchant: com.sperance.exileforge.core.model.trade.MerchantStock? = null,
    val slots: com.sperance.exileforge.core.model.auction.AuctionSlots? = null,
)

/** The administrator's tools: the catalogue and its editor, promo codes and the self-checks. */
data class AdminState(
    val catalog: Catalog = Catalog.EQUIPMENT,
    val filter: CatalogFilter = CatalogFilter(), val query: String = "",
    val items: List<JsonObject> = emptyList(), val page: Int = 0, val totalPages: Int = 0, val total: Long = 0,
    val original: JsonObject? = null, val editorOpen: Boolean = false, val draft: JsonObject = JsonObject(emptyMap()),
    /**
     * Promo codes, as an administrator sees them.
     *
     * They are not a [Catalog]: a player never lists them, only types one in, so they have no
     * page, no filter and no place in the catalogue's three collections.
     */
    val redemptions: List<RedemptionCode> = emptyList(),
    val checks: List<CheckResult> = emptyList(),
)

/**
 * How many characters one account may hold — the server's `CONST_USER_MAX_CHARACTERS`.
 *
 * A copy of a server constant, which the client normally refuses to keep. It earns its place by
 * being display only: it greys the create button early instead of letting the player fill a form
 * the server will reject with `CH_005`. The refusal is still shown if the numbers ever disagree.
 */
const val MAX_CHARACTERS = 3

/**
 * What a read reads. One read per name runs at a time, and a command names the reads it will
 * redo itself, so none of them lands after it with what was true before.
 */
object Reads {
    const val HERO = "hero"
    const val CHARACTERS = "characters"
    const val AUCTION = "auction"
    const val LOTS = "lots"
    const val CATALOG = "catalog"
    const val PROGRESSION = "progression"
    const val REDEMPTIONS = "redemptions"
    const val HEALTH = "health"
    const val DEFINITIONS = "definitions"
    const val CAMPAIGN = "campaign"
    const val MERCHANT = "merchant"
    const val MAP_SERVICES = "mapServices"
    const val CRAFTS = "crafts"
}

/**
 * The tabs, by name.
 *
 * Five of them are the bottom bar a player sees — the expedition joined them in 2.24.0 — and
 * [TAB_ADMIN] is the one an administrator has on top of it. Everything else is a screen a button opens: the forge from the Hero tab, and the
 * catalogue, the editor and the checks from the administrator's tab. They are named because a
 * bare number in another file says nothing about which screen it is.
 */
const val TAB_CATALOG = 0
const val TAB_EDITOR = 1
const val TAB_CHECKS = 2
const val TAB_ACCOUNT = 3
const val TAB_HERO = 4
const val TAB_TREE = 5
const val TAB_AUCTION = 6
const val TAB_CRAFT = 7
const val TAB_ADMIN = 8
const val TAB_REDEMPTION = 9
const val TAB_EXPEDITION = 10
/** The crafts (2.41.0): the bar's place the tree left. */
const val TAB_CRAFTS = 11

/** What the bottom bar offers a player — and, with [TAB_ADMIN] appended, an administrator. */
/** The bar. The tree left it in 2.40.0: it opens from the Hero tab's header, as the forge does. */
val PLAYER_TABS = listOf(TAB_HERO, TAB_EXPEDITION, TAB_CRAFTS, TAB_AUCTION, TAB_ACCOUNT)

/** Screens only an administrator may open, whichever button leads to them. */
val ADMIN_TABS = setOf(TAB_CATALOG, TAB_EDITOR, TAB_CHECKS, TAB_ADMIN, TAB_REDEMPTION)
