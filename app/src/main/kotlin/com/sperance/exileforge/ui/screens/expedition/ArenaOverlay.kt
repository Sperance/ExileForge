package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.features.key
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.scene.FightLayout
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt

internal fun rarityTint(rarity: MonsterRarity) = when (rarity) {
    MonsterRarity.NORMAL -> Parchment
    MonsterRarity.MAGIC -> Color(0xFF8888FF)
    MonsterRarity.RARE -> Color(0xFFFFFF77)
}

/** Each damage type's colour, on a number, in the log and on the scene alike. */
internal fun damageTint(type: DamageType?, onHero: Boolean = false): Color = when (type) {
    DamageType.FIRE -> Ember
    DamageType.COLD -> ShieldCyan
    DamageType.LIGHTNING -> Color(0xFFFFD34A)
    DamageType.CHAOS -> Elder
    DamageType.MAGICAL -> Rune
    else -> if (onHero) LifeRed else Parchment
}

/** Each ailment's colour: the damage that brings it, or the state it leaves. */
internal fun ailmentTint(ailment: Ailment): Color = when (ailment) {
    Ailment.BURNING -> Ember
    Ailment.CHILLED -> ShieldCyan
    Ailment.FROZEN -> Shaper
    Ailment.SHOCKED -> Color(0xFFFFD34A)
    Ailment.POISONED -> Vital
    Ailment.BLEEDING -> LifeRed
}

internal fun Ailment.key() = "enum.ailment.$name"
internal fun DamageType.key() = "enum.damage.$name"

/**
 * The fight — «Арена» (2.26.0, reworked in 2.27.0): the two sides named on top, each with life,
 * shield, the hero's mana, a bar that fills toward its next swing at its own attack speed and a
 * second one toward its next spell, and whatever is on it — burning, chilled, frozen, shocked,
 * poisoned, bleeding — as chips that drain as the ailment runs out; the fighters in the middle,
 * drawn by the scene; the log under them, newest line first, which unfolds over the scene on
 * demand; and the player's two hands: the flask and the way out.
 *
 * Everything here is read off [FightHud]; the numbers flying off the fighters are placed over the
 * spots [FightLayout] gives the scene, so a hit rises from the one who took it, coloured by what it was.
 */
