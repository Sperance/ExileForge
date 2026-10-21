package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.*
import com.sperance.exileforge.core.campaign.combat.*
import com.sperance.exileforge.core.campaign.run.*
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Buzz
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.sheet.MightExtras
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.theme.*

/** The key a card's bounds are kept under: the hero's, and each foe's by its place in the pack. */
private const val HERO_CARD = -1

/**
 * The fight as cards (the owner's mockup B «карточки против карточек»): the foes on screen across the
 * top in one line — any of them is in reach of any weapon — the hero's card at the foot, and between them
 * either the scouting panel or the latest blows. Whoever swings is lifted toward the other side and
 * lit — gold for the hero, blood for a foe — and a line runs from them to whom they struck. A tap on
 * a foe singles it out as the hero's target; the same tap again gives the choice back to the class.
 *
 * Before «В бой», and whenever paused, nothing moves: the tapped foe — or the one the hero would
 * strike — is laid open, its numbers held against the hero's.
 *
 * [biome] - небо и погода боя с боссом; [shares] - источники строк карты на монстрах; [mapEffects] - строки карты захода на рельсе героя;
 * [pulse] - часы и замахи кадра, снимок [fight] выходит реже кадра; [top] - полоса режима над врагами (Честь Разлома). Часы арены читаются только в рисовании ([FightFrame]). Пауза и вибрация ([LocalBuzzes]) -
 * раз при падении здоровья через свою черту, вибрация - и при гибели. Лист врага открывается касанием на паузе и до боя; лист умения держит
 * бой. Мощь боя: герой - по телу боя под эффектами карты, враги - по своему листу. Стороны арены: верх - враги, низ - герой; бой с боссом -
 * своя раскладка. Карточка врага - по ключу врага. Победу говорит окно наград, здесь - только поражение и отступление. Касание врага за
 * живым провокатором ставит фокус в очередь и говорит об этом подсказкой ([FocusHint]); касание тотема в бою - что он неуязвим.
 */
