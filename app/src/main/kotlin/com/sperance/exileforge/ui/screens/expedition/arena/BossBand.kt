package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.NoteKind
import com.sperance.exileforge.core.campaign.NoteTrace
import com.sperance.exileforge.core.campaign.TraitView
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.RiftGuard
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.FoeView
import com.sperance.exileforge.core.campaign.run.SlotView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.phaseTitle
import com.sperance.exileforge.core.display.totemTitle
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.RiftLaw
import com.sperance.exileforge.rules.content.TotemKind
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Сколько секунд над портретом держится титул новой фазы. */
private const val PHASE_TITLE_SECONDS = 2.6

/** За сколько секунд до готовности умения вокруг портрета загорается круг-предупреждение. */
private const val TELEGRAPH_SECONDS = 1.6

/** Палитра боя с боссом «Кино» (3.93.0): золото рамки, кровь полосы, фиолет каста и свиты. */
internal val FrameGold = Color(0xFFD9B46A)
internal val FrameGoldBright = Color(0xFFF5D58A)
private val FrameInk = Color(0xFF120A08)
private val FrameDeep = Color(0xFF26170F)
private val LifeTop = Color(0xFFFF6A5A)
private val LifeMid = Color(0xFFC22A22)
private val LifeLow = Color(0xFF7A1210)
private val LifeTrail = Color(0xFFF2B8A8)
private val LifeTrack = Color(0xFF2A0B09)
private val CastTint = Color(0xFFA26BFF)
private val PortalTint = Color(0xFFB07CFF)
private val StoneLight = Color(0xFF8A7458)
private val StoneDark = Color(0xFF2A2018)
private val ManaThread = Color(0xFF4A8AE0)
private val StunGold = Color(0xFFF2C14A)
private val FreezeCyan = Color(0xFF7FD8F0)
private val ShockViolet = Color(0xFFB79CFF)

/** Цвет тотема по роду и стихии. */
internal fun totemTint(slot: SlotView.Totem): Color = when (slot.kind) {
    TotemKind.POWER -> Color(0xFFFF6A3A)

    TotemKind.CURSE -> Color(0xFFB07CFF)

    TotemKind.RETINUE -> Color(0xFFE8C060)

    TotemKind.ELEMENT -> when (slot.element) {
        "FIRE" -> Color(0xFFFF7A3A)
        "COLD" -> Color(0xFF5FD0F0)
        "LIGHTNING" -> Color(0xFFF2E04A)
        else -> Color(0xFF9FE8C0)
    }
}

/** Номер текущей фазы босса: одна плюс сработавшие шаги. */
internal fun phaseNumber(boss: BossHud): Int = 1 + boss.passed.count { it }

/**
 * Бой с боссом «Кино» (3.93.0, макет владельца): сверху - кованая рамка с именем, номером фазы, полосой здоровья со шлейфом и
 * кристаллами порогов, щитом и барьером, нитью маны, накоплениями и кастом; ниже - крупный портрет в ауре фазы и по три ниши
 * склепа с каждой стороны: в них встаёт свита из портала и тотемы со своим сроком. Касание приспешника выбирает его целью,
 * а на паузе открывает его лист; касание тотема - его справку.
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
    onInspect: (Int) -> Unit,
) {
    val foe = fight.foes.firstOrNull { it.index == boss.index } ?: return
    val name = monsterTitle(foe.monster.code)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BossFrame(foe, name, boss, fight, time)
        if (boss.slots.isEmpty()) {
            // Бой без слотов (страж без фаз и тотемов): прочие враги поля - узкими карточками по бокам, как прежде
            val others = fight.field.filter { it.index != boss.index }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                others.filterIndexed { i, _ -> i % 2 == 0 }.takeIf { it.isNotEmpty() }?.let { Retinue(it, fight, time, chosen, large, traits, track, onFocus) }
                PortraitStage(foe, boss, fight, time, chosen == foe.index, track(foe.index).weight(1f).height(196.dp)) { tap(fight, foe.index, onFocus, onInspect) }
                others.filterIndexed { i, _ -> i % 2 == 1 }.takeIf { it.isNotEmpty() }?.let { Retinue(it, fight, time, chosen, large, traits, track, onFocus) }
            }
        } else {
            NicheArena(foe, boss, fight, time, chosen, track, name) { tap(fight, it, onFocus, onInspect) }
        }
    }
}

/** Касание врага: на паузе и до боя - его лист, в бою - выбор цели. */
private fun tap(fight: FightHud, index: Int, onFocus: (Int) -> Unit, onInspect: (Int) -> Unit) = if (fight.scouting) onInspect(index) else onFocus(index)

// ============================================================ Рамка: имя, полосы, каст

