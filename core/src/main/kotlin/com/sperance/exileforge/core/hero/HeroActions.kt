package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.shardsPerOrb
import com.sperance.exileforge.core.display.shardsText
import com.sperance.exileforge.core.display.withShards
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.core.session.Buzzes
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.NoticeKind
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotGroup
import com.sperance.exileforge.rules.roll.Dice

/**
 * Команды героя: сундук, кузница, гримуар, древо, выдачи тестеру. Каждая отвечает героем, как
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
    private val buzzes: Buzzes,
) {
    private val api: GameApi get() = connection.api

    /** Предмет под кузницей: тот, на который укажет следующая сфера. */
    fun selectEquipment(itemId: String) = heroes.selectEquipment(itemId)

    /** Фраза наковальни уходит вместе с целью, о которой она: выбрана другая вещь. */
    fun clearForgeLine() = heroes.forgeLine("")

    fun equip(itemId: String, slot: Slot? = null) {
        buzzes.buzz(Buzz.BUTTON)
        heroCommand { id -> api.hero.equip(id, itemId, slot) }
    }
    fun unequip(itemId: String) = heroCommand { id -> api.hero.unequip(id, itemId) }

    /** Титул рядом с именем: открытый хроникой, или никакой. */
    fun setTitle(title: String) = heroCommand { id -> api.hero.setTitle(id, title) }

    /** Замок на предмете: запертый не продаётся, не выставляется и не уходит автопродажей; флаг едет в снимке. */
    fun lockItem(itemId: String, locked: Boolean) = heroCommand { id -> api.hero.lock(id, itemId, locked) }

    /** Ещё пачка мест сундука за золото; снимок с ответом несёт новый счёт. */
    fun expandStash() = heroCommand { id -> api.hero.expandStash(id) }

    /** Из переполнения в сундук: [itemId], или сколько влезет. */
    fun claimOverflow(itemId: String? = null) = heroCommand { id -> api.hero.claimOverflow(id, itemId) }
    fun sellOverflow(itemId: String) = heroCommand { id -> shardsText(api.hero.sellOverflow(id, itemId).shards, perOrb)?.let(notices::toast) }

    /** Окно тестера: одна выдача `/hero/grant/` герою в игре. */
    fun testerGrant(what: String, vararg params: Pair<String, String?>) = heroCommand { id ->
        check(sessions.state.value.grants(heroes.state.value)) { ui("hero.grant_admin_only") }
        api.hero.grant(id, what, *params)
        sync.readHero()
    }

    /**
     * Тестировщику: свободный прогресс [on] героя в игре - замки уровня и прогресса открыты или закрыты; снимок героя - заново. Не
     * тестировщику отказывает сервер (`AUTH_007`).
     */
    fun freeProgress(on: Boolean) = heroCommand { id ->
        api.hero.freeProgress(id, on)
        sync.readHero()
    }

    /** Тестеру или админу: названный шаблон герою, брошенный сервером. */
    fun grant(template: String, rarity: Rarity? = null) = heroCommand { id ->
        check(sessions.state.value.grants(heroes.state.value)) { ui("hero.grant_admin_only") }
        api.hero.grantEquipment(id, template, rarity)
    }

    /**
     * Тестеру или админу: случайный шаблон выбранной редкости и слота - выбран здесь из контента, брошен сервером.
     *
     * Редкость - только та, что выдача даёт шаблону: уникальный и мифический - своя, база - обычная, волшебная, редкая.
     */
    fun grantRandom(rarityCode: String, slotCode: String) = heroCommand { id ->
        check(sessions.state.value.grants(heroes.state.value)) { ui("hero.grant_admin_only") }
        val index = world.state.value.content ?: error(ui("runtime.request_failed"))
        val rarity = Rarity.of(rarityCode)
        val slot = Slot.of(slotCode)
        val factory = com.sperance.exileforge.rules.roll.ItemFactory(index)
        val candidates = index.templates.values.filter { (slot == null || it.slot == slot) && (rarity == null || rarity in factory.grantable(it)) }
        check(candidates.isNotEmpty()) { ui("hero.no_template") }
        api.hero.grantEquipment(id, Dice.system().pick(candidates).code, rarity)
    }

    /** Тестеру или админу: стопка в сумку. */
    fun grantItem(code: String, amount: Long) = heroCommand { id ->
        check(sessions.state.value.grants(heroes.state.value)) { ui("hero.bag_admin_only") }
        api.hero.grantItem(id, code, amount)
    }

    /**
     * Одна сфера на один предмет: применима ли и что перебрасывает - дело правил; фраза возвращается. [omen] тратится с ней;
     * [line] - строка, что гравирует Сфера гравёра; [count] - пачка сферы качества одним запросом.
     *
     * Бережливый кузнец (Предначертание): сфера может остаться в сумке.
     */
    fun applyOrb(itemId: String, orb: String, omen: String, onApplied: () -> Unit = {}, line: Int? = null, count: Int = 1) = forgeCommand { id ->
        val outcome = api.hero.applyOrb(id, itemId, orb, omen, line, count)
        heroes.forgeLine(outcome.message)
        if (outcome.kept) notices.toast(ui("fate.orb_kept", itemTitle(orb)), NoticeKind.CRAFT)
        heroes.selectEquipment(outcome.created?.id ?: outcome.item.id)
        onApplied()
    }

    /** Печать Атласа [seal] на карту [itemId]: как сфера - строка итога, бережливость Предначертания, знамение тратится с ней. */
    fun applySeal(itemId: String, seal: String, omen: String, onApplied: () -> Unit = {}) = forgeCommand { id ->
        val outcome = api.hero.applySeal(id, itemId, seal, omen)
        heroes.forgeLine(outcome.message)
        if (outcome.kept) notices.toast(ui("fate.seal_kept", itemTitle(seal)), NoticeKind.CRAFT)
        heroes.selectEquipment(outcome.item.id)
        onApplied()
    }

    /** Закалка кузнеца: фраза о том, что поднялось, уходит тостом. */
    fun temper(itemId: String) = forgeCommand { id -> notices.toast(api.hero.temper(id, itemId).message, NoticeKind.CRAFT) }

    fun unveil(itemId: String, choice: Int) = forgeCommand { id -> heroes.forgeLine(api.hero.unveil(id, itemId, choice).message) }

    /** Строка Предзнаменования выбора оставлена. */
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

    /** Сундук с добычей вскрыт: добыча на экране, пока её не закрыли. */
    fun openChest(code: String) = heroCommand { id -> heroes.chest(api.hero.openChest(id, code)) }
    fun dismissChest() = heroes.chest(null)
    fun slotSkill(kind: String, index: Int, code: String?, condition: String? = null) = heroCommand { id -> api.hero.slotSkill(id, kind, index, code, condition) }
    fun flaskCondition(index: Int, condition: String?) = heroCommand { id -> api.hero.flaskCondition(id, index, condition) }

    /** Обмен книг: полученная книга сразу читается сервером - умение изучено или выросло. */
    fun exchangeBooks(books: List<String>, code: String) = heroCommand { id ->
        api.hero.exchangeBooks(id, books, code)
        notices.toast(ui("skills.exchanged", com.sperance.exileforge.core.display.SkillText.title(code)))
    }

    /** Руна в гнездо умения или гнездо пустеет: вынутая и заменённая руна разрушаются. */
    fun socketRune(code: String, socket: Int, rune: String?) = heroCommand { id -> api.hero.socketRune(id, code, socket, rune) }

    /** Древо навыков: взять узел, вернуть его или сбросить всё древо. Каждое правило - серверное. */
    fun allocateNode(code: String, choice: Int? = null) = heroCommand { id -> api.tree.allocate(id, code, choice) }
    fun refundNode(code: String) = heroCommand { id -> api.tree.refund(id, code) }
    fun refundBranch(code: String) = heroCommand { id -> api.tree.refundBranch(id, code) }
    fun rechooseNode(code: String, choice: Int) = heroCommand { id -> api.tree.rechoose(id, code, choice) }
    fun resetTree() = heroCommand { id -> api.tree.reset(id) }
    fun autoSell(rarity: Rarity, groups: Set<SlotGroup>) = heroCommand { id -> api.hero.autoSell(id, rarity, groups) }

    /** Правило автопродажи «ниже героя»: [levels] уровней и более для редкостей [rarities]; ноль выключает. */
    fun autoSellBelow(levels: Int, rarities: Set<Rarity>) = heroCommand { id -> api.hero.autoSellBelow(id, levels, rarities) }

    /** Правило автопродажи «всё, что герой не может носить». */
    fun autoSellUnwearable(on: Boolean) = heroCommand { id -> api.hero.autoSellUnwearable(id, on) }

    /** Тестеру или админу: опыт герою, уровень решает сервер. */
    fun addExperience(amount: Double) = heroCommand { id ->
        check(sessions.state.value.grants(heroes.state.value)) { ui("hero.grant_admin_only") }
        api.hero.grantExperience(id, amount)
    }

    fun redeem(code: String) = heroCommand { id ->
        notices.toast(ui("redemption.redeemed"))
        api.hero.redeem(id, code)
    }

    /** Один уникальный самоцвет вида на героя - общее правило, спрошенное прежде отказа сервера; самоцвет занятого гнезда уходит в тайник. */
    fun socketJewel(itemId: String, nodeCode: String) = heroCommand { id ->
        val hero = heroes.state.value.hero
        val item = hero?.item(itemId)
        val index = world.state.value.content
        if (hero != null && item != null && index != null) check(hero.jewelFree(index, item, nodeCode)) { ui("tree.jewel_unique_taken") }
        api.hero.socket(id, itemId, nodeCode)
    }
    fun unsocketJewel(itemId: String) = heroCommand { id -> api.hero.unsocket(id, itemId) }

    /**
     * Продажа торговцу: цену ставит и платит сервер; карточка показала ту же сумму заранее. [unequip] - надетую снять и продать разом;
     * [onSold] - сервер продал вещь, до перерисовки героя.
     */
    fun sellForGold(itemId: String, unequip: Boolean = false, onSold: () -> Unit = {}) = heroCommand { id ->
        val sold = api.hero.sell(id, itemId, unequip)
        onSold()
        notices.toast(withShards(ui("toast.sold", sold.gold), sold.shards, perOrb))
    }

    /** Продажа пачкой: выбор тайника и «по правилам»; сколько и за сколько - тостом. */
    fun sellMany(itemIds: Collection<String>) = heroCommand { id -> sellBatch(id, itemIds) }

    /** Та же продажа пачкой внутри идущей команды - «Продать и вернуться» конца захода, уже вне захода. */
    suspend fun sellBatch(heroId: String, itemIds: Collection<String>) {
        val sold = api.hero.sellMany(heroId, itemIds.toList())
        notices.toast(withShards(ui("toast.sold_many", sold.itemIds.size, sold.gold), sold.shards, perOrb))
    }

    /** Сколько осколков склеиваются в сферу - для строки осколков в тосте. */
    private val perOrb: Int get() = world.state.value.content.shardsPerOrb

    /** Одна команда героя: владелец или админ; ответ без снимка перечитывается и рисуется. */
    private fun heroCommand(block: suspend (String) -> Unit) = commands.task(writing = true, touches = setOf(Reads.HERO)) {
        val id = heroes.heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        val session = sessions.state.value
        check(session.isAdmin || (session.signedIn && session.profile?.id == heroes.state.value.owner)) { ui("hero.owner_only") }
        block(id)
        if (heroes.state.value.readAt == 0L) sync.readHero()
        sync.drawn()
    }

    /** Команда кузницы: прежняя фраза уходит, как только начинается другая; каждое касание кузницы вибрирует. */
    private fun forgeCommand(block: suspend (String) -> Unit) {
        buzzes.buzz(Buzz.BUTTON)
        heroCommand { id ->
            heroes.forgeLine("")
            block(id)
        }
    }
}
