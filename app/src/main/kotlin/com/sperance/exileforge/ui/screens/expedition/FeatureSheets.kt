package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.FeatureView
import com.sperance.exileforge.core.campaign.run.HazardView
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.modifierLine
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.LocalLore
import com.sperance.exileforge.ui.components.LocalSettings
import com.sperance.exileforge.ui.components.Lore
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.screens.expedition.scene.SCENE_UNIT
import com.sperance.exileforge.ui.theme.*

/**
 * Лист объекта карты (3.90.0, сервер 1.81.3), по объекту правил: алтарь, торговец, узел ремесла. Остальные объекты листа не
 * открывают - они срабатывают сами.
 */
@Composable internal fun FeatureSheet(game: GameUi, view: FeatureView, onCommand: (RunCommand) -> Unit) {
    when (val feature = view.feature) {
        is MapFeature.Altar -> AltarSheet(game, feature) { onCommand(RunCommand.Choose(it)) }
        is MapFeature.Merchant -> MerchantSheet(game, feature, view.taken, onCommand)
        is MapFeature.Node -> NodeSheet(game, feature, view.gathering, onCommand)
        is MapFeature.Trap, is MapFeature.Room -> Unit
    }
}

/**
 * Алтарь сделки: пары «дар + проклятие» строками модификаторов, как их пишет словарь сервера, и выбор одной. Отказаться
 * нельзя, поэтому это не шторка, которую смахивают, а панель над картой, как у кристалла.
 */
@Composable private fun AltarSheet(game: GameUi, altar: MapFeature.Altar, onPick: (Int) -> Unit) {
    val index = game.index ?: return
    fun text(line: Line) = index.modifier(line.code)?.let { modifierLine(index, it, line.values) } ?: line.code.value
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = .85f), Ink))), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).background(Panel.copy(alpha = .97f), RoundedCornerShape(12.dp))
                .border(1.dp, Gold.copy(alpha = .7f), RoundedCornerShape(12.dp)).padding(16.dp).heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(ui("feature.altar.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            MutedText(ui("feature.altar.note"))
            altar.pacts.forEachIndexed { i, pact ->
                Column(
                    Modifier.fillMaxWidth().border(1.dp, Bronze.copy(alpha = .6f), RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(ui("feature.altar.boon"), color = Vital, style = MaterialTheme.typography.labelMedium)
                    Text(text(pact.boon), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                    // Проклятие сделки (3.92.0) открывает свой лист: что меняет и что действует до конца карты
                    val lore = LocalLore.current
                    Column(
                        Modifier.fillMaxWidth().then(
                            if (lore != null) Modifier.clickable { lore(Lore.CurseText(ui("feature.altar.curse"), listOf(text(pact.curse)), ui("lore.altar_note"))) } else Modifier,
                        ),
                    ) {
                        Text(ui("feature.altar.curse") + " ›", color = LifeRed, style = MaterialTheme.typography.labelMedium)
                        Text(text(pact.curse), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                    }
                    ForgeButton(onClick = { onPick(i) }, modifier = Modifier.fillMaxWidth()) { Text(ui("feature.altar.pick")) }
                }
            }
        }
    }
}

/** Странствующий торговец: вещи плитами, цена на кнопке; купленное помечено, золота не хватает - кнопка не нажимается. */
@Composable private fun MerchantSheet(game: GameUi, merchant: MapFeature.Merchant, bought: List<Int>, onCommand: (RunCommand) -> Unit) {
    val money = game.hero?.money ?: 0L
    ForgeSheet(onDismissRequest = { onCommand(RunCommand.StepOff) }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feature.merchant.title"), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            MutedText(ui("feature.merchant.note", number(money.toDouble())))
            merchant.offers.forEachIndexed { i, offer ->
                val item = game.view(offer.item) ?: return@forEachIndexed
                val sold = i in bought
                ItemRow(
                    item,
                    enabled = !sold,
                    footer = {
                        if (sold) {
                            Text(ui("feature.merchant.sold"), color = Vital, style = MaterialTheme.typography.labelMedium)
                        } else {
                            ForgeButton(enabled = money >= offer.price, onClick = { onCommand(RunCommand.Choose(i)) }, modifier = Modifier.fillMaxWidth()) {
                                Text(ui("feature.merchant.buy", number(offer.price.toDouble())))
                            }
                        }
                    },
                    onClick = {},
                )
            }
            ForgeOutlinedButton(onClick = { onCommand(RunCommand.StepOff) }, modifier = Modifier.fillMaxWidth()) { Text(ui("feature.leave")) }
        }
    }
}

/**
 * Узел ремесла: что даёт и сколько собирать. Без инструмента профессии сбор не начать - лист говорит, какой нужен; идущий
 * сбор показывает свою долю.
 */
@Composable private fun NodeSheet(game: GameUi, node: MapFeature.Node, gathering: Float?, onCommand: (RunCommand) -> Unit) {
    val tooled = game.hero?.equipped?.containsKey(node.tool) == true
    ForgeSheet(onDismissRequest = { onCommand(RunCommand.StepOff) }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("feature.node.title", itemTitle(node.job.output)), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            MutedText(ui("feature.node.yield", number(node.seconds), node.amount.first(), node.amount.last()))
            if (!tooled) Text(ui("feature.node.tool", professionTitle(node.profession)), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
            if (gathering != null) {
                Text(ui("feature.node.gathering"), color = Rune, style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { gathering }, modifier = Modifier.fillMaxWidth(), color = Gold)
            } else {
                ForgeButton(enabled = tooled, onClick = { onCommand(RunCommand.Gather) }, modifier = Modifier.fillMaxWidth()) { Text(ui("feature.node.gather")) }
                ForgeOutlinedButton(onClick = { onCommand(RunCommand.StepOff) }, modifier = Modifier.fillMaxWidth()) { Text(ui("feature.leave")) }
            }
        }
    }
}

/** Простой у трещины тайной комнаты на полосе карты; удар ловушки с 3.94.1 всплывает над героем ([HazardFloat]). */
@Composable internal fun OpeningLine(opening: Float?) {
    opening?.let {
        Text(ui("feature.secret.opening"), color = Rune, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Start)
        LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().height(3.dp), color = Rune)
    }
}

/**
 * Удар ловушки над героем (3.94.1): красное «−N» встаёт над жетоном героя - камера держит его по центру чуть ниже середины -
 * и за полторы секунды уплывает вверх и гаснет; без анимаций просто висит, пока виден удар.
 */
@Composable internal fun HazardFloat(hazard: HazardView?) {
    val shown = hazard ?: return
    val motion = LocalSettings.current.animations
    val rise = remember(shown) { Animatable(0f) }
    LaunchedEffect(shown) { if (motion) rise.animateTo(1f, tween(HAZARD_FLOAT_MS, easing = LinearOutSlowInEasing)) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Ноги героя - на 55% высоты и полтайла ниже; жетон над ними - около двух полутайлов.
        val top = maxHeight * .55f + SCENE_UNIT - SCENE_UNIT * 2.6f - SCENE_UNIT * rise.value
        Text(
            ui("trap.float", shown.damage),
            color = LifeRed,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = top).alpha(1f - rise.value * .8f),
        )
    }
}

private const val HAZARD_FLOAT_MS = 1500
