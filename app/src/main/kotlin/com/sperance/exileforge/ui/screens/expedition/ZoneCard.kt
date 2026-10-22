package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.LootFalloffView
import com.sperance.exileforge.core.campaign.MapLaunch
import com.sperance.exileforge.core.campaign.TokenState
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.core.campaign.WorldToken
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.mapDescription
import com.sperance.exileforge.core.display.mapTitle
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.regionTitle
import com.sperance.exileforge.core.i18n.pluralKey
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.SummonBar
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.AtlasPoints
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.Monster
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.auction.untilText
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.screens.server.SettingsViewModel
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.pow

/** One stash map of a zone, with the view its square and its lines are drawn from. */
internal data class StashMap(val item: ItemInstance, val view: ItemView)

/**
 * How many maps of each zone the stash holds. Read off the items' zone alone: building a full view of every
 * thing in the stash on each opening of the tab froze the tap.
 *
 * Карты тайника по зонам: карта мира метит жетон их числом.
 */
fun stashCounts(game: GameUi): Map<String, Int> = game.hero?.stash.orEmpty().filter { it.mapZone.isNotEmpty() }.groupingBy { it.mapZone.value }.eachCount()

internal fun stashMaps(game: GameUi): List<StashMap> = game.hero?.stash.orEmpty()
    .filter { it.mapZone.isNotEmpty() }.mapNotNull { item -> game.view(item)?.takeIf { it.slot == Slot.MAP }?.let { StashMap(item, it) } }

/**
 * Карточка зоны на карте мира (вместо окна входа): поднимается над низом карты по касанию жетона. Каждая часть - одной строкой: имя
 * (касание - описание зоны) и состояние; уровень, биом, финал, регион и прохождения; страж - ждёт или убит до срока - и три точки
 * Атласа; лента снаряжения (карты, зелья, скарабеи); плита выбранной карты; «Войти в портал». Зона «???» говорит только, чей страж её
 * открывает. Правила и контент - индекса; сроки зоны - возвращение стража - кампании героя.
 *
 * Запертая карта хранится нарочно: трата на заход спрашивается. Тестировщик со свободным прогрессом начинает заход сразу боем со стражем. Новое семя раньше
 * правил не даётся: кнопка погашена и считает секунды; погашенная без карты объясняет причину по касанию.
 */
