package com.sperance.exileforge.core.load

import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.ForgeHttp
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.HttpLink
import com.sperance.exileforge.core.sim.Walkthrough
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.HallBoard
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Нагрузочный прогон аудита (не тест, не CI): виртуальные игроки на настоящем клиенте [GameApi] и настоящем
 * заходе ядра ([Walkthrough]) против ЛОКАЛЬНОГО сервера. Каждый игрок - свой аккаунт по устройству (адрес - свой
 * `X-Forwarded-For`, сервер запускается с `TRUSTED_PROXIES=127.0.0.1`), Предначертание, герой, затем цикл
 * действий профиля. Итог - задержки по маршрутам (p50/p95/p99), ошибки по коду, отклонённые события журнала.
 *
 * Запуск: `./gradlew :core:loadRun -Purl=http://localhost:8081/ -Pplayers=50 -Pseconds=120 -Pprofile=mixed -Pramp=30`
 */
private val stats = ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>>()
private val codes = ConcurrentHashMap<String, AtomicInteger>()
private val rejected = AtomicLong()
private val rejectedKinds = ConcurrentHashMap<String, AtomicInteger>()
private val applied = AtomicLong()
private val runsDone = AtomicLong()
private val bytesIn = AtomicLong()
private val startedAt = System.currentTimeMillis()

/** Шкала по 10 с от старта: ответы 2xx-3xx и прочие (4xx/5xx/обрыв) - видно провал и восстановление. */
private val timeline = ConcurrentHashMap<Long, Pair<AtomicInteger, AtomicInteger>>()

private fun tick(ok: Boolean) = timeline.computeIfAbsent((System.currentTimeMillis() - startedAt) / 10_000) { AtomicInteger() to AtomicInteger() }.let { (good, bad) -> (if (ok) good else bad).incrementAndGet() }

private fun route(path: String) = path.replace(Regex("/[0-9a-f]{24}"), "/{id}")

private fun metrics(ip: String) = Interceptor { chain ->
    val request = chain.request().newBuilder().header("X-Forwarded-For", ip).build()
    val key = "${request.method} ${route(request.url.encodedPath)}"
    val t0 = System.nanoTime()
    try {
        val response = chain.proceed(request)
        val ms = (System.nanoTime() - t0) / 1_000_000
        stats.computeIfAbsent(key) { ConcurrentLinkedQueue() }.add(ms)
        response.body.contentLength().takeIf { it > 0 }?.let(bytesIn::addAndGet)
        codes.computeIfAbsent("$key ${response.code}") { AtomicInteger() }.incrementAndGet()
        tick(response.code < 400)
        response
    } catch (e: Exception) {
        codes.computeIfAbsent("$key IO:${e.javaClass.simpleName}") { AtomicInteger() }.incrementAndGet()
        tick(false)
        throw e
    }
}

private class Player(val n: Int, val url: String, val index: ContentIndex, val profile: String, val fightSpeed: Int) {
    val api = GameApi(url, link = HttpLink(ForgeHttp.client.newBuilder().addInterceptor(metrics("10.${n / 65536 % 256}.${n / 256 % 256}.${n % 256}")).build()))
    lateinit var heroId: String
    var parts: HeroParts? = null
    val rnd = Random(n)
    val auctionLevel = index.rules.unlocks["AUCTION"] ?: Int.MAX_VALUE

    suspend fun enter() {
        val user = api.loginByDevice(null, "load-$n-${UUID.randomUUID()}")
        val fate = api.fate.view()
        if (fate.chosen == null) api.fate.choose(fate.offers.first().code)
        val heroClass = index.classes.classes[n % index.classes.classes.size].code
        heroId = api.hero.create(user.id, "Load" + letters(n) + letters(RUN_TAG), "", heroClass).id
        view()
    }

    suspend fun view() {
        val held = parts ?: HeroParts(heroId)
        parts = api.hero.view(heroId, held)?.let { held.merge(it) } ?: held
    }

