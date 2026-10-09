package com.sperance.exileforge.core.network

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.contract.requireId
import com.sperance.exileforge.core.contract.requireItemId
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.CreateHeroCommand
import com.sperance.exileforge.core.model.hero.CurrencyApplyResponse
import com.sperance.exileforge.core.model.hero.DeletionConfirm
import com.sperance.exileforge.core.model.hero.DeletionPreview
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.core.model.hero.PetOrbResponse
import com.sperance.exileforge.core.model.hero.PetState
import com.sperance.exileforge.core.model.hero.PlayerCard
import com.sperance.exileforge.core.model.hero.SellBatch
import com.sperance.exileforge.core.model.hero.SellOutcome
import com.sperance.exileforge.core.model.hero.StashState
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.rules.content.AutoSell
import com.sperance.exileforge.rules.content.BenchRecipe
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.rules.roll.ItemInstance
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private const val HERO = "api/v1/hero"
private const val DELETION = "$HERO/deletion"

/**
 * One hero: what they are, what they carry and wear, and every command that changes that. Each command
 * names the hero and the thing; every rule is the server's, and a refusal comes back as it was said.
 */
class HeroClient internal constructor(private val http: Transport) {
    /**
     * The hero in one read: only the parts whose fingerprints [parts] does not hold, or `null` on a 304. В ETag и счёт
     * непрочитанной почты [unread] (3.94.1, сервер 1.81.15): новое письмо не прячется за 304.
     */
    suspend fun view(heroId: String, parts: HeroParts, unread: Int = 0): HeroSnapshot? {
        val headers = buildMap {
            put(HeroParts.HEADER, parts.header())
            if (parts.complete && parts.version.isNotBlank()) put("If-None-Match", "\"${parts.version}.$unread\"")
        }
        val answer = http.request("GET", "$HERO/view", heroQuery(heroId), authenticated = true, headers = headers)
        return if (answer is JsonNull) null else WireJson.decodeFromJsonElement(HeroSnapshot.serializer(), answer)
    }

    /** Every figure of the hero's statistics (server 1.49.0): only what is not zero. */
    suspend fun stats(heroId: String): com.sperance.exileforge.core.model.hero.HeroStatsView = http.get("$HERO/stats", heroQuery(heroId))

    /** The unique templates the hero has found (server 1.81.7), each with its first find and count. */
    suspend fun uniques(heroId: String): com.sperance.exileforge.core.model.hero.UniquesView = http.get("$HERO/uniques", heroQuery(heroId))

    /** Герои аккаунта и их действующие санкции одним запросом (3.94.1, server 1.81.15) - что предлагает меню героев. */
    suspend fun rosterOf(userId: String): HeroRoster {
        requireId(userId)
        return http.get("$HERO/byUser", mapOf("userId" to userId))
    }

    suspend fun create(userId: String, name: String, description: String, heroClass: String): HeroSummary {
        requireId(userId)
        require(name.isNotBlank()) { ui("contract.enter_name") }
        require(heroClass.isNotBlank()) { ui("api.choose_class") }
        val body = JsonArray(listOf(WireJson.encodeToJsonElement(CreateHeroCommand.serializer(), CreateHeroCommand(userId, name.trim(), description.trim(), heroClass))))
        return http.request("POST", HERO, body = body, authenticated = true).jsonArray.single().let { WireJson.decodeFromJsonElement(it) }
    }

    /** Стирание героя администратором (общий `DELETE hero`, с сервера 4.5.1 - только ему); игрок удаляет своего через [markDeletion]. */
    suspend fun delete(heroId: String) {
        requireId(heroId)
        http.request("DELETE", HERO, mapOf("id" to heroId), authenticated = true)
    }

    // ---- самоудаление (сервер 4.5.1): корзина на `selfDeleteDays` дней; героя запрос называет `hero` ----

    /** Что случится при удалении своего героя [heroId]: гильдия, лоты и срок, пока его можно вернуть. */
    suspend fun deletionPreview(heroId: String): DeletionPreview = http.get("$DELETION/preview", deletionQuery(heroId))

    /** Свой герой [heroId] - в корзину; [name] - его имя, введённое для подтверждения (`CH_040`). Ответ - герои аккаунта. */
    suspend fun markDeletion(heroId: String, name: String): HeroRoster = http.post("$DELETION/mark", deletionQuery(heroId), confirm(name))

    /** Вернуть своего героя [heroId] из самоудаления; места на аккаунте нет - `CH_005`. */
    suspend fun restoreDeletion(heroId: String): HeroRoster = http.post("$DELETION/restore", deletionQuery(heroId))

