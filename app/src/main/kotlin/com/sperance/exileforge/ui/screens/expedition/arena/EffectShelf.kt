package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.effectText
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.guildLineTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.StatIcon
import com.sperance.exileforge.ui.theme.*
import kotlin.math.roundToInt

/**
 * Плитка полки эффектов (4.9.3, макет «Полки эффектов», вариант C): что лежит на бойце - плиткой на полке и строкой листа эффектов.
 * Новый вид состояния - новая реализация здесь, полка и лист берут его сами.
 */
internal sealed interface Shelved {
    /** Оглушение (не заморозка): золотая звезда. */
    data object Stun : Shelved

    /** Недуг со стаками. */
    data class Sick(val view: AilmentView) : Shelved

    /** Бафф, проклятие, помеха, глоток флакона, пассивное умение. */
    data class Effect(val view: EffectView) : Shelved

    /** Заряды героя одного вида. */
    data class Charge(val view: ChargeView) : Shelved

    /** Действующий рычаг Предначертания ✦. */
    data class Fate(val view: FateGlow) : Shelved

    /** Строка карты, что меняет героя. */
    data class Mod(val mod: HeroMapMod) : Shelved
}

/** Строка листа эффектов: имя цветом эффекта, откуда он, что делает и сколько ему осталось ([seconds] null - весь бой). */
internal data class ShelfRow(val title: String, val tint: Color, val source: String, val lines: List<String>, val seconds: Double?)

/** Оглушение героя или врага: держится и это не заморозка. */
internal fun stunned(held: Boolean, ailments: List<AilmentView>): Boolean = held && ailments.none { it.ailment == Ailment.FROZEN }

/** Временная полка бойца: оглушение, недуги, эффекты с таймером, заряды. */
internal fun timedShelf(held: Boolean, ailments: List<AilmentView>, effects: List<EffectView>, charges: List<ChargeView> = emptyList()): List<Shelved> = buildList {
    if (stunned(held, ailments)) add(Shelved.Stun)
    ailments.forEach { add(Shelved.Sick(it)) }
    effects.forEach { if (!it.lasting) add(Shelved.Effect(it)) }
    charges.forEach { add(Shelved.Charge(it)) }
}

/** Полка «на весь бой» героя: пассивные умения, носимые с начала баффы, строки карты и дары Предначертания. */
internal fun lastingShelf(fight: FightHud, mods: List<HeroMapMod>): List<Shelved> = buildList {
    fight.heroPassives.forEach { add(Shelved.Effect(it)) }
    fight.heroEffects.forEach { if (it.lasting) add(Shelved.Effect(it)) }
    mods.forEach { add(Shelved.Mod(it)) }
    fight.heroFate.forEach { add(Shelved.Fate(it)) }
}

@Composable internal fun ShelfTile(item: Shelved, side: Dp) {
    // Подсказка плитки (4.10.x, макет «Эффекты в бою», вариант A «Выноска у плитки»): касание - её подробная выноска, читается при открытии
    val tip = { item.tip() }
    when (item) {
        Shelved.Stun -> StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned"), side, tip)
        is Shelved.Sick -> StateTile(item.view.ailment, ailmentTint(item.view.ailment), item.view.left, item.view.stacks, ailmentLabel(item.view), side, tip)
        is Shelved.Effect -> EffectTile(item.view, side, tip)
        is Shelved.Charge -> ChargeTile(item.view, side, tip)
        is Shelved.Fate -> FateTile(item.view, side, tip)
        is Shelved.Mod -> MapModTile(item.mod, side, tip)
    }
}

/**
 * Подробная выноска плитки (4.10.x): имя, что делает - те же строки, что в листе эффектов ([row]), откуда, сколько осталось и
 * стаки; у недуга - ещё его сила.
 */
