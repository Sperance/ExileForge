package com.sperance.exileforge.presentation.expedition

import com.sperance.exileforge.core.campaign.AtlasWindow
import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.core.campaign.Expedition
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.LootEntry
import com.sperance.exileforge.core.campaign.MapLaunch
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.UnfinishedRun
import com.sperance.exileforge.core.campaign.combat.FateRun
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.receivedText
import com.sperance.exileforge.core.display.shardsPerOrb
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.RunReport
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.data.settings.PreferencesRepository
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.quests.QuestActions
import com.sperance.exileforge.presentation.state.speedPresses
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunEventKind
import com.sperance.exileforge.rules.run.RunStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Кампания: карта мира и поход по карте, которым идёт игрок (из `ExpeditionViewModel`).
 *
 * Поход - мир, который сцена шагает каждый кадр; он держится здесь, а оверлей читает его [ExpeditionRun.hud].
 * Каждое убийство, сундук и спуск - событие журнала похода; журнал лежит на диске и уходит на сервер пачками - на
 * контрольных точках, на выходе, при падении, в фоне, - и ответ сервера есть истина. Награды только серверные:
 * каждый ответ называет, что принесло каждое событие, и поход показывает это по мере прихода; офлайн - ждёт связи.
 * Пачка хранит ключ идемпотентности, пока не отвечена: потерянный ответ спрашивается снова. Неотправленный журнал
 * уходит на следующем запуске. Один на приложение.
 */
