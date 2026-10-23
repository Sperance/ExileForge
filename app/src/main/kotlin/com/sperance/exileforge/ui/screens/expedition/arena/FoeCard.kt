package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.DamageNumbers
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

/** Мощь врага [monster] мелко: печать и число цвета его стойки против героя; вне оверлея боя и у босса - ничего. Не прогноз боя. */
@Composable internal fun FoeMightMark(monster: RolledMonster, modifier: Modifier = Modifier) {
    val might = LocalFightMight.current ?: return
    val reading = might.of(monster) ?: return
    FoeMight(reading, might.standing(reading), modifier)
}

/** A swing and a skill's blow carry a card toward the other side; a tick, a reflection, a draught do not. */
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
 *
 * [narrow] - три и больше на поле: узкая плитка. Замах (последние 30% - рамка наливается кровью), выпады и тряска - в рисовании кадра
 * ([FightFrame]): карточка не перекомпонуется на каждом шаге боя. Уровень врага - плашкой [LevelBadge] на правом нижнем углу портрета
 * в нити его редкости: свой, на карте он бывает выше или ниже карты, без своего - уровень зоны [level]. Полка
 * эффектов одна, не влезло - «+N». Фокус за провокатором держится (отметка «Следом»), удары идут провокатору. Замах по герою - ещё и
 * стрелка ↓ в правом верхнем углу ([AimArrow]), проступает по доле замаха; для чтения с экрана - состояние карточки. Метка цели - в
 * правом нижнем углу.
 */
@Composable internal fun FoeCard(
    foe: FoeView,
    fight: FightHud,
    frame: FightFrame,
    open: Boolean,
    modifier: Modifier,
    large: Boolean,
    traits: List<TraitView>,
    level: Int,
    narrow: Boolean = false,
    onTap: () -> Unit,
) {
    val ring = rarityTint(foe.monster.rarity)
    val live = foe.alive && fight.outcome == null

    fun swing() = if (live) frame.foeSwing(foe) else 0f
    fun acting() = strikeReach(fight, frame.now(fight), Side.MONSTER, foe.index, swing = swing(), held = foe.held)
    fun flash() = if (foe.alive) strikeFlash(fight, frame.now(fight), Side.MONSTER, foe.index) else 0f
    val focused = fight.focus == foe.index
    val shape = RoundedCornerShape(8.dp)
    var effectsOpen by remember { mutableStateOf<ShelfFocus?>(null) }
    val wash = foe.ailments.map { it.ailment }.maxByOrNull(::washAmount)
    val shelves = rememberFoeShelves(foe.ailments, foe.held, foe.effects)
    val shelf = remember(shelves) { shelves.timed + shelves.lasting }
    val tile = if (large) 16.dp else 14.dp
    val hits = remember { ShelfHits() }
    val peek = rememberShelfPeek()
    val aiming = live && !foe.held && foe.swing >= AIM_FROM
    val aimNote = ui("fight.swing_at_you")
    Box(
        modifier.semantics { if (aiming) stateDescription = aimNote }.graphicsLayer {
            translationY = acting() * 10.dp.toPx()
            translationX = shake(flash(), frame.time).dp.toPx()
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
                    Box {
                        Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF0B0E13)).border(2.dp, ring.copy(alpha = .8f), CircleShape)) {
                            Canvas(Modifier.fillMaxSize()) {
                                Portraits.monster(
                                    this,
                                    foe.monster.code.value,
                                    foe.monster.form,
                                    frame.breath,
                                    wash?.let(::ailmentTint),
                                    wash?.let(::washAmount) ?: 0f,
                                    flash(),
                                )
                            }
                        }
                        FoeLevelBadge(foe.monster.level.takeIf { it > 0 } ?: level, ring, Modifier.align(Alignment.BottomEnd))
                    }
                }
                val seals: @Composable () -> Unit = {
                    if (traits.isNotEmpty() && foe.alive) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
                    portrait(FOE_AVATAR_NARROW)
                    FoeName(monsterTitle(foe.monster.code), ring)
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
                            FoeMightMark(foe.monster)
                            seals()
                        }
                    }
                }
                LifeBar(foe.life, foe.maxLife, foe.shield, foe.maxShield, Modifier.fillMaxWidth().height(12.dp))
                if (foe.maxMana > 0) {
                    Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0x14FFFFFF), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth((foe.mana / foe.maxMana.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(ManaBlue, RoundedCornerShape(2.dp)))
                    }
                }
                if (foe.alive) foe.buildup?.let { BuildupBar(it, Modifier.fillMaxWidth()) }
                SwingBar({ swing() }, foe.held, Modifier.fillMaxWidth()) { if (acting() > 0f) LifeRed else SwingRed }
                if (shelf.isNotEmpty() || peek.key != null) EffectShelf(shelf, tile, { effectsOpen = ShelfFocus(it?.key) }, peek, Modifier.fillMaxWidth().height(tile), gap = 2.dp, hits = hits)
            }
            if (shelf.isNotEmpty()) {
                ShelfTouch(hits, Modifier.align(Alignment.BottomCenter)) { item -> if (item != null) peek.open(item) else effectsOpen = ShelfFocus(null) }
            }
            if (fight.target == foe.index && foe.alive && fight.outcome == null) {
                Text(if (focused) "◉" else "◎", color = GoldBright, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
            }
            if (live && !foe.held) AimArrow({ ((swing() - AIM_FROM) / AIM_FADE).coerceIn(0f, 1f) }, Modifier.align(Alignment.TopEnd).padding(3.dp))
            if (foe.taunt && foe.alive) TauntSeal(frame, Modifier.align(Alignment.TopStart).padding(2.dp).size(18.dp)) { tauntTip() }
            if (!foe.alive) {
                Text(ui("fight.fallen"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center))
            } else {
                focusMark(foe, fight, taunt = false)?.takeIf { it == FocusMark.QUEUED }?.let { FocusBadge(it, Modifier.align(Alignment.BottomCenter)) }
            }
            if (foe.alive) foe.buildup?.let { BuildupMark(it, frame) }
            CardHits(fight, frame, fight.hits.filter { it.target == Side.MONSTER && it.foe == foe.index })
        }
    }
    effectsOpen?.let { focus -> EffectsSheet(ui("fight.shelf.foe", monsterTitle(foe.monster.code)), shelves.timed, shelves.lasting, null, focus.key) { effectsOpen = null } }
}

