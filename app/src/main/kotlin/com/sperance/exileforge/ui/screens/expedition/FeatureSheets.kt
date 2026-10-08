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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.AfflictionView
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
import com.sperance.exileforge.ui.screens.auction.OfferSheet
import com.sperance.exileforge.ui.screens.expedition.arena.ailmentTint
import com.sperance.exileforge.ui.screens.expedition.arena.damageTint
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.scene.SCENE_UNIT
import com.sperance.exileforge.ui.theme.*
import kotlin.math.ceil

/**
 * Лист объекта карты (3.90.0, сервер 1.81.3), по объекту правил: алтарь, торговец, узел ремесла. Остальные объекты листа не
 * открывают - они срабатывают сами (очаг Скверны - касанием точки и сундука).
 */
@Composable internal fun FeatureSheet(game: GameUi, view: FeatureView, onCommand: (RunCommand) -> Unit) {
    when (val feature = view.feature) {
        is MapFeature.Altar -> AltarSheet(game, feature) { onCommand(RunCommand.Choose(it)) }
        is MapFeature.Merchant -> MerchantSheet(game, feature, view.taken, onCommand)
        is MapFeature.Node -> NodeSheet(game, feature, view.gathering, onCommand)
        is MapFeature.Trap, is MapFeature.Room, is MapFeature.Blight -> Unit
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

/**
 * Странствующий торговец: вещи плитами, цена на кнопке; купленное помечено, золота не хватает - кнопка не нажимается.
 * Нажатие на товар открывает полную карточку со сравнением с надетым - ту же, что у лавки Города (4.2.0).
 */
@Composable private fun MerchantSheet(game: GameUi, merchant: MapFeature.Merchant, bought: List<Int>, onCommand: (RunCommand) -> Unit) {
    val money = game.hero?.money ?: 0L
    var chosen by remember { mutableStateOf<Int?>(null) }
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
                    onClick = { chosen = i },
                )
            }
            ForgeOutlinedButton(onClick = { onCommand(RunCommand.StepOff) }, modifier = Modifier.fillMaxWidth()) { Text(ui("feature.leave")) }
        }
    }
    chosen?.takeIf { it !in bought }?.let { i ->
        val offer = merchant.offers.getOrNull(i) ?: return@let
        OfferSheet(game, offer.item, offer.price, money, onDismiss = { chosen = null }) {
            chosen = null
            onCommand(RunCommand.Choose(i))
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
 * Урон карты над героем (3.94.1; с 3.95.3 - каждое число своё): удар ловушки - «−42 огонь» цветом стихии, принятое щитом -
 * голубым рядом, наложенный эффект - строкой ниже («+ Горение 4 с»); тики эффекта - мелким курсивом, как в бою. Число встаёт
 * над жетоном героя - камера держит его чуть ниже середины - и уплывает вверх, гаснет; без анимаций висит, пока видно.
 */
@Composable internal fun HazardFloat(hazards: List<HazardView>) {
    if (hazards.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Ноги героя - на 55% высоты и полтайла ниже; жетон над ними - около двух полутайлов.
        val feet = maxHeight * .55f + SCENE_UNIT - SCENE_UNIT * 2.6f
        hazards.forEach { hazard -> androidx.compose.runtime.key(hazard.id) { HazardNumber(hazard, feet) } }
    }
}

@Composable private fun BoxWithConstraintsScope.HazardNumber(hazard: HazardView, feet: Dp) {
    val motion = LocalSettings.current.animations
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (motion) rise.animateTo(1f, tween(if (hazard.tick) HAZARD_TICK_MS else HAZARD_FLOAT_MS, easing = LinearOutSlowInEasing)) }
    // Тик чуть в стороне от удара: оба читаются, когда эффект тикает, пока удар ещё висит
    val side = if (hazard.tick) SCENE_UNIT * .9f else 0.dp
    Column(
        Modifier.align(Alignment.TopCenter).offset(x = side, y = feet - SCENE_UNIT * rise.value).alpha(1f - rise.value * .8f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val size = if (hazard.tick) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium
            val style = if (hazard.tick) FontStyle.Italic else FontStyle.Normal
            if (hazard.life > 0 || hazard.shield == 0) {
                Text(ui("trap.float", hazard.life), color = damageTint(hazard.type, onHero = true), fontWeight = FontWeight.Bold, fontStyle = style, style = size)
            }
            if (hazard.shield > 0) Text(ui("trap.float", hazard.shield), color = ShieldCyan, fontWeight = FontWeight.Bold, fontStyle = style, style = size)
            if (!hazard.tick) Text(ui(hazard.type.key()), color = damageTint(hazard.type, onHero = true), style = MaterialTheme.typography.labelSmall)
        }
        hazard.ailment?.let { ailment ->
            Text(ui("trap.ailment", ui(ailment.key()), ceil(hazard.seconds).toInt()), color = ailmentTint(ailment), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Эффекты ловушек на герое (3.95.3) под полосой жизни: «Горение −32/с · 3 с». */
@Composable internal fun AfflictionChips(afflictions: List<AfflictionView>, modifier: Modifier = Modifier) {
    if (afflictions.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        afflictions.forEach { affliction ->
            val tint = ailmentTint(affliction.ailment)
            Text(
                ui("trap.affliction", ui(affliction.ailment.key()), affliction.perSecond, ceil(affliction.left).toInt()),
                color = tint,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.background(Ink.copy(alpha = .8f), RoundedCornerShape(8.dp)).border(1.dp, tint.copy(alpha = .5f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private const val HAZARD_TICK_MS = 1100
private const val HAZARD_FLOAT_MS = 1500
