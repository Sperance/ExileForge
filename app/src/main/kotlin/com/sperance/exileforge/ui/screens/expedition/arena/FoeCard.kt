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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.DamageNumbers
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/** The pack's leader by name and what the pack is: its rarity, the map's level, the fight of a chain, how many are left standing. */
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
                ui("fight.stage", fight.round, fight.rounds).takeIf { fight.rounds > 1 },
                ui("expedition.pack_left", fight.standing, fight.foes.size).takeIf { fight.foes.size > 1 },
            ).joinToString(" · ")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(line, color = Muted, style = MaterialTheme.typography.labelSmall)
                // Мощь вожака стаи (4.8.0): «⚜ 15.9k» цвета его стойки против героя
                FoeMightMark(leader)
            }
            // Ярость боя (4.3.0): ступень и отсчёт - и у стаи, не только у стража
            fight.rage?.let { RageMark(it) }
        }
        BugAction(Modifier.align(Alignment.TopEnd))
    }
}

/** Мощь врага [monster] мелко (4.8.0): печать и число цвета его стойки против героя; вне оверлея боя - ничего. Не прогноз боя. */
@Composable internal fun FoeMightMark(monster: RolledMonster, modifier: Modifier = Modifier) {
    val might = LocalFightMight.current ?: return
    val reading = might.of(monster)
    FoeMight(reading, might.standing(reading), modifier)
}

/** A swing and a skill's blow carry a card toward the other side (2.78.0); a tick, a reflection, a draught do not. */
internal val Action.strikes: Boolean get() = this == Action.ATTACK || this == Action.SKILL

/**
 * Выпад снимка [fight] к часам кадра [now] ([FightFrame.now]): доля 0..1; за её концом выпада уже нет, как в бою - null.
 */
internal fun lungeAt(fight: FightHud, now: Double): Float? = fight.lunge?.progressAt(fight, now)?.takeIf { it < 1f }

/** How far a card is carried toward the other side by its own swing: out and back over the lunge ([progress] - его доля). */
internal fun reach(lunge: LungeView?, progress: Float?, actor: Side, foe: Int?): Float {
    if (lunge == null || progress == null) return 0f
    if (lunge.actor != actor || !lunge.action.strikes || (foe != null && lunge.foe != foe)) return 0f
    return sin(progress * PI).toFloat()
}

/** Whether this card is the one being struck right now, past the lunge's midpoint. */
internal fun struck(lunge: LungeView?, progress: Float?, target: Side, foe: Int?): Boolean {
    if (lunge == null || progress == null) return false
    if (lunge.actor == target || !lunge.action.strikes || progress < .5f) return false
    return foe == null || lunge.foe == foe
}

/** How white a portrait flashes as a landed blow reaches it. */
internal fun flash(lunge: LungeView?, progress: Float?, target: Side, foe: Int?): Float = if (lunge != null && progress != null && lunge.landed && struck(lunge, progress, target, foe)) (1 - progress) * 2f * (if (lunge.kind == HitKind.CRIT) 1f else .6f) else 0f

/** Вспышка портрета [foe] от выпада снимка [fight] в кадре [frame]: читать в рисовании. */
internal fun lungeFlash(fight: FightHud, frame: FightFrame, target: Side, foe: Int?): Float = flash(fight.lunge, lungeAt(fight, frame.now(fight)), target, foe)

/** Край карточки, как у `Modifier.border` скруглённой формы: нить [width] внутри границ, углы [radius]. */
internal fun DrawScope.roundEdge(radius: Dp, width: Dp, color: Color) {
    val stroke = ceil(width.toPx()).coerceAtMost(ceil(size.minDimension / 2))
    val half = stroke / 2
    val corner = (radius.toPx() - half).coerceAtLeast(0f)
    drawRoundRect(color, Offset(half, half), Size(size.width - stroke, size.height - stroke), CornerRadius(corner), style = Stroke(stroke))
}

/**
 * One foe's card: its portrait in its rarity's frame, its name, life and shield, its swing and what
 * is on it. Lit and lowered toward the hero while it swings, ringed in blood while struck, ringed in
 * gold when the player singled it out, marked with a sight when the hero's next blow goes to it.
 * The fallen go dark; one the hero's weapon cannot reach yet is dimmed.
 */