@Composable private fun BossFrame(foe: FoeView, name: String, boss: BossHud, fight: FightHud, time: Float) {
    val lore = LocalLore.current
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(FrameDeep, FrameInk))).border(1.dp, Color(0xFF7A5C26), shape)
            .drawBehind { cornerFlourish(FrameGold) }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = FrameGold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            foe.monster.level.takeIf { it > 0 }?.let { level ->
                Text(
                    level.toString(),
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 6.dp).border(1.dp, Color(0xFF5A4632), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            // Ярость (3.95.0): сколько уже прибавил урон и когда следующая ступень; без неё - когда первая
            if (foe.alive) {
                Text(
                    if (boss.rage > 0) ui("boss.rage", number(boss.rage), ceil(boss.rageIn).toInt()) else ui("boss.rage_in", ceil(boss.rageIn).toInt()),
                    color = if (boss.rage > 0) Ember else Muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            if (boss.marks.isNotEmpty()) {
                Text(
                    ui("boss.phase_number", roman(phaseNumber(boss))),
                    color = FrameGoldBright,
                    fontSize = 9.sp,
                    letterSpacing = 2.5.sp,
                    modifier = if (lore != null && boss.phase != null) Modifier.clickable { lore(Lore.Phase(boss.phase!!, name)) } else Modifier,
                )
            }
        }
        LifeBar(foe, boss, time)
        if (boss.rift != null && foe.alive) RiftStrip(boss, time)
        if (foe.maxMana > 0) ThinBar(foe.mana / foe.maxMana.toFloat(), ManaThread, 3.dp)
        foe.buildup?.takeIf { foe.alive && it.bars.any { share -> share > 0.005f } }?.let { Buildups(it.bars) }
        if (foe.alive && (foe.ailments.isNotEmpty() || foe.effects.isNotEmpty() || foe.held)) StateTiles(foe.ailments, foe.held, foe.effects)
        boss.cast?.let { cast ->
            Row(
                Modifier.fillMaxWidth().then(if (lore != null) Modifier.clickable { lore(Lore.Skill(cast.code, name, null, fight.heroBody)) } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val near = cast.left < TELEGRAPH_SECONDS
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF2A1840))
                        .border(1.dp, if (near) CastTint.copy(alpha = .6f + .4f * sin(time * 9f)) else CastTint.copy(alpha = .5f), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("✦", color = Color(0xFFD4B6FF), fontSize = 11.sp) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row {
                        Text(SkillText.title(cast.code), color = Color(0xFFD4B6FF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(ui("boss.cast_in", fineNumber(cast.left)), color = if (near) Color(0xFFFFB0B0) else Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    ThinBar(cast.progress, CastTint, 4.dp)
                }
            }
        }
    }
}

/** Кованые уголки рамки: золотая скоба в каждом углу. */
private fun DrawScope.cornerFlourish(tint: Color) {
    val l = 12.dp.toPx()
    val w = 1.6.dp.toPx()
    val inset = 2.dp.toPx()
    listOf(Offset(inset, inset) to Offset(1f, 1f), Offset(size.width - inset, inset) to Offset(-1f, 1f), Offset(inset, size.height - inset) to Offset(1f, -1f), Offset(size.width - inset, size.height - inset) to Offset(-1f, -1f))
        .forEach { (corner, dir) ->
            drawLine(tint, corner, corner + Offset(l * dir.x, 0f), w, StrokeCap.Round)
            drawLine(tint, corner, corner + Offset(0f, l * dir.y), w, StrokeCap.Round)
            drawCircle(tint, 1.8.dp.toPx(), corner + Offset(3.dp.toPx() * dir.x, 3.dp.toPx() * dir.y))
        }
}

/**
 * Полоса здоровья босса: шлейф урона догоняет её с запозданием, глянец и бегущий блик, кристаллы порогов фаз (сработавшие
 * гаснут), щит - штриховкой поверх, барьер - золотым свечением и числом, здоровье числом на тёмной плашке.
 */
@Composable private fun LifeBar(foe: FoeView, boss: BossHud, time: Float) {
    val share = if (foe.maxLife > 0) (foe.life / foe.maxLife.toFloat()).coerceIn(0f, 1f) else 0f
    val trail by animateFloatAsState(share, tween(700, delayMillis = 260, easing = FastOutSlowInEasing), label = "trail")
    val shield = if (foe.maxShield > 0) (foe.shield / foe.maxShield.toFloat()).coerceIn(0f, 1f) else 0f
    val motion = LocalSettings.current.animations
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.fillMaxWidth().height(18.dp)
            .drawBehind { if (foe.barrier > 0) drawRoundRect(FrameGoldBright.copy(alpha = .35f + .2f * sin(time * 3f)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()), style = Stroke(4.dp.toPx()), topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()), size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx())) }
            .clip(shape).background(LifeTrack).border(1.dp, Color(0xFF6A4A1A), shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(LifeTrail.copy(alpha = .75f), size = Size(w * trail.coerceAtLeast(share), h))
            drawRect(Brush.verticalGradient(listOf(LifeTop, LifeMid, LifeLow)), size = Size(w * share, h))
            drawRect(Color.White.copy(alpha = .22f), size = Size(w * share, h * .38f))
            if (motion && share > 0) {
                val x = ((time * .35f) % 1.6f - .3f) * w
                clipRect(right = w * share) { drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .28f), Color.Transparent), x, x + w * .18f), size = size) }
            }
            if (shield > 0) {
                clipRect(right = w * shield) {
                    drawRect(ShieldCyan.copy(alpha = .28f))
                    var s = -h
                    while (s < w) {
                        drawLine(ShieldCyan.copy(alpha = .7f), Offset(s, h), Offset(s + h, 0f), 1.2.dp.toPx())
                        s += 5.dp.toPx()
                    }
                }
            }
            boss.marks.forEachIndexed { i, at ->
                val passed = boss.passed.getOrElse(i) { false }
                val x = w * (at / 100).toFloat()
                val r = h * .36f
                val gem = Path().apply {
                    moveTo(x, h / 2 - r)
                    lineTo(x + r, h / 2)
                    lineTo(x, h / 2 + r)
                    lineTo(x - r, h / 2)
                    close()
                }
                if (!passed) drawCircle(FrameGoldBright.copy(alpha = .35f), r * 1.7f, Offset(x, h / 2))
                drawPath(gem, if (passed) Brush.linearGradient(listOf(Color(0xFF3A2A16), Color(0xFF2A1E10))) else Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE8B04A)), Offset(x - r, h / 2 - r), Offset(x + r, h / 2 + r)))
                drawPath(gem, Color(0xFF7A5A1E), style = Stroke(1.dp.toPx()))
            }
        }
        val text = buildString {
            append(number(foe.life.toDouble()))
            append(" / ")
            append(number(foe.maxLife.toDouble()))
            if (foe.maxShield > 0 && foe.shield > 0) append("  ·  ⛨ ").append(number(foe.shield.toDouble()))
            if (foe.barrier > 0) append("  ·  ◈ ").append(number(foe.barrier.toDouble()))
        }
        Text(
            text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(6.dp)).background(Color(0x8C0A0404)).padding(horizontal = 7.dp),
        )
    }
}

