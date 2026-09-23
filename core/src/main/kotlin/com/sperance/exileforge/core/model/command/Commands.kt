package com.sperance.exileforge.core.model.command

import com.sperance.exileforge.core.i18n.ui
import kotlinx.serialization.Serializable

/** Server page bounds; the client never asks for a page the server would reject. */
const val CATALOG_PAGE_SIZE = 20
const val REFERENCE_PAGE_SIZE = 50
/** The showcase is paged by the server, so this is what the client asks it for. */
const val AUCTION_PAGE_SIZE = 20
const val MAX_ITEM_AMOUNT = 100_000_000_000L

@Serializable data class CreateCharacterCommand(val userId: String, val name: String, val description: String = "")

/** One bag change. A negative amount removes units; the server refuses to go below zero. */
@Serializable data class ItemStack(val itemId: String, val amount: Long) {
    init {
        require(amount != 0L) { ui("cmd.nonzero_amount") }
        require(kotlin.math.abs(amount) <= MAX_ITEM_AMOUNT) { ui("cmd.max_amount", MAX_ITEM_AMOUNT) }
    }
}

/** Body of `POST /api/v1/recipe/useRecipe`. The field name is the server's, typo included. */
@Serializable data class UseRecipeCommand(val ingridientsId: List<String>, val amount: Long = 1) {
    init { require(amount in 1..100) { ui("cmd.uses_range") } }
}

/** The signed-in account. This server has no token: the login response is the whole session. */
@Serializable data class UserProfile(
    val id: String,
    val version: Long = 0,
    val name: String = "",
    val login: String = "",
    val email: String = "",
    val role: String = "USER",
    val isActive: Boolean = true,
    val countCharacters: Int = 0,
)

/** What every sign-in answers since server 0.21.0: the account and the token that stands for it. */
@Serializable data class SignedIn(val user: UserProfile, val token: String)
@Serializable data class LoginCredentials(val login: String, val password: String)
@Serializable data class DeviceCredentials(val deviceId: String)
@Serializable data class PasswordChange(val password: String, val newPassword: String)

/**
 * One entry of `GET /system/routes`: how the client learns what this server can do.
 *
 * The method arrives as the Ktor selector prints it — `(GET)` — so it is normalised to letters.
 */
@Serializable data class RouteInfo(val path: String, val method: String) {
    val verb: String get() = method.uppercase().filter { it.isLetter() }
}

/**
 * What the connected server supports, read from its own route table rather than guessed.
 *
 * The server publishes every registered route, so a missing feature is a missing path and the
 * client can say which one instead of failing later inside a screen.
 */
data class ApiCapabilities(val routes: Set<String>) {
    fun has(method: String, path: String) = "$method $path" in routes
    /** Everything the catalogue, editor and hero screens call. Missing any of it means a stale server. */
    fun requireWorkbench() {
        val required = listOf(
            // Since 0.21.0 a sign-in is a POST that answers a token, and /me is how one comes back.
            "POST" to "/api/v1/user/login",
            // The player never types anything: the account is found, or made, by device.
            "POST" to "/api/v1/user/login/byDeviceId",
            "POST" to "/api/v1/user/byDeviceId",
            "GET" to "/api/v1/user/me",
            "POST" to "/api/v1/user/logout",
            "GET" to "/api/v1/character/byUser",
            "GET" to "/api/v1/equipment/paged",
            // The base of an item lives in the catalogue alone since 0.16.0, so reading it whole
            // once per session is not an optimisation any more — a card cannot be drawn without it.
            "GET" to "/api/v1/equipment",
            "GET" to "/api/v1/character/inventory/equipments",
            "GET" to "/api/v1/character/inventory/stats",
            "POST" to "/api/v1/character/inventory/itemToInventory",
            "POST" to "/api/v1/characterequipment/equip",
            "POST" to "/api/v1/characterequipment/applyOrb",
            "GET" to "/api/v1/modifierdefinition",
            "GET" to "/api/v1/characterclass",
            "GET" to "/api/v1/experiencelevel",
            "GET" to "/api/v1/skilltreenode",
            "GET" to "/api/v1/character/skilltree/state",
            "POST" to "/api/v1/character/skilltree/allocate",
            "POST" to "/api/v1/characterequipment/socket",
            "POST" to "/api/v1/characterequipment/unsocket",
            "POST" to "/api/v1/characterequipment/sell",
            // The crafting bench, since 0.23.0.
            "GET" to "/api/v1/characterequipment/bench",
            "POST" to "/api/v1/characterequipment/craft",
            "POST" to "/api/v1/characterequipment/uncraft",
            "GET" to "/api/v1/auctionlot/search",
            "POST" to "/api/v1/auctionlot/sell/equipment",
            "POST" to "/api/v1/auctionlot/buy",
        )
        val missing = required.filterNot { (method, path) -> has(method, path) }
        require(missing.isEmpty()) {
            ui("cmd.stale_server", missing.joinToString { "${it.first} ${it.second}" })
        }
    }
    companion object {
        fun of(routes: List<RouteInfo>) = ApiCapabilities(routes.mapTo(mutableSetOf()) { "${it.verb} ${it.path}" })
    }
}
