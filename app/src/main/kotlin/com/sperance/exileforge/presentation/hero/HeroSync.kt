package com.sperance.exileforge.presentation.hero

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.hero.HeroRepository
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
import com.sperance.exileforge.data.settings.ServerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

/**
 * Синхронизация героя (3.80.15): части, что держит клиент, снимок сервера поверх них, лист по правилам и готовый
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
    private val store: ServerStore,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api

    private var parts: HeroParts? = null
    private var copyJob: Job? = null

    /** Номер последнего слияния, чей рисунок идёт; более старый, пришедший позже, отбрасывается. */
    private var drawing = 0L

    /** Рисунок последнего слияния: команда ждёт его, прежде чем поход возьмёт снаряжение героя. */
    @Volatile private var draw: Job? = null

    fun forget() {
        parts = null
        copyJob?.cancel()
    }

    /** Герой, каким его сохранило устройство (3.30.0): рисуется сразу быстрым стартом, перечитывается, когда сессия подтверждена. */
    fun restore(heroId: String, snapshot: HeroSnapshot) {
        parts = null
        apply(heroId, snapshot, keep = false)
    }

    /** Что команда на [heroId] говорит серверу о частях у клиента; `null` ничего не просит. */
    fun heldParts(heroId: String): String? = if (!heroes.onScreen(heroId)) null else parts?.takeIf { it.heroId == heroId }?.header() ?: HeroParts(heroId).header()

    /** Ответ команды: её снимок, или ничего - тогда чтение холодное и перечитывается. */
    fun delivered(heroId: String, snapshot: HeroSnapshot?) {
        if (!heroes.onScreen(heroId)) return
        if (snapshot == null) heroes.stale() else apply(heroId, snapshot)
    }

    fun load() = commands.read(Reads.HERO) { readHero() }

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
        val snapshot = api.hero.view(id, held)
        when {
            snapshot != null -> apply(id, snapshot)
            heroes.state.value.hero == null -> apply(id, HeroSnapshot(held.version))
            else -> heroes.seen(System.currentTimeMillis())
        }
    }

    /** Сливает снимок с частями и рисует героя из них; лист складывают правила здесь. */
    private fun apply(heroId: String, snapshot: HeroSnapshot, keep: Boolean = true) {
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
        // Части декодируются и лист складывается вне главного потока (3.55.0): герой с тысячей предметов замораживал
        // кадр после каждой команды. Снимки ложатся по порядку: обогнанный более новым слиянием отбрасывается.
        val ticket = ++drawing
        draw = scope.launch {
            val view = withContext(Dispatchers.Default) {
                val info = merged.hero
                val sheet = Sheets.calculate(index, info.level, info.heroClass, merged.tree, merged.items, merged.pets.active)
                HeroView(info, merged.items, merged.overflow, merged.bag, merged.tree, merged.campaign, merged.crafts, merged.merchant, sheet, merged.pets)
            }
            if (ticket != drawing || parts !== merged) return@launch
            heroes.set(view, System.currentTimeMillis())
            // Новое очко атласа (3.47.0) объявляется вслух: древо легко забыть за кнопкой карты мира.
            earnedBefore?.let { before -> (view.info.earned.size - before.size).takeIf { it > 0 }?.let { notices.toast(ui("atlas.point_earned", it), NoticeKind.ATLAS) } }
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

/** Герой на устройстве (3.30.0): кто им играл, под какой ревизией API, и каждая часть, как прочитана последней. */
@Serializable
data class HeroCopy(val revision: Int, val account: UserProfile, val snapshot: HeroSnapshot)
