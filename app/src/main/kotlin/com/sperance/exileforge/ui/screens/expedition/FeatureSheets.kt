package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.FeatureView
import com.sperance.exileforge.core.campaign.run.HazardView
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.display.Rate
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.modifierLine
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeIconButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.GoldCost
import com.sperance.exileforge.ui.components.IconButtonStyle
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.LocalLore
import com.sperance.exileforge.ui.components.LocalSettings
import com.sperance.exileforge.ui.components.Lore
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.payLabel
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.OfferSheet
import com.sperance.exileforge.ui.screens.expedition.arena.ailmentTint
import com.sperance.exileforge.ui.screens.expedition.arena.key
import com.sperance.exileforge.ui.screens.expedition.arena.stateGlyph
import com.sperance.exileforge.ui.screens.expedition.arena.tickTint
import com.sperance.exileforge.ui.screens.expedition.scene.SCENE_UNIT
import com.sperance.exileforge.ui.theme.*

/**
 * Лист объекта карты, по объекту правил: алтарь, торговец, узел ремесла. Остальные объекты листа не
 * открывают - они срабатывают сами (очаг Скверны - касанием точки и сундука).
 */
@Composable internal fun FeatureSheet(game: GameUi, view: FeatureView, onCommand: (RunCommand) -> Unit) {
    when (val feature = view.feature) {
        is MapFeature.Altar -> DockPanel { AltarSheet(game, feature, onLeave = { onCommand(RunCommand.StepOff) }) { onCommand(RunCommand.Choose(it)) } }
        is MapFeature.Merchant -> DockPanel { MerchantSheet(game, feature, view.taken, onCommand) }
        is MapFeature.Node -> DockPanel { NodeSheet(game, feature, view.guarded, onCommand) }
        is MapFeature.Trap, is MapFeature.Room, is MapFeature.Blight -> Unit
    }
}

/**
 * Алтарь (в духе Eldritch): готовые варианты - у каждого цель (герой, монстры карты, босс карты) и связанная пара
 * «минус цели - награда» строками модификаторов, как их пишет словарь сервера; выбрать один или пройти мимо ([onLeave]). Панель над
 * картой, как у кристалла.
 *
 * Минус сделки открывает свой лист: что меняет и что действует до конца карты.
 */
@Composable internal fun AltarSheet(game: GameUi, altar: MapFeature.Altar, onLeave: () -> Unit, onPick: (Int) -> Unit) {
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
                    Text(ui("feature.altar.target.${pact.target.name}"), color = GoldBright, style = MaterialTheme.typography.titleSmall)
                    val lore = LocalLore.current
                    Column(
                        Modifier.fillMaxWidth().then(
                            if (lore != null) Modifier.clickable { lore(Lore.CurseText(ui("feature.altar.curse"), listOf(text(pact.curse)), ui("lore.altar_note"))) } else Modifier,
                        ),
                    ) {
                        Text(ui("feature.altar.curse") + " ›", color = LifeRed, style = MaterialTheme.typography.labelMedium)
                        Text(text(pact.curse), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(ui("feature.altar.boon"), color = Vital, style = MaterialTheme.typography.labelMedium)
                    Text(text(pact.boon), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                    ForgeButton(onClick = { onPick(i) }, modifier = Modifier.fillMaxWidth()) { Text(ui("feature.altar.pick")) }
                }
            }
            ForgeButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) { Text(ui("feature.altar.leave")) }
        }
    }
}

/**
 * Единый шаблон листа объекта карты, компактный - в одну-две строки: гнездо 40dp с иконкой [icon] в рамке тона [tone], имя [name]
 * серифом и сводка фактов [facts] текстом через «·», круглый ✕ ([onDismiss]) справа. Вторая строка - фишка [lead] слева
 * (инструмент) и главное действие [action] справа; запрет [blocked] - красная строка причины на месте фишки, действие гаснет.
 * Своё тело [body] (товары торговца) - под шапкой. Новый объект карты с листом - этот шаблон, а не своя вёрстка.
 */
