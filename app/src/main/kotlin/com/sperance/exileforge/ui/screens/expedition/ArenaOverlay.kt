package com.sperance.exileforge.ui.screens.expedition

import com.sperance.exileforge.presentation.state.sellPrice
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.core.model.campaign.CombatRules
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.PI
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignReward
import com.sperance.exileforge.core.model.campaign.MonsterRarity
import com.sperance.exileforge.presentation.features.key
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.GoldBright

internal fun rarityTint(rarity: MonsterRarity) = when (rarity) {
    MonsterRarity.NORMAL -> Parchment
    MonsterRarity.MAGIC -> Color(0xFF8888FF)
    MonsterRarity.RARE -> Color(0xFFFFFF77)
    MonsterRarity.UNIQUE -> Color(0xFFAF6025)
}

/** Each damage type's colour, on a number and in the log alike. */
internal fun damageTint(type: DamageType?, onHero: Boolean = false): Color = when (type) {
    DamageType.FIRE -> Ember
    DamageType.COLD -> ShieldCyan
    DamageType.LIGHTNING -> Color(0xFFFFD34A)
    DamageType.CHAOS -> Elder
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

/** The key a card's bounds are kept under: the hero's, and each foe's by its place in the pack. */
private const val HERO_CARD = -1

/**
 * The fight as cards (2.70.0, the owner's mockup B «карточки против карточек»): the pack across the
 * top in two rows — ranged behind, melee in front — the hero's card at the foot, and between them
 * either the scouting panel or the latest blows. Whoever swings is lifted toward the other side and
 * lit — gold for the hero, blood for a foe — and a line runs from them to whom they struck. A tap on
 * a foe singles it out as the hero's target; the same tap again gives the choice back to the class.
 *
 * Before «В бой», and whenever paused, nothing moves: the tapped foe — or the one the hero would
 * strike — is laid open, its numbers held against the hero's.
 */
@Composable internal fun ArenaOverlay(s: ForgeState, hud: RunHud, fight: FightHud, level: Int, hero: Combatant, rules: CombatRules, stance: HeroStance,
                                      onCommand: (RunCommand) -> Unit) {
    val time by rememberClock()
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val names = remember(fight.foes.size, fight.leader) { fight.foes.associate { it.index to monsterTitle(it.monster.code) } }
    val chosen = fight.focus ?: fight.target ?: fight.foes.firstOrNull { it.alive }?.index
    fun track(key: Int) = Modifier.onGloballyPositioned { bounds[key] = it.boundsInRoot() }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PackHeader(fight, level)
            val (back, front) = fight.foes.partition { it.ranged }
            listOf(back to "fight.row_back", front to "fight.row_front").filter { it.first.isNotEmpty() }.forEach { (row, title) ->
                Caption(ui(title))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    row.forEach { foe ->
                        FoeCard(foe, fight, time, chosen == foe.index && fight.scouting, track(foe.index).weight(1f, fill = false).widthIn(max = 120.dp)) {
                            onCommand(RunCommand.Focus(foe.index))
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val shown = fight.foes.firstOrNull { it.index == chosen }
                if (fight.scouting && shown != null) ScoutPanel(shown, fight, level, hero, rules, stance)
                else FightFeed(fight.events, names)
            }
            HeroCard(s, hud, fight, time, names, stance, track(HERO_CARD))
            Controls(fight, onCommand)
        }
        StrikeLine(fight.lunge, bounds, origin)
        fight.outcome?.let {
            Text(ui("expedition.outcome_${it.name.lowercase()}"), color = outcomeColour(it), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center).background(Ink.copy(alpha = .8f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 6.dp))
        }
    }
}

/** The pack's leader by name and what the pack is: its rarity, the map's level, how many are left standing. */
@Composable private fun PackHeader(fight: FightHud, level: Int) {
    val leader = fight.leader
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(monsterTitle(leader.code), color = rarityTint(leader.rarity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        val line = ui("expedition.monster_line", ui(leader.rarity.key()), level)
        Text(if (fight.foes.size > 1) "$line · " + ui("expedition.pack_left", fight.foes.count { it.alive }, fight.foes.size) else line,
            color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** How far a card is carried toward the other side by its own swing: out and back over the lunge. */
private fun reach(lunge: LungeView?, actor: Side, foe: Int?): Float {
    lunge ?: return 0f
    if (lunge.actor != actor || lunge.action != Action.ATTACK || (foe != null && lunge.foe != foe)) return 0f
    return sin(lunge.progress * PI).toFloat()
}

/** Whether this card is the one being struck right now, past the lunge's midpoint. */
private fun struck(lunge: LungeView?, target: Side, foe: Int?): Boolean {
    lunge ?: return false
    if (lunge.actor == target || lunge.action != Action.ATTACK || lunge.progress < .5f) return false
    return foe == null || lunge.foe == foe
}

/** How white a portrait flashes as a landed blow reaches it. */
private fun flash(lunge: LungeView?, target: Side, foe: Int?): Float =
    if (lunge != null && lunge.landed && struck(lunge, target, foe)) (1 - lunge.progress) * 2f * (if (lunge.kind == HitKind.CRIT) 1f else .6f) else 0f

/**
 * One foe's card: its portrait in its rarity's frame, its name, life and shield, its swing and what
 * is on it. Lit and lowered toward the hero while it swings, ringed in blood while struck, ringed in
 * gold when the player singled it out, marked with a sight when the hero's next blow goes to it.
 * The fallen go dark; one the hero's weapon cannot reach yet is dimmed.
 */
@Composable private fun FoeCard(foe: FoeView, fight: FightHud, time: Float, open: Boolean, modifier: Modifier, onTap: () -> Unit) {
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
    Column(modifier.graphicsLayer {
            translationY = acting * 10.dp.toPx()
            translationX = if (hit && foe.alive) sin(time * 60f) * 2.dp.toPx() else 0f
            alpha = when { !foe.alive -> .35f; !foe.reachable && fight.started -> .7f; else -> 1f }
        }
        .background(if (acting > 0f) Blood.copy(alpha = .35f) else Panel.copy(alpha = .9f), shape)
        .border(if (focused || acting > 0f) 2.dp else 1.dp, border, shape)
        .clip(shape).clickable(enabled = foe.alive && fight.outcome == null, onClick = onTap)
        .padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(.75f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF0B0E13))) {
            Canvas(Modifier.fillMaxSize()) {
                Portraits.monster(this, foe.monster.code, foe.monster.form, ring, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f,
                    flash(lunge, Side.MONSTER, foe.index))
            }
            if (fight.target == foe.index && foe.alive && fight.outcome == null)
                Text(if (focused) "◉" else "◎", color = GoldBright, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(3.dp))
            if (!foe.alive) Text(ui("fight.fallen"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center))
            else if (!foe.reachable) Text(ui("fight.out_of_reach_short"), color = Muted, fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomCenter).background(Ink.copy(alpha = .8f)).padding(horizontal = 4.dp))
            CardHits(fight.hits.filter { it.target == Side.MONSTER && it.foe == foe.index })
        }
        Text(monsterTitle(foe.monster.code), color = ring, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LifeBar(foe.life, foe.maxLife, foe.shield, foe.maxShield, Modifier.fillMaxWidth().height(12.dp), compact = true)
        SwingBar(foe.swing, foe.held, Modifier.fillMaxWidth(), if (acting > 0f) LifeRed else LifeRed.copy(alpha = .7f))
        Row(Modifier.height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (foe.held && foe.ailments.none { it.ailment == Ailment.FROZEN }) Box(Modifier.size(8.dp).background(GoldBright, CircleShape))
            foe.ailments.take(5).forEach { Box(Modifier.size(8.dp).background(ailmentTint(it.ailment).copy(alpha = .4f + .6f * it.left), CircleShape)) }
        }
    }
}

/** The numbers rising off a card: the blows that reached it in the last second. */
@Composable private fun BoxScope.CardHits(hits: List<FloatingHit>) {
    hits.takeLast(3).forEach { hit ->
        val rise = (hit.age / ExpeditionRun.HIT_LIFETIME).toFloat().coerceIn(0f, 1f)
        Text(hitText(hit), color = hitColour(hit).copy(alpha = 1 - rise), textAlign = TextAlign.Center,
            fontSize = when { hit.kind == HitKind.CRIT -> 20.sp; hit.action == Action.TICK -> 12.sp; else -> 16.sp },
            fontWeight = if (hit.action == Action.TICK) FontWeight.Normal else FontWeight.Black,
            fontStyle = if (hit.action == Action.TICK) FontStyle.Italic else FontStyle.Normal,
            modifier = Modifier.align(Alignment.Center).offset(x = (((hit.id % 3) - 1) * 14).dp, y = (-36 * rise).dp))
    }
}

/**
 * The hero's card at the foot: portrait, name and class, life with the shield over it, the swing,
 * every state on them, and whom the next blow goes to — the player's pick or the class's rule.
 * Lifted and lit in gold while they swing; ringed in blood while struck.
 */
@Composable private fun HeroCard(s: ForgeState, hud: RunHud, fight: FightHud, time: Float, names: Map<Int, String>, stance: HeroStance, modifier: Modifier) {
    val character = s.play.hero?.character
    val lunge = fight.lunge
    val acting = reach(lunge, Side.HERO, null)
    val hit = struck(lunge, Side.MONSTER, null)
    val shape = RoundedCornerShape(10.dp)
    val wash = fight.heroAilments.map { it.ailment }.maxByOrNull(::washAmount)
    Row(modifier.fillMaxWidth().graphicsLayer {
            translationY = -acting * 10.dp.toPx()
            translationX = if (hit) sin(time * 60f) * 2.dp.toPx() else 0f
        }
        .background(if (acting > 0f) Color(0xFF2A2416) else Panel.copy(alpha = .94f), shape)
        .border(if (acting > 0f || hit) 2.dp else 1.dp, when { acting > 0f -> GoldBright; hit -> LifeRed; else -> Gold.copy(alpha = .6f) }, shape)
        .padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(64.dp).aspectRatio(.75f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0B0E13))) {
            Canvas(Modifier.fillMaxSize()) {
                Portraits.hero(this, s.heroClass?.code, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash(lunge, Side.HERO, null))
            }
            CardHits(fight.hits.filter { it.target == Side.HERO })
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(listOfNotNull(character?.name, ui("expedition.hero_line", s.heroClass?.title.orEmpty(), character?.level ?: 1)).joinToString(" · "),
                color = GoldBright, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LifeBar(fight.heroLife, hud.heroMaxLife, fight.heroShield, hud.heroMaxShield, Modifier.fillMaxWidth().height(16.dp))
            SwingBar(fight.heroSwing, fight.heroHeld, Modifier.fillMaxWidth())
            StateTiles(fight.heroAilments, fight.heroHeld)
            val target = fight.target?.let(names::get)
            if (target != null && fight.outcome == null) Text(
                ui("fight.target_line", target, if (fight.focus != null) ui("fight.target_yours") else ui("fight.rule.${stance.rule.name}")),
                color = if (fight.focus != null) GoldBright else Parchment, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The scouting panel (2.70.0): everything about one foe while nothing moves — what it is and where
 * it stands, its pools and defences, how hard and how often it strikes, its resistances, what it
 * rolled — and what that means for this hero: whether the weapon reaches it, and how its
 * resistance meets the hero's leading damage.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ScoutPanel(foe: FoeView, fight: FightHud, level: Int, hero: Combatant, rules: CombatRules, stance: HeroStance) {
    val body = remember(foe.monster) { Combatant(foe.monster.stats, level, rules) }
    val shape = RoundedCornerShape(10.dp)
    val ring = rarityTint(foe.monster.rarity)
    Column(Modifier.fillMaxSize().background(Panel.copy(alpha = .95f), shape).border(1.dp, ring.copy(alpha = .8f), shape)
        .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(monsterTitle(foe.monster.code), color = ring, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(listOf(ui(foe.monster.rarity.key()), ui("fight.level", level), ui(if (foe.ranged) "fight.ranged" else "fight.melee")).joinToString(" · "),
            color = Muted, style = MaterialTheme.typography.labelSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Fact(ui("fight.stat_life"), number(body.maxLife), LifeRed)
            if (body.maxShield > 0) Fact(ui("fight.stat_shield"), number(body.maxShield), ShieldCyan)
            if (body.armour > 0) Fact(ui("fight.stat_armour"), number(body.armour), Parchment)
            if (body.evasion > 0) Fact(ui("fight.stat_evasion"), number(body.evasion), Parchment)
            if (body.block > 0) Fact(ui("fight.stat_block"), "${(body.block * 100).roundToInt()}%", Parchment)
            Fact(ui("fight.stat_speed"), String.format(Locale.ROOT, "%.2f", body.attackSpeed), Parchment)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("fight.stat_damage"), color = Muted, style = MaterialTheme.typography.labelSmall)
            body.damage.filterValues { it > 0 }.forEach { (type, amount) ->
                Text("${ui(type.key())} ${number(amount)}", color = damageTint(type), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DamageType.entries.filter { it.resist != null }.forEach { type ->
                val resist = (body.resist(type) * 100).roundToInt()
                Text("${ui(type.key())} $resist%", color = damageTint(type), fontSize = 10.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).background(Ink.copy(alpha = .6f), RoundedCornerShape(4.dp)).padding(vertical = 2.dp))
            }
        }
        // What it means for this hero.
        val leading = hero.leading
        val against = (body.resist(leading) * 100).roundToInt()
        if (!foe.reachable) Hint(ui("fight.out_of_reach"), LifeRed)
        if (leading != DamageType.PHYSICAL) Hint(ui(if (against <= 0) "fight.resist_good" else "fight.resist_bad", ui(leading.key()), against),
            if (against <= 25) Vital else LifeRed)
        else if (body.armour > 0) Hint(ui("fight.armour_note", number(body.armour)), Muted)
        Hint(if (fight.focus == foe.index) ui("fight.focus_on") else ui("fight.focus_off", ui("fight.rule.${stance.rule.name}")), GoldBright)
        monsterLines(foe.monster).takeIf { it.isNotEmpty() }?.let { lines ->
            Caption(ui("fight.modifiers", lines.size))
            lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Rhombus(if (line.fromMap) LifeRed else Rune, 4.dp)
                    Text(monsterLineText(line), color = Rune, style = MaterialTheme.typography.labelSmall)
                    if (line.fromMap) Text(ui("fight.line_map"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable private fun Fact(label: String, value: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Hint(text: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Rhombus(tint, 4.dp)
        Text(text, color = tint, style = MaterialTheme.typography.labelSmall)
    }
}

/** The latest blows while the fight runs, newest on top, each under the name of the foe it was about. */
@Composable private fun FightFeed(events: List<CombatEvent>, names: Map<Int, String>) {
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.fillMaxSize().background(Panel.copy(alpha = .75f), shape).border(1.dp, Bronze.copy(alpha = .4f), shape).padding(horizontal = 10.dp, vertical = 6.dp)) {
        if (events.isEmpty()) MutedText(ui("expedition.log"), style = MaterialTheme.typography.labelSmall)
        FightLog(events, names)
    }
}

/**
 * Under the hero: before «В бой» the call to fight and the way back; once it runs, pause and go on,
 * the speed, and the retreat.
 */
@Composable private fun Controls(fight: FightHud, onCommand: (RunCommand) -> Unit) {
    val live = fight.outcome == null
    if (!fight.started) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onCommand(RunCommand.Begin) }, modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright)) {
                Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui("expedition.begin"), style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = { onCommand(RunCommand.Retreat) }, modifier = Modifier.height(52.dp)) { Text(ui("fight.walk_away")) }
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = live && !fight.retreating, onClick = { onCommand(if (fight.paused) RunCommand.Begin else RunCommand.Pause) },
            modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(ui(if (fight.paused) "fight.resume" else "fight.pause"), style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(onClick = { onCommand(RunCommand.Speed) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(ui("expedition.speed", fight.speed), style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(enabled = live && !fight.retreating, onClick = { onCommand(RunCommand.Retreat) }, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(ui(if (fight.retreating) "expedition.retreating" else "expedition.retreat"), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The line from whoever swings to whom they strike, fading in and out over the lunge: gold from the hero, blood from a foe. */
@Composable private fun StrikeLine(lunge: LungeView?, bounds: Map<Int, Rect>, origin: Offset) {
    if (lunge == null || lunge.action != Action.ATTACK) return
    val from = bounds[if (lunge.actor == Side.HERO) HERO_CARD else lunge.foe] ?: return
    val to = bounds[if (lunge.actor == Side.HERO) lunge.foe else HERO_CARD] ?: return
    val tint = if (lunge.actor == Side.HERO) GoldBright else LifeRed
    val strength = sin(lunge.progress * PI).toFloat()
    Canvas(Modifier.fillMaxSize()) {
        val a = from.center - origin
        val b = to.center - origin
        drawLine(tint.copy(alpha = .25f * strength), a, b, 8.dp.toPx(), StrokeCap.Round)
        drawLine(tint.copy(alpha = .9f * strength), a, a + (b - a) * lunge.progress, 2.5.dp.toPx(), StrokeCap.Round)
    }
}

/** A life bar with the shield laid over its top edge and the figures written across it. */
@Composable private fun LifeBar(life: Int, maxLife: Int, shield: Int, maxShield: Int, modifier: Modifier, compact: Boolean = false) {
    val shape = CutCornerShape(3.dp)
    val share by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    Box(modifier.background(Color(0xCC0A0D12), shape).border(1.dp, Gold.copy(alpha = .7f), shape)) {
        Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
        if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).background(ShieldCyan.copy(alpha = .85f)))
        Text(if (compact) "$life" else if (maxShield > 0) ui("expedition.vitals_shield", life, maxLife, shield) else ui("expedition.vitals", life, maxLife),
            color = GoldBright, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

/** The swing: full the instant the next blow lands; dimmed while nothing can land. */
@Composable private fun SwingBar(swing: Float, held: Boolean, modifier: Modifier, tint: Color = Gold) {
    Box(modifier.height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
        Box(Modifier.fillMaxWidth(swing.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else tint, RoundedCornerShape(2.dp)))
    }
}

/** Every state on the hero as a tile of its colour whose dark fill rises as it wears off; a stun is a gold star. */
@Composable private fun StateTiles(ailments: List<AilmentView>, held: Boolean) {
    val stunned = held && ailments.none { it.ailment == Ailment.FROZEN }
    Row(Modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!stunned && ailments.isEmpty()) MutedText(ui("fight.no_states"), style = MaterialTheme.typography.labelSmall)
        if (stunned) StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned"))
        ailments.forEach { StateTile(it.ailment, ailmentTint(it.ailment), it.left, it.stacks, ailmentLabel(it)) }
    }
}

@Composable private fun StateTile(ailment: Ailment?, tint: Color, left: Float, stacks: Int, label: String) {
    val shape = RoundedCornerShape(4.dp)
    Box(Modifier.size(30.dp).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
        .semantics { contentDescription = label }) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) { stateGlyph(ailment, tint) }
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
        if (stacks > 1) Text("$stacks", color = Parchment, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp))
    }
}

/** An ailment's mark drawn from shapes: a flame, a snowflake, a crystal, a bolt, a drop, two drops; a star for a stun. */
private fun DrawScope.stateGlyph(ailment: Ailment?, tint: Color) {
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
        Ailment.POISONED -> { drawPath(path(8f to 2f, 12f to 9f, 11f to 13f, 8f to 14f, 5f to 13f, 4f to 9f), tint); drawCircle(Color(0xFF0B0E13), 1.4f * u, Offset(8 * u, 10 * u)) }
        Ailment.BLEEDING -> { drawPath(path(6f to 2f, 9f to 8f, 8f to 11f, 6f to 12f, 4f to 11f, 3f to 8f), tint); drawPath(path(12f to 7f, 14f to 11f, 13f to 13f, 12f to 14f, 11f to 13f, 10f to 11f), tint) }
        null -> drawPath(path(8f to 1f, 9.8f to 5.2f, 14.3f to 5.6f, 10.9f to 8.6f, 11.9f to 13f, 8f to 10.7f, 4.1f to 13f, 5.1f to 8.6f, 1.7f to 5.6f, 6.2f to 5.2f), tint)
    }
}

private fun ailmentLabel(view: AilmentView) =
    if (view.stacks > 1) ui("expedition.ailment_stacks", ui(view.ailment.key()), view.stacks) else ui(view.ailment.key())

/** How strongly an ailment washes a portrait: a freeze is the whole face, a bleed a tint. */
internal fun washAmount(ailment: Ailment): Float = when (ailment) {
    Ailment.FROZEN -> .55f
    Ailment.BURNING -> .35f
    Ailment.CHILLED, Ailment.POISONED, Ailment.SHOCKED -> .3f
    Ailment.BLEEDING -> .25f
}

/** A clock in seconds for the portraits' idle motion, ticking once a frame while the frame is on screen. */
@Composable internal fun rememberClock(): State<Float> {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now -> if (last != 0L) clock.floatValue += ((now - last) / 1e9f).coerceAtMost(.05f); last = now }
    }
    return clock
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
@Composable internal fun FightLog(events: List<CombatEvent>, names: Map<Int, String>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        items(events) { EventRow(it, names[it.foe].orEmpty()) }
    }
}

/**
 * The log of a whole pack (since 2.54.0): one list, each foe's blows under its own name — a mixed
 * pack's «they hit» lines would otherwise all say the wrong name. A caption between them names which
 * of the pack it was, only when there was more than one.
 */
@Composable internal fun FightLog(pack: List<PackHit>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        pack.forEachIndexed { index, hit ->
            val name = monsterTitle(hit.monster.code)
            if (pack.size > 1) item { Caption(ui("expedition.report_pack_enemy", index + 1, pack.size, name)) }
            items(hit.events) { EventRow(it, name) }
        }
    }
}

@Composable private fun EventRow(event: CombatEvent, monster: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(ui("expedition.log_time", String.format(Locale.ROOT, "%.1f", event.time)), color = Muted,
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
        Text(logLine(event, monster), color = logColour(event), style = MaterialTheme.typography.bodySmall,
            fontWeight = if (event.kind == HitKind.CRIT) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (event.action == Action.TICK) FontStyle.Italic else FontStyle.Normal)
    }
}

private fun logLine(event: CombatEvent, monster: String): String {
    val damage = event.damage.roundToInt()
    val hero = event.actor == Side.HERO
    val line = when (event.action) {
        Action.RETREAT -> ui("expedition.log_retreat")
        Action.TICK -> {
            val ailment = event.ailment?.let { ui(it.key()) }.orEmpty()
            if (hero) ui("expedition.log_tick_they", monster, damage, ailment) else ui("expedition.log_tick_you", damage, ailment)
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
    event.action == Action.RETREAT -> Muted
    event.action == Action.TICK -> damageTint(event.type).copy(alpha = .85f)
    event.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    event.kind == HitKind.HIT -> if (event.actor == Side.HERO) Parchment else Color(0xFFE9A0A0)
    else -> Muted
}

private fun hitText(hit: FloatingHit): String = when {
    hit.kind == HitKind.EVADED -> ui("expedition.evaded")
    hit.kind == HitKind.BLOCKED -> ui("expedition.blocked")
    hit.kind == HitKind.CRIT -> ui("expedition.crit", hit.amount)
    else -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when {
    hit.kind == HitKind.EVADED || hit.kind == HitKind.BLOCKED -> Muted
    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    else -> damageTint(hit.type, onHero = hit.target == Side.HERO)
}

internal fun outcomeColour(outcome: Outcome) = when (outcome) { Outcome.WIN -> Vital; Outcome.LOSS -> LifeRed; Outcome.RETREAT -> Muted }

/** What the server rolled — experience, gold, orbs and items — for a kill and a chest alike. */
@Composable internal fun RewardLines(s: ForgeState, reward: CampaignReward) {
    Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (reward.experience > 0) Text(ui("expedition.loot_experience", number(reward.experience)), color = Rune)
            if (reward.gold > 0) Text(ui("expedition.loot_gold", reward.gold), color = GoldBright)
        }
        reward.items.forEach { stack ->
            val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (orb != null) com.sperance.exileforge.ui.icons.OrbGlyph(orb.orb, Modifier.size(20.dp))
                else Icon(ForgeGlyphs.Orb, null, tint = Gold, modifier = Modifier.size(18.dp))
                Text(ui("expedition.loot_stack", orb?.title(s.lang) ?: ui("common.item"), stack.amount), color = Parchment)
            }
        }
        reward.equipment.forEach { instance ->
            ItemRow(inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId]), s.world.definitions, price = s.sellPrice(instance)) {}
        }
        if (reward.items.isEmpty() && reward.equipment.isEmpty()) Text(ui("expedition.loot_nothing"), color = Muted)
    }
}
