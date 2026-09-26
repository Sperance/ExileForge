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
import com.sperance.exileforge.core.model.campaign.LoneWolfRule
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawOutline
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
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.ui.screens.skills.FlaskBottle
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

/**
 * What each effect's window says (2.72.0, a window growing out of the icon since 2.73.0): its name,
 * the rule in words, and its figures — strength, seconds left, stacks.
 */
private fun ailmentTip(view: AilmentView) = Tip(ui(view.ailment.key()), ui("fight.effect.${view.ailment.name}"), ailmentTint(view.ailment), listOfNotNull(
    view.strength.takeIf { it > 0 && view.ailment != Ailment.FROZEN }?.let {
        ui("fight.fact_strength") to (if (view.ailment.hurts) ui("fight.fact_dps", fineNumber(it)) else ui("fight.fact_percent", fineNumber(it)))
    },
    (ui("fight.fact_left") to ui("fight.fact_seconds", fineNumber(view.seconds))).takeIf { view.seconds > 0 },
    (ui("fight.fact_stacks") to view.stacks.toString()).takeIf { view.stacks > 1 },
))

private fun stunTip() = Tip(ui("expedition.stunned"), ui("fight.effect.STUN"), GoldBright)

private fun tauntTip(hero: Boolean) = Tip(ui("fight.taunt"), ui(if (hero) "fight.taunt_hero" else "fight.taunt_hint"), TauntCrimson)

private fun loneWolfTip(rule: LoneWolfRule) = Tip(ui("fight.lone_wolf_title"), ui("fight.lone_wolf_body", number(rule.dealt), number(rule.taken)), GoldBright,
    listOf(ui("fight.fact_dealt") to "+${number(rule.dealt)}%", ui("fight.fact_taken") to "−${number(rule.taken)}%"))

/** The taunt's colours: dried blood and old gold. */
private val TauntCrimson = Color(0xFFB8373A)
private val TauntGold = Color(0xFFE8B06A)

/**
 * A taunter's aura (2.72.0): a soft crimson glow breathing round its card, under the border, so a
 * taunter is found at a glance without a word on it.
 */
private fun Modifier.tauntAura(shape: Shape, time: Float) = drawBehind {
    val breath = .35f + .25f * sin(time * 2.4f)
    val outline = shape.createOutline(size, layoutDirection, this)
    for (step in 3 downTo 1) drawOutline(outline, TauntCrimson.copy(alpha = breath / step), style = Stroke((step * 2.5f).dp.toPx()))
    drawOutline(outline, Brush.verticalGradient(listOf(TauntGold, TauntCrimson)), style = Stroke(1.5.dp.toPx()))
}

/**
 * The taunt's seal (2.72.0): a heraldic shield in crimson with a gold rim and a gold chevron,
 * glowing faintly — the mark a taunter carries on its portrait; a tap opens its window.
 */