@Composable internal fun ArenaOverlay(s: ForgeState, hud: RunHud, fight: FightHud, level: Int, onCommand: (RunCommand) -> Unit) {
    var logOpen by rememberSaveable { mutableStateOf(false) }
    val monster = fight.monster
    val hero = s.play.hero?.character
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        val density = LocalDensity.current
        fight.hits.forEach { hit ->
            val column = if (hit.target == Side.HERO) FightLayout.HERO_X else FightLayout.MONSTER_X
            val rise = (hit.age / ExpeditionRun.HIT_LIFETIME).toFloat()
            val x = with(density) { (width * column).toPx() } + ((hit.id % 3) - 1) * 40f
            val y = with(density) { (height * (FightLayout.GROUND_Y - .3f)).toPx() } - rise * 120f
            val alpha = (1 - rise).coerceIn(0f, 1f)
            Column(Modifier.offset { IntOffset((x - 60f).roundToInt(), y.roundToInt()) }.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(hitText(hit), color = hitColour(hit).copy(alpha = alpha), textAlign = TextAlign.Center,
                    fontSize = when { hit.kind == HitKind.CRIT -> 30.sp; hit.action == Action.TICK -> 16.sp; else -> 22.sp },
                    fontWeight = if (hit.action == Action.TICK) FontWeight.Normal else FontWeight.Bold,
                    fontStyle = if (hit.action == Action.TICK) FontStyle.Italic else FontStyle.Normal)
                // What the blow left behind is said under the number: a stun, an ailment.
                val marks = (if (hit.stunned) listOf(ui("expedition.stunned")) else emptyList()) + hit.inflicted.map { ui(it.key()) }
                if (marks.isNotEmpty()) Text(marks.joinToString(" · "), color = (hit.inflicted.firstOrNull()?.let(::ailmentTint) ?: GoldBright).copy(alpha = alpha),
                    style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
        fight.outcome?.let {
            Text(ui("expedition.outcome_${it.name.lowercase()}"), color = outcomeColour(it), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Nameplate(Modifier.weight(1f), hero?.name.orEmpty(), ui("expedition.hero_line", s.heroClass?.title.orEmpty(), hero?.level ?: 1), GoldBright,
                    fight.heroLife, hud.heroMaxLife, fight.heroShield, hud.heroMaxShield, fight.heroMana, fight.heroMaxMana,
                    fight.heroSwing, fight.heroCast.takeIf { fight.heroCasts }, fight.heroAilments, fight.heroHeld, alignEnd = false)
                Nameplate(Modifier.weight(1f), monsterTitle(monster.code), ui("expedition.monster_line", ui(monster.rarity.key()), level), rarityTint(monster.rarity),
                    fight.monsterLife, fight.monsterMaxLife, fight.monsterShield, fight.monsterMaxShield, 0, 0,
                    fight.monsterSwing, fight.monsterCast.takeIf { fight.monsterCasts }, fight.monsterAilments, fight.monsterHeld, alignEnd = true)
            }
            // What the monster rolled sits under its own name: a tier's strength is read before the fight.
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.End) {
                monster.modifiers.forEach {
                    Text(monsterModifierText(it), color = Rune, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
                }
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth().animateContentSize().background(Panel.copy(alpha = .92f), RoundedCornerShape(10.dp))
                .border(1.dp, Bronze.copy(alpha = .5f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)
                .height(if (logOpen) height * .5f else 116.dp)) {
                FightLog(fight.events, monster.code)
            }
            val live = fight.outcome == null
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // The flask: what is left of it is the label, and it cannot be drunk twice at once.
                Button(enabled = live && fight.flasks > 0 && !fight.flaskActive, onClick = { onCommand(RunCommand.Flask) }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright, disabledContainerColor = Panel, disabledContentColor = Muted)) {
                    Icon(ForgeGlyphs.Flask, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(ui(if (fight.flaskActive) "expedition.flask_drinking" else "expedition.flask", fight.flasks, hud.maxFlasks))
                }
                OutlinedButton(enabled = live && !fight.retreating, onClick = { onCommand(RunCommand.Retreat) }, modifier = Modifier.weight(1f)) {
                    Text(ui(if (fight.retreating) "expedition.retreating" else "expedition.retreat"))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onCommand(RunCommand.Speed) }, modifier = Modifier.weight(1f)) { Text(ui("expedition.speed", fight.speed)) }
                OutlinedButton(onClick = { logOpen = !logOpen }, modifier = Modifier.weight(1f)) {
                    Text(ui(if (logOpen) "expedition.log_less" else "expedition.log"))
                }
            }
        }
    }
}

/** One side of the fight: its name, what it is, life with shield over it, mana if it has any, the swing and cast bars, and what is on it. */
@Composable private fun Nameplate(modifier: Modifier, name: String, line: String, tint: Color, life: Int, maxLife: Int,
    shield: Int, maxShield: Int, mana: Int, maxMana: Int, swing: Float, cast: Float?, ailments: List<AilmentView>, held: Boolean, alignEnd: Boolean) {
    val align = if (alignEnd) Alignment.End else Alignment.Start
    val shape = CutCornerShape(3.dp)
    val share by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    Column(modifier, horizontalAlignment = align, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(name, color = tint, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(line, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.fillMaxWidth().height(16.dp).background(Color(0xCC0A0D12), shape).border(1.dp, LifeRed.copy(alpha = .8f), shape)) {
            Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
            if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).background(ShieldCyan.copy(alpha = .85f)))
            Text(if (maxShield > 0) "$life / $maxLife · $shield" else "$life / $maxLife", color = Parchment, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.Center))
        }
        if (maxMana > 0) Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xCC0A0D12), shape).border(1.dp, ManaBlue.copy(alpha = .7f), shape)) {
            Box(Modifier.fillMaxWidth((mana / maxMana.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(ManaBlue, shape))
        }
        // The swing bar: full the instant the next blow lands. A held fighter's bar is dimmed: nothing lands while it stands stunned or frozen.
        Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(swing.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else Gold, RoundedCornerShape(2.dp)))
        }
        // The cast bar, for a side that knows a spell.
        cast?.let {
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(it.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else Rune, RoundedCornerShape(2.dp)))
            }
        }
        if (ailments.isNotEmpty() || held) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (held && ailments.none { it.ailment == Ailment.FROZEN }) AilmentChip(ui("expedition.stunned"), GoldBright, 1f)
            ailments.forEach { AilmentChip(if (it.stacks > 1) ui("expedition.ailment_stacks", ui(it.ailment.key()), it.stacks) else ui(it.ailment.key()), ailmentTint(it.ailment), it.left) }
        }
    }
}