    /** Стереть своего героя [heroId] сразу и навсегда - живого или из самоудаления; [name] - его имя. */
    suspend fun eraseDeletion(heroId: String, name: String): HeroRoster = http.post("$DELETION/erase", deletionQuery(heroId), confirm(name))

    private fun deletionQuery(heroId: String): Map<String, String> {
        requireId(heroId)
        return mapOf("hero" to heroId)
    }

    private fun confirm(name: String): JsonElement {
        require(name.isNotBlank()) { ui("contract.enter_name") }
        return WireJson.encodeToJsonElement(DeletionConfirm.serializer(), DeletionConfirm(name.trim()))
    }

    /** Карточка игрока (сервер 4.5.1): герой [target] глазами своего героя [heroId], только на его игровом сервере. */
    suspend fun card(heroId: String, target: String): PlayerCard {
        requireId(target)
        return http.get("$HERO/card", heroQuery(heroId, "target" to target))
    }

    // ---- an administrator grants out of nothing ----

    suspend fun grantExperience(heroId: String, amount: Double): Int {
        require(amount > 0 && amount.isFinite()) { ui("api.xp_positive") }
        return http.request("POST", "$HERO/grant/experience", heroQuery(heroId, "amount" to amount.toString()), authenticated = true).jsonPrimitive.content.toInt()
    }

    suspend fun grantItem(heroId: String, code: String, amount: Long): Map<String, Long> {
        require(code.isNotBlank() && amount > 0) { ui("api.amount_positive") }
        return http.post("$HERO/grant/item", heroQuery(heroId, "code" to code, "amount" to amount.toString()))
    }

    suspend fun grantEquipment(heroId: String, template: String, rarity: Rarity? = null): ItemInstance {
        require(template.isNotBlank()) { ui("api.choose_template") }
        return http.post("$HERO/grant/equipment", heroQuery(heroId, "template" to template, "rarity" to rarity?.name))
    }

    /**
     * The testing window (3.73.0, server 1.69.0): one grant [what] of `/hero/grant/` — gold, level, points, zones, rares, a map,
     * professions, recipes, a reset — for a tester's own hero or by an administrator; the server answers with the hero's level.
     */
    suspend fun grant(heroId: String, what: String, vararg params: Pair<String, String?>) {
        http.request("POST", "$HERO/grant/$what", heroQuery(heroId, *params), authenticated = true)
    }

    // ---- wearing ----

    /** Puts an item on; [slot] names which ring or flask place to take, the server picks a free one without it. */
    suspend fun equip(heroId: String, itemId: String, slot: Slot? = null): ItemInstance {
        requireItemId(itemId)
        return http.post("$HERO/equip", heroQuery(heroId, "itemId" to itemId, "slot" to slot?.name))
    }
    suspend fun unequip(heroId: String, itemId: String): ItemInstance = item("unequip", heroId, itemId)
    suspend fun socket(heroId: String, itemId: String, nodeCode: String): ItemInstance {
        requireItemId(itemId)
        require(nodeCode.isNotBlank()) { ui("api.choose_socket") }
        return http.post("$HERO/socket", heroQuery(heroId, "itemId" to itemId, "nodeCode" to nodeCode))
    }
    suspend fun unsocket(heroId: String, itemId: String): ItemInstance = item("unsocket", heroId, itemId)

    /** Sells an item to a merchant for gold; the copy is gone when this returns. */
    suspend fun sell(heroId: String, itemId: String): SellOutcome {
        requireItemId(itemId)
        return http.post("$HERO/sell", heroQuery(heroId, "itemId" to itemId))
    }

    /**
     * Продажа пачкой (3.90.3, сервер 1.81.8): вещи тайника телом запроса. Сервер отказывает всей пачке, если хоть одна
     * надета, заперта или не уходит пачкой.
     */
    suspend fun sellMany(heroId: String, itemIds: List<String>): SellBatch {
        require(itemIds.isNotEmpty()) { ui("api.choose_item") }
        itemIds.forEach(::requireItemId)
        return http.post("$HERO/sell/batch", heroQuery(heroId), body = WireJson.encodeToJsonElement(itemIds))
    }

    /**
     * Locks or unlocks one item, in the stash or worn (server 1.28.0): a locked item is never sold, listed or
     * auto-sold from the overflow; orbs and the bench still work on it.
     */
    suspend fun lock(heroId: String, itemId: String, locked: Boolean) {
        requireItemId(itemId)
        http.request("POST", "$HERO/item/lock", heroQuery(heroId, "itemId" to itemId, "locked" to locked.toString()), authenticated = true)
    }

