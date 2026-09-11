package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.verification.CheckResult
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

data class ForgeState(
    val tab: Int = 0, val catalog: Catalog = Catalog.EQUIPMENT,
    val server: String = "http://10.0.2.2:8080/", val serverDraft: String = "http://10.0.2.2:8080/",
    val busy: Boolean = true, val message: String? = null, val error: Boolean = false,
    val items: List<JsonObject> = emptyList(), val page: Int = 0, val totalPages: Int = 0, val total: Long = 0,
    val query: String = "",
    val original: JsonObject? = null, val editorOpen: Boolean = false,
    val draft: JsonObject = JsonObject(emptyMap()), val definitions: List<JsonObject> = emptyList(),
    val definitionQuery: String = "", val definitionPage: Int = 0, val definitionTotal: Int = 0,
    val signedIn: Boolean = false, val characterId: String = "", val inventory: List<JsonObject> = emptyList(),
    val inventoryBases: Map<String, JsonObject> = emptyMap(), val inventoryDefinitions: List<JsonObject> = emptyList(),
    val inventoryVersion: Long? = null, val currencies: List<JsonObject> = emptyList(),
    val selectedEquipment: String = "", val selectedCurrency: String = "",
    val pending: PendingInventoryAction? = null,
    val checks: List<CheckResult> = emptyList(), val health: String = "Соединение ещё не проверено"
)
