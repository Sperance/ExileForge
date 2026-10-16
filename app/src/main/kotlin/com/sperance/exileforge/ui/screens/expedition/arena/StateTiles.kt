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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
 * Предначертания (4.9.0) - золотой ✦ дара [fates]. С 4.9.3 - полка эффектов ([EffectShelf]): что не влезло - «+N», касание
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

/**
 * Действующий рычаг дара (4.9.0, макет «Свиток + значок»): ✦ золотом Предначертания, тёмная заливка растёт по мере ухода срока,
 * в углу - стаки; подсказка - имя дара и что он делает сейчас (`fight.fate_state.<рычаг>`).
 */
@Composable internal fun FateTile(view: FateGlow, side: Dp = 30.dp, tip: () -> Tip) {
    val shape = RoundedCornerShape(4.dp)
    val title = locOr("fate.${view.fate}.name", view.fate)
    Tipped(
        tip,
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(FateInk.Gold.copy(alpha = .16f)).border(1.dp, FateInk.Gold, shape)
            .semantics { contentDescription = title },
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

/** A kind of the hero's charges (3.33.0): the drawing of its maximum and the count held. */
@Composable internal fun ChargeTile(view: ChargeView, side: Dp = 30.dp, tip: () -> Tip) {
    val shape = RoundedCornerShape(4.dp)
    val title = ui("fight.charge.${view.kind.name}")
    Tipped(
        tip,
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(Gold.copy(alpha = .16f)).border(1.dp, Gold, shape)
            .semantics { contentDescription = "$title ${view.count}" },
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

/** A buff or a curse (2.78.0) as a tile: the skill's mark in gold for a buff, in blood for a curse, darkening as it wears off. */
@Composable internal fun EffectTile(view: EffectView, side: Dp = 30.dp, tip: () -> Tip) {
    val tint = effectTint(view)
    val shape = RoundedCornerShape(4.dp)
    // A buff of the rules (3.35.0) — Onslaught, Fortify — is named and drawn by its kind, a skill's by the skill; глоток (4.9.3) - бутылью флакона.
    val buff = view.buff
    // Бафф Предначертания (4.6.0) - не умение: называется даром
    val title = effectTitle(view)
    // Проклятие (3.92.0) открывает лист: что меняет, сколько ещё, чем снять; бафф - подсказку [tip]
    val lore = LocalLore.current?.takeIf { view.kind == EffectKind.CURSE && buff == null }
    Tipped(
        tip.takeIf { lore == null },
        Modifier.then(if (lore != null) Modifier.clickable { lore(Lore.Curse(view.source, null, view.seconds)) } else Modifier).size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
            .semantics { contentDescription = title },
    ) {
        val flask = view.flask
        if (flask != null) {
            FlaskBottle(flask, 1f, true, Modifier.fillMaxSize().padding(horizontal = side / 4, vertical = side / 8))
        } else if (buff != null) {
            com.sperance.exileforge.ui.icons.StatIcon(buff.icon, tint, Modifier.fillMaxSize().padding(side / 6))
        } else if (view.source == PROVOKED) {
            // Провокация героя на враге (4.12.x, STAT-DEF-05) - знаком своей характеристики
            com.sperance.exileforge.ui.icons.StatIcon(CoreStat.PROVOKE.code, tint, Modifier.fillMaxSize().padding(side / 6))
        } else {
            SkillGlyph(view.icon, Modifier.fillMaxSize().padding(side / 6), tint)
        }
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - view.left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
    }
}

@Composable internal fun StateTile(ailment: Ailment?, tint: Color, left: Float, stacks: Int, label: String, side: Dp = 30.dp, tip: () -> Tip) {
    val shape = RoundedCornerShape(4.dp)
    Tipped(
        tip,
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
            .semantics { contentDescription = label },
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
 * An ailment's mark drawn from shapes: a flame, a snowflake, a crystal, a bolt, a drop, two drops; a star for a stun. Изранение
 * (4.11.0) - рваная рана со стежками, иссушение - капля в трещинах.
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

        // Разложение (0.15.0) - треснувший круг с тремя каплями распада; высасывание сути - капля, втянутая в воронку
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
