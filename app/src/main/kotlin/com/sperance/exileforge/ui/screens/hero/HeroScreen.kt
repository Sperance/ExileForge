package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.ForgeSection
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_CRAFT
import com.sperance.exileforge.core.app.TAB_HERO
import com.sperance.exileforge.core.app.TAB_SKILLS
import com.sperance.exileforge.core.campaign.FateKit
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.fate.fateNow
import com.sperance.exileforge.core.hero.HeroPage
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateLever
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import com.sperance.exileforge.ui.screens.craft.SmithyViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The hero: who they are, what they wear, and what they own.
 *
 * Each part is its own section, one tap apart, under a header that says who the character is — the one part they all
 * share; the stash, the list a player opens this tab for, is not buried under the sheet and the slots.
 * Every item, wherever it is shown, opens the same [ItemSheet].
 *
 * The stash holds only what lies loose — what is worn or socketed is the Equipment
 * section's — and the bag is a section of its own, a list rather than a strip of chips.
 * every copy is read through its view over the content on the device, and the bag is keyed by item code.
 *
 * Полоса разделов - в оболочке ([HeroChrome]), раздел - [HeroPage] модели героя; тайник - рейка мест слева и список
 * справа, с режимом продажи и автопродажей. Герой - в шапке
 * игры, разделов четыре, дерево и гримуар - в «Развитии», продажа - удержанием плитки.
 *
 * Фильтры - свои у экрана и уходят с ним. Открытие вкладки освежает героя, только если прошлое чтение остыло; потягивание - если игрок не
 * согласен. Вкладка нажата снова - карточки закрыты, герой на первой странице и наверху. Предначертание - лентой над листом. Номер героя -
 * касание копирует, Мощь - справа в той же строке. Лист стопки закрывается вместе с ушедшей стопкой; место идёт с выбором (кольцо во вторую
 * строку - во второе кольцо).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroScreen() {
    val game by koinViewModel<HeroViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val model = koinViewModel<HeroViewModel>()
    val smithy = koinViewModel<SmithyViewModel>().model
    val page by model.page.collectAsStateWithLifecycle()
    val heroId = game.heroId
    var detailId by remember(heroId) { mutableStateOf<String?>(null) }
    var pickPlace by remember(heroId) { mutableStateOf<BodyPlace?>(null) }
    var stackCode by remember(heroId) { mutableStateOf<String?>(null) }
    var listStack by remember(heroId) { mutableStateOf<String?>(null) }
    LaunchedEffect(heroId, game.sessionEpoch) { model.ensure() }
    val list = rememberLazyListState()
    OnReselect(TAB_HERO) {
        detailId = null
        stackCode = null
        model.page(HeroPage.CHARACTER)
        list.animateScrollToItem(0)
    }
    val hero = game.hero
    PullToRefreshBox(isRefreshing = game.refreshing(Reads.HERO), onRefresh = model::load, modifier = Modifier.fillMaxSize()) {
        when {
            hero == null -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) { item { InfoCard(ui("common.loading"), ui("hero.stash_empty_hint")) } }

            page == HeroPage.STASH -> StashPane(game, model, onOpen = { id -> detailId = id })

            else -> LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    HeroPage.WORN -> {
                        item { rememberEquipment(game)?.let { EquipmentLedger(it) { place, worn -> if (worn != null) detailId = worn else pickPlace = place } } }
                    }

                    HeroPage.BAG -> {
                        val sections = bagSections(game)
                        if (sections.isEmpty()) item { InfoCard(ui("hero.bag_empty"), ui("bag.empty_hint")) } else item(key = "bag") { BagGrid(game, sections) { stackCode = it } }
                    }

                    else -> {
                        heroFate(game)?.let { fate ->
                            item(key = "fate") {
                                val effects = remember(game.hero, game.index) { heroFateEffects(game) }
                                val now = remember(effects, game.lang) { effects?.let { e -> game.index?.let { fateNow(it, e) } }.orEmpty() }
                                val caption = fateProgress(game, effects) ?: ui("fate.ribbon.caption")
                                FateRibbon(
                                    fate,
                                    effects?.power?.takeIf { it > 0 }?.let { ui("fate.ribbon.power", caption, number(it)) } ?: caption,
                                    Modifier.fillMaxWidth(),
                                    now = now,
                                )
                            }
                        }
                        game.hero?.let { hero -> item(key = "number") { NumberAndMight(hero, game.heroId) } }
                        item { HeroSummary(game) }
                    }
                }
            }
        }
    }
    detailId?.let { id -> ItemSheet(game, model, id) { detailId = null } }
    stackCode?.let { code ->
        hero?.bag?.get(code)?.takeIf { it > 0 }?.let { amount ->
            BagSheet(
                game,
                BagStack(code, amount),
                onDismiss = { stackCode = null },
                onForge = { orb ->
                    stackCode = null
                    smithy.selectOrb(orb)
                    smithy.open(null, ForgeSection.ORBS)
                    shell.tab(TAB_CRAFT)
                },
                onAuction = { stack ->
                    stackCode = null
                    listStack = stack
                },
                onRead = { skill ->
                    stackCode = null
                    model.learnSkill(skill)
                    shell.tab(TAB_SKILLS)
                },
                onEssence = { essence ->
                    stackCode = null
                    smithy.selectEssence(essence)
                    smithy.open(null, ForgeSection.ESSENCES)
                    shell.tab(TAB_CRAFT)
                },
                onSeal = { seal ->
                    stackCode = null
                    smithy.selectSeal(seal)
                    smithy.open(null, ForgeSection.SEALS)
                    shell.tab(TAB_CRAFT)
                },
                onOpenChest = { chest ->
                    stackCode = null
                    model.openChest(chest)
                },
            )
        }
    }
    game.holding.chest?.let { opening -> ChestOpenedSheet(game, opening, model::dismissChest) }
    listStack?.let { code ->
        ListingSheet(game, itemTitle(code), owned = game.bagAmount(code) ?: 0L, onDismiss = { listStack = null }, hint = { model.priceHint(code, null, 0) }) { orb, price, amount ->
            listStack = null
            model.sellItem(code, amount, orb, price)
        }
    }
    pickPlace?.let { place -> SlotPicker(game, place, onDismiss = { pickPlace = null }, onEquip = { itemId -> model.equip(itemId, place.place) }) }
}

/** Предначертание аккаунта героя карточкой; не выбрано или нет в контенте - null. */
private fun heroFate(game: GameUi): FateCard? = game.index?.fates?.of(game.hero?.info?.fate)?.let(FateCard::of)

/** Дар героя на его силе и с действующими строками надетого - тем же резолвером правил, что бой; нет дара - null. */
private fun heroFateEffects(game: GameUi): FateEffects? {
    val hero = game.hero ?: return null
    val index = game.index ?: return null
    return FateKit.of(index, hero.info.fate, hero.stats, hero.equipped.values).effects.takeIf { it.fate != null }
}

/**
 * Боёв до сферы Щедрости судьбы (`FateLever.FIGHT_BOUNTY`, счёт сервера `fateFights`) - с судьбоносными строками надетого
 * (строка «раньше» сокращает счёт); у дара без рычага - null.
 */
private fun fateProgress(game: GameUi, effects: FateEffects?): String? {
    val info = game.hero?.info ?: return null
    val every = effects?.of(FateLever.FIGHT_BOUNTY)?.every?.takeIf { it > 0 } ?: return null
    return ui("fate.bounty_next", every - (info.fateFights % every).toInt())
}