    /** One row of the loot filter (server 1.45.0): the slot groups of [rarity] the merchant takes at once; none clears it. */
    suspend fun autoSell(heroId: String, rarity: Rarity, groups: Set<SlotGroup>): AutoSell = http.post("$HERO/autosell", heroQuery(heroId, "rarity" to rarity.name, "groups" to groups.joinToString(",") { it.name }))

    /** Правило фильтра добычи «всё, что герой не может носить» (3.90.3, сервер 1.81.8). */
    suspend fun autoSellUnwearable(heroId: String, on: Boolean): AutoSell = http.post("$HERO/autosell/unwearable", heroQuery(heroId, "on" to on.toString()))

    /** Wears the title [title] beside the name — one the chronicle has earned — or takes it off when blank (server 1.3.0). */
    suspend fun setTitle(heroId: String, title: String): String = http.post("$HERO/title", heroQuery(heroId, "title" to title))

    // ---- the menagerie (server 1.5.0); none retried: a repeat would spend a second egg or orb ----

    /** Lays [egg] into the incubator (server 1.67.0): place [slot], or the first free one; its rarity and level are rolled now. */
    suspend fun incubatePet(heroId: String, egg: String, slot: Int? = null): PetState = http.post("$HERO/pets/incubate", heroQuery(heroId, "egg" to egg, "slot" to slot?.toString()))

    /** Takes the ripe egg of place [slot] out of the incubator: the pet hatches, its species and lines rolled now. */
    suspend fun collectPet(heroId: String, slot: Int): PetState = http.post("$HERO/pets/collect", heroQuery(heroId, "slot" to slot.toString()))

    /**
     * Any crafting orb on a pet (server 1.65.0), with an [omen] spent along with it when given; the pets' own growth orb too.
     * Ответ (4.4.1) - строка итога, как у сферы на вещи, и зверинец.
     */
    suspend fun petOrb(heroId: String, petId: String, orb: String, omen: String? = null): PetOrbResponse = http.post("$HERO/pets/orb", heroQuery(heroId, "petId" to petId.also(::requireItemId), "orb" to orb, "omen" to omen?.takeIf { it.isNotBlank() }))

    /** The Omen of Choice's line kept on a pet (server 1.65.0): [choice] of the ones it offers. */
    suspend fun choosePetLine(heroId: String, petId: String, choice: Int): PetState = http.post("$HERO/pets/choose", heroQuery(heroId, "petId" to petId.also(::requireItemId), "choice" to choice.toString()))
    suspend fun activatePet(heroId: String, petId: String): PetState = http.post("$HERO/pets/activate", heroQuery(heroId, "petId" to petId.also(::requireItemId)))

    /** Breeds two combat pets with the Orb of Breeding (server 1.74.0): a hybrid joins, or an egg of a parent goes to the bag. Never retried. */
    suspend fun breedPets(heroId: String, first: String, second: String): PetState = http.post("$HERO/pets/breed", heroQuery(heroId, "first" to first.also(::requireItemId), "second" to second.also(::requireItemId)))
    suspend fun sellPet(heroId: String, petId: String): PetState = http.post("$HERO/pets/sell", heroQuery(heroId, "petId" to petId.also(::requireItemId)))

    // ---- the stash's places and its overflow (server 1.1.0) ----

    /** One more pack of stash places, for the rules' gold; never retried — a repeat would buy twice. */
    suspend fun expandStash(heroId: String): StashState = http.post("$HERO/stash/expand", heroQuery(heroId))

    /** Takes [itemId] out of the overflow into the stash — or, without it, as many as fit, in order. */
    suspend fun claimOverflow(heroId: String, itemId: String? = null): StashState = http.post("$HERO/stash/claim", heroQuery(heroId, "itemId" to itemId))

    /** Sells an item of the overflow to the merchant without taking it in. */
    suspend fun sellOverflow(heroId: String, itemId: String): StashState {
        requireItemId(itemId)
        return http.post("$HERO/stash/sell", heroQuery(heroId, "itemId" to itemId))
    }

    // ---- orbs, essences, the bench ----

    /** Spends one orb of the bag on one item; [orb] is the orb's item code. */
    /** An orb on an item, with an [omen] (3.36.0) spent along with it when given. */

