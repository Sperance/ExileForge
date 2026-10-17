package com.sperance.exileforge.presentation.crafts

import com.sperance.exileforge.core.crafts.Absence
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.crafts.Harvest
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.freshness
import com.sperance.exileforge.core.world.ContentLoader
import com.sperance.exileforge.presentation.app.Foreground
import com.sperance.exileforge.rules.roll.ActiveWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ремёсла. Работа идёт на сервере по времени, кости цикла - у него: полоса цикла идёт здесь по кругу. С
 * сбор ленивый: сервер досчитывает циклы при любом чтении героя, а на границе цикла спрашивается лишь при открытом
 * экране ремёсел - один запрос, сбор сразу со снимком героя. Действия общие для экрана, прогрева и возврата.
 */
class CraftsActions(
    private val repository: CraftsRepository,
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val events: GameEvents,
    private val content: ContentLoader,
    private val buzzes: Buzzes,
    private val scope: CoroutineScope,
    private val foreground: Foreground,
) {
    private val api: GameApi get() = connection.api
    private val crafts: Crafts get() = repository.state.value

    /**
     * Игрок отсутствовал: запуск, вход в героя или уход игры с переднего плана; первый ответ после этого может стать листом «Пока вас не
     * было» ([Absence]).
     */
    @Volatile private var absent = true

    init {
        scope.launch { foreground.inFront.collect { front -> if (!front) absent = true } }
    }

    private fun crafts(transform: (Crafts) -> Crafts) = repository.update(transform)

    /** Досчёт ремёсел; [fresh] - экран открылся и примет ответ моложе срока свежести (`freshness`) без нового запроса. */
    fun load(silent: Boolean = false, fresh: Boolean = false) = commands.read(Reads.CRAFTS, silent = silent, tag = heroes.heroId, maxAgeMs = freshness(fresh)) {
        val id = heroes.heroId
        if (id.isBlank()) return@read
        content.ensure()
        land(id, api.crafts.state(id))
    }

    private var cycle: Job? = null

    /** Экран ремёсел открыт: только тогда будильник границы цикла стоит. */
    private var watching = false

    /** Экран ремёсел открылся [on] или закрылся: будильник ставится или снимается. */
    fun watch(on: Boolean) {
        watching = on
        armCycle()
    }

    /** Граница цикла на часах сервера, под которую уже стоит пересчёт: один запрос на границу. */
    private var armedAt = 0L

    /**
     * Будильник границы цикла - только при открытом экране ремёсел: вне его сбор приходит с любым чтением героя.
     * На каждой границе - один тихий пересчёт с сервера, без спиннера; граница считается по кругу от `settledAt`
     * работы, так что ответ, ещё не сдвинувший работу, ставит будильник на следующую границу, а не повторяет запрос.
     *
     * Сервер считает цикл по своим часам: пересчёт - чуть позже границы. В фоне пересчёт не спрашивается: пройденная в фоне граница
     * пересчитывается по возврату.
     */
    private fun armCycle() {
        val held = crafts
        val work = held.state?.work?.takeIf { it.cycleMillis > 0 && watching }
        if (work == null) {
            cycle?.cancel()
            cycle = null
            armedAt = 0L
            return
        }
        val boundary = work.nextBoundary(System.currentTimeMillis() + held.offset)
        if (boundary == armedAt && cycle?.isActive == true) return
        cycle?.cancel()
        armedAt = boundary
        cycle = scope.launch {
            delay((boundary - held.offset - System.currentTimeMillis()).coerceAtLeast(0L) + SETTLE_GRACE)
            foreground.awaitFront()
            cycle = null
            load(silent = true)
            armCycle()
        }
    }

    /** Герой уходит с экрана: будильник снят, ремёсла забыты. */
    fun drop() {
        cycle?.cancel()
        cycle = null
        armedAt = 0L
        absent = true
        repository.clear()
    }

    /** Лист «Пока вас не было» закрыт. */
    fun seen() = repository.seen()

    /** Новая работа или та же с другим выбором. */
    fun start(job: String, choice: String = "", additives: List<String> = emptyList()) {
        buzzes.buzz(Buzz.BUTTON)
        commands.task(writing = true, touches = setOf(Reads.CRAFTS)) {
            val id = heroes.heroId
            land(id, api.crafts.start(id, job, choice, additives))
        }
    }

    fun stop() = commands.task(writing = true, touches = setOf(Reads.CRAFTS)) {
        val id = heroes.heroId
        land(id, api.crafts.stop(id))
    }

    /** Инструмент в слот профессии: экипировка сервером, и ремёсла прочитаны заново ради новых чисел. */
    fun equipTool(itemId: String) = commands.task(writing = true, touches = setOf(Reads.CRAFTS, Reads.HERO)) {
        val id = heroes.heroId
        api.hero.equip(id, itemId, null)
        land(id, api.crafts.state(id))
    }

    /**
     * Ответ сервера: работа как он её пересчитал; сбор с циклами - [Crafts.last] для
     * всплывашки у полосы цикла. Первый ответ после отсутствия игрока, покрывший отлучку, копится в [Crafts.away] до показа листа.
     * Сумка приходит снимком героя в том же ответе; не пришёл - герой перечитывается.
     */
    private fun land(id: String, answer: CraftsState) {
        if (!heroes.onScreen(id)) return
        val gains = answer.gains
        val returned = absent
        absent = false
        crafts { c ->
            val before = c.state?.work?.let { ActiveWork(it.profession, it.job, it.startedAt, it.settledAt, choice = it.choice) } ?: heroes.state.value.hero?.crafts?.work
            val away = Absence.of(answer, before)?.takeIf { returned }
            c.copy(
                state = answer,
                readAt = System.currentTimeMillis(),
                last = if (gains.cycles > 0) Harvest(gains, (c.last?.seq ?: 0L) + 1) else c.last,
                away = away?.let { c.away?.plus(it) ?: it } ?: c.away,
            )
        }
        events.heroChanged()
        armCycle()
    }

    private companion object {
        const val SETTLE_GRACE = 600L
    }
}