/** A small coloured chip whose fill drains as the ailment wears off. */
@Composable private fun AilmentChip(label: String, tint: Color, left: Float) {
    val shape = RoundedCornerShape(4.dp)
    val fill = tint.copy(alpha = .3f)
    Box(Modifier.clip(shape).border(1.dp, tint.copy(alpha = .8f), shape).drawBehind {
        drawRect(Color(0xAA0A0D12))
        drawRect(fill, size = Size(size.width * left.coerceIn(0f, 1f), size.height))
    }) {
        Text(label, color = tint, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), maxLines = 1)
    }
}

/** The blows so far, newest first: when, who, and what came of it, coloured by what it was. */
@Composable internal fun FightLog(events: List<CombatEvent>, monsterCode: String, modifier: Modifier = Modifier) {
    val monster = monsterTitle(monsterCode)
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        items(events) { event ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", event.time)), color = Muted,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                Text(logLine(event, monster), color = logColour(event), style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (event.kind == HitKind.CRIT) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (event.action == Action.TICK) FontStyle.Italic else FontStyle.Normal)
            }
        }
    }
}

private fun logLine(event: CombatEvent, monster: String): String {
    val damage = event.damage.roundToInt()
    val hero = event.actor == Side.HERO
    val line = when (event.action) {
        Action.FLASK -> ui("expedition.log_flask", event.healed.roundToInt())
        Action.RETREAT -> ui("expedition.log_retreat")
        Action.TICK -> {
            val ailment = event.ailment?.let { ui(it.key()) }.orEmpty()
            if (hero) ui("expedition.log_tick_they", monster, damage, ailment) else ui("expedition.log_tick_you", damage, ailment)
        }
        Action.SPELL -> when (event.kind) {
            HitKind.CRIT -> if (hero) ui("expedition.log_you_spell_crit", damage) else ui("expedition.log_they_spell_crit", monster, damage)
            HitKind.BLOCKED -> if (hero) ui("expedition.log_they_block", monster) else ui("expedition.log_you_block")
            else -> if (hero) ui("expedition.log_you_spell", damage) else ui("expedition.log_they_spell", monster, damage)
        }
        Action.ATTACK -> when (event.kind) {
            HitKind.HIT -> if (hero) ui("expedition.log_you_hit", damage) else ui("expedition.log_they_hit", monster, damage)
            HitKind.CRIT -> if (hero) ui("expedition.log_you_crit", damage) else ui("expedition.log_they_crit", monster, damage)
            // An evasion or a block belongs to the one who was struck at.
            HitKind.EVADED -> if (hero) ui("expedition.log_they_evade", monster) else ui("expedition.log_you_evade")
            HitKind.BLOCKED -> if (hero) ui("expedition.log_they_block", monster) else ui("expedition.log_you_block")
        }
    }
    // The blow's leading element and what it left behind, as words after the sentence.
    val marks = buildList {
        event.type?.takeIf { event.action == Action.ATTACK && event.landed && it != DamageType.PHYSICAL }?.let { add(ui(it.key())) }
        if (event.stunned) add(ui("expedition.stunned"))
        event.inflicted.forEach { add(ui(it.key())) }
    }
    return if (marks.isEmpty()) line else "$line · ${marks.joinToString(" · ")}"
}

private fun logColour(event: CombatEvent): Color = when {
    event.action == Action.FLASK -> Vital
    event.action == Action.RETREAT -> Muted
    event.action == Action.TICK -> damageTint(event.type).copy(alpha = .85f)
    event.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    event.action == Action.SPELL && event.landed -> Rune
    event.kind == HitKind.HIT -> if (event.actor == Side.HERO) Parchment else Color(0xFFE9A0A0)
    else -> Muted
}