@Composable private fun ThinBar(share: Float, tint: Color, height: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(Color(0xFF1A1414))) {
        Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(tint.copy(alpha = .7f), tint))))
    }
}

/** Накопления босса: оглушение, заморозка, электрошок - узкими полосами со знаком. */
@Composable private fun Buildups(bars: List<Float>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Buildup.entries.forEachIndexed { i, kind ->
            val share = bars.getOrElse(i) { 0f }
            if (share <= 0.005f) return@forEachIndexed
            val (sign, tint) = when (kind) {
                Buildup.STUN -> "⚡" to StunGold
                Buildup.FREEZE -> "❄" to FreezeCyan
                Buildup.ELECTROCUTE -> "ϟ" to ShockViolet
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sign, color = tint, fontSize = 10.sp)
                Box(Modifier.weight(1f)) { ThinBar(share, tint, 3.dp) }
            }
        }
    }
}

private fun roman(n: Int): String = listOf("I", "II", "III", "IV", "V", "VI").getOrElse(n - 1) { n.toString() }

// ============================================================ Арена: ниши и портрет

/**
 * Портрет босса между двумя колоннами ниш: слева слоты первой половины, справа - второй. Нити тянутся от тотемов: к боссу -
 * у силы и знамени, вниз к герою - у проклятия и стихии.
 */
@Composable private fun NicheArena(foe: FoeView, boss: BossHud, fight: FightHud, time: Float, chosen: Int?, track: (Int) -> Modifier, name: String, onTap: (Int) -> Unit) {
    val half = (boss.slots.size + 1) / 2
    val centres = remember { mutableStateMapOf<Int, Offset>() }
    var portrait by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxWidth().height(242.dp).onGloballyPositioned { origin = it.positionInRoot() }) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NicheColumn(0 until half, boss, fight, time, chosen, track, name, { i, at -> centres[i] = at - origin }, onTap)
            PortraitStage(
                foe,
                boss,
                fight,
                time,
                chosen == foe.index,
                track(foe.index).weight(1f).fillMaxHeight().padding(vertical = 10.dp).onGloballyPositioned { portrait = it.boundsInRoot().translate(-origin) },
            ) { onTap(foe.index) }
            NicheColumn(half until boss.slots.size, boss, fight, time, chosen, track, name, { i, at -> centres[i] = at - origin }, onTap)
        }
        // Нити тотемов поверх: от ниши к боссу или вниз, к герою; бегущий пунктир их цвета
        Canvas(Modifier.fillMaxSize()) {
            boss.slots.forEachIndexed { i, slot ->
                val totem = slot as? SlotView.Totem ?: return@forEachIndexed
                val from = centres[i] ?: return@forEachIndexed
                val tint = totemTint(totem)
                val toBoss = totem.kind == TotemKind.POWER || totem.kind == TotemKind.RETINUE
                val to = if (toBoss) Offset(if (from.x < portrait.center.x) portrait.left + 6.dp.toPx() else portrait.right - 6.dp.toPx(), portrait.center.y) else Offset(size.width / 2, size.height)
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    quadraticTo((from.x + to.x) / 2, (from.y + to.y) / 2 - 24.dp.toPx(), to.x, to.y)
                }
                drawPath(path, tint.copy(alpha = .22f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                drawPath(
                    path,
                    tint.copy(alpha = .85f),
                    style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 7.dp.toPx()), -time * 30.dp.toPx())),
                )
            }
        }
    }
}

