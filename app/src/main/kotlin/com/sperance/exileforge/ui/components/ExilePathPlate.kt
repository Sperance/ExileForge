package com.sperance.exileforge.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.presentation.state.TAB_CRAFT
import com.sperance.exileforge.presentation.state.TAB_EXPEDITION
import com.sperance.exileforge.presentation.state.TAB_HERO
import com.sperance.exileforge.presentation.state.TAB_PROGRESS
import com.sperance.exileforge.presentation.state.TAB_SKILLS
import com.sperance.exileforge.presentation.state.TAB_TREE
import com.sperance.exileforge.rules.content.PathCheck
import com.sperance.exileforge.rules.content.PathStep
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*

/** Where the player does what a step asks (3.79.0): the plate's «Туда» opens it, and the bottom bar's tab of it pulses. */
val PathCheck.tab: Int get() = when (this) {
    PathCheck.ZONE, PathCheck.BOSS -> TAB_EXPEDITION
    PathCheck.EQUIP -> TAB_HERO
    PathCheck.TREE -> TAB_TREE
    PathCheck.SKILL -> TAB_SKILLS
    PathCheck.ORB -> TAB_CRAFT
}

/** The bottom bar's destination a tab is reached from: the tree and the grimoire are the hero's, the forge the hub's. */
val PathCheck.destination: Int get() = when (this) {
    PathCheck.TREE, PathCheck.SKILL, PathCheck.EQUIP -> TAB_HERO
    PathCheck.ORB -> TAB_PROGRESS
    PathCheck.ZONE, PathCheck.BOSS -> TAB_EXPEDITION
}

/** The step the hero stands on and whether it is done; null once the path is walked or before the hero is read. */
fun ForgeState.pathStep(): Pair<PathStep, Boolean>? {
    val hero = hero ?: return null
    val step = index?.rules?.path?.step(hero.info.pathStep) ?: return null
    return step to hero.pathFacts.done(step.check)
}

/** A pulse for what the player should tap next: 1 → [peak] and back, forever. */
@Composable fun pulse(peak: Float = 1.08f): Float {
    val beat by rememberInfiniteTransition(label = "pulse").animateFloat(1f, peak, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "beat")
    return beat
}

/**
 * The Exile's Path (3.79.0): a thin plate under the banner for the first hour — the step, one line of what to do,
 * the steps filling, and one pulsing button: «Туда» while the step waits, «Забрать» once it is done.
 */
@Composable fun ExilePathPlate(s: ForgeState, onGo: (Int) -> Unit, onClaim: () -> Unit) {
    val (step, done) = s.pathStep() ?: return
    val total = s.index?.rules?.path?.steps?.size ?: return
    val at = s.hero?.info?.pathStep ?: return
    val shape = RoundedCornerShape(10.dp)
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp).background(PanelRaised, shape)
        .border(1.dp, (if (done) GoldBright else Gold).copy(alpha = .4f), shape).padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(ForgeGlyphs.Sigil, null, tint = Gold, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ui("path.title", at + 1, total) + " · " + ui("path.${step.code.lowercase()}.title"), color = GoldBright, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (done) ui("path.done") else ui("path.${step.code.lowercase()}.text"), color = Parchment, style = MaterialTheme.typography.labelSmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(progress = { (at + if (done) 1 else 0).toFloat() / total }, modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Gold, trackColor = Abyss)
        }
        TextButton(onClick = { if (done) onClaim() else onGo(step.check.tab) }, enabled = !s.busy, modifier = Modifier.scale(pulse())) {
            Text(ui(if (done) "path.claim" else "path.go"), color = if (done) GoldBright else Gold)
        }
    }
}