internal fun Shelved.tip(): Tip {
    val row = row()
    val own = (this as? Shelved.Sick)?.let { ailmentTip(it.view) }
    return Tip(
        row.title,
        (own?.body?.let(::listOf) ?: row.lines).joinToString("\n"),
        row.tint,
        listOf(ui("fight.fact_source") to row.source) + (own?.facts ?: listOfNotNull(row.seconds?.takeIf { it > 0 }?.let { ui("fight.fact_left") to ui("fight.fact_seconds", fineNumber(it)) })),
    )
}

/** Что сказать о плитке в листе эффектов: те же слова, что её подсказка, и строки, что она кладёт. */
internal fun Shelved.row(): ShelfRow = when (this) {
    Shelved.Stun -> ShelfRow(ui("expedition.stunned"), GoldBright, ui("fight.src.stun"), listOf(ui("fight.effect.STUN")), null)

    is Shelved.Sick -> ailmentTip(view).let { tip ->
        ShelfRow(ailmentLabel(view), tip.tint, ui("fight.src.ailment"), listOf(tip.body) + tip.facts.filter { it.first != ui("fight.fact_left") }.map { (k, v) -> "$k: $v" }, view.seconds)
    }

    is Shelved.Effect -> ShelfRow(
        effectTitle(view),
        effectTint(view),
        ui(
            when {
                view.passive -> "fight.src.passive"
                view.flask != null -> "fight.src.flask"
                view.kind == EffectKind.CURSE -> "fight.src.curse"
                view.kind == EffectKind.HINDER -> "fight.src.hinder"
                view.lasting -> "fight.src.worn"
                else -> "fight.src.buff"
            },
        ),
        view.lines.map { SkillText.statLine(it.stat, it.op, it.value) },
        view.seconds.takeUnless { view.lasting },
    )

    is Shelved.Charge -> ShelfRow(ui("fight.charge.${view.kind.name}"), Gold, ui("fight.src.charge"), listOf(ui("fight.charge_count", view.count, view.max)), view.seconds)

    is Shelved.Fate -> ShelfRow(
        locOr("fate.${view.fate}.name", view.fate),
        FateInk.Gold,
        ui("fight.src.fate"),
        listOf(ui("fight.fate_state.${view.lever.name}", fineNumber(view.value), view.count)),
        null,
    )

    is Shelved.Mod -> ShelfRow(
        modTitle(mod),
        modTint(mod),
        ui(modSource(mod)),
        mod.shifts.map { (stat, delta) -> effectText(stat, Op.ADD, delta) },
        null,
    )
}

/** Имя эффекта: бафф правил - по виду, бафф дара - даром, глоток - флаконом, прочее - умением. */
internal fun effectTitle(view: EffectView): String = when {
    view.buff != null -> ui("fight.buff.${view.buff!!.name}")
    view.source == FATE_SOURCE -> ui("fight.fate_buff")
    view.flask != null -> equipmentTitle(view.source)
    else -> SkillText.title(view.source)
}

/** Цвет эффекта: вред - кровью, глоток - цветом флакона, прочее - золотом. */
internal fun effectTint(view: EffectView): Color = when {
    view.kind.harmful -> LifeRed
    view.flask != null -> flaskTint(view.flask!!)
    else -> Gold
}

private fun modTint(mod: HeroMapMod): Color = if (mod.harm) Color(0xFFC0504A) else Gold

/** Имя плитки строки на герое (4.10.x): строка карты - своим кодом, доля гильдии и зелья - своим источником. */
private fun modTitle(mod: HeroMapMod): String = when (mod.source.kind) {
    SourceKind.GUILD -> guildLineTitle(mod.source.ref)
    SourceKind.POTION -> itemTitle(mod.source.ref)
    else -> statTitle(mod.source.ref)
}

/** Откуда плитка строки на герое: карта, древо гильдии или зелье. */
private fun modSource(mod: HeroMapMod): String = when (mod.source.kind) {
    SourceKind.GUILD -> "fight.src.guild"
    SourceKind.POTION -> "fight.src.potion"
    else -> "fight.src.map"
}