@Composable fun ZoneCard(game: GameUi, vm: ExpeditionViewModel, world: WorldMap, launch: MapLaunch, forced: Set<MapMechanic>, modifier: Modifier = Modifier) {
    val token = world.token(launch.mapCode) ?: return
    val index = game.index ?: return
    val zone = token.zone
    val maps = remember(game.hero?.items, index, zone.code, launch.picked) { zoneMaps(game, zone, launch) }
    var askLocked by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launchGuarded: (() -> Unit) -> Unit = { go -> if (maps.picked?.item?.locked == true) askLocked = go else go() }
    askLocked?.let { go ->
        ConfirmSheet(
            title = ui("expedition.launch_locked_q"),
            confirm = ui("expedition.launch_locked_go"),
            danger = true,
            onDismiss = { askLocked = null },
        ) { go() }
    }
    Surface(
        modifier.fillMaxWidth(),
        color = Panel,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        border = BorderStroke(1.dp, PanelRaised),
        shadowElevation = 12.dp,
    ) {
        Column(
            Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(28.dp)) {
                Box(Modifier.align(Alignment.Center).size(38.dp, 4.dp).background(Color(0xFF3A414B), RoundedCornerShape(2.dp)))
                ForgeIconButton(Icons.Outlined.Close, ui("common.close"), onClick = vm::closeZone, modifier = Modifier.align(Alignment.CenterEnd), size = ButtonSize.Compact)
            }
            val region = world.campaign.regions.firstOrNull { region -> region.zones.any { it.code == zone.code } }?.code
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Header(token)
                ZoneMeta(game, index, token, region, maps.picked)
            }
            if (token.state == TokenState.LOCKED) {
                val keys = world.keysTo(zone.code).joinToString(ui("expedition.or")) { "«${mapTitle(it.code)}»" }
                Text(if (keys.isEmpty()) ui("expedition.opens_after_any") else ui("expedition.opens_after", keys), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val wait = seedWait(seedReadyAt(game, zone.code, launch, forced))
            val mapless = launch.picked == null && !continues(game, zone.code, launch, forced) && zone.needsMap(game.hero?.campaign?.cleared.orEmpty())
            val summon = GuardianCall(
                mapRun = launch.picked != null,
                blocked = if (wait > 0) ui("expedition.launch_wait", wait) else null,
            ) { launchGuarded { vm.startRun(zone.code.value, summon = true) } }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                index.monster(zone.boss)?.let { Guardian(game, zone, it, maps.picked?.view?.areaLevel ?: zone.level, summon, Modifier.weight(1f).fillMaxHeight()) } ?: Spacer(Modifier.weight(1f))
                AtlasPips(game.atlasState?.earned.orEmpty(), zone.code.value, Modifier.fillMaxHeight())
            }
            KitRibbon(game, vm, zone, maps, launch)
            maps.picked?.let { MapPlate(game, index, zone, it, launch) }
            LaunchButton(
                label = when {
                    mapless -> ui("expedition.launch_need_map")
                    wait > 0 -> ui("expedition.launch_wait", wait)
                    else -> ui("expedition.launch_go")
                },
                state = ButtonState.of(game.hero != null && !game.busy && wait == 0L && !mapless),
                why = ui("expedition.launch_needs_map").takeIf { mapless },
            ) { launchGuarded { vm.startRun(zone.code.value) } }
            if (game.freeProgress) TesterMechanics(vm, forced, zone, launch.picked != null)
            if (game.freeProgress) {
                ForgeButton(
                    state = ButtonState.of(game.hero != null && !game.busy && wait == 0L),
                    onClick = { launchGuarded { vm.startRun(zone.code.value, toBoss = true) } },
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Secondary,
                ) { Text(ui("expedition.launch_boss")) }
            }
        }
    }
}

/** «Войти в портал» во всю ширину: подпись [label] несёт и ожидание, и причину запрета; погашенная с причиной [why] - касание её объясняет. */
@Composable private fun LaunchButton(label: String, state: ButtonState, why: String?, onClick: () -> Unit) {
    var explain by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        ForgeButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Wide,
            state = state,
            icon = ForgeGlyphs.Portal,
            onDisabledClick = why?.let { { explain = true } },
        ) { Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        if (explain && why != null) TipCallout(Tip(body = why, tint = Muted)) { explain = false }
    }
}

/**
 * Когда портал зоны [zone] даст новое семя, мс эпохи сервера: вход без карты в зону открытого захода с теми же механиками [forced]
 * продолжает его - ждать нечего (0); иначе - правило `run.newSeedSeconds` от прошлого семени героя (`RunRules.seedReadyAt`).
 */
private fun seedReadyAt(game: GameUi, zone: MapCode, launch: MapLaunch, forced: Set<MapMechanic>): Long {
    val campaign = game.hero?.campaign ?: return 0
    val rules = game.index?.rules?.run ?: return 0
    if (continues(game, zone, launch, forced)) return 0
    return rules.seedReadyAt(campaign.seededAt)
}

/** Вход без карты в зону [zone] открытого захода с теми же механиками [forced] продолжает его: ни семени, ни карты ему не нужно. */
private fun continues(game: GameUi, zone: MapCode, launch: MapLaunch, forced: Set<MapMechanic>): Boolean {
    val run = game.hero?.campaign?.run ?: return false
    return launch.picked == null && run.zone == zone.value && run.context.forced == forced
}

/** Сколько целых секунд до [readyAt] по часам сервера, вживую раз в секунду; 0 - можно сейчас. */
@Composable private fun seedWait(readyAt: Long): Long = (rememberServerLeft(readyAt, SEED_TICK_MS) + SEED_TICK_MS - 1) / SEED_TICK_MS

/** Шаг отсчёта до нового семени: секунда. */
private const val SEED_TICK_MS = 1000L

