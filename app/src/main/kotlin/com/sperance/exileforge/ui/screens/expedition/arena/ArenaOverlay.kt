package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

/** The key a card's bounds are kept under: the hero's, and each foe's by its place in the pack. */
private const val HERO_CARD = -1

/**
 * The fight as cards (2.70.0, the owner's mockup B «карточки против карточек»): the foes on screen across the
 * top in one line — any of them is in reach of any weapon — the hero's card at the foot, and between them
 * either the scouting panel or the latest blows. Whoever swings is lifted toward the other side and
 * lit — gold for the hero, blood for a foe — and a line runs from them to whom they struck. A tap on
 * a foe singles it out as the hero's target; the same tap again gives the choice back to the class.
 *
 * Before «В бой», and whenever paused, nothing moves: the tapped foe — or the one the hero would
 * strike — is laid open, its numbers held against the hero's.
 */
@Composable internal fun ArenaOverlay(
    game: GameUi,
    hud: RunHud,
    fight: FightHud,
    level: Int,
    rules: CombatRules,
    stance: HeroStance,
    onCommand: (RunCommand) -> Unit,
    onLogFilter: (Set<LogKind>) -> Unit = {},
    onBuzz: (Buzz) -> Unit = {},
) {
    val time by rememberClock()
    // The settings' pause and buzz (3.77.0): each once as the hero's life falls through its line, the buzz again at a fall.
    val settings = LocalSettings.current
    val share = if (hud.heroMaxLife > 0) fight.heroLife / hud.heroMaxLife.toFloat() else 1f
    val low = share * 100 < settings.autoPause
    val danger = share < DANGER_SHARE
    LaunchedEffect(low) { if (low && fight.started && !fight.paused && fight.outcome == null) onCommand(RunCommand.Pause) }
    LaunchedEffect(danger) { if (danger && fight.outcome == null) onBuzz(Buzz.DANGER) }
    LaunchedEffect(fight.outcome) { if (fight.outcome == Outcome.LOSS) onBuzz(Buzz.DANGER) }
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val names = remember(fight.foes.size, fight.leader, fight.stage) { fight.foes.associate { it.index to monsterTitle(it.monster.code) } }
    // Each foe's traits (3.73.0): seals on its card, their whole text in its window.
    val traits = remember(fight.foes.size, fight.leader, fight.stage, game.index) {
        fight.foes.associate { foe -> foe.index to (game.index?.let { traitViews(foe.monster, it) } ?: emptyList()) }
    }
    val chosen = fight.focus ?: fight.target ?: fight.field.firstOrNull { it.alive }?.index
    // The tiles are larger while the fight stands still; a tap on any of them opens its window at any time (2.73.0).
    val large = fight.scouting
    // The skill whose page is open over the fight (3.24.0); the fight holds still while it is read.
    var info by remember { mutableStateOf<SkillView?>(null) }
    fun track(key: Int) = Modifier.onGloballyPositioned { bounds[key] = it.boundsInRoot() }
    Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PackHeader(fight, level)
            if (fight.field.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    // Keyed by the foe: one stepping into a fallen one's place is a card of its own.
                    fight.field.forEach { foe ->
                        key(foe.index) {
                            FoeCard(
                                foe,
                                fight,
                                time,
                                chosen == foe.index && fight.scouting,
                                track(foe.index).weight(1f).widthIn(max = 180.dp),
                                large,
                                traits[foe.index].orEmpty(),
                            ) {
                                onCommand(RunCommand.Focus(foe.index))
                            }
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val shown = fight.field.firstOrNull { it.index == chosen }
                if (fight.scouting && shown != null) {
                    ScoutPanel(
                        shown,
                        fight,
                        shown.monster.level.takeIf { it > 0 } ?: level,
                        rules,
                        stance,
                        game.index,
                        traits[shown.index].orEmpty(),
                    )
                } else {
                    FightFeed(game, fight, names, onCommand, onLogFilter)
                }
            }
            HeroCard(game, hud, fight, time, names, stance, track(HERO_CARD), large)
            // The skills and the belt (2.78.0): under the hero, over the fight's own controls.
            if (fight.skills.any { it != null } || fight.flasks.any { it != null }) ActionBar(fight, onCommand) { info = it }
            Controls(fight, hud.auto, onCommand)
        }
        StrikeLine(fight.lunge, bounds, origin)
        info?.let { view -> FightSkillSheet(game, view, onCommand) { info = null } }
        // A win says so in the rewards window itself (2.73.0); only a loss or a retreat is announced here.
        fight.outcome?.takeIf { it != Outcome.WIN }?.let {
            Text(
                ui("expedition.outcome_${it.name.lowercase()}"),
                color = outcomeColour(it),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center).background(Ink.copy(alpha = .8f), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 6.dp),
            )
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

/** The life share under which the phone warns of danger (3.77.0). */
private const val DANGER_SHARE = .3f