/**
 * Зона касания полки эффектов врага - нижние [SHELF_TOUCH] карточки (полоса здоровья, замах и полка): плитки по 14-16 dp пальцу
 * не попасть, а вся карточка выбирает цель. Касание - плитка под пальцем по горизонтали ([onOpen]: её выноска) - её называет раскладка
 * самой полки ([ShelfHits]); мимо плиток и на «+N» - null (лист эффектов сверху).
 */
@Composable private fun ShelfTouch(hits: ShelfHits, modifier: Modifier, onOpen: (Shelved?) -> Unit) {
    val pick by rememberUpdatedState(onOpen)
    Box(
        modifier.fillMaxWidth().height(SHELF_TOUCH).onPlaced { hits.zonePlaced(it.positionInRoot().x) }.pointerInput(hits) {
            detectTapGestures { at -> pick(hits.at(at.x)) }
        },
    )
}

/**
 * Плашка уровня [level] врага или свиты на правом нижнем углу его портрета, нить цвета [tint] (редкость врага, у тотема - его цвет);
 * сдвиг за угол - как у гнезда вещи ([LevelBadgeOffset]). Боссу плашки нет: его уровень - в полосе рамки.
 */
@Composable internal fun FoeLevelBadge(level: Int, tint: Color, modifier: Modifier = Modifier) {
    if (level <= 0) return
    LevelBadge(level, tint, ui("fight.level_badge", level), modifier.offset(x = LevelBadgeOffset.x, y = LevelBadgeOffset.y))
}

/** Высота зоны касания полки эффектов врага. */
private val SHELF_TOUCH = 44.dp

