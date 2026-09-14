package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.entityVersion
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.ItemPage
import com.sperance.exileforge.core.network.ItemRepository
import com.sperance.exileforge.core.verification.CheckResult
import com.sperance.exileforge.core.verification.CrudScenario
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test

class CrudScenarioTest {
    private class FakeRepository(val failUpdate: Boolean = false, val ambiguousCreate: Boolean = false) : ItemRepository {
        val documents = mutableMapOf<String, JsonObject>()
        val deleted = mutableListOf<String>()
        override suspend fun page(catalog: Catalog, page: Int) = ItemPage(documents.values.toList(), page, 1, documents.size.toLong())
        override suspend fun get(catalog: Catalog, id: String) = documents[id]
        override suspend fun create(catalog: Catalog, document: JsonObject): JsonObject {
            val stored = JsonObject(document + mapOf("_id" to JsonPrimitive("0123456789abcdef01234567"), "version" to JsonPrimitive(0)))
            documents[stored.entityId] = stored
            if (ambiguousCreate) error("Connection lost after server write")
            return stored
        }
        override suspend fun update(catalog: Catalog, id: String, changes: JsonObject, expectedVersion: Long): JsonObject {
            if (failUpdate) error("Write failed")
            check(documents.getValue(id).entityVersion == expectedVersion)
            return JsonObject(documents.getValue(id) + changes + ("version" to JsonPrimitive(expectedVersion + 1))).also { documents[id] = it }
        }
        override suspend fun delete(catalog: Catalog, id: String, expectedVersion: Long) { check(documents.getValue(id).entityVersion == expectedVersion); documents.remove(id); deleted += id }
    }
    @Test fun `scenario verifies all equipment operations and keeps existing items`(): Unit = runBlocking {
        val repo = FakeRepository()
        repo.documents["existing"] = buildJsonObject { put("_id", "existing") }
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo).run(Catalog.EQUIPMENT, reports::add)
        assertEquals(6, reports.size); assertTrue(reports.all { it.passed })
        assertEquals(setOf("existing"), repo.documents.keys)
        assertEquals(1, repo.deleted.size)
    }
    @Test fun `failed update still cleans only its test item`(): Unit = runBlocking {
        val repo = FakeRepository(failUpdate = true)
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo).run(Catalog.ITEMS, reports::add)
        assertTrue(reports.any { !it.passed }); assertTrue(repo.documents.isEmpty())
        assertEquals("Очистка", reports.last().label)
    }
    @Test fun `lost create response never guesses an identity for deletion`(): Unit = runBlocking {
        val repo = FakeRepository(ambiguousCreate = true)
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo).run(Catalog.ITEMS, reports::add)
        assertEquals(1, repo.documents.size); assertTrue(repo.deleted.isEmpty())
        assertFalse(reports.last().passed)
    }
}