@Composable private fun NicheColumn(
    range: IntRange,
    boss: BossHud,
    fight: FightHud,
    time: Float,
    chosen: Int?,
    track: (Int) -> Modifier,
    name: String,
    place: (Int, Offset) -> Unit,
    onTap: (Int) -> Unit,
) {
    Column(Modifier.width(76.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
        range.forEach { i ->
            Box(Modifier.fillMaxWidth().height(74.dp).onGloballyPositioned { place(i, it.boundsInRoot().center) }) {
                NicheSlot(boss.slots.getOrNull(i), fight, time, chosen, track, name, onTap)
            }
        }
    }
}

/** Ключ занятого слота: у приспешника и тотема свой, чтобы смена жильца шла анимацией ухода и входа. */
private fun slotKey(slot: SlotView?): String = when (slot) {
    null -> "-"
    is SlotView.Minion -> "m${slot.index}"
    is SlotView.Totem -> "t${slot.serial}"
}

/**
 * Ниша слота: пустая - тёмная арка с руной; приспешник встаёт из фиолетового портала и при смерти рассыпается вниз прахом;
 * тотем встаёт из портала своего цвета, его срок стекает по табличке, и он гаснет вспышкой.
 */
@Composable private fun NicheSlot(slot: SlotView?, fight: FightHud, time: Float, chosen: Int?, track: (Int) -> Modifier, name: String, onTap: (Int) -> Unit) {
    // Последний вид каждого жильца: уходящий рисуется таким, каким был
    val seen = remember { mutableMapOf<String, Pair<SlotView, FoeView?>>() }
    val key = slotKey(slot)
    if (slot != null) seen[key] = slot to (slot as? SlotView.Minion)?.let { m -> fight.foes.firstOrNull { it.index == m.index } }
    val lore = LocalLore.current
    AnimatedContent(
        targetState = key,
        transitionSpec = {
            (fadeIn(tween(420, delayMillis = 260)) + scaleIn(tween(520, delayMillis = 260), initialScale = .7f)) togetherWith
                (fadeOut(tween(900, easing = LinearEasing)) + slideOutVertically(tween(900)) { it / 5 } + scaleOut(tween(900), targetScale = .92f))
        },
        label = "niche",
    ) { shown ->
        val (view, minion) = seen[shown] ?: (null to null)
        Box(Modifier.fillMaxSize()) {
            when (view) {
                null -> EmptyNiche()

                is SlotView.Minion -> {
                    val live = fight.foes.firstOrNull { it.index == view.index } ?: minion
                    if (live != null) MinionNiche(live, fight, time, chosen == live.index, track(live.index)) { onTap(live.index) }
                }

                is SlotView.Totem -> {
                    val now = (slot as? SlotView.Totem)?.takeIf { it.serial == view.serial } ?: view
                    TotemNiche(now, time) { lore?.invoke(Lore.Totem(now.code, name, now.left)) }
                }
            }
            if (view != null) PortalBurst(if (view is SlotView.Totem) totemTint(view) else PortalTint)
        }
    }
}

/** Форма ниши: арка сверху, прямой низ. */
private val ArchShape = GenericShape { size, _ ->
    val r = size.width / 2
    moveTo(0f, size.height)
    lineTo(0f, r)
    arcTo(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.width), 180f, 180f, false)
    lineTo(size.width, size.height)
    close()
}

@Composable private fun EmptyNiche() {
    Canvas(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp)) {
        val outline = ArchShape.createOutline(Size(size.width, size.height - 12.dp.toPx()), layoutDirection, this)
        drawOutline(outline, Brush.radialGradient(listOf(Color(0x552A221A), Color.Transparent)))
        drawOutline(outline, Color(0xFF4A3E30), style = Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))
        drawCircle(Color(0xFF4A3E30), 3.dp.toPx(), Offset(size.width / 2, (size.height - 12.dp.toPx()) * .55f), style = Stroke(1.dp.toPx()))
    }
}

/** Каменная арка ниши с ромбом-замком сверху; [glow] - свечение тотема. */
private fun DrawScope.archFrame(glow: Color?) {
    val body = Size(size.width, size.height)
    val outline = ArchShape.createOutline(body, layoutDirection, this)
    glow?.let { drawOutline(outline, it.copy(alpha = .45f), style = Stroke(6.dp.toPx())) }
    drawOutline(outline, Brush.linearGradient(listOf(glow ?: StoneLight, StoneDark, Color(0xFF5A4A36), glow ?: StoneDark), Offset.Zero, Offset(size.width, size.height)))
    drawOutline(outline, Color(0x88D8C090), style = Stroke(1.dp.toPx()))
    val k = 5.dp.toPx()
    val gem = Path().apply {
        moveTo(size.width / 2, -k)
        lineTo(size.width / 2 + k, 0f)
        lineTo(size.width / 2, k)
        lineTo(size.width / 2 - k, 0f)
        close()
    }
    drawPath(gem, Brush.linearGradient(listOf(Color(0xFFF5E2B0), Color(0xFF9A7A3A)), Offset(size.width / 2 - k, -k), Offset(size.width / 2 + k, k)))
    drawPath(gem, Color(0xFF3A2A12), style = Stroke(1.dp.toPx()))
}

