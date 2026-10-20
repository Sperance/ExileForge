package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.ForgeSection
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.TAB_CRAFT
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.ShellViewModel
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.craft.SmithyViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Герой сейчас в бою - поход, испытание или Разлом: снаряжение взято на входе, кузница закрыта. Ставит корень экранов. */
val LocalInFight = compositionLocalOf { false }

/**
 * Действия своей вещи по умолчанию - один ряд малых кнопок под главным действием карточки, где бы её ни открыли (общий хозяин карточек,
 * лист вещи тайника): «В кузницу» открывает кузницу с этой вещью - только вне боя ([LocalInFight]) и если место не ведёт в кузницу
 * само ([forge]). Чужая вещь - не в снаряжении и не в тайнике героя [game] - ряда не получает. [trailing] - что место ставит в конец
 * ряда; [onDone] - действие ушло, лист закрывается.
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
        trailing()
    }
}