@Composable
private fun FeatureCard(
    tone: Color,
    name: String,
    facts: List<FeatureFact>,
    icon: @Composable () -> Unit,
    onDismiss: () -> Unit,
    lead: FeatureFact? = null,
    blocked: String? = null,
    body: @Composable ColumnScope.() -> Unit = {},
    action: (@Composable RowScope.(enabled: Boolean) -> Unit)? = null,
) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val socket = RoundedCornerShape(9.dp)
                val rim = if (blocked != null) LifeRed else tone
                Box(Modifier.size(40.dp).background(Abyss, socket).border(1.dp, rim.copy(alpha = .5f), socket), contentAlignment = Alignment.Center) { icon() }
                Column(Modifier.weight(1f)) {
                    Text(name, color = GoldBright, style = relicName(15), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (facts.isNotEmpty()) Text(summary(facts), style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                ForgeIconButton(Icons.Outlined.Close, ui("common.close"), onClick = onDismiss, style = IconButtonStyle.Tonal, compact = true)
            }
            if (action != null || lead != null || blocked != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        when {
                            blocked != null -> Text("✕ $blocked", color = LifeRed, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            lead != null -> FactChip(lead)
                        }
                    }
                    action?.invoke(this, blocked == null)
                }
            }
            body()
        }
    }
}

/** Факты сводкой одной строкой: каждый своим цветом, между ними - приглушённая «·». */
private fun summary(facts: List<FeatureFact>): AnnotatedString = buildAnnotatedString {
    facts.forEachIndexed { i, fact ->
        if (i > 0) withStyle(SpanStyle(color = Muted)) { append(" · ") }
        withStyle(SpanStyle(color = fact.tone)) { append(fact.text) }
    }
}

/** Фишка факта: текст его цветом в пилюле того же тона. */
@Composable private fun FactChip(fact: FeatureFact) {
    val pill = RoundedCornerShape(50)
    Text(
        fact.text,
        color = fact.tone,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.background(fact.tone.copy(alpha = .08f), pill).border(1.dp, fact.tone.copy(alpha = .3f), pill).padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** Факт объекта карты: текст и его цвет. */
private data class FeatureFact(val text: String, val tone: Color = Muted)

/**
 * Странствующий торговец (шаблон объекта карты): в сводке - золото героя, сколько товаров осталось, куплено и скидка; под шапкой -
 * вещи плитами, цена на кнопке; купленное помечено, золота не хватает - кнопка не нажимается. Нажатие на товар открывает полную
 * карточку со сравнением с надетым - ту же, что у лавки Города.
 *
 * Скидка против витрины Города - множитель цены торговца из правил меньше единицы; цена против кошелька - чип «цена / есть».
 */
@Composable private fun MerchantSheet(game: GameUi, merchant: MapFeature.Merchant, bought: List<Int>, onCommand: (RunCommand) -> Unit) {
    val money = game.hero?.money ?: 0L
    var chosen by remember { mutableStateOf<Int?>(null) }
    val left = merchant.offers.indices.count { it !in bought }
    FeatureCard(
        tone = GoldBright,
        name = ui("feature.merchant.title"),
        facts = listOfNotNull(
            FeatureFact(ui("feature.merchant.gold", number(money.toDouble())), GoldBright),
            FeatureFact(ui("feature.merchant.left", left), Parchment),
            FeatureFact(ui("feature.merchant.bought", bought.size), Vital).takeIf { bought.isNotEmpty() },
            game.index?.campaign?.features?.merchant?.markup?.takeIf { it < 1 }?.let { FeatureFact(ui("feature.merchant.discount", number((1 - it) * 100)), Vital) },
        ),
        icon = { Icon(ForgeGlyphs.Coins, null, tint = Gold, modifier = Modifier.size(24.dp)) },
        onDismiss = { onCommand(RunCommand.StepOff) },
        body = {
            MutedText(ui("feature.merchant.note"))
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
                            val bill = game.goldBill(offer.price)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                bill?.let { GoldCost(it) }
                                ForgeButton(enabled = bill?.affordable == true, onClick = { onCommand(RunCommand.Choose(i)) }, modifier = Modifier.weight(1f)) {
                                    Text(bill?.payLabel(ui("feature.merchant.buy", number(offer.price.toDouble()))) ?: ui("feature.merchant.buy", number(offer.price.toDouble())))
                                }
                            }
                        }
                    },
                    onClick = { chosen = i },
                )
            }
        },
    )
    chosen?.takeIf { it !in bought }?.let { i ->
        val offer = merchant.offers.getOrNull(i) ?: return@let
        OfferSheet(game, offer.item, offer.price, onDismiss = { chosen = null }) {
            chosen = null
            onCommand(RunCommand.Choose(i))
        }
    }
}

/**
 * Узел ремесла (шаблон объекта карты): ресурс в гнезде, в сводке - сколько штук, опыт ремесла, сколько уже есть и уровень
 * профессии; надетый инструмент - зелёной фишкой рядом с «Собрать». Нет инструмента или узел под стражами сокровищ ([guarded]) -
 * красная причина и погасшая кнопка; идущий сбор - пилюля действия у объекта ([ObjectActionPanel]), не этот лист.
 */