@Composable private fun TauntSeal(time: Float, modifier: Modifier, tip: (() -> Tip)?) = Tipped(tip, modifier) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = ui("fight.taunt") }) {
        val w = size.width
        val h = size.height
        drawCircle(TauntCrimson.copy(alpha = .25f + .15f * sin(time * 2.4f)), w * .62f, center)
        val shield = Path().apply {
            moveTo(w * .5f, h * .06f); lineTo(w * .9f, h * .2f); lineTo(w * .86f, h * .56f)
            quadraticTo(w * .78f, h * .84f, w * .5f, h * .96f); quadraticTo(w * .22f, h * .84f, w * .14f, h * .56f); lineTo(w * .1f, h * .2f); close()
        }
        drawPath(shield, Brush.verticalGradient(listOf(Color(0xFF7A1C22), TauntCrimson, Color(0xFF4A0E12))))
        drawPath(shield, TauntGold, style = Stroke(1.4.dp.toPx()))
        val chevron = Path().apply { moveTo(w * .3f, h * .42f); lineTo(w * .5f, h * .6f); lineTo(w * .7f, h * .42f) }
        drawPath(chevron, TauntGold, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * The lone wolf's medallion (2.72.0): a wolf's head in gold on a dark coin with a bronze rim; a tap
 * opens what the bonus gives.
 */
@Composable private fun LoneWolfMedal(modifier: Modifier, tip: () -> Tip) = Tipped(tip, modifier.clip(CircleShape)) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = ui("fight.lone_wolf_title") }) {
        val w = size.width
        val h = size.height
        drawCircle(Brush.radialGradient(listOf(PanelRaised, Color(0xFF0B0D11)), center, w / 2), w / 2, center)
        drawCircle(Bronze, w / 2 - 1.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
        val head = Path().apply {
            moveTo(w * .24f, h * .22f); lineTo(w * .38f, h * .38f); lineTo(w * .62f, h * .38f); lineTo(w * .76f, h * .22f)
            lineTo(w * .74f, h * .52f); lineTo(w * .58f, h * .8f); lineTo(w * .5f, h * .84f); lineTo(w * .42f, h * .8f); lineTo(w * .26f, h * .52f); close()
        }
        drawPath(head, Brush.verticalGradient(listOf(GoldBright, Gold)))
        drawCircle(Color(0xFF0B0D11), w * .045f, Offset(w * .41f, h * .5f))
        drawCircle(Color(0xFF0B0D11), w * .045f, Offset(w * .59f, h * .5f))
    }
}

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
@Composable internal fun ArenaOverlay(s: ForgeState, hud: RunHud, fight: FightHud, level: Int, rules: CombatRules, stance: HeroStance,
                                      onCommand: (RunCommand) -> Unit) {
    val time by rememberClock()
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val names = remember(fight.foes.size, fight.leader) { fight.foes.associate { it.index to monsterTitle(it.monster.code) } }
    val chosen = fight.focus ?: fight.target ?: fight.foes.firstOrNull { it.alive }?.index
    // The tiles are larger while the fight stands still; a tap on any of them opens its window at any time (2.73.0).
    val large = fight.scouting
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
                        FoeCard(foe, fight, time, chosen == foe.index && fight.scouting, track(foe.index).weight(1f, fill = false).widthIn(max = 120.dp), large) {
                            onCommand(RunCommand.Focus(foe.index))
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val shown = fight.foes.firstOrNull { it.index == chosen }
                if (fight.scouting && shown != null) ScoutPanel(shown, fight, level, rules, stance)
                else FightFeed(fight.events, names)
            }
            HeroCard(s, hud, fight, time, names, stance, track(HERO_CARD), large)
            // The skills and the belt (2.78.0): under the hero, over the fight's own controls.
            if (fight.skills.any { it != null } || fight.flasks.any { it != null }) ActionBar(fight, onCommand)
            Controls(fight, onCommand)
        }
        StrikeLine(fight.lunge, bounds, origin)
        // A win says so in the rewards window itself (2.73.0); only a loss or a retreat is announced here.
        fight.outcome?.takeIf { it != Outcome.WIN }?.let {
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

/** A swing and a skill's blow carry a card toward the other side (2.78.0); a tick, a reflection, a draught do not. */
private val Action.strikes: Boolean get() = this == Action.ATTACK || this == Action.SKILL

/** How far a card is carried toward the other side by its own swing: out and back over the lunge. */
private fun reach(lunge: LungeView?, actor: Side, foe: Int?): Float {
    lunge ?: return 0f
    if (lunge.actor != actor || !lunge.action.strikes || (foe != null && lunge.foe != foe)) return 0f
    return sin(lunge.progress * PI).toFloat()
}

/** Whether this card is the one being struck right now, past the lunge's midpoint. */
private fun struck(lunge: LungeView?, target: Side, foe: Int?): Boolean {
    lunge ?: return false
    if (lunge.actor == target || !lunge.action.strikes || lunge.progress < .5f) return false
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
@Composable private fun FoeCard(foe: FoeView, fight: FightHud, time: Float, open: Boolean, modifier: Modifier,
                                 large: Boolean, onTap: () -> Unit) {
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
        .then(if (foe.taunt && foe.alive && acting == 0f && !hit && !focused && !open) Modifier.tauntAura(shape, time) else Modifier)
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
            if (foe.taunt && foe.alive) TauntSeal(time, Modifier.align(Alignment.TopStart).padding(3.dp).size(22.dp)) { tauntTip(false) }
            if (!foe.alive) Text(ui("fight.fallen"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center))
            // A card behind a taunter says nothing (2.73.0): the taunter's seal already tells why.
            else if (!foe.reachable && fight.foes.none { it.alive && it.taunt }) Text(ui("fight.out_of_reach_short"),
                color = Muted, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter).background(Ink.copy(alpha = .8f)).padding(horizontal = 4.dp))
            CardHits(fight.hits.filter { it.target == Side.MONSTER && it.foe == foe.index })
        }
        Text(monsterTitle(foe.monster.code), color = ring, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LifeBar(foe.life, foe.maxLife, foe.shield, foe.maxShield, Modifier.fillMaxWidth().height(12.dp), compact = true)
        // A caster's or a boss's mana (2.78.0), a thread under its life: what its spells are paid with.
        if (foe.maxMana > 0) Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth((foe.mana / foe.maxMana.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(ManaBlue, RoundedCornerShape(2.dp)))
        }
        SwingBar(foe.swing, foe.held, Modifier.fillMaxWidth(), if (acting > 0f) LifeRed else LifeRed.copy(alpha = .7f))
        if (foe.taunt && foe.alive) Text(ui("fight.taunt"), color = Color(0xFFE8B06A), fontSize = 9.sp, fontStyle = FontStyle.Italic, maxLines = 1)
        // What is on it: small tiles while it runs, larger while paused; a tap on one opens its window.
        Row(Modifier.height(if (large) 18.dp else 12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val tile = if (large) 18.dp else 10.dp
            if (foe.held && foe.ailments.none { it.ailment == Ailment.FROZEN })
                StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned"), tile) { stunTip() }
            foe.ailments.take(5).forEach { view ->
                StateTile(view.ailment, ailmentTint(view.ailment), view.left, view.stacks, ailmentLabel(view), tile) { ailmentTip(view) }
            }
            foe.effects.take(3).forEach { EffectTile(it, tile) }
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
@Composable private fun HeroCard(s: ForgeState, hud: RunHud, fight: FightHud, time: Float, names: Map<Int, String>, stance: HeroStance, modifier: Modifier,
                                  large: Boolean) {
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
        .background(if (acting > 0f) PanelRaised else Panel.copy(alpha = .94f), shape)
        .then(if (fight.heroTaunt && acting == 0f && !hit) Modifier.tauntAura(shape, time) else Modifier)
        .border(if (acting > 0f || hit) 2.dp else 1.dp, when { acting > 0f -> GoldBright; hit -> LifeRed; else -> Gold.copy(alpha = .6f) }, shape)
        .padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(64.dp).aspectRatio(.75f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0B0E13))) {
            Canvas(Modifier.fillMaxSize()) {
                Portraits.hero(this, s.heroClass?.code, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash(lunge, Side.HERO, null))
            }
            CardHits(fight.hits.filter { it.target == Side.HERO })
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(listOfNotNull(character?.name, ui("expedition.hero_line", s.heroClass?.title.orEmpty(), character?.level ?: 1)).joinToString(" · "),
                    color = GoldBright, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                // The hero's own marks (2.72.0): the taunt's seal and the lone wolf's medallion, each opening its window on a tap.
                if (fight.heroTaunt) TauntSeal(time, Modifier.size(22.dp)) { tauntTip(true) }
                fight.loneWolf?.let { rule -> LoneWolfMedal(Modifier.size(24.dp)) { loneWolfTip(rule) } }
            }
            LifeBar(fight.heroLife, hud.heroMaxLife, fight.heroShield, hud.heroMaxShield, Modifier.fillMaxWidth().height(16.dp), barrier = fight.heroBarrier)
            if (fight.heroMaxMana > 0) ManaBar(fight.heroMana, fight.heroMaxMana, Modifier.fillMaxWidth().height(10.dp))
            SwingBar(fight.heroSwing, fight.heroHeld, Modifier.fillMaxWidth())
            StateTiles(fight.heroAilments, fight.heroHeld, fight.heroEffects)
            val target = fight.target?.let(names::get)
            if (target != null && fight.outcome == null) Text(
                ui("fight.target_line", target, if (fight.focus != null) ui("fight.target_yours") else ui("fight.rule.${stance.rule.name}")),
                color = if (fight.focus != null) GoldBright else Parchment, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * The scouting panel (2.70.0): one foe while nothing moves — what it is and where it stands, its
 * pools, block, how hard and how often it strikes, what it rolled — and whether the hero's weapon
 * reaches it. Its armour, evasion and resistances are not shown since 2.75.0: the fight is read by
 * what happens in it, not by the foe's defence sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun ScoutPanel(foe: FoeView, fight: FightHud, level: Int, rules: CombatRules, stance: HeroStance) {
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
            if (body.block > 0) Fact(ui("fight.stat_block"), "${(body.block * 100).roundToInt()}%", Parchment)
            Fact(ui("fight.stat_speed"), String.format(Locale.ROOT, "%.2f", body.attackSpeed), Parchment)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(ui("fight.stat_damage"), color = Muted, style = MaterialTheme.typography.labelSmall)
            body.damage.filterValues { it > 0 }.forEach { (type, amount) ->
                Text("${ui(type.key())} ${number(amount)}", color = damageTint(type), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        // What it casts for its mana (2.78.0): a boss's own skills, a caster's spell, a borrowed one.
        if (foe.monster.skills.isNotEmpty()) Text(ui("fight.skills", foe.monster.skills.joinToString(", ") { SkillText.title(it) }),
            color = Rune, style = MaterialTheme.typography.labelSmall)
        // What it means for this hero.
        val taunting = fight.foes.any { it.alive && it.taunt }
        when {
            foe.taunt -> Hint(ui("fight.taunt_hint"), LifeRed)
            taunting -> Hint(ui("fight.behind_taunt"), LifeRed)
            !foe.reachable -> Hint(ui("fight.out_of_reach"), LifeRed)
        }
        Hint(if (fight.focus == foe.index) ui("fight.focus_on") else ui("fight.focus_off", ui("fight.rule.${stance.rule.name}")), GoldBright)
        monsterLines(foe.monster).takeIf { it.isNotEmpty() }?.let { lines ->
            Caption(ui("fight.modifiers", lines.size))
            lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Rhombus(if (line.fromMap) LifeRed else ModBlue, 4.dp)
                    Text(monsterLineText(line), color = ModBlue, style = MaterialTheme.typography.labelSmall)
                    // The sum first, then what each source put in it (2.73.0); a line the map alone gives is tagged.
                    val sources = monsterLineSources(line)
                    if (sources != null) Text("($sources)", color = Muted, style = MaterialTheme.typography.labelSmall)
                    else if (line.fromMap) Text(ui("fight.line_map"), color = LifeRed, style = MaterialTheme.typography.labelSmall)
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
            ForgeButton(onClick = { onCommand(RunCommand.Begin) }, modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright)) {
                Icon(ForgeGlyphs.Swords, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui("expedition.begin"), style = MaterialTheme.typography.titleMedium)
            }
            // The Abyss lets nobody walk away from its wave (2.82.0).
            if (fight.escape) ForgeOutlinedButton(onClick = { onCommand(RunCommand.Retreat) }, modifier = Modifier.height(52.dp)) { Text(ui("fight.walk_away")) }
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ForgeOutlinedButton(enabled = live && !fight.retreating, onClick = { onCommand(if (fight.paused) RunCommand.Begin else RunCommand.Pause) },
            modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(ui(if (fight.paused) "fight.resume" else "fight.pause"), style = MaterialTheme.typography.labelMedium)
        }
        ForgeOutlinedButton(onClick = { onCommand(RunCommand.Speed) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(ui("expedition.speed", fight.speed), style = MaterialTheme.typography.labelMedium)
        }
        if (fight.escape) ForgeOutlinedButton(enabled = live && !fight.retreating, onClick = { onCommand(RunCommand.Retreat) }, modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(ui(if (fight.retreating) "expedition.retreating" else "expedition.retreat"), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The line from whoever swings to whom they strike, fading in and out over the lunge: gold from the hero, blood from a foe. */
@Composable private fun StrikeLine(lunge: LungeView?, bounds: Map<Int, Rect>, origin: Offset) {
    if (lunge == null || !lunge.action.strikes) return
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
@Composable private fun LifeBar(life: Int, maxLife: Int, shield: Int, maxShield: Int, modifier: Modifier, compact: Boolean = false, barrier: Int = 0) {
    val shape = RoundedCornerShape(3.dp)
    val share by animateFloatAsState(if (maxLife > 0) life / maxLife.toFloat() else 0f, label = "life")
    // A barrier (2.78.0) rings the bar in gold-white while it soaks.
    Box(modifier.background(Color(0xCC0A0D12), shape).border(if (barrier > 0) 2.dp else 1.dp, if (barrier > 0) GoldBright else Gold.copy(alpha = .7f), shape)) {
        Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(LifeRed, LifeRed.copy(alpha = .55f))), shape))
        if (maxShield > 0) Box(Modifier.fillMaxWidth((shield / maxShield.toFloat()).coerceIn(0f, 1f)).height(4.dp).background(ShieldCyan.copy(alpha = .85f)))
        Text(if (compact) "$life" else if (maxShield > 0) ui("expedition.vitals_shield", life, maxLife, shield) else ui("expedition.vitals", life, maxLife),
            color = GoldBright, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

/** Mana (2.78.0) under life: what the skills are paid with, and what the auras leave of it. */
@Composable private fun ManaBar(mana: Int, maxMana: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(2.dp)
    val share by animateFloatAsState(if (maxMana > 0) mana / maxMana.toFloat() else 0f, label = "mana")
    Box(modifier.background(Color(0xCC0A0D12), shape).border(1.dp, ManaBlue.copy(alpha = .8f), shape)) {
        Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(ManaBlue, ManaBlue.copy(alpha = .5f))), shape))
        Text(ui("fight.mana", mana, maxMana), color = GoldBright, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}

/**
 * The hero's skills and belt in the fight (2.78.0): the three active slots — dark while they recover,
 * dim while the mana is short — then the three flasks, filled to their charges and ringed while one runs.
 * A tap uses a skill or drinks a flask at once, whatever its condition.
 */
@Composable private fun ActionBar(fight: FightHud, onCommand: (RunCommand) -> Unit) {
    val live = fight.started && fight.outcome == null && !fight.retreating
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        fight.skills.forEach { view ->
            if (view == null) Box(Modifier.weight(1f).height(52.dp).border(1.dp, Bronze.copy(alpha = .35f), RoundedCornerShape(8.dp)))
            else SkillButton(view, live, Modifier.weight(1f)) { onCommand(RunCommand.Cast(view.slot)) }
        }
        Spacer(Modifier.width(4.dp))
        fight.flasks.forEach { view ->
            if (view == null) Box(Modifier.size(44.dp).border(1.dp, Bronze.copy(alpha = .35f), CircleShape))
            else FlaskButton(view, live) { onCommand(RunCommand.Drink(view.slot)) }
        }
    }
}

@Composable private fun SkillButton(view: SkillView, live: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val ready = view.ready >= 1f
    // A skill that can go now glows (2.80.0, «Эфир»): the light is the readiness.
    Box(modifier.height(52.dp).glow(Gold, on = ready && view.affordable, radius = 10.dp, shape = shape).clip(shape).background(PanelRaised, shape)
        .border(if (ready && view.affordable) 1.5.dp else 1.dp, if (ready && view.affordable) Gold else Bronze, shape)
        .clickable(enabled = live && ready && view.affordable, onClick = onTap)
        .semantics { contentDescription = SkillText.title(view.code) }) {
        SkillGlyph(view.icon, Modifier.size(26.dp).align(Alignment.Center), if (view.affordable) GoldBright else Muted)
        // What is left to recover darkens the button from the top, as a flask's charge fills it from the bottom.
        if (!ready) Box(Modifier.fillMaxWidth().fillMaxHeight(1 - view.ready).background(Color.Black.copy(alpha = .6f)))
        if (!ready) Text(fineNumber(view.seconds), color = Parchment, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        Text("${view.cost}", color = if (view.affordable) Rune else LifeRed, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 1.dp))
        if (view.condition == com.sperance.exileforge.core.model.skills.SlotCondition.MANUAL)
            Text("✋", fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).padding(2.dp))
        Text("${view.level}", color = Gold, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 3.dp))
    }
}

@Composable private fun FlaskButton(view: FlaskView, live: Boolean, onTap: () -> Unit) {
    val tint = flaskTint(view.kind)
    Box(Modifier.size(44.dp).glow(tint, on = view.active > 0f, radius = 10.dp, shape = CircleShape).clip(CircleShape).background(Color(0xE60A0D12))
        .border(if (view.active > 0f) 2.dp else 1.dp, if (view.active > 0f) GoldBright else Bronze, CircleShape)
        .clickable(enabled = live && view.usable, onClick = onTap), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val fill = if (view.maxCharges > 0) view.charges / view.maxCharges.toFloat() else 0f
            drawRect(tint.copy(alpha = if (view.usable) .55f else .25f), topLeft = Offset(0f, size.height * (1 - fill)), size = Size(size.width, size.height * fill))
            if (view.active > 0f) drawArc(GoldBright, -90f, 360f * view.active, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        FlaskBottle(view.kind, 0f, true, Modifier.size(14.dp, 22.dp))
        Text("${view.charges}", color = GoldBright, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
    }
}

/** The swing: full the instant the next blow lands; dimmed while nothing can land. */
@Composable private fun SwingBar(swing: Float, held: Boolean, modifier: Modifier, tint: Color = Gold) {
    Box(modifier.height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
        Box(Modifier.fillMaxWidth(swing.coerceIn(0f, 1f)).fillMaxHeight().background(if (held) Muted else tint, RoundedCornerShape(2.dp)))
    }
}

/** Every state on the hero as a tile of its colour whose dark fill rises as it wears off; a stun is a gold star. */
@Composable private fun StateTiles(ailments: List<AilmentView>, held: Boolean, effects: List<EffectView> = emptyList()) {
    val stunned = held && ailments.none { it.ailment == Ailment.FROZEN }
    Row(Modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!stunned && ailments.isEmpty() && effects.isEmpty()) MutedText(ui("fight.no_states"), style = MaterialTheme.typography.labelSmall)
        if (stunned) StateTile(null, GoldBright, 1f, 1, ui("expedition.stunned")) { stunTip() }
        ailments.forEach { view -> StateTile(view.ailment, ailmentTint(view.ailment), view.left, view.stacks, ailmentLabel(view)) { ailmentTip(view) } }
        effects.take(4).forEach { EffectTile(it) }
    }
}

/** A buff or a curse (2.78.0) as a tile: the skill's mark in gold for a buff, in blood for a curse, darkening as it wears off. */
@Composable private fun EffectTile(view: EffectView, side: Dp = 30.dp) {
    val tint = if (view.kind == EffectKind.CURSE) LifeRed else Gold
    val shape = RoundedCornerShape(4.dp)
    val title = SkillText.title(view.source)
    Tipped({ Tip(title, ui(if (view.kind == EffectKind.CURSE) "fight.effect_curse" else "fight.effect_buff", fineNumber(view.seconds))) },
        Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
            .semantics { contentDescription = title }) {
        SkillGlyph(view.icon, Modifier.fillMaxSize().padding(side / 6), tint)
        Box(Modifier.fillMaxWidth().fillMaxHeight((1 - view.left).coerceIn(0f, 1f)).background(Color.Black.copy(alpha = .55f)))
    }
}

@Composable private fun StateTile(ailment: Ailment?, tint: Color, left: Float, stacks: Int, label: String, side: Dp = 30.dp, tip: () -> Tip) {
    val shape = RoundedCornerShape(4.dp)
    Tipped(tip, Modifier.size(side).clip(shape).background(Color(0xFF0B0E13)).background(tint.copy(alpha = .16f)).border(1.dp, tint, shape)
        .semantics { contentDescription = label }) {
        Canvas(Modifier.fillMaxSize().padding(side / 5)) { stateGlyph(ailment, tint) }
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
        // Thorns and reflect (2.75.0): the one who was struck gives a blow back.
        Action.REFLECT -> if (hero) ui("expedition.log_reflect_you", monster, damage) else ui("expedition.log_reflect_they", monster, damage)
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
        // A skill (2.78.0): a blow names it, anything else says who used it and on whom.
        Action.SKILL -> {
            val skill = SkillText.title(event.skill.orEmpty())
            when {
                event.kind == HitKind.EVADED -> if (hero) ui("expedition.log_skill_evaded", skill, monster) else ui("expedition.log_skill_you_evade", monster, skill)
                event.kind == HitKind.BLOCKED -> if (hero) ui("expedition.log_skill_blocked", skill, monster) else ui("expedition.log_skill_you_block", monster, skill)
                event.damage > 0 -> if (hero) ui(if (event.kind == HitKind.CRIT) "expedition.log_skill_crit" else "expedition.log_skill_hit", skill, monster, damage)
                    else ui(if (event.kind == HitKind.CRIT) "expedition.log_they_skill_crit" else "expedition.log_they_skill_hit", monster, skill, damage)
                event.onSelf -> (if (hero) ui("expedition.log_skill_self", skill) else ui("expedition.log_they_skill_self", monster, skill)) +
                    (if (event.healed >= 1) " · +${event.healed.roundToInt()}" else "")
                else -> if (hero) ui("expedition.log_skill_on", skill, monster) else ui("expedition.log_they_skill_on", monster, skill)
            }
        }
        Action.FLASK -> ui("expedition.log_flask", locOr(LocaleKey.equipmentName(event.skill.orEmpty()), displayName(event.skill.orEmpty()))) +
            (if (event.healed >= 1) " · +${event.healed.roundToInt()}" else "")
    }
    // The blow's leading element and what it left behind, as words after the sentence.
    val marks = buildList {
        event.type?.takeIf { event.action.strikes && event.landed && event.damage > 0 && it != DamageType.PHYSICAL }?.let { add(ui(it.key())) }
        if (event.stunned) add(ui("expedition.stunned"))
        event.inflicted.forEach { add(ui(it.key())) }
    }
    return if (marks.isEmpty()) line else "$line · ${marks.joinToString(" · ")}"
}

private fun logColour(event: CombatEvent): Color = when {
    event.action == Action.RETREAT -> Muted
    event.action == Action.FLASK -> Vital
    event.action == Action.SKILL && event.damage <= 0 && event.landed -> if (event.actor == Side.HERO) Gold else Color(0xFFE9A0A0)
    event.action == Action.TICK || event.action == Action.REFLECT -> damageTint(event.type).copy(alpha = .85f)
    event.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    event.kind == HitKind.HIT -> if (event.actor == Side.HERO) Parchment else Color(0xFFE9A0A0)
    else -> Muted
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
    hit.kind == HitKind.EVADED || hit.kind == HitKind.BLOCKED -> Muted
    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    else -> damageTint(hit.type, onHero = hit.target == Side.HERO)
}

internal fun outcomeColour(outcome: Outcome) = when (outcome) { Outcome.WIN -> Vital; Outcome.LOSS -> LifeRed; Outcome.RETREAT -> Muted }

/** What the server rolled — experience, gold, orbs and items — for a kill and a chest alike; an item opens its card (2.72.0). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun RewardLines(s: ForgeState, reward: CampaignReward) {
    var opened by remember { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    opened?.let { document ->
        ModalBottomSheet(onDismissRequest = { opened = null }, containerColor = Panel) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp)) {
                ItemCard(document, enabled = false, detailed = true, definitions = s.world.definitions)
            }
        }
    }
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
            val document = inventoryDocument(instance, s.world.inventoryBases[instance.equipmentId])
            ItemRow(document, s.world.definitions, price = s.sellPrice(instance)) { opened = document }
        }
        if (reward.items.isEmpty() && reward.equipment.isEmpty()) Text(ui("expedition.loot_nothing"), color = Muted)
    }
}
