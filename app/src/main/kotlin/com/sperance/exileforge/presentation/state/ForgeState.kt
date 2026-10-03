package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.network.Outage
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.feedback.OwnReport
import com.sperance.exileforge.core.model.feedback.Suggestion
import com.sperance.exileforge.core.network.TesterAccount
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.atlas.AtlasState
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.AuctionLot
import com.sperance.exileforge.core.model.auction.AuctionPage
import com.sperance.exileforge.core.model.auction.AuctionSlots
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.guild.GuildLogEntry
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.core.model.guild.GuildPage
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.trade.MerchantStock
import com.sperance.exileforge.core.model.tree.TreeState
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.network.QueuedCommand
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.GuildQuests
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.QuestBoard
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.WorkGains
import com.sperance.exileforge.rules.sheet.SheetCalculator

enum class AppMode { PLAYER, ADMIN }

/** What a toast says had happened: its kind picks the colour, [at] tells two equal texts apart. */
enum class NoticeKind { DONE, LOOT, CRAFT, ATLAS }
/** A line read in the player's language when it is shown, not when it was made (3.79.0): a language switch re-reads it. */
fun interface Phrase { fun read(): String }

/** A [Phrase] of the dictionary: [key] with [args], looked up anew each time it is read. */
fun phrase(key: String, vararg args: Any?): Phrase = Phrase { com.sperance.exileforge.core.i18n.ui(key, *args) }

data class Notice(val text: String, val kind: NoticeKind = NoticeKind.DONE, val at: Long = System.nanoTime())

/** Which of the three screens the app is on, above the tabs: the tabs only make sense once there is an account and a hero. */
enum class AppPhase { AUTH, CHARACTERS, GAME }

/**
 * The whole app, as one immutable value. What every screen needs sits at the top; everything else is
 * grouped by whose it is: [account] the session and the heroes it owns, [world] the content read once
 * per server, [play] the hero being played, [market] the auction, [guild] the hero's guild, [admin] the administrator's promo codes.
 */