@Composable private fun NodeSheet(game: GameUi, node: MapFeature.Node, guarded: Boolean, onCommand: (RunCommand) -> Unit) {
    val index = game.index
    val tool = game.hero?.equipped?.get(node.tool)
    val output = node.job.output
    val level = game.hero?.crafts?.professions?.get(node.profession)?.level ?: 1
    val experience = index?.let { it.professions.rules.experience(node.job) * node.share } ?: 0.0
    val facts = listOfNotNull(
        FeatureFact(ui("feature.node.amount", node.amount.first(), node.amount.last()), Parchment),
        FeatureFact(ui("feature.node.experience", number(experience)), Rune).takeIf { experience > 0 },
        game.bagAmount(output)?.let { FeatureFact(ui("feature.node.have", number(it.toDouble()))) },
        FeatureFact(ui("feature.node.profession", professionTitle(node.profession), level)),
    )
    val blocked = listOfNotNull(
        ui("feature.node.tool", professionTitle(node.profession)).takeIf { tool == null },
        ui("feature.guarded").takeIf { guarded },
    ).joinToString(" · ").ifEmpty { null }
    FeatureCard(
        tone = Vital,
        name = itemTitle(output),
        facts = facts,
        icon = { BagIcon(output, Modifier.size(26.dp)) },
        onDismiss = { onCommand(RunCommand.StepOff) },
        lead = tool?.let { FeatureFact(equipmentTitle(it.template), Vital) },
        blocked = blocked,
    ) { enabled ->
        ForgeButton(enabled = enabled, onClick = { onCommand(RunCommand.Gather) }) { Text(ui("feature.node.gather_for", number(node.seconds)), maxLines = 1) }
    }
}

/**
 * Урон карты над героем (одной строкой): удар ловушки - «−12 Хаос · Яд: 3% здоровья/с, 6 с» цветом её урона,
 * щит и здоровье вместе; тики состояния - мелким курсивом, как в бою. Число встаёт над жетоном героя - камера держит его чуть
 * ниже середины - и уплывает вверх, гаснет; без анимаций висит, пока видно.
 *
 * Ноги героя - на 55% высоты и полтайла ниже, жетон над ними - около двух полутайлов.
 */
@Composable internal fun HazardFloat(hazards: List<HazardView>) {
    if (hazards.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val feet = maxHeight * .55f + SCENE_UNIT - SCENE_UNIT * 2.6f
        hazards.forEach { hazard -> androidx.compose.runtime.key(hazard.id) { HazardNumber(hazard, feet) } }
    }
}

/** Тик чуть в стороне от удара: оба читаются, пока удар висит; тик недуга - «−34» и знак недуга его цветом. */
@Composable private fun BoxWithConstraintsScope.HazardNumber(hazard: HazardView, feet: Dp) {
    val motion = LocalSettings.current.animations
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { if (motion) rise.animateTo(1f, tween(if (hazard.tick) HAZARD_TICK_MS else HAZARD_FLOAT_MS, easing = LinearOutSlowInEasing)) }
    val side = if (hazard.tick) SCENE_UNIT * .9f else 0.dp
    Column(
        Modifier.align(Alignment.TopCenter).offset(x = side, y = feet - SCENE_UNIT * rise.value).alpha(1f - rise.value * .8f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val ticked = hazard.of
        val tint = tickTint(ticked, hazard.type, onHero = true)
        if (ticked != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(ui("trap.float", hazard.total), color = tint, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.labelMedium)
                val glyph = ailmentTint(ticked)
                Canvas(Modifier.size(12.dp)) { stateGlyph(ticked, glyph) }
            }
        } else {
            Text(hazardLine(hazard), color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        }
    }
}

/** Удар ловушки одной строкой: сколько снял и чем, затем её состояние - сколько здоровья в секунду и сколько секунд. */
private fun hazardLine(hazard: HazardView): String {
    val type = ui(hazard.type.key())
    val ailment = hazard.ailment ?: return ui("trap.hit", hazard.total, type)
    return ui("trap.hit_ailment", hazard.total, type, ui(ailment.ailment.key()), Rate.of(number(ailment.lifeShare) + "%"), number(ailment.seconds))
}

private const val HAZARD_TICK_MS = 1100
private const val HAZARD_FLOAT_MS = 1500
