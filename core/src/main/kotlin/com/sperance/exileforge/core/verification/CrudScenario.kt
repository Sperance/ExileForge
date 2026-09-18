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
        val name = "EF-test-${java.util.UUID.randomUUID()}"
        var owned: JsonObject? = null
        try {
            val initial = JsonObject(template(catalog) + ("name" to JsonPrimitive(name)))
            owned = repository.create(catalog, initial)
            val id = owned.entityId
            report(CheckResult(tr("Создание", "Create"), true, id))
            check(repository.get(catalog, id)?.text("name") == name)
            report(CheckResult(tr("Получение по ID", "Get by id"), true, id))
            suspend fun update(changes: JsonObject, label: String) {
                owned = repository.update(catalog, id, changes)
                val loaded = repository.get(catalog, id) ?: error(tr("Запись исчезла", "The record disappeared"))
                check(changes.all { (key, value) -> loaded[key] == value })
                check(loaded.entityVersion > 0)
                report(CheckResult(label, true, tr("Свойства и версия подтверждены", "Fields and version confirmed")))
            }
            update(buildJsonObject { put("description", "CRUD verification complete") }, tr("Изменение + GET", "Update + GET"))
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
            report(CheckResult(tr("Сценарий остановлен", "Scenario stopped"), false, e.message.orEmpty() + if (owned == null) tr(". При потере ответа POST проверьте вручную запись $name; повторное создание автоматически не выполняется.", ". If the POST response was lost, check record $name by hand; it is never re-created automatically.") else ""))
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
