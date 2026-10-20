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
import com.sperance.exileforge.core.model.hero.PlayerCard
import com.sperance.exileforge.core.model.hero.SellBatch
import com.sperance.exileforge.core.model.hero.SellOutcome
import com.sperance.exileforge.core.model.hero.ServerClock
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
     * непрочитанной почты [unread]: новое письмо не прячется за 304; часть по часам, чей срок прошёл, - тоже ([HeroParts.conditional]).
     */
    suspend fun view(heroId: String, parts: HeroParts, unread: Int = 0): HeroSnapshot? {
        val headers = buildMap {
            put(HeroParts.HEADER, parts.header())
            if (parts.conditional(ServerClock.now())) put("If-None-Match", "\"${parts.version}.$unread\"")
        }
        val answer = http.request("GET", "$HERO/view", heroQuery(heroId), authenticated = true, headers = headers)
        return if (answer is JsonNull) null else WireJson.decodeFromJsonElement(HeroSnapshot.serializer(), answer)
    }

    /** Every figure of the hero's statistics: only what is not zero. */
    suspend fun stats(heroId: String): com.sperance.exileforge.core.model.hero.HeroStatsView = http.get("$HERO/stats", heroQuery(heroId))

    /** The unique templates the hero has found, each with its first find and count. */
    suspend fun uniques(heroId: String): com.sperance.exileforge.core.model.hero.UniquesView = http.get("$HERO/uniques", heroQuery(heroId))

    /** Герои аккаунта и их действующие санкции одним запросом - что предлагает меню героев. */
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

    /** Стирание героя администратором (общий `DELETE hero` - только ему); игрок удаляет своего через [markDeletion]. */
    suspend fun delete(heroId: String) {
        requireId(heroId)
        http.request("DELETE", HERO, mapOf("id" to heroId), authenticated = true)
    }

    /**
     * Что случится при удалении своего героя [heroId]: гильдия, лоты и срок, пока его можно вернуть.
     *
     * Самоудаление: корзина на `selfDeleteDays` дней; героя запрос называет `hero`.
     */
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

    /** Карточка игрока: герой [target] глазами своего героя [heroId], только на его игровом сервере. */
    suspend fun card(heroId: String, target: String): PlayerCard {
        requireId(target)
        return http.get("$HERO/card", heroQuery(heroId, "target" to target))
    }

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
     * The testing window: one grant [what] of `/hero/grant/` — gold, level, points, zones, rares, a map,
     * professions, recipes, a reset — for a tester's own hero or by an administrator; the server answers with the hero's level.
     */
    suspend fun grant(heroId: String, what: String, vararg params: Pair<String, String?>) {
        http.request("POST", "$HERO/grant/$what", heroQuery(heroId, *params), authenticated = true)
    }

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

    /** Sells an item to a merchant for gold; the copy is gone when this returns. [unequip] - надетую сервер снимает и продаёт одной записью. */
    suspend fun sell(heroId: String, itemId: String, unequip: Boolean = false): SellOutcome {
        requireItemId(itemId)
        return http.post("$HERO/sell", heroQuery(heroId, "itemId" to itemId, "unequip" to "true".takeIf { unequip }))
    }

    /**
     * Продажа пачкой: вещи тайника телом запроса. Сервер отказывает всей пачке, если хоть одна
     * надета, заперта или не уходит пачкой.
     */
    suspend fun sellMany(heroId: String, itemIds: List<String>): SellBatch {
        require(itemIds.isNotEmpty()) { ui("api.choose_item") }
        itemIds.forEach(::requireItemId)
        return http.post("$HERO/sell/batch", heroQuery(heroId), body = WireJson.encodeToJsonElement(itemIds))
    }

    /**
     * Locks or unlocks one item, in the stash or worn: a locked item is never sold, listed or
     * auto-sold from the overflow; orbs and the bench still work on it.
     */
    suspend fun lock(heroId: String, itemId: String, locked: Boolean) {
        requireItemId(itemId)
        http.request("POST", "$HERO/item/lock", heroQuery(heroId, "itemId" to itemId, "locked" to locked.toString()), authenticated = true)
    }

    /** One row of the loot filter: the slot groups of [rarity] the merchant takes at once; none clears it. */
    suspend fun autoSell(heroId: String, rarity: Rarity, groups: Set<SlotGroup>): AutoSell = http.post("$HERO/autosell", heroQuery(heroId, "rarity" to rarity.name, "groups" to groups.joinToString(",") { it.name }))

    /** Правило фильтра добычи «ниже героя»: вещи редкостей [rarities] на [levels] уровней и более ниже героя; ноль выключает. */
    suspend fun autoSellBelow(heroId: String, levels: Int, rarities: Set<Rarity>): AutoSell = http.post("$HERO/autosell/below", heroQuery(heroId, "levels" to levels.toString(), "rarities" to rarities.joinToString(",") { it.name }))

    /** Правило фильтра добычи «всё, что герой не может носить». */
    suspend fun autoSellUnwearable(heroId: String, on: Boolean): AutoSell = http.post("$HERO/autosell/unwearable", heroQuery(heroId, "on" to on.toString()))

    /** Wears the title [title] beside the name — one the chronicle has earned — or takes it off when blank. */
    suspend fun setTitle(heroId: String, title: String): String = http.post("$HERO/title", heroQuery(heroId, "title" to title))

    /** One more pack of stash places, for the rules' gold; a repeat goes only under the same key ([SafeRetry]) and never buys twice. */
    suspend fun expandStash(heroId: String): StashState = http.post("$HERO/stash/expand", heroQuery(heroId))

    /** Takes [itemId] out of the overflow into the stash — or, without it, as many as fit, in order. */
    suspend fun claimOverflow(heroId: String, itemId: String? = null): StashState = http.post("$HERO/stash/claim", heroQuery(heroId, "itemId" to itemId))

    /** Sells an item of the overflow to the merchant without taking it in. */
    suspend fun sellOverflow(heroId: String, itemId: String): StashState {
        requireItemId(itemId)
        return http.post("$HERO/stash/sell", heroQuery(heroId, "itemId" to itemId))
    }

    /** Tempers a weapon or armour with the smith's ore: quality and item level up, once per item. Repeated only under its key. */
    suspend fun temper(heroId: String, itemId: String): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/temper", heroQuery(heroId, "itemId" to itemId))
    }

    /**
     * Spends one orb of the bag on one item; [orb] is the orb's item code, with an [omen] spent along with it when given.
     * [line] - номер строки в `item.rolls`, что гравирует Сфера гравёра; прочим сферам - null. [count] - пачка сферы качества: сервер
     * применяет её столько раз, но не выше потолка качества; прочие сферы - только по одной.
     */
    suspend fun applyOrb(heroId: String, itemId: String, orb: String, omen: String? = null, line: Int? = null, count: Int = 1): CurrencyApplyResponse {
        requireItemId(itemId)
        require(orb.isNotBlank()) { ui("api.choose_orb") }
        return http.post("$HERO/orb", heroQuery(heroId, "itemId" to itemId, "orb" to orb, "omen" to omen?.takeIf { it.isNotBlank() }, "line" to line?.toString(), "count" to count.takeIf { it > 1 }?.toString()))
    }

    /** Печать Атласа [seal] на карту [itemId] со знамением [omen], если печать его берёт; ответ - как у сферы. */
    suspend fun applySeal(heroId: String, itemId: String, seal: String, omen: String? = null): CurrencyApplyResponse {
        requireItemId(itemId)
        require(seal.isNotBlank()) { ui("api.choose_seal") }
        return http.post("$HERO/seal", heroQuery(heroId, "itemId" to itemId, "seal" to seal, "omen" to omen?.takeIf { it.isNotBlank() }))
    }

    /** The unveiling's choice: which of the offered modifiers takes the veiled one's place. */
    suspend fun unveil(heroId: String, itemId: String, choice: Int): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/unveil", heroQuery(heroId, "itemId" to itemId, "choice" to choice.toString()))
    }

    /** The Omen of Choice's line kept: [choice] of the ones an Orb of Alchemy or an Exalted Orb offered. */
    suspend fun choose(heroId: String, itemId: String, choice: Int): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/choose", heroQuery(heroId, "itemId" to itemId, "choice" to choice.toString()))
    }

    suspend fun applyEssence(heroId: String, itemId: String, essence: String): CurrencyApplyResponse {
        requireItemId(itemId)
        require(essence.isNotBlank()) { ui("api.choose_orb") }
        return http.post("$HERO/essence", heroQuery(heroId, "itemId" to itemId, "essence" to essence))
    }

    suspend fun craft(heroId: String, itemId: String, recipe: String): CurrencyApplyResponse {
        requireItemId(itemId)
        require(recipe.isNotBlank()) { ui("api.choose_recipe") }
        return http.post("$HERO/craft", heroQuery(heroId, "itemId" to itemId, "recipe" to recipe))
    }

    suspend fun uncraft(heroId: String, itemId: String): CurrencyApplyResponse {
        requireItemId(itemId)
        return http.post("$HERO/uncraft", heroQuery(heroId, "itemId" to itemId))
    }

    suspend fun learnSkill(heroId: String, skill: String): HeroSkills = http.post("$HERO/skills/learn", heroQuery(heroId, "skill" to skill))

    /** Руна [rune] в гнездо [socket] умения [skill]: прежняя руна гнезда пропадает; без руны гнездо пустеет, снятая руна тоже пропадает. */
    suspend fun socketRune(heroId: String, skill: String, socket: Int, rune: String?): HeroSkills = http.post("$HERO/skills/rune", heroQuery(heroId, "skill" to skill, "socket" to socket.toString(), "rune" to rune))

    /** Opens one loot chest of the bag: the server rolls what is inside. */
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

/** Герои аккаунта и санкции по id героя: бан или удаление. */
@kotlinx.serialization.Serializable
data class HeroRoster(val heroes: List<HeroSummary> = emptyList(), val sanctions: Map<String, SanctionNotice> = emptyMap())
