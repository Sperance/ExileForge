package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.entityVersion
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
    private val modifierId = "0123456789abcdef01234567"

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
        override suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject {
            if (failUpdate) error("Write failed")
            val current = documents.getValue(id)
            return JsonObject(current + changes + ("version" to JsonPrimitive(current.entityVersion + 1))).also { documents[id] = it }
        }
        override suspend fun delete(catalog: Catalog, id: String) { documents.remove(id) ?: error("Missing $id"); deleted += id }
    }

    @Test fun `the scenario walks every equipment operation and leaves other records alone`(): Unit = runBlocking {
        val repo = FakeRepository()
        repo.documents["existing"] = buildJsonObject { put("_id", "existing"); put("version", 1) }
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo, modifierId).run(Catalog.EQUIPMENT, reports::add)
        assertEquals(6, reports.size); assertTrue(reports.all { it.passed })
        assertEquals(setOf("existing"), repo.documents.keys)
        assertEquals(1, repo.deleted.size)
    }

    @Test fun `a failed update still cleans up only its own test record`(): Unit = runBlocking {
        val repo = FakeRepository(failUpdate = true)
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo).run(Catalog.ITEMS, reports::add)
        assertTrue(reports.any { !it.passed }); assertTrue(repo.documents.isEmpty())
        assertEquals("Очистка", reports.last().label)
    }

    @Test fun `a lost create response never guesses an identity to delete`(): Unit = runBlocking {
        val repo = FakeRepository(ambiguousCreate = true)
        val reports = mutableListOf<CheckResult>()
        CrudScenario(repo).run(Catalog.ITEMS, reports::add)
        assertEquals(1, repo.documents.size); assertTrue(repo.deleted.isEmpty())
        assertFalse(reports.last().passed)
    }

    @Test fun `characters are out of scope for a write scenario`(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { CrudScenario(FakeRepository()).run(Catalog.CHARACTERS) {} }
    }
}
