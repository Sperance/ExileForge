package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.ForgeApplication
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.contract.entityVersion
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.definitionKey
import com.sperance.exileforge.core.contract.diff
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.referenceKey
import com.sperance.exileforge.core.contract.starterModifier
import com.sperance.exileforge.core.contract.template
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.editor.validateForm
import com.sperance.exileforge.core.generation.modifierFromDefinition
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.EquipmentKind
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.RequestJournal
import com.sperance.exileforge.core.network.normalizeServer
import com.sperance.exileforge.core.verification.CrudScenario
import com.sperance.exileforge.data.settings.ServerStore
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.PendingInventoryAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*

class ForgeViewModel(private val store: ServerStore, private val journal: RequestJournal) : ViewModel() {
    private val mutable = MutableStateFlow(ForgeState())
    val state = mutable.asStateFlow()
    val logs = journal.entries
    private lateinit var api: GameApi
    private var metadataJob: Job? = null
    init {
        viewModelScope.launch {
            try {
                val server = store.server.first()
                api = GameApi(server, journal)
                val restored = store.pending.first()?.let { raw ->
                    val saved = WireJson.parseToJsonElement(raw).jsonObject
                    PendingInventoryAction(saved.text("characterId"), saved.text("operation"), saved.getValue("payload").jsonObject)
                }
                mutable.update { it.copy(pending = restored, characterId = restored?.characterId.orEmpty()) }
                mutable.update { it.copy(server = server, serverDraft = server, busy = false) }
                mutable.update { it.copy(message = "Войдите в аккаунт для загрузки каталога") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                api = GameApi("http://10.0.2.2:8080/", journal)
                mutable.update { it.copy(busy = false, error = true, message = e.message) }
            }
        }
    }
    fun tab(tab: Int) { mutable.update { it.copy(tab = tab) } }
    fun query(value: String) { mutable.update { it.copy(query = value) } }
    suspend fun referencePage(source: EntitySource, page: Int) = api.referencePage(source, page)
    fun serverDraft(value: String) { if (!state.value.busy) mutable.update { it.copy(serverDraft = value) } }
    fun dismissMessage() { mutable.update { it.copy(message = null) } }
    fun clearLogs() = journal.clear()
    fun catalog(value: Catalog) {
        if (state.value.busy || state.value.editorOpen) return
        mutable.update { it.copy(catalog = value, items = emptyList(), page = 0, total = 0, totalPages = 0, query = "") }
        refresh()
    }
    private fun task(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, message = null, error = false) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if(e is ApiFailure && e.status == 401) clearSession()
                if(e is ApiFailure && e.status == 409) mutable.update { it.copy(conflict = true, inventoryVersion = null) }
                val prefix = if(e is ApiFailure && e.status == 409) "Запись изменена. Черновик сохранён; обновите данные и проверьте изменения. " else if (e is ApiFailure) "HTTP ${e.status ?: "—"} ${e.code.orEmpty()}: " else ""
                mutable.update { it.copy(error = true, message = prefix + (e.message ?: "Ошибка запроса")) }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    private suspend fun loadPage(page: Int) {
        val result = api.page(state.value.catalog, page)
        mutable.update { it.copy(items = result.items, page = result.page, total = result.totalItems, totalPages = result.totalPages) }
    }
    fun refresh(page: Int = state.value.page) = task { if(state.value.signedIn) loadPage(page) else mutable.update { it.copy(tab = 3, message = "Войдите в аккаунт") } }
    fun connect() = task {
        metadataJob?.cancel()
        check(state.value.pending == null) { "Сначала подтвердите результат ожидающего запроса" }
        val server = normalizeServer(state.value.serverDraft)
        store.save(server)
        clearSession()
        api = GameApi(server, journal)
        journal.clear()
        mutable.update { it.copy(server = server, serverDraft = server, items = emptyList(), original = null, editorOpen = false, page = 0, total = 0, totalPages = 0, checks = emptyList(), definitions = emptyList(), inventoryBases = emptyMap(), inventoryDefinitions = emptyList(), signedIn = false, inventory = emptyList(), inventoryVersion = null, pending = null, characterId = "", currencies = emptyList(), health = "Проверка соединения…") }
        val health = api.health()
        mutable.update { it.copy(health = health.toString(), message = "Сервер доступен") }
    }
    fun health() = task {
        val result = api.health().toString()
        mutable.update { it.copy(health = result, message = "Проверка соединения завершена") }
    }
    fun count() = task {
        val result = api.count(state.value.catalog)
        mutable.update { it.copy(message = "Количество: $result") }
    }
    fun open(id: String) = task {
        val doc = api.get(state.value.catalog, id) ?: error("Предмет не найден")
        val pinned = pinnedDefinitions(doc)
        setEditor(doc, doc)
        mutable.update { it.copy(definitions = (pinned + it.definitions).distinctBy(::definitionKey)) }
    }
    fun create(kind: EquipmentKind = EquipmentKind.Weapon) {
        if (state.value.busy || !state.value.canEdit) return
        setEditor(template(state.value.catalog, kind), null)
    }
    private fun setEditor(document: JsonObject, original: JsonObject?) {
        mutable.update { it.copy(conflict = false, original = original, editorOpen = true, draft = document, tab = 1,
            definitions = it.definitions) }
    }
    fun closeEditor() { if (!state.value.busy) mutable.update { it.copy(editorOpen = false, original = null, draft = JsonObject(emptyMap())) } }
    fun edit(document: JsonObject) { if (!state.value.busy) mutable.update { it.copy(draft = document) } }
    private suspend fun pinnedDefinitions(document: JsonObject): List<JsonObject> {
        val refs = listOf("modifierDefinitionRefs", "stockModifierDefinitionRefs").flatMap { (document[it] as? JsonArray).orEmpty() }.map { it.jsonObject }
        val rolls = listOf("modifiers", "params").flatMap { (document[it] as? JsonArray).orEmpty() }.map { raw ->
            val mod = raw.jsonObject
            buildJsonObject { put("definitionId", mod.text("definitionId")); put("revision", mod.text("definitionRevision").toIntOrNull() ?: 1) }
        }
        return (refs + rolls).distinctBy { referenceKey(it) }.map { api.definition(it.text("definitionId"), it.text("revision").toIntOrNull() ?: 1) }
    }
    fun publishDefinition(document: JsonObject, expectedRevision: Int) = task {
        validateForm("definition", document)
        val saved = api.publishDefinition(document, expectedRevision)
        mutable.update { it.copy(definitions = (listOf(saved) + it.definitions).distinctBy(::definitionKey), message = "Опубликовано ${saved.text("id")} v${saved.text("revision")}") }
    }
    fun definitionQuery(value: String) { mutable.update { it.copy(definitionQuery = value) } }
    fun loadDefinitions(page: Int = 0) = task {
        val result = api.definitions(state.value.definitionQuery.trim(), page)
        val definitions = (pinnedDefinitions(state.value.draft) + result.getValue("items").jsonArray.map { it.jsonObject }).distinctBy(::definitionKey)
        mutable.update { it.copy(definitions = definitions, definitionPage = page, definitionTotal = result.getValue("total").jsonPrimitive.int) }
    }
    private fun clearSession() {
        metadataJob?.cancel(); api.logout(); journal.clear()
        mutable.update { it.copy(signedIn = false, profile = null, sessionEpoch = it.sessionEpoch + 1, items = emptyList(), total = 0, page = 0, totalPages = 0,
            original = null, draft = JsonObject(emptyMap()), editorOpen = false, inventory = emptyList(), inventoryVersion = null, equipmentView = null,
            inventoryBases = emptyMap(), inventoryDefinitions = emptyList(), characterOwner = "", selectedEquipment = "", tab = 3, conflict = false) }
    }
    fun login(login: String, password: String) = task {
        clearSession()
        api.capabilities().requireCompatible()
        api.login(login, password)
        val profile = try { api.currentUser() } catch(e: Exception) { api.logout(); throw e }
        mutable.update { it.copy(signedIn = true, profile = profile, message = "Вход выполнен", tab = 0) }
        loadPage(0)
    }
    fun logout() { if(!state.value.busy) clearSession() }
    fun changePassword(current: String, replacement: String) = task {
        require(replacement.length in 12..128) { "Новый пароль: от 12 до 128 символов" }
        val profile = api.currentUser()
        api.changePassword(ChangePasswordCommand(profile.version, current, replacement))
        clearSession()
        mutable.update { it.copy(message = "Пароль изменён. Войдите снова.") }
    }
    fun reloadEditor() = task {
        val original = requireNotNull(state.value.original)
        val latest = api.get(state.value.catalog, original.entityId) ?: error("Запись удалена")
        setEditor(latest, latest)
    }
    fun characterId(value: String) {
        if(state.value.busy || state.value.pending != null) return
        mutable.update { it.copy(characterId = value, equipmentView = null, characterOwner = "", inventory = emptyList(), inventoryVersion = null, selectedEquipment = "") }
    }
    fun selectEquipment(value: String) = task {
        mutable.update { it.copy(selectedEquipment = value) }
        val item = state.value.inventory.firstOrNull { it.text("uuid") == value } ?: return@task
        val definitions = pinnedDefinitions(buildJsonObject { put("params", item["params"] ?: JsonArray(emptyList())) })
        mutable.update { it.copy(inventoryDefinitions = (it.inventoryDefinitions + definitions).distinctBy(::definitionKey)) }
    }
    fun showCharacterInventory(id: String) = task {
        check(state.value.pending == null || state.value.characterId == id) { "Сначала подтвердите предыдущую операцию" }
        mutable.update { it.copy(tab = 4, characterId = id, equipmentView = null, characterOwner = "", inventory = emptyList(), inventoryVersion = null) }
        readInventory()
        val currencies = api.currencies()
        mutable.update { it.copy(currencies = currencies, selectedCurrency = currencies.firstOrNull()?.text("id").orEmpty()) }
    }
    fun editInventoryBase(id: String) = task {
        check(!state.value.editorOpen || state.value.original?.let { diff(it, state.value.draft).isEmpty() } == true) { "Сохраните или закройте текущий черновик" }
        val doc = api.get(Catalog.EQUIPMENT, id) ?: error("База предмета не найдена")
        val definitions = pinnedDefinitions(doc)
        mutable.update { it.copy(catalog = Catalog.EQUIPMENT, items = emptyList(), total = 0, page = 0, totalPages = 0, definitions = definitions) }
        setEditor(doc, doc)
    }
    private fun loadInventoryMetadata() {
        metadataJob?.cancel()
        val snapshot = state.value
        val currentApi = api
        metadataJob = viewModelScope.launch {
            val limit = Semaphore(4)
            snapshot.inventory.map { it.text("equipmentId") }.distinct().filter { it !in snapshot.inventoryBases }.map { id ->
                async {
                    limit.withPermit {
                        try {
                            val base = currentApi.get(Catalog.EQUIPMENT, id) ?: return@withPermit
                            mutable.update { if(it.server == snapshot.server && it.characterId == snapshot.characterId) it.copy(inventoryBases = it.inventoryBases + (id to base)) else it }
                        } catch(e: CancellationException) { throw e } catch(_: Exception) { /* Fallback emblem/name stays available; refresh retries metadata. */ }
                    }
                }
            }.awaitAll()
        }
    }
    fun selectCurrency(value: String) { if(!state.value.busy) mutable.update { it.copy(selectedCurrency = value) } }
    private fun applyEquipmentView(result: EquipmentView) {
        mutable.update { it.copy(equipmentView = result, inventory = result.inventory, inventoryVersion = result.characterVersion, conflict = false,
            inventoryBases = it.inventoryBases + result.inventory.mapNotNull { item -> (item["baseSnapshot"] as? JsonObject)?.let { base -> item.text("equipmentId") to base } },
            selectedEquipment = it.selectedEquipment.takeIf { id -> result.inventory.any { e -> e.text("uuid") == id } } ?: result.inventory.firstOrNull()?.text("uuid").orEmpty()) }
        loadInventoryMetadata()
    }
    private suspend fun readInventory() {
        val id = state.value.characterId.trim()
        val character = api.get(Catalog.CHARACTERS, id) ?: error("Персонаж недоступен")
        val result = api.equipment(id)
        mutable.update { it.copy(characterOwner = character.text("userId")) }
        applyEquipmentView(result)
    }
    private fun characterCommand(block: suspend (String, Long) -> EquipmentView) = task {
        check(state.value.pending == null) { "Сначала подтвердите предыдущую операцию" }
        val version = requireNotNull(state.value.inventoryVersion) { "Обновите экипировку" }
        try { applyEquipmentView(block(state.value.characterId, version)); mutable.update { it.copy(message = "Изменения сохранены") } }
        catch(e: Exception) { mutable.update { it.copy(inventoryVersion = null) }; throw e }
    }
    fun equip(uuid: String, slot: EquipmentSlot) = characterCommand { id, version -> api.equip(id, EquipCommand(version, uuid, slot)) }
    fun unequip(slot: EquipmentSlot) = characterCommand { id, version -> api.unequip(id, UnequipCommand(version, slot)) }
    fun grant(equipmentId: String) = characterCommand { id, version -> check(state.value.isAdmin); api.grant(id, GrantEquipmentCommand(version, equipmentId)) }
    fun adjustItems(itemId: String, amount: Long) = characterCommand { id, version -> check(state.value.isAdmin); require(amount != 0L); api.adjustItems(id, AdjustItemsCommand(version, listOf(ItemStack(itemId, amount)))) }
    fun redeem(code: String) = characterCommand { id, version -> api.redeem(id, RedeemCommand(version, code.trim())) }
    suspend fun recipe(id: String) = api.recipe(id)
    fun useRecipe(recipe: JsonObject, ingredients: List<String>, amount: Long) = characterCommand { id, version -> api.useRecipe(id, UseRecipeCommand(version, recipe.entityId, recipe.entityVersion, ingredients, amount)) }
    fun loadInventory() = task {
        readInventory()
        val currencies = api.currencies()
        mutable.update { it.copy(currencies = currencies, selectedCurrency = it.selectedCurrency.ifBlank { currencies.firstOrNull()?.text("id").orEmpty() }) }
    }
    fun randomItem() { if(state.value.busy || state.value.editorOpen) return; mutable.update { it.copy(tab = 4) } }
    fun inventoryAction(operation: String) = task {
        check(state.value.pending == null) { "Сначала разрешите результат предыдущей операции" }
        val state = state.value
        check(state.ownsCharacter) { "Сферы и дроп доступны только владельцу персонажа" }
        if(operation == "drop") check(state.isAdmin) { "Дроп доступен администратору" }
        val version = requireNotNull(state.inventoryVersion) { "Загрузите инвентарь" }
        val payload = buildJsonObject {
            put("requestId", java.util.UUID.randomUUID().toString()); put("expectedVersion", version)
            if(operation == "craft") {
                require(state.selectedEquipment.isNotBlank() && state.selectedCurrency.isNotBlank()) { "Выберите экипировку и сферу" }
                put("equipmentUuid", state.selectedEquipment); put("currency", state.selectedCurrency)
            }
        }
        val pending = PendingInventoryAction(state.characterId.trim(), operation, payload)
        store.savePending(buildJsonObject { put("characterId", pending.characterId); put("operation", operation); put("payload", payload) }.toString())
        mutable.update { it.copy(pending = pending) }
        executePending()
    }
    fun retryInventoryAction() = task { executePending() }
    private suspend fun executePending() {
        val pending = requireNotNull(state.value.pending)
        val character = api.get(Catalog.CHARACTERS, pending.characterId) ?: error("Персонаж недоступен")
        check(character.text("userId") == state.value.profile?.id) { "Войдите в аккаунт владельца ожидающей операции" }
        val result = try { api.mutateInventory(pending.characterId, pending.operation, pending.payload) }
        catch(e: ApiFailure) {
            // Only explicit client rejections are definitive. Network/5xx can hide a committed write.
            if(e.status in listOf(400, 403, 404, 409, 422)) {
                store.savePending(null)
                mutable.update { it.copy(pending = null, inventoryVersion = null) }
            }
            throw e
        }
        val equipment = result.getValue("equipment").jsonObject
        store.savePending(null)
        mutable.update { it.copy(pending = null, inventoryVersion = result.getValue("characterVersion").jsonPrimitive.long,
            inventory = it.inventory.filterNot { item -> item.text("uuid") == equipment.text("uuid") } + equipment,
            selectedEquipment = equipment.text("uuid"), message = "Операция выполнена" + (result["currencyRemaining"]?.let { amount -> ". Осталось сфер: $amount" } ?: "")) }
        try { readInventory() } catch(e: CancellationException) { throw e }
        catch(_: Exception) { mutable.update { it.copy(inventoryVersion = null, message = "Операция выполнена. Обновите инвентарь перед следующей.") } }
    }
    fun save() = task {
        check(state.value.canEdit) { "Недостаточно прав" }
        check(!state.value.conflict) { "Сначала обновите запись после конфликта" }
        val document = state.value.draft
        val catalog = state.value.catalog
        validate(document, catalog)
        val original = state.value.original
        val saved = if (original == null) api.create(catalog, JsonObject(document.filterKeys { it in editableFields(catalog) || it == "type" })) else {
            val changes = diff(original, document)
            require(catalog != Catalog.CHARACTERS || "userId" !in changes) { "Владельца существующего персонажа менять нельзя" }
            require(changes.isNotEmpty()) { "Нет изменений для сохранения" }
            api.update(catalog, original.entityId, changes, original.entityVersion)
        }
        if(catalog == Catalog.EQUIPMENT) mutable.update { it.copy(inventoryBases = it.inventoryBases + (saved.entityId to saved)) }
        setEditor(saved, saved)
        mutable.update { it.copy(message = "Сохранено: ${saved.entityId}") }
        // List refresh failure must not imply that the successful mutation failed.
        try { loadPage(state.value.page) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = "Сохранено. Обновите список вручную.") } }
    }
    fun delete() = task {
        val original = state.value.original ?: error("Сначала сохраните предмет")
        check(state.value.canEdit && !state.value.conflict)
        api.delete(state.value.catalog, original.entityId, original.entityVersion)
        mutable.update { it.copy(editorOpen = false, original = null, tab = 0, items = it.items.filterNot { item -> item.entityId == original.entityId }, message = "Предмет удалён") }
        try { loadPage(0) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(message = "Удалено. Обновите список вручную.") } }
    }
    fun runChecks() = task {
        check(state.value.isAdmin) { "Проверки записи доступны администратору" }
        require(state.value.catalog != Catalog.CHARACTERS) { "CRUD-сценарий предназначен для предметов" }
        mutable.update { it.copy(checks = emptyList()) }
        val modifier = if(state.value.catalog == Catalog.EQUIPMENT) {
            val definitions = api.definitions("", 0).getValue("items").jsonArray
            modifierFromDefinition(definitions.firstOrNull()?.jsonObject ?: error("Каталог модификаторов пуст. Запустите Seeder сервера"))
        } else starterModifier()
        CrudScenario(api, modifier).run(state.value.catalog) { result -> mutable.update { it.copy(checks = it.checks + result) } }
    }
    class Factory(private val app: ForgeApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ForgeViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ForgeViewModel(app.serverStore, app.journal) as T
        }
    }
}