@Composable internal fun ArenaOverlay(
    game: GameUi,
    hud: RunHud,
    fight: FightHud,
    level: Int,
    rules: CombatRules,
    stance: HeroStance,
    clock: State<Float>,
    onCommand: (RunCommand) -> Unit,
    onLogFilter: (Set<LogKind>) -> Unit = {},
    biome: String = "",
    shares: (Boolean) -> List<BuffShare> = { emptyList() },
    mapEffects: Map<String, Double> = emptyMap(),
    origins: EffectOrigins = EffectOrigins.NONE,
    pulse: FightPulse? = null,
    top: (@Composable () -> Unit)? = null,
) {
    val breath = decorTime()
    val frame = remember(clock, pulse, breath) { FightFrame(clock, pulse, breath) }
    val settings = LocalSettings.current
    val share = if (fight.heroMaxLife > 0) fight.heroLife / fight.heroMaxLife.toFloat() else 1f
    val low = share * 100 < settings.autoPause
    val danger = share < DANGER_SHARE
    LaunchedEffect(low) { if (low && fight.started && !fight.paused && fight.outcome == null) onCommand(RunCommand.Pause) }
    val buzzes = LocalBuzzes.current
    LaunchedEffect(danger) { if (danger && fight.outcome == null) buzzes?.buzz(Buzz.DANGER) }
    LaunchedEffect(fight.outcome) { if (fight.outcome == Outcome.LOSS) buzzes?.buzz(Buzz.DANGER) }
    val bounds = remember { mutableStateMapOf<Int, Rect>() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val names = remember(fight.foes.size, fight.leader, fight.round) { fight.foes.associate { it.index to monsterTitle(it.monster.code) } }
    val traits = remember(fight.foes.size, fight.leader, fight.round, game.index) {
        fight.foes.associate { foe -> foe.index to (game.index?.let { traitViews(foe.monster, it) } ?: emptyList()) }
    }
    val chosen = fight.focus ?: fight.target ?: fight.field.firstOrNull { it.alive }?.index
    val large = fight.scouting
    var info by remember { mutableStateOf<SkillView?>(null) }
    var inspect by remember { mutableStateOf<Int?>(null) }
    var hint by remember { mutableStateOf<FocusHint?>(null) }

    fun focusOn(index: Int) {
        val foe = fight.foes.firstOrNull { it.index == index }
        if (foe != null && foe.alive && !foe.reachable && fight.focus != index) {
            val taunter = fight.foes.firstOrNull { it.alive && it.taunt }?.index
            hint = FocusHint.TauntFirst(names[index].orEmpty(), taunter?.let(names::get).orEmpty())
        }
        onCommand(RunCommand.Focus(index))
    }
    fun track(key: Int) = Modifier.onGloballyPositioned { bounds[key] = it.boundsInRoot() }
    val mods = remember(mapEffects, origins, game.hero, game.index) {
        if (mapEffects.isEmpty()) emptyList() else game.index?.let { MapEffects.heroMods(it, game.hero?.figures?.stats.orEmpty(), mapEffects, origins) }.orEmpty()
    }
    val might = rememberFightMight(game.index, fight.heroBody, level, game.hero?.combat?.mightExtras ?: MightExtras.NONE)
    val intro = rememberBossIntro(fight.boss?.index)
    val fx = intro ?: ArenaFxGate.OPEN
    val introBounds = remember { IntroBounds() }
    val onIntroFrame: (LayoutCoordinates) -> Unit = remember(intro) { { if (intro?.staged == true) introBounds.frame(it) } }
    val introField = remember(intro) { if (intro == null) Modifier else Modifier.onGloballyPositioned { if (intro.staged) introBounds.field(it) } }
    CompositionLocalProvider(LocalFightMight provides might, LocalArenaFx provides fx) {
        Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }.graphicsLayer { alpha = intro?.entry ?: 1f }) {
            fight.boss?.let { BossBackdrop(biome, it, frame, fx) }
            Spacer(Modifier.fillMaxSize().sideTint())
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                top?.invoke()
                val boss = fight.boss
                if (boss != null) {
                    BossBand(fight, boss, frame, chosen, large, traits, ::track, ::focusOn, onIntroFrame, { hint = FocusHint.Totem(it.left) }) { inspect = it }
                } else {
                    PackHeader(fight, level)
                    if (fight.field.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                            fight.field.forEach { foe ->
                                key(foe.index) {
                                    FoeCard(
                                        foe,
                                        fight,
                                        frame,
                                        chosen == foe.index && fight.scouting,
                                        track(foe.index).weight(1f).widthIn(max = 180.dp),
                                        large,
                                        traits[foe.index].orEmpty(),
                                        narrow = fight.field.size >= 3,
                                    ) {
                                        focusOn(foe.index)
                                    }
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
                            traits[shown.index].orEmpty(),
                        )
                    } else {
                        FightFeed(game, fight, names, onCommand, onLogFilter)
                    }
                }
                HeroCard(game, hud, fight, frame, names, stance, track(HERO_CARD).then(introField), large, mods, onCommand)
                if (fight.skills.any { it != null } || fight.flasks.any { it != null }) ActionBar(game, fight, frame, onCommand) { info = it }
                Controls(fight, onCommand)
            }
            StrikeFx(fight, frame, { foe -> bounds[foe ?: HERO_CARD] }, origin)
            HitVignette(fight, frame)
            SkillFx(fight, frame, bounds[HERO_CARD], bounds::get, origin)
            fight.boss?.let { BossCinema(fight, it, intro, introBounds) }
            inspect?.let { i -> fight.foes.firstOrNull { it.index == i } }?.let { foe ->
                FoeSheet(
                    foe,
                    fight,
                    foe.monster.level.takeIf { it > 0 } ?: level,
                    rules,
                    traits[foe.index].orEmpty(),
                    fight.boss,
                    remember(foe.index, fight.boss?.index) { shares(fight.boss?.index == foe.index) },
                    onFocus = {
                        focusOn(foe.index)
                        inspect = null
                    }.takeIf { foe.alive && fight.outcome == null && fight.focus != foe.index },
                ) { inspect = null }
            }
            hint?.let { FocusHintCard(it, { hint = null }, Modifier.align(Alignment.Center).padding(horizontal = 12.dp)) }
            info?.let { view -> FightSkillSheet(game, view, onCommand) { info = null } }
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
}

/** The life share under which the phone warns of danger. */
private const val DANGER_SHARE = .3f
