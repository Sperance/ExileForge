package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.*
import kotlin.math.sin

/**
 * The hero's card at the foot: portrait, name and class, life with the shield over it, the swing,
 * every state on them, and whom the next blow goes to — the player's pick or the class's rule.
 * Lifted and lit in gold while they swing; ringed in blood while struck.
 *
 * Эффекты: под портретом - кнопка «≡ N» ([LastingButton]) - лист эффектов «на весь бой» ([LastingSheet]: ауры и пассивные умения,
 * силы уникалок с именем вещи, носимые баффы, строки карты [mods], дары ✦); внизу карточки во всю ширину - плитка итога здоровья
 * в секунду первой и временная полка (оглушение, недуги, глотки, проклятия, заряды, силы-события). Касание полосы открывает лист
 * временных эффектов с итогом здоровья.
 *
 * Замах (последние 30% - рамка золотом), выпады и тряска - в рисовании кадра ([FightFrame]). Запас, что ауры держат целиком, - полной
 * штрихованной полосой.
 */
@Composable internal fun HeroCard(
    game: GameUi,
    hud: RunHud,
    fight: FightHud,
    frame: FightFrame,
    names: Map<Int, String>,
    stance: HeroStance,
    modifier: Modifier,
    large: Boolean,
    mods: List<HeroMapMod> = emptyList(),
    onCommand: (RunCommand) -> Unit = {},
) {
    val hero = game.heroInfo
    var effectsOpen by remember { mutableStateOf(false) }
    var lastingOpen by remember { mutableStateOf(false) }
    val live = fight.outcome == null

    fun swing() = if (live) frame.heroSwing(fight) else 0f
    fun acting() = strikeReach(fight, frame.now(fight), Side.HERO, null, swing = swing(), held = fight.heroHeld)
    fun flash() = strikeFlash(fight, frame.now(fight), Side.HERO, null)
    val shape = RoundedCornerShape(10.dp)
    val wash = fight.heroAilments.map { it.ailment }.maxByOrNull(::washAmount)
    val holder: (String) -> String? = { id -> game.hero?.item(id)?.template?.let(::equipmentTitle) }
    val timed = timedShelf(fight.heroHeld, fight.heroAilments, fight.heroEffects, fight.heroCharges, fight.heroPowers, holder)
    val lasting = lastingShelf(fight, mods, holder)
    val openEffects = { effectsOpen = true }
    Column(
        modifier.fillMaxWidth().graphicsLayer {
            translationY = -acting() * 10.dp.toPx()
            translationX = shake(flash(), frame.time).dp.toPx()
        }
            .drawWithContent {
                val acting = acting()
                val hit = flash() > 0f
                val glow = if (live) swingGlow(swing(), fight.heroHeld) else 0f
                val outline = shape.createOutline(size, layoutDirection, this)
                swingAura(outline, Gold, glow)
                drawOutline(outline, if (acting > 0f) PanelRaised else HeroGround)
                if (fight.heroProvoke != null && acting == 0f && !hit) tauntAura(outline, frame.time)
                drawContent()
                val border = when {
                    hit -> LifeRed
                    acting > 0f -> GoldBright
                    else -> lerp(HeroRim, GoldBright, glow)
                }
                roundEdge(10.dp, if (acting > 0f || hit || glow > 0f) 2.dp else 1.dp, border)
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.width(PORTRAIT), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(.75f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF0B0E13))) {
                    Canvas(Modifier.fillMaxSize()) {
                        Portraits.hero(this, game.heroClass?.code, frame.breath, wash?.let(::ailmentTint), wash?.let(::washAmount) ?: 0f, flash())
                    }
                    CardHits(fight, frame, fight.hits.filter { it.target == Side.HERO })
                }
                if (lasting.isNotEmpty()) LastingButton(lasting.size, Modifier.fillMaxWidth()) { lastingOpen = true }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        listOfNotNull(hero?.name?.takeIf { it.isNotBlank() }, ui("expedition.hero_line", hero?.heroClass?.let(::classTitle).orEmpty(), game.heroLevel)).joinToString(" · "),
                        color = GoldBright,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    fight.heroProvoke?.let { provoke -> TauntSeal(frame, Modifier.size(22.dp)) { tauntTip(provoke) } }
                }
                if (fight.heroMaxShield > 0) VitalBar(fight.heroShield, fight.heroMaxShield, ShieldCyan, Modifier.fillMaxWidth().height(16.dp))
                VitalBar(fight.heroLife, fight.heroMaxLife, LifeRed, Modifier.fillMaxWidth().height(18.dp), ring = GoldBright.takeIf { fight.heroBarrier > 0 }, reserved = hud.heroLifeReserved)
                if (fight.heroMaxMana + fight.heroReserved.coerceAtLeast(0) > 0) VitalBar(fight.heroMana, fight.heroMaxMana, ManaBlue, Modifier.fillMaxWidth().height(16.dp), reserved = fight.heroReserved)
                fight.heroBuildup?.let { BuildupBar(it, Modifier.fillMaxWidth()) }
                SwingBar({ swing() }, fight.heroHeld, Modifier.fillMaxWidth())
                val target = fight.target?.let(names::get)
                if (target != null && live) {
                    Text(
                        ui("fight.target_line", target, if (fight.focus != null) ui("fight.target_yours") else ui("fight.rule.${stance.rule.name}")),
                        color = if (fight.focus != null) GoldBright else Parchment,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(30.dp).clickable(role = Role.Button, onClick = openEffects),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FlowTile(fight.heroFlow, Modifier.size(30.dp))
            if (timed.isEmpty()) {
                MutedText(ui("fight.no_states"), style = MaterialTheme.typography.labelSmall)
            } else {
                EffectShelf(timed, 30.dp, openEffects, Modifier.weight(1f))
            }
        }
    }
    if (effectsOpen) EffectsSheet(ui("fight.shelf.hero"), timed, emptyList(), fight.heroFlow) { effectsOpen = false }
    if (lastingOpen) LastingSheet(lasting) { lastingOpen = false }
}

/**
 * Кнопка «≡ N» под портретом героя: N - эффектов на весь бой; касание - их лист ([LastingSheet]), бой идёт. Ширина портрета, зона
 * касания - 44dp ([ForgeButton]).
 */
@Composable private fun LastingButton(count: Int, modifier: Modifier, onClick: () -> Unit) {
    val label = ui("fight.lasting.button", count)
    ForgeButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        kind = ButtonKind.Secondary,
        size = ButtonSize.Small,
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text("≡ $count", maxLines = 1)
    }
}

/** Мини-кнопка «все эффекты»: «≡» и сколько эффектов на герое; касание - лист всех эффектов, бой идёт. */
@Composable internal fun AllEffects(count: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val label = ui("fight.shelf.all", count)
    Column(
        modifier.clip(shape).background(Vital.copy(alpha = .08f), shape).border(1.dp, Vital.copy(alpha = .45f), shape).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("≡", color = Vital, fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold)
        Text(count.toString(), color = Vital, fontSize = 9.sp, lineHeight = 9.sp)
    }
}

/** Ширина портрета героя и кнопки «≡ N» под ним. */
private val PORTRAIT = 64.dp

/** Подложка и рамка карточки героя вне выпада. */
private val HeroGround = Panel.copy(alpha = .94f)
private val HeroRim = Gold.copy(alpha = .6f)
