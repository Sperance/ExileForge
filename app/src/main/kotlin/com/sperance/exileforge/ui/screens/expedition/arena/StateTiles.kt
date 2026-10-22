package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.skills.FlaskBottle
import com.sperance.exileforge.ui.theme.*

/**
 * Every state on a fighter as a tile of its colour whose dark fill rises as it wears off; a stun is a gold star. Действующие рычаги
 * Предначертания - золотой ✦ дара [fates]. Полка эффектов ([EffectShelf]): что не влезло - «+N», касание
 * открывает лист всех эффектов [title].
 */
@Composable internal fun StateTiles(
    title: String,
    ailments: List<AilmentView>,
    held: Boolean,
    effects: List<EffectView> = emptyList(),
    charges: List<ChargeView> = emptyList(),
    fates: List<FateGlow> = emptyList(),
) {
    var open by remember { mutableStateOf(false) }
    val timed = timedShelf(held, ailments, effects, charges)
    val lasting = effects.filter { it.lasting }.map { Shelved.Effect(it) } + fates.map { Shelved.Fate(it) }
    val all = timed + lasting
    if (all.isEmpty()) {
        MutedText(ui("fight.no_states"), style = MaterialTheme.typography.labelSmall)
    } else {
        EffectShelf(all, 30.dp, { open = true }, Modifier.fillMaxWidth())
    }
    if (open) EffectsSheet(title, timed, lasting, null) { open = false }
}

/** Эффекты стороны боя для ленты и листа: временные ([timedShelf]) и на весь бой ([lasting]). */
@Immutable
internal class EffectShelves(val timed: List<Shelved>, val lasting: List<Shelved>) {
    val isEmpty: Boolean get() = timed.isEmpty() && lasting.isEmpty()
}

/** Эффекты врага ([EffectShelves]) - пересчёт только при смене его недугов, оглушения и эффектов. */
@Composable internal fun rememberFoeShelves(ailments: List<AilmentView>, held: Boolean, effects: List<EffectView>): EffectShelves = remember(ailments, held, effects) {
    EffectShelves(timedShelf(held, ailments, effects), effects.filter { it.lasting }.map { Shelved.Effect(it) })
}