/** Табличка под нишей: имя и полоса - жизни у приспешника, срока у тотема. */
@Composable private fun BoxScope.Plaque(text: String, tint: Color, share: Float, bar: Color) {
    Box(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp).clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3A2E22), Color(0xFF1A140E)))).border(1.dp, Color(0xFF6A5636), RoundedCornerShape(3.dp)),
    ) {
        Text(text, color = tint, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(top = 1.dp, start = 2.dp, end = 2.dp))
        Box(Modifier.align(Alignment.BottomCenter).padding(horizontal = 4.dp, vertical = 3.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF3A1010))) {
            Box(Modifier.fillMaxWidth(share.coerceIn(0f, 1f)).fillMaxHeight().background(bar))
        }
    }
}

@Composable private fun MinionNiche(foe: FoeView, fight: FightHud, time: Float, focused: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val ring = rarityTint(foe.monster.rarity)
    val wash = foe.ailments.map { it.ailment }.maxByOrNull(::washAmount)
    Box(modifier.fillMaxSize().clickable(enabled = foe.alive && fight.outcome == null, onClick = onTap)) {
        Box(Modifier.fillMaxWidth().padding(bottom = 16.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) { archFrame(null) }
            Canvas(Modifier.fillMaxSize().padding(3.dp).clip(ArchShape)) {
                Portraits.monster(this, foe.monster.code.value, foe.monster.form, ring, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash(fight.lunge, Side.MONSTER, foe.index))
            }
            if (focused) Box(Modifier.matchParentSize().border(2.dp, FrameGoldBright, ArchShape))
            CardHits(fight.hits.filter { it.target == Side.MONSTER && it.foe == foe.index })
        }
        Plaque(monsterTitle(foe.monster.code), Parchment, if (foe.maxLife > 0) foe.life / foe.maxLife.toFloat() else 0f, LifeMid)
    }
}

@Composable private fun TotemNiche(totem: SlotView.Totem, time: Float, onTap: () -> Unit) {
    val tint = totemTint(totem)
    Box(Modifier.fillMaxSize().clickable(onClick = onTap)) {
        Box(Modifier.fillMaxWidth().padding(bottom = 16.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) { archFrame(tint) }
            Canvas(Modifier.fillMaxSize().padding(3.dp).clip(ArchShape).background(Brush.radialGradient(listOf(tint.copy(alpha = .3f), Color(0xFF0A0807))))) {
                totemFigure(tint, time, totem.kind)
            }
        }
        Plaque("${totemTitle(totem.code)} · ${totem.left.roundToInt()} ${ui("boss.seconds_short")}", tint, totem.share, tint)
    }
}

/** Тотем рисунком: основание, столб с перехватами, навершие, глаза его цвета и знак рода; чуть покачивается. */
private fun DrawScope.totemFigure(tint: Color, time: Float, kind: TotemKind) {
    val w = size.width
    val h = size.height
    val bob = sin(time * 2.6f) * h * .015f
    val cx = w / 2
    val pillar = Size(w * .36f, h * .62f)
    val top = h * .2f + bob
    drawRect(Color(0xFF2A2018), Offset(cx - w * .3f, h * .86f), Size(w * .6f, h * .1f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF5A4630), Color(0xFF2A2016))), Offset(cx - pillar.width / 2, top), pillar)
    drawRect(Color(0xFF8A6A44), Offset(cx - pillar.width / 2, top), pillar, style = Stroke(1.dp.toPx()))
    val cap = Path().apply {
        moveTo(cx - w * .34f, top + h * .06f)
        quadraticTo(cx, top - h * .14f, cx + w * .34f, top + h * .06f)
        lineTo(cx + w * .24f, top + h * .1f)
        quadraticTo(cx, top - h * .04f, cx - w * .24f, top + h * .1f)
        close()
    }
    drawPath(cap, Color(0xFF6A5236))
    drawPath(cap, Color(0xFFA8865A), style = Stroke(1.dp.toPx()))
    listOf(.38f, .66f).forEach { y -> drawRect(Color(0xFF1A140E), Offset(cx - pillar.width / 2, top + pillar.height * y), Size(pillar.width, 2.dp.toPx())) }
    val glow = .7f + .3f * sin(time * 4f)
    listOf(-1, 1).forEach { side ->
        val eye = Offset(cx + side * pillar.width * .22f, top + pillar.height * .24f)
        drawCircle(tint.copy(alpha = .35f * glow), 6.dp.toPx(), eye)
        drawCircle(tint, 2.4.dp.toPx(), eye)
    }
    drawLine(tint, Offset(cx - pillar.width * .22f, top + pillar.height * .32f), Offset(cx + pillar.width * .22f, top + pillar.height * .32f), 1.5.dp.toPx())
    val mark = Offset(cx, top + pillar.height * .52f)
    val r = pillar.width * .22f
    when (kind) {
        TotemKind.POWER -> drawPath(
            Path().apply {
                moveTo(mark.x, mark.y - r)
                lineTo(mark.x + r, mark.y + r)
                lineTo(mark.x - r, mark.y + r)
                close()
            },
            tint,
        )

        TotemKind.CURSE -> drawCircle(tint, r * .8f, mark, style = Stroke(1.6.dp.toPx()))

        TotemKind.RETINUE -> drawRect(tint, Offset(mark.x - r * .7f, mark.y - r), Size(r * 1.4f, r * 1.4f), style = Stroke(1.6.dp.toPx()))

        TotemKind.ELEMENT -> repeat(3) { k -> drawLine(tint, Offset(mark.x - r + k * r, mark.y - r), Offset(mark.x - r * .5f + k * r, mark.y + r), 1.6.dp.toPx()) }
    }
}