    /**
     * Заход как у клиента: проход без игрока ([Walkthrough]), журнал пачками по 6 событий или на контрольной точке, темп - реальное время боя / [speedup].
     *
     * Окна сундуков и кристаллов - как их отдал сервер; возобновлённый заход - как у клиента. Скорость боя - кнопкой, как у игрока, ходьба
     * и время - реальные. Временный отказ (503, 409) и обрыв - повтор той же пачкой и ключом; журнал шлётся по мере боя, время сжато в
     * speedup раз.
     */
    suspend fun run(speedup: Double) {
        val p = parts ?: return
        val zoneCode = index.world.unlocked(p.campaign.cleared).let { open -> open.getOrNull(rnd.nextInt(open.size)) } ?: return
        val start = api.campaign.start(heroId, zoneCode)
        val zone = index.zone(zoneCode) ?: return
        val hero = p.hero
        val sheet = Sheets.calculate(index, hero.level, hero.heroClass, p.tree, p.items)
        val skill = index.skills.own(hero.heroClass).firstOrNull { it.type.kind == SkillKind.ACTIVE && it.unlock <= hero.level }
        val skills = HeroSkills(learned = skill?.let { mapOf(it.code to 1) }.orEmpty(), active = listOf(skill?.let { ActiveSlot(it.code, SlotCondition.READY) }))
        val gear = HeroGear(sheet.stats, hero.level, sheet.model, HeroStance.of(hero.heroClass), Loadout.of(skills, index, hero.heroClass, emptyList(), sheet.stats, index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, zone, start.seed, start.context)
        val journal = RunJournal(start.id, heroId, zone.code.value, applied = start.applied, base = start.applied)
        val campaign = p.campaign
        val expedition = ExpeditionRun.start(
            index, zone, run, journal, gear, campaign, 0L, 0.0, hero.level, { 0L },
            killed = start.killed, opened = start.chests, features = start.features,
        )
        generateSequence(1) { index.rules.fight.next(it).takeIf { next -> next > it } }.takeWhile { it < fightSpeed }.forEach { _ -> expedition.send(RunCommand.Speed) }
        var guard = 0
        var sentAt = 0
        var gameSeconds = 0.0
        var lastFlush = 0.0
        suspend fun flush() {
            while (true) {
                val (batch, events) = journal.outgoing { UUID.randomUUID().toString() } ?: return
                val report = try {
                    api.campaign.events(heroId, start.id, events, batch.key)
                } catch (e: ApiFailure) {
                    if (e.code in setOf("CP_018", "CP_020", "CP_026")) return
                    if ((e.status ?: 0) >= 500 || e.status == 409) {
                        delay(2_000)
                        continue
                    }
                    journal.release()
                    throw e
                } catch (e: java.io.IOException) {
                    delay(2_000)
                    continue
                }
                if (report == null) {
                    journal.confirm(batch.end)
                } else {
                    journal.confirm(report.applied, report.rejected)
                    applied.addAndGet(events.size.toLong())
                    rejected.addAndGet(report.rejected.size.toLong())
                    events.filter { it.n in report.rejected }.forEach { rejectedKinds.computeIfAbsent(it.kind.name) { AtomicInteger() }.incrementAndGet() }
                }
                sentAt = journal.all.size
            }
        }
        val walk = Walkthrough(expedition)
        while (guard++ < 200_000) {
            val going = walk.step(STEP)
            gameSeconds += STEP
            val pending = journal.all.size - sentAt
            val checkpoint = journal.all.drop(sentAt).any { it is RunEvent.Boss || it is RunEvent.Leave || it is RunEvent.Fall }
            if (pending >= 6 || checkpoint || (pending > 0 && gameSeconds - lastFlush > 20.0)) {
                delay(((gameSeconds - lastFlush) * 1000 / speedup).toLong().coerceAtMost(30_000))
                lastFlush = gameSeconds
                flush()
            }
            if (!going) break
        }
        flush()
        runsDone.incrementAndGet()
        view()
    }

    /** Покупка сферы у торговца - пишущая команда с транзакцией; отказ «мало золота» - тоже ответ сервера. */
    suspend fun browse() {
        when (rnd.nextInt(7)) {
            0 -> api.hero.stats(heroId)
            1 -> api.hall.table(heroId, HallBoard.TOWER)
            2 -> if ((parts?.hero?.level ?: 1) >= auctionLevel) api.auction.search(heroId, AuctionFilter(), listOf("")) else api.hero.stats(heroId)
            3 -> api.mail.inbox(heroId)
            4 -> api.guild.search(heroId, "", 0)
            5 -> merchantOrb?.let { api.merchant.buyOrb(heroId, it) } ?: view()
            else -> view()
        }
    }

    private val merchantOrb: String? = index.rules.merchant.orbs.codes.firstOrNull()

    suspend fun loop(until: Long, speedup: Double) {
        while (System.currentTimeMillis() < until) {
            try {
                when (profile) {
                    "idle" -> {
                        view()
                        delay(30_000)
                    }

                    "browse" -> {
                        browse()
                        delay(1_000L + rnd.nextLong(3_000))
                    }

                    "fight" -> run(speedup)

                    else -> if (rnd.nextInt(4) == 0) {
                        run(speedup)
                    } else {
                        browse()
                        delay(2_000L + rnd.nextLong(4_000))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiFailure) {
                codes.computeIfAbsent("FAIL ${e.code ?: e.status}") { AtomicInteger() }.incrementAndGet()
                delay(1_000)
            } catch (e: Exception) {
                codes.computeIfAbsent("FAIL ${e.javaClass.simpleName}") { AtomicInteger() }.incrementAndGet()
                delay(1_000)
            }
        }
    }

    companion object {
        const val STEP = 0.1
    }
}

/** Метка прогона в имени героя: ступени одной базы не спорят за имена. */
private val RUN_TAG = (System.currentTimeMillis() / 1000 % 400_000).toInt()

private fun letters(n: Int): String {
    var x = n
    return buildString {
        repeat(4) {
            append('a' + x % 26)
            x /= 26
        }
    }
}

private fun pct(sorted: List<Long>, p: Double) = if (sorted.isEmpty()) 0 else sorted[((sorted.size - 1) * p).toInt()]

fun main(args: Array<String>): Unit = runBlocking {
    val url = System.getProperty("url", "http://localhost:8081/")
    require(url.contains("localhost") || url.contains("127.0.0.1")) { "load only against a local server" }
    val players = System.getProperty("players", "20").toInt()
    val seconds = System.getProperty("seconds", "60").toLong()
    val profile = System.getProperty("profile", "mixed")
    val ramp = System.getProperty("ramp", "10").toLong()
    val speedup = System.getProperty("speedup", "1").toDouble()
    val fightSpeed = System.getProperty("fightSpeed", "1").toInt()
    val content = File(System.getProperty("content"))
    val index = ContentLoader.load { File(content, it).readText() }
    val t0 = System.currentTimeMillis()
    val until = t0 + (ramp + seconds) * 1000
    val entered = AtomicInteger()
    val enterFailed = AtomicInteger()
    val jobs = (0 until players).map { n ->
        async(kotlinx.coroutines.Dispatchers.IO) {
            delay(ramp * 1000 * n / players.coerceAtLeast(1))
            val player = Player(n, url, index, profile, fightSpeed)
            val ok = withTimeoutOrNull(60_000) {
                runCatching { player.enter() }.onFailure { codes.computeIfAbsent("ENTER ${(it as? ApiFailure)?.code ?: it.javaClass.simpleName}") { AtomicInteger() }.incrementAndGet() }.isSuccess
            } == true
            if (ok) {
                entered.incrementAndGet()
                player.loop(until, speedup)
            } else {
                enterFailed.incrementAndGet()
            }
        }
    }
    jobs.awaitAll()
    val wall = (System.currentTimeMillis() - t0) / 1000.0
    val total = stats.values.sumOf { it.size }
    val report = buildString {
        appendLine("profile=$profile players=$players entered=${entered.get()} enterFailed=${enterFailed.get()} ramp=${ramp}s hold=${seconds}s speedup=$speedup fightSpeed=$fightSpeed wall=${"%.1f".format(wall)}s")
        appendLine("requests=$total rps=${"%.1f".format(total / wall)} runs=${runsDone.get()} eventsApplied=${applied.get()} eventsRejected=${rejected.get()} bytesIn=${bytesIn.get()}")
        appendLine("rejected by kind: ${rejectedKinds.mapValues { it.value.get() }}")
        appendLine("timeline (10 s: ok/bad): " + timeline.toSortedMap().entries.joinToString(" ") { (t, v) -> "${t * 10}s=${v.first.get()}/${v.second.get()}" })
        appendLine()
        appendLine("| route | n | p50 ms | p95 ms | p99 ms | max ms |")
        appendLine("|---|---|---|---|---|---|")
        stats.entries.sortedByDescending { it.value.size }.forEach { (k, v) ->
            val s = v.sorted()
            appendLine("| $k | ${s.size} | ${pct(s, .5)} | ${pct(s, .95)} | ${pct(s, .99)} | ${s.lastOrNull() ?: 0} |")
        }
        appendLine()
        appendLine("| status | n |")
        appendLine("|---|---|")
        codes.entries.sortedBy { it.key }.forEach { (k, v) -> appendLine("| $k | ${v.get()} |") }
    }
    println(report)
    args.firstOrNull()?.let { File(it).apply { parentFile.mkdirs() }.writeText(report) }
}