data class ForgeState(
    val phase: AppPhase = AppPhase.AUTH,
    val lang: Lang = uiLanguage,
    val mode: AppMode = AppMode.PLAYER,
    val failure: FailureState? = null,
    /** [busy] is a command in flight — the one thing that disables controls; [loading] names the reads in flight. */
    val busy: Boolean = true, val loading: Set<String> = emptySet(), val message: Phrase? = null, val error: Boolean = false, val notice: Notice? = null,
    val tab: Int = TAB_HERO,
    /** The building of the City tab that is open (3.22.0); none is the square with the three of them. */
    val building: Building? = null,
    val account: AccountState = AccountState(),
    val world: WorldState = WorldState(),
    val play: PlayState = PlayState(),
    val market: MarketState = MarketState(),
    val guild: GuildState = GuildState(),
    val feedback: FeedbackState = FeedbackState(),
    val quests: QuestState = QuestState(),
    val admin: AdminState = AdminState(),
    val link: LinkState = LinkState(),
    /** How the stash is sorted (3.30.0); kept on the device, unlike its filters, and across heroes. */
    val stashSort: StashSort = StashSort.NEWEST,
    /** Whether the gear shelf hides what the hero wears (3.69.0); kept on the device like the order. */
    val stashHideWorn: Boolean = true,
    /** The player's settings (3.77.0). */
    val settings: GameSettings = GameSettings(),
    /** The shelves of the fight's log shown (3.37.0). */
    val logFilter: Set<com.sperance.exileforge.core.campaign.LogKind> = com.sperance.exileforge.core.campaign.LogKind.DEFAULT,
) {
    val isAdmin: Boolean get() = account.signedIn && account.profile?.role == "ADMIN"
    /** The testing window (3.73.0): a tester's, and an administrator's too. */
    val isTester: Boolean get() = isAdmin || (account.signedIn && account.profile?.role == "TESTER")
    val reading: Boolean get() = loading.isNotEmpty()
    fun refreshing(read: String): Boolean = busy || read in loading
    /** The content this server serves, once it has been read; every card and every roll is drawn from it. */
    val index: ContentIndex? get() = world.content
    /** The hero every command acts on; the gate guarantees there is one. */
    val hero: HeroView? get() = play.hero?.takeIf { it.id == play.heroId }
    /** Who the hero is: the loaded hero wins over the menu's row, taken before any experience was earned. */
    val heroInfo: HeroInfo? get() = hero?.info
    val heroRow: HeroSummary? get() = account.characters.firstOrNull { it.id == play.heroId }
    val heroName: String get() = heroInfo?.name ?: heroRow?.name.orEmpty()
    val heroLevel: Int get() = heroInfo?.level ?: heroRow?.level ?: 1
    /** The rules refuse one hero more than they allow, so the button that would ask for one is not offered. */
    val characterSlotsLeft: Int get() = ((index?.rules?.maxCharacters ?: MAX_CHARACTERS) - account.characters.size).coerceAtLeast(0)
    /** An account nobody named: a device registration leaves `name` and `login` empty. */
    val accountTitle: String get() = account.profile?.name?.takeIf { it.isNotBlank() }
        ?: account.profile?.login?.takeIf { it.isNotBlank() }
        ?: ui("session.guest") + " · …${account.profile?.id.orEmpty().takeLast(6)}"
    val ownLots: List<AuctionLot> get() = market.myLots.filter { it.onSale }
    /** The class the shown hero belongs to, with the shared base folded in. */
    val heroClass: HeroClass? get() = index?.let { i -> hero?.let { i.heroClass(it.heroClass) } }
    /** Инструменты администратора - только в отладочной сборке (3.44.0). */
    val adminTools: Boolean get() = BuildConfig.DEBUG && isAdmin && mode == AppMode.ADMIN
    val ownsCharacter: Boolean get() = account.signedIn && account.profile?.id == play.heroOwner
    /** The refusal to print where the action was taken; a success is never shown. */
    val refusal: Phrase? get() = message.takeIf { error }
    /** How many of one stacking item the hero holds, or null while the hero has not been read. */
    fun bagAmount(code: String): Long? = hero?.let { it.bag[code] ?: 0L }
    /** The orbs of the world, in the order of their price: what the forge and the auction offer. */
    val orbs: List<Item> get() = index?.itemsByCategory?.get(Item.CURRENCY).orEmpty().sortedBy { it.price }
    /** The auction's money (server 1.65.0): the base orbs a lot is priced, bought and filtered in, cheapest first. */
    val currencies: List<Item> get() = index?.let { i -> orbs.filter { i.rules.auction.trades(it.code) } }.orEmpty()
    /** The bench lines the hero has found; the rest of the bench stays hidden. */
    val bench: List<BenchRecipe> get() = index?.let { i -> hero?.let { h -> i.bench.filter { it.code in h.info.recipes } } }.orEmpty()
    /** Which zones the hero has passed and which are open: derived from the hero, no request. */
    val progress: CampaignProgress? get() = index?.let { i -> hero?.let { h -> CampaignProgress(h.campaign.cleared.filter { it in i.zones }, i.world.unlocked(h.campaign.cleared)) } }
    /** The hero's atlas as the rules count it: points earned and free. */
    val atlasState: AtlasState? get() = index?.let { i -> hero?.let { h ->
        AtlasState(listOf(i.atlasGraph.start) + h.info.atlas, h.info.earned, AtlasPoints.total(i.atlas.points, h.info.earned, i.atlas.cap),
            AtlasPoints.available(i.atlas.points, h.info.earned, h.info.atlas, i.atlas.cap))
    } }
    /** The hero's tree as the rules count it: the point balance and what the taken nodes give. */
    val treeState: TreeState? get() = index?.let { i -> hero?.let { h ->
        val total = i.classes.pointsTotal(h.level) + h.info.bonusPoints
        val spent = i.tree.spent(h.tree)
        val calculator = SheetCalculator(i)
        TreeState(total, spent, total - spent, h.tree, calculator.contributions(calculator.expand(i.tree.lines(h.tree))))
    } }
}

/**
 * The link to the server (3.30.0): [offline] while it cannot be reached — an icon, not the red strip — and the
 * commands given while it could not, waiting to be sent in order. Nothing is drawn as done before the server says so.
 */
data class LinkState(
    val offline: Boolean = false,
    val waiting: List<QueuedCommand> = emptyList(),
    /** Why the server is not answering (3.79.0), and the transport's own words for an administrator. */
    val outage: Outage? = null,
    val detail: String? = null,
) {
    /** The items a waiting command is about: their cards say «ждёт отправки». */
    val waitingItems: Set<String> get() = waiting.mapNotNullTo(HashSet()) { it.itemId }
}

