package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.PoolReserve
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.display.ConditionText
import com.sperance.exileforge.core.display.SkillGrowthView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.ReservePool
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.round
import kotlin.math.roundToInt

/** The grimoire's two sections (the owner's mockup A «Гримуар»): the class's skills, and the belt. */
internal enum class GrimoireSection(val title: String, val icon: ImageVector) {
    SKILLS("skills.section_skills", ForgeGlyphs.Grimoire),
    BELT("skills.section_belt", ForgeGlyphs.Flask),
}

/** What a sheet of the grimoire is choosing: a skill for a slot, when a slot fires, when a flask is drunk. */
internal sealed interface Pick {
    data class Slot(val kind: SkillKind, val index: Int) : Pick
    data class Condition(val index: Int) : Pick
    data class Belt(val index: Int) : Pick
}

/**
 * The grimoire: the class's skills as the pages of a book and the belt of flasks.
 *
 * On top are the slots — three active, each with when it fires by itself, and two passive — opening with
 * the hero's level; under them every skill of the class, its level, and what the next book of it asks.
 * A page opens its skill whole: what it does now and at the next level, the book to read, the slot to
 * put it in. The belt holds the three flasks worn, each with when it is drunk by itself. Every rule is
 * the rules module's, read off the content on screen; the page says beforehand what the server will say.
 *
 * «Можно изучить» - неизученные своего класса и общие, чья книга в сумке. Показано только изученное; общие умения - своим разделом. Замена
 * умения ячейки - из её листа условия.
 */
