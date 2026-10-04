package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.DamageNumbers
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/** The pack's leader by name and what the pack is: its rarity, the map's level, the stage of a gathered fight, how many are left standing. */
@Composable internal fun PackHeader(fight: FightHud, level: Int) {
    val leader = fight.leader
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                monsterTitle(leader.code),
                color = rarityTint(leader.rarity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val line = listOfNotNull(
                ui("expedition.monster_line", ui(leader.rarity.key()), level),
                ui("fight.stage", fight.stage, fight.stages).takeIf { fight.stages > 1 },
                ui("expedition.pack_left", fight.standing, fight.foes.size).takeIf { fight.foes.size > 1 },
            ).joinToString(" · ")
            Text(line, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
        BugAction(Modifier.align(Alignment.TopEnd))
    }
}

/** A swing and a skill's blow carry a card toward the other side (2.78.0); a tick, a reflection, a draught do not. */
internal val Action.strikes: Boolean get() = this == Action.ATTACK || this == Action.SKILL

/** How far a card is carried toward the other side by its own swing: out and back over the lunge. */
internal fun reach(lunge: LungeView?, actor: Side, foe: Int?): Float {
    lunge ?: return 0f
    if (lunge.actor != actor || !lunge.action.strikes || (foe != null && lunge.foe != foe)) return 0f
    return sin(lunge.progress * PI).toFloat()
}

/** Whether this card is the one being struck right now, past the lunge's midpoint. */
internal fun struck(lunge: LungeView?, target: Side, foe: Int?): Boolean {
    lunge ?: return false
    if (lunge.actor == target || !lunge.action.strikes || lunge.progress < .5f) return false
    return foe == null || lunge.foe == foe
}

/** How white a portrait flashes as a landed blow reaches it. */
internal fun flash(lunge: LungeView?, target: Side, foe: Int?): Float = if (lunge != null && lunge.landed && struck(lunge, target, foe)) (1 - lunge.progress) * 2f * (if (lunge.kind == HitKind.CRIT) 1f else .6f) else 0f

/**
 * One foe's card: its portrait in its rarity's frame, its name, life and shield, its swing and what
 * is on it. Lit and lowered toward the hero while it swings, ringed in blood while struck, ringed in
 * gold when the player singled it out, marked with a sight when the hero's next blow goes to it.
 * The fallen go dark; one the hero's weapon cannot reach yet is dimmed.
 */
@Composable internal fun FoeCard(
    foe: FoeView,
    fight: FightHud,
    time: Float,
    open: Boolean,
    modifier: Modifier,
    large: Boolean,
    traits: List<TraitView>,
    onTap: () -> Unit,
) {
    val lunge = fight.lunge
    val ring = rarityTint(foe.monster.rarity)
    val acting = reach(lunge, Side.MONSTER, foe.index)
    val hit = struck(lunge, Side.HERO, foe.index)
    val focused = fight.focus == foe.index
    val shape = RoundedCornerShape(8.dp)
    val border = when {
        acting > 0f -> LifeRed
        hit -> LifeRed.copy(alpha = .8f)
        focused || open -> GoldBright
        else -> ring.copy(alpha = .55f)
    }
    val wash = foe.ailments.map { it.ailment }.maxByOrNull(::washAmount)
    // Variant C (3.80.0): a round portrait beside the name; a blow, a stun, the frost play over the whole card.
    Box(
        modifier.graphicsLayer {
            translationY = acting * 10.dp.toPx()
            translationX = if (hit && foe.alive) sin(time * 60f) * 2.dp.toPx() else 0f
            // A stun gone off (3.78.0) tilts the card while it holds.
            rotationZ = if (foe.alive && foe.buildup?.stunned == true) -3f else 0f
            alpha = when {
                !foe.alive -> .35f
                !foe.reachable && fight.started -> .7f
                else -> 1f
            }
        }
            .background(if (acting > 0f) Blood.copy(alpha = .35f) else Panel.copy(alpha = .9f), shape)
            .then(if (foe.taunt && foe.alive && acting == 0f && !hit && !focused && !open) Modifier.tauntAura(shape, time) else Modifier)
            .border(if (focused || acting > 0f) 2.dp else 1.dp, border, shape)
            .clip(shape).clickable(enabled = foe.alive && fight.outcome == null, onClick = onTap),
    ) {
        Column(Modifier.fillMaxWidth().padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(FOE_AVATAR).clip(CircleShape).background(Color(0xFF0B0E13)).border(2.dp, ring.copy(alpha = .8f), CircleShape)) {
                    Canvas(Modifier.fillMaxSize()) {
                        Portraits.monster(
                            this,
                            foe.monster.code.value,
                            foe.monster.form,
                            ring,
                            time,
                            wash?.let(::ailmentTint),
                            wash?.let(::washAmount) ?: 0f,
                            flash(lunge, Side.MONSTER, foe.index),
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        monsterTitle(foe.monster.code),
                        color = ring,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp,
                    )
                    // Its own level (3.73.0): on a map a foe may stand a little above or below the map.
                    if (foe.monster.level > 0) Text(ui("fight.level_short", foe.monster.level), color = Muted, fontSize = 9.sp, maxLines = 1)
                    // Its traits (3.73.0) as seals; a tap on the card opens what they do.
                    if (traits.isNotEmpty() && foe.alive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            traits.forEach { trait -> SkillGlyph(trait.icon, Modifier.size(14.dp), Color(0xFFE8B06A)) }
                        }
                    }
                }
            }
            LifeBar(foe.life, foe.maxLife, foe.shield, foe.maxShield, Modifier.fillMaxWidth().height(12.dp))
            // A caster's or a boss's mana (2.78.0), a thread under its life: what its spells are paid with.
            if (foe.maxMana > 0) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth((foe.mana / foe.maxMana.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(ManaBlue, RoundedCornerShape(2.dp)))
                }
            }
            if (foe.alive) foe.buildup?.let { BuildupBar(it, Modifier.fillMaxWidth()) }
            SwingBar(foe.swing, foe.held, Modifier.fillMaxWidth(), if (acting > 0f) LifeRed else LifeRed.copy(alpha = .7f))
            if (foe.taunt && foe.alive) Text(ui("fight.taunt"), color = Color(0xFFE8B06A), fontSize = 9.sp, fontStyle = FontStyle.Italic, maxLines = 1)
            // What is on it: small tiles while it runs, larger while paused; a tap on one opens its window.
            val tile = if (large) 16.dp else 12.dp
            if (foe.held || foe.ailments.isNotEmpty() || foe.effects.isNotEmpty()) {
                Row(Modifier.height(tile), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (foe.held && foe.ailments.none { it.ailment == Ailment.FROZEN }) {
                        StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned"), tile) { stunTip() }
                    }
                    foe.ailments.take(5).forEach { view ->
                        StateTile(view.ailment, ailmentTint(view.ailment), view.left, view.stacks, ailmentLabel(view), tile) { ailmentTip(view) }
                    }
                    foe.effects.take(3).forEach { EffectTile(it, tile) }
                }
            }
        }
        if (fight.target == foe.index && foe.alive && fight.outcome == null) {
            Text(if (focused) "◉" else "◎", color = GoldBright, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
        if (foe.taunt && foe.alive) TauntSeal(time, Modifier.align(Alignment.TopStart).padding(2.dp).size(18.dp)) { tauntTip(false) }
        if (!foe.alive) {
            foe.reinforce?.let { left -> ReinforceRing(left, foe.reinforceDelay, Modifier.align(Alignment.Center).size(40.dp)) }
                ?: Text(ui("fight.fallen"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center))
        } // A foe singled out behind a standing taunter: the focus holds, the blows go to the taunter.
        else if (focused && !foe.reachable) {
            Text(
                ui("fight.out_of_reach_short"),
                color = Muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomCenter).background(Ink.copy(alpha = .8f)).padding(horizontal = 4.dp),
            )
        }
        if (foe.alive) foe.buildup?.let { BuildupMark(it, time) }
        CardHits(fight.hits.filter { it.target == Side.MONSTER && it.foe == foe.index })
    }
}

