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
import com.sperance.exileforge.core.display.BodyPlace
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateCard
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.presentation.ShellViewModel
import com.sperance.exileforge.presentation.forge.SmithyViewModel
import com.sperance.exileforge.presentation.hero.HeroViewModel
import com.sperance.exileforge.presentation.state.ForgeSection
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.HeroPage
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.TAB_SKILLS
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.auction.ListingSheet
import org.koin.compose.viewmodel.koinViewModel

/**
 * The hero: who they are, what they wear, and what they own.
 *
 * It used to be one long scroll with the stash — the list a player opens this tab for — at the very
 * bottom, under a character sheet and eleven slots that were mostly empty. Each is its own section
 * now, one tap apart, under a header that says who the character is — the one part they all share.
 * Every item, wherever it is shown, opens the same [ItemSheet].
 *
 * Since 2.22.0 the stash holds only what lies loose — what is worn or socketed is the Equipment
 * section's — and the bag is a section of its own, a list rather than a strip of chips. Since 3.0.0
 * every copy is read through its view over the content on the device, and the bag is keyed by item code.
 *
 * С 3.90.3 полоса разделов - в оболочке ([HeroChrome]), раздел - [HeroPage] модели героя; тайник - рейка мест слева и список
 * справа (макет «Тайник» В), с режимом продажи и автопродажей (макет «Продажа» А и В). С 3.90.5 (макет «Герой» A) герой - в шапке
 * игры, разделов четыре, дерево и гримуар - в «Развитии», продажа - удержанием плитки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroScreen() {
    val game by koinViewModel<HeroViewModel>().game.collectAsStateWithLifecycle()
    val shell: ShellViewModel = koinViewModel()
    val model = koinViewModel<HeroViewModel>()
    val smithy = koinViewModel<SmithyViewModel>()
    val page by model.page.collectAsStateWithLifecycle()
    val heroId = game.heroId
    var detailId by remember(heroId) { mutableStateOf<String?>(null) }
    var pickPlace by remember(heroId) { mutableStateOf<BodyPlace?>(null) }
    var stackCode by remember(heroId) { mutableStateOf<String?>(null) }
    var listStack by remember(heroId) { mutableStateOf<String?>(null) }
    // The filters (3.30.0) are the screen's own and go with it: a page switched away and back keeps the rail's choice.
    // Opening the tab is what refreshes the hero, and only when the last reading has gone cold.
    // Nothing here asks the player to press anything: the pull below is for when they disagree.
    LaunchedEffect(heroId, game.sessionEpoch) { model.ensure() }
    val list = rememberLazyListState()
    // Вкладка нажата снова (3.95.0): карточки закрыты, герой - на первой странице и наверху
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
                        item { PetSlots(game, model::activatePet) }
                    }

                    HeroPage.BAG -> {
                        val sections = bagSections(game)
                        // A table since 2.75.0: icon and count per cell, everything else behind the tap.
                        if (sections.isEmpty()) item { InfoCard(ui("hero.bag_empty"), ui("bag.empty_hint")) } else item(key = "bag") { BagGrid(game, sections) { stackCode = it } }
                    }

                    else -> {
                        // Предначертание аккаунта (4.6.0) над листом: тема и имя, касание - описание
                        heroFate(game)?.let { fate -> item(key = "fate") { FateBadge(fate, Modifier.fillMaxWidth()) } }
                        item { HeroSummary(game) }
                    }
                }
            }
        }
    }
    detailId?.let { id -> ItemSheet(game, model, id) { detailId = null } }
    // The sheet is about a stack the bag still holds: listed or read away, it closes with it.
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
                // A book is read where it lies, and its page opens in the grimoire (2.78.0); an essence goes to the forge.
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
    // The place goes with the pick: a ring chosen for the second line lands in the second ring, a flask in its own bay.
    pickPlace?.let { place -> SlotPicker(game, place, onDismiss = { pickPlace = null }, onEquip = { itemId -> model.equip(itemId, place.place) }) }
}

/** Предначертание аккаунта героя карточкой (4.6.0); не выбрано или нет в контенте - null. */
private fun heroFate(game: GameUi): FateCard? = game.index?.fates?.of(game.hero?.info?.fate)?.let(FateCard::of)
