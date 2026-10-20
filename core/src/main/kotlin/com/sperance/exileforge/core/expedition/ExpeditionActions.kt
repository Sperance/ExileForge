package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.app.DeviceStore
import com.sperance.exileforge.core.app.Landing
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.app.Screens
import com.sperance.exileforge.core.campaign.AtlasWindow
import com.sperance.exileforge.core.campaign.BeltFlask
import com.sperance.exileforge.core.campaign.CombatSheet
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
import com.sperance.exileforge.core.campaign.run.FOUGHT_EVENTS
import com.sperance.exileforge.core.campaign.run.GearPreview
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.display.receivedText
import com.sperance.exileforge.core.display.shardsPerOrb
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.hero.HeroSync
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignProgress
import com.sperance.exileforge.core.model.campaign.RunReport
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.questboard.QuestActions
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.WorldRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

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
    private val sessions: SessionRepository,
    private val heroSync: HeroSync,
    private val world: WorldRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val quests: QuestActions,
    private val prefs: PreferencesRepository,
    private val store: DeviceStore,
    private val screens: Screens,
    /** Действия героя - продажа добычи по концу захода; лениво: они сами держат поход. */
    private val heroActions: Lazy<HeroActions>,
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

    /** Заход, которому принадлежат пометки к продаже на экране: вход в него же после перезапуска их сохраняет, в другой - снимает. */
    private var marked: String? = null

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

    /** Смены набора похода считаются по одной ([wear]): следующая - поверх набора, что ушёл последним. */
    private val wearings = Mutex()

    /** Смена набора, что ушла в поход и им ещё не принята или не отклонена; null - смен в пути нет, база смены - набор похода. */
    private val sentWear = AtomicReference<SentWear?>(null)

    /** Смена в пути: поход [run] и надетое по месту [worn] с ней. */
    private class SentWear(val run: ExpeditionRun, val worn: Map<Slot, String>)

    /**
     * Герой, как его одел поход по карте на экране: вещи - по набору, что поход носит ([ExpeditionRun.hud]), лист и лист боя - с ним; набор совпал с
     * сервером - сам снимок героя; null - похода нет. Сравнения и метки лута в походе - с ним, а не с набором на сервере. Лист считается
     * вне главного потока и только пока его читают.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val runHero: StateFlow<HeroView?> = mutableRun
        .flatMapLatest { run -> run?.hud?.map { it.worn }?.distinctUntilChanged() ?: flowOf(null) }
        .combine(heroes.state.map { it.hero }.distinctUntilChanged { a, b -> a === b }) { worn, view -> worn?.let { view?.to(it) } }
        .mapLatest { pair -> pair?.let { (view, worn) -> dressed(view, worn) } }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.WhileSubscribed(), null)

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

    /** Прогресс кампании, как его считают правила по герою и миру. */
    private fun progress(): CampaignProgress? = index?.let { i -> hero?.let { h -> CampaignProgress(h.campaign.cleared.filter { it in i.zones }, i.world.unlocked(h.campaign.cleared)) } }

    /** Зона героя на карте мира: открытый на сервере заход, иначе последняя, в которую он входил на этом устройстве; нет - null. */
    fun heroZone(): MapCode? = hero?.campaign?.run?.zone?.let(::MapCode) ?: prefs.lastZone(heroes.heroId)

    /** Карта мира своего не читает: прогресс и атлас - героя. */
    fun loadCampaign() = heroSync.ensure()

    fun selectZone(mapCode: String) = expedition { it.copy(launch = MapLaunch(MapCode(mapCode))) }
    fun closeZone() = expedition { it.copy(launch = null) }

    /** Карта из сундука, с которой войти, или null - войти без неё. */
    fun pickMap(itemId: String?) = expedition { e -> e.copy(launch = e.launch?.let { l -> l.copy(picked = itemId, scarabs = if (itemId == null) emptyList() else l.scarabs) }) }

    /** Механика, что тестировщик ставит на заход насильно: переключить одну; не тестировщику - ничего. */
    fun toggleForced(mechanic: MapMechanic) {
        if (tester) expedition { it.copy(forced = if (mechanic in it.forced) it.forced - mechanic else it.forced + mechanic) }
    }

    /** Все механики разом - или ни одной; не тестировщику - ничего. */
    fun forceAll(on: Boolean) {
        if (tester) expedition { it.copy(forced = if (on) MapMechanic.entries.toSet() else emptySet()) }
    }

    /** Тестировщик со свободным прогрессом: насильные механики и бой сразу со стражем. */
    private val tester: Boolean get() = sessions.state.value.free(heroes.state.value)

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
     *
     * [toBoss] (тестировщик и выше) - сразу бой со стражем.
     */
    fun startRun(mapCode: String, toBoss: Boolean = false) = start(MapCode(mapCode), toBoss = toBoss && tester)

    /** Заход в зону [mapCode]; [toBoss] - сразу бой со стражем. */
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
     * на сервере, как выходом. Итогов нет - отложенная добыча захода продаётся вся.
     */
    fun abandonUnfinished() {
        val offer = repository.state.value.unfinished ?: return
        expedition { it.copy(unfinished = null) }
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroes.heroId
            finish(id, offer.runId)
            settleDeferred(id, emptyList())
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
        val marks = repository.state.value.saleMarks.takeIf { marked == started.id }.orEmpty()
        marked = started.id
        scope.launch { prefs.keepLastZone(id, zone.code) }
        val journal = RunJournal(started.id, id, zone.code.value, applied = started.applied, base = started.applied, saleMarks = marks, chestStock = started.chestStock).also { runJournal = it }
        if (marks.isNotEmpty()) persist()
        val board = quests.board().takeIf { it.board != null }
        expedition {
            it.copy(
                runLoot = emptyList(), launch = null, pending = 0, rejected = 0, unfinished = null, saleMarks = marks,
                quests = board, turnedIn = emptyList(), atlasPoints = emptyList(), potion = potion, wearable = started.wearable.toSet(),
            )
        }
        mutableRun.value = ExpeditionRun.start(
            i, zone, run, journal, gear, h.campaign, System.currentTimeMillis(), h.info.experience, h.level,
            vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded,
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, onGatherBroken = { notices.toast(ui("expedition.gather_broken"), NoticeKind.FAILED) },
            killed = started.killed, opened = started.chests, features = started.features,
            summoned = started.tally.summons,
            bossSlain = started.tally.bosses > 0,
            fateRun = FateRun(started.killed, started.vaalKilled, started.tally.bosses),
            speed = prefs.fightSpeed(i.rules.fight),
        ).also { r -> if (toBoss) r.send(RunCommand.ToBoss) }
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
     * не говорят; прочие отказы - красный тост с причиной сервера поверх тоста добычи. Длинный журнал уходит частями подряд. `CP_018`, `CP_020`, `CP_026` кончают журнал похода. Сервер недоступен (5xx) -
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
            receivedText(report.received, world.state.value.content.shardsPerOrb)?.let { notices.toast(it) }
            notices.refused(report.rejected.filter { it !in engages }, report.refusals)
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

    /**
     * Ответ ложится на идущие походы - награды по событиям, затем принесённая кампания, - а снаряжение в «Новый лут». Перекатанный
     * запас сундуков ответа подменяет запас журнала.
     */
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
            report.rewards.filter { it.atlas.isNotEmpty() }.associate { it.n to it.atlas },
        )
        report.rewards.lastOrNull { it.chestStock != null }?.chestStock?.let { j.chestStock = it }
        markDeferred(j, report.received.deferred)
        val reported = runs().takeIf { it.isNotEmpty() }?.let { report.rewards.filter { r -> r.kind in FOUGHT_EVENTS }.flatMap { r -> r.atlas } }.orEmpty()
        runs().forEach { it.send(answer) }
        campaign()
        loot(j.heroId, report.rewards.flatMap { it.reward.equipment })
        atlasEarned(j.heroId, report.rewards.flatMap { it.atlas }, reported)
    }

    /**
     * Новые очки Атласа из ответа: строки в итогах карты или испытания и тост на каждое, кроме [reported] - их показывает отчёт
     * боя или окно уровня; [heroId] - чей ответ, чужой экран их не получает.
     */
    fun atlasEarned(heroId: String, keys: List<String>, reported: Collection<String> = emptyList()) {
        if (keys.isEmpty() || !heroes.onScreen(heroId)) return
        heroSync.pointsEarned(keys, reported)
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

    /**
     * Пометка вещи добычи к продаже посреди захода: в итогах карты помеченное уже отмечено; повтор снимает. Пометки пишутся в журнал
     * захода на устройстве ([RunJournal.saleMarks]) и переживают перезапуск.
     */
    fun toggleSaleMark(itemId: String) {
        expedition { it.copy(saleMarks = if (itemId in it.saleMarks) it.saleMarks - itemId else it.saleMarks + itemId) }
        runJournal?.let { j ->
            j.saleMarks = repository.state.value.saleMarks
            persist()
        }
    }

    /** Вещь [itemId] продана поштучно: её отметка к продаже уходит с экрана и из журнала захода ([RunJournal.saleMarks]). */
    fun forget(itemId: String) {
        if (itemId in repository.state.value.saleMarks) expedition { it.copy(saleMarks = it.saleMarks - itemId) }
        runJournal?.takeIf { itemId in it.saleMarks }?.let { j ->
            j.saleMarks = j.saleMarks - itemId
            persist()
        }
    }

    /**
     * Отложенная продажа: добыча захода, что продаёт фильтр героя, сервер кладёт в тайник и называет её id - она ложится в пометки к продаже
     * ([RunJournal.saleMarks]), и итоги карты отмечают ровно её; снятую отметку итоги передают серверу ([closeSettling]).
     */
    private fun markDeferred(j: RunJournal, ids: List<String>) {
        if (ids.isEmpty()) return
        j.saleMarks = j.saleMarks + ids
        if (marked == j.runId) expedition { it.copy(saleMarks = it.saleMarks + ids) }
    }

    /** Отправить журнал сейчас: приложение уходит в фон или поход окончен. */
    fun flushRun() {
        flushes.trySend(Unit)
    }

    /**
     * При входе в героя: журнал с прошлого запуска отправляется, если поход героя всё ещё тот, что он называет;
     * начатый с тех пор или никакой значит, что сервер его закрыл, и журнал отбрасывается. Пометки к продаже журнала того же
     * захода возвращаются на экран; принятый сервером журнал с пометками лежит на диске до входа в заход.
     */
    suspend fun resume(id: String) {
        store.journal(id)?.let(RunJournal::decode)?.let { kept -> resumeJournal(id, kept) }
        offerUnfinished(id)
    }

    private suspend fun resumeJournal(id: String, kept: RunJournal) {
        val open = hero?.takeIf { it.id == id }?.campaign?.run
        if (open?.id != kept.runId) {
            store.clearJournal(id)
            return
        }
        marked = kept.runId
        expedition { it.copy(saleMarks = kept.saleMarks) }
        if (kept.settled) {
            if (kept.saleMarks.isEmpty()) store.clearJournal(id)
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

    /** Герой, как его берёт поход: в своём наборе ([gear] с вещами героя). */
    fun gear(): HeroGear? = hero?.let { gear(it, it.items) }

    /**
     * Герой [h] с вещами [items], как его берёт поход: лист и из чего он сложен, стойка, что он несёт сверх листа, и надетое по месту.
     * Вещи героя как есть - его лист; иной набор - лист тем же расчётом правил, что у сервера ([Sheets.calculate]).
     *
     * Рычаги боя Предначертания - на силе листа, с судьбоносными строками надетого.
     */
    private fun gear(h: HeroView, items: List<ItemInstance>): HeroGear? {
        val i = index ?: return null
        val sheet = if (items === h.items) h.sheet else Sheets.calculate(i, h.level, h.heroClass, h.tree, items, h.info.laws, h.info.fate)
        val equipped = items.filter { it.equipped && !it.socketed }.associateBy { it.slot!! }
        val conditions = h.skills.flasks
        val flasks = Slot.FLASKS.mapIndexed { n, slot -> equipped[slot]?.let { item -> i.template(item.template)?.let { BeltFlask.of(item, it, i, conditions.getOrNull(n)) } } }
        val fate = FateKit.of(i, h.info.fate, sheet.stats, equipped.values)
        val kit = Loadout.of(h.skills, i, h.heroClass, flasks, sheet.stats, i.powers, i.rules.charges, fate)
        return HeroGear(sheet.stats, h.level, sheet.model, stance(), kit, i.stats.percent, equipped.mapValues { it.value.id })
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
            speed = prefs.fightSpeed(i.rules.fight),
        )
        parent = outer
        mutableRun.value = inner
    }

    /** Атлас (страница «Похода»): прежде готовит своё окно с последним взятым узлом. */
    fun openAtlas() {
        prepareAtlas()
        screens.tab(Landing.ATLAS)
    }

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

    /** Новое чтение героя: походу - кампания, зона Ваал или кристалл, что сервер решил, и набор сервера ([resync]). */
    private fun heroChanged(view: HeroView) {
        if (runJournal?.heroId != view.id) return
        runs().forEach { it.send(RunCommand.Campaign(view.campaign)) }
        resync(view)
    }

    /**
     * Поход берёт набор героя [view], каким его держит сервер, когда журнал уже не везёт своих смен ([RunEventKind.REGEAR]), а набор похода
     * с ним разошёлся: сервер отклонил смену. Совпадают - ничего не считается.
     */
    private fun resync(view: HeroView) {
        if (runJournal?.heroId != view.id || runJournal?.pending?.any { it.kind == RunEventKind.REGEAR } == true) return
        val worn = view.equipped.mapValues { it.value.id }
        val behind = runs().filter { it.build.gear.worn != worn }.takeIf { it.isNotEmpty() } ?: return
        val gear = gear(view, view.items) ?: return
        behind.forEach { it.send(RunCommand.Regear(gear.copy(stance = it.stance))) }
    }

    /**
     * «Надеть» в походе по карте: вещь, что в заходе можно надеть ([Expedition.wearable]: набор входа и находки), встаёт на место [slot]
     * (null - решают правила, как у сервера: [Sheets.worn]) между боями. Смены идут по одной: набор с вещью считается тем же расчётом листа,
     * что у сервера, поверх набора, что ушёл последней сменой (нет её в пути - что поход носит); поход берёт его, только если носит ровно
     * эту базу, и пишет смену в журнал с выбранным местом ([RunCommand.PutOn]); карта под зоной Ваал - следом, без своего события, только
     * если поход смену принял. Сервер проверяет то же сам: отказ вернёт набор сервера ([resync]). Лист считается вне главного потока.
     */
    fun wear(itemId: String, slot: Slot? = null) {
        val run = mutableRun.value ?: return
        if (itemId !in repository.state.value.wearable || run.hud.value.gearLock != null) return
        val outer = parent
        scope.launch(Dispatchers.Default) {
            wearings.withLock {
                val over = baseOf(run)
                val (gear, place) = wearing(run, over, itemId, slot) ?: return@withLock
                val sent = SentWear(run, gear.worn)
                sentWear.set(sent)
                run.send(
                    RunCommand.Regear(
                        gear.copy(stance = run.stance),
                        RunCommand.PutOn(itemId, place, over) { taken ->
                            sentWear.compareAndSet(sent, null)
                            if (taken) outer?.send(RunCommand.Regear(gear.copy(stance = outer.stance)))
                        },
                    ),
                )
            }
        }
    }

    /**
     * Что изменит в походе надетая вещь [itemId] на месте [slot] ([ExpeditionRun.preview]); null - похода нет или вещь не надеть. Лист
     * считается здесь - вызывать вне главного потока.
     */
    fun wearPreview(itemId: String, slot: Slot? = null): GearPreview? {
        val run = mutableRun.value ?: return null
        return wearing(run, baseOf(run), itemId, slot)?.first?.let(run::preview)
    }

    /** Набор, поверх которого считается смена в поход [run]: что ушло последней сменой, а без неё в пути - что поход носит. */
    private fun baseOf(run: ExpeditionRun): Map<Slot, String> = sentWear.get()?.takeIf { it.run === run }?.worn ?: run.build.gear.worn

    /**
     * Набор героя похода с вещью [itemId] на месте [slot] поверх надетого [over] и место, куда она встала по правилу сервера
     * ([Sheets.worn]); null - вещь не надеть.
     */
    private fun wearing(run: ExpeditionRun, over: Map<Slot, String>, itemId: String, slot: Slot?): Pair<HeroGear, Slot>? {
        val i = index ?: return null
        val h = hero?.takeIf { it.id == runJournal?.heroId && mutableRun.value === run } ?: return null
        val items = wornAs(h.items, over)
        val item = items.firstOrNull { it.id == itemId }?.takeIf { !it.equipped } ?: return null
        val worn = Sheets.worn(i, item, items, slot) ?: return null
        val place = worn.firstOrNull { it.id == itemId }?.slot ?: return null
        return gear(h, worn)?.let { it to place }
    }

    /** Герой [view] в наборе похода [worn]: вещи по месту и листы с ними; набор тот же, что у снимка, - сам снимок. */
    private fun dressed(view: HeroView, worn: Map<Slot, String>): HeroView {
        val i = index ?: return view
        if (view.equipped.mapValues { it.value.id } == worn) return view
        val items = wornAs(view.items, worn)
        val sheet = Sheets.calculate(i, view.level, view.heroClass, view.tree, items, view.info.laws, view.info.fate)
        val combat = CombatSheet.of(i, sheet, view.level, view.heroClass, view.skills, view.info.fate, items.filter { it.equipped })?.also { it.figures }
        return view.copy(items = items, sheet = sheet, combat = combat)
    }

    /** Вещи героя [items] в наборе [worn] - как их носит поход: надетое по месту, прочее в тайнике; самоцветы гнёзд - как были. */
    private fun wornAs(items: List<ItemInstance>, worn: Map<Slot, String>): List<ItemInstance> {
        if (worn.isEmpty()) return items
        val place = worn.entries.associate { (slot, id) -> id to slot }
        return items.map { if (it.socketed || it.slot == place[it.id]) it else it.copy(slot = place[it.id]) }
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
     * Выход из итогов захода: заход закрывается и на сервере, отмеченная добыча [sell] продаётся пачкой, затем сервер продаёт оставшуюся
     * отложенную добычу захода, кроме [keep] - добычи итогов со снятой отметкой.
     */
    fun closeSettling(sell: Collection<String>, keep: Collection<String>) = close { id ->
        if (sell.isNotEmpty()) heroActions.value.sellBatch(id, sell)
        settleDeferred(id, keep)
    }

    /** Отложенная добыча закрытого захода продаётся сервером, кроме [keep]; проданное - тостом, как автопродажа. */
    private suspend fun settleDeferred(heroId: String, keep: Collection<String>) {
        val received = api.campaign.settle(heroId, keep.toList())
        receivedText(received, world.state.value.content.shardsPerOrb)?.let { notices.toast(it) }
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

    /** Снаряжение из ответа журнала - в «Новый лут» и в то, что в заходе можно надеть ([Expedition.wearable]). */
    private fun loot(heroId: String, equipment: List<ItemInstance>) {
        if (equipment.isEmpty() || !heroes.onScreen(heroId)) return
        val now = System.currentTimeMillis()
        expedition { it.copy(runLoot = it.runLoot + equipment.map { piece -> LootEntry(piece, now) }, wearable = it.wearable + equipment.map { piece -> piece.id }) }
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
         * Объект карты (золото торговца, сделка алтаря, добыча комнаты) ждёт ответа сразу; смена набора - тоже: сервер мерит по ней следующий бой.
         */
        val CHECKPOINTS = setOf(
            RunEventKind.CHEST,
            RunEventKind.BOSS, RunEventKind.CORRUPT, RunEventKind.CRYSTAL, RunEventKind.CRYSTAL_VAAL, RunEventKind.VAAL_OPEN, RunEventKind.VAAL_LEAVE,
            RunEventKind.ABYSS_OPEN, RunEventKind.ABYSS_CLAIM, RunEventKind.SUMMON, RunEventKind.FALL, RunEventKind.LEAVE,
            RunEventKind.FEATURE, RunEventKind.REGEAR,
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
