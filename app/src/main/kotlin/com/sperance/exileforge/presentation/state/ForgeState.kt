package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.i18n.Lang
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

data class ForgeState(
    val phase: AppPhase = AppPhase.AUTH,
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

    /**
     * The account's characters, as the server narrowed them, and whether they have been read yet.
     *
     * [charactersRead] is not `characters.isNotEmpty()`: an account with none is a real answer and
     * the one that opens the creation form, so "no characters" and "not asked yet" must differ.
     */
    val characters: List<CharacterSummary> = emptyList(), val charactersRead: Boolean = false,
    /** The identifier this device registers under; shown so a support log can name the account. */
    val deviceId: String = "",

    val characterId: String = "", val characterOwner: String = "", val hero: HeroView? = null,
    /**
     * When the hero was last read whole, as epoch millis; 0 means "never, or known to be stale".
     *
     * Reading the hero is five requests, so it is not done on every glance at a tab. A command
     * re-reads it because the command changed it; anything that changed it *elsewhere* — a trade,
     * another device, an administrator — is caught by this stamp going cold.
     */
    val heroReadAt: Long = 0,
    /** Templates of the instances on screen, keyed by `equipmentId`; an instance carries only rolls. */
    val inventoryBases: Map<String, JsonObject> = emptyMap(),
    val selectedEquipment: String = "",
    /** What the admin's random grant asks the server for. Blank means "any". */
    val grantRarity: String = "", val grantSlot: String = "",
    /** The currency catalogue, read once per session; `selectedOrb` is the `items` id of one orb. */
    val orbs: List<CurrencyItem> = emptyList(), val selectedOrb: String = "",
    /** The world's reference tables, read once per session: classes and the shared skill tree. */
    val classes: List<CharacterClass> = emptyList(), val treeNodes: List<SkillTreeNode> = emptyList(),
    /** The level table, read with the classes: it says what the next level costs. */
    val levels: List<ExperienceLevel> = emptyList(),
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

    val checks: List<CheckResult> = emptyList(),
    val health: String = ui("runtime.not_checked"),
) {
    val isAdmin: Boolean get() = signedIn && profile?.role == "ADMIN"
    /**
     * The character every command on every tab acts on; the gate guarantees there is one.
     *
     * The loaded hero wins over the menu's snapshot, which was taken before any experience was
     * earned — otherwise the banner would keep showing the level the player came in on.
     */
    val character: CharacterSummary? get() = hero?.character?.takeIf { it.id == characterId }
        ?: characters.firstOrNull { it.id == characterId }
    /** The server refuses a fourth, so the button that would ask for one is not offered. */
    val characterSlotsLeft: Int get() = (MAX_CHARACTERS - characters.size).coerceAtLeast(0)
    /** An account nobody named: a device registration leaves `name` and `login` empty. */
    val accountTitle: String get() = profile?.name?.takeIf { it.isNotBlank() }
        ?: profile?.login?.takeIf { it.isNotBlank() }
        ?: ui("session.guest") + " · …${deviceId.takeLast(6)}"
    /** Lots the character may act on: the showcase hides their own unless asked not to. */
    val ownLots: List<AuctionLot> get() = myLots.filter { it.onSale }
    /** The class the shown hero belongs to; the server owns the base it hands out. */
    val heroClass: CharacterClass? get() = hero?.let { view -> classes.firstOrNull { it.id == view.character.classId } }
    val adminTools: Boolean get() = isAdmin && mode == AppMode.ADMIN
    /** The editor is an administrator's tool now that a character is made from the menu. */
    val canEdit: Boolean get() = signedIn && adminTools
    val ownsCharacter: Boolean get() = signedIn && profile?.id == characterOwner
}

/**
 * How many characters one account may hold — the server's `CONST_USER_MAX_CHARACTERS`.
 *
 * A copy of a server constant, which the client normally refuses to keep. It earns its place by
 * being display only: it greys the create button early instead of letting the player fill a form
 * the server will reject with `CH_005`. The refusal is still shown if the numbers ever disagree.
 */
const val MAX_CHARACTERS = 3

/**
 * The tabs, by name.
 *
 * Four of them are the bottom bar a player sees, and [TAB_ADMIN] is the one an administrator has
 * on top of it. Everything else is a screen a button opens: the forge from the Hero tab, and the
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

/** What the bottom bar offers a player — and, with [TAB_ADMIN] appended, an administrator. */
val PLAYER_TABS = listOf(TAB_HERO, TAB_TREE, TAB_AUCTION, TAB_ACCOUNT)

/** Screens only an administrator may open, whichever button leads to them. */
val ADMIN_TABS = setOf(TAB_CATALOG, TAB_EDITOR, TAB_CHECKS, TAB_ADMIN)