@Composable fun GrimoireScreen() {
    val game by koinViewModel<GrimoireViewModel>().game.collectAsStateWithLifecycle()
    val vm = koinViewModel<GrimoireViewModel>()
    var section by rememberSaveable(game.heroId) { mutableStateOf(GrimoireSection.SKILLS) }
    var page by remember(game.heroId) { mutableStateOf<String?>(null) }
    var pick by remember(game.heroId) { mutableStateOf<Pick?>(null) }
    var exchanging by remember(game.heroId) { mutableStateOf(false) }
    var belt by rememberSaveable(game.heroId) { mutableIntStateOf(0) }
    LaunchedEffect(game.heroId, game.sessionEpoch) { vm.ensure() }
    val hero = game.hero
    val index = game.index
    val book = index?.skills
    val classCode = hero?.heroClass.orEmpty()
    val skills = hero?.skills ?: HeroSkills()
    val level = hero?.level ?: 1
    val body = remember(hero?.sheet, index) { index?.let { i -> hero?.let { Combatant(it.stats, it.level, i.campaign.combat) } } }
    val pages = remember(book, classCode) { book?.ofClass(classCode).orEmpty() }
    if (hero != null && body != null && index != null) {
        val loadout = remember(skills, index, classCode, hero.stats) { Loadout.of(skills, index, classCode, emptyList(), hero.stats) }
        val mana = loadout.reserve(ReservePool.MANA, body)
        val life = loadout.reserve(ReservePool.LIFE, body)
        TrailData(
            listOfNotNull(
                ui("skills.data_level", level),
                ui("skills.data_mana", number(body.maxMana)),
                ui("skills.data_reserve", number(mana.percent)).takeIf { mana.percent > 0 },
                ui("skills.data_life", number(life.percent)).takeIf { life.percent > 0 },
            ).joinToString(" · "),
            remember(index, level, body, mana, life) { trailTip(index, level, body.maxMana, mana, life) },
        )
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Tabs(section) { section = it } }
        if (hero == null || body == null || index == null) {
            item { InfoCard(ui("common.loading"), ui("skills.loading_hint")) }
        } else {
            when (section) {
                GrimoireSection.SKILLS -> {
                    item { Slots(index, skills, level, body.stats) { pick = it } }
                    val learnable = pages.filter { skills.level(it.code) == 0 && bookCount(game, it.code) > 0 }
                    if (learnable.isNotEmpty()) item { Caption(ui("skills.can_learn")) }
                    items(learnable, key = { "learn:" + it.code }) { skill ->
                        Learnable(game, index, skill, skills, level, body.stats, onOpen = { page = skill.code }) { vm.learnSkill(skill.code) }
                    }
                    item { Caption(ui("skills.book_of", classTitle(classCode))) }
                    items(pages.filter { !it.common && skills.level(it.code) > 0 }, key = { it.code }) { skill ->
                        Page(game, index, skill, skills, level, body.stats, slotOf(skills, skill.code)) { page = skill.code }
                    }
                    val shared = pages.filter { it.common && skills.level(it.code) > 0 }
                    if (shared.isNotEmpty()) item { Caption(ui("skills.book_common")) }
                    items(shared, key = { it.code }) { skill ->
                        Page(game, index, skill, skills, level, body.stats, slotOf(skills, skill.code)) { page = skill.code }
                    }
                    item { Books(game, index, pages) { exchanging = true } }
                }

                GrimoireSection.BELT -> {
                    item { MutedText(ui("skills.belt_hint", index.campaign.fountains.flasks.roundToInt())) }
                    item { Belt(hero, index, skills, body, belt) { belt = it } }
                    item { BeltDetail(hero, index, skills, body, belt) { pick = Pick.Belt(belt) } }
                }
            }
        }
    }
    page?.let { code -> book?.byCode?.get(code) }?.let { skill ->
        if (hero != null && body != null && index != null) SkillSheet(game, vm, index, skill, skills, level, body.stats) { page = null }
    }
    when (val chosen = pick) {
        is Pick.Slot -> SkillPicker(game, chosen, skills, pages, onDismiss = { pick = null }) { code ->
            pick = null
            vm.slotSkill(chosen.kind.name, chosen.index, code)
        }

        is Pick.Condition -> skills.active.getOrNull(chosen.index)?.let { slot ->
            val skill = index?.skills?.byCode?.get(slot.skill)
            ConditionSheet(
                ui("skills.when_fires", SkillText.title(slot.skill)),
                ui("skills.in_slot", chosen.index + 1),
                slot.condition,
                flask = false,
                icon = { if (skill != null && index != null) NeonSkillIcon(skill, index.skillGrowth.tier(skill, skills, hero?.stats.orEmpty()), SHEET_ICON) },
                onDismiss = { pick = null },
                onSwap = { pick = Pick.Slot(SkillKind.ACTIVE, chosen.index) },
            ) { condition ->
                pick = null
                vm.slotSkill(SkillKind.ACTIVE.name, chosen.index, slot.skill, condition.token)
            }
        }

        is Pick.Belt -> if (hero != null && index != null) {
            worn(hero, index, skills).getOrNull(chosen.index)?.let { (view, flask) ->
                ConditionSheet(
                    ui("skills.when_drunk", view.title),
                    ui("skills.belt_place", chosen.index + 1),
                    flask.condition,
                    flask = true,
                    icon = { FlaskBottle(flask.sheet.kind, 1f, true, Modifier.size(SHEET_FLASK_WIDTH, SHEET_ICON)) },
                    onDismiss = { pick = null },
                ) { condition ->
                    pick = null
                    vm.flaskCondition(chosen.index, condition.token)
                }
            }
        }

        null -> Unit
    }
    if (exchanging && hero != null && index != null && body != null) Exchange(game, vm, index, hero, pages, body.stats, onExchanged = { page = it }) { exchanging = false }
}

/** The slot a skill stands in, as a word — «Слот 2 · Как готово» — or null. */
internal fun slotOf(skills: HeroSkills, code: String): String? {
    skills.active.indexOfFirst { it?.skill == code }.takeIf { it >= 0 }?.let { index ->
        return ui("skills.in_slot", index + 1) + " · " + ConditionText.short(skills.active[index]!!.condition)
    }
    return skills.passive.indexOfFirst { it == code }.takeIf { it >= 0 }?.let { ui("skills.in_passive", it + 1) }
}

