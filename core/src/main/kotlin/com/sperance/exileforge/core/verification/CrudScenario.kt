package com.sperance.exileforge.core.verification

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.ItemRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Only a confirmed server-generated ID belongs to this run; cleanup removes exactly that record. */
class CrudScenario(private val repository: ItemRepository, private val modifierId: String = "") {
    suspend fun run(catalog: Catalog, report: (CheckResult) -> Unit) {
        require(catalog != Catalog.CHARACTERS) { tr("CRUD-сценарий предназначен для предметов", "The CRUD scenario is meant for items") }
        // A code, not a name: content has no text of its own since 0.14.0.
        val code = "EF_TEST_" + java.util.UUID.randomUUID().toString().replace("-", "_").uppercase()
        var owned: JsonObject? = null
        try {
            val initial = JsonObject(template(catalog) + ("code" to JsonPrimitive(code)))
            owned = repository.create(catalog, initial)
            val id = owned.entityId
            report(CheckResult(tr("Создание", "Create"), true, id))
            check(repository.get(catalog, id)?.text("code") == code)
            report(CheckResult(tr("Получение по ID", "Get by id"), true, id))
            suspend fun update(changes: JsonObject, label: String) {
                owned = repository.update(catalog, id, changes)
                val loaded = repository.get(catalog, id) ?: error(tr("Запись исчезла", "The record disappeared"))
                // Items and equipment are StockEntity on this server: they carry no version at all.
                check(changes.all { (key, value) -> loaded[key] == value })
                report(CheckResult(label, true, tr("Свойства подтверждены чтением", "Fields confirmed by a read")))
            }
            // `image` is the one editable field both catalogues share now that text has moved out.
            update(buildJsonObject { put("image", "ef-crud-check") }, tr("Изменение + GET", "Update + GET"))
            if (catalog == Catalog.EQUIPMENT && modifierId.isNotBlank()) {
                update(buildJsonObject { put("modifierIds", buildJsonArray { add(modifierId) }) }, tr("Пул модификаторов + GET", "Modifier pool + GET"))
                update(buildJsonObject { put("modifierIds", JsonArray(emptyList())) }, tr("Очистка пула + GET", "Pool cleared + GET"))
            }
            repository.delete(catalog, id)
            owned = null
            check(repository.get(catalog, id) == null)
            report(CheckResult(tr("Удаление + GET", "Delete + GET"), true, tr("Запись недоступна", "The record is gone")))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            report(CheckResult(tr("Сценарий остановлен", "Scenario stopped"), false, e.message.orEmpty() + if (owned == null) tr(". При потере ответа POST проверьте вручную запись $code; повторное создание автоматически не выполняется.", ". If the POST response was lost, check record $code by hand; it is never re-created automatically.") else ""))
        } finally {
            owned?.let { document -> withContext(NonCancellable) {
                try {
                    repository.delete(catalog, document.entityId)
                    check(repository.get(catalog, document.entityId) == null)
                    report(CheckResult(tr("Очистка", "Cleanup"), true, tr("Тестовая запись удалена", "The test record was deleted")))
                } catch (e: Exception) { report(CheckResult(tr("Очистка не подтверждена", "Cleanup not confirmed"), false, tr("Проверьте ${document.entityId}: ${e.message}", "Check ${document.entityId}: ${e.message}"))) }
            } }
        }
    }
}
