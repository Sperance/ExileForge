package com.sperance.exileforge.presentation.hero

import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.GameEvents
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.Dice

/**
 * Команды героя (3.80.15): сундук, кузница, гримуар, древо, зверинец, выдачи тестеру. Каждая отвечает героем, как
 * его теперь держит сервер, так что из ответа ничего не латается - кроме ответа без снимка, который `HeroSync`
 * отмечает холодным чтением. Общие для всех экранов героя.
 */
class HeroActions(
    private val heroes: HeroRepository,
    private val sync: HeroSync,
    private val sessions: SessionRepository,
    private val world: WorldRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val notices: Notices,
    private val events: GameEvents,
    private val buzzes: Buzzes,
) {
    private val api: GameApi get() = connection.api

    /** Предмет под кузницей: тот, на который укажет следующая сфера. */
    fun selectEquipment(itemId: String) = heroes.selectEquipment(itemId)

    fun equip(itemId: String, slot: Slot? = null) {
        buzzes.buzz(Buzz.BUTTON)
        heroCommand { id -> api.hero.equip(id, itemId, slot) }
    }
    fun unequip(itemId: String) = heroCommand { id -> api.hero.unequip(id, itemId) }

    /** Титул рядом с именем (1.3.0): открытый хроникой, или никакой. */
    fun setTitle(title: String) = heroCommand { id -> api.hero.setTitle(id, title) }

    /** Замок на предмете (3.30.0): запертый не продаётся, не выставляется и не уходит автопродажей; флаг едет в снимке. */
    fun lockItem(itemId: String, locked: Boolean) = heroCommand { id -> api.hero.lock(id, itemId, locked) }

    /** Ещё пачка мест сундука за золото (1.1.0); снимок с ответом несёт новый счёт. */
    fun expandStash() = heroCommand { id -> api.hero.expandStash(id) }

    /** Из переполнения в сундук: [itemId], или сколько влезет. */
    fun claimOverflow(itemId: String? = null) = heroCommand { id -> api.hero.claimOverflow(id, itemId) }
    fun sellOverflow(itemId: String) = heroCommand { id -> api.hero.sellOverflow(id, itemId) }

    // Зверинец (3.5.0): снимок с каждым ответом несёт питомцев и сумку.
    fun incubatePet(egg: String, slot: Int? = null) = heroCommand { id -> api.hero.incubatePet(id, egg, slot) }
    fun collectPet(slot: Int) = heroCommand { id -> api.hero.collectPet(id, slot) }

    /** Сфера ремесла на питомце (сервер 1.65.0), с предзнаменованием, если оно положено. */
    fun petOrb(petId: String, orb: String, omen: String? = null) = heroCommand { id -> api.hero.petOrb(id, petId, orb, omen) }
    fun choosePetLine(petId: String, choice: Int) = heroCommand { id -> api.hero.choosePetLine(id, petId, choice) }
    fun activatePet(petId: String) = heroCommand { id -> api.hero.activatePet(id, petId) }
    fun releasePet(petId: String) = heroCommand { id -> api.hero.releasePet(id, petId) }

    /** Разведение (3.79.0): рождённое названо - гибрид по виду, иначе яйцо, ушедшее в сумку. */
    fun breedPets(first: String, second: String) = heroCommand { id ->
        val before = heroes.state.value.hero?.pets?.pets?.map { it.id }?.toSet().orEmpty()
        val after = api.hero.breedPets(id, first, second)
        val born = after.pets.firstOrNull { it.id !in before }
        notices.toast(born?.let { ui("pets.bred_hybrid", locOr("pet.${it.species}", it.species)) } ?: ui("pets.bred_egg"), NoticeKind.LOOT)
    }

    /** Окно тестера (3.73.0): одна выдача `/hero/grant/` герою в игре. */
    fun testerGrant(what: String, vararg params: Pair<String, String?>) = heroCommand { id ->
        check(sessions.state.value.isTester) { ui("hero.grant_admin_only") }
        api.hero.grant(id, what, *params)
        sync.readHero()
    }

    /** Тестеру или админу: названный шаблон герою, брошенный сервером. */
    fun grant(template: String, rarity: Rarity? = null) = heroCommand { id ->
        check(sessions.state.value.isTester) { ui("hero.grant_admin_only") }
        api.hero.grantEquipment(id, template, rarity)
    }

    /** Админу: случайный шаблон выбранной редкости и слота - выбран здесь из контента, брошен сервером. */
    fun grantRandom(rarityCode: String, slotCode: String) = heroCommand { id ->
        check(sessions.state.value.isTester) { ui("hero.grant_admin_only") }
        val index = world.state.value.content ?: error(ui("runtime.request_failed"))
        val rarity = Rarity.of(rarityCode)
        val slot = Slot.of(slotCode)
        val candidates = index.templates.values.filter { (slot == null || it.slot == slot) && (rarity == null || rarity.fixed == it.unique) }
        check(candidates.isNotEmpty()) { ui("hero.no_template") }
        api.hero.grantEquipment(id, Dice.system().pick(candidates).code, rarity)
    }

    /** Админу: стопка в сумку. */
    fun grantItem(code: String, amount: Long) = heroCommand { id ->
        check(sessions.state.value.isTester) { ui("hero.bag_admin_only") }
        api.hero.grantItem(id, code, amount)
    }

    /** Одна сфера на один предмет: применима ли и что перебрасывает - дело правил; фраза возвращается. [omen] тратится с ней. */
    fun applyOrb(itemId: String, orb: String, omen: String, onApplied: () -> Unit = {}) = forgeCommand { id ->
        val outcome = api.hero.applyOrb(id, itemId, orb, omen)
        heroes.forgeLine(outcome.message)
        heroes.selectEquipment(outcome.created?.id ?: outcome.item.id)
        onApplied()
    }

    /** Закалка кузнеца (3.79.0): фраза о том, что поднялось, уходит тостом. */
    fun temper(itemId: String) = forgeCommand { id -> notices.toast(api.hero.temper(id, itemId).message, NoticeKind.CRAFT) }

    fun unveil(itemId: String, choice: Int) = forgeCommand { id -> heroes.forgeLine(api.hero.unveil(id, itemId, choice).message) }

    /** Строка Предзнаменования выбора оставлена (сервер 1.65.0). */
    fun choose(itemId: String, choice: Int) = forgeCommand { id -> heroes.forgeLine(api.hero.choose(id, itemId, choice).message) }

    fun craft(itemId: String, recipe: String) = forgeCommand { id -> heroes.forgeLine(api.hero.craft(id, itemId, recipe).message) }
    fun uncraft(itemId: String) = forgeCommand { id -> heroes.forgeLine(api.hero.uncraft(id, itemId).message) }

    fun applyEssence(itemId: String, essence: String) = forgeCommand { id ->
        val outcome = api.hero.applyEssence(id, itemId, essence)
        heroes.forgeLine(outcome.message)
        heroes.selectEquipment(outcome.item.id)
    }

    /** Гримуар: книга прочитана, навык вставлен или вынут, условие ячейки или флакона, книги обменяны на одну. */
    fun learnSkill(code: String) = heroCommand { id -> api.hero.learnSkill(id, code) }

    /** Сундук с добычей вскрыт (3.76.0): добыча на экране, пока её не закрыли. */
    fun openChest(code: String) = heroCommand { id -> heroes.chest(api.hero.openChest(id, code)) }
    fun dismissChest() = heroes.chest(null)
    fun slotSkill(kind: String, index: Int, code: String?, condition: String? = null) = heroCommand { id -> api.hero.slotSkill(id, kind, index, code, condition) }
    fun flaskCondition(index: Int, condition: String?) = heroCommand { id -> api.hero.flaskCondition(id, index, condition) }
    fun exchangeBooks(books: List<String>, code: String) = heroCommand { id -> api.hero.exchangeBooks(id, books, code) }

    /** Древо навыков: взять узел, вернуть его или сбросить всё древо. Каждое правило - серверное. */
    fun allocateNode(code: String, choice: Int? = null) = heroCommand { id -> api.tree.allocate(id, code, choice) }
    fun allocatePath(code: String, choice: Int? = null) = heroCommand { id -> api.tree.path(id, code, choice) }
    fun refundNode(code: String) = heroCommand { id -> api.tree.refund(id, code) }
    fun refundBranch(code: String) = heroCommand { id -> api.tree.refundBranch(id, code) }
    fun rechooseNode(code: String, choice: Int) = heroCommand { id -> api.tree.rechoose(id, code, choice) }
    fun resetTree() = heroCommand { id -> api.tree.reset(id) }
    fun autoSell(rarity: Rarity, groups: Set<SlotGroup>) = heroCommand { id -> api.hero.autoSell(id, rarity, groups) }

    /** Админу: опыт герою, уровень решает сервер. */
    fun addExperience(amount: Double) = heroCommand { id ->
        check(sessions.state.value.isAdmin) { ui("hero.xp_admin_only") }
        api.hero.grantExperience(id, amount)
    }

    fun redeem(code: String) = heroCommand { id ->
        notices.toast(ui("redemption.redeemed"))
        api.hero.redeem(id, code)
    }

    fun socketJewel(itemId: String, nodeCode: String) = heroCommand { id ->
        // Один уникальный самоцвет вида на героя (сервер 1.31.0): общее правило, спрошенное прежде отказа сервера.
        val hero = heroes.state.value.hero
        val item = hero?.item(itemId)
        val index = world.state.value.content
        if (hero != null && item != null && index != null) check(hero.jewelFree(index, item)) { ui("tree.jewel_unique_taken") }
        api.hero.socket(id, itemId, nodeCode)
    }
    fun unsocketJewel(itemId: String) = heroCommand { id -> api.hero.unsocket(id, itemId) }

    /** Продажа торговцу: цену ставит и платит сервер; карточка показала ту же сумму заранее. */
    fun sellForGold(itemId: String) = heroCommand { id -> notices.toast(ui("toast.sold", api.hero.sell(id, itemId).gold)) }

    /** Одна команда героя: владелец или админ; ответ без снимка перечитывается; поход берёт новое снаряжение, когда герой нарисован. */
    private fun heroCommand(block: suspend (String) -> Unit) = commands.task(writing = true, touches = setOf(Reads.HERO)) {
        val id = heroes.heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        val session = sessions.state.value
        check(session.isAdmin || (session.signedIn && session.profile?.id == heroes.state.value.owner)) { ui("hero.owner_only") }
        block(id)
        if (heroes.state.value.readAt == 0L) sync.readHero()
        // Снимок рисуется вне главного потока: поход берёт новое снаряжение, когда герой, из которого оно читается, уже новый.
        sync.drawn()
        events.regear()
    }

    /** Команда кузницы: прежняя фраза уходит, как только начинается другая; каждое касание кузницы вибрирует (3.77.0). */
    private fun forgeCommand(block: suspend (String) -> Unit) {
        buzzes.buzz(Buzz.BUTTON)
        heroCommand { id ->
            heroes.forgeLine("")
            block(id)
        }
    }
}