@Composable internal fun Tabs(selected: GrimoireSection, onSelect: (GrimoireSection) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().background(Abyss)) {
            GrimoireSection.entries.forEach { entry ->
                val on = entry == selected
                Column(
                    Modifier.weight(1f).selectable(selected = on, role = Role.Tab, onClick = { onSelect(entry) }).padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(entry.icon, null, tint = if (on) Gold else Muted, modifier = Modifier.size(20.dp))
                    Text(ui(entry.title), color = if (on) GoldBright else Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (on) Gold else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = PanelRaised)
    }
}

/**
 * Подсказка данных цепочки Гримуара: что значат знаки строки, уровень героя, открытые активные и пассивные ячейки из всех и уровень
 * следующей ячейки каждого вида ([SkillRules.nextSlotLevel]), максимум маны, резерв маны аурами [mana] и здоровья [life] - долей и
 * числом; нулевого резерва нет.
 */
private fun trailTip(index: ContentIndex, level: Int, maxMana: Double, mana: PoolReserve, life: PoolReserve): Tip {
    val rules = index.skillRules
    fun next(kind: SkillKind): String? = rules.nextSlotLevel(kind, level)?.let { ui("skills.opens_at", it) }
    fun share(reserve: PoolReserve): String = ui("skills.data_tip.share", number(reserve.percent), number(round(reserve.held)))
    return Tip(
        ui("skills.data_tip.title"),
        ui("skills.data_tip.body"),
        ManaBlue,
        listOfNotNull(
            ui("skills.data_tip.level") to "$level",
            ui("skills.data_tip.actives") to ui("skills.data_tip.open", rules.activeSlots(level), rules.slots(SkillKind.ACTIVE).size),
            ui("skills.data_tip.passives") to ui("skills.data_tip.open", rules.passiveSlots(level), rules.slots(SkillKind.PASSIVE).size),
            next(SkillKind.ACTIVE)?.let { ui("skills.data_tip.next_active") to it },
            next(SkillKind.PASSIVE)?.let { ui("skills.data_tip.next_passive") to it },
            ui("skills.data_tip.mana") to number(maxMana),
            (ui("skills.data_tip.reserve") to share(mana)).takeIf { mana.percent > 0 },
            (ui("skills.data_tip.life") to share(life)).takeIf { life.percent > 0 },
        ),
    )
}

/**
 * Ячейки умений: активные и пассивные - одна сетка в две колонки, ячейка -
 * строка [SLOT_ROW] (иконка [SLOT_ICON] с уровнем внутри, имя и строка условия или резерва). Ряд с нечётным числом ячеек
 * дополняется пустым местом - ширина ячеек одна у обоих разделов. Касание пустой или пассивной ячейки - выбор умения, занятой
 * активной - лист условия (там же «Заменить умение»); долгое нажатие на занятую - карточка её умения, о нём - одна
 * подсказка «Осмотра» под панелью. У занятой активной - номер в очереди проверки боя ([SlotCondition.queue]), под активными -
 * подсказка порядка: умения с условием - первыми.
 */
@Composable internal fun Slots(index: ContentIndex, skills: HeroSkills, level: Int, stats: Map<String, Double>, onPick: (Pick) -> Unit) {
    val rules = index.skillRules
    val inspect = rememberSkillInspect()
    ForgePanel {
        val activeSlots = rules.slots(SkillKind.ACTIVE)
        val actives = activeSlots.indices.map { skills.active.getOrNull(it) }
        val queue = remember(actives) { SlotCondition.queue(actives.map { it?.condition }) }
        Engraved(ui("skills.actives", actives.count { it != null }, rules.activeSlots(level)))
        SlotGrid(activeSlots) { at, opens, modifier ->
            val slot = actives[at]
            val skill = slot?.let { index.skills.byCode[it.skill] }
            SlotTile(
                skill,
                skills.level(skill?.code.orEmpty()),
                skill?.let { index.skillGrowth.tier(it, skills, stats) } ?: 1,
                null,
                opens,
                level,
                modifier,
                onTap = { onPick(if (slot == null) Pick.Slot(SkillKind.ACTIVE, at) else Pick.Condition(at)) },
                onPeek = slot?.let { { inspect(SkillPeek(it.skill, condition = it.condition)) } },
                condition = slot?.condition,
                queue = queue[at],
            )
        }
        if (actives.any { it != null }) OrderHint()
        val passiveSlots = rules.slots(SkillKind.PASSIVE)
        val passives = passiveSlots.indices.map { skills.passive.getOrNull(it) }
        Engraved(ui("skills.passives", passives.count { it != null }, rules.passiveSlots(level)))
        SlotGrid(passiveSlots) { at, opens, modifier ->
            val skill = passives[at]?.let { index.skills.byCode[it] }
            SlotTile(
                skill,
                skills.level(skill?.code.orEmpty()),
                skill?.let { index.skillGrowth.tier(it, skills, stats) } ?: 1,
                skill?.takeIf { it.reserve > 0 }?.let { ui("skills.reserve", number(it.reserve)) },
                opens,
                level,
                modifier,
                onTap = { onPick(Pick.Slot(SkillKind.PASSIVE, at)) },
                onPeek = skill?.let { { inspect(SkillPeek(it.code)) } },
                quiet = true,
            )
        }
        if (actives.any { it != null } || passives.any { it != null }) HoldHint(HoldSubject.SKILL)
    }
}

/** Ячейки [opens] (уровень открытия каждой) сеткой в [SLOT_COLUMNS] колонки: недостающее в последнем ряду - пустое место. */
@Composable private fun SlotGrid(opens: List<Int>, cell: @Composable (at: Int, opens: Int, modifier: Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SLOT_GAP)) {
        opens.indices.chunked(SLOT_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SLOT_GAP)) {
                row.forEach { at -> cell(at, opens[at], Modifier.weight(1f)) }
                repeat(SLOT_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Подсказка порядка проверки слотов в бою: с условием - первыми, дальше сверху вниз. */
@Composable private fun OrderHint() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Info, null, tint = Muted, modifier = Modifier.size(14.dp))
        Text(ui("skills.condition.order_hint"), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/**
 * Одна ячейка - строка: знак умения «Неон рун» с тиром [tier] и уровнем [level] в углу внутри, имя и под ним условие ячейки
 * [condition] (знак вида его цветом и краткий итог) или строка [line] - резерв ауры (приглушённо, [quiet]); [queue] - номер в очереди
 * проверки боя в левом верхнем углу знака. Пустая - пунктир с «+», «Пусто · коснитесь»; закрытая - цепь и «с ур. N».
 * [onPeek] - долгое нажатие, карточка умения ячейки.
 */
@Composable internal fun SlotTile(
    skill: SkillDefinition?,
    level: Int,
    tier: Int,
    line: String?,
    opens: Int,
    heroLevel: Int,
    modifier: Modifier,
    onTap: () -> Unit,
    onPeek: (() -> Unit)? = null,
    quiet: Boolean = false,
    condition: SlotCondition? = null,
    queue: Int? = null,
) {
    val locked = heroLevel < opens
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.height(SLOT_ROW).clip(shape).background(Abyss, shape).border(1.dp, Bronze, shape)
            .combinedClickable(enabled = !locked, onLongClick = onPeek, onClick = onTap).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            locked -> {
                SlotHollow(ForgeGlyphs.Chain, Muted)
                SlotWords(ui("skills.slot_locked"), Muted, ui("skills.opens_at", opens), Muted)
            }

            skill == null -> {
                SlotHollow(ForgeGlyphs.Plus, Gold)
                SlotWords(ui("skills.empty_slot"), Parchment, ui("skills.empty_tap"), Muted)
            }

            else -> {
                Box(Modifier.size(SLOT_ICON)) {
                    NeonSkillIcon(skill, tier, SLOT_ICON)
                    Text(
                        "$level",
                        color = GoldBright,
                        fontSize = 8.5.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).background(Color.Black.copy(alpha = .62f), RoundedCornerShape(3.dp)).padding(horizontal = 2.dp),
                    )
                    queue?.let { QueueBadge(it, Modifier.align(Alignment.TopStart)) }
                }
                if (condition != null) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        SlotName(SkillText.title(skill.code), Parchment)
                        ConditionLine(condition)
                    }
                } else {
                    SlotWords(SkillText.title(skill.code), Parchment, line, if (quiet) Muted else Rune)
                }
            }
        }
    }
}

