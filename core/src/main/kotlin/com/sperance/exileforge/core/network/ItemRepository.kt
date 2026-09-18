package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.model.Catalog
import kotlinx.serialization.json.JsonObject

/** The CRUD surface of the server, small enough for a test to fake. */
interface ItemRepository {
    suspend fun page(catalog: Catalog, page: Int): ItemPage
    suspend fun get(catalog: Catalog, id: String): JsonObject?
    suspend fun create(catalog: Catalog, document: JsonObject): JsonObject
    suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject
    suspend fun delete(catalog: Catalog, id: String)
}