/**
 * Механики тестировщика со свободным прогрессом: что поставить на новый заход насильно - каждая переключателем и «Все» разом. Выбор держится
 * между зонами; открытый заход с другим выбором начинается заново. Панель свёрнута заголовком «Механики · выбрано N»;
 * раскрытость помнит устройство ([com.sperance.exileforge.core.app.GameSettings.testerMechanicsOpen]). Механика, что не встанет на заход
 * в зону [zone] по смыслу ([MapMechanic.bar]; [withMap] - вход с картой), - чип погашен, касание называет причину.
 */
@Composable private fun TesterMechanics(vm: ExpeditionViewModel, forced: Set<MapMechanic>, zone: Zone, withMap: Boolean) {
    val settings = koinViewModel<SettingsViewModel>()
    val open = settings.settings.collectAsStateWithLifecycle().value.testerMechanicsOpen
    ForgePanel {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).foldToggle(open) { settings.change { copy(testerMechanicsOpen = !testerMechanicsOpen) } },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.weight(1f)) { Engraved(ui("tester.mechanics_count", forced.size)) }
            FoldChevron(open)
        }
        FoldBody(open, Arrangement.spacedBy(10.dp)) {
            MutedText(ui("tester.mechanics_hint"), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val all = forced.size == MapMechanic.entries.size
                ForgeChip(ui("tester.mechanics_all"), all, { vm.forceAll(!all) })
                MapMechanic.entries.forEach { mechanic ->
                    val bar = mechanic.bar(zone, withMap)
                    Tipped(bar?.let { { Tip(body = ui("tester.mechanic_bar.${it.name}"), tint = Muted) } }) {
                        ForgeChip(ui("tester.mechanic.${mechanic.name}"), mechanic in forced, { vm.toggleForced(mechanic) }, state = ButtonState.of(bar == null))
                    }
                }
            }
        }
    }
}

