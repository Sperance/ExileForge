package com.sperance.exileforge.core.expedition

import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.receivedText
import com.sperance.exileforge.core.display.shardsPerOrb
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.hero.HeroSync
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.TrialStart
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.CommandQueue
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.rules.content.TrialEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Испытания (из `TrialViewModel`): раш по области и башня. Арена - мир, который сцена шагает; её события
 * уходят на сервер пачками под ключом, ответ ложится на арену наградами. Один на приложение.
 */
class TrialActions(
    private val expedition: ExpeditionActions,
    private val heroes: HeroRepository,
    private val heroSync: HeroSync,
    private val world: WorldRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val scope: CoroutineScope,
) {
    private val api: GameApi get() = connection.api

    private val mutableArena = MutableStateFlow<TrialArena?>(null)
    val arena: StateFlow<TrialArena?> = mutableArena.asStateFlow()
    private var owner = ""

    /** Испытание на сервере, чьи события уходят под его id. */
    private var runId = ""
    private val pending = mutableListOf<TrialEvent>()

    /** Пачка в пути под своим ключом: повторяется, пока не отвечена. */
    private var batch: Pair<String, List<TrialEvent>>? = null
    private val sends = Channel<Unit>(Channel.CONFLATED)
    private var retry: Job? = null
    private var failures = 0

    /** Запуск после сборки графа: отправки по сигналу. */
    fun start() {
        scope.launch { for (signal in sends) flush() }
    }

    /** «Раш»: ключ раша тратится на раш [region] ступени [tier] (с 0). */
    fun rush(region: String, tier: Int = 0) = enter { api.trials.rush(it, region, tier) }

    /** «Башня»: печать тратится, башня начинается с последней контрольной точки. */
    fun tower() = enter { api.trials.tower(it) }

    private fun enter(call: suspend (String) -> TrialStart) {
        if (mutableArena.value != null || commands.state.value.busy) return
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val id = heroes.heroId
            val started = call(id)
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
            heroSync.drawn()
            begin(id, started)
        }
    }

    private fun begin(id: String, started: TrialStart) {
        val index = world.state.value.content ?: return
        val hero = heroes.state.value.hero?.takeIf { it.id == id } ?: return
        val gear = expedition.gear() ?: return
        owner = id
        runId = started.run.id
        expedition.atlasReset()
        synchronized(pending) {
            pending.clear()
            batch = null
        }
        mutableArena.value = TrialArena(index, started.run, started.context, gear, hero.campaign.trials.towerPicks, onEvent = ::recorded)
    }

    /** «Завершить»: открытое на сервере испытание без арены здесь - приложение закрылось посреди - кончается, принесённое остаётся. */
    fun abandon() {
        val open = heroes.state.value.hero?.campaign?.trials?.run ?: return
        if (mutableArena.value != null) return
        commands.task(writing = true, touches = setOf(Reads.HERO)) {
            val report = api.trials.events(heroes.heroId, open.id, listOf(TrialEvent.End(open.applied)))
            report?.let { receivedText(it.received, world.state.value.content.shardsPerOrb) }?.let { notices.toast(it) }
            notices.toast(ui("trials.abandoned"))
            if (heroes.state.value.readAt == 0L) heroSync.readHero()
        }
    }

    fun send(command: RunCommand) {
        mutableArena.value?.send(command)
    }

    /** Событие арены: итоги боя - только статистика, они ждут следующего события и уходят с ним одной пачкой. */
    private fun recorded(event: TrialEvent) {
        synchronized(pending) { pending += event }
        if (event !is TrialEvent.Fight) sends.trySend(Unit)
    }

    /**
     * Отправляет пачку в пути снова или ожидающие события под новым ключом; ответ ложится на арену.
     *
     * Не больше пачки за раз: длинный журнал сервер отвергает целиком. Дошедшая без отчёта пачка взята - добыча придёт с героем. Рубеж
     * башни и первая зачистка раша - тост нового очка Атласа. Закрытое испытание или чужой журнал - ничто больше не ждёт; любой другой
     * отказ оставляет события неотправленными под новым ключом позже.
     */
    private suspend fun flush() {
        val (key, events) = synchronized(pending) {
            batch ?: pending.take(RunJournal.MAX_BATCH).takeIf { it.isNotEmpty() }?.let {
                (UUID.randomUUID().toString() to it).also { made ->
                    batch = made
                    pending.removeAll(it)
                }
            }
        } ?: return
        try {
            val report = api.trials.events(owner, runId, events, key)
            failures = 0
            synchronized(pending) { batch = null }
            if (report == null) {
                mutableArena.value?.settle(events.last().n + 1, emptyMap())
                heroSync.readHero()
                if (synchronized(pending) { pending.isNotEmpty() }) sends.trySend(Unit)
                return
            }
            mutableArena.value?.settle(report.applied, report.rewards.associate { it.n to it.reward.toReward() })
            expedition.atlasEarned(owner, report.rewards.flatMap { it.atlas })
            if (report.rejected.isNotEmpty()) notices.toast(ui("expedition.rejected", report.rejected.size))
            receivedText(report.received, world.state.value.content.shardsPerOrb)?.let { notices.toast(it) }
            if (heroes.state.value.readAt == 0L && heroes.onScreen(owner)) heroSync.readHero()
            if (synchronized(pending) { pending.isNotEmpty() }) sends.trySend(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFailure) {
            if (e.code in TRIAL_CLOSED) {
                synchronized(pending) {
                    batch = null
                    pending.clear()
                }
                mutableArena.value?.settle(Int.MAX_VALUE, emptyMap())
            } else if (!CommandQueue.transient(e.status)) {
                synchronized(pending) {
                    batch?.let { pending.addAll(0, it.second) }
                    batch = null
                }
                retryLater()
            }
            commands.report(e, writing = true)
        } catch (_: Exception) {
            retryLater()
        }
    }

    private fun retryLater() {
        failures++
        retry?.cancel()
        retry = scope.launch {
            delay((RETRY_FIRST shl (failures - 1).coerceAtMost(RETRY_DOUBLINGS)))
            sends.trySend(Unit)
        }
    }

    /**
     * Экран испытания закрыт: неотправленное уходит, герой перечитывается.
     *
     * Испытание, закрытое посреди боя, кончено на сервере уходом; принесённое остаётся.
     */
    fun close() {
        val arena = mutableArena.value ?: return
        if (arena.hud.value.phase == TrialPhase.FIGHT) arena.send(RunCommand.Leave).also { arena.update(0.0) }
        mutableArena.value = null
        sends.trySend(Unit)
        heroSync.ensure()
    }

    /** Брошено без слова: герой или его сессия ушли. */
    fun drop() {
        mutableArena.value = null
        synchronized(pending) {
            pending.clear()
            batch = null
        }
    }

    private companion object {
        const val RETRY_FIRST = 2_000L
        const val RETRY_DOUBLINGS = 3

        /** Испытание не открыто (CP_023) или журнал другого (CP_026). */
        val TRIAL_CLOSED = setOf("CP_023", "CP_026")
    }
}
