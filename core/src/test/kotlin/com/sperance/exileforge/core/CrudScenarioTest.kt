package com.sperance.exileforge.core

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
            documents[document.entityId] = document
            if (ambiguousCreate) error("Connection lost after server write")
            return document
        }
        override suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject {
            if (failUpdate) error("Write failed")
            return JsonObject(documents.getValue(id) + changes).also { documents[id] = it }
        }
        override suspend fun delete(catalog: Catalog, id: String) { documents.remove(id); deleted += id }
    }
    @Test fun `scenario verifies all equipment operations and keeps existing items`(): Unit = runBlocking {
        val repo = FakeRepository()
        repo.documents["existing"] = buildJsonObject { put("_id", "existing") }
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo).run(Catalog.EQUIPMENT, reports::add)
        assertEquals(7, reports.size); assertTrue(reports.all { it.passed })
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
    @Test fun `lost create response still cleans preallocated id`(): Unit = runBlocking {
        val repo = FakeRepository(ambiguousCreate = true)
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo).run(Catalog.ITEMS, reports::add)
        assertTrue(repo.documents.isEmpty()); assertEquals(1, repo.deleted.size)
        assertTrue(reports.last().passed)
    }
}