class ExpeditionActions(
    private val repository: ExpeditionRepository,
    private val heroes: HeroRepository,
    private val heroSync: HeroSync,
    private val world: WorldRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val quests: QuestActions,
    private val prefs: PreferencesRepository,
    private val store: ServerStore,
    private val navigator: Navigator,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api

    /** Боссы сервера героя: спрошены раз за запуск, следующий вызов берёт их отсюда. */
    private var bossRecords: com.sperance.exileforge.core.network.BossRecords? = null

    /** Сколько героев сервера дрались с боссом [code] и сколько победили; null - сервер не ответил. */
    suspend fun bossRecord(code: String): com.sperance.exileforge.core.network.BossRecord? {
        val known = bossRecords ?: try {
            api.campaign.bosses(heroes.heroId).also { bossRecords = it }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return known?.records?.get(code) ?: known?.let { com.sperance.exileforge.core.network.BossRecord() }
    }
    private val index: ContentIndex? get() = world.state.value.content
    private val hero: HeroView? get() = heroes.state.value.hero

    private val mutableRun = MutableStateFlow<ExpeditionRun?>(null)
    val run: StateFlow<ExpeditionRun?> = mutableRun.asStateFlow()

    /** Поход по карте, из которого вошли в зону Ваал: стоит, пока зона играется. */
    private var parent: ExpeditionRun? = null

    /** Добыча основной карты под открытой зоной Ваал: павший в зоне заканчивает весь заход и продаёт и её. */
    fun outerLoot(): List<String> = parent?.hud?.value?.tally?.loot?.equipment?.map { it.id }.orEmpty()

    /** Журнал похода, что идёт. */
    private var runJournal: RunJournal? = null

    /** Заход, который сервер уже закрыл по ответу журнала (выход или гибель, `RunReport.open` = false): бросать его нечего. */
    private var shut: String? = null
    private val flushes = Channel<Unit>(Channel.CONFLATED)
    private var saveJob: Job? = null

    /** Следующая отправка сама по себе: повтор после неудачной. */
    private var sendJob: Job? = null

    /**
     * Пачка боя: отправка через `run.journalSeconds` правил после первого неотправленного события; поздние присоединяются,
     * ничто её не откладывает.
     */
    private var batchJob: Job? = null

    /** Отправок подряд не дошло: каждая ждёт вдвое дольше прошлой. */
    private var failures = 0

    /** Жетоны зоны Ваал, которые сервер уже сосчитал, когда в поход вошли снова. */
    private var vaalKilled: List<Int> = emptyList()

    /**
     * Запуск после сборки графа: отправки журнала по часам и по сигналу, новое снаряжение и новый герой - походу.
     *
     * Отправка по часам: что тихий отрезок карты оставил в ожидании, уходит само; без связи повторы ведёт пауза [retryLater]. Новое чтение
     * героя: поход читает сумку через состояние и берёт решённое сервером (зона Ваал, кристалл).
     */
    fun start() {
        scope.launch { for (signal in flushes) flush() }
        scope.launch {
            while (true) {
                delay(FLUSH_EVERY)
                if (failures == 0 && runJournal?.pending?.isNotEmpty() == true) flushes.trySend(Unit)
            }
        }
        scope.launch { heroes.state.map { it.hero }.distinctUntilChanged { a, b -> a === b }.filterNotNull().collect { heroChanged(it) } }
    }

    private fun expedition(transform: (Expedition) -> Expedition) = repository.update(transform)

    /** Сколько нажатий кнопки скорости боя дают скорость из настроек: 1 → 2 → 4. */
    private val speedSteps: Int get() = speedPresses(index, prefs.settings.value.fightSpeed)

    /** Прогресс кампании, как его считают правила по герою и миру. */
    private fun progress(): CampaignProgress? = index?.let { i -> hero?.let { h -> CampaignProgress(h.campaign.cleared.filter { it in i.zones }, i.world.unlocked(h.campaign.cleared)) } }

    /** Карта мира своего не читает: прогресс и атлас - героя. */
    fun loadCampaign() = heroSync.ensure()

    fun selectZone(mapCode: String) = expedition { it.copy(launch = MapLaunch(MapCode(mapCode))) }
    fun closeZone() = expedition { it.copy(launch = null) }

    /** Карта из сундука, с которой войти, или null - войти без неё. */
    fun pickMap(itemId: String?) = expedition { e -> e.copy(launch = e.launch?.let { l -> l.copy(picked = itemId, scarabs = if (itemId == null) emptyList() else l.scarabs) }) }

    /** Механика, что тестировщик ставит на заход насильно: переключить одну. */
    fun toggleForced(mechanic: MapMechanic) = expedition { it.copy(forced = if (mechanic in it.forced) it.forced - mechanic else it.forced + mechanic) }

    /** Все механики разом - или ни одной. */
    fun forceAll(on: Boolean) = expedition { it.copy(forced = if (on) MapMechanic.entries.toSet() else emptySet()) }

    /** Зелье на поход: одно, повторным касанием снимается. */
    fun pickPotion(code: String?) = expedition { e -> e.copy(launch = e.launch?.let { it.copy(potion = code.takeIf { c -> c != it.potion }) }) }

    /** Скарабей к карте или снят: не больше, чем велят правила, и не больше одного вида, чем в сумке. */
    fun toggleScarab(code: String, add: Boolean) = expedition { e ->
        val launch = e.launch ?: return@expedition e
        val max = index?.rules?.brews?.scarabsPerMap ?: 0
        val held = hero?.bag?.get(code) ?: 0L
        val scarabs = when {
            !add -> launch.scarabs - code
            launch.picked == null || launch.scarabs.size >= max || launch.scarabs.count { it == code } >= held -> return@expedition e
            else -> launch.scarabs + code
        }
        e.copy(launch = launch.copy(scarabs = scarabs))
    }

    /**
     * «В путь»: зона входится на сервере - с выбранной картой, потраченной там, или без неё, - и семя с замороженным
     * контекстом приходят с героем; поход строится здесь и идёт.
     */
    fun start(mapCode: MapCode, toBoss: Boolean = false) = enter(mapCode, repository.state.value.launch?.takeIf { it.mapCode == mapCode }, toBoss)

    /**
     * «Продолжить» незаконченный заход: вход в ту же зону без карты - сервер отдаёт тот же заход, его павших и открытые
     * сундуки; герой у входа, туман заново.
     */
    fun continueUnfinished() {
        val offer = repository.state.value.unfinished ?: return
        if (commands.state.value.busy) return
        expedition { it.copy(unfinished = null) }
        enter(offer.zone, launch = null)
    }

    /**
     * «Покинуть» незаконченный заход: журнал, что ещё не дошёл, отправляется - его добыча героя, - и заход закрывается
     * на сервере, как выходом.
     */
    fun abandonUnfinished() {
        val offer = repository.state.value.unfinished ?: return
        expedition { it.copy(unfinished = null) }
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroes.heroId
            finish(id, offer.runId)
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
        }
    }

    /**
     * Журнал, которого сервер ещё не взял, не бросается ради нового похода. Новое семя не раньше правил - ожидание, не ошибка.
     */
    private fun enter(mapCode: MapCode, launch: MapLaunch?, toBoss: Boolean = false) {
        if (mutableRun.value != null || commands.state.value.busy) return
        val i = index ?: return
        val progress = progress()
        if (i.zone(mapCode) == null || progress?.unlocked?.contains(mapCode) != true) return
        val picked = launch?.picked
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroes.heroId
            runJournal?.let { j ->
                flush()
                check(j.settled || runJournal == null) { ui("expedition.unsent") }
                store.clearJournal(id)
                runJournal = null
            }
            val started = try {
                api.campaign.start(
                    id,
                    mapCode,
                    picked,
                    launch?.potion,
                    launch?.scarabs.orEmpty().takeIf { picked != null }.orEmpty(),
                    forced = repository.state.value.forced,
                )
            } catch (e: ApiFailure) {
                if (e.code != SEED_TOO_SOON) throw e
                notices.toast(ui("expedition.seed_wait", e.args.firstOrNull().orEmpty()))
                return@task
            }
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
            heroSync.drawn()
            begin(id, started, toBoss, launch?.potion)
        }
    }

    /**
     * Войдя снова, поход продолжается со своими числами и павшими. Лист «Задания» считает от доски на входе;
     * счёт Предначертания - павшие карты и её Ваал-зоны, как у сервера. Тестировщик идёт сразу в бой со стражем.
     */
    private fun begin(id: String, started: RunStart, toBoss: Boolean = false, potion: String? = null) {
        val i = index ?: return
        val h = hero?.takeIf { it.id == id } ?: return
        val zone = i.zone(started.zone) ?: return
        val gear = gear() ?: return
        val run = Run(i, zone, started.seed, started.context)
        val journal = RunJournal(started.id, id, zone.code.value, applied = started.applied, base = started.applied).also { runJournal = it }
        val board = quests.board().takeIf { it.board != null }
        expedition {
            it.copy(
                runLoot = emptyList(), launch = null, pending = 0, rejected = 0, unfinished = null, saleMarks = emptySet(),
                quests = board, turnedIn = emptyList(), atlasPoints = emptyList(), potion = potion,
            )
        }
        mutableRun.value = ExpeditionRun.start(
            i, zone, run, journal, gear, h.campaign, System.currentTimeMillis(), h.info.experience, h.level,
            vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded,
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, killed = started.killed, opened = started.chests, features = started.features,
            summoned = started.tally.summons,
            fateRun = FateRun(started.killed, started.vaalKilled),
        ).also { r ->
            repeat(speedSteps) { r.send(RunCommand.Speed) }
            if (toBoss) r.send(RunCommand.ToBoss)
        }
        vaalKilled = started.vaalKilled
        persist()
    }

    /** Сферы Ваал под рукой за вычетом тех, что журнал потратил, а сервер ещё не сосчитал. */
    private fun vaalOrbsFree(): Long = (hero?.bag?.get(Orb.VAAL_ORB.name) ?: 0L) - (runJournal?.pending?.count { it.kind == RunEventKind.CRYSTAL_VAAL } ?: 0)

    /**
     * Ещё одно событие: журнал записан, контрольная точка или полная пачка отправляют его сразу. Прочие события боя -
     * убийства и начала боёв - копятся и уходят одной пачкой через `run.journalSeconds` правил; итог выигранного боя вручную
     * отправляет её сразу - его добычу ждёт экран итогов. Сбоящая связь держит паузу повтора (и для контрольных точек и полных
     * пачек - без связи событие не шлёт запрос само).
     */
    private fun recorded(event: RunEvent) {
        val j = runJournal ?: return
        expedition { it.copy(pending = j.pending.size) }
        persist()
        when {
            failures > 0 -> Unit
            event.kind in CHECKPOINTS || j.pending.size >= RunJournal.MAX_BATCH -> flushes.trySend(Unit)
            event is RunEvent.Fight && event.fight.won -> flushes.trySend(Unit)
            else -> sendBatch()
        }
    }

    /** Пачка боя уходит через `run.journalSeconds` правил после первого события, если её отправка ещё не назначена. */
    private fun sendBatch() {
        if (batchJob?.isActive == true) return
        val seconds = index?.rules?.run?.journalSeconds ?: return
        batchJob = scope.launch {
            delay(seconds * 1_000L)
            flushes.trySend(Unit)
        }
    }

    /** Отправка через паузу [after] вместо ждущей. */
    private fun sendIn(after: Long) {
        sendJob?.cancel()
        sendJob = scope.launch {
            delay(after)
            flushes.trySend(Unit)
        }
    }

    /**
     * Отправка не дошла: следующая после удвоенной паузы, не дольше [FLUSH_EVERY]; ответ сервера сбрасывает счёт ([linkBack]).
     */
    private fun retryLater() {
        failures++
        sendIn((RETRY_FIRST shl (failures - 1).coerceAtMost(RETRY_DOUBLINGS)).coerceAtMost(FLUSH_EVERY))
    }

    /** Сервер снова ответил: счёт неудач сброшен. */
    private fun linkBack() {
        failures = 0
    }

    /** Журнал на диск через миг после последнего события: залп убийств - одна запись. */
    private fun persist() {
        val j = runJournal ?: return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_AFTER)
            store.saveJournal(j.heroId, j.encode())
        }
    }

    /**
     * Отправляет, что держит журнал - пачку в пути снова под её ключом или ожидающие события под новым; ответ -
     * счёт сервера и награда каждого события, снимок героя едет с ним.
     *
     * Ключ пачки - на диске до её ухода: потерянный с процессом ответ спрашивается снова. Отклонённое начало боя - не потеря игрока, о нём
     * не говорят. Длинный журнал уходит частями подряд. `CP_018`, `CP_020`, `CP_026` кончают журнал похода. Сервер недоступен (5xx) -
     * повтор с паузой и одно сообщение на всю пропажу связи; отказ - последнее слово под ключом пачки, следующая отправка - новый запрос;
     * офлайн или таймаут - снова скоро.
     */
    private suspend fun flush() {
        val j = runJournal ?: return
        val outgoing = j.outgoing { UUID.randomUUID().toString() }
        if (outgoing == null) {
            if (j.closed) done(j)
            return
        }
        val (batch, pending) = outgoing
        try {
            store.saveJournal(j.heroId, j.encode())
            val report = api.campaign.events(j.heroId, j.runId, pending, batch.key)
            linkBack()
            if (report == null) {
                landed(j, batch.end)
                return
            }
            settle(j, pending, report)
            if (!report.open) shut = j.runId
            j.confirm(report.applied, report.rejected)
            expedition { it.copy(pending = j.pending.size, rejected = j.rejected.size) }
            val engages = pending.filter { it.kind == RunEventKind.ENGAGE }.map { it.n }.toSet()
            report.rejected.count { it !in engages }.takeIf { it > 0 }?.let { notices.toast(ui("expedition.rejected", it)) }
            report.received.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { notices.toast(receivedText(it, world.state.value.content.shardsPerOrb)) }
            if (!report.open && j.settled) done(j, report) else store.saveJournal(j.heroId, j.encode())
            if (heroes.state.value.readAt == 0L && heroes.onScreen(j.heroId)) heroSync.readHero()
            if (report.open && !j.settled) flushes.trySend(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFailure) {
            val transient = CommandQueue.transient(e.status) || (e.status ?: 0) >= SERVER_ERROR
            when {
                e.code in RUN_CLOSED -> {
                    shut = j.runId
                    done(j)
                }

                transient && failures > 0 -> Unit

                else -> commands.report(e, writing = true)
            }
            if (transient) retryLater() else j.release()
        } catch (_: Exception) {
            retryLater()
        }
    }

    /** Пачка отвечена как дошедшая без отчёта: берётся до [end], добыча придёт с перечитанным героем. */
    private suspend fun landed(j: RunJournal, end: Int) {
        runs().forEach { it.send(RunCommand.Settled(end)) }
        j.confirm(end)
        expedition { it.copy(pending = j.pending.size, rejected = j.rejected.size) }
        if (j.closed && j.settled) done(j) else store.saveJournal(j.heroId, j.encode())
        heroSync.readHero()
        campaign()
        if (!j.settled) flushes.trySend(Unit)
    }

    /** Ответ ложится на идущие походы - награды по событиям, затем принесённая кампания, - а снаряжение в «Новый лут». */
    private fun settle(j: RunJournal, batch: List<RunEvent>, report: RunReport) {
        val fell = batch.any { it.kind == RunEventKind.FALL && it.n < report.applied && it.n !in report.rejected }
        val crystals = report.rewards.mapNotNull { event -> event.crystal?.let { event.n to it.crystal } }.toMap()
        val answer = RunCommand.Settled(
            report.applied,
            report.rewards.associate { it.n to it.reward.toReward() },
            report.rejected,
            report.lost.takeIf { fell },
            crystals,
            report.rewards.mapNotNull { r -> r.rank?.let { r.n to it } }.toMap(),
        )
        runs().forEach { it.send(answer) }
        campaign()
        loot(j.heroId, report.rewards.flatMap { it.reward.equipment })
        atlasEarned(j.heroId, report.rewards.flatMap { it.atlas })
    }

    /**
     * Новые очки Атласа из ответа: тост на каждое и строки в итогах карты или испытания; [heroId] - чей ответ, чужой
     * экран их не получает.
     */
    fun atlasEarned(heroId: String, keys: List<String>) {
        if (keys.isEmpty() || !heroes.onScreen(heroId)) return
        heroSync.pointsEarned(keys)
        expedition { it.copy(atlasPoints = (it.atlasPoints + keys).distinct()) }
    }

    /** Заход или испытание начат: очки Атласа прошлого больше не его. */
    fun atlasReset() = expedition { it.copy(atlasPoints = emptyList()) }

    /** Походы, которых касается ответ журнала: на экране и карта, из которой вошли в зону Ваал. */
    private fun runs(): List<ExpeditionRun> = listOfNotNull(mutableRun.value, parent)

    /** Кампания героя, как её держит клиент сейчас, - идущим походам. */
    private fun campaign() {
        val state = hero?.takeIf { it.id == runJournal?.heroId }?.campaign ?: return
        runs().forEach { it.send(RunCommand.Campaign(state)) }
    }

    /**
     * Поход закрыт и журнал сведён; [report] - ответ, что закрыл заход: с ним сервер сдал задания.
     *
     * Чего сервер не ответил, того не ответит. Поход сдаётся целиком: сервером в закрывшем ответе или отдельным запросом; сданное - блоком
     * в итогах карты, закрытые итоги - тостом.
     */
    private suspend fun done(j: RunJournal, report: RunReport? = null) {
        if (runJournal === j) runs().forEach { it.send(RunCommand.Settled(Int.MAX_VALUE)) }
        if (runJournal === j) runJournal = null
        store.clearJournal(j.heroId)
        quests.turnIn(j.heroId, report?.turnIns, report?.guildQuests) { turnedIn ->
            val shown = mutableRun.value?.journal?.runId == j.runId
            if (shown) expedition { it.copy(turnedIn = it.turnedIn + turnedIn) }
            shown
        }
    }

    /** Пометка вещи добычи к продаже посреди захода: в итогах карты помеченное уже отмечено; повтор снимает. */
    fun toggleSaleMark(itemId: String) = expedition { it.copy(saleMarks = if (itemId in it.saleMarks) it.saleMarks - itemId else it.saleMarks + itemId) }

    /** Отправить журнал сейчас: приложение уходит в фон или поход окончен. */
    fun flushRun() {
        flushes.trySend(Unit)
    }

    /**
     * При входе в героя: журнал с прошлого запуска отправляется, если поход героя всё ещё тот, что он называет;
     * начатый с тех пор или никакой значит, что сервер его закрыл, и журнал отбрасывается.
     */
    suspend fun resume(id: String) {
        store.journal(id)?.let(RunJournal::decode)?.let { kept -> resumeJournal(id, kept) }
        offerUnfinished(id)
    }

    private suspend fun resumeJournal(id: String, kept: RunJournal) {
        val open = hero?.takeIf { it.id == id }?.campaign?.run
        if (open?.id != kept.runId || kept.settled) {
            store.clearJournal(id)
            return
        }
        runJournal = kept
        expedition { it.copy(pending = kept.pending.size, rejected = kept.rejected.size) }
        flush()
    }

    /**
     * Заход, открытый на сервере, когда похода на экране нет: приложение закрылось или упало посреди него. Сервер держит
     * его час с последнего запроса - простоявший он уже закрыл, - игроку предлагается продолжить или покинуть.
     */
    private fun offerUnfinished(id: String) {
        val open = hero?.takeIf { it.id == id }?.campaign?.run ?: return
        if (mutableRun.value != null) return
        expedition { it.copy(unfinished = UnfinishedRun(open.id, MapCode(open.zone))) }
    }

    private fun stance(): HeroStance = hero?.let { HeroStance.of(it.heroClass) } ?: HeroStance()

    /**
     * Герой, как его берёт поход: лист и из чего он сложен, стойка и что он несёт сверх листа.
     *
     * Рычаги боя Предначертания - на силе листа, с судьбоносными строками надетого.
     */
    fun gear(): HeroGear? {
        val i = index ?: return null
        val h = hero ?: return null
        val conditions = h.skills.flasks
        val flasks = Slot.FLASKS.mapIndexed { n, slot -> h.equipped[slot]?.let { item -> i.template(item.template)?.let { BeltFlask.of(item, it, i, conditions.getOrNull(n)) } } }
        val fate = FateKit.of(i, h.info.fate, h.stats, h.equipped.values)
        return HeroGear(h.stats, h.level, h.sheet.model, stance(), Loadout.of(h.skills, i, h.heroClass, flasks, h.stats, i.powers, i.rules.charges, fate), i.stats.percent)
    }

    fun send(command: RunCommand) {
        mutableRun.value?.send(command)
    }

    /** «Войти» у врат Ваал: портал закрывается за героем, зона - свой поход на том же семени и журнале. */
    fun enterVaal() {
        val outer = mutableRun.value ?: return
        val i = index ?: return
        val h = hero ?: return
        val gear = gear()?.copy(stance = outer.stance) ?: return
        val journal = runJournal ?: return
        if (parent != null || outer.hud.value.gate == null) return
        outer.send(RunCommand.ShutGate(entered = true))
        val inner = ExpeditionRun.start(
            i, outer.run.zone, outer.run, journal, gear, h.campaign, System.currentTimeMillis(), h.info.experience, h.level,
            vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded, vaal = true, startPools = outer.pools,
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, killed = vaalKilled, inherited = outer.pacts,
            fateRun = outer.fateRun,
        ).also { r -> repeat(speedSteps) { r.send(RunCommand.Speed) } }
        parent = outer
        mutableRun.value = inner
    }

    /** Страница «Похода»: карта, испытания или Атлас - экраном под вкладкой; Атлас прежде готовит своё окно. */
    fun openPage(route: Route) {
        if (route == Route.Atlas) prepareAtlas()
        navigator.tab(route)
    }

    /** Атлас (страница «Похода»): окно с последним взятым узлом. */
    fun openAtlas() = openPage(Route.Atlas)

    private fun prepareAtlas() {
        val last = index?.let { i -> hero?.let { h -> (listOf(i.atlasGraph.start) + h.info.atlas).lastOrNull() } }.orEmpty()
        expedition { it.copy(atlas = it.atlas ?: AtlasWindow(selected = last)) }
        heroSync.ensure()
    }

    fun selectAtlasNode(code: String) = expedition { it.copy(atlas = it.atlas?.copy(selected = code)) }

    /** Команда атласа: снимок героя с её ответом несёт новые узлы и очки. */
    fun allocateAtlas(code: String) = atlasCommand { id -> api.atlas.allocate(id, code) }

    /** Золотом или, с [regret], Сферой сожаления за узел. */
    fun refundAtlas(code: String, regret: Boolean = false) = atlasCommand { id -> api.atlas.refund(id, code, regret) }
    fun resetAtlas(regret: Boolean = false) = atlasCommand { id -> api.atlas.reset(id, regret) }

    private fun atlasCommand(call: suspend (String) -> Unit) = commands.task(writing = true, touches = setOf(Reads.HERO)) {
        call(heroes.heroId)
        if (heroes.state.value.readAt == 0L) heroSync.readHero()
    }

    /** Новое чтение героя: походу - кампания, зона Ваал или кристалл, что сервер решил. */
    private fun heroChanged(view: HeroView) {
        if (runJournal?.heroId == view.id) runs().forEach { it.send(RunCommand.Campaign(view.campaign)) }
    }

    /**
     * Поход окончен или брошен: журнал уходит, герой перечитывается, если ответы оставили его холодным. Выигранная
     * зона Ваал - не конец: герой снова на карте у портала с жизнью, что оставила зона. Павший в ней
     * гибнет, как везде: весь поход окончен. С [then] заход ещё и закрывается на сервере - журнал сдаётся, заход
     * брошен, как выходом, - и уже вне захода выполняется [then]: продажа добычи, которой в заходе нет.
     *
     * Конец захода закрывает его на сервере всегда, иначе та же карта с её бонусами проходилась бы снова.
     */
    fun close(then: (suspend (heroId: String) -> Unit)? = null) {
        val outer = parent
        val zone = mutableRun.value
        if (outer != null && zone != null && zone.heroLife > 0) {
            outer.send(RunCommand.Returned(outer.hero.maxLife * zone.heroLife / zone.hero.maxLife, zone.pools, zone.share()))
            parent = null
            mutableRun.value = outer
            flushes.trySend(Unit)
            return
        }
        parent = null
        mutableRun.value = null
        heroes.stale()
        val runId = runJournal?.runId ?: hero?.campaign?.run?.id
        val closing = commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroes.heroId
            runId?.let { finish(id, it) }
            then?.invoke(id)
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
        }
        if (!closing) {
            flushes.trySend(Unit)
            heroSync.ensure()
        }
    }

    /**
     * Закрывает заход [runId] на сервере: недошедший журнал отправляется (его добыча - героя), затем заход брошен, как выходом;
     * если последний ответ журнала уже закрыл заход ([shut]), лишнего запроса нет.
     */
    private suspend fun finish(heroId: String, runId: String) {
        runJournal?.takeIf { it.runId == runId }?.let { j ->
            flush()
            if (runJournal === j) runJournal = null
            store.clearJournal(heroId)
        }
        if (shut != runId) api.campaign.abandon(heroId, runId)
    }

    private fun loot(heroId: String, equipment: List<ItemInstance>) {
        if (equipment.isEmpty() || !heroes.onScreen(heroId)) return
        val now = System.currentTimeMillis()
        expedition { it.copy(runLoot = it.runLoot + equipment.map { piece -> LootEntry(piece, now) }) }
    }

    /** Брошен без слова: герой или его сессия ушли. Журнал остаётся на диске до следующего входа. */
    fun drop() {
        mutableRun.value = null
        parent = null
        runJournal = null
        repository.clear()
    }

    private companion object {
        /** Отказы сервера «поход не открыт» и «мир сменился, поход закрыт»: журналу некуда идти. */
        val RUN_CLOSED = setOf("CP_018", "CP_020", "CP_026")

        /** Отказ сервера «новое семя только спустя время» (CP_021). */
        const val SEED_TOO_SOON = "CP_021"

        /**
         * События, что отправляют журнал сразу: всё, что не убийство и не начало или итог боя. Сундук - тоже: его крышка
         * открыта, добыча вылетает, как придёт ответ.
         *
         * Объект карты (золото торговца, сделка алтаря, добыча комнаты) ждёт ответа сразу.
         */
        val CHECKPOINTS = setOf(
            RunEventKind.CHEST,
            RunEventKind.BOSS, RunEventKind.CORRUPT, RunEventKind.CRYSTAL, RunEventKind.CRYSTAL_VAAL, RunEventKind.VAAL_OPEN, RunEventKind.VAAL_LEAVE,
            RunEventKind.ABYSS_OPEN, RunEventKind.ABYSS_CLAIM, RunEventKind.SUMMON, RunEventKind.FALL, RunEventKind.LEAVE,
            RunEventKind.FEATURE,
        )
        const val FLUSH_EVERY = 20_000L

        /** С этого кода ответ - сбой самого сервера, а не отказ правила. */
        const val SERVER_ERROR = 500

        /** Первый повтор после неудачной отправки и сколько раз пауза удваивается до часовой. */
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 3
        const val SAVE_AFTER = 800L
    }
}