/** Строка карты на герое: пунктирная плитка - значок первого сдвинутого стата, цвет вреда или блага. */
@Composable private fun MapModTile(mod: HeroMapMod, side: Dp, tip: () -> Tip) {
    val tint = modTint(mod)
    val shape = RoundedCornerShape(4.dp)
    val title = modTitle(mod)
    Tipped(
        tip,
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .1f)).dashedBorder(tint, shape)
            .semantics { contentDescription = title },
    ) {
        mod.shifts.firstOrNull()?.let { (stat, _) -> StatIcon(stat, tint, Modifier.fillMaxSize().padding(side / 6)) }
    }
}

/** Плитка переполнения «+N»: сколько ещё эффектов не влезло; касание - лист эффектов [onOpen]. */
@Composable private fun MoreTile(count: Int, side: Dp, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(Muted.copy(alpha = .08f)).border(1.dp, Muted, shape).clickable(onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        Text(ui("fight.shelf.more", count), color = Parchment, fontSize = (side.value * .36f).coerceIn(7f, 11f).sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * Полка эффектов (4.9.3): плитки [items] стороной [side] в ряд или столбец ([vertical]), сколько влезает; не влезшие - плиткой
 * «+N» последней. Касание плитки - её подробная выноска ([tip], 4.10.x), касание «+N» - лист эффектов ([onOpen]).
 * Подкомпоновка по месту - число «+N» зависит от размера полки; интринсиков у полки нет, её размер задаёт родитель.
 */
@Composable internal fun EffectShelf(items: List<Shelved>, side: Dp, onOpen: () -> Unit, modifier: Modifier = Modifier, vertical: Boolean = false, gap: Dp = 4.dp) {
    SubcomposeLayout(modifier) { constraints ->
        val sidePx = side.roundToPx()
        val gapPx = gap.roundToPx()
        val room = if (vertical) constraints.maxHeight else constraints.maxWidth
        val fit = if (room == Constraints.Infinity) items.size else ((room + gapPx) / (sidePx + gapPx)).coerceAtLeast(1)
        val over = items.size > fit
        val shown = if (over) fit - 1 else items.size
        val tile = Constraints.fixed(sidePx, sidePx)
        val tiles = (0 until shown).map { i -> subcompose(i) { ShelfTile(items[i], side) }.first().measure(tile) } +
            (if (over) listOf(subcompose(MORE) { MoreTile(items.size - shown, side, onOpen) }.first().measure(tile)) else emptyList())
        val length = if (tiles.isEmpty()) 0 else tiles.size * sidePx + (tiles.size - 1) * gapPx
        val width = (if (vertical) sidePx else length).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (if (vertical) length else sidePx).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            tiles.forEachIndexed { i, placeable -> if (vertical) placeable.place(0, i * (sidePx + gapPx)) else placeable.place(i * (sidePx + gapPx), 0) }
        }
    }
}

private const val MORE = "more"

/** Итог здоровья в секунду числом со знаком: «−12», «+8», «±0». */
internal fun flowNumber(net: Double): String {
    val n = net.roundToInt()
    return when {
        n < 0 -> "−${-n}"
        n > 0 -> "+$n"
        else -> "±0"
    }
}

/** Цвет итога здоровья: убыль - красным, прибыль - зелёным, покой - приглушённо. */
internal fun flowTint(net: Double): Color = when {
    net <= -.5 -> Color(0xFFE5635C)
    net >= .5 -> Vital
    else -> Muted
}

/** Из чего сложился итог: «реген +8 · деген 2% · отложенный урон −14»; без источников - пусто. */
internal fun flowParts(flow: LifeFlow): String = listOfNotNull(
    ui("fight.flow.regen", flow.regen.roundToInt()).takeIf { flow.regen >= 1 },
    ui("fight.flow.degen", fineNumber(flow.degenShare)).takeIf { flow.degen > 0 },
    ui("fight.flow.delayed", flow.delayed.roundToInt()).takeIf { flow.delayed > 0 },
).joinToString(" · ")

/** Плитка итога здоровья (4.9.3): «−12/с» красным, «+8/с» зелёным, сверху рельса героя; [stacked] - «/с» строкой ниже. */
@Composable internal fun FlowTile(flow: LifeFlow, modifier: Modifier, stacked: Boolean = true) {
    val tint = flowTint(flow.net)
    val shape = RoundedCornerShape(4.dp)
    val number = flowNumber(flow.net)
    Box(
        modifier.clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .14f)).border(1.dp, tint, shape)
            .semantics { contentDescription = ui("fight.flow.title") + " " + ui("fight.flow.rate", number) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (stacked) number + "\n" + ui("fight.flow.per_second") else ui("fight.flow.rate", number),
            color = tint,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/**
 * Лист эффектов (4.9.3): всё, что лежит на бойце, - временные и на весь бой; у героя сверху - итог здоровья в секунду и его
 * разбор. Читается вживую: таймеры идут, бой не стоит.
 */
@Composable internal fun EffectsSheet(title: String, timed: List<Shelved>, lasting: List<Shelved>, flow: LifeFlow?, onDismiss: () -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium)
            flow?.let { FlowSummary(it) }
            if (timed.isEmpty() && lasting.isEmpty() && flow == null) MutedText(ui("fight.no_states"))
            if (timed.isNotEmpty()) {
                ShelfGroup(ui("fight.shelf.timed", timed.size))
                timed.forEach { SheetRow(it) }
            }
            val standing = lasting.size + (if (flow != null) 1 else 0)
            if (standing > 0) {
                ShelfGroup(ui("fight.shelf.lasting", standing))
                flow?.let { FlowRow(it) }
                lasting.forEach { SheetRow(it) }
            }
        }
    }
}

