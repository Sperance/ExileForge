package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.SummonView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.ConfirmSheet
import com.sperance.exileforge.ui.components.GoldCost
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SmallAction
import com.sperance.exileforge.ui.components.payLabel
import com.sperance.exileforge.ui.icons.ForgeGlyphs

/**
 * Призыв поверженного стража (цена в золоте у места оплаты): кнопка «Призвать стража» и чип «цена / есть»
 * ([GoldCost]) под строкой стража. Цена - одна функция правил ([SummonView.price], `SummonLog.price`); не хватает золота - кнопка
 * погашена и пишет недостачу, заход исчерпал призывы - погашена, под ней причина. Нажатие - подтверждение оплаты ([ConfirmSheet] с
 * золотом), пока оно открыто, заход стоит.
 */
@Composable internal fun GuardianSummon(game: GameUi, run: ExpeditionRun, summon: SummonView, map: String, onCommand: (RunCommand) -> Unit) {
    val bill = game.goldBill(summon.price) ?: return
    var asking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallAction(
                ForgeGlyphs.Portal,
                if (summon.spent) ui("expedition.summon") else bill.payLabel(ui("expedition.summon")),
                onClick = { asking = true },
                enabled = !summon.spent && bill.affordable,
            )
            GoldCost(bill)
        }
        if (summon.spent) MutedText(ui("expedition.summon_spent", summon.perRun), style = MaterialTheme.typography.labelSmall)
    }
    if (asking) {
        HoldsRun(run)
        ConfirmSheet(
            title = ui("expedition.summon_q"),
            confirm = ui("expedition.summon"),
            subtitle = map,
            note = ui("expedition.summon_note", summon.perRun),
            gold = bill,
            onDismiss = { asking = false },
        ) {
            asking = false
            onCommand(RunCommand.Summon)
        }
    }
}