/** Портал, из которого встаёт жилец ниши: кольцо раскручивается, вспыхивает и гаснет. */
@Composable private fun PortalBurst(tint: Color) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(1100, easing = LinearEasing)) }
    val t = anim.value
    if (t >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val grow = if (t < .35f) t / .35f * 1.15f else 1.15f - (t - .35f) / .65f * .95f
        val r = size.minDimension * .42f * grow
        val c = Offset(size.width / 2, size.height * .42f)
        val fade = if (t < .75f) 1f else 1 - (t - .75f) / .25f
        drawCircle(tint.copy(alpha = .25f * fade), r * 1.25f, c)
        drawArc(tint.copy(alpha = fade), t * 600f, 260f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawArc(Color.White.copy(alpha = .7f * fade), t * 600f + 180f, 90f, false, Offset(c.x - r * .8f, c.y - r * .8f), Size(r * 1.6f, r * 1.6f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/**
 * Портрет босса: двойная золотая рамка, аура цвета фазы дышит за ним, удар - вспышкой, крит - коротким толчком (без него при
 * упрощённых эффектах); перед готовым умением вокруг пульсирует фиолетовый круг, новая фаза - титулом поверх, павший босс
 * растворяется искрами.
 */
@Composable private fun PortraitStage(foe: FoeView, boss: BossHud, fight: FightHud, time: Float, open: Boolean, modifier: Modifier, onTap: () -> Unit) {
    val settings = LocalSettings.current
    val ring = rarityTint(foe.monster.rarity)
    val wash = foe.ailments.map { it.ailment }.maxByOrNull(::washAmount)
    val aura = phaseAura(phaseNumber(boss))
    val focused = fight.focus == foe.index
    val gone by animateFloatAsState(if (foe.alive) 0f else 1f, tween(1400, easing = LinearEasing), label = "dissolve")
    val crit = fight.lunge?.takeIf { it.actor == Side.HERO && it.landed && it.foe == foe.index && it.kind == HitKind.CRIT }?.progress ?: 1f
    val nudge = if (settings.simpleEffects || crit >= 1f) 0f else sin(crit * PI.toFloat() * 6) * (1 - crit) * 2.5f
    val shape = RoundedCornerShape(12.dp)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val breath = .55f + .2f * sin(time * 2f)
            drawOval(Brush.radialGradient(listOf(aura.copy(alpha = .45f * breath * (1 - gone)), Color.Transparent), center, size.maxDimension * .62f), Offset(-size.width * .15f, -size.height * .08f), Size(size.width * 1.3f, size.height * 1.16f))
        }
        Box(
            Modifier.fillMaxSize().padding(horizontal = 4.dp).graphicsLayer {
                translationX = nudge.dp.toPx()
                alpha = 1 - gone
                scaleX = 1 + gone * .06f
                scaleY = 1 + gone * .06f
            }.clip(shape).background(Color(0xFF1A0F0E))
                .border(if (focused || open) 2.dp else 1.5.dp, if (focused || open) FrameGoldBright else FrameGold.copy(alpha = .8f), shape)
                .padding(3.dp).border(1.dp, Color(0xFF6A5226), RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                .clickable(enabled = foe.alive && fight.outcome == null, onClick = onTap),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                Portraits.monster(this, foe.monster.code.value, foe.monster.form, ring, time, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash(fight.lunge, Side.MONSTER, foe.index))
            }
            CardHits(fight.hits.filter { it.target == Side.MONSTER && it.foe == foe.index })
        }
        boss.cast?.takeIf { foe.alive && it.left < TELEGRAPH_SECONDS }?.let { cast -> Telegraph((1 - cast.left / TELEGRAPH_SECONDS).toFloat(), time) }
        if (gone > 0f && gone < 1f) Sparks(gone, FrameGoldBright)
        PhaseTitle(fight, boss)
    }
}

/** Цвет ауры фазы: тёплое золото, затем кровь, затем фиолетовая тьма. */
internal fun phaseAura(phase: Int): Color = when (phase) {
    1 -> Color(0xFFFF9A4A)
    2 -> Color(0xFFE0402A)
    else -> Color(0xFFA040E0)
}

/** Круг-предупреждение: сжимается к портрету и пульсирует, пока умение не готово. */
@Composable private fun Telegraph(progress: Float, time: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension * (.78f - .22f * progress)
        val pulse = .5f + .5f * sin(time * 12f)
        drawCircle(CastTint.copy(alpha = .18f + .2f * progress), r, center, style = Stroke(10.dp.toPx()))
        drawCircle(CastTint.copy(alpha = .55f + .4f * pulse * progress), r, center, style = Stroke(2.dp.toPx()))
        repeat(8) { k ->
            val a = k * PI.toFloat() / 4 + time * 1.4f
            drawCircle(Color(0xFFE0C8FF).copy(alpha = .8f * progress), 2.dp.toPx(), center + Offset(cos(a) * r, sin(a) * r))
        }
    }
}

/** Искры растворения: поднимаются и гаснут по мере [t]. */
@Composable private fun Sparks(t: Float, tint: Color) {
    Canvas(Modifier.fillMaxSize()) {
        repeat(26) { k ->
            val seed = (k * 73 % 97) / 97f
            val x = size.width * (.15f + .7f * seed)
            val y = size.height * (.85f - t * (.5f + .5f * ((k * 31 % 89) / 89f)))
            drawCircle(tint.copy(alpha = (1 - t) * .9f), (1.5f + 2f * seed).dp.toPx(), Offset(x + sin(t * 9f + k) * 6.dp.toPx(), y))
        }
    }
}

/** Титул новой фазы поверх портрета: «Фаза II» и её имя, нарастает и гаснет. */
@Composable private fun PhaseTitle(fight: FightHud, boss: BossHud) {
    val now = fight.events.firstOrNull()?.time ?: return
    val event = fight.events.firstOrNull { it.foe == boss.index && it.actor == Side.MONSTER && (it.trace as? NoteTrace)?.kind == NoteKind.PHASE } ?: return
    val age = now - event.time
    if (age > PHASE_TITLE_SECONDS) return
    val k = (age / PHASE_TITLE_SECONDS).toFloat()
    val alpha = when {
        k < .15f -> k / .15f
        k > .7f -> (1 - k) / .3f
        else -> 1f
    }
    val note = event.trace as NoteTrace
    Column(
        Modifier.alpha(alpha).graphicsLayer {
            scaleX = .9f + .1f * k.coerceAtMost(.3f) / .3f
            scaleY = scaleX
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(ui("boss.phase_number", roman(phaseNumber(boss))), color = FrameGoldBright, fontFamily = FontFamily.Serif, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Text(phaseTitle(note.ref), color = Parchment, fontFamily = FontFamily.Serif, fontSize = 13.sp, letterSpacing = 2.sp)
    }
}

/** Свита сбоку без слотов: узкие карточки столбиком. */
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
            key(foe.index) {
                FoeCard(foe, fight, time, chosen == foe.index && fight.scouting, track(foe.index).fillMaxWidth(), large, traits[foe.index].orEmpty(), narrow = true) { onFocus(foe.index) }
            }
        }
    }
}