/**
 * Стрелка ↓ замаха врага по герою в углу карточки: проступает по доле замаха [shown] (0..1, читается в слое кадра); для чтения с экрана
 * её нет - замах называет состояние карточки.
 */
@Composable private fun AimArrow(shown: () -> Float, modifier: Modifier) {
    Box(
        modifier.size(AIM_SIDE).graphicsLayer { alpha = shown() }.background(AimGround, CircleShape).border(1.dp, LifeRed, CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text("↓", color = AimInk, fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** Стрелка замаха: сторона, подложка и цвет знака. */
private val AIM_SIDE = 16.dp
private val AimGround = Color(0xFF3A1414)
private val AimInk = Color(0xFFFF8A83)

/** С какой доли замаха врага проступает стрелка ↓ и за какую долю она набирает силу. */
private const val AIM_FROM = .55f
private const val AIM_FADE = .2f

/** Подложка карточки врага: в выпаде - кровью, иначе панелью. */
private val FoeActing = Blood.copy(alpha = .35f)
private val FoeGround = Panel.copy(alpha = .9f)
private val SwingRed = LifeRed.copy(alpha = .7f)

/** The round portrait of a foe's card (variant C). */
private val FOE_AVATAR = 52.dp

/** The portrait of a narrow tile (variant A). */
private val FOE_AVATAR_NARROW = 46.dp

/** Имя врага узкой плитки - до двух строк по центру: кегль от 11sp вниз до 8.5sp, сколько влезет, дальше многоточие. */
@Composable private fun FoeName(name: String, color: Color) {
    BasicText(
        name,
        style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 12.sp, textAlign = TextAlign.Center),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.5.sp, maxFontSize = 11.sp, stepSize = .5.sp),
    )
}

/**
 * The numbers rising off a card: the blows that reached it in the last second. Подъём и угасание - в рисовании кадра [frame]:
 * возраст числа идёт и между снимками боя.
 *
 * Настройки выбирают, какие числа всплывают: все, только криты или ни одного.
 */
@Composable internal fun BoxScope.CardHits(fight: FightHud, frame: FightFrame, hits: List<FloatingHit>) {
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

/** Удар по герою: число всегда кровью и с «−», за ним - значок стихии её цветом; по врагу - цветом стихии. */
private val FloatingHit.onHero: Boolean get() = target == Side.HERO && amount > 0 && !(kind == HitKind.EVADED || kind == HitKind.BLOCKED || kind == HitKind.INVULNERABLE)

private fun hitLabel(hit: FloatingHit): AnnotatedString {
    if (!hit.onHero) return AnnotatedString(hitText(hit))
    val glyph = hit.type?.let(::damageGlyph)
    return buildAnnotatedString {
        append("−")
        append(hitText(hit))
        if (glyph != null) withStyle(SpanStyle(color = tickTint(hit.ailment, hit.type), fontSize = 11.sp, fontWeight = FontWeight.Bold)) { append(" $glyph") }
    }
}

private fun hitText(hit: FloatingHit): String = when {
    hit.amount == 0 && hit.healed > 0 -> "+${hit.healed}"
    hit.kind == HitKind.EVADED -> ui("expedition.evaded")
    hit.kind == HitKind.BLOCKED -> ui("expedition.blocked")
    hit.kind == HitKind.INVULNERABLE -> ui("expedition.invulnerable")
    hit.kind == HitKind.CRIT -> ui("expedition.crit", hit.amount)
    else -> hit.amount.toString()
}

private fun hitColour(hit: FloatingHit): Color = when {
    hit.amount == 0 && hit.healed > 0 -> Vital
    hit.kind == HitKind.EVADED || hit.kind == HitKind.BLOCKED -> Muted
    hit.kind == HitKind.INVULNERABLE -> FrameGoldBright
    hit.onHero -> HeroHurt
    hit.kind == HitKind.CRIT -> Color(0xFFFFD34A)
    else -> tickTint(hit.ailment, hit.type)
}
