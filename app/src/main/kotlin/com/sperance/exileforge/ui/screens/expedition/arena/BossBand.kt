package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.TraitView
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FoeView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.phaseTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import kotlin.math.roundToInt

/** Сколько секунд висит плашка новой фазы. */
private const val PHASE_BANNER_SECONDS = 6.0

/** Цвета полосы босса и его каста. */
private val BossLife = Color(0xFFC6403A)
private val BossLifeTrack = Color(0xFF2A0F0E)
private val PhaseMark = Color(0xFFF5D58A)
private val CastTint = Color(0xFFA26BFF)

/**
 * Бой с боссом (3.92.0, макет B «крупный портрет»): над всем - полоса его здоровья во всю ширину с засечками порогов фаз
 * (пройденные тусклые) и щитом тонкой чертой, под ней - что он готовит и через сколько; по центру - крупный портрет, свита -
 * узкими карточками по бокам; новая фаза - плашкой на несколько секунд. Портрет, как карточка, выбирает цель.
 */
@Composable internal fun BossBand(
    fight: FightHud,
    boss: BossHud,
    time: Float,
    chosen: Int?,
    large: Boolean,
    traits: Map<Int, List<TraitView>>,
    track: (Int) -> Modifier,
    onFocus: (Int) -> Unit,
) {
    val foe = fight.foes.firstOrNull { it.index == boss.index } ?: return
    val name = monsterTitle(foe.monster.code)
    val lore = LocalLore.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LifeBar(foe, name, boss)
        boss.cast?.let { cast ->
            Column(
                Modifier.fillMaxWidth().then(if (lore != null) Modifier.clickable { lore(Lore.Skill(cast.code, name, null, fight.heroBody)) } else Modifier),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(PanelRaised)) {
                    Box(Modifier.fillMaxWidth(cast.progress).fillMaxHeight().background(CastTint))
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(SkillText.title(cast.code), color = Color(0xFFD4B6FF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    MutedText(ui("boss.cast_in", fineNumber(cast.left)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        val others = fight.field.filter { it.index != boss.index }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            others.filterIndexed { i, _ -> i % 2 == 0 }.takeIf { it.isNotEmpty() }?.let { left -> Retinue(left, fight, time, chosen, large, traits, track, onFocus) }
            BossPortrait(foe, fight, time, chosen == foe.index, track(foe.index).weight(1f)) { onFocus(foe.index) }
            others.filterIndexed { i, _ -> i % 2 == 1 }.takeIf { it.isNotEmpty() }?.let { right -> Retinue(right, fight, time, chosen, large, traits, track, onFocus) }
        }
        if (foe.alive && (foe.ailments.isNotEmpty() || foe.effects.isNotEmpty() || foe.held)) StateTiles(foe.ailments, foe.held, foe.effects)
        phaseBanner(fight, boss)?.let { (code, at) ->
            Text(
                ui("boss.phase_banner", phaseTitle(code), at.roundToInt()),
                color = PhaseMark,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF3A2410)).border(1.dp, Color(0xFF6B4A1E), RoundedCornerShape(8.dp))
                    .then(if (lore != null) Modifier.clickable { lore(Lore.Phase(code, name)) } else Modifier).padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

/** Последняя фаза босса, если она сработала не раньше [PHASE_BANNER_SECONDS] назад: шаблон и порог. */
private fun phaseBanner(fight: FightHud, boss: BossHud): Pair<String, Double>? {
    val now = fight.events.firstOrNull()?.time ?: return null
    val event = fight.events.firstOrNull { it.foe == boss.index && it.actor == Side.MONSTER && (it.trace as? NoteTrace)?.kind == NoteKind.PHASE } ?: return null
    if (now - event.time > PHASE_BANNER_SECONDS) return null
    val note = event.trace as NoteTrace
    return note.ref to note.value
}

/** Полоса здоровья босса во всю ширину: имя и доля внутри, щит - тонкой чертой поверх, засечки порогов фаз. */
@Composable private fun LifeBar(foe: FoeView, name: String, boss: BossHud) {
    val share = if (foe.maxLife > 0) foe.life / foe.maxLife.toFloat() else 0f
    val shape = RoundedCornerShape(6.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().height(22.dp).clip(shape).background(BossLifeTrack).border(1.dp, Color(0xFF4A1D1B), shape)) {
        Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(BossLife))
        if (foe.maxShield > 0) Box(Modifier.fillMaxWidth((foe.shield / foe.maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).background(ShieldCyan))
        boss.marks.forEachIndexed { i, at ->
            val passed = boss.passed.getOrElse(i) { false }
            Box(Modifier.offset(x = maxWidth * (at / 100).toFloat()).width(2.dp).fillMaxHeight().background(if (passed) PhaseMark.copy(alpha = .35f) else PhaseMark))
        }
        Text(
            "$name · ${(share * 100).roundToInt()}%",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** Крупный портрет босса: вспышка удара и цвет состояния, рамка цели; касание выбирает его целью. */
@Composable private fun BossPortrait(foe: FoeView, fight: FightHud, time: Float, open: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val ring = rarityTint(foe.monster.rarity)
    val wash = foe.ailments.map { it.ailment }.maxByOrNull(::washAmount)
    val focused = fight.focus == foe.index
    Canvas(
        modifier.height(150.dp).clip(shape).background(Color(0xFF1A0F0E))
            .border(if (focused || open) 2.dp else 1.dp, if (focused || open) GoldBright else ring.copy(alpha = .6f), shape)
            .clickable(enabled = foe.alive && fight.outcome == null, onClick = onTap),
    ) {
        Portraits.monster(this, foe.monster.code.value, foe.monster.form, ring, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash(fight.lunge, Side.MONSTER, foe.index))
    }
}

/** Свита сбоку: узкие карточки столбиком. */
@Composable private fun Retinue(
    foes: List<FoeView>,
    fight: FightHud,
    time: Float,
    chosen: Int?,
    large: Boolean,
    traits: Map<Int, List<TraitView>>,
    track: (Int) -> Modifier,
    onFocus: (Int) -> Unit,
) {
    Column(Modifier.width(72.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        foes.forEach { foe ->
            androidx.compose.runtime.key(foe.index) {
                FoeCard(foe, fight, time, chosen == foe.index && fight.scouting, track(foe.index).fillMaxWidth(), large, traits[foe.index].orEmpty(), narrow = true) { onFocus(foe.index) }
            }
        }
    }
}
