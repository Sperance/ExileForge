package com.sperance.exileforge.core.verification

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.ItemRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Only a confirmed server-generated ID belongs to this run. Cleanup retains its last known version. */
class CrudScenario(private val repository: ItemRepository, private val testModifier: JsonObject = starterModifier()) {
    suspend fun run(catalog: Catalog, report: (CheckResult) -> Unit) {
        require(catalog != Catalog.CHARACTERS)
        val name = "EF-test-${java.util.UUID.randomUUID()}"
        var owned: JsonObject? = null
        try {
            val initial = JsonObject(template(catalog) + ("name" to JsonPrimitive(name)))
            owned = repository.create(catalog, initial)
            val id = owned.entityId
            report(CheckResult("Создание", true, id))
            check(repository.get(catalog, id)?.text("name") == name)
            report(CheckResult("Получение по ID", true, id))
            suspend fun update(changes: JsonObject, label: String) {
                owned = repository.update(catalog, id, changes, requireNotNull(owned).entityVersion)
                val loaded = repository.get(catalog, id) ?: error("Запись исчезла")
                check(changes.all { (key, value) -> loaded[key] == value })
                check(loaded.entityVersion == requireNotNull(owned).entityVersion)
                report(CheckResult(label, true, "Свойства и версия подтверждены"))
            }
            update(buildJsonObject { put("description", "CRUD verification complete") }, "Изменение + GET")
            if (catalog == Catalog.EQUIPMENT) {
                val refs = buildJsonArray { add(buildJsonObject { put("definitionId", testModifier.text("definitionId")); put("revision", testModifier.text("definitionRevision").toIntOrNull() ?: 1) }) }
                update(buildJsonObject { put("modifierDefinitionRefs", refs) }, "Выбор модификатора + GET")
                update(buildJsonObject { put("modifierDefinitionRefs", JsonArray(emptyList())) }, "Удаление ссылки + GET")
            }
            repository.delete(catalog, id, requireNotNull(owned).entityVersion)
            owned = null
            check(repository.get(catalog, id) == null)
            report(CheckResult("Удаление + GET", true, "Запись недоступна"))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            report(CheckResult("Сценарий остановлен", false, e.message.orEmpty() + if(owned == null) ". При потере ответа POST проверьте вручную запись $name; повторное создание автоматически не выполняется." else ""))
        } finally {
            owned?.let { document -> withContext(NonCancellable) {
                try {
                    repository.delete(catalog, document.entityId, document.entityVersion)
                    check(repository.get(catalog, document.entityId) == null)
                    report(CheckResult("Очистка", true, "Тестовая запись удалена"))
                } catch (e: Exception) { report(CheckResult("Очистка не подтверждена", false, "Проверьте ${document.entityId}: ${e.message}")) }
            } }
        }
    }
}