    /** Tempers a weapon or armour with the smith's ore (server 1.74.0): quality and item level up, once per item. Never retried. */
    suspend fun temper(heroId: String, itemId: String): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/temper", heroQuery(heroId, "itemId" to itemId))
    }

    suspend fun applyOrb(heroId: String, itemId: String, orb: String, omen: String? = null): CurrencyApplyResponse {
        requireItemId(itemId)
        require(orb.isNotBlank()) { ui("api.choose_orb") }
        return http.post("$HERO/orb", heroQuery(heroId, "itemId" to itemId, "orb" to orb, "omen" to omen?.takeIf { it.isNotBlank() }))
    }

    /** The unveiling's choice (3.36.0): which of the offered modifiers takes the veiled one's place. */
    suspend fun unveil(heroId: String, itemId: String, choice: Int): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/unveil", heroQuery(heroId, "itemId" to itemId, "choice" to choice.toString()))
    }

    /** The Omen of Choice's line kept (server 1.65.0): [choice] of the ones an Orb of Alchemy or an Exalted Orb offered. */
    suspend fun choose(heroId: String, itemId: String, choice: Int): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/choose", heroQuery(heroId, "itemId" to itemId, "choice" to choice.toString()))
    }

    suspend fun applyEssence(heroId: String, itemId: String, essence: String): CurrencyApplyResponse {
        requireItemId(itemId)
        require(essence.isNotBlank()) { ui("api.choose_orb") }
        return http.post("$HERO/essence", heroQuery(heroId, "itemId" to itemId, "essence" to essence))
    }

    /** The bench lines the hero has found on maps; the rest stay hidden. */

    suspend fun craft(heroId: String, itemId: String, recipe: String): CurrencyApplyResponse {
        requireItemId(itemId)
        require(recipe.isNotBlank()) { ui("api.choose_recipe") }
        return http.post("$HERO/craft", heroQuery(heroId, "itemId" to itemId, "recipe" to recipe))
    }

    suspend fun uncraft(heroId: String, itemId: String): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/uncraft", heroQuery(heroId, "itemId" to itemId))
    }

    // ---- the class skills ----

    suspend fun learnSkill(heroId: String, skill: String): HeroSkills = http.post("$HERO/skills/learn", heroQuery(heroId, "skill" to skill))

    /** Руна [rune] в гнездо [socket] умения [skill] (4.4.0): прежняя руна гнезда пропадает; без руны гнездо пустеет, снятая руна тоже пропадает. */
    suspend fun socketRune(heroId: String, skill: String, socket: Int, rune: String?): HeroSkills = http.post("$HERO/skills/rune", heroQuery(heroId, "skill" to skill, "socket" to socket.toString(), "rune" to rune))

    /** Opens one loot chest of the bag (3.76.0, server 1.71.0): the server rolls what is inside. */
    suspend fun openChest(heroId: String, code: String): com.sperance.exileforge.core.model.hero.ChestOpening = http.post("$HERO/chest/open", heroQuery(heroId, "code" to code))

    /** Puts a learned skill into slot [index] of [kind] — `ACTIVE` or `PASSIVE` — or empties it without [skill]. */
    suspend fun slotSkill(heroId: String, kind: String, index: Int, skill: String?, condition: String? = null): HeroSkills = http.post("$HERO/skills/slot", heroQuery(heroId, "kind" to kind, "index" to index.toString(), "skill" to skill, "condition" to condition))

    suspend fun flaskCondition(heroId: String, index: Int, condition: String?): HeroSkills = http.post("$HERO/skills/flask", heroQuery(heroId, "index" to index.toString(), "condition" to condition))

    suspend fun exchangeBooks(heroId: String, books: List<String>, skill: String): HeroSkills {
        require(books.isNotEmpty() && skill.isNotBlank()) { ui("skills.choose_books") }
        return http.post("$HERO/skills/exchange", heroQuery(heroId, "books" to books.joinToString(","), "skill" to skill))
    }

    /** Redeems a promo code for the hero; the reward lands in the snapshot that comes back with the answer. */
    suspend fun redeem(heroId: String, code: String): String {
        require(code.isNotBlank()) { ui("api.enter_promo") }
        return http.request("POST", "api/v1/redemptioncodes/redeem", heroQuery(heroId, "code" to code.trim()), authenticated = true).jsonPrimitive.content
    }

    private suspend fun item(operation: String, heroId: String, itemId: String): ItemInstance {
        requireItemId(itemId)
        return http.post("$HERO/$operation", heroQuery(heroId, "itemId" to itemId))
    }
}

/** Герои аккаунта (3.94.1, server 1.81.15) и санкции по id героя: бан или удаление. */
@kotlinx.serialization.Serializable
data class HeroRoster(val heroes: List<HeroSummary> = emptyList(), val sanctions: Map<String, SanctionView> = emptyMap())
