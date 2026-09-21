package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.creationFields
import com.sperance.exileforge.core.contract.editableFields
import com.sperance.exileforge.core.contract.entityId
import com.sperance.exileforge.core.contract.protectedFields
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.text
import com.sperance.exileforge.core.contract.validate
import com.sperance.exileforge.core.contract.validateModifierPool
import com.sperance.exileforge.core.display.IconManifest
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.LocaleLanguage
import com.sperance.exileforge.core.i18n.LocaleManifest
import com.sperance.exileforge.core.i18n.tr
import com.sperance.exileforge.core.model.Catalog
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.model.EntitySource
import com.sperance.exileforge.core.model.auction.*
import com.sperance.exileforge.core.model.command.*
import com.sperance.exileforge.core.model.currency.CURRENCY_CATEGORY
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.hero.*
import com.sperance.exileforge.core.model.modifier.ModifierDefinition
import com.sperance.exileforge.core.model.modifier.ModifierTier
import com.sperance.exileforge.core.model.progression.CharacterClass
import com.sperance.exileforge.core.model.progression.ExperienceLevel
import com.sperance.exileforge.core.model.skilltree.SkillTreeNode
import com.sperance.exileforge.core.model.skilltree.SkillTreeState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

private val JsonMedia = "application/json; charset=utf-8".toMediaType()

/** Route roots that carry more than one command, kept in one place so a move is one edit. */
private const val TREE = "api/v1/character/skilltree"
private const val AUCTION = "api/v1/auctionlot"

/** "No account for this device yet" — the server's way of saying "register it". */
private const val DEVICE_UNKNOWN = "US_015"

fun normalizeServer(value: String): String {
    val url = value.trim().toHttpUrlOrNull() ?: error(tr("Введите URL с http:// или https://", "Enter a URL starting with http:// or https://"))
    require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { tr("URL не должен содержать пароль, query или fragment", "The URL must not contain a password, query or fragment") }
    return url.toString().trimEnd('/') + "/"
}

/**
 * The single HTTP client for every ktor-bestgame route.
 *
 * This server issues no token: `GET /api/v1/user/login` answers with the account document and the
 * client keeps it in memory for the rest of the session. [account] is therefore the whole session —
 * it is what `authenticated = true` requires and what [logout] drops.
 */
