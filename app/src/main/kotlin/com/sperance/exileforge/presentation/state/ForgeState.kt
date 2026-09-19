package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.network.FailureState
import com.sperance.exileforge.core.verification.CheckResult
import kotlinx.serialization.json.JsonObject

enum class AppMode { PLAYER, ADMIN }

data class ForgeState(
    val lang: Lang = uiLanguage,
    val mode: AppMode = AppMode.PLAYER,
    val failure: FailureState? = null,

    val profile: UserProfile? = null, val signedIn: Boolean = false, val sessionEpoch: Int = 0,
    val server: String = "http://10.0.2.2:8080/", val serverDraft: String = "http://10.0.2.2:8080/",
    val busy: Boolean = true, val message: String? = null, val error: Boolean = false,

    val tab: Int = 3, val catalog: Catalog = Catalog.EQUIPMENT,
    val filter: CatalogFilter = CatalogFilter(), val query: String = "",
    val items: List<JsonObject> = emptyList(), val page: Int = 0, val totalPages: Int = 0, val total: Long = 0,

    val original: JsonObject? = null, val editorOpen: Boolean = false, val draft: JsonObject = JsonObject(emptyMap()),
    /** The shared modifier catalogue, read once per session so rolled values can be named. */
    val definitions: List<ModifierDefinition> = emptyList(),

    val characterId: String = "", val characterOwner: String = "", val hero: HeroView? = null,
    /** Templates of the instances on screen, keyed by `equipmentId`; an instance carries only rolls. */
    val inventoryBases: Map<String, JsonObject> = emptyMap(),
    val selectedEquipment: String = "",
    /** What the admin's random grant asks the server for. Blank means "any". */
    val grantRarity: String = "", val grantSlot: String = "",
    /** The currency catalogue, read once per session; `selectedOrb` is the `items` id of one orb. */
    val orbs: List<CurrencyItem> = emptyList(), val selectedOrb: String = "",

    val checks: List<CheckResult> = emptyList(),
    val health: String = tr("Соединение ещё не проверено", "The connection has not been checked yet"),
) {
    val isAdmin: Boolean get() = signedIn && profile?.role == "ADMIN"
    val adminTools: Boolean get() = isAdmin && mode == AppMode.ADMIN
    val canEdit: Boolean get() = signedIn && (catalog == Catalog.CHARACTERS || adminTools)
    val ownsCharacter: Boolean get() = signedIn && profile?.id == characterOwner
}
