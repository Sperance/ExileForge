package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.compactNumber
import com.sperance.exileforge.core.display.fullIfShortened
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Цена в золоте у места оплаты: монета, цена и
 * «/ есть» мелко и приглушённо - числа короткие ([compactNumber]: «150к»); сокращённое касанием показывает полные цену и кошелёк.
 * Не хватает - цена красная. Решает счёт [GoldBill]; кнопка оплаты
 * пишет недостачу [payLabel], подтверждение - строки [goldLedger]. Цена без кошелька (продажа, награда) - [GoldPrice]. [purse] false - без
 * «/ есть»: строки списка, когда кошелёк уже напечатан на экране (цепочка торговца), цена всё так же красная при нехватке.
 */
@Composable fun GoldCost(bill: GoldBill, modifier: Modifier = Modifier, purse: Boolean = true) {
    val shape = RoundedCornerShape(50)
    val tip = fullNumbersTip(listOfNotNull(ui("confirm.spend") to bill.price, (ui("gold.have") to bill.have).takeIf { purse }))
    Tipped(tip, modifier) {
        Row(
            Modifier.background(Abyss, shape).border(1.dp, Bronze, shape).padding(horizontal = 8.dp, vertical = 3.dp)
                .semantics(mergeDescendants = true) { contentDescription = ui("gold.cost", groupedNumber(bill.price), groupedNumber(bill.have)) },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(12.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (bill.affordable) GoldBright else LossText, fontWeight = FontWeight.SemiBold)) { append(compactNumber(bill.price)) }
                    if (purse) withStyle(SpanStyle(color = Muted)) { append(" / " + compactNumber(bill.have)) }
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * Сумма в золоте на кнопке - облик [GoldCost] без кошелька: монета и число короткой записью в тёмной плашке; на погашенной кнопке
 * приглушена. Цена продажи на удержании «Продать». Касание и удержание принадлежат кнопке, поэтому подсказки у суммы нет; чтение с
 * экрана слышит полное число.
 */
@Composable fun ButtonGold(gold: Long, modifier: Modifier = Modifier) {
    val live = LocalButtonLive.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier.background(Abyss, shape).border(1.dp, Bronze, shape).padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = ui("merchant.gold_amount", groupedNumber(gold)) },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ForgeGlyphs.Coins, null, tint = if (live) Gold else Muted, modifier = Modifier.size(12.dp))
        Text(compactNumber(gold), color = if (live) GoldBright else Muted, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

/**
 * Счёт оплаты тремя ячейками «Есть · Спишется · Останется» (не хватает - третья «Не хватает» красным): тот же счёт [GoldBill],
 * что строки подтверждения [goldLedger], там, где покупка раскрывается на месте, а не листом. Сокращённое число ячейки касанием
 * показывает полное ([FullNumberTipped]).
 */
@Composable fun GoldBillCells(bill: GoldBill, modifier: Modifier = Modifier) {
    val cells = listOf(
        Triple(ui("gold.have"), bill.have, Parchment),
        Triple(ui("confirm.spend"), bill.price, Caution),
        if (bill.affordable) Triple(ui("confirm.left"), bill.left, Parchment) else Triple(ui("gold.lacking"), bill.short, LifeRed),
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.forEach { (label, gold, tint) ->
            FullNumberTipped(gold, Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(vertical = 6.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = Muted, fontSize = 11.sp, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(12.dp))
                        Text(compactNumber(gold), color = tint, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Подпись кнопки оплаты: хватает - [ready], не хватает - «Не хватает 389к» (кнопка при этом погашена: `enabled` - [GoldBill.affordable]). */
fun GoldBill.payLabel(ready: String): String = if (affordable) ready else shortText()

/** «Не хватает 389к» - недостача золота одной строкой. */
fun GoldBill.shortText(): String = ui("gold.short", compactNumber(short))

/** Строки подтверждения оплаты золотом: есть, спишется и останется - или, если не хватает, сколько ([goldLine]). */
fun GoldBill.goldLedger(): List<LedgerLine> = listOf(
    goldLine(ui("gold.have"), have),
    goldLine(ui("confirm.spend"), price, Tone.SPEND),
    if (affordable) goldLine(ui("confirm.left"), left) else goldLine(ui("gold.lacking"), short, Tone.SPEND),
)

/**
 * Строка подтверждения с золотом [gold] под подписью [label]: «150к золота» короткой записью, перед числом - [sign] («+» выручки);
 * сокращённое число касанием показывает полное ([LedgerLine.full]).
 */
fun goldLine(label: String, gold: Long, tone: Tone = Tone.PLAIN, sign: String = ""): LedgerLine = LedgerLine(label, sign + ui("merchant.gold_amount", compactNumber(gold)), tone, fullIfShortened(gold)?.let { sign + ui("merchant.gold_amount", it) })
