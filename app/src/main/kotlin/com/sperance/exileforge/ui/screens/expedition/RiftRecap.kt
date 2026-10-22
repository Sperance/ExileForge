package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.expedition.RiftRecap
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.RiftMetal
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.expedition.arena.FightLogSheet
import kotlin.math.roundToInt

/**
 * Лист итога выигранного боя Разлома в палитре Разлома - нижний лист, как итог забега; заголовок - по узлу: «Бой выигран», «Элита
 * повержена», «Чемпион повержен», «Страж повержен», после Владыки - «Владыка повержен». Сверху бой: плитки времени, урона, урона в
 * секунду, полученного, критов с долей и фаз (у боя без фаз - без плитки), «Разбор» и «Журнал». Ниже Разлом: свиток Чести со снятой боем
 * долей, взятый ключ (после Владыки - ключи к сундукам по металлам), очки боя и счёт недели. Лист держится ([ForgeSheet] `held`):
 * закрывает его только главная кнопка - «Дальше», после Владыки с ключами - «К сундукам · N»; закрытый итог ждёт плашкой на карте
 * ([RiftRecapPlate]).
 */
@Composable internal fun RiftRecapSheet(game: GameUi, recap: RiftRecap, onClose: () -> Unit) {
    val stats = recap.fight.stats
    val name = recapName(recap)
    val guardian = recap.guardian
    val phases = remember(guardian, game.index) { guardian?.let { code -> game.index?.let { index -> index.monster(code)?.let(index.campaign::phasesOf)?.size } } ?: 0 }
    val chests = recap.keys.values.sum().takeIf { recap.final && it > 0 }
    var breakdown by remember { mutableStateOf(false) }
    var journal by remember { mutableStateOf(false) }
    ForgeSheet(onDismissRequest = onClose, containerColor = RiftColors.Panel, held = true) {
        Column(Modifier.padding(horizontal = 16.dp).padding(vertical = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui(recapTitle(recap)), color = if (recap.final) RiftColors.Pick else RiftColors.Rift, style = MaterialTheme.typography.titleLarge)
            Text(
                ui("rift.recap.subtitle", name, if (recap.final) ui("rift.recap.final") else ui("rift.act", recap.act + 1), clock(stats.seconds)),
                color = RiftColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tile(clock(stats.seconds), ui("rift.recap.time"), RiftColors.Text)
                Tile(number(stats.dealt.total), ui("rift.recap.dealt"), RiftColors.Text)
                Tile(number(stats.dps), ui("rift.recap.dps"), RiftColors.Text)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tile(number(stats.taken.total), ui("rift.recap.taken"), RiftColors.Ember)
                val share = if (stats.dealt.hits > 0) (stats.dealt.crits * 100.0 / stats.dealt.hits).roundToInt() else 0
                Tile(number(stats.dealt.crits.toDouble()), ui("rift.recap.crits", share), RiftColors.Text)
                if (phases > 0) Tile(ui("rift.recap.phases_of", recap.fight.phases.size, phases), ui("rift.recap.phases"), RiftColors.Text) { PhaseGems(recap.fight.phases.size, phases) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeButton(onClick = { breakdown = true }, kind = ButtonKind.Secondary, accent = RiftColors.Text, modifier = Modifier.weight(1f)) { Text(ui("rift.recap.breakdown")) }
                ForgeButton(onClick = { journal = true }, kind = ButtonKind.Secondary, accent = RiftColors.Text, modifier = Modifier.weight(1f)) { Text(ui("run.menu.log")) }
            }
            Text(ui("rift.recap.rift").uppercase(), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
            HonorScroll(recap.toll.before, recap.honorMax, recap.toll.before - recap.toll.after, Modifier.fillMaxWidth(), spent = true)
            val key = recap.toll.key
            when {
                recap.final -> RecapLine(ui("rift.recap.chests")) { ChestKeys(recap.keys) }
                key != null -> RecapLine(ui("rift.recap.key")) { MetalName(key) }
            }
            if (recap.points > 0) RecapLine(ui("rift.recap.points")) { Value("+" + number(recap.points.toDouble()), RiftColors.Rift) }
            RecapLine(ui("rift.result_week")) { Value(ui("rift.recap.score", number(recap.scoreBefore.toDouble()), number(recap.scoreAfter.toDouble())), RiftColors.Hot) }
            ForgeButton(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), accent = if (chests != null) RiftColors.Pick else RiftColors.Rift) {
                Text(if (chests != null) ui("rift.recap.to_chests", chests) else ui("rift.recap.to_map"))
            }
        }
    }
    if (breakdown) FightStatsSheet(stats, null) { breakdown = false }
    if (journal) FightLogSheet(recap.fight) { journal = false }
}

/**
 * Плашка «Итог боя» на карте забега: закрытый лист «Страж повержен» ждёт здесь до следующего боя; касание открывает его снова.
 */
@Composable internal fun RiftRecapPlate(recap: RiftRecap, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).background(RiftColors.Panel.copy(alpha = .95f), shape).border(1.dp, RiftColors.LineHi, shape)
            .clickable(onClickLabel = ui("rift.recap.open"), onClick = onOpen).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ui("rift.recap.plate", recapName(recap)), color = RiftColors.Soft, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(ui("rift.recap.open"), color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Ключ заголовка итога по узлу боя: Владыка, страж, Чемпион, Элита или рядовой бой. */
private fun recapTitle(recap: RiftRecap): String = when {
    recap.final -> "rift.recap.lord_title"
    recap.kind == RiftNodeKind.GUARDIAN -> "rift.recap.title"
    recap.kind == RiftNodeKind.CHAMPION -> "rift.recap.title_champion"
    recap.kind == RiftNodeKind.ELITE -> "rift.recap.title_elite"
    else -> "rift.recap.title_fight"
}

/** Кого победили: страж - по имени, прочий бой - именем узла. */
private fun recapName(recap: RiftRecap): String = recap.guardian?.let { monsterTitle(it) } ?: loc("rift.node.${recap.kind.name}.name")

/** Плитка итога боя: крупное число [value] цветом [tint], сжимается шрифтом, а не обрезается; под ним подпись [label] и [extra]. */
@Composable private fun RowScope.Tile(value: String, label: String, tint: Color, extra: (@Composable () -> Unit)? = null) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.weight(1f).background(RiftColors.Ground2, shape).border(1.dp, RiftColors.Line, shape).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            value,
            style = TextStyle(color = tint, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFeatureSettings = "tnum"),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 15.sp, stepSize = .5.sp),
        )
        extra?.invoke()
        Text(label, color = RiftColors.Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Кристаллы фаз: пройденные [passed] из [total] горят, прочие погашены. */
@Composable private fun PhaseGems(passed: Int, total: Int) {
    Canvas(Modifier.size(width = (total * 15).dp, height = 11.dp)) {
        val r = size.height / 2
        repeat(total) { i ->
            val c = Offset(r + i * 15.dp.toPx(), r)
            val gem = Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + r, c.y)
                lineTo(c.x, c.y + r)
                lineTo(c.x - r, c.y)
                close()
            }
            drawPath(gem, if (i < passed) GemLit else GemDark)
        }
    }
}

private val GemLit = Color(0xFFE3A93A)
private val GemDark = Color(0xFF1A241D)

/** Строка Разлома итога: подпись слева, значение справа. */
@Composable private fun RecapLine(label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = RiftColors.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        value()
    }
}

@Composable private fun Value(text: String, tint: Color) = Text(text, color = tint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)

/** Металл ключа: знак и имя его цветом. */
@Composable private fun MetalName(metal: RiftMetal) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        KeyMark(metal, Modifier.size(10.dp))
        Value(ui("rift.metal.${metal.name}"), metal.color())
    }
}

/** Ключи к сундукам Владыки по металлам: знак и число у каждого металла, что есть. */
@Composable private fun ChestKeys(keys: Map<RiftMetal, Int>) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RiftMetal.entries.filter { (keys[it] ?: 0) > 0 }.forEach { metal ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyMark(metal, Modifier.size(10.dp))
                Value("${keys[metal]}", metal.color())
            }
        }
        if (keys.values.none { it > 0 }) Value(ui("rift.recap.no_chests"), RiftColors.Muted)
    }
}