@Composable private fun ShelfGroup(text: String) = Text(
    text.uppercase(),
    color = Muted,
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
)

/** Сводка сверху листа героя: итог здоровья в секунду крупно и из чего он. */
@Composable private fun FlowSummary(flow: LifeFlow) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).background(PanelRaised, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(ui("fight.flow.title"), color = Parchment, style = MaterialTheme.typography.bodySmall)
            MutedText(flowParts(flow).ifEmpty { ui("fight.flow.quiet") }, style = MaterialTheme.typography.labelSmall)
        }
        Text(ui("fight.flow.rate", flowNumber(flow.net)), color = flowTint(flow.net), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun FlowRow(flow: LifeFlow) = EffectRow(
    { FlowTile(flow, Modifier.size(30.dp)) },
    ShelfRow(
        ui("fight.flow.row"),
        flowTint(flow.net),
        ui("fight.flow.row_source"),
        listOf(flowParts(flow).ifEmpty { ui("fight.flow.quiet") }),
        null,
    ),
    ui("fight.shelf.now"),
)

@Composable private fun SheetRow(item: Shelved) {
    val row = item.row()
    EffectRow({ ShelfTile(item, 30.dp) }, row, row.seconds?.let { ui("fight.fact_seconds", fineNumber(it)) } ?: ui("fight.shelf.whole"))
}

@Composable private fun EffectRow(tile: @Composable () -> Unit, row: ShelfRow, time: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        tile()
        Column(Modifier.weight(1f)) {
            Text(row.title, color = row.tint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            MutedText(row.source, style = MaterialTheme.typography.labelSmall)
            row.lines.filter { it.isNotBlank() }.forEach { Text(it, color = Parchment.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall) }
        }
        Text(time, color = if (row.seconds != null) Parchment else Muted, style = MaterialTheme.typography.labelMedium, fontWeight = if (row.seconds != null) FontWeight.Bold else FontWeight.Normal)
    }
    HorizontalDivider(color = Bronze.copy(alpha = .5f))
}

/** Пунктирная рамка 1dp: строка карты - не эффект боя, а условие захода. */
private fun Modifier.dashedBorder(tint: Color, shape: Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
    onDrawWithContent {
        drawContent()
        drawOutline(outline, tint, style = stroke)
    }
}