/**
 * Лента эффектов босса над его головой на арене: все плитки состояний и эффектов [shelves] стороной 22dp на полупрозрачной подложке,
 * по шесть в строке, лишние - следующей строкой, без «+N»; последним - чип «≡», его касание - лист эффектов [onOpen]. Касание плитки -
 * её выноска; подложка касаний не ловит - они уходят портрету под ней (выбор цели).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EffectRibbon(shelves: EffectShelves, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    if (shelves.isEmpty) return
    FlowRow(
        modifier.clip(RibbonShape).background(RibbonVeil).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        maxItemsInEachRow = RIBBON_ROW,
    ) {
        shelves.timed.forEach { ShelfTile(it, RIBBON_TILE) }
        shelves.lasting.forEach { ShelfTile(it, RIBBON_TILE) }
        SheetChip(shelves.timed.size + shelves.lasting.size, onOpen)
    }
}

/** Чип «≡» ленты эффектов: плитка ленты, касание - лист всех [count] эффектов. */
@Composable private fun SheetChip(count: Int, onOpen: () -> Unit) {
    val label = ui("fight.shelf.all", count)
    Box(
        Modifier.size(RIBBON_TILE).clip(RibbonChip).background(Muted.copy(alpha = .12f)).border(1.dp, Muted, RibbonChip)
            .clickable(role = Role.Button, onClick = onOpen).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text("≡", color = Parchment, fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Плиток в строке ленты эффектов босса, их сторона, подложка, её форма и форма чипа листа. */
private const val RIBBON_ROW = 6
private val RIBBON_TILE = 22.dp
private val RibbonVeil = Color(0x9E06080A)
private val RibbonShape = RoundedCornerShape(6.dp)
private val RibbonChip = RoundedCornerShape(4.dp)

/**
 * Действующий рычаг дара: ✦ золотом Предначертания, тёмная заливка растёт по мере ухода срока,
 * в углу - стаки; подсказка - имя дара и что он делает сейчас (`fight.fate_state.<рычаг>`).
 */
@Composable internal fun FateTile(view: FateGlow, bearing: Bearing, side: Dp = 30.dp, tip: () -> Tip) {
    val title = locOr("fate.${view.fate}.name", view.fate)
    Tipped(
        tip,
        Modifier.shelfTile(FateInk.Gold, bearing, side).semantics { contentDescription = title },
    ) {
        Text(FATE_GLYPH, color = FateInk.GoldLight, fontSize = 15.sp, modifier = Modifier.align(Alignment.Center))
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - view.left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
        if (view.count > 1) {
            Text(
                "${view.count}",
                color = Parchment,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
            )
        }
    }
}

/** Знак Предначертания на значке рычага. */
private const val FATE_GLYPH = "✦"

/** A kind of the hero's charges: the drawing of its maximum and the count held. */
@Composable internal fun ChargeTile(view: ChargeView, bearing: Bearing, side: Dp = 30.dp, tip: () -> Tip) {
    val title = ui("fight.charge.${view.kind.name}")
    Tipped(
        tip,
        Modifier.shelfTile(Gold, bearing, side).semantics { contentDescription = "$title ${view.count}" },
    ) {
        com.sperance.exileforge.ui.icons.StatIcon(view.stat, Gold, Modifier.fillMaxSize().padding(side / 6))
        Text(
            "${view.count}",
            color = Parchment,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
        )
    }
}

/** Справка проклятия на плитке: проклятие тотема - справка тотема, умения - его строки. */
private fun curseLore(view: EffectView): Lore = when (val origin = view.origin) {
    is EffectOrigin.Totem -> Lore.Totem(origin.code, null, view.seconds)
    else -> Lore.Curse(view.source, null, view.seconds)
}

/**
 * A buff or a curse as a tile: the skill's mark in gold for a buff, in blood for a curse, darkening as it wears off. Глоток, прерванный
 * полным здоровьем, - погасшая бутыль под знаком «стоп».
 *
 * Бафф правил (Натиск, Укрепление) назван и нарисован по виду, умения - по умению, глоток - бутылью; бафф Предначертания называется даром.
 * Проклятие открывает лист; проклятие со слоями пишет их число «×N» в углу.
 */
@Composable internal fun EffectTile(view: EffectView, bearing: Bearing, side: Dp = 30.dp, tip: () -> Tip) {
    val tint = effectTint(view)
    val buff = view.buff
    val title = effectTitle(view)
    val lore = LocalLore.current?.takeIf { view.kind == EffectKind.CURSE && buff == null }
    Tipped(
        tip.takeIf { lore == null },
        Modifier.then(if (lore != null) Modifier.clickable { lore(curseLore(view)) } else Modifier).shelfTile(tint, bearing, side)
            .semantics { contentDescription = title },
    ) {
        val flask = view.flask
        if (flask != null) {
            FlaskBottle(flask, if (view.halted) 0f else 1f, true, Modifier.fillMaxSize().padding(horizontal = side / 4, vertical = side / 8))
        } else if (buff != null) {
            com.sperance.exileforge.ui.icons.StatIcon(buff.icon, tint, Modifier.fillMaxSize().padding(side / 6))
        } else if (view.source == INTIMIDATED) {
            com.sperance.exileforge.ui.icons.StatIcon(CoreStat.INTIMIDATE.code, tint, Modifier.fillMaxSize().padding(side / 6))
        } else {
            SkillGlyph(view.icon, Modifier.fillMaxSize().padding(side / 6), tint)
        }
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - view.left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
        if (view.halted) Canvas(Modifier.fillMaxSize().padding(side / 3)) { haltGlyph(Parchment) }
        if (view.layers > 1) {
            Text(
                ui("fight.layers", view.layers),
                color = Parchment,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
            )
        }
    }
}

/** Знак прерванного глотка: две вертикальные черты «стоп» поверх погасшей бутыли. */
private fun DrawScope.haltGlyph(tint: Color) {
    val bar = size.width * .3f
    drawRect(tint, Offset(0f, 0f), Size(bar, size.height))
    drawRect(tint, Offset(size.width - bar, 0f), Size(bar, size.height))
}

@Composable internal fun StateTile(ailment: Ailment?, tint: Color, left: Float, stacks: Int, label: String, bearing: Bearing, side: Dp = 30.dp, tip: () -> Tip) {
    Tipped(
        tip,
        Modifier.shelfTile(tint, bearing, side).semantics { contentDescription = label },
    ) {
        Canvas(Modifier.fillMaxSize().padding(side / 5)) { stateGlyph(ailment, tint) }
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
        if (stacks > 1) {
            Text(
                "$stacks",
                color = Parchment,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp),
            )
        }
    }
}

/**
 * An ailment's mark drawn from shapes: a flame, a snowflake, a crystal, a bolt, a drop, two drops; a star for a stun. Изранение - рваная рана со стежками, иссушение - капля в трещинах.
 */
internal fun DrawScope.stateGlyph(ailment: Ailment?, tint: Color) {
    val u = size.minDimension / 16f
    fun path(vararg points: Pair<Float, Float>) = Path().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
        close()
    }
    when (ailment) {
        Ailment.BURNING -> drawPath(path(8f to 1f, 12f to 7f, 12f to 11f, 10f to 15f, 6f to 15f, 4f to 11f, 5f to 7f, 7f to 9f), tint)

        Ailment.CHILLED -> listOf(0f, 60f, 120f).forEach { angle ->
            val a = Math.toRadians(angle.toDouble())
            val dx = (kotlin.math.cos(a) * 7 * u).toFloat()
            val dy = (kotlin.math.sin(a) * 7 * u).toFloat()
            drawLine(tint, Offset(8 * u - dx, 8 * u - dy), Offset(8 * u + dx, 8 * u + dy), 1.6f * u)
        }

        Ailment.FROZEN -> drawPath(path(8f to 1f, 14f to 5f, 14f to 11f, 8f to 15f, 2f to 11f, 2f to 5f), tint)

        Ailment.SHOCKED -> drawPath(path(10f to 1f, 3f to 9f, 7f to 9f, 6f to 15f, 13f to 7f, 9f to 7f), tint)

        Ailment.POISONED -> {
            drawPath(path(8f to 2f, 12f to 9f, 11f to 13f, 8f to 14f, 5f to 13f, 4f to 9f), tint)
            drawCircle(Color(0xFF0B0E13), 1.4f * u, Offset(8 * u, 10 * u))
        }

        Ailment.BLEEDING -> {
            drawPath(path(6f to 2f, 9f to 8f, 8f to 11f, 6f to 12f, 4f to 11f, 3f to 8f), tint)
            drawPath(path(12f to 7f, 14f to 11f, 13f to 13f, 12f to 14f, 11f to 13f, 10f to 11f), tint)
        }

        Ailment.WOUNDED -> {
            drawPath(path(2f to 14f, 6f to 7f, 14f to 2f, 10f to 9f), tint)
            listOf(5f to 10f, 8f to 8f, 11f to 5.5f).forEach { (x, y) -> drawLine(tint, Offset((x - 2f) * u, (y - 2f) * u), Offset((x + 2f) * u, (y + 2f) * u), 1.1f * u) }
        }

        Ailment.PARCHED -> {
            drawPath(path(8f to 2f, 12f to 9f, 11f to 13f, 8f to 14f, 5f to 13f, 4f to 9f), tint)
            val crack = Color(0xFF0B0E13)
            listOf(8f to 6f, 7f to 9f, 9f to 11f, 8f to 14f).zipWithNext { (x1, y1), (x2, y2) -> drawLine(crack, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), u) }
            drawLine(crack, Offset(7 * u, 9 * u), Offset(5 * u, 11 * u), u)
        }

        Ailment.DECAYED -> {
            drawCircle(tint, 5.5f * u, Offset(8 * u, 7 * u), style = androidx.compose.ui.graphics.drawscope.Stroke(1.6f * u))
            listOf(8f to 7f, 6f to 9f, 9f to 11f).zipWithNext { (x1, y1), (x2, y2) -> drawLine(tint, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), u) }
            listOf(4f to 14f, 8f to 15f, 12f to 14f).forEach { (x, y) -> drawCircle(tint, 0.9f * u, Offset(x * u, y * u)) }
        }

        Ailment.DRAINED -> {
            drawPath(path(2f to 2f, 14f to 2f, 9f to 9f, 7f to 9f), tint)
            drawPath(path(8f to 9f, 11f to 13f, 8f to 15f, 5f to 13f), tint)
        }

        null -> drawPath(path(8f to 1f, 9.8f to 5.2f, 14.3f to 5.6f, 10.9f to 8.6f, 11.9f to 13f, 8f to 10.7f, 4.1f to 13f, 5.1f to 8.6f, 1.7f to 5.6f, 6.2f to 5.2f), tint)
    }
}

internal fun ailmentLabel(view: AilmentView) = if (view.stacks > 1) ui("expedition.ailment_stacks", ui(view.ailment.key()), view.stacks) else ui(view.ailment.key())