@Composable internal fun FoeCard(
    foe: FoeView,
    fight: FightHud,
    frame: FightFrame,
    open: Boolean,
    modifier: Modifier,
    large: Boolean,
    traits: List<TraitView>,
    /** Three and more on the field (3.81.0): the narrow tile of variant A. */
    narrow: Boolean = false,
    onTap: () -> Unit,
) {
    val ring = rarityTint(foe.monster.rarity)
    val live = foe.alive && fight.outcome == null

    // Замах и удары (4.9.3): последние 30% замаха - рамка наливается кровью, выпад - каждым своим ударом, тряска - каждым по нему.
    // Всё это - в рисовании кадра ([FightFrame]): карточка не перекомпонуется на каждом шаге боя
    fun swing() = if (live) frame.foeSwing(foe) else 0f
    fun acting() = strikeReach(fight, frame.now(fight), Side.MONSTER, foe.index, pet = null, swing = swing(), held = foe.held)
    fun flash() = if (foe.alive) strikeFlash(fight, frame.now(fight), Side.MONSTER, foe.index, pet = null) else 0f
    val focused = fight.focus == foe.index
    val shape = RoundedCornerShape(8.dp)
    var effectsOpen by remember { mutableStateOf(false) }
    val wash = foe.ailments.map { it.ailment }.maxByOrNull(::washAmount)
    Box(
        modifier.graphicsLayer {
            translationY = acting() * 10.dp.toPx()
            translationX = shake(flash(), frame.time).dp.toPx()
            // A stun gone off (3.78.0) tilts the card while it holds.
            rotationZ = if (foe.alive && foe.buildup?.stunned == true) -3f else 0f
            alpha = when {
                !foe.alive -> .35f
                !foe.reachable && fight.started -> .7f
                else -> 1f
            }
        },
    ) {
        Box(
            Modifier.drawWithContent {
                val acting = acting()
                val hit = flash() > 0f
                val glow = if (live) swingGlow(swing(), foe.held) else 0f
                val outline = shape.createOutline(size, layoutDirection, this)
                swingAura(outline, LifeRed, glow)
                drawOutline(outline, if (acting > 0f) FoeActing else FoeGround)
                if (foe.taunt && foe.alive && acting == 0f && !hit && !focused && !open) tauntAura(outline, frame.time)
                drawContent()
                val border = when {
                    acting > 0f -> LifeRed
                    hit -> LifeRed.copy(alpha = .8f)
                    focused || open -> GoldBright
                    else -> lerp(ring.copy(alpha = .55f), LifeRed, glow)
                }
                roundEdge(8.dp, if (focused || acting > 0f || glow > 0f) 2.dp else 1.dp, border)
            }.clip(shape).clickable(enabled = foe.alive && fight.outcome == null, onClick = onTap),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(if (narrow) 5.dp else 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = if (narrow) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                val portrait: @Composable (Dp) -> Unit = { size ->
                    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF0B0E13)).border(2.dp, ring.copy(alpha = .8f), CircleShape)) {
                        Canvas(Modifier.fillMaxSize()) {
                            Portraits.monster(
                                this,
                                foe.monster.code.value,
                                foe.monster.form,
                                frame.time,
                                wash?.let(::ailmentTint),
                                wash?.let(::washAmount) ?: 0f,
                                flash(),
                            )
                        }
                    }
                }
                val seals: @Composable () -> Unit = {
                    // Its traits (3.73.0) as seals; a tap on the card opens what they do.
                    if (traits.isNotEmpty() && foe.alive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            // Печать свойства (3.92.0) открывает его лист
                            val lore = LocalLore.current
                            traits.forEach { trait ->
                                SkillGlyph(
                                    trait.icon,
                                    Modifier.size(14.dp).then(if (lore != null) Modifier.clickable { lore(Lore.Trait(trait, monsterTitle(foe.monster.code))) } else Modifier),
                                    Color(0xFFE8B06A),
                                )
                            }
                        }
                    }
                }
                if (narrow) {
                    // Variant A (3.81.0): three narrow tiles — the portrait over a one-line name that shrinks to fit; the whole name is in its window.
                    portrait(FOE_AVATAR_NARROW)
                    FittedName(monsterTitle(foe.monster.code), ring)
                    FoeMightMark(foe.monster)
                    seals()
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        portrait(FOE_AVATAR)
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
                            FoeMightMark(foe.monster)
                            seals()
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
                SwingBar({ swing() }, foe.held, Modifier.fillMaxWidth()) { if (acting() > 0f) LifeRed else SwingRed }
                // What is on it (4.9.3): одна компактная полка - что не влезло, то «+N», касание открывает лист эффектов врага.
                val tile = if (large) 16.dp else 14.dp
                if (foe.held || foe.ailments.isNotEmpty() || foe.effects.isNotEmpty()) {
                    val shelf = timedShelf(foe.held, foe.ailments, foe.effects) + foe.effects.filter { it.lasting }.map { Shelved.Effect(it) }
                    EffectShelf(shelf, tile, { effectsOpen = true }, Modifier.fillMaxWidth().height(tile), gap = 2.dp)
                }
            }
            // A narrow tile keeps the level in its corner.
            if (narrow && foe.monster.level > 0) {
                Text(ui("fight.level_short", foe.monster.level), color = Muted, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 5.dp))
            }
            if (fight.target == foe.index && foe.alive && fight.outcome == null) {
                Text(if (focused) "◉" else "◎", color = GoldBright, fontSize = 14.sp, modifier = Modifier.align(if (narrow) Alignment.BottomEnd else Alignment.TopEnd).padding(4.dp))
            }
            if (foe.taunt && foe.alive) TauntSeal(frame, Modifier.align(Alignment.TopStart).padding(2.dp).size(18.dp)) { tauntTip(false) }
            if (!foe.alive) {
                Text(ui("fight.fallen"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center))
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
            if (foe.alive) foe.buildup?.let { BuildupMark(it, frame) }
            CardHits(fight, frame, fight.hits.filter { it.target == Side.MONSTER && it.foe == foe.index })
        }
        // Плашка намерения (4.9.3): замах врага вот-вот ляжет на героя - проступает в рисовании по доле замаха
        if (live && fight.foesAimHero && !foe.held) {
            Text(
                ui("fight.at_you"),
                color = Color(0xFFFF8A83),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 14.dp).graphicsLayer { alpha = ((swing() - AIM_FROM) / AIM_FADE).coerceIn(0f, 1f) }
                    .background(Color(0xFF3A1414), RoundedCornerShape(10.dp)).border(1.dp, LifeRed, RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
    if (effectsOpen) {
        val timed = timedShelf(foe.held, foe.ailments, foe.effects)
        EffectsSheet(ui("fight.shelf.foe", monsterTitle(foe.monster.code)), timed, foe.effects.filter { it.lasting }.map { Shelved.Effect(it) }, null) { effectsOpen = false }
    }
}

/** С какой доли замаха врага проступает плашка «→ по вам» и за какую долю она набирает силу. */
private const val AIM_FROM = .55f
private const val AIM_FADE = .2f

/** Подложка карточки врага: в выпаде - кровью, иначе панелью. */
private val FoeActing = Blood.copy(alpha = .35f)
private val FoeGround = Panel.copy(alpha = .9f)
private val SwingRed = LifeRed.copy(alpha = .7f)

/** The round portrait of a foe's card (3.80.0, variant C). */
private val FOE_AVATAR = 52.dp

/** The portrait of a narrow tile (3.81.0, variant A). */
private val FOE_AVATAR_NARROW = 46.dp

/** A foe's name on one line, its type stepping down from 11sp to 8.5sp until it fits, cut only past that. */
@Composable private fun FittedName(name: String, color: Color) {
    var size by remember(name) { mutableFloatStateOf(11f) }
    Text(
        name,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        overflow = if (size > 8.5f) TextOverflow.Clip else TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        onTextLayout = { if (it.hasVisualOverflow && size > 8.5f) size -= .5f },
    )
}

/**
 * The numbers rising off a card: the blows that reached it in the last second. Подъём и угасание - в рисовании кадра [frame]:
 * возраст числа идёт и между снимками боя.
 */
@Composable internal fun BoxScope.CardHits(fight: FightHud, frame: FightFrame, hits: List<FloatingHit>) {
    // The settings (3.77.0) choose which numbers rise: all, the critical strikes alone, or none.
    val shown = when (LocalSettings.current.damageNumbers) {
        DamageNumbers.ALL -> hits
        DamageNumbers.CRITS -> hits.filter { it.kind == HitKind.CRIT }
        DamageNumbers.OFF -> return
    }
    shown.takeLast(3).forEach { hit ->
        Text(
            hitLabel(hit),
            color = hitColour(hit),
            textAlign = TextAlign.Center,
            fontSize = when {
                hit.kind == HitKind.CRIT -> 20.sp
                hit.action == Action.TICK -> 12.sp
                else -> 16.sp
            },
            fontWeight = if (hit.action == Action.TICK) FontWeight.Normal else FontWeight.Black,
            fontStyle = if (hit.action == Action.TICK) FontStyle.Italic else FontStyle.Normal,
            modifier = Modifier.align(Alignment.Center).offset(x = (((hit.id % 3) - 1) * 14).dp).graphicsLayer {
                val rise = (hit.ageAt(fight, frame.now(fight)) / ExpeditionRun.HIT_LIFETIME).toFloat().coerceIn(0f, 1f)
                translationY = (-36 * rise).dp.toPx()
                alpha = 1 - rise
            },
        )
    }
}

/** Удар по герою (4.9.3): число всегда кровью и с «−», за ним - значок стихии её цветом; по врагу - цветом стихии, как было. */
private val FloatingHit.onHero: Boolean get() = target == Side.HERO && amount > 0 && !(kind == HitKind.EVADED || kind == HitKind.BLOCKED)

private fun hitLabel(hit: FloatingHit): AnnotatedString {
    if (!hit.onHero) return AnnotatedString(hitText(hit))
    val glyph = hit.type?.let(::damageGlyph)
    return buildAnnotatedString {
        append("−")
        append(hitText(hit))
        if (glyph != null) withStyle(SpanStyle(color = damageTint(hit.type), fontSize = 11.sp, fontWeight = FontWeight.Bold)) { append(" $glyph") }
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

    hit.onHero -> HeroHurt

    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)

    else -> damageTint(hit.type)
}
