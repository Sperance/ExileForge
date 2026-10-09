package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.skills.FlaskBottle
import com.sperance.exileforge.ui.screens.skills.SkillFacts
import com.sperance.exileforge.ui.theme.*

/**
 * The hero's skills and belt in the fight (2.78.0): фляги слева, умения справа (4.4.1, решение владельца) - каждое квадратной
 * иконкой «Неон рун» ([NeonSkillIcon]) своего тира: откат - тёмный сектор и секунды, нехватка маны - синяя подложка, пустой
 * слот - пунктирный квадрат того же размера. A tap uses a skill or drinks a flask at once, whatever its condition; a long press
 * on a skill, or its «i», opens its page (3.24.0).
 */
@Composable internal fun ActionBar(game: GameUi, fight: FightHud, onCommand: (RunCommand) -> Unit, onInfo: (SkillView) -> Unit) {
    val live = fight.started && fight.outcome == null
    val index = game.index
    val skills = game.hero?.skills ?: HeroSkills()
    val stats = fight.heroBody?.stats ?: game.hero?.stats.orEmpty()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        fight.flasks.forEach { view ->
            if (view == null) {
                Box(Modifier.size(44.dp).border(1.dp, Bronze.copy(alpha = .35f), CircleShape))
            } else {
                FlaskButton(view, live) { onCommand(RunCommand.Drink(view.slot)) }
            }
        }
        Spacer(Modifier.weight(1f))
        fight.skills.forEach { view ->
            if (view == null) {
                EmptySkillSlot()
            } else {
                val skill = index?.skills?.byCode?.get(view.code)
                val tier = remember(skill, skills, stats) { skill?.let { index?.skillGrowth?.tier(it, skills, stats) } ?: HeroSkills.FIRST_TIER }
                SkillButton(view, skill, tier, live, onInfo = { onInfo(view) }) { onCommand(RunCommand.Cast(view.slot)) }
            }
        }
    }
}

/** Сторона квадрата умения в бою (4.4.1). */
private val SKILL_SIDE = 52.dp

/** Скругление плитки умения - как у [NeonSkillIcon]: четверть стороны. */
private val SkillShape = RoundedCornerShape(SKILL_SIDE / 4)

/** Пустой слот умения: пунктирный квадрат размера иконки. */
@Composable private fun EmptySkillSlot() {
    Canvas(Modifier.size(SKILL_SIDE)) {
        val line = 1.dp.toPx()
        drawRoundRect(
            Bronze.copy(alpha = .6f),
            topLeft = Offset(line / 2, line / 2),
            size = Size(size.width - line, size.height - line),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension / 4),
            style = Stroke(line, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillButton(view: SkillView, skill: SkillDefinition?, tier: Int, live: Boolean, onInfo: () -> Unit, onTap: () -> Unit) {
    val ready = view.ready >= 1f && view.locked <= 0.0
    Box(
        Modifier.size(SKILL_SIDE)
            .combinedClickable(onLongClick = onInfo) { if (live && ready && view.affordable) onTap() }
            .semantics { contentDescription = SkillText.title(view.code) },
    ) {
        if (skill != null) {
            NeonSkillIcon(skill, tier, SKILL_SIDE)
        } else {
            SkillGlyph(view.icon, Modifier.size(26.dp).align(Alignment.Center), GoldBright)
        }
        // Нехватка маны (4.4.1): синяя подложка под отметками слота
        if (!view.affordable) Box(Modifier.matchParentSize().clip(SkillShape).background(ManaBlue.copy(alpha = .28f)))
        // Откат - тёмный сектор того, что ещё восстанавливается, по часовой от верха, и секунды
        if (!ready && view.locked <= 0.0) {
            Canvas(Modifier.matchParentSize().clip(SkillShape)) {
                val reach = size.maxDimension * 1.5f
                drawArc(
                    Color.Black.copy(alpha = .62f),
                    -90f + 360f * view.ready,
                    360f * (1 - view.ready),
                    useCenter = true,
                    topLeft = Offset(center.x - reach / 2, center.y - reach / 2),
                    size = Size(reach, reach),
                )
            }
            Text(fineNumber(view.seconds), color = Parchment, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }
        if (view.locked > 0.0) SealedSlot(view.locked, SkillShape)
        Text(
            "${view.cost}",
            color = if (view.affordable) Rune else LifeRed,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 2.dp),
        )
        if (view.condition == SlotCondition.MANUAL) {
            Text("✋", fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).padding(3.dp))
        }
        Box(Modifier.align(Alignment.BottomStart).size(18.dp).clickable(onClickLabel = ui("fight.skill_info"), onClick = onInfo), contentAlignment = Alignment.Center) {
            Text(
                "i",
                color = Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.size(12.dp).border(1.dp, Muted.copy(alpha = .6f), CircleShape).wrapContentSize(Alignment.Center),
            )
        }
    }
}

/**
 * A skill's page over the fight (3.24.0): what the grimoire says of it at its level — damage, cost, cooldown,
 * preparation and effects — with the condition its slot fires on. The fight holds while the page is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FightSkillSheet(game: GameUi, view: SkillView, onCommand: (RunCommand) -> Unit, onDismiss: () -> Unit) {
    DisposableEffect(view.slot) {
        onCommand(RunCommand.Hold(true))
        onDispose { onCommand(RunCommand.Hold(false)) }
    }
    val index = game.index
    val skill = index?.skills?.byCode?.get(view.code)
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkillGlyph(view.icon, Modifier.size(40.dp), GoldBright)
                Column(Modifier.weight(1f)) {
                    Text(SkillText.title(view.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                    skill?.let { Text(skillKindLine(it), color = Muted, style = MaterialTheme.typography.labelMedium) }
                }
                Text(ui("skills.level_short", view.level), color = Gold, style = MaterialTheme.typography.titleLarge)
            }
            if (index != null && skill != null) {
                SkillFacts(index, skill, view.level, game.hero?.stats.orEmpty(), condition = view.condition)
            } else {
                MutedText(ui("common.loading"))
            }
        }
    }
}

@Composable private fun FlaskButton(view: FlaskView, live: Boolean, onTap: () -> Unit) {
    val tint = flaskTint(view.kind)
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color(0xE60A0D12))
            .border(if (view.active > 0f) 2.dp else 1.dp, if (view.active > 0f) GoldBright else Bronze, CircleShape)
            .clickable(enabled = live && view.usable, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val fill = if (view.maxCharges > 0) view.charges / view.maxCharges.toFloat() else 0f
            drawRect(tint.copy(alpha = if (view.usable) .55f else .25f), topLeft = Offset(0f, size.height * (1 - fill)), size = Size(size.width, size.height * fill))
            if (view.active > 0f) drawArc(GoldBright, -90f, 360f * view.active, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        FlaskBottle(view.kind, 0f, true, Modifier.size(14.dp, 22.dp))
        Text("${view.charges}", color = GoldBright, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
        if (view.locked > 0.0) SealedSlot(view.locked, CircleShape)
    }
}

/** Слот, запертый «Запечатыванием» Стража Врат (3.96.0): тёмная печать Разлома и сколько ещё секунд. */
@Composable private fun BoxScope.SealedSlot(seconds: Double, shape: Shape) {
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .65f), shape).border(1.5.dp, RiftSeal, shape), contentAlignment = Alignment.Center) {
        Text("◈ ${fineNumber(seconds)}", color = RiftSeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