/** Имя зоны (касание - её описание) и её состояние плашкой; у неизведанной - «Неизведанная зона» погашенно. */
@Composable internal fun Header(token: WorldToken) {
    val zone = token.zone
    val locked = token.state == TokenState.LOCKED
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val style = MaterialTheme.typography.titleLarge
        if (locked) {
            Text(ui("expedition.unknown_zone"), color = Muted, style = style, modifier = Modifier.weight(1f))
        } else {
            val name = mapTitle(zone.code)
            Box(Modifier.weight(1f)) { TapText(name, GoldBright, style, { Tip(body = mapDescription(zone.code.value)) }, maxLines = 2) }
        }
        val (label, tint) = when (token.state) {
            TokenState.PASSED -> ui("expedition.token_passed") to Gold
            TokenState.OPEN -> ui("expedition.token_open") to Rune
            TokenState.LOCKED -> ui("expedition.token_locked") to Muted
        }
        Row(
            Modifier.border(1.dp, tint.copy(alpha = .55f), CircleShape).background(tint.copy(alpha = .1f), CircleShape).padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (locked) Icon(Icons.Outlined.Lock, null, tint = tint, modifier = Modifier.size(12.dp))
            Text(label.uppercase(), color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Строка под именем: уровень, биом, финал, регион и прохождения через «·»; у неизведанной - только уровень и регион. Выбранная карта
 * с уровнем района - «Ур. 63 → 80», касание объясняет тир или уровень карты и ворота тиров героя.
 */
@Composable private fun ZoneMeta(game: GameUi, index: ContentIndex, token: WorldToken, region: String?, picked: StashMap?) {
    val zone = token.zone
    val locked = token.state == TokenState.LOCKED
    val passed = game.hero?.campaign?.completions?.get(zone.code.value) ?: 0
    val rest = listOfNotNull(
        zone.biome.takeUnless { locked || it.isBlank() }?.let { ui("expedition.biome.$it") },
        ui("expedition.finale").takeIf { zone.finale && !locked },
        region?.let(::regionTitle),
        (if (passed > 0) ui(pluralKey("expedition.completions", passed), passed) else ui("expedition.completions_none")).takeUnless { locked },
    )
    val style = MaterialTheme.typography.labelMedium
    val map = picked?.takeUnless { locked }?.let { stash -> stash.view.areaLevel?.let { stash to it } }
    Row(verticalAlignment = Alignment.Top) {
        if (map == null) {
            Text(ui("expedition.map_level", zone.level), color = Muted, style = style)
        } else {
            val (stash, area) = map
            val tier = stash.item.mapTier
            val line = if (tier > 0) ui("expedition.launch_area", tier, area, zone.level) else ui("expedition.launch_map_level", area)
            val gate = tierGate(index, game.hero?.level)
            TapText(ui("expedition.zone_level_to", zone.level, area), GoldBright, style, { Tip(line, gate.orEmpty()) })
        }
        if (rest.isNotEmpty()) Text(rest.joinToString(" · ", prefix = " · "), color = Muted, style = style, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Призыв павшего стража перед входом из карточки зоны: [mapRun] - вход с картой-предметом (финал региона зовёт стража только с ней),
 * [blocked] - почему войти сейчас нельзя (ожидание семени), [enter] - вход с призывом.
 */
internal class GuardianCall(val mapRun: Boolean, val blocked: String?, val enter: () -> Unit)

/**
 * The zone's guardian: its bust and name, and whether it waits by the exit or lies slain until its
 * time — the hero's campaign says when it is back.
 *
 * Страж открывает свой лист: умения и фазы, каждое своей справкой; [areaLevel] - уровень района захода, как его считает сервер
 * (выбранная карта - [ItemView.areaLevel], без карты - уровень зоны). Пока страж повержен, под его именем - призыв до входа ([SummonRow]).
 */
@Composable internal fun Guardian(game: GameUi, zone: Zone, boss: Monster, areaLevel: Int, summon: GuardianCall?, modifier: Modifier = Modifier) {
    val campaign = game.hero?.campaign
    val back = campaign?.takeIf { it.bossDown(zone.code, System.currentTimeMillis()) }?.bosses?.get(zone.code.value)
    val shape = RoundedCornerShape(12.dp)
    var open by remember { mutableStateOf(false) }
    if (open) GuardianSheet(game, zone, boss, areaLevel) { open = false }
    Row(
        modifier.background(Abyss, shape).border(1.dp, PanelRaised, shape).clip(shape).clickable { open = true }.padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(34.dp, 42.dp).clip(RoundedCornerShape(6.dp))) { Portraits.monster(this, boss.code.value, boss.form, 0f) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(monsterTitle(boss.code), color = Parchment, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                back?.let { ui("expedition.guardian_slain", untilText(it)) } ?: ui("expedition.guardian_waits"),
                color = if (back != null) Muted else Color(0xFFE0907F),
                style = MaterialTheme.typography.labelMedium,
            )
            if (back != null && summon != null) SummonRow(game, zone, boss, areaLevel, summon)
        }
    }
}

/**
 * Призыв павшего стража до входа (цена в золоте у места оплаты): слева «Призыв до входа» и у повтора в окне - во сколько дороже,
 * справа цена [GoldCost] над маленькой «Призвать» ([RowButton], не хватает - недостача). Касание - подтверждение «Призвать и войти»
 * удержанием со строками «Есть · Спишется · Останется»; открытый заход, финал без карты и ожидание семени гасят его с причиной.
 * Цена и причина - [com.sperance.exileforge.core.model.campaign.CampaignState.summonOffer] по часам сервера.
 */
@Composable private fun SummonRow(game: GameUi, zone: Zone, boss: Monster, areaLevel: Int, call: GuardianCall) {
    val index = game.index ?: return
    val services = index.campaign.services
    val now = rememberServerNow(SUMMON_TICK_MS)
    val offer = game.hero?.campaign?.summonOffer(zone, services, areaLevel, call.mapRun, now) ?: return
    val bill = game.goldBill(offer.price) ?: return
    var asking by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            MutedText(ui("expedition.summon_before"), style = MaterialTheme.typography.labelSmall)
            if (offer.repeats > 0) {
                Text(ui("expedition.summon_repeat", number(services.summonWindowHours), number(services.summonGrowth.pow(offer.repeats))), color = Caution, style = MaterialTheme.typography.labelSmall)
            }
        }
        RowActions(value = { GoldCost(bill) }) {
            RowButton(bill.payLabel(ui("expedition.summon_short")), { asking = true }, kind = ButtonKind.Secondary, state = ButtonState.of(bill.affordable))
        }
    }
    if (asking) {
        val bar = when (offer.bar) {
            SummonBar.OPEN_RUN -> game.hero?.campaign?.run?.let { ui("expedition.summon_open_run", mapTitle(it.zone)) }
            SummonBar.NEEDS_MAP -> ui("expedition.summon_needs_map")
            null -> call.blocked
        }
        ConfirmSheet(
            title = ui("expedition.summon_q"),
            confirm = ui("expedition.summon_go"),
            onDismiss = { asking = false },
            subtitle = "${monsterTitle(boss.code)} · ${mapTitle(zone.code)}",
            note = ui("expedition.summon_note", number(services.summonWindowHours), number(services.summonGrowth)),
            warning = bar,
            blocked = bar != null,
            gold = bill,
        ) {
            asking = false
            call.enter()
        }
    }
}

/** Такт часов призыва: цена и срок стража меняются не чаще окна правил, подписи хватает раз в полминуты. */
private const val SUMMON_TICK_MS = 30_000L

/**
 * Лист стража зоны: уровень и здоровье, под ними - срез добычи у героя выше района [areaLevel] ([LootFalloffView], только когда доля
 * меньше 1), умения и фазы чипами - каждый открывает свою справку.
 *
 * Хаос и яд идут мимо энергощита - лист говорит это до боя.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuardianSheet(game: GameUi, zone: Zone, boss: Monster, areaLevel: Int, onDismiss: () -> Unit) {
    val index = game.index ?: return
    val rules = index.campaign.combat
    val body = remember(boss, zone) { com.sperance.exileforge.core.campaign.combat.Combatant(com.sperance.exileforge.rules.roll.MonsterRoller(index).stats(boss, zone.level), zone.level, rules) }
    val name = monsterTitle(boss.code)
    val lore = LocalLore.current
    val steps = remember(boss) { index.campaign.phasesOf(boss) }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("challenge.subtitle", zone.level, com.sperance.exileforge.core.display.number(body.maxLife)))
            game.hero?.level?.let { LootFalloffView.of(index.campaign.lootFalloff, it, areaLevel) }?.let { MutedText(it.text) }
            if (boss.skills.isNotEmpty()) {
                Text(ui("challenge.skills").uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    boss.skills.forEach { code -> LoreChip(com.sperance.exileforge.core.display.SkillText.title(code), Rune) { lore?.invoke(Lore.Skill(code, name, body)) } }
                }
            }
            boss.phases?.takeIf { steps.isNotEmpty() }?.let { code ->
                Text(ui("challenge.traits").uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
                val marks = steps.map { it.at }.distinct().joinToString("/") { "${it.toInt()}" }
                LoreChip(ui("challenge.phase", com.sperance.exileforge.core.display.phaseTitle(code), marks), Elder) { lore?.invoke(Lore.Phase(code, name)) }
            }
            val terms = remember(body) { com.sperance.exileforge.core.display.Term.ofStats(body.stats.filterValues { it != 0.0 }.keys) }
            TermsBlock(terms, rules)
        }
    }
}

/**
 * Три очка Атласа зоны - страж, страж с редкой картой, страж Изнанки - точками рядом со стражем, зажжённые, когда взяты; касание
 * называет каждое.
 */
@Composable internal fun AtlasPips(earned: List<String>, zone: String, modifier: Modifier = Modifier) {
    val kinds = listOf(AtlasPoints.BOSS, AtlasPoints.RARE, AtlasPoints.VAAL).map { it to (AtlasPoints.key(it, zone) in earned) }
    val tip = { Tip(ui("atlas.open"), facts = kinds.map { (kind, on) -> ui("expedition.key_$kind") to if (on) "✓" else "—" }) }
    val shape = RoundedCornerShape(12.dp)
    Tipped(tip, modifier.background(Abyss, shape).border(1.dp, PanelRaised, shape).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(ui("atlas.open"), color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.dottedUnderline(Muted).padding(end = 2.dp))
            kinds.forEach { (_, on) -> Box(Modifier.size(8.dp).border(1.dp, if (on) Gold else Muted, CircleShape).background(if (on) Gold else Color.Transparent, CircleShape)) }
        }
    }
}