private fun hitText(hit: FloatingHit): String = when {
    hit.action == Action.FLASK -> ui("expedition.flask_heal", hit.healed)
    hit.kind == HitKind.EVADED -> ui("expedition.evaded")
    hit.kind == HitKind.BLOCKED -> ui("expedition.blocked")
    hit.kind == HitKind.CRIT -> ui("expedition.crit", hit.amount)
    else -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when {
    hit.action == Action.FLASK -> Vital
    hit.kind == HitKind.EVADED || hit.kind == HitKind.BLOCKED -> Muted
    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    else -> damageTint(hit.type, onHero = hit.target == Side.HERO)
}

private fun outcomeColour(outcome: Outcome) = when (outcome) { Outcome.WIN -> Vital; Outcome.LOSS -> LifeRed; Outcome.RETREAT -> Muted }

/**
 * After the fight: how it ended, what it came to, the whole log to scroll back through and — for a
 * victory — what the server rolled. The same screen closes a defeat, with what the death cost
 * instead of the loot.
 */
@Composable internal fun ReportScreen(s: ForgeState, hud: RunHud, report: FightReport, onContinue: () -> Unit) {
    val won = report.outcome == Outcome.WIN
    Column(Modifier.fillMaxSize().background(Ink.copy(alpha = .9f)).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(ui("expedition.outcome_${report.outcome.name.lowercase()}"), color = outcomeColour(report.outcome), style = MaterialTheme.typography.headlineSmall)
        Text(monsterTitle(report.monster.code), color = rarityTint(report.monster.rarity), style = MaterialTheme.typography.titleMedium)
        val cells = listOf(
            report.dealt.toString() to "expedition.sum_dealt", report.taken.toString() to "expedition.sum_taken",
            ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", report.duration)) to "expedition.sum_time",
            report.crits.toString() to "expedition.sum_crits", report.spells.toString() to "expedition.sum_spells", report.flasks.toString() to "expedition.sum_flasks",
            report.dotDealt.toString() to "expedition.sum_dot_dealt", report.blocked.toString() to "expedition.sum_blocked", report.evaded.toString() to "expedition.sum_evaded")
        cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    Column(Modifier.weight(1f).background(Panel, RoundedCornerShape(8.dp)).border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(ui(label), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        // What was inflicted, each way, as coloured words.
        if (report.inflicted.isNotEmpty()) AilmentLine(ui("expedition.sum_inflicted"), report.inflicted)
        if (report.suffered.isNotEmpty()) AilmentLine(ui("expedition.sum_suffered"), report.suffered)
        Box(Modifier.weight(1f).fillMaxWidth().background(Panel, RoundedCornerShape(10.dp)).border(1.dp, Bronze.copy(alpha = .4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)) {
            // The whole fight, from the first blow down, as it happened.
            FightLog(report.events, report.monster.code, Modifier.fillMaxSize())
        }
        if (won) Loot(s, hud) else Fall(hud)
        Button(enabled = !hud.rewardPending && !hud.fallPending, onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(ui(if (won) "expedition.continue" else "expedition.back_to_camp"))
        }
    }
}

@Composable private fun AilmentLine(label: String, ailments: List<Ailment>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        ailments.forEach { Text(ui(it.key()), color = ailmentTint(it), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
    }
}

/** What the kill brought: the server's roll, or its absence said plainly. */
@Composable private fun Loot(s: ForgeState, hud: RunHud) {
    val reward = hud.reward
    when {
        hud.rewardPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
            Text(ui("expedition.loot_pending"), color = Muted)
        }
        hud.rewardFailed -> Text(ui("expedition.loot_failed"), color = LifeRed, style = MaterialTheme.typography.bodyMedium)
        reward != null -> Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(ui("expedition.loot_experience", number(reward.experience)), color = Rune)
                if (reward.gold > 0) Text(ui("expedition.loot_gold", reward.gold), color = GoldBright)
            }
            reward.items.forEach { stack ->
                val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(18.dp))
                    Text(ui("expedition.loot_stack", orb?.title(s.lang) ?: ui("common.item"), stack.amount), color = Parchment)
                }
            }
            reward.equipment.forEach { instance ->
                ItemRow(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), s.world.definitions) {}
            }
            if (reward.items.isEmpty() && reward.equipment.isEmpty()) Text(ui("expedition.loot_nothing"), color = Muted)
        }
    }
}

/** What the death cost: the server's word, awaited, or its absence said plainly. */
@Composable private fun Fall(hud: RunHud) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("expedition.dead_hint"), color = Parchment, style = MaterialTheme.typography.bodySmall)
        val fall = hud.fall
        when {
            hud.fallPending -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Gold, strokeWidth = 2.dp)
                Text(ui("expedition.fall_pending"), color = Muted)
            }
            fall == null -> Text(ui("expedition.fall_failed"), color = LifeRed, style = MaterialTheme.typography.bodySmall)
            fall.lost > 0 -> Text(ui("expedition.fall_lost", number(fall.lost)), color = LifeRed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            else -> Text(ui("expedition.fall_free"), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
