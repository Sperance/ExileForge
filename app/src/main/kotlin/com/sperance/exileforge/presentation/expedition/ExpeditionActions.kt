package com.sperance.exileforge.presentation.expedition

import com.sperance.exileforge.core.campaign.AtlasWindow
import com.sperance.exileforge.core.campaign.Expedition
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.LootEntry
import com.sperance.exileforge.core.campaign.MapLaunch
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.StageCarry
import com.sperance.exileforge.core.campaign.UnfinishedRun
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.AutoPlan
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
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
import com.sperance.exileforge.presentation.state.GameSettings
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
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
 * Кампания: карта мира и поход по карте, которым идёт игрок (3.80.20, из `ExpeditionViewModel`).
 *
 * Поход - мир, который сцена шагает каждый кадр; он держится здесь, а оверлей читает его [ExpeditionRun.hud].
 * Каждое убийство, сундук и спуск - событие журнала похода; журнал лежит на диске и уходит на сервер пачками - на
 * контрольных точках, на выходе, при падении, в фоне, - и ответ сервера есть истина. Награды только серверные (1.30.0):
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
    private val index: ContentIndex? get() = world.state.value.content
    private val hero: HeroView? get() = heroes.state.value.hero

    private val mutableRun = MutableStateFlow<ExpeditionRun?>(null)
    val run: StateFlow<ExpeditionRun?> = mutableRun.asStateFlow()

    /** Поход по карте, из которого вошли в зону Ваал: стоит, пока зона играется. */
    private var parent: ExpeditionRun? = null

    /** Журнал похода, что идёт. */
    private var runJournal: RunJournal? = null
    private val flushes = Channel<Unit>(Channel.CONFLATED)
    private var saveJob: Job? = null

    /** Следующая отправка сама по себе: тихий отрезок после последнего события или повтор после неудачной. */
    private var sendJob: Job? = null

    /** Отправка события с добычей через [PROMPT_AFTER] после первого из залпа; поздние присоединяются, ничто не откладывает. */
    private var promptJob: Job? = null

    /** Отправок подряд не дошло: каждая ждёт вдвое дольше прошлой. */
    private var failures = 0

    /** Автопоход, с которым начат поход; его зона Ваал тоже идёт сама. */
    private var autoPlan: AutoPlan? = null

    /** Жетоны зоны Ваал, которые сервер уже сосчитал, когда в поход вошли снова. */
    private var vaalKilled: List<Int> = emptyList()

    /** Запуск после сборки графа: отправки журнала по часам и по сигналу, новое снаряжение и новый герой - походу. */
    fun start() {
        scope.launch { for (signal in flushes) flush() }
        // Отправка по часам: что тихий отрезок карты оставил в ожидании, уходит само.
        scope.launch {
            while (true) {
                delay(FLUSH_EVERY)
                if (runJournal?.pending?.isNotEmpty() == true) flushes.trySend(Unit)
            }
        }
        // Новое чтение героя: поход читает сумку через состояние и берёт кампанию - зону Ваал или кристалл, что сервер решил.
        scope.launch { heroes.state.map { it.hero }.distinctUntilChanged { a, b -> a === b }.filterNotNull().collect { heroChanged(it) } }
    }

    private fun expedition(transform: (Expedition) -> Expedition) = repository.update(transform)

    /** Сколько нажатий кнопки скорости боя дают скорость из настроек (3.77.0): 1 → 2 → 4. */
    private val speedSteps: Int get() = GameSettings.SPEEDS.indexOf(prefs.settings.value.fightSpeed).coerceAtLeast(0)

    /** Прогресс кампании, как его считают правила по герою и миру. */
    private fun progress(): CampaignProgress? = index?.let { i -> hero?.let { h -> CampaignProgress(h.campaign.cleared.filter { it in i.zones }, i.world.unlocked(h.campaign.cleared)) } }

    /** Карта мира своего не читает: прогресс и атлас - героя. */
    fun loadCampaign() = heroSync.ensure()

    fun selectZone(mapCode: String) = expedition { it.copy(launch = MapLaunch(MapCode(mapCode))) }
    fun closeZone() = expedition { it.copy(launch = null) }

    /** Карта из сундука, с которой войти, или null - войти без неё. */
    fun pickMap(itemId: String?) = expedition { e -> e.copy(launch = e.launch?.let { l -> l.copy(picked = itemId, scarabs = if (itemId == null) emptyList() else l.scarabs) }) }

    /** Зелье на поход (3.79.0): одно, повторным касанием снимается. */
    fun pickPotion(code: String?) = expedition { e -> e.copy(launch = e.launch?.let { it.copy(potion = code.takeIf { c -> c != it.potion }) }) }

    /** Скарабей к карте или снят (3.79.0): не больше, чем велят правила, и не больше одного вида, чем в сумке. */
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
    fun start(mapCode: MapCode, auto: AutoPlan? = null) = enter(mapCode, repository.state.value.launch?.takeIf { it.mapCode == mapCode }, auto)

    /**
     * «Продолжить» незаконченный заход (3.89.0): вход в ту же зону без карты - сервер отдаёт тот же заход, его павших и открытые
     * сундуки; герой у входа, туман заново.
     */
    fun continueUnfinished() {
        val offer = repository.state.value.unfinished ?: return
        if (commands.state.value.busy) return
        expedition { it.copy(unfinished = null) }
        enter(offer.zone, launch = null, auto = null)
    }

    /**
     * «Покинуть» незаконченный заход (3.89.0): журнал, что ещё не дошёл, отправляется - его добыча героя, - и заход закрывается
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

    private fun enter(mapCode: MapCode, launch: MapLaunch?, auto: AutoPlan?) {
        if (mutableRun.value != null || commands.state.value.busy) return
        val i = index ?: return
        val progress = progress()
        if (i.zone(mapCode) == null || progress?.unlocked?.contains(mapCode) != true) return
        val picked = launch?.picked
        // Автопоход (3.2.0) тратит карту зоны, чей страж уже пал однажды.
        if (auto != null && (picked == null || progress.cleared.contains(mapCode) != true)) return
        autoPlan = auto
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroes.heroId
            // Перенос этапа боя, прерванного перезапуском, продолжается в том же походе, куда вошли снова.
            val kept = runJournal?.let { it.runId to it.carry }
            // Журнал, которого сервер ещё не взял, не бросается ради нового похода: его убийства - героя.
            runJournal?.let { j ->
                flush()
                check(j.settled || runJournal == null) { ui("expedition.unsent") }
                store.clearJournal(id)
                runJournal = null
            }
            val started = try {
                api.campaign.start(id, mapCode, picked, launch?.potion, launch?.scarabs.orEmpty().takeIf { picked != null }.orEmpty())
            } catch (e: ApiFailure) {
                // Новое семя не раньше, чем велят правила, чем бы ни кончился прошлый поход (сервер 1.30.0): ожидание, не ошибка.
                if (e.code != SEED_TOO_SOON) throw e
                notices.toast(ui("expedition.seed_wait", e.args.firstOrNull().orEmpty()))
                return@task
            }
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
            heroSync.drawn()
            begin(id, started, kept?.takeIf { it.first == started.id }?.second)
        }
    }

    private fun begin(id: String, started: RunStart, carry: StageCarry? = null) {
        val i = index ?: return
        val h = hero?.takeIf { it.id == id } ?: return
        val zone = i.zone(started.zone) ?: return
        val gear = gear() ?: return
        // Войдя снова, поход продолжается (сервер 1.1.0): его числа и его павшие до сих пор.
        val run = Run(i, zone, started.seed, started.context)
        val journal = RunJournal(started.id, id, zone.code.value, applied = started.applied, base = started.applied, carry = carry).also { runJournal = it }
        journal.onCarry = ::persist
        expedition { it.copy(runLoot = emptyList(), launch = null, pending = 0, rejected = 0, unfinished = null) }
        mutableRun.value = ExpeditionRun.start(
            i, zone, run, journal, gear, h.campaign, System.currentTimeMillis(), h.info.experience, h.level,
            vaalOrbs = ::vaalOrbsFree, onRecorded = ::recorded,
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, killed = started.killed, opened = started.chests, features = started.features, auto = autoPlan,
            pet = ::combatPet,
        ).also { r -> repeat(speedSteps) { r.send(RunCommand.Speed) } }
        vaalKilled = started.vaalKilled
        persist()
    }

    /** Боевой питомец героя сейчас (3.70.0): поход читает его в начале каждого боя. */
    private fun combatPet(): Pet? = hero?.let { it.pets.pet(it.pets.combat) }

    /** Сферы Ваал под рукой за вычетом тех, что журнал потратил, а сервер ещё не сосчитал. */
    private fun vaalOrbsFree(): Long = (hero?.bag?.get(Orb.VAAL_ORB.name) ?: 0L) - (runJournal?.pending?.count { it.kind == RunEventKind.CRYSTAL_VAAL } ?: 0)

    /**
     * Ещё одно событие: журнал записан, контрольная точка или полная пачка отправляют его. Убийство или сундук боя
     * вручную уходят сразу: добыча в пути, пока бой ещё идёт. Автопоход ждёт тихого отрезка. Сбоящая связь держит
     * паузу повтора.
     */
    private fun recorded(event: RunEvent) {
        val j = runJournal ?: return
        expedition { it.copy(pending = j.pending.size) }
        persist()
        when {
            event.kind in CHECKPOINTS || j.pending.size >= BATCH -> flushes.trySend(Unit)
            failures > 0 -> Unit
            event.kind in LOOT && mutableRun.value?.hud?.value?.auto == null -> sendSoon()
            else -> sendIn(QUIET_AFTER)
        }
    }

    /** Отправка через [PROMPT_AFTER], если никакая не назначена раньше: враги одного удара уходят одной пачкой. */
    private fun sendSoon() {
        if (promptJob?.isActive == true) return
        promptJob = scope.launch {
            delay(PROMPT_AFTER)
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

    /** Отправка не дошла: следующая после удвоенной паузы, не дольше часовой. */
    private fun retryLater() {
        failures++
        sendIn((RETRY_FIRST shl (failures - 1).coerceAtMost(RETRY_DOUBLINGS)).coerceAtMost(FLUSH_EVERY))
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
            // Ключ пачки на диске до её ухода: ответ, потерянный с процессом, спрашивается снова после него.
            store.saveJournal(j.heroId, j.encode())
            val report = api.campaign.events(j.heroId, j.runId, pending, batch.key)
            failures = 0
            if (report == null) {
                landed(j, batch.end)
                return
            }
            settle(j, pending, report)
            j.confirm(report.applied, report.rejected)
            expedition { it.copy(pending = j.pending.size, rejected = j.rejected.size) }
            // Отклонённое начало боя (бой, который сервер не начал) - не потеря игрока: о нём не говорят.
            val engages = pending.filter { it.kind == RunEventKind.ENGAGE }.map { it.n }.toSet()
            report.rejected.count { it !in engages }.takeIf { it > 0 }?.let { notices.toast(ui("expedition.rejected", it)) }
            report.received.takeIf { it.overflowed > 0 || it.sold > 0 }?.let { notices.toast(ui("stash.received", it.overflowed, it.sold, it.gold)) }
            if (!report.open && j.settled) done(j) else store.saveJournal(j.heroId, j.encode())
            if (heroes.state.value.readAt == 0L && heroes.onScreen(j.heroId)) heroSync.readHero()
            // Журнал длиннее одной пачки идёт дальше сразу, часть за частью.
            if (report.open && !j.settled) flushes.trySend(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFailure) {
            // CP_018: сервер не держит похода для героя - поход журнала окончен, последняя пачка уже учтена;
            // CP_020: мир сменился под походом, сервер закрыл его; CP_026: журнал старшего похода.
            if (e.code in RUN_CLOSED) done(j) else commands.report(e, writing = true)
            // Отказ - последнее слово сервера под ключом пачки: следующая отправка - новый запрос, не повтор.
            if (!CommandQueue.transient(e.status)) j.release()
        } catch (_: Exception) {
            // Офлайн или таймаут: снова скоро, не на следующем круге часов.
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
            // Добыча начатых боёв (3.88.0): только показывается, выдаст её убийство - в «Новый лут» она не идёт.
            report.rewards.mapNotNull { r -> r.pending?.let { drops -> r.n to drops.associate { it.m to it.reward.toReward() } } }.toMap(),
        )
        runs().forEach { it.send(answer) }
        campaign()
        loot(j.heroId, report.rewards.flatMap { it.reward.equipment })
    }

    /** Походы, которых касается ответ журнала: на экране и карта, из которой вошли в зону Ваал. */
    private fun runs(): List<ExpeditionRun> = listOfNotNull(mutableRun.value, parent)

    /** Кампания героя, как её держит клиент сейчас, - идущим походам. */
    private fun campaign() {
        val state = hero?.takeIf { it.id == runJournal?.heroId }?.campaign ?: return
        runs().forEach { it.send(RunCommand.Campaign(state)) }
    }

    private suspend fun done(j: RunJournal) {
        // Чего сервер не ответил, того не ответит: никто больше не ждёт.
        if (runJournal === j) runs().forEach { it.send(RunCommand.Settled(Int.MAX_VALUE)) }
        if (runJournal === j) runJournal = null
        store.clearJournal(j.heroId)
        // Поход считается целиком (3.24.0): всё, что он закончил, сдаётся разом.
        quests.claimAll(j.heroId)
    }

    /** Отправить журнал сейчас: приложение уходит в фон или поход окончен. */
    fun flushRun() {
        failures = 0
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
        // Учтённый журнал с переносом этапа остаётся: поход, в который вошли снова, его подхватит.
        if (open?.id != kept.runId || kept.settled && kept.carry == null) {
            store.clearJournal(id)
            return
        }
        runJournal = kept
        kept.onCarry = ::persist
        expedition { it.copy(pending = kept.pending.size, rejected = kept.rejected.size) }
        flush()
    }

    /**
     * Заход, открытый на сервере, когда похода на экране нет (3.89.0): приложение закрылось или упало посреди него. Сервер держит
     * его час с последнего запроса - простоявший он уже закрыл, - игроку предлагается продолжить или покинуть.
     */
    private fun offerUnfinished(id: String) {
        val open = hero?.takeIf { it.id == id }?.campaign?.run ?: return
        if (mutableRun.value != null) return
        expedition { it.copy(unfinished = UnfinishedRun(open.id, MapCode(open.zone))) }
    }

    private fun stance(): HeroStance = hero?.let { HeroStance.of(it.heroClass) } ?: HeroStance()

    /** Герой, как его берёт поход: лист и из чего он сложен, стойка и что он несёт сверх листа. */
    fun gear(): HeroGear? {
        val i = index ?: return null
        val h = hero ?: return null
        val conditions = h.skills.flasks
        val flasks = Slot.FLASKS.mapIndexed { n, slot -> h.equipped[slot]?.let { item -> i.template(item.template)?.let { Flask.of(item, it, i, conditions.getOrNull(n)) } } }
        return HeroGear(h.stats, h.level, h.sheet.model, stance(), Loadout.of(h.skills, i.skills, h.heroClass, flasks, i.powers, i.rules.charges), i.stats.percent)
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
            onCleared = { flushes.trySend(Unit) }, onFallen = { flushes.trySend(Unit) }, killed = vaalKilled, inherited = outer.pacts, auto = autoPlan.takeIf { outer.hud.value.auto != null },
            pet = ::combatPet,
        ).also { r -> repeat(speedSteps) { r.send(RunCommand.Speed) } }
        parent = outer
        mutableRun.value = inner
    }

    // ==================== Атлас ====================

    fun openAtlas() {
        val last = index?.let { i -> hero?.let { h -> (listOf(i.atlasGraph.start) + h.info.atlas).lastOrNull() } }.orEmpty()
        expedition { it.copy(atlas = it.atlas ?: AtlasWindow(selected = last)) }
        navigator.open(Route.Atlas)
        heroSync.ensure()
    }

    fun closeAtlas() {
        navigator.back()
        expedition { it.copy(atlas = null) }
    }
    fun selectAtlasNode(code: String) = expedition { it.copy(atlas = it.atlas?.copy(selected = code)) }

    /** Команда атласа: снимок героя с её ответом несёт новые узлы и очки. */
    fun allocateAtlas(code: String) = atlasCommand { id -> api.atlas.allocate(id, code) }

    /** Золотом или, с [regret] (сервер 1.65.0), Сферой сожаления за узел. */
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
     * зона Ваал - не конец: герой снова на карте у портала с жизнью, что оставила зона. Павший в ней (3.71.0)
     * гибнет, как везде: весь поход окончен. С [then] (3.90.4) заход ещё и закрывается на сервере - журнал сдаётся, заход
     * брошен, как выходом, - и уже вне захода выполняется [then]: продажа добычи, которой в заходе нет (сервер - `CH_038`).
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
        if (then == null) {
            flushes.trySend(Unit)
            heroSync.ensure()
            return
        }
        val runId = runJournal?.runId ?: hero?.campaign?.run?.id
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroes.heroId
            runId?.let { finish(id, it) }
            then(id)
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
        }
    }

    /** Закрывает заход [runId] на сервере: недошедший журнал отправляется (его добыча - героя), затем заход брошен, как выходом. */
    private suspend fun finish(heroId: String, runId: String) {
        runJournal?.takeIf { it.runId == runId }?.let { j ->
            flush()
            if (runJournal === j) runJournal = null
            store.clearJournal(heroId)
        }
        api.campaign.abandon(heroId, runId)
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

        /** События, что отправляют журнал сразу: всё, что не простое убийство и не сундук. */
        val CHECKPOINTS = setOf(
            RunEventKind.BOSS, RunEventKind.CORRUPT, RunEventKind.CRYSTAL, RunEventKind.CRYSTAL_VAAL, RunEventKind.VAAL_OPEN, RunEventKind.VAAL_LEAVE,
            RunEventKind.ABYSS_OPEN, RunEventKind.ABYSS_CLAIM, RunEventKind.SUMMON, RunEventKind.FALL, RunEventKind.LEAVE,
            // Начало боя (3.88.0): его добыча нужна до первого убийства.
            RunEventKind.ENGAGE,
            // Объект карты (3.90.0): золото торговца, сделка алтаря и добыча комнаты - ответ нужен сразу.
            RunEventKind.FEATURE,
        )
        const val BATCH = 6
        const val FLUSH_EVERY = 20_000L

        /** Пачка убийства уходит через столько после последнего события, если ничто не отправит её раньше. */
        const val QUIET_AFTER = 3_000L

        /** События с добычей, отправляемые в миг, когда случились в бою вручную. */
        val LOOT = setOf(RunEventKind.KILL, RunEventKind.CHEST)

        /** Сколько событие с добычей ждёт остаток своего залпа: убийства одного кадра - один запрос. */
        const val PROMPT_AFTER = 120L

        /** Первый повтор после неудачной отправки и сколько раз пауза удваивается до часовой. */
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 3
        const val SAVE_AFTER = 800L
    }
}
