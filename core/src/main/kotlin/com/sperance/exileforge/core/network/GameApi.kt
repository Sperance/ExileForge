package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.ApiCapabilities
import com.sperance.exileforge.core.model.command.BugReportRequest
import com.sperance.exileforge.core.model.command.ClientIdentity
import com.sperance.exileforge.core.model.command.DeviceCredentials
import com.sperance.exileforge.core.model.command.LoginCredentials
import com.sperance.exileforge.core.model.command.PasswordChange
import com.sperance.exileforge.core.model.command.SignedIn
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.core.model.sync.StaticManifest
import com.sperance.exileforge.rules.content.RULES_VERSION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.OkHttpClient

/** "No account for this device yet" — the server's way of saying "register it". */
private const val REVISION_RETRIES = 2
private const val REVISION_RETRY_DELAY_MS = 1_500L

/** Сколько ответ манифеста общий для всех, кто его спрашивает (3.94.1): волна старта - один запрос. */
private const val SHARED_MS = 10_000L
private const val MANIFEST = "static/index.json"
private const val DEVICE_UNKNOWN = "US_015"

/** Ответ лимита запросов (3.94.0). */
private const val TOO_MANY = 429

/** Паузы перед повтором входа, упёршегося в лимит, мс (3.94.0). */
private val RATE_WAITS = listOf(2_000L, 5_000L, 12_000L)

/**
 * The client of one ktor-bestgame server: the session here, every other route in a feature client.
 *
 * A sign-in answers the account *and* a token, and every other request carries that token as
 * `Authorization: Bearer`. The token lives in [Transport] and is the session; [account] is who it
 * belongs to, and [logout] drops both. The routes are grouped by what they are about — [files],
 * [hero], [campaign], [tree], [atlas], [auction], [merchant], [crafts], [guild], [quests], [promo].
 */
