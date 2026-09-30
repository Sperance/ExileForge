package com.sperance.exileforge.core.model.command

import com.sperance.exileforge.core.i18n.ui
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
@Serializable data class LoginCredentials(val login: String, val password: String)
@Serializable data class DeviceCredentials(val deviceId: String)
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
            "GET" to "/api/v1/hero/byUser", "GET" to "/api/v1/hero/view", "POST" to "/api/v1/hero",
            "POST" to "/api/v1/hero/equip", "POST" to "/api/v1/hero/orb", "POST" to "/api/v1/hero/sell",
            "GET" to "/api/v1/hero/bench", "POST" to "/api/v1/hero/craft",
            "GET" to "/api/v1/hero/skilltree/state", "POST" to "/api/v1/hero/skilltree/allocate",
            "POST" to "/api/v1/hero/skilltree/path",
            "GET" to "/api/v1/hero/atlas/state", "POST" to "/api/v1/hero/skills/learn",
            "GET" to "/api/v1/hero/crafts", "POST" to "/api/v1/hero/crafts/start",
            "GET" to "/api/v1/hero/merchant", "POST" to "/api/v1/hero/merchant/buy", "POST" to "/api/v1/hero/merchant/buyOrb",
            "GET" to "/api/v1/hero/campaign/progress", "POST" to "/api/v1/hero/campaign/start", "POST" to "/api/v1/hero/campaign/events",
            "POST" to "/api/v1/hero/trials/rush", "POST" to "/api/v1/hero/trials/tower", "POST" to "/api/v1/hero/trials/events",
            "GET" to "/api/v1/auctionlot/search", "POST" to "/api/v1/auctionlot/sell/equipment", "POST" to "/api/v1/auctionlot/buy",
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

/** A bug report (server 1.46.0): the words of the player, where they were, and the tail of the request journal. */
@Serializable data class BugReportRequest(val text: String, val screen: String, val context: Map<String, String>, val requests: List<String>)