/** The session: who is signed in, to which server, and which heroes they own. */
data class AccountState(
    val profile: UserProfile? = null, val signedIn: Boolean = false, val sessionEpoch: Int = 0,
    /** A kept session the server could not be reached to confirm: the sign-in screen offers to try again. */
    val resumable: Boolean = false,
    val server: String = DEFAULT_SERVER, val serverDraft: String = DEFAULT_SERVER,
    /** The account's heroes and whether they have been read yet: "none" and "not asked yet" must differ. */
    val characters: List<HeroSummary> = emptyList(), val charactersRead: Boolean = false,
    val health: String = ui("runtime.not_checked"),
    /** The administrator's testers (3.73.0), and the account whose password was just shown — once, to be copied. */
    val testers: List<TesterAccount> = emptyList(), val shownTester: TesterAccount? = null,
)

/** Players' voices and the account's mail (3.73.0): suggestions, one's own reports, the administrator's reading, the inbox. */
data class FeedbackState(
    val suggestions: List<Suggestion> = emptyList(),
    val mine: List<OwnReport> = emptyList(),
    val reports: List<AdminReport> = emptyList(),
    val mail: List<Mail> = emptyList(),
) {
    val unread: Int get() = mail.count { !it.read }
}

/** The world as this server serves it: the content, read once per server and kept on the device by chunk, and the static files. */
data class WorldState(
    /** The content — every table of the game — parsed by the rules; null until the chunks have been read. */
    val content: ContentIndex? = null,
    /** The fingerprint of the content on screen, so a changed world is noticed and an unchanged one is never fetched. */
    val contentHash: String = "",
    /** The server's dictionary for the current language, and how many strings it holds; the bundle itself is global. */
    val localeLanguage: String = "", val localeStrings: Int = 0,
    /** Which languages the player may choose from: the server's manifest decides, the client's tables narrow. */
    val languages: List<Lang> = listOf(Lang.RU, Lang.EN),
    val iconKeys: Int = 0, val iconSprites: Int = 0,
    val portraits: Int = 0,
)

/** The hero being played, and what the player has picked on its screens. */
data class PlayState(
    val heroId: String = "", val heroOwner: String = "", val hero: HeroView? = null,
    /** The loot chest just opened (3.76.0): its spoils on screen until dismissed. */
    val chestOpening: com.sperance.exileforge.core.model.hero.ChestOpening? = null,
    /** The warm-up of the hero entered (3.54.0): the loading screen stands until it is finished. */
    val warmup: com.sperance.exileforge.presentation.features.Warmup? = null,
    /** When the hero was last read whole, as epoch millis; 0 means "never, or known to be stale". */
    val heroReadAt: Long = 0,
    val heroSeenAt: Long = 0,
    val selectedEquipment: String = "",
    /** The codes of the orb and the essence picked in the forge. */
    val selectedOrb: String = "", val selectedEssence: String = "",
    /** The omen spent with the next orb (3.36.0); blank for none. */
    val selectedOmen: String = "",
    val forgeSection: ForgeSection = ForgeSection.ORBS, val forgeLine: String = "",
    /** What the admin's random grant asks for: a rarity and a slot, blank meaning "any". */
    val grantRarity: String = "", val grantSlot: String = "",
    /** The class a new hero is being created with, and the tree node under the cursor. */
    val draftClass: String = "", val selectedNode: String = "",
    val nodeQuery: String = "",
    /** The zone whose card is open on the world map, and the map picked for it. */
    val launch: MapLaunchState? = null,
    /** The crafts as the server last answered, with the device's clock at that moment; the profession whose window is open. */
    val crafts: CraftsState? = null,
    val craftsAt: Long = 0,
    val craftsProfession: String = "",
    val craftsTotals: WorkGains = WorkGains(),
    val craftsLast: WorkGains? = null,
    val craftsPending: WorkGains = WorkGains(),
    /** The gear this run brought, with when each piece landed, for the gear sheet's «Новый лут». */
    val runLoot: List<LootEntry> = emptyList(),
    /** The atlas window, null while it is closed. */
    val atlas: AtlasScreenState? = null,
    /** The run's journal: events the server has not taken yet, and the ones it refused, for the badge. */
    val runPending: Int = 0, val runRejected: Int = 0,
)

/** The stash's orders (3.30.0): the newest first as the server keeps it, or by rarity, item level or the merchant's price. */
enum class StashSort { NEWEST, RARITY, LEVEL, PRICE;
    companion object { fun of(name: String?): StashSort = entries.firstOrNull { it.name == name } ?: NEWEST }
}

/** The atlas window: the node looked at. */
data class AtlasScreenState(val selected: String = "")

/** A piece a run brought and the moment it landed. */
data class LootEntry(val item: ItemInstance, val at: Long)

/** One zone's card: the stash map picked to enter it with — null enters without one. */
data class MapLaunchState(val mapCode: String, val picked: String? = null)

