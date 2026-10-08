package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.i18n.Phrase
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.atlas.AtlasState
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.trade.Cost
import com.sperance.exileforge.core.model.trade.Shortfall
import com.sperance.exileforge.core.model.tree.TreeState
import com.sperance.exileforge.core.network.Link
import com.sperance.exileforge.core.network.LinkRepository
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notice
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Срез «игра» (3.80.33): что читают экраны героя и его мест - команда в полёте, язык, мир, сессия, герой на экране,
 * связь, режим и настройки устройства. Имена - те же, что у общего состояния, чтобы экраны переезжали без пересказа.
 */
data class GameUi(
    val activity: Activity = Activity(),
    val lang: Lang = uiLanguage,
    val world: World = World(),
    val session: Session = Session(DEFAULT_SERVER),
    val holding: HeroHolding = HeroHolding(),
    val link: Link = Link(),
    val mode: AppMode = AppMode.PLAYER,
    val settings: GameSettings = GameSettings(),
    val logFilter: Set<LogKind> = LogKind.DEFAULT,
    /** Тост успеха на экране (3.80.38); отказ - у [activity]. */
    val notice: Notice? = null,
) {
    val busy: Boolean get() = activity.held
    val loading: Set<String> get() = activity.loading
    val reading: Boolean get() = activity.reading

    /** Отказ, который печатают там, где нажали; успех не показывают. */
    val refusal: Phrase? get() = activity.refusal
    fun refreshing(read: String): Boolean = busy || read in loading
    val isAdmin: Boolean get() = session.isAdmin
    val isTester: Boolean get() = session.isTester
    val adminTools: Boolean get() = BuildConfig.DEBUG && isAdmin && mode == AppMode.ADMIN
    val sessionEpoch: Int get() = session.sessionEpoch
    val index: ContentIndex? get() = world.content
    val heroId: String get() = holding.heroId
    val hero: HeroView? get() = holding.hero?.takeIf { it.id == holding.heroId }
    val heroInfo: HeroInfo? get() = hero?.info
    val heroRow: HeroSummary? get() = session.characters.firstOrNull { it.id == holding.heroId }
    val heroName: String get() = heroInfo?.name ?: heroRow?.name.orEmpty()
    val heroLevel: Int get() = heroInfo?.level ?: heroRow?.level ?: 1
    val heroClass: HeroClass? get() = index?.let { i -> hero?.let { i.heroClass(it.heroClass) } }

    /** The rules refuse one hero more than they allow, so the button that would ask for one is not offered. */
    val characterSlotsLeft: Int get() = ((index?.rules?.maxCharacters ?: MAX_CHARACTERS) - session.characters.size).coerceAtLeast(0)
    val ownsCharacter: Boolean get() = session.signedIn && session.profile?.id == holding.owner

    /** Что продажа вещей редкостей [rarities] даст осколками сейчас (3.95.3), словами: правило сервера над сумкой героя; null - ничего. */
    fun shardsFor(rarities: Collection<com.sperance.exileforge.rules.content.Rarity>): String? {
        val rules = index?.rules?.sell ?: return null
        return com.sperance.exileforge.core.display.shardsText(rules.yields(rarities, hero?.bag.orEmpty()), rules.shardsPerOrb)
    }

    /** How many of one stacking item the hero holds, or null while the hero has not been read. */
    fun bagAmount(code: String): Long? = hero?.let { it.bag[code] ?: 0L }

    /** Чего герою не хватает на [cost] (3.89.0); null, пока герой не прочитан. */
    fun shortfall(cost: Cost): Shortfall? = hero?.let { h -> cost.shortfall({ h.bag[it] ?: 0L }, h.money) }
    val orbs: List<Item> get() = HeroLens.orbs(index)
    val currencies: List<Item> get() = HeroLens.currencies(index)
    val bench: List<BenchRecipe> get() = HeroLens.bench(index, hero)
    val progress: CampaignProgress? get() = HeroLens.progress(index, hero)
    val atlasState: AtlasState? get() = HeroLens.atlasState(index, hero)
    val treeState: TreeState? get() = HeroLens.treeState(index, hero)
}

/** Один срез «игра» на процесс (3.80.33): модели экранов отдают его своим экранам. */
class GameSlice(
    sessions: SessionRepository,
    commands: CommandRunner,
    languages: LanguageRepository,
    worlds: WorldRepository,
    heroes: HeroRepository,
    links: LinkRepository,
    modes: AppModes,
    prefs: PreferencesRepository,
    notices: Notices,
    scope: CoroutineScope,
) {
    val ui: StateFlow<GameUi> = combine(
        combine(commands.state, languages.lang, worlds.state, sessions.state, heroes.state) { activity, lang, world, session, holding ->
            GameUi(activity, lang, world, session, holding)
        },
        combine(links.state, modes.mode, prefs.settings, prefs.logFilter, notices.state) { link, mode, settings, logFilter, notice ->
            GameUi(link = link, mode = mode, settings = settings, logFilter = logFilter, notice = notice)
        },
    ) { a, b -> a.copy(link = b.link, mode = b.mode, settings = b.settings, logFilter = b.logFilter, notice = b.notice) }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            GameUi(
                commands.state.value, languages.lang.value, worlds.state.value, sessions.state.value, heroes.state.value, links.state.value, modes.mode.value,
                prefs.settings.value, prefs.logFilter.value, notices.state.value,
            ),
        )
}