/** Пустая или закрытая ячейка: пунктирная рамка размера знака умения со значком [icon]. */
@Composable private fun SlotHollow(icon: ImageVector, tint: Color) {
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.size(SLOT_ICON).dashedBorder(Bronze, shape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

/** Имя ячейки [title] (11sp, в ширину - кегль вниз, а не многоточие) и строка под ним [line] (10sp). */
@Composable private fun RowScope.SlotWords(title: String, color: Color, line: String?, lineColor: Color) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        SlotName(title, color)
        line?.let { Text(it, color = lineColor, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** Имя ячейки: 11sp, в ширину - кегль вниз до [NAME_MIN], а не многоточие. */
@Composable private fun SlotName(title: String, color: Color) {
    WordFitText(title, color, MaterialTheme.typography.labelMedium, NAME_MAX, NAME_MIN, Modifier.fillMaxWidth(), maxLines = 1, textAlign = TextAlign.Start)
}

/** Номер слота в очереди проверки боя: кружок в углу знака умения. */
@Composable private fun QueueBadge(number: Int, modifier: Modifier) {
    val label = ui("skills.condition.queue", number)
    Box(
        modifier.size(QUEUE_BADGE).background(Abyss, CircleShape).border(1.dp, Rune, CircleShape).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", color = Rune, fontSize = 8.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/** Кегль имени умения в ячейке: обычный и наименьший, до которого он сходит, чтобы имя встало целиком. */
private val NAME_MAX = 11.sp
private val NAME_MIN = 8.sp

/** Ячейка Гримуара: строка высотой [SLOT_ROW], знак [SLOT_ICON], [SLOT_COLUMNS] колонки с зазором [SLOT_GAP]. */
private val SLOT_ROW = 48.dp
private val SLOT_ICON = 32.dp
private val SLOT_GAP = 6.dp
private const val SLOT_COLUMNS = 2

/** Кружок номера очереди на знаке ячейки. */
private val QUEUE_BADGE = 13.dp

/** Знак в шапке листа условия: умение - [SHEET_ICON], фляга - бутыль [SHEET_FLASK_WIDTH] × [SHEET_ICON]. */
private val SHEET_ICON = 40.dp
private val SHEET_FLASK_WIDTH = 24.dp

/**
 * A page of the class's book: what the skill is, its level, where it stands, and what comes next. Иконка «Неон рун»
 * с тиром, тонкая полоса опыта и подпись роста: опыт, ожидание требования, потолок или книга в опыт.
 */
@Composable internal fun Page(game: GameUi, index: ContentIndex, skill: SkillDefinition, skills: HeroSkills, heroLevel: Int, stats: Map<String, Double>, slot: String?, onOpen: () -> Unit) {
    val books = bookCount(game, skill.code)
    val view = remember(skill, skills, heroLevel, stats) { SkillGrowthView.of(index, skill, skills, heroLevel, stats) }
    val learned = view.level
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).depthPanel(shape).border(1.dp, if (slot != null) Gold.copy(alpha = .6f) else PanelRaised, shape)
            .clickable(onClick = onOpen).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeonSkillIcon(skill, view.tier, 48.dp, dim = learned == 0)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(skillKindLine(skill) + " · " + tierLine(view), color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(SkillText.title(skill.code), color = if (learned > 0) GoldBright else Parchment, style = MaterialTheme.typography.titleSmall)
            slot?.let { Text(it, color = Rune, style = MaterialTheme.typography.labelSmall) }
            if (learned > 0) XpBar(index, skill, view, compact = true)
            Text(
                nextLine(index, skill, view, heroLevel, books),
                color = if (books > 0 && !view.capped && !view.waiting) Vital else Muted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(if (learned > 0) ui("skills.level_short", learned) else "—", color = if (learned > 0) Gold else Muted, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Умение, что можно изучить: страница неизученного умения, чья книга в сумке, и под ней «Изучить» - тот же путь, что
 * лист умения ([BookButton]: погашена с причиной, если герою не хватает уровня или атрибутов).
 */
@Composable private fun Learnable(game: GameUi, index: ContentIndex, skill: SkillDefinition, skills: HeroSkills, heroLevel: Int, stats: Map<String, Double>, onOpen: () -> Unit, onLearn: () -> Unit) {
    val view = remember(skill, skills, heroLevel, stats) { SkillGrowthView.of(index, skill, skills, heroLevel, stats) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Page(game, index, skill, skills, heroLevel, stats, null, onOpen)
        BookButton(game, index, skill, view, heroLevel, stats, onLearn)
    }
}

/**
 * What stands before the next level of a skill: неизученное - книга или уровень открытия; изученное - потолок, ожидание
 * требования, книга в опыт или опыт к следующему уровню.
 */
internal fun nextLine(index: ContentIndex, skill: SkillDefinition, view: SkillGrowthView, heroLevel: Int, books: Long): String = when {
    view.level == 0 && books > 0 -> ui("skills.book_learn", books)
    view.level == 0 && heroLevel < skill.unlock -> ui("skills.no_book_opens", skill.unlock)
    view.level == 0 -> ui("skills.no_book")
    view.capped || view.waiting -> xpCaption(index, skill, view)
    books > 0 -> ui("skills.book_ready_xp", books, number(view.bookXp))
    else -> xpCaption(index, skill, view)
}

/** A level's requirements as the page prints them: the hero's level and each attribute. */
internal fun needLine(index: ContentIndex, skill: SkillDefinition, level: Int): String {
    val need = index.skillRules.need(skill, level)
    return (listOf(ui("skills.need_level", need.heroLevel)) + need.attributes.map { (stat, amount) -> "${statTitle(stat).lowercase()} $amount" }).joinToString(" · ")
}

/** How many books of [code] lie in the bag: the bag is keyed by the book's item code. */
internal fun bookCount(game: GameUi, code: String): Long = game.bagAmount(SkillRules.book(code)) ?: 0L

/** Every book the content knows, of every class: what the bag is counted by. */
internal fun books(index: ContentIndex): List<Item> = index.itemsByCategory[Item.BOOK].orEmpty()

/** The books in the bag, of every class, and the trade of three for one of the class's own. */
@Composable internal fun Books(game: GameUi, index: ContentIndex, pages: List<SkillDefinition>, onExchange: () -> Unit) {
    val owned = books(index).sumOf { game.bagAmount(it.code.value) ?: 0L }
    val rule = index.skills.rules.exchange
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(ForgeGlyphs.Tome, null, tint = Gold, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(ui("skills.books_owned", owned), color = GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("skills.exchange_hint", rule.books))
            }
            ForgeButton(state = ButtonState.of(owned >= rule.books && pages.isNotEmpty() && !game.busy), onClick = onExchange, kind = ButtonKind.Secondary) { Text(ui("skills.exchange")) }
        }
    }
}