class GameApi(
    server: String,
    journal: RequestJournal = RequestJournal(),
    client: OkHttpClient = ForgeHttp.client,
    private val onUnauthorized: () -> Unit = {},
) {
    private val http = Transport(server, journal, client) {
        account = null
        onUnauthorized()
    }
    private var account: UserProfile? = null

    init {
        http.account = { account?.id }
    }

    val files = StaticClient(http)
    val hero = HeroClient(http)
    val tree = TreeClient(http)
    val atlas = AtlasClient(http)
    val auction = AuctionClient(http)
    val promo = PromoClient(http)
    val campaign = CampaignClient(http)
    val trials = TrialClient(http)
    val merchant = MerchantClient(http)
    val crafts = CraftsClient(http)
    val guild = GuildClient(http)
    val quests = QuestClient(http)
    val admin = AdminClient(http)
    val moderation = ModerationClient(http)
    val feedback = FeedbackClient(http)
    val mail = MailClient(http)

    /** Credentials travel in the body: a query string settles in every proxy log on the way. */
    suspend fun login(login: String, password: String): UserProfile {
        logout()
        // Логин без регистра на сервере (1.80.0); пробелы по краям - опечатка, а не часть логина.
        val name = login.trim()
        require(name.isNotBlank() && password.isNotEmpty()) { ui("api.credentials") }
        val device = identity
        val credentials = LoginCredentials(name, password, device?.fingerprint().orEmpty(), device?.model.orEmpty(), device?.version.orEmpty())
        return signedIn(http.request("POST", "api/v1/user/login", body = WireJson.encodeToJsonElement(credentials), sensitive = true))
    }

    /**
     * Гостевой аккаунт (вход по устройству) получает логин и пароль (server 1.80.0): ответ - аккаунт, как `/me`; вход по
     * устройству остаётся. Отказы: `US_017` логин уже есть, `US_016`/`US_019` длина, `US_018` символы, `US_006` занят.
     */
    suspend fun bind(login: String, password: String): UserProfile {
        val answer = http.request("POST", "api/v1/user/bind", body = WireJson.encodeToJsonElement(LoginCredentials(login.trim(), password)), authenticated = true, sensitive = true)
        return WireJson.decodeFromJsonElement(UserProfile.serializer(), answer).also { account = it }
    }

    /**
     * Sign in with the secret of the device (server 1.46.0), or register when there is none or the server no longer
     * knows it (`US_015`): the server issues a fresh secret, and [deviceSecret] holds it until the caller keeps it.
     * The [fingerprint] lets the server hand back the account this device already has after its data was wiped.
     */
    suspend fun loginByDevice(secret: String?, fingerprint: String = ""): UserProfile = patiently { deviceSignIn(secret, fingerprint) }

    /**
     * Вход, переживший лимит (3.94.0): сервер ответил 429 - ждём и пробуем снова, всё дольше, не больше [RATE_WAITS] раз;
     * игрок не видит «слишком много запросов» от того, что за одним адресом вошли сразу несколько устройств.
     */
    private suspend fun <T> patiently(block: suspend () -> T): T {
        RATE_WAITS.forEach { wait ->
            try {
                return block()
            } catch (e: ApiFailure) {
                if (e.status != TOO_MANY) throw e
                kotlinx.coroutines.delay(wait)
            }
        }
        return block()
    }

    private suspend fun deviceSignIn(secret: String?, fingerprint: String): UserProfile {
        logout()
        fun credentials(deviceId: String) = DeviceCredentials(deviceId, fingerprint, identity?.model.orEmpty(), identity?.version.orEmpty())
        val answer = secret?.let {
            try {
                http.request("POST", "api/v1/user/login/byDeviceId", body = WireJson.encodeToJsonElement(credentials(it)), sensitive = true)
            } catch (e: ApiFailure) {
                if (e.code == DEVICE_UNKNOWN) null else throw e
            }
        } ?: http.request("POST", "api/v1/user/byDeviceId", body = WireJson.encodeToJsonElement(credentials("")), sensitive = true)
        return signedIn(answer)
    }

    /** Кто шлёт отчёты (3.88.0): устройство, версия и герой; null - отчёт уходит как собран. */
    var identity: ClientIdentity? = null

    /**
     * Files a bug report (server 1.46.0): open before the sign-in too, signed when there is a session. Подписан
     * [identity] (3.88.0): устройство, версия клиента и активный герой.
     */
    suspend fun reportBug(report: BugReportRequest) {
        val stamped = identity?.stamp(report) ?: report
        http.request("POST", "api/v1/bugreport", body = WireJson.encodeToJsonElement(stamped), authenticated = http.token != null)
    }

    /** The secret a device registration just brought, or null: read once and kept by the app. */
    var deviceSecret: String? = null
        private set

    private fun signedIn(answer: JsonElement): UserProfile {
        val session = WireJson.decodeFromJsonElement<SignedIn>(answer)
        deviceSecret = session.deviceSecret
        requireId(session.user.id)
        require(session.token.isNotBlank()) { ui("api.no_token") }
        require(session.user.isActive) { ui("api.account_disabled") }
        http.token = session.token
        http.holding = false
        account = session.user
        return session.user
    }

    /** Comes back to a session kept from an earlier launch; a token the server no longer knows answers 401. */
    suspend fun resume(saved: String): UserProfile {
        logout()
        require(saved.isNotBlank()) { ui("api.no_token") }
        http.token = saved
        return try {
            refreshUser().also { require(it.isActive) { ui("api.account_disabled") } }
        } catch (e: Exception) {
            logout()
            throw e
        }
    }

    /**
     * Takes a kept token as the session before the server has confirmed it (3.30.0): the fast start draws
     * the last hero from the device, and its commands line up in the queue until [resume] answers.
     */
    fun adopt(saved: String, profile: UserProfile) {
        require(saved.isNotBlank()) { ui("api.no_token") }
        http.token = saved
        http.holding = true
        account = profile
    }

    /** The adopted session, asked of the server: the account as it is now, and the commands free to go. A 401 drops it. */
    suspend fun confirm(): UserProfile = refreshUser().also {
        require(it.isActive) { ui("api.account_disabled") }
        http.holding = false
    }

    fun logout() {
        account = null
        http.token = null
        http.holding = false
    }
    fun currentUser(): UserProfile? = account
    fun sessionToken(): String? = http.token

    /** Ends one session on the server: only the echo of a local sign-out, nothing waits for it. */
    suspend fun revoke(saved: String) {
        try {
            http.request("POST", "api/v1/user/logout", bearer = saved)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}
    }

    suspend fun refreshUser(): UserProfile {
        val profile: UserProfile = http.get("api/v1/user/me")
        requireId(profile.id)
        account = profile
        return profile
    }

    suspend fun changePassword(current: String, replacement: String) {
        http.request(
            "POST",
            "api/v1/user/changePassword",
            body = WireJson.encodeToJsonElement(PasswordChange(current, replacement)),
            authenticated = true,
            sensitive = true,
        )
    }

    private val manifestLock = Mutex()
    private var manifest: StaticManifest? = null

    /** Последний ответ сервера на манифест (3.94.1): текст, его ETag и когда спрошен - один запрос на волну старта. */
    private var served: Served? = null

    private class Served(val text: String, val etag: String?, val at: Long) {
        val manifest: StaticManifest by lazy { WireJson.decodeFromString(StaticManifest.serializer(), text) }
    }

    /**
     * Манифест с сервера (3.94.1): спрошенный не раньше [maxAgeMs] назад отдаётся из памяти - старт, вход и проверка
     * связи берут один ответ; иначе запрос с `If-None-Match`, и на 304 тело не качается.
     */
    private suspend fun ask(maxAgeMs: Long = SHARED_MS): Served {
        val now = System.currentTimeMillis()
        served?.takeIf { now - it.at <= maxAgeMs }?.let { return it }
        val known = served
        val answer = http.fetchTagged(MANIFEST, known?.etag)
        return (if (answer.status == 304 && known != null) Served(known.text, known.etag, now) else Served(answer.body, answer.etag, now)).also { served = it }
    }

    /** Where the last manifest this server served is kept on the device (3.1.0); none keeps nothing. */
    var manifestCache: ManifestCache? = null

    /**
     * `static/index.json`, read once for this server; [fresh] asks again. Out of reach, the manifest the
     * device kept stands in (3.1.0): the content chunks it names are on the device too, so a cold start
     * without the network still has its world. A refusal of the server is not "out of reach" and is thrown.
     */
    suspend fun manifest(fresh: Boolean = false): StaticManifest = manifestLock.withLock {
        manifest?.takeIf { !fresh } ?: try {
            ask().also { manifestCache?.write(it.text) }.manifest
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiFailure) {
            throw e
        } catch (e: Exception) {
            manifest ?: manifestCache?.read()?.let { runCatching { WireJson.decodeFromString(StaticManifest.serializer(), it) }.getOrNull() } ?: throw e
        }.also { manifest = it }
    }

    /**
     * The manifest the sign-in checks against: always asked of the server, never the device's copy. Another
     * revision may be a deploy midway, so it is asked [REVISION_RETRIES] more times before the client refuses it.
     */
    suspend fun workbench(): StaticManifest {
        repeat(REVISION_RETRIES) { attempt ->
            served(if (attempt == 0) SHARED_MS else 0L).takeIf { it.matchesClient }?.let {
                it.requireWorkbench()
                return it
            }
            delay(REVISION_RETRY_DELAY_MS)
        }
        return served(0L).also {
            if (it.revision > API_REVISION || it.rules > RULES_VERSION) onNewerServer()
            it.requireWorkbench()
        }
    }

    /** The server speaks a newer wire than this build (3.74.0): the app looks for the build that speaks it at once. */
    var onNewerServer: () -> Unit = {}

    /**
     * Доступ закрыт санкцией (`AUTH_006`, 3.88.5, server 1.80.8): бан аккаунта или устройства, удаление аккаунта. Id санкции -
     * для экрана санкции; пустой - аккаунт удалён и уже стирается.
     */
    var onSanctioned: (sanctionId: String) -> Unit
        get() = http.onSanctioned
        set(value) {
            http.onSanctioned = value
        }

    /** Сервер и клиент друг друга не поняли (3.94.1): приложение проверяет обновление. */
    var onConfused: () -> Unit
        get() = http.onConfused
        set(value) {
            http.onConfused = value
        }

    /** Герой запроса под санкцией (`CH_034`, 3.88.0; id санкции - 3.88.5): приложение уводит к выбору героя. */
    var onHeroBlocked: (heroId: String, failure: ApiFailure) -> Unit
        get() = http.onHeroBlocked
        set(value) {
            http.onHeroBlocked = value
        }

    /**
     * `static/index.json` as served right now (3.74.0): never the device's copy - what the update check compares against. С 3.94.1
     * ответ последних секунд общий, а без перемен сервер отвечает 304.
     */
    suspend fun liveManifest(): StaticManifest = manifestLock.withLock { ask().manifest }

    /** The manifest as served now; kept, on the device and in memory, only when it is this client's revision — a deploy midway is not. */
    private suspend fun served(maxAgeMs: Long): StaticManifest = manifestLock.withLock {
        val answer = ask(maxAgeMs)
        answer.manifest.also {
            if (it.matchesClient) {
                manifestCache?.write(answer.text)
                manifest = it
            }
        }
    }

    /** The commands waiting for this server (3.30.0); none until the app gives them a place on the device. */
    val commands: CommandQueue? get() = http.queue

    fun commandStore(store: CommandStore) {
        http.queue = CommandQueue(store)
    }

    /**
     * Sends what waits, in order, each with its own key. A refusal drops that command and goes on to the
     * next — [refused] says it; "not yet" (the network, a duplicate still running) stops the pass and keeps
     * the rest; [delivered] names each command the server has answered. Another account's commands are dropped, and [foreign] names each.
     */
    suspend fun flushCommands(
        expired: (List<QueuedCommand>) -> Unit,
        refused: (QueuedCommand, ApiFailure) -> Unit,
        delivered: (QueuedCommand) -> Unit,
        foreign: (QueuedCommand) -> Unit = {},
    ): FlushOutcome {
        val queue = http.queue ?: return FlushOutcome.EMPTY
        while (true) {
            // No session, or one the server has not confirmed yet: the commands wait for it.
            if (http.token == null || http.holding) return FlushOutcome.SIGNED_OUT
            val next = queue.head(expired) ?: return FlushOutcome.EMPTY
            val owner = account?.id
            if (next.account != null && owner != null && next.account != owner) {
                queue.remove(next.key)
                foreign(next)
                continue
            }
            try {
                http.replay(next)
                queue.remove(next.key)
                delivered(next)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiFailure) {
                when {
                    e.status == 401 -> return FlushOutcome.SIGNED_OUT

                    CommandQueue.transient(e.status) -> return FlushOutcome.OFFLINE

                    else -> {
                        queue.remove(next.key)
                        refused(next, e)
                    }
                }
            } catch (_: java.io.IOException) {
                return FlushOutcome.OFFLINE
            }
        }
    }

    /** Where commands deliver the hero: [parts] names the fingerprints held for a hero, [apply] receives the snapshot or `null`. */
    fun heroSync(parts: (String) -> String?, apply: (String, HeroSnapshot?) -> Unit) {
        http.heroParts = parts
        http.onHero = apply
    }
    suspend fun health(): JsonElement = http.request("GET", "system/health")

    /** Когда сервер последний раз ответил на запрос API (3.94.1). */
    val answeredAt: Long get() = http.answeredAt
}
