package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.fate.FateDeal
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.ItemUse
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ConfirmSheet
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.HeroNameSheet
import com.sperance.exileforge.ui.components.inputs
import com.sperance.exileforge.ui.screens.session.FateAltar
import com.sperance.exileforge.ui.theme.Muted
import org.koin.compose.viewmodel.koinViewModel

/**
 * «Использовать» стопки сумки [code] по виду из контента: «Лист Угнетения» - лист нового имени героя; «Колода Рока» - предупреждение
 * удержанием (дар сменится, счёт прежнего - с нуля), затем алтарь новой раздачи ([FateDealScreen]). [onDone] - поток закрыт.
 */
@Composable internal fun BagUseFlow(game: GameUi, model: HeroViewModel, code: String, onDone: () -> Unit) {
    when (game.index?.item(ItemCode(code))?.use) {
        ItemUse.RENAME_HERO -> HeroNameSheet(ui("rename.title"), game.heroName, game.inputs, game.busy, onDismiss = onDone) { name, _ ->
            onDone()
            model.renameHero(code, name)
        }

        ItemUse.REDRAW_FATE -> ConfirmSheet(itemTitle(code), ui("deck.hold"), onDismiss = onDone, warning = ui("deck.warning"), busy = game.busy) {
            onDone()
            model.openDeck(code)
        }

        null -> LaunchedEffect(code) { onDone() }
    }
}

/** Хост раздачи «Колоды Рока»: открытая раздача ([HeroViewModel.fateDeal]) - во весь экран ([FateDealScreen]), иначе ничего. */
@Composable internal fun FateDealHost(game: GameUi) {
    val model = koinViewModel<HeroViewModel>()
    val deal by model.fateDeal.collectAsStateWithLifecycle()
    deal?.let { FateDealScreen(game, it, model) }
}

/**
 * Новая раздача «Колоды Рока» во весь экран - тот же алтарь, что первый выбор дара ([FateAltar]): три новых дара без нынешнего, выбор
 * удержанием тратит колоду; принятый - вспышка и выход. До выбора экран закрывается «Назад», колода цела.
 */
@Composable private fun FateDealScreen(game: GameUi, deal: FateDeal, model: HeroViewModel) {
    Dialog(
        onDismissRequest = { if (deal.accepted == null) model.closeDeal() },
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        FateAltar(
            deal.view?.offers.orEmpty(),
            deal.accepted,
            busy = game.busy,
            waiting = deal.view == null,
            subtitle = ui("deck.subtitle"),
            warning = ui("deck.warning"),
            onRetry = { model.openDeck(deal.item) },
            onChoose = model::chooseDeal,
            onAccepted = model::closeDeal,
        ) {
            ForgeButton(
                onClick = model::closeDeal,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp),
                kind = ButtonKind.Text,
                state = ButtonState.of(!game.busy && deal.accepted == null),
                accent = Muted,
            ) { Text(ui("common.back"), style = MaterialTheme.typography.labelMedium) }
        }
    }
}