/** The round portrait of a foe's card (3.80.0, variant C). */
private val FOE_AVATAR = 52.dp

/** The place of a fallen foe while the next of the line closes in (3.73.0): a ring running down and the seconds left. */
@Composable private fun ReinforceRing(left: Double, delay: Double, modifier: Modifier) {
    val share = if (delay > 0) (left / delay).toFloat().coerceIn(0f, 1f) else 0f
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Muted.copy(alpha = .25f), style = Stroke(3.dp.toPx()))
            drawArc(GoldBright, -90f, 360f * share, useCenter = false, style = Stroke(3.dp.toPx()))
        }
        Text(ceil(left).toInt().toString(), color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** The numbers rising off a card: the blows that reached it in the last second. */
@Composable internal fun BoxScope.CardHits(hits: List<FloatingHit>) {
    // The settings (3.77.0) choose which numbers rise: all, the critical strikes alone, or none.
    val shown = when (LocalSettings.current.damageNumbers) {
        DamageNumbers.ALL -> hits
        DamageNumbers.CRITS -> hits.filter { it.kind == HitKind.CRIT }
        DamageNumbers.OFF -> return
    }
    shown.takeLast(3).forEach { hit ->
        val rise = (hit.age / ExpeditionRun.HIT_LIFETIME).toFloat().coerceIn(0f, 1f)
        Text(
            hitText(hit),
            color = hitColour(hit).copy(alpha = 1 - rise),
            textAlign = TextAlign.Center,
            fontSize = when {
                hit.kind == HitKind.CRIT -> 20.sp
                hit.action == Action.TICK -> 12.sp
                else -> 16.sp
            },
            fontWeight = if (hit.action == Action.TICK) FontWeight.Normal else FontWeight.Black,
            fontStyle = if (hit.action == Action.TICK) FontStyle.Italic else FontStyle.Normal,
            modifier = Modifier.align(Alignment.Center).offset(x = (((hit.id % 3) - 1) * 14).dp, y = (-36 * rise).dp),
        )
    }
}

private fun hitText(hit: FloatingHit): String = when {
    // A heal (2.78.0): a skill's or a draught's, rising in green.
    hit.amount == 0 && hit.healed > 0 -> "+${hit.healed}"

    hit.kind == HitKind.EVADED -> ui("expedition.evaded")

    hit.kind == HitKind.BLOCKED -> ui("expedition.blocked")

    hit.kind == HitKind.CRIT -> ui("expedition.crit", hit.amount)

    else -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when {
    hit.amount == 0 && hit.healed > 0 -> Vital

    // The pet's blows rise green on the foe (3.79.0).
    hit.pet && hit.target == Side.MONSTER && hit.amount > 0 -> Vital

    hit.kind == HitKind.EVADED || hit.kind == HitKind.BLOCKED -> Muted

    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)

    else -> damageTint(hit.type, onHero = hit.target == Side.HERO)
}
