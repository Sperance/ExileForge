package com.sperance.exileforge.core.verification

import com.sperance.exileforge.core.contract.*
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.network.ItemRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Only a confirmed server-generated ID belongs to this run; cleanup removes exactly that record. */
class CrudScenario(private val repository: ItemRepository, private val modifierId: String = "") {
    suspend fun run(catalog: Catalog, report: (CheckResult) -> Unit) {
        require(catalog != Catalog.CHARACTERS) { ui("checks.items_only") }
        // A code, not a name: content has no text of its own since 0.14.0.
        val code = "EF_TEST_" + java.util.UUID.randomUUID().toString().replace("-", "_").uppercase()
        var owned: JsonObject? = null
        try {
            val initial = JsonObject(template(catalog) + ("code" to JsonPrimitive(code)))
            owned = repository.create(catalog, initial)
            val id = owned.entityId
            report(CheckResult(ui("crud.create"), true, id))
            check(repository.get(catalog, id)?.text("code") == code)
            report(CheckResult(ui("crud.get_by_id"), true, id))
            suspend fun update(changes: JsonObject, label: String) {
                owned = repository.update(catalog, id, changes)
                val loaded = repository.get(catalog, id) ?: error(ui("crud.vanished"))
                // Items and equipment are StockEntity on this server: they carry no version at all.
                check(changes.all { (key, value) -> loaded[key] == value })
                report(CheckResult(label, true, ui("crud.fields_confirmed")))
            }
            // The two catalogues share no editable field at all, so each writes its most inert one:
            // a price changes nothing about an item, and a required level is only ever printed.
            // Rarity and item level are deliberately left alone — they decide affix capacity and
            // which tiers may roll, so editing them would change what the next copy comes out as.
            update(when (catalog) {
                Catalog.ITEMS -> buildJsonObject { put("price", 7L) }
                else -> buildJsonObject { put("requiredLevel", 7) }
            }, ui("crud.update"))
            if (catalog == Catalog.EQUIPMENT && modifierId.isNotBlank()) {
                update(buildJsonObject { put("modifierIds", buildJsonArray { add(modifierId) }) }, ui("crud.pool"))
                update(buildJsonObject { put("modifierIds", JsonArray(emptyList())) }, ui("crud.pool_cleared"))
            }
            repository.delete(catalog, id)
            owned = null
            check(repository.get(catalog, id) == null)
            report(CheckResult(ui("crud.delete"), true, ui("crud.gone")))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            report(CheckResult(ui("crud.stopped"), false, e.message.orEmpty() + if (owned == null) ui("crud.lost_post", code) else ""))
        } finally {
            owned?.let { document -> withContext(NonCancellable) {
                try {
                    repository.delete(catalog, document.entityId)
                    check(repository.get(catalog, document.entityId) == null)
                    report(CheckResult(ui("crud.cleanup"), true, ui("crud.cleanup_done")))
                } catch (e: Exception) { report(CheckResult(ui("crud.cleanup_failed"), false, ui("crud.check", document.entityId, e.message))) }
            } }
        }
    }
}