// ============================================================ Стражи Разлома (3.96.0)

/** Палитра Разлома (RULES.md, «Стражи Разлома»): панель, черта, ядовитая зелень, текст, тревога. */
private val RiftPanel = Color(0xFF0E1812)
private val RiftPanelDeep = Color(0xFF0A120D)
private val RiftLine = Color(0xFF1F3427)
private val RiftLineBright = Color(0xFF2F5A3F)
private val RiftGreen = Color(0xFF39FF88)

/** Печать Разлома на запертом слоте героя - та же ядовитая зелень. */
internal val RiftSeal = RiftGreen
private val RiftSoft = Color(0xFF9DFFB8)
private val RiftHot = Color(0xFFD4FF6A)
private val RiftDeep = Color(0xFF0B3A21)
private val RiftText = Color(0xFFDBE8DC)
private val RiftMuted = Color(0xFF7F9686)
private val RiftDim = Color(0xFF4B5E51)
private val RiftAlarm = Color(0xFFFF6B4A)

/** Сколько длится разгорание печати и её раскол, мс. */
private const val SEAL_GROW_MS = 2400
private const val SEAL_BREAK_MS = 700

/** Пульс печатей и мигание тревоги знамения, секунды периода. */
private const val SEAL_PULSE_SECONDS = 2.4f
private const val ALARM_SECONDS = .5f

/** Сколько въезжает новая плашка украденного дара, мс. */
private const val CHIP_MS = 500

/** Полоса механики стража Разлома под здоровьем: печати Стража, Законы Владыки или украденное Поглотителем. */
@Composable private fun RiftStrip(boss: BossHud, time: Float) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(RiftPanel, RiftPanelDeep))).border(1.dp, RiftLine, shape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (boss.rift) {
            RiftGuard.WARDEN -> Seals(boss, time)
            RiftGuard.DEVOURER -> Stolen(boss)
            RiftGuard.LORD -> Laws(boss, time)
            null -> Unit
        }
    }
}

