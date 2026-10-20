package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.ForgeSection
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_CRAFT
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.sellPrice
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.ButtonGold
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.Hold
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.craft.SmithyViewModel
import com.sperance.exileforge.ui.theme.LifeRed
import org.koin.compose.viewmodel.koinViewModel

/** Герой сейчас в заходе - поход, испытание или Разлом по состоянию сервера ([com.sperance.exileforge.core.model.hero.HeroView.inRun]): кузница закрыта, снаряжение меняется только в походе по карте, между боями. Ставит корень экранов. */
val LocalInFight = compositionLocalOf { false }

/**
 * Действия своей вещи по умолчанию - один ряд малых кнопок под главным действием карточки, где бы её ни открыли (общий хозяин карточек,
 * лист вещи тайника, добыча захода): «В кузницу» открывает кузницу с этой вещью - только вне боя ([LocalInFight]) и если место не ведёт
 * в кузницу само ([forge]); «Продать» с ценой торговца - удержанием, без окна подтверждения. Надетую вне боя сервер снимает и продаёт
 * одной операцией - кнопка «Снять и продать» со знаком предупреждения; в бою надетую не продать, вставленный самоцвет и запертую вещь
 * тоже - кнопка погашена, причина строкой под рядом. Вещь тайника продаётся и в походе. Чужая вещь - не в снаряжении и не в тайнике
 * героя [game] - ряда не получает. [trailing] - что место ставит в конец ряда; [onDone] - действие ушло: лист закрывается сразу, до ответа сервера, отказ сервера приходит тостом.
 */
@Composable fun ColumnScope.OwnItemActions(
    game: GameUi,
    item: ItemInstance,
    forge: Boolean = true,
    onDone: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    if (game.hero?.item(item.id) == null) return
    val inFight = LocalInFight.current
    val can = !game.busy && game.session.signedIn && (game.ownsCharacter || game.isAdmin)
    val smithy = koinViewModel<SmithyViewModel>().model
    val shell = koinViewModel<ShellViewModel>()
    val model = koinViewModel<HeroViewModel>()
    val worn = item.equipped && !item.socketed
    val unequip = worn && !inFight
    val refusal = when {
        item.socketed -> ui("item.sell_socketed")
        item.locked -> ui("item.sell_locked")
        worn && inFight -> ui("item.sell_worn_run")
        else -> null
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (forge && !inFight) {
            ForgeButton(
                onClick = {
                    onDone()
                    smithy.open(item.id, ForgeSection.ORBS)
                    shell.tab(TAB_CRAFT)
                },
                kind = ButtonKind.Secondary,
                size = ButtonSize.Small,
                state = ButtonState.of(can),
                icon = ForgeGlyphs.Anvil,
            ) { Text(ui("bag.to_forge"), maxLines = 1) }
        }
        ForgeButton(
            onClick = {
                onDone()
                model.sellForGold(item.id, unequip)
            },
            modifier = Modifier.weight(1f),
            kind = ButtonKind.Danger,
            size = ButtonSize.Small,
            state = ButtonState.of(can && refusal == null),
            icon = if (unequip) Icons.Outlined.WarningAmber else Icons.Outlined.TouchApp,
            hold = Hold(),
        ) {
            Text(ui(if (unequip) "item.unequip_sell" else "hero.action_sell"), Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            game.sellPrice(item)?.let { ButtonGold(it, Modifier.padding(start = 6.dp)) }
        }
        trailing()
    }
    refusal?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
}
