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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.skills.FlaskBottle
import com.sperance.exileforge.ui.screens.skills.SkillFacts
import com.sperance.exileforge.ui.theme.*

/**
 * The hero's skills and belt in the fight (2.78.0): the three active slots — dark while they recover,
 * dim while the mana is short — then the three flasks, filled to their charges and ringed while one runs.
 * A tap uses a skill or drinks a flask at once, whatever its condition; a long press on a skill, or its «i»,
 * opens its page (3.24.0).
 */
@Composable internal fun ActionBar(fight: FightHud, onCommand: (RunCommand) -> Unit, onInfo: (SkillView) -> Unit) {
    val live = fight.started && fight.outcome == null && !fight.retreating
    // Between stages the belt is open (3.28.0): a draught then is drunk as on the road.
    val drinkable = live || (!fight.started && fight.interlude != null)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        fight.skills.forEach { view ->
            if (view == null) {
                Box(Modifier.weight(1f).height(52.dp).border(1.dp, Bronze.copy(alpha = .35f), RoundedCornerShape(8.dp)))
            } else {
                SkillButton(view, live, Modifier.weight(1f), onInfo = { onInfo(view) }) { onCommand(RunCommand.Cast(view.slot)) }
            }
        }
        Spacer(Modifier.width(4.dp))
        fight.flasks.forEach { view ->
            if (view == null) {
                Box(Modifier.size(44.dp).border(1.dp, Bronze.copy(alpha = .35f), CircleShape))
            } else {
                FlaskButton(view, drinkable) { onCommand(RunCommand.Drink(view.slot)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillButton(view: SkillView, live: Boolean, modifier: Modifier, onInfo: () -> Unit, onTap: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val ready = view.ready >= 1f
    // A skill that can go now glows (2.80.0, «Эфир»): the light is the readiness.
    Box(
        modifier.height(52.dp).glow(Gold, on = ready && view.affordable, radius = 10.dp, shape = shape).clip(shape).background(PanelRaised, shape)
            .border(if (ready && view.affordable) 1.5.dp else 1.dp, if (ready && view.affordable) Gold else Bronze, shape)
            .combinedClickable(onLongClick = onInfo) { if (live && ready && view.affordable) onTap() }
            .semantics { contentDescription = SkillText.title(view.code) },
    ) {
        SkillGlyph(view.icon, Modifier.size(26.dp).align(Alignment.Center), if (view.affordable) GoldBright else Muted)
        // What is left to recover darkens the button from the top, as a flask's charge fills it from the bottom.
        if (!ready) Box(Modifier.fillMaxWidth().fillMaxHeight(1 - view.ready).background(Color.Black.copy(alpha = .6f)))
        if (!ready) Text(fineNumber(view.seconds), color = Parchment, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        Text(
            "${view.cost}",
            color = if (view.affordable) Rune else LifeRed,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 1.dp),
        )
        if (view.condition == SlotCondition.MANUAL) {
            Text("✋", fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).padding(2.dp))
        }
        Text("${view.level}", color = Gold, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 3.dp))
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
internal fun FightSkillSheet(s: ForgeState, view: SkillView, onCommand: (RunCommand) -> Unit, onDismiss: () -> Unit) {
    DisposableEffect(view.slot) {
        onCommand(RunCommand.Hold(true))
        onDispose { onCommand(RunCommand.Hold(false)) }
    }
    val index = s.index
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
                SkillFacts(index, skill, view.level, s.hero?.stats.orEmpty(), condition = view.condition)
            } else {
                MutedText(ui("common.loading"))
            }
        }
    }
}

@Composable private fun FlaskButton(view: FlaskView, live: Boolean, onTap: () -> Unit) {
    val tint = flaskTint(view.kind)
    Box(
        Modifier.size(44.dp).glow(tint, on = view.active > 0f, radius = 10.dp, shape = CircleShape).clip(CircleShape).background(Color(0xE60A0D12))
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
    }
}