/** The forge's sections: orbs, the bench and the essences work on one item. */
enum class ForgeSection { ORBS, BENCH, ESSENCES }

/** The auction, as this hero sees it. */
data class MarketState(
    val tab: Int = 0,
    val showcase: AuctionPage = AuctionPage(), val filter: AuctionFilter = AuctionFilter(),
    val showOwnLots: Boolean = false,
    val myLots: List<AuctionLot> = emptyList(),
    /** The hero's deals of the last days (3.73.0), newest first. */
    val history: List<AuctionLot> = emptyList(),
    /** Why the auction is closed to this hero, in the server's own words. */
    val locked: String? = null,
    val merchant: MerchantStock? = null,
    val slots: AuctionSlots? = null,
)

/** The City's buildings (3.22.0): each one a screen of its own behind the square. */
enum class Building { QUESTS, MERCHANT, AUCTION, GUILD }

/** The guild's sections, each a tile of its hub; [APPLICATIONS] only for those who may answer them. */
enum class GuildTab { MEMBERS, QUESTS, APPLICATIONS, CONTRIBUTE, LOG, SETTINGS }

/**
 * The hero's guild as the server last answered (3.22.0): [mine] is null until it has been read; `guilds.json` comes with
 * the content ([ContentIndex.guilds]). The journal is read page by page.
 */
data class GuildState(
    val mine: GuildMine? = null,
    val query: String = "", val search: GuildPage = GuildPage(),
    /** The faction the list is narrowed to (3.28.0), blank for all. */
    val faction: String = "",
    /** The section open over the guild's hub; null — the hub itself. */
    val tab: GuildTab? = null,
    val log: List<GuildLogEntry> = emptyList(), val logPage: Int = 0, val logEnd: Boolean = false,
)

/** The quest board's sections (3.23.0). */
enum class QuestTab { DAILY, WEEKLY, CONTRACTS, STORY }

/**
 * The hero's quests as the server last answered (3.23.0): [board] — the dailies, weeklies, contracts and the story step of the
 * City's board; [guild] — the guild's personal and common quests. Both are null until read.
 */
data class QuestState(val board: QuestBoard? = null, val guild: GuildQuests? = null, val tab: QuestTab = QuestTab.DAILY)

/** The administrator's tools: promo codes. */
data class AdminState(val redemptions: List<RedemptionCode> = emptyList())

/** How many heroes one account may hold when the rules have not been read yet. */
const val MAX_CHARACTERS = 3

/** What a read reads. One read per name runs at a time, and a command names the reads it will redo itself. */
object Reads {
    const val FEEDBACK = "feedback"
    const val MAIL = "mail"
    const val HERO = "hero"
    const val CHARACTERS = "characters"
    const val AUCTION = "auction"
    const val LOTS = "lots"
    const val CONTENT = "content"
    const val REDEMPTIONS = "redemptions"
    const val HEALTH = "health"
    const val MERCHANT = "merchant"
    const val CRAFTS = "crafts"
    const val GUILD = "guild"
    const val GUILD_SEARCH = "guild_search"
    const val GUILD_LOG = "guild_log"
    const val QUESTS = "quests"
    const val GUILD_QUESTS = "guild_quests"
}

/** The tabs, by name: the bottom bar a player sees, and the screens a button opens. */
const val TAB_ACCOUNT = 3
const val TAB_HERO = 4
const val TAB_TREE = 5
/** The City (3.22.0): the merchant, the auction and the guild, where the auction's tab was. */
const val TAB_CITY = 6
const val TAB_CRAFT = 7
const val TAB_ADMIN = 8
const val TAB_REDEMPTION = 9
const val TAB_EXPEDITION = 10
const val TAB_CRAFTS = 11
const val TAB_SKILLS = 12
/** «Развитие»: the hub of the forge ([TAB_CRAFT]), the menagerie, the atlas, the trials and the chronicle. */
const val TAB_PROGRESS = 13
const val TAB_PETS = 14
const val TAB_TRIALS = 15
/** The chronicle (3.69.0): a page of «Развитие», where the Hero tab's card was. */
const val TAB_CHRONICLE = 16
/** «Настройки» (3.77.0): from the banner's menu, «back» leading to the tab it was opened over. */
const val TAB_SETTINGS = 17

val PLAYER_TABS = listOf(TAB_HERO, TAB_EXPEDITION, TAB_CRAFTS, TAB_PROGRESS, TAB_CITY)

/** Screens only an administrator may open, whichever button leads to them. */
val ADMIN_TABS = setOf(TAB_ADMIN, TAB_REDEMPTION)