class GameApi(
    server: String,
    private val journal: RequestJournal = RequestJournal(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build(),
    private val onUnauthorized: () -> Unit = {}
) : ItemRepository {
    private val base = normalizeServer(server).toHttpUrlOrNull()!!
    private var account: UserProfile? = null

    // ==================== session ====================

    /** Credentials travel as query parameters because that is the route the server exposes. */
    suspend fun login(login: String, password: String): UserProfile {
        account = null
        require(login.isNotBlank() && password.isNotEmpty()) { tr("Введите логин и пароль", "Enter a login and a password") }
        val profile: UserProfile = WireJson.decodeFromJsonElement(request("GET", "api/v1/user/login", mapOf("login" to login, "password" to password), sensitive = true))
        requireId(profile.id)
        require(profile.isActive) { tr("Учётная запись отключена", "The account is disabled") }
        account = profile
        return profile
    }
    /**
     * Sign in with the device's own identifier, and register on the first try.
     *
     * The server keeps one account per `device_id` and answers `US_015` when it has never seen
     * this one, which is the whole registration handshake: a miss becomes a `POST` that creates
     * the account and answers with it. There is no password in this path at all, so nothing here
     * is sensitive — the identifier is not a secret, it is a name.
     */
    suspend fun loginByDevice(deviceId: String): UserProfile {
        account = null
        require(deviceId.isNotBlank()) { tr("Не удалось определить устройство", "The device could not be identified") }
        val profile = try { device("GET", "api/v1/user/login/byDeviceId", deviceId) }
            catch (e: ApiFailure) { if (e.code == DEVICE_UNKNOWN) device("POST", "api/v1/user/byDeviceId", deviceId) else throw e }
        require(profile.isActive) { tr("Учётная запись отключена", "The account is disabled") }
        account = profile
        return profile
    }

    private suspend fun device(method: String, path: String, deviceId: String): UserProfile =
        WireJson.decodeFromJsonElement<UserProfile>(request(method, path, mapOf("deviceId" to deviceId)))
            .also { requireId(it.id) }

    fun logout() { account = null }
    fun currentUser(): UserProfile? = account
    /** Re-reads the signed-in account, so a role or character count change is picked up. */
    suspend fun refreshUser(): UserProfile {
        val id = requireNotNull(account) { tr("Войдите в аккаунт", "Sign in to your account") }.id
        val profile: UserProfile = WireJson.decodeFromJsonElement(request("GET", "api/v1/user", mapOf("id" to id), authenticated = true))
        account = profile
        return profile
    }
    suspend fun changePassword(current: String, replacement: String) {
        val id = requireNotNull(account) { tr("Войдите в аккаунт", "Sign in to your account") }.id
        request("GET", "api/v1/user/changePassword", mapOf("id" to id, "password" to current, "new_password" to replacement), authenticated = true, sensitive = true)
        logout()
    }

    suspend fun capabilities(): ApiCapabilities =
        ApiCapabilities.of(WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(RouteInfo.serializer()), request("GET", "system/routes")))
    suspend fun health(): JsonElement = request("GET", "system/health")

    // ==================== catalogues ====================

    private fun route(catalog: Catalog) = "api/v1/${catalog.path}"

    /**
     * The whole collection.
     *
     * This is how every list is read. The server's `/paged` route still hands `page` straight to the
     * repository as the offset instead of `page * size`, so every page but the first is off by all
     * but one record — 0.9.1 swapped the arguments of `findLimited`, not the arithmetic above it.
     * Reading the collection is safe here because these collections are small and server-seeded.
     */
    private suspend fun all(path: String): List<JsonObject> =
        request("GET", path, authenticated = true).jsonArray.map { it.jsonObject }

    override suspend fun page(catalog: Catalog, page: Int): ItemPage { requirePage(page); return slice(all(route(catalog)), page, CATALOG_PAGE_SIZE) }

    /**
     * A filtered search narrows the collection on the client: the server offers no filter at all.
     * Nothing is recomputed — only fields the server already wrote are compared.
     */
    suspend fun search(catalog: Catalog, page: Int, filter: CatalogFilter): ItemPage {
        requirePage(page)
        return slice(all(route(catalog)).filter(filter::matches), page, CATALOG_PAGE_SIZE)
    }

    suspend fun referencePage(source: EntitySource, page: Int, query: String = ""): ItemPage {
        requirePage(page)
        val records = all("api/v1/${source.path}")
        val matching = if (query.isBlank()) records else records.filter { document ->
            listOf("name", "login", "code", "category", "subCategory").any { document.text(it).contains(query.trim(), true) }
        }
        return slice(matching, page, REFERENCE_PAGE_SIZE)
    }

    private fun requirePage(page: Int) = require(page >= 0) { tr("Номер страницы не может быть отрицательным", "A page number cannot be negative") }

    private fun slice(items: List<JsonObject>, page: Int, size: Int): ItemPage {
        val pages = (items.size + size - 1) / size
        return ItemPage(items.drop(page * size).take(size), page, pages, items.size.toLong())
    }

    override suspend fun get(catalog: Catalog, id: String): JsonObject? {
        requireId(id)
        return try { request("GET", route(catalog), mapOf("id" to id), authenticated = true).let { if (it == JsonNull) null else it.jsonObject } }
        catch (e: ApiFailure) { if (e.status == 404) null else throw e }
    }

    /** POST takes an array of documents and answers with the created ones, identity included. */
    override suspend fun create(catalog: Catalog, document: JsonObject): JsonObject {
        val allowed = editableFields(catalog) + creationFields(catalog)
        // Naming the offenders: a caller that posts a form's draft raw is the way this goes wrong,
        // and "one of your fields" leaves the reader to guess which of a dozen it was.
        val refused = document.keys.filterNot { it in allowed }
        require(refused.isEmpty()) {
            tr("Поля не разрешены при создании: ${refused.joinToString()}", "These fields are not allowed on create: ${refused.joinToString()}")
        }
        validate(document, catalog)
        return request("POST", route(catalog), body = JsonArray(listOf(document)), authenticated = true).jsonArray.single().jsonObject
    }

    /**
     * PUT carries the changed fields only. The server reads the stored document, checks its own
     * version and rejects a racing write with an error — the client never retries one silently.
     */
    override suspend fun update(catalog: Catalog, id: String, changes: JsonObject): JsonObject {
        requireId(id)
        require(changes.isNotEmpty()) { tr("Нет изменений", "No changes") }
        require(changes.keys.none { it in protectedFields }) { tr("Нельзя изменять служебные поля", "Service fields cannot be changed") }
        require(changes.keys.all { it in editableFields(catalog) }) { tr("Свойство управляется сервером и недоступно для редактирования", "The property is server-owned and cannot be edited") }
        if (catalog == Catalog.EQUIPMENT) validateModifierPool(changes)
        return request("PUT", route(catalog), mapOf("id" to id), changes, authenticated = true).let {
            if (it == JsonNull) throw ApiFailure(200, null, tr("Сервер не вернул изменённый предмет", "The server returned no updated item"))
            it.jsonObject
        }
    }

    override suspend fun delete(catalog: Catalog, id: String) { requireId(id); request("DELETE", route(catalog), mapOf("id" to id), authenticated = true) }

    /**
     * One equipment template of the chosen rarity and category, drawn at random.
     *
     * Choosing which base to ask for is an input, not a calculation: which modifiers land on the
     * instance, in which tier and with which values, is decided by the server when it is created.
     */
    suspend fun randomTemplate(rarity: String = "", slot: String = "", random: kotlin.random.Random = kotlin.random.Random): JsonObject {
        require(rarity.isBlank() || rarity in com.sperance.exileforge.core.contract.rarities) { tr("Неизвестная редкость", "Unknown rarity") }
        require(slot.isBlank() || slot in com.sperance.exileforge.core.contract.slots) { tr("Неизвестная категория", "Unknown category") }
        val matching = all(route(Catalog.EQUIPMENT)).filter {
            (rarity.isBlank() || it.text("rarity") == rarity) && (slot.isBlank() || it.text("slot") == slot)
        }
        require(matching.isNotEmpty()) { tr("Нет шаблонов с такой редкостью и категорией", "No templates match that rarity and category") }
        return matching[random.nextInt(matching.size)]
    }
    suspend fun count(catalog: Catalog): JsonElement = request("GET", "${route(catalog)}/count", authenticated = true)

    // ==================== modifiers ====================

    /** Descriptions are a small, shared catalogue: the whole set is read once and kept in state. */
    suspend fun modifierDefinitions(): List<ModifierDefinition> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(ModifierDefinition.serializer()), request("GET", "api/v1/modifierdefinition", authenticated = true))
    suspend fun modifierTiers(modifierId: String): List<ModifierTier> {
        requireId(modifierId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(ModifierTier.serializer()), request("GET", "api/v1/modifiertier/byModifier", mapOf("modifierId" to modifierId), authenticated = true))
    }

    // ==================== progression and the skill tree ====================

    /** The classes the world offers. A character references one; its base is never copied here. */
    suspend fun characterClasses(): List<CharacterClass> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(CharacterClass.serializer()),
            request("GET", "api/v1/${EntitySource.CHARACTER_CLASS.path}", authenticated = true))

    /** The progression table: when a level is reached and how many skill points it hands over. */
    suspend fun experienceLevels(): List<ExperienceLevel> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(ExperienceLevel.serializer()),
            request("GET", "api/v1/${EntitySource.EXPERIENCE_LEVEL.path}", authenticated = true))
            .sortedBy { it.level }

    /** The whole shared tree. It is one seeded graph, so it is read once and drawn from memory. */
    suspend fun skillTree(): List<SkillTreeNode> =
        WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(SkillTreeNode.serializer()),
            request("GET", "api/v1/${EntitySource.SKILL_NODE.path}", authenticated = true))

    /**
     * The character's own tree.
     *
     * Since 0.12.0 the taken nodes live inside the character document rather than a collection of
     * their own, so the whole tree travels under `character/skilltree` with the character.
     */
    suspend fun characterTree(characterId: String): SkillTreeState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(request("GET", "$TREE/state",
            mapOf("characterId" to characterId), authenticated = true))
    }

    /**
     * Takes, gives back or drops tree nodes; every one of them answers with the whole tree state.
     *
     * Which node may be taken, whether a refund would leave the rest hanging and what a node costs
     * are the server's rules: the client names a node and reports the refusal it gets.
     */
    suspend fun allocateNode(characterId: String, nodeCode: String): SkillTreeState = node("allocate", characterId, nodeCode)
    suspend fun refundNode(characterId: String, nodeCode: String): SkillTreeState = node("refund", characterId, nodeCode)
    suspend fun resetTree(characterId: String): SkillTreeState {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(request("POST", "$TREE/reset",
            mapOf("characterId" to characterId), authenticated = true))
    }
    private suspend fun node(operation: String, characterId: String, nodeCode: String): SkillTreeState {
        requireId(characterId)
        require(nodeCode.isNotBlank()) { tr("Выберите узел дерева", "Choose a node of the tree") }
        return WireJson.decodeFromJsonElement(request("POST", "$TREE/$operation",
            mapOf("characterId" to characterId, "nodeCode" to nodeCode), authenticated = true))
    }

    // ==================== localisation ====================

    /**
     * The locale manifest and one dictionary.
     *
     * These are static resources, not API routes: they come back as plain JSON with no
     * `{success, data}` envelope around them, so they are fetched rather than requested. Nor do
     * they need an account — a language has to be readable before anyone has signed in.
     */
    suspend fun localeManifest(): LocaleManifest =
        WireJson.decodeFromJsonElement(fetch("locale/index.json"))

    /**
     * One language's dictionary, tagged with the fingerprint the manifest gave it.
     *
     * The fingerprint travels with the bundle rather than being looked up again later: that is what
     * lets a stored dictionary be reused without downloading it to compare.
     */
    suspend fun localeBundle(language: LocaleLanguage): LocaleBundle =
        LocaleBundle.parse(language.code, language.hash, localeDocument(language.code))

    /** The dictionary as it was served, so a caller can store the very text it parsed. */
    suspend fun localeDocument(code: String): String {
        require(code.isNotBlank()) { tr("Не указан язык", "No language given") }
        return fetchText("locale/$code.json")
    }

    // ==================== icons ====================

    /**
     * The icon manifest and the set itself.
     *
     * Static content like the dictionaries: plain JSON, no envelope, no account. The manifest
     * carries the fingerprint the server computed from the file, so a set that was edited is
     * always noticed and one that was not is never downloaded twice.
     */
    suspend fun iconManifest(): IconManifest = WireJson.decodeFromJsonElement(fetch("icons/index.json"))

    /** The set as it was served, so a caller can store the very text it parsed. */
    suspend fun iconDocument(file: String): String {
        require(file.isNotBlank()) { tr("Не указан файл иконок", "No icon file given") }
        return fetchText("icons/$file")
    }

    // ==================== auction ====================

    /**
     * The showcase, narrowed and paged by the server.
     *
     * This is the one list the server filters itself: every field the filter compares is a snapshot
     * the lot carries, so the whole search is a single query. Nothing is narrowed here afterwards.
     */
    suspend fun auctionSearch(characterId: String, filter: AuctionFilter, page: Int): AuctionPage {
        requireId(characterId); requirePage(page)
        return WireJson.decodeFromJsonElement(request("GET", "$AUCTION/search",
            mapOf("characterId" to characterId, "page" to page.toString(), "size" to AUCTION_PAGE_SIZE.toString()) + filter.query(),
            authenticated = true))
    }

    /** Everything the character ever listed, open and closed alike — the lots are their history. */
    suspend fun myLots(characterId: String): List<AuctionLot> {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(AuctionLot.serializer()),
            request("GET", "$AUCTION/my", mapOf("characterId" to characterId), authenticated = true))
    }

    /**
     * Lists an item. The price is always counted in orbs, so [priceOrbId] must be a `CURRENCY`
     * document — the server refuses anything else rather than inventing a conversion.
     *
     * An equipment instance has to be off the character first: while it is listed the goods live
     * in the lot, and a worn item cannot be in two places.
     */
    suspend fun sellEquipment(characterId: String, inventoryId: String, priceOrbId: String, price: Long): AuctionLot {
        requireId(inventoryId)
        return sell("equipment", characterId, priceOrbId, price, mapOf("inventoryId" to inventoryId))
    }
    suspend fun sellItem(characterId: String, itemId: String, amount: Long, priceOrbId: String, price: Long): AuctionLot {
        requireId(itemId)
        require(amount > 0) { tr("Количество должно быть больше нуля", "The amount must be greater than zero") }
        return sell("item", characterId, priceOrbId, price, mapOf("itemId" to itemId, "amount" to amount.toString()))
    }
    private suspend fun sell(what: String, characterId: String, priceOrbId: String, price: Long, extra: Map<String, String>): AuctionLot {
        requireId(characterId); requireId(priceOrbId)
        require(price > 0) { tr("Цена должна быть больше нуля", "The price must be greater than zero") }
        return WireJson.decodeFromJsonElement(request("POST", "$AUCTION/sell/$what",
            extra + mapOf("characterId" to characterId, "priceOrbId" to priceOrbId, "price" to price.toString()),
            authenticated = true))
    }

    /**
     * Buys a lot, or takes one back off the showcase.
     *
     * Payment, delivery and closing the lot are one server transaction, so a buyer short of orbs
     * loses neither the orbs nor the goods. The client never checks the balance itself.
     */
    suspend fun buyLot(characterId: String, lotId: String): AuctionLot = lot("buy", characterId, lotId)
    suspend fun cancelLot(characterId: String, lotId: String): AuctionLot = lot("cancel", characterId, lotId)
    private suspend fun lot(operation: String, characterId: String, lotId: String): AuctionLot {
        requireId(characterId); requireId(lotId)
        return WireJson.decodeFromJsonElement(request("POST", "$AUCTION/$operation",
            mapOf("characterId" to characterId, "lotId" to lotId), authenticated = true))
    }

    // ==================== character ====================

    suspend fun character(id: String): CharacterSummary {
        val document = get(Catalog.CHARACTERS, id) ?: error(tr("Персонаж недоступен", "The character is unavailable"))
        return WireJson.decodeFromJsonElement(document)
    }

    /**
     * The characters one account owns — what the character menu offers.
     *
     * The server narrows this itself rather than the client reading the whole collection: a player
     * has at most a handful, and nobody else's characters need to leave the server to show them.
     */
    suspend fun charactersOf(userId: String): List<CharacterSummary> {
        requireId(userId)
        return request("GET", "api/v1/character/byUser", mapOf("userId" to userId), authenticated = true)
            .jsonArray.map { WireJson.decodeFromJsonElement(it) }
    }

    suspend fun inventory(characterId: String): List<EquipmentInstance> =
        instances("api/v1/character/inventory/equipments", characterId)
    /**
     * The character sheet.
     *
     * Since 0.10.0 this is an object, not a flat map: alongside the numbers the server reports the
     * equipped items it counted and the ones it refused, with the requirement each of them misses.
     * An item whose requirements stopped being met keeps its slot and stops working — that verdict
     * is the server's and arrives here already made.
     */
    suspend fun stats(characterId: String): CharacterSheet {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(request("GET", "api/v1/character/inventory/stats", mapOf("characterId" to characterId), authenticated = true))
    }

    /** Grants experience; the server decides whether that crosses a level threshold. */
    suspend fun addExperience(characterId: String, amount: Double): CharacterSummary {
        requireId(characterId)
        require(amount > 0 && amount.isFinite()) { tr("Опыт должен быть положительным числом", "Experience must be a positive number") }
        return WireJson.decodeFromJsonElement(request("POST", "api/v1/character/inventory/experience",
            mapOf("characterId" to characterId, "amount" to amount.toString()), authenticated = true))
    }
    suspend fun bag(characterId: String): List<CharacterItem> {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(CharacterItem.serializer()), request("GET", "api/v1/character/inventory/items", mapOf("characterId" to characterId), authenticated = true))
    }
    /** Adds or removes stacking items; a negative amount removes them. Answers with a status word. */
    suspend fun adjustItems(characterId: String, items: List<ItemStack>): String {
        requireId(characterId)
        require(items.isNotEmpty()) { tr("Список предметов пуст", "The item list is empty") }
        val body = JsonArray(items.map { buildJsonObject { put("itemId", it.itemId); put("amount", it.amount) } })
        return request("POST", "api/v1/character/inventory/addItem", mapOf("characterId" to characterId), body, authenticated = true).jsonPrimitive.content
    }

    /**
     * Creates one instance of a template in the character's inventory.
     *
     * The rolls belong to the server: it picks prefixes and suffixes in the count the rarity allows
     * and a tier inside each. The client only names the template.
     */
    suspend fun grant(characterId: String, equipmentId: String): EquipmentInstance {
        requireId(characterId); requireId(equipmentId)
        return WireJson.decodeFromJsonElement(request("POST", "api/v1/character/inventory/itemToInventory",
            mapOf("characterId" to characterId, "equipmentId" to equipmentId), authenticated = true))
    }

    suspend fun equip(characterId: String, inventoryId: String): EquipmentInstance = wear("equip", characterId, inventoryId)
    suspend fun unequip(characterId: String, inventoryId: String): EquipmentInstance = wear("unequip", characterId, inventoryId)
    private suspend fun wear(operation: String, characterId: String, inventoryId: String): EquipmentInstance {
        requireId(characterId); requireId(inventoryId)
        return WireJson.decodeFromJsonElement(request("POST", "api/v1/characterequipment/$operation",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId), authenticated = true))
    }
    private suspend fun instances(path: String, characterId: String): List<EquipmentInstance> {
        requireId(characterId)
        return WireJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(EquipmentInstance.serializer()),
            request("GET", path, mapOf("characterId" to characterId), authenticated = true))
    }

    // ==================== currency ====================

    /**
     * Every currency orb the server serves, read out of the shared `items` collection.
     *
     * An orb is an ordinary stacking item filed under one category, so the catalogue is whatever the
     * server seeded: the client filters by that category and never carries a list of its own.
     */
    suspend fun currencyOrbs(): List<CurrencyItem> =
        all("api/v1/${Catalog.ITEMS.path}").filter { it.text("category") == CURRENCY_CATEGORY }
            .map { WireJson.decodeFromJsonElement(CurrencyItem.serializer(), it) }
            .sortedBy { it.price }

    /**
     * Spends one orb of the character's on one item of their inventory.
     *
     * What the orb does is entirely the server's: it checks the rarity the orb demands, rolls new
     * affixes, tiers and values, and answers with the item as it now stands plus a sentence saying
     * what happened. The orb is debited in the same transaction, so a refusal costs nothing.
     */
    suspend fun applyOrb(characterId: String, inventoryId: String, orbItemId: String): OrbOutcome {
        requireId(characterId); requireId(inventoryId); requireId(orbItemId)
        return WireJson.decodeFromJsonElement(request("POST", "api/v1/characterequipment/applyOrb",
            mapOf("characterId" to characterId, "inventoryId" to inventoryId, "orbItemId" to orbItemId), authenticated = true))
    }

    // ==================== recipes and codes ====================

    suspend fun recipe(id: String): RecipeDocument { requireId(id); return WireJson.decodeFromJsonElement(request("GET", "api/v1/recipe", mapOf("id" to id), authenticated = true)) }
    suspend fun useRecipe(characterId: String, recipeId: String, command: UseRecipeCommand): JsonElement {
        requireId(characterId); requireId(recipeId)
        command.ingridientsId.forEach(::requireId)
        return request("POST", "api/v1/recipe/useRecipe", mapOf("characterId" to characterId, "recipeId" to recipeId), WireJson.encodeToJsonElement(command), authenticated = true)
    }
    suspend fun redeem(characterId: String, code: String): JsonElement {
        requireId(characterId)
        require(code.isNotBlank()) { tr("Введите промокод", "Enter a promo code") }
        return request("POST", "api/v1/redemptioncodes/useRedeptionCode", mapOf("characterId" to characterId, "redemptionCode" to code.trim()), authenticated = true)
    }

    // ==================== transport ====================

    /**
     * A plain JSON file from the server, outside the API envelope.
     *
     * Only the locale files are served this way. It still goes through the journal, because a
     * missing dictionary is exactly the kind of thing that has to be visible when text turns into
     * raw keys on screen.
     */
    private suspend fun fetch(path: String): JsonElement = WireJson.parseToJsonElement(fetchText(path))

    /** The same file as text, so a dictionary can be stored verbatim and parsed again offline. */
    private suspend fun fetchText(path: String): String {
        val url = base.newBuilder().addPathSegments(path).build()
        val start = System.nanoTime()
        var status: Int? = null
        var responseText = ""
        var success = false
        try {
            val payload = client.newCall(Request.Builder().url(url).header("Accept", "application/json").get().build()).awaitPayload()
            status = payload.status
            responseText = payload.body.take(2_000)
            if (status !in 200..299) throw ApiFailure(status, null, tr("HTTP $status: файл не получен", "HTTP $status: the file was not served"))
            try { withContext(Dispatchers.Default) { WireJson.parseToJsonElement(payload.body) } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { throw ApiFailure(status, null, tr("Некорректный JSON в $path", "Malformed JSON in $path")) }
            success = true
            return payload.body
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (e is ApiFailure) status = e.status
            if (responseText.isBlank()) responseText = e.message.orEmpty()
            throw e
        } finally {
            journal.add(RequestLog("GET", url.encodedPath, status, (System.nanoTime() - start) / 1_000_000, "", responseText.take(400), success))
        }
    }

    private suspend fun request(method: String, path: String, query: Map<String, String> = emptyMap(), body: JsonElement? = null, authenticated: Boolean = false, sensitive: Boolean = false): JsonElement {
        if (authenticated) require(account != null) { tr("Войдите во вкладке «Аккаунт»", "Sign in on the Account tab") }
        val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        val bodyText = body?.toString().orEmpty()
        // Several server commands are POSTs carrying their arguments in the query string; OkHttp
        // still demands a body for those methods, so an empty one stands in for "no payload".
        val payload = body?.toString()?.toRequestBody(JsonMedia)
            ?: if (method in setOf("POST", "PUT", "PATCH")) "".toRequestBody(JsonMedia) else null
        val request = Request.Builder().url(url).header("Accept", "application/json").method(method, payload).build()
        val start = System.nanoTime()
        var status: Int? = null
        var responseText = ""
        var success = false
        try {
            val payload = client.newCall(request).awaitPayload()
            status = payload.status
            if (status == 401 && authenticated) { logout(); onUnauthorized() }
            val raw = payload.body
            responseText = raw.take(12_000)
            val envelope = try { withContext(Dispatchers.Default) { WireJson.parseToJsonElement(raw).jsonObject } }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { throw ApiFailure(status, null, if (status == 401) tr("Сессия истекла. Войдите снова", "The session has expired. Sign in again") else if (status == 403) tr("Недостаточно прав", "Not enough permissions") else tr("HTTP $status: пустой или некорректный JSON ответ сервера", "HTTP $status: empty or malformed JSON response")) }
            if (status !in 200..299 || (envelope["success"] as? JsonPrimitive)?.booleanOrNull != true) {
                val error = envelope["error"] as? JsonObject
                throw ApiFailure(status, error?.text("errorCode"), error?.text("message")?.takeIf { it.isNotBlank() } ?: tr("HTTP $status: операция отклонена", "HTTP $status: the operation was rejected"))
            }
            success = true
            return envelope["data"] ?: JsonNull
        } catch (e: CancellationException) {
            responseText = tr("Запрос отменён. Результат записи следует проверить на сервере.", "The request was cancelled. Verify the write on the server."); throw e
        } catch (e: Exception) {
            if (e is ApiFailure) status = e.status
            if (responseText.isBlank()) responseText = e.message.orEmpty()
            throw e
        } finally {
            journal.add(RequestLog(method, url.encodedPath + (url.encodedQuery?.let { "?${if (sensitive) tr("[скрыто]", "[hidden]") else it}" } ?: ""), status,
                (System.nanoTime() - start) / 1_000_000, if (sensitive) tr("[скрыто]", "[hidden]") else bodyText.take(12_000), if (sensitive) tr("[скрыто]", "[hidden]") else responseText, success))
        }
    }
}
