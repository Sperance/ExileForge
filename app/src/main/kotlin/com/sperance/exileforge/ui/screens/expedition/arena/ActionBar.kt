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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.screens.skills.FlaskBottle
import com.sperance.exileforge.ui.screens.skills.SkillCard
import com.sperance.exileforge.ui.theme.*

/**
 * The hero's skills and belt in the fight: фляги слева, умения справа - каждое квадратной
 * иконкой «Неон рун» ([NeonSkillIcon]) своего тира: откат - тёмный сектор и секунды, нехватка маны - синяя подложка, пустой
 * слот - пунктирный квадрат того же размера. A tap uses a skill or drinks a flask at once, whatever its condition; a long press
 * on a skill, or its «i», opens its page.
 *
 * Четыре слота умений с тремя флягами - плитки меньше, чтобы ряд влез в узкий экран.
 */
@Composable internal fun ActionBar(game: GameUi, fight: FightHud, frame: FightFrame, onCommand: (RunCommand) -> Unit, onInfo: (SkillView) -> Unit) {
    val live = fight.started && fight.outcome == null
    val index = game.index
    val skills = game.hero?.skills ?: HeroSkills()
    val stats = fight.heroBody?.stats ?: game.hero?.stats.orEmpty()
    val compact = fight.skills.size > COMPACT_FROM
    val side = if (compact) COMPACT_SIDE else SKILL_SIDE
    val flask = if (compact) COMPACT_FLASK else FLASK_SIDE
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
        fight.flasks.forEach { view ->
            if (view == null) {
                Box(Modifier.size(flask).border(1.dp, Bronze.copy(alpha = .35f), CircleShape))
            } else {
                FlaskGain(view.charges, flaskTint(view.kind)) { FlaskButton(view, live, flask) { onCommand(RunCommand.Drink(view.slot)) } }
            }
        }
        Spacer(Modifier.weight(1f))
        fight.skills.forEach { view ->
            if (view == null) {
                EmptySkillSlot(side)
            } else {
                val skill = index?.skills?.byCode?.get(view.code)
                val tier = remember(skill, skills, stats) { skill?.let { index?.skillGrowth?.tier(it, skills, stats) } ?: HeroSkills.FIRST_TIER }
                SkillButton(view, skill, tier, live, side, { view.readyAt(fight, frame.now(fight)) }, onInfo = { onInfo(view) }) { onCommand(RunCommand.Cast(view.slot)) }
            }
        }
    }
}

/** Сторона квадрата умения в бою и круга фляги; при четырёх слотах - меньше. */
private val SKILL_SIDE = 52.dp
private val FLASK_SIDE = 44.dp
private val COMPACT_SIDE = 44.dp
private val COMPACT_FLASK = 40.dp
private const val COMPACT_FROM = 3

/** Скругление плитки умения - как у [NeonSkillIcon]: четверть стороны (у малой плитки разница меньше точки). */
private val SkillShape = RoundedCornerShape(SKILL_SIDE / 4)

