package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.creationFields
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.contract.validateModifierPool
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleLanguage
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.*
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierTier
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** "No account for this device yet" — the server's way of saying "register it". */
private const val DEVICE_UNKNOWN = "US_015"

/**
 * The client of one ktor-bestgame server: the session here, every other route in a feature client.
 *
 * Since server 0.21.0 a sign-in answers the account *and* a token, and every other request carries
 * that token as `Authorization: Bearer`. The token lives in [Transport] and is the session;
 * [account] is who it belongs to, and [logout] drops both. The token is a secret: every exchange
 * that carries one in its body is journaled as hidden.
 *
 * The routes are grouped by what they are about — [catalog], [world], [files], [hero], [tree],
 * [auction], [promo] — so a feature reads as `api.auction.buy(…)` rather than one flat list of
 * seventy calls.
 */
class GameApi(
    server: String,
    journal: RequestJournal = RequestJournal(),
    client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build(),
    private val onUnauthorized: () -> Unit = {}
) {
    private val http = Transport(server, journal, client) { account = null; onUnauthorized() }
    private var account: UserProfile? = null

    val catalog = CatalogClient(http)
    val world = WorldClient(http)
    val files = StaticClient(http)
    val hero = HeroClient(http, catalog)
    val tree = TreeClient(http)
    val auction = AuctionClient(http)
    val promo = PromoClient(http)
    val campaign = CampaignClient(http)
    val merchant = MerchantClient(http)
    val crafts = CraftsClient(http)

    /** Credentials travel in the body: a query string settles in every proxy log on the way. */
    suspend fun login(login: String, password: String): UserProfile {
        logout()
        require(login.isNotBlank() && password.isNotEmpty()) { ui("api.credentials") }
        return signedIn(http.request("POST", "api/v1/user/login", body = WireJson.encodeToJsonElement(LoginCredentials(login, password)), sensitive = true))
    }
    /**
     * Sign in with the device's own identifier, and register on the first try.
     *
     * The server keeps one account per device and answers `US_015` when it has never seen this
     * one, which is the whole registration handshake: a miss becomes a second `POST` that creates
     * the account and answers with it. The identifier is not a secret, but the token that comes
     * back is, so the exchange is journaled as hidden all the same.
     */
    suspend fun loginByDevice(deviceId: String): UserProfile {
        logout()
        require(deviceId.isNotBlank()) { ui("api.no_device") }
        val body = WireJson.encodeToJsonElement(DeviceCredentials(deviceId))
        val answer = try { http.request("POST", "api/v1/user/login/byDeviceId", body = body, sensitive = true) }
            catch (e: ApiFailure) { if (e.code == DEVICE_UNKNOWN) http.request("POST", "api/v1/user/byDeviceId", body = body, sensitive = true) else throw e }
        return signedIn(answer)
    }

    private fun signedIn(answer: JsonElement): UserProfile {
        val session = WireJson.decodeFromJsonElement<SignedIn>(answer)
        requireId(session.user.id)
        require(session.token.isNotBlank()) { ui("api.no_token") }
        require(session.user.isActive) { ui("api.account_disabled") }
        http.token = session.token
        account = session.user
        return session.user
    }

    /**
     * Comes back to a session kept from an earlier launch. The server extends a token each time it
     * is used, so a player who opens the game once a month never signs in again; a token it no
     * longer knows answers 401, which [onUnauthorized] turns into a fresh sign-in.
     */
    suspend fun resume(saved: String): UserProfile {
        logout()
        require(saved.isNotBlank()) { ui("api.no_token") }
        http.token = saved
        return try { refreshUser().also { require(it.isActive) { ui("api.account_disabled") } } }
            catch (e: Exception) { logout(); throw e }
    }

    /** Drops the session here. The server's half is [revoke]; an unrevoked token expires on its own. */
    fun logout() { account = null; http.token = null }
    fun currentUser(): UserProfile? = account
    /** The token to keep between launches, or null when nobody is signed in. */
    fun sessionToken(): String? = http.token
    /**
     * Ends one session on the server. It takes the token rather than reading the session's own, because
     * signing out drops the local session at once and this call is only its echo: nothing waits
     * for it, and a failure changes nothing a player can see.
     */
    suspend fun revoke(saved: String) {
        try { http.request("POST", "api/v1/user/logout", bearer = saved) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) {}
    }
    /** Re-reads the signed-in account, so a role or character count change is picked up. */
    suspend fun refreshUser(): UserProfile {
        val profile: UserProfile = WireJson.decodeFromJsonElement(http.request("GET", "api/v1/user/me", authenticated = true))
        requireId(profile.id)
        account = profile
        return profile
    }
    /** The server ends every other session of the account and keeps this one. */
    suspend fun changePassword(current: String, replacement: String) {
        http.request("POST", "api/v1/user/changePassword", body = WireJson.encodeToJsonElement(PasswordChange(current, replacement)),
            authenticated = true, sensitive = true)
    }

    suspend fun capabilities(): ApiCapabilities =
        ApiCapabilities.of(WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(RouteInfo.serializer()), http.request("GET", "system/routes")))
    suspend fun health(): JsonElement = http.request("GET", "system/health")
}
