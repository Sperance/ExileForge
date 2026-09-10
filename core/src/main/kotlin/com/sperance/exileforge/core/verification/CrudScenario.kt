package com.sperance.exileforge.core.verification

import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.starterModifier
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.ItemRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Each run owns one preallocated ID. Never deletes a preexisting entity. */
class CrudScenario(private val repository: ItemRepository, private val testModifier: JsonObject = starterModifier()) {
    suspend fun run(catalog: Catalog, report: (CheckResult) -> Unit) {
        val id = UUID.randomUUID().toString().replace("-", "").take(24)
        var ownsId = false
        try {
            check(repository.get(catalog, id) == null) { "ID уже занят" }
            val initial = JsonObject(template(catalog) + mapOf("_id" to JsonPrimitive(id), "name" to JsonPrimitive("EF-test-$id")))
            // Set ownership before POST so ambiguous write failures also attempt cleanup.
            ownsId = true
            val created = repository.create(catalog, initial)
            check(created.entityId == id) { "Сервер вернул другой ID" }
            report(CheckResult("Создание", true, id))
            val loaded = repository.get(catalog, id)
            check(loaded?.text("name") == initial.text("name")) { "Название после GET не совпало" }
            report(CheckResult("Получение по ID", true, id))
            val changes = buildJsonObject { put("description", "CRUD verification complete") }
            repository.update(catalog, id, changes)
            check(repository.get(catalog, id)?.text("description") == "CRUD verification complete") { "Изменение не сохранилось" }
            report(CheckResult("Изменение + повторный GET", true, "Описание совпало"))
            if (catalog == Catalog.EQUIPMENT) {
                val mods = buildJsonArray { add(testModifier) }
                repository.update(catalog, id, buildJsonObject { put("modifiers", mods) })
                check(repository.get(catalog, id)?.get("modifiers") == mods) { "Модификаторы не совпали" }
                report(CheckResult("Добавление модификатора + GET", true, "Модификатор совпал"))
                val changed = buildJsonArray { add(JsonObject(testModifier + ("values" to buildJsonArray { add(buildJsonObject { put("value", 73.0) }) }))) }
                repository.update(catalog, id, buildJsonObject { put("modifiers", changed) })
                check(repository.get(catalog, id)?.get("modifiers") == changed) { "Изменение модификатора не сохранилось" }
                report(CheckResult("Изменение модификатора + GET", true, "Значение совпало"))
                val empty = JsonArray(emptyList())
                repository.update(catalog, id, buildJsonObject { put("modifiers", empty) })
                check(repository.get(catalog, id)?.get("modifiers") == empty) { "Модификаторы не удалены" }
                report(CheckResult("Удаление модификаторов + GET", true, "modifiers = []"))
            }
            repository.delete(catalog, id)
            check(repository.get(catalog, id) == null) { "Предмет остался после DELETE" }
            ownsId = false
            report(CheckResult("Удаление + повторный GET", true, "data = null"))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { report(CheckResult("Сценарий остановлен", false, e.message.orEmpty())) }
        finally {
            if (ownsId) withContext(NonCancellable) {
                try {
                    if (repository.get(catalog, id) != null) repository.delete(catalog, id)
                    check(repository.get(catalog, id) == null)
                    report(CheckResult("Очистка", true, "Тестовый предмет удалён"))
                } catch (e: Exception) {
                    report(CheckResult("Очистка не подтверждена", false, "Проверьте вручную ${catalog.path}, ID $id: ${e.message}"))
                }
            }
        }
    }
}
