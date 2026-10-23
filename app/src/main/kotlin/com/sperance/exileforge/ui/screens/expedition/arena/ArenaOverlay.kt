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
import com.sperance.exileforge.ui.screens.expedition.StatsSheet
import com.sperance.exileforge.ui.screens.expedition.rememberFightSheets
import com.sperance.exileforge.ui.screens.server.SettingsViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** The key a card's bounds are kept under: the hero's, and each foe's by its place in the pack. */
private const val HERO_CARD = -1

/**
 * Бой карточками: враги поля одной строкой сверху - всё о враге в его карточке ([FoeCard]: имя, плашка уровня, редкость рамкой, Мощь,
 * печати, полосы, полка эффектов), карточка героя внизу, между ними всегда журнал боя ([FightFeed]); над журналом - строка стаи
 * ([PackLine]: волна, сколько осталось, ярость). Кто бьёт - приподнят к другой стороне и подсвечен, от него к цели - линия удара.
 * Касание врага в бою - цель героя (то же касание снимает); до «В бой» и на паузе - лист врага ([FoeSheet]) с разведкой. Один враг
 * без волн - карточка шире ([SOLO_CARD]), строки стаи нет. Бой с боссом - тот же принцип: рамка босса, журнал в центре.
 *
 * [biome] - небо и погода боя с боссом; [shares] - источники строк карты на монстрах; [mapEffects] - строки карты захода на рельсе героя
 * и в листе характеристик (null - бой без карты: испытания, Разлом, башня; кнопка «Характеристики» в карточке героя открывает
 * [StatsSheet] на вкладке «В бою», её счёт ▲/▼ - сдвиг тела боя против листа «На карте» или «Герой»);
 * [pulse] - часы и замахи кадра, снимок [fight] выходит реже кадра; [top] - полоса режима над врагами (Честь Разлома). Часы арены читаются только в рисовании ([FightFrame]). Пауза и вибрация ([LocalBuzzes]) -
 * раз при падении здоровья через свою черту, вибрация - и при гибели. Лист врага открывается касанием на паузе и до боя; лист умения держит
 * бой, карточка фляги (долгое нажатие на флягу, [FlaskCardSheet]) - тоже. [game] - срез героя в наборе боя (в походе - с находками
 * захода), по нему - карточка фляги. Мощь боя: герой - по телу боя под эффектами карты, враги - по своему листу. Стороны арены: верх - враги, низ - герой; бой с боссом -
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
    mapEffects: Map<String, Double>? = null,
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
    var flaskCard by remember { mutableStateOf<Int?>(null) }
    var statsOpen by remember { mutableStateOf(false) }
    val sheets = rememberFightSheets(game, mapEffects, origins, fight.heroBody?.stats)
    var inspect by remember { mutableStateOf<Int?>(null) }
    var hint by remember { mutableStateOf<FocusHint?>(null) }
    val prefs = koinViewModel<SettingsViewModel>()
    val firstTip = !settings.focusTipSeen && fight.started && fight.outcome == null && fight.foes.count { it.alive && it.onField } >= 2

    fun focusOn(index: Int) {
        if (!settings.focusTipSeen) prefs.change { copy(focusTipSeen = true) }
        val foe = fight.foes.firstOrNull { it.index == index }
        if (foe != null && foe.alive && !foe.reachable && fight.focus != index) {
            val taunter = fight.foes.firstOrNull { it.alive && it.taunt }?.index
            hint = FocusHint.TauntFirst(names[index].orEmpty(), taunter?.let(names::get).orEmpty())
        }
        onCommand(RunCommand.Focus(index))
    }
    fun track(key: Int) = Modifier.onGloballyPositioned { bounds[key] = it.boundsInRoot() }
    val mods = remember(mapEffects, origins, game.hero, game.index) {
        if (mapEffects.isNullOrEmpty()) emptyList() else game.index?.let { MapEffects.heroMods(it, game.hero?.figures?.stats.orEmpty(), mapEffects, origins) }.orEmpty()
    }
    val might = rememberFightMight(game.index, fight.heroBody, level, game.hero?.combat?.mightExtras ?: MightExtras.NONE)
    val intro = rememberBossIntro(fight.boss?.index)
    val fx = intro ?: ArenaFxGate.OPEN
    val introBounds = remember { IntroBounds() }
    val onIntroFrame: (LayoutCoordinates) -> Unit = remember(intro) { { if (intro?.staged == true) introBounds.frame(it) } }
    val introField = remember(intro) { if (intro == null) Modifier else Modifier.onGloballyPositioned { if (intro.staged) introBounds.field(it) } }
    val hold: (Boolean) -> Unit = remember(onCommand) { { on -> onCommand(RunCommand.Hold(on)) } }
    CompositionLocalProvider(LocalFightMight provides might, LocalArenaFx provides fx, LocalArenaHold provides hold) {
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
                    BossBand(fight, boss, frame, level, chosen, large, traits, ::track, ::focusOn, onIntroFrame, { hint = FocusHint.Totem(it.left) }) { inspect = it }
                } else if (fight.field.isNotEmpty()) {
                    val cardWidth = if (fight.field.size == 1) SOLO_CARD else PACK_CARD
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        fight.field.forEach { foe ->
                            key(foe.index) {
                                FoeCard(
                                    foe,
                                    fight,
                                    frame,
                                    chosen == foe.index && fight.scouting,
                                    track(foe.index).weight(1f).widthIn(max = cardWidth),
                                    large,
                                    traits[foe.index].orEmpty(),
                                    level,
                                    narrow = fight.field.size >= 3,
                                ) {
                                    tapFoe(fight, foe.index, ::focusOn) { inspect = it }
                                }
                            }
                        }
                    }
                }
                if (firstTip) FocusTip({ prefs.change { copy(focusTipSeen = true) } })
                if (boss == null) PackLine(fight)
                Box(Modifier.weight(1f).fillMaxWidth()) { FightFeed(game, fight, names, onCommand, onLogFilter) }
                HeroCard(game, hud, fight, frame, names, stance, sheets.fightShift, { statsOpen = true }, track(HERO_CARD).then(introField), large, mods, onCommand)
                if (fight.skills.any { it != null } || fight.flasks.any { it != null }) ActionBar(game, fight, frame, onCommand, { flaskCard = it.slot }) { info = it }
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
                    stance,
                    onFocus = {
                        focusOn(foe.index)
                        inspect = null
                    }.takeIf { foe.alive && fight.outcome == null && fight.focus != foe.index },
                ) { inspect = null }
            }
            hint?.let { FocusHintCard(it, { hint = null }, Modifier.align(Alignment.Center).padding(horizontal = 12.dp)) }
            info?.let { view -> FightSkillSheet(game, view) { info = null } }
            flaskCard?.let(game::beltFlask)?.let { item -> FlaskCardSheet(game, item) { flaskCard = null } }
            if (statsOpen) StatsSheet(game, mapEffects, origins, fight) { statsOpen = false }
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

/** Предел ширины карточки врага: одного на поле - шире, стаи - уже. */
private val SOLO_CARD = 220.dp
private val PACK_CARD = 180.dp

/** The life share under which the phone warns of danger. */
private const val DANGER_SHARE = .3f
