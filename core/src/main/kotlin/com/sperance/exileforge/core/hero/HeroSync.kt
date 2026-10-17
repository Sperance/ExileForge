package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.app.DeviceStore
import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.display.atlasPointToast
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.core.world.WorldRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

/**
 * Синхронизация героя: части, что держит клиент, снимок сервера поверх них, лист по правилам и готовый
 * [HeroView] в `HeroRepository`. Копия героя на устройстве - для быстрого старта. Один на приложение.
 */
class HeroSync(
    private val heroes: HeroRepository,
    private val sessions: SessionRepository,
    private val world: WorldRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val content: ContentLoader,
    private val notices: Notices,
    private val store: DeviceStore,
    private val feedback: FeedbackRepository,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api

    /** Очки Атласа, о которых уже сказал ответ события: чтение героя о них молчит. */
    private val announcedPoints = HashSet<String>()

    /**
     * Новые очки Атласа из награды события похода или испытания (ключи [com.sperance.exileforge.rules.content.AtlasPoints.key]):
     * тост на каждое - за что оно, - раз; следующее чтение героя их не повторяет.
     */
    fun pointsEarned(keys: Collection<String>) {
        keys.filter { announcedPoints.add(it) }.forEach { notices.toast(atlasPointToast(it), NoticeKind.ATLAS) }
    }

    private var parts: HeroParts? = null
    private var copyJob: Job? = null

    /** Последний сложенный лист и его отпечаток частей. */
    @Volatile private var sheets: DrawnSheet? = null

    /** Лист героя [sheet] и лист боя [combat], сложенные из частей с отпечатком [key] на контенте [index]. */
    private class DrawnSheet(val key: String, val index: com.sperance.exileforge.rules.content.ContentIndex, val sheet: com.sperance.exileforge.core.character.HeroSheet, val combat: CombatSheet?)

    /** Номер последнего слияния, чей рисунок идёт; более старый, пришедший позже, отбрасывается. */
    private var drawing = 0L

    /** Рисунок последнего слияния: команда ждёт его, прежде чем поход возьмёт снаряжение героя. */
    @Volatile private var draw: Job? = null

    fun forget() {
        parts = null
        sheets = null
        announcedPoints.clear()
        copyJob?.cancel()
    }

    /** Герой, каким его сохранило устройство: рисуется сразу быстрым стартом, перечитывается, когда сессия подтверждена. */
    fun restore(heroId: String, snapshot: HeroSnapshot) {
        parts = null
        apply(heroId, snapshot, keep = false)
    }

    /**
     * Части героя [heroId] из его копии на устройстве: вход в героя из меню шлёт серверу их отпечатки и `If-None-Match`,
     * и сервер отвечает только сдвинутым или 304, а не всем героем. Копия чужого аккаунта или другой ревизии API не годится.
     */
    suspend fun seed(heroId: String) {
        val server = sessions.state.value.server
        val account = sessions.state.value.profile?.id ?: return
        val text = store.heroCopy(server, heroId) ?: return
        val copy = withContext(Dispatchers.Default) { runCatching { WireJson.decodeFromString(HeroCopy.serializer(), text) }.getOrNull() } ?: return
        if (copy.revision != API_REVISION || copy.account.id != account) return
        parts = HeroParts(heroId).merge(copy.snapshot, fresh = false).takeIf { it.complete }
    }

    /** Что команда на [heroId] говорит серверу о частях у клиента; `null` ничего не просит. */
    fun heldParts(heroId: String): String? = if (!heroes.onScreen(heroId)) null else parts?.takeIf { it.heroId == heroId }?.header() ?: HeroParts(heroId).header()

    /** Ответ команды: её снимок, или ничего - тогда чтение холодное и перечитывается. */
    fun delivered(heroId: String, snapshot: HeroSnapshot?) {
        if (!heroes.onScreen(heroId)) return
        if (snapshot == null) heroes.stale() else apply(heroId, snapshot)
    }

    fun load() = commands.read(Reads.HERO) { readHero() }

    /** Герой, если ответ команды не принёс снимка: тихое чтение под общим ключом, без повтора. */
    fun refreshIfStale() {
        if (heroes.heroId.isBlank() || heroes.state.value.readAt != 0L) return
        commands.read(Reads.HERO, silent = true) { readHero() }
    }

    /** Герой, если то, что на экране, остыло: один запрос, обычно 304. */
    fun ensure() {
        if (heroes.heroId.isBlank()) return
        if (heroes.state.value.fresh(System.currentTimeMillis(), FRESH_FOR)) return
        commands.read(Reads.HERO) { readHero() }
    }

    /** Герой одним запросом: только то, что сдвинулось против частей здесь, или 304, когда ничего. Контент прежде. */
    suspend fun readHero() {
        val id = heroes.heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        content.ensure()
        val held = parts?.takeIf { it.heroId == id } ?: HeroParts(id)
        val snapshot = api.hero.view(id, held, feedback.state.value.unread)
        when {
            snapshot != null -> apply(id, snapshot)
            heroes.state.value.hero == null -> apply(id, HeroSnapshot(held.version, unread = feedback.state.value.unread))
            else -> heroes.seen(System.currentTimeMillis())
        }
    }

    /**
     * Сливает снимок с частями и рисует героя из них; лист складывают правила здесь.
     *
     * Счёт почты едет в снимке, копия с устройства его не несёт. Части декодируются и лист складывается вне главного потока; снимки ложатся
     * по порядку, обогнанный новым отбрасывается. Лист складывается заново, только если сдвинулись герой, дерево или вещи; лист боя сводки
     * - здесь же. Новое очко атласа объявляется вслух, кроме названных ответом события.
     */
    private fun apply(heroId: String, snapshot: HeroSnapshot, keep: Boolean = true) {
        if (keep) feedback.update { it.copy(unread = snapshot.unread) }
        val earnedBefore = parts?.takeIf { it.heroId == heroId && it.complete }?.hero?.earned
        val merged = (parts?.takeIf { it.heroId == heroId } ?: HeroParts(heroId)).merge(snapshot, fresh = keep)
        if (!merged.complete) {
            parts = null
            heroes.stale()
            return
        }
        val index = world.state.value.content ?: run {
            heroes.stale()
            return
        }
        parts = merged
        if (keep) keepCopy(heroId, merged)
        val ticket = ++drawing
        draw = scope.launch {
            val view = withContext(Dispatchers.Default) {
                val info = merged.hero
                val key = merged.sheetKey()
                val (sheet, combat) = sheets?.takeIf { it.key == key && it.index === index }?.let { it.sheet to it.combat } ?: run {
                    val sheet = Sheets.calculate(index, info.level, info.heroClass, merged.tree, merged.items, info.laws, info.fate)
                    val combat = CombatSheet.of(index, sheet, info.level, info.heroClass, info.skills, info.fate, merged.items.filter { it.equipped })?.also { it.figures }
                    sheets = DrawnSheet(key, index, sheet, combat)
                    sheet to combat
                }
                HeroView(info, merged.items, merged.overflow, merged.bag, merged.tree, merged.campaign, merged.crafts, merged.merchant, sheet, combat = combat)
            }
            if (ticket != drawing || parts !== merged) return@launch
            heroes.set(view, System.currentTimeMillis())
            earnedBefore?.let { before -> (view.info.earned - before.toSet() - announcedPoints).size.takeIf { it > 0 }?.let { notices.toast(ui("atlas.point_earned", it), NoticeKind.ATLAS) } }
        }
    }

    /**
     * Ждёт, пока последний снимок нарисован в состояние; рисунок, обогнанный новым слиянием, ждёт того. Не дольше
     * [DRAWN_WAIT_MS]: рисунок - удобство экрана, зависший расчёт не держит команду, что его ждёт.
     */
    suspend fun drawn() {
        withTimeoutOrNull(DRAWN_WAIT_MS) {
            while (true) {
                val job = draw ?: break
                job.join()
                if (draw === job) break
            }
        }
    }

    /** Герой на устройстве к следующему запуску, через миг после последнего снимка: очередь команд - одна запись. */
    private fun keepCopy(heroId: String, held: HeroParts) {
        val profile = sessions.state.value.profile ?: return
        val server = sessions.state.value.server
        copyJob?.cancel()
        copyJob = scope.launch {
            delay(COPY_AFTER)
            val text = withContext(Dispatchers.Default) { WireJson.encodeToString(HeroCopy.serializer(), HeroCopy(API_REVISION, profile, held.snapshot())) }
            store.saveHeroCopy(server, heroId, text)
        }
    }

    private companion object {
        /** Сколько чтению героя верят, не спрашивая снова. */
        const val FRESH_FOR = 30_000L

        /** Сколько копия героя ждёт следующего снимка, прежде чем записаться. */
        const val COPY_AFTER = 1_500L

        /** Дольше этого команда не ждёт рисунка героя, мс. */
        const val DRAWN_WAIT_MS = 10_000L
    }
}

/** Герой на устройстве: кто им играл, под какой ревизией API, и каждая часть, как прочитана последней. */
@Serializable
data class HeroCopy(val revision: Int, val account: UserProfile, val snapshot: HeroSnapshot)