/** Подпись полосы Разлома: заглавные серифы, мягкая зелень. */
@Composable private fun RiftCaption(text: String, modifier: Modifier = Modifier, tint: Color = RiftSoft) {
    Text(text, color = tint, fontFamily = FontFamily.Serif, fontSize = 10.sp, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

/** Печати брони: ромб на печать - горит, пока стоит, гаснет с расколом и разгорается вновь; ниже - попадания до следующей. */
@Composable private fun Seals(boss: BossHud, time: Float) {
    val motion = LocalMotion.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RiftCaption(ui("boss.rift_seals", boss.seals, boss.sealsMax).uppercase(), Modifier.weight(1f, fill = false))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(boss.sealsMax) { i -> SealPip(i < boss.seals, if (motion) .75f + .25f * sin(time * 2 * PI.toFloat() / SEAL_PULSE_SECONDS + i) else 1f) }
        }
    }
    if (boss.sealEvery > 0 && boss.seals > 0) {
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(RiftDeep)) {
            Box(Modifier.fillMaxWidth((boss.sealHits / boss.sealEvery.toFloat()).coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(RiftGreen.copy(alpha = .6f), RiftHot))))
        }
    }
}

/** Ромб печати: [lit] - стоит; [pulse] - дыхание свечения. Раскол - быстрая вспышка, возврат - медленное разгорание. */
@Composable private fun SealPip(lit: Boolean, pulse: Float) {
    val glow by animateFloatAsState(if (lit) 1f else 0f, tween(if (lit) SEAL_GROW_MS else SEAL_BREAK_MS, easing = FastOutSlowInEasing), label = "seal")
    Canvas(Modifier.size(12.dp)) {
        val c = center
        val r = size.minDimension / 2
        val gem = Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + r * .72f, c.y)
            lineTo(c.x, c.y + r)
            lineTo(c.x - r * .72f, c.y)
            close()
        }
        if (glow > 0f) drawCircle(RiftGreen.copy(alpha = .45f * glow * pulse), r * 1.1f, c)
        drawPath(gem, Brush.linearGradient(listOf(RiftHot.copy(alpha = glow), RiftGreen.copy(alpha = .25f + .75f * glow), RiftDeep), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)))
        drawPath(gem, if (glow > .5f) RiftSoft else RiftLineBright, style = Stroke(1.dp.toPx()))
        // Снятая печать - трещина поперёк ромба
        if (glow < 1f) drawLine(RiftDim.copy(alpha = 1 - glow), Offset(c.x - r * .4f, c.y - r * .3f), Offset(c.x + r * .35f, c.y + r * .4f), 1.dp.toPx())
    }
}

/** Название Закона по словарю сервера; «Одна стихия» - с её стихией. */
private fun lawName(law: RiftLaw, element: String?): String {
    val name = locOr("rift.law.${law.name}.name", law.name)
    return if (law == RiftLaw.ONE_ELEMENT && element != null) ui("boss.rift_law_element", name, ui("enum.damage.$element")) else name
}

/** Законы Владыки: скрижаль с Законами в силе; в знамение - тревога, отсчёт и имя следующего. */
@Composable private fun Laws(boss: BossHud, time: Float) {
    val motion = LocalMotion.current
    val omen = boss.lawNext.isNotEmpty()
    val flash = when {
        !omen -> 0f
        motion -> if ((time / ALARM_SECONDS).toInt() % 2 == 0) 1f else .35f
        else -> 1f
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RiftCaption(ui("boss.rift_law").uppercase(), tint = RiftMuted)
        val now = boss.law.joinToString(" · ") { lawName(it, boss.lawElement) }.ifEmpty { ui("boss.rift_law_none") }
        Text(
            now.uppercase(),
            color = if (boss.law.isEmpty()) RiftDim else RiftGreen,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
    val seconds = ceil(boss.lawIn).toInt()
    if (omen) {
        val shape = RoundedCornerShape(4.dp)
        Text(
            ui("boss.rift_law_omen", seconds, boss.lawNext.joinToString(" · ") { lawName(it, boss.lawNextElement) }),
            color = RiftAlarm.copy(alpha = .55f + .45f * flash),
            fontFamily = FontFamily.Serif,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().clip(shape).background(RiftAlarm.copy(alpha = .12f * flash)).border(1.dp, RiftAlarm.copy(alpha = .7f * flash), shape)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    } else {
        RiftCaption(ui("boss.rift_law_in", seconds), tint = RiftMuted)
    }
}

/** Поглотитель: его сила от Эха, счёт украденных даров и их плашки - новая въезжает слева. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Stolen(boss: BossHud) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RiftCaption(ui("boss.rift_stolen", boss.stolen.size).uppercase(), Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(1f))
        if (boss.devoured > 0) RiftCaption(ui("boss.rift_devoured", number(boss.devoured)), tint = RiftHot)
    }
    if (boss.stolen.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            boss.stolen.forEach { code -> key(code) { StolenChip(code) } }
        }
    }
}

@Composable private fun StolenChip(code: String) {
    val arrive = remember { Animatable(0f) }
    LaunchedEffect(Unit) { arrive.animateTo(1f, tween(CHIP_MS, easing = FastOutSlowInEasing)) }
    val shape = RoundedCornerShape(4.dp)
    Text(
        locOr("rift.boon.$code.name", code),
        color = RiftText,
        fontFamily = FontFamily.Serif,
        fontSize = 9.sp,
        maxLines = 1,
        modifier = Modifier.graphicsLayer {
            translationX = (1 - arrive.value) * -16.dp.toPx()
            alpha = arrive.value
        }.clip(shape).background(RiftDeep).border(1.dp, RiftLineBright, shape).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
