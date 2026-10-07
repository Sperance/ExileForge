package com.sperance.exileforge.core.model.command

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import kotlinx.serialization.Serializable

/** The showcase is paged by the server, so this is what the client asks it for. */
const val AUCTION_PAGE_SIZE = 20

/** The document that makes a hero: the rest the server defaults. */
@Serializable data class CreateHeroCommand(val userId: String, val name: String, val description: String = "", val heroClass: String)

/** The signed-in account. */
@Serializable data class UserProfile(
    val id: String,
    val version: Long = 0,
    val name: String = "",
    val login: String = "",
    val role: String = "USER",
    val isActive: Boolean = true,
    val countCharacters: Int = 0,
)

/** What every sign-in answers: the account and the token that stands for it. */

/** The answer to a sign-in; a device registration (server 1.46.0) also brings the secret of the device, once. */
@Serializable data class SignedIn(val user: UserProfile, val token: String, val deviceSecret: String? = null)

/** Вход по логину (3.88.5: с устройством и версией клиента - сервер ведёт устройства аккаунта и баны устройств). */
@Serializable data class LoginCredentials(val login: String, val password: String, val hardwareId: String = "", val model: String = "", val clientVersion: String = "")

@Serializable data class DeviceCredentials(val deviceId: String, val hardwareId: String = "", val model: String = "", val clientVersion: String = "")

@Serializable data class PasswordChange(val password: String, val newPassword: String)

/** One entry of the server's route table; the method arrives as Ktor prints it — `(GET)` — so it is normalised to letters. */
@Serializable data class RouteInfo(val path: String, val method: String) {
    val verb: String get() = method.uppercase().filter { it.isLetter() }
}

/** What the connected server supports, read from its own route table rather than guessed. */
data class ApiCapabilities(val routes: Set<String>) {
    fun has(method: String, path: String) = "$method $path" in routes

    /** Everything the client calls; a missing route means a stale server, named before any screen fails inside. */
    fun requireWorkbench() {
        val missing = REQUIRED.filterNot { (method, path) -> has(method, path) }
        require(missing.isEmpty()) { ui("cmd.stale_server", missing.joinToString { "${it.first} ${it.second}" }) }
    }

    companion object {
        val REQUIRED = listOf(
            "POST" to "/api/v1/user/login", "POST" to "/api/v1/user/login/byDeviceId", "POST" to "/api/v1/user/byDeviceId",
            "GET" to "/api/v1/user/me", "POST" to "/api/v1/user/logout",
            "GET" to "/api/v1/hero/byUser", "GET" to "/api/v1/notice", "GET" to "/api/v1/hero/view", "POST" to "/api/v1/hero",
            "POST" to "/api/v1/hero/equip", "POST" to "/api/v1/hero/orb", "POST" to "/api/v1/hero/sell",
            "POST" to "/api/v1/hero/craft", "POST" to "/api/v1/hero/skilltree/allocate",
            "POST" to "/api/v1/hero/skilltree/refundBranch",
            "POST" to "/api/v1/hero/atlas/allocate", "POST" to "/api/v1/hero/skills/learn",
            "GET" to "/api/v1/hero/crafts", "POST" to "/api/v1/hero/crafts/start",
            "POST" to "/api/v1/hero/merchant/buy", "POST" to "/api/v1/hero/merchant/buyOrb",
            "POST" to "/api/v1/hero/campaign/start", "POST" to "/api/v1/hero/campaign/events",
            "POST" to "/api/v1/hero/campaign/abandon",
            "POST" to "/api/v1/hero/trials/rush", "POST" to "/api/v1/hero/trials/tower", "POST" to "/api/v1/hero/trials/key", "GET" to "/api/v1/hero/stats", "GET" to "/api/v1/hero/uniques", "POST" to "/api/v1/hero/trials/events",
            "GET" to "/api/v1/auctionlot/search", "GET" to "/api/v1/auctionlot/mine", "GET" to "/portraits/all.json", "POST" to "/api/v1/auctionlot/sell/equipment", "POST" to "/api/v1/auctionlot/buy",
            "GET" to "/api/v1/guild/mine", "GET" to "/api/v1/guild/search", "POST" to "/api/v1/guild/create", "POST" to "/api/v1/guild/join",
            "POST" to "/api/v1/guild/apply", "POST" to "/api/v1/guild/applications/accept", "POST" to "/api/v1/guild/applications/decline",
            "POST" to "/api/v1/guild/invite", "POST" to "/api/v1/guild/invites/accept", "POST" to "/api/v1/guild/invites/decline",
            "POST" to "/api/v1/guild/leave", "POST" to "/api/v1/guild/kick", "POST" to "/api/v1/guild/promote", "POST" to "/api/v1/guild/demote",
            "POST" to "/api/v1/guild/transfer", "POST" to "/api/v1/guild/disband", "POST" to "/api/v1/guild/settings",
            "POST" to "/api/v1/guild/contribute", "GET" to "/api/v1/guild/log",
            "POST" to "/api/v1/redemptioncodes/redeem",
            "GET" to "/content/{file}", "GET" to "/static/index.json",
        )

        fun of(routes: List<RouteInfo>) = ApiCapabilities(routes.mapTo(HashSet()) { "${it.verb} ${it.path}" })
    }
}

/** A bug report (server 1.46.0): the words of the player and where they were; no request journal (3.91.0) - nobody reads it. */
@Serializable data class BugReportRequest(
    val text: String,
    val screen: String,
    val context: Map<String, String>,
    /** A bug or a player's suggestion (3.73.0, server 1.69.0). */
    val kind: FeedbackKind = FeedbackKind.BUG,
    /** Герой отправителя, отпечаток устройства и версия клиента (3.88.0, server 1.80.0): ставит [ClientIdentity]. */
    val heroId: String? = null,
    val device: String = "",
    val clientVersion: String = "",
    /** Служебный журнал (3.91.1, server 1.81.11): запросы, ошибки и зависания; сервер отдаёт его только вложением в Asana. */
    val service: String = "",
)

/**
 * Кто шлёт отчёт (3.88.0): версия сборки и отпечаток устройства от приложения, активный герой - если он есть. Каждый
 * отчёт ([com.sperance.exileforge.core.network.GameApi.reportBug]) подписывается им, кто бы его ни собрал.
 */
class ClientIdentity(val version: String, private val device: () -> String, private val heroId: () -> String?, val model: String = "") {
    /** Отпечаток устройства; не прочитался - пусто. */
    fun fingerprint(): String = runCatching(device).getOrDefault("")

    fun stamp(report: BugReportRequest): BugReportRequest = report.copy(
        heroId = report.heroId ?: heroId()?.takeIf { it.isNotBlank() },
        device = report.device.ifBlank { runCatching(device).getOrDefault("") },
        clientVersion = report.clientVersion.ifBlank { version },
    )
}