/** Пустой слот умения: пунктирный квадрат размера иконки. */
@Composable private fun EmptySkillSlot(side: Dp) {
    Canvas(Modifier.size(side)) {
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

/**
 * Нехватка маны - синяя подложка; откат - тёмный сектор по часовой от верха и секунды, доля - к часам кадра; полоса применения растёт по
 * низу плитки.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillButton(view: SkillView, skill: SkillDefinition?, tier: Int, live: Boolean, side: Dp, readyNow: () -> Float, onInfo: () -> Unit, onTap: () -> Unit) {
    val ready = view.ready >= 1f && view.locked <= 0.0
    Box(
        Modifier.size(side)
            .combinedClickable(onLongClick = onInfo) { if (live && ready && view.affordable) onTap() }
            .semantics { contentDescription = SkillText.title(view.code) },
    ) {
        if (skill != null) {
            NeonSkillIcon(skill, tier, side)
        } else {
            SkillGlyph(view.icon, Modifier.size(26.dp).align(Alignment.Center), GoldBright)
        }
        if (!view.affordable) Box(Modifier.matchParentSize().clip(SkillShape).background(ManaBlue.copy(alpha = .28f)))
        if (!ready && view.locked <= 0.0) {
            Canvas(Modifier.matchParentSize().clip(SkillShape)) {
                val share = readyNow()
                val reach = size.maxDimension * 1.5f
                drawArc(
                    Color.Black.copy(alpha = .62f),
                    -90f + 360f * share,
                    360f * (1 - share),
                    useCenter = true,
                    topLeft = Offset(center.x - reach / 2, center.y - reach / 2),
                    size = Size(reach, reach),
                )
            }
            Text(fineNumber(view.seconds), color = Parchment, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }
        if (view.locked > 0.0) SealedSlot(view.locked, SkillShape)
        if (view.casting > 0f) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).clip(SkillShape).background(Color.Black.copy(alpha = .5f))) {
                Box(Modifier.fillMaxWidth(view.casting).fillMaxHeight().background(GoldBright))
            }
        }
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
 * A skill's page over the fight: what the grimoire says of it at its level — damage, cost, cooldown,
 * cast time and effects — with the condition its slot fires on. The fight holds while the page is open.
 *
 * Та же карточка умения, что везде: уровень и условие - ячейки боя.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FightSkillSheet(game: GameUi, view: SkillView, onCommand: (RunCommand) -> Unit, onDismiss: () -> Unit) {
    DisposableEffect(view.slot) {
        onCommand(RunCommand.Hold(true))
        onDispose { onCommand(RunCommand.Hold(false)) }
    }
    val index = game.index
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (index != null) SkillCard(game, index, view.code, view.level, view.condition) else MutedText(ui("common.loading"))
        }
    }
}

/** Фляга пояса в бою; погашенная из-за глотка впустую - касание называет полные полосы ([flaskTap]). */
@Composable private fun FlaskButton(view: FlaskView, live: Boolean, side: Dp, onTap: () -> Unit) {
    val tint = flaskTint(view.kind)
    val why = remember { mutableStateOf<String?>(null) }
    Box(
        Modifier.size(side).clip(CircleShape).background(Color(0xE60A0D12))
            .border(if (view.active > 0f) 2.dp else 1.dp, if (view.active > 0f) GoldBright else Bronze, CircleShape)
            .flaskTap(view, why, live, onTap),
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
        FlaskWhy(why)
    }
}

/**
 * Касание фляги [view], пока [live]: годная пьётся ([onDrink]), погашенная из-за глотка впустую кладёт в [why] слова её полных полос -
 * их показывает [FlaskWhy].
 */
internal fun Modifier.flaskTap(view: FlaskView, why: MutableState<String?>, live: Boolean = true, onDrink: () -> Unit): Modifier = clickable(enabled = live && (view.usable || view.wasted)) { if (view.usable) onDrink() else why.value = view.wastedWhy }

/** Подсказка погашенной фляги - почему её глоток впустую ([flaskTap]); касание её прячет. */
@Composable internal fun FlaskWhy(why: MutableState<String?>) {
    why.value?.let { TipCallout(Tip(body = it, tint = Muted)) { why.value = null } }
}

/** Почему глоток фляги сейчас впустую: её полные полосы по именам - слова подсказки у погашенной плитки. */
private val FlaskView.wastedWhy: String get() = fullBars.joinToString(" · ") { ui(it.fullKey) }

/** Слот, запертый «Запечатыванием» Стража Врат: тёмная печать Разлома и сколько ещё секунд. */
@Composable private fun BoxScope.SealedSlot(seconds: Double, shape: Shape) {
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .65f), shape).border(1.5.dp, RiftSeal, shape), contentAlignment = Alignment.Center) {
        Text("◈ ${fineNumber(seconds)}", color = RiftSeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
