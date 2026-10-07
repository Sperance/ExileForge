package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.UUID

/** The header a command's key travels in: the server answers a repeated key with the answer it stored (server 1.28.0). */
const val IDEMPOTENCY_HEADER = "Idempotency-Key"

/**
 * One command the server has not answered yet, exactly as it would go on the wire. [key] is its
 * idempotency key: a replay is the same command, never a second one. [account] is whose session sent it,
 * when known; [heroId] and [itemId] name what it is about, so the screen can mark the thing that waits.
 */
@Serializable
data class QueuedCommand(
    val key: String,
    val method: String,
    val path: String,
    val query: Map<String, String> = emptyMap(),
    val body: String? = null,
    val account: String? = null,
    val createdAt: Long,
) {
    val heroId: String? get() = query["heroId"]
    val itemId: String? get() = query["itemId"] ?: query["petId"]
}

/** Where one server's queue is kept on the device: it survives the process, as the run's journal does. */
interface CommandStore {
    suspend fun read(): String?
    suspend fun write(text: String)
}

/** A command went into the queue instead of being answered: nothing failed, the answer comes when the server is reached. */
class CommandQueued(val command: QueuedCommand, cause: Throwable? = null) : Exception(command.path, cause)

/** How one pass over the queue ended. */
enum class FlushOutcome { EMPTY, OFFLINE, SIGNED_OUT }

/**
 * The commands of one server waiting for an answer, in the order they were given (3.30.0).
 *
 * A command joins it when the network fails under it, or when others already wait — so the server always
 * sees them in order. Each keeps the key it was first sent with, and [TTL_MS] after it was given it is
 * dropped: the server forgets the key by then, and a replay could apply it twice.
 */
class CommandQueue(private val store: CommandStore?, private val clock: () -> Long = System::currentTimeMillis) {
    private val lock = Mutex()
    private val entries = MutableStateFlow<List<QueuedCommand>>(emptyList())
    private var loaded = false

    /** What waits, oldest first. */
    val waiting: StateFlow<List<QueuedCommand>> = entries.asStateFlow()

    /** Whether anything waits — the kept queue read first, so a command after a restart still lines up behind it. */
    suspend fun busy(): Boolean = lock.withLock {
        loadLocked()
        entries.value.isNotEmpty()
    }

    /** Reads the kept queue once; returns the commands dropped as too old on the way. */
    suspend fun load(): List<QueuedCommand> = lock.withLock {
        loadLocked()
        expireLocked()
    }

    private suspend fun loadLocked() {
        if (loaded) return
        loaded = true
        val kept = store?.read()?.let { text -> runCatching { WireJson.decodeFromString(Codec, text) }.getOrNull() }.orEmpty()
        entries.update { (kept + it).distinctBy(QueuedCommand::key) }
    }

    fun command(method: String, path: String, query: Map<String, String>, body: String?, account: String?): QueuedCommand = QueuedCommand(UUID.randomUUID().toString(), method, path, query, body, account, clock())

    suspend fun add(command: QueuedCommand) = lock.withLock {
        loadLocked()
        entries.update { it.filterNot { e -> e.key == command.key } + command }
        persist()
    }

    suspend fun remove(key: String) = lock.withLock {
        entries.update { it.filterNot { e -> e.key == key } }
        persist()
    }

    /** Forgets every command — a sign-out, or another account on this server. */
    suspend fun clear() = lock.withLock {
        loaded = true
        entries.value = emptyList()
        persist()
    }

    /** The oldest command still in time, after the ones too old are dropped and handed to [expired]. */
    suspend fun head(expired: (List<QueuedCommand>) -> Unit): QueuedCommand? {
        val dropped = lock.withLock {
            loadLocked()
            expireLocked()
        }
        if (dropped.isNotEmpty()) expired(dropped)
        return entries.value.firstOrNull()
    }

    private suspend fun expireLocked(): List<QueuedCommand> {
        val now = clock()
        val (old, fresh) = entries.value.partition { now - it.createdAt > TTL_MS }
        if (old.isNotEmpty()) {
            entries.value = fresh
            persist()
        }
        return old
    }

    private suspend fun persist() {
        store?.write(WireJson.encodeToString(Codec, entries.value))
    }

    companion object {
        /** How long a key is honoured by the server (server 1.53.0: an hour), and so how long a command may wait. */
        const val TTL_MS = 60 * 60 * 1000L
        private val Codec = ListSerializer(QueuedCommand.serializer())

        /** The commands that never wait here: the session's own, and the run's journal, which keeps its own retry. */
        private val OWN_RETRY = listOf("api/v1/user/", "api/v1/hero/campaign/")

        /**
         * Commands whose outcome is rolled (3.55.0): an orb, an essence, an unveiling or a chosen line, a bench craft, a
         * hatching (an egg laid or collected), a pet's orb or chosen line, a job started. Offline they are refused rather than kept: a second press would act on an item
         * the player has not yet seen, and the whole row would land blind once the link is back.
         */
        private val ROLLED = listOf(
            "api/v1/hero/orb", "api/v1/hero/essence", "api/v1/hero/unveil", "api/v1/hero/choose", "api/v1/hero/craft",
            "api/v1/hero/pets/incubate", "api/v1/hero/pets/collect", "api/v1/hero/pets/orb", "api/v1/hero/pets/choose", "api/v1/hero/crafts/start",
            "api/v1/hero/chest/open",
        )

        /** Команды администратора (3.88.0) не ждут сети: уходят сразу или падают видимой ошибкой - блок героя не ложится вслепую. */
        private const val ADMIN = "api/v1/admin/"

        fun queues(path: String): Boolean = !path.startsWith(ADMIN) && OWN_RETRY.none { path.startsWith(it) } && ROLLED.none { path.startsWith(it) && !path.startsWith("api/v1/hero/crafts/stop") }

        /**
         * Whether an answer means "not yet" rather than "no": a duplicate still running (409), a throttle or
         * a gateway that did not reach the server. Anything else of 4xx/5xx is the server's word, and final.
         */
        fun transient(status: Int?): Boolean = status == 409 || status == 429 || status in 502..504
    }
}
