package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.display.SkillGrowthView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.skills.GrimoireViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** The grimoire's two sections (2.78.0, the owner's mockup A «Гримуар»): the class's skills, and the belt. */
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
 * The grimoire (2.78.0, server 0.69.0): the class's skills as the pages of a book and the belt of flasks.
 *
 * On top are the slots — three active, each with when it fires by itself, and two passive — opening with
 * the hero's level; under them every skill of the class, its level, and what the next book of it asks.
 * A page opens its skill whole: what it does now and at the next level, the book to read, the slot to
 * put it in. The belt holds the three flasks worn, each with when it is drunk by itself. Every rule is
 * the rules module's (3.0.0), read off the content on screen; the page says beforehand what the server will say.
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val mana = body?.maxMana ?: 0.0
            val reserved = if (body != null && index != null) Loadout.of(skills, index, classCode, emptyList(), hero?.stats.orEmpty()).reserved(body) else 0.0
            ScreenHeader(
                ui("skills.title"),
                listOfNotNull(
                    classCode.takeIf { it.isNotBlank() }?.let(::classTitle),
                    ui("skills.level", level),
                    ui("skills.mana", number(mana)) + if (reserved > 0) " " + ui("skills.reserved", number(reserved)) else "",
                ).joinToString(" · "),
                ForgeGlyphs.Grimoire,
            )
        }
        item { Tabs(section) { section = it } }
        if (hero == null || body == null || index == null) {
            item { InfoCard(ui("common.loading"), ui("skills.loading_hint")) }
        } else {
            when (section) {
                GrimoireSection.SKILLS -> {
                    item { Slots(index, skills, level, body.stats) { pick = it } }
                    item { Caption(ui("skills.book_of", classTitle(classCode))) }
                    // Only what the hero has learned (2.81.0): an unread book's skill appears once its book is read.
                    items(pages.filter { !it.common && skills.level(it.code) > 0 }, key = { it.code }) { skill ->
                        Page(game, index, skill, skills, level, body.stats, slotOf(skills, skill.code)) { page = skill.code }
                    }
                    // Общие умения (0.14.0, SKILL-02) - своим разделом: любому классу, атрибут - свой у умения
                    val shared = pages.filter { it.common && skills.level(it.code) > 0 }
                    if (shared.isNotEmpty()) item { Caption(ui("skills.book_common")) }
                    items(shared, key = { it.code }) { skill ->
                        Page(game, index, skill, skills, level, body.stats, slotOf(skills, skill.code)) { page = skill.code }
                    }
                    item { Books(game, index, pages) { exchanging = true } }
                }

                GrimoireSection.BELT -> {
                    item { MutedText(ui("skills.belt_hint")) }
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
            ConditionPicker(
                ui("skills.when_fires", SkillText.title(slot.skill)),
                slot.condition,
                flask = false,
                onDismiss = { pick = null },
                // Замена умения ячейки (0.14.5, #38) - из её листа условия: отдельной надписи «заменить» в ячейке нет
                onSwap = { pick = Pick.Slot(SkillKind.ACTIVE, chosen.index) },
            ) { condition ->
                pick = null
                vm.slotSkill(SkillKind.ACTIVE.name, chosen.index, slot.skill, condition?.name)
            }
        }

        is Pick.Belt -> ConditionPicker(ui("skills.when_drunk"), skills.flasks.getOrNull(chosen.index), flask = true, onDismiss = { pick = null }) { condition ->
            pick = null
            vm.flaskCondition(chosen.index, condition?.name)
        }

        null -> Unit
    }
    if (exchanging && hero != null && index != null && body != null) Exchange(game, vm, index, hero, pages, body.stats, onExchanged = { page = it }) { exchanging = false }
}

/** The slot a skill stands in, as a word — «Слот 2 · Как готово» — or null. */
internal fun slotOf(skills: HeroSkills, code: String): String? {
    skills.active.indexOfFirst { it?.skill == code }.takeIf { it >= 0 }?.let { index ->
        return ui("skills.in_slot", index + 1) + " · " + conditionTitle(skills.active[index]!!.condition)
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
 * Ячейки умений (0.14.5, #38, макет «Строки в две колонки», вариант A): активные и пассивные - одна сетка в две колонки, ячейка -
 * строка [SLOT_ROW] (иконка [SLOT_ICON] с уровнем внутри, имя и строка условия или резерва). Ряд с нечётным числом ячеек
 * дополняется пустым местом - ширина ячеек одна у обоих разделов. Касание пустой или пассивной ячейки - выбор умения, занятой
 * активной - лист условия (там же «Заменить умение»); долгое нажатие на занятую - карточка её умения (0.14.4, #25), о нём - одна
 * подсказка «Осмотра» под панелью.
 */
@Composable internal fun Slots(index: ContentIndex, skills: HeroSkills, level: Int, stats: Map<String, Double>, onPick: (Pick) -> Unit) {
    val rules = index.skills.rules
    val inspect = rememberSkillInspect()
    ForgePanel {
        val actives = rules.activeSlots.indices.map { skills.active.getOrNull(it) }
        Engraved(ui("skills.actives", actives.count { it != null }, index.skillRules.activeSlots(level)))
        SlotGrid(rules.activeSlots) { at, opens, modifier ->
            val slot = actives[at]
            val skill = slot?.let { index.skills.byCode[it.skill] }
            SlotTile(
                skill,
                skills.level(skill?.code.orEmpty()),
                skill?.let { index.skillGrowth.tier(it, skills, stats) } ?: 1,
                slot?.condition?.let(::conditionTitle),
                opens,
                level,
                modifier,
                onTap = { onPick(if (slot == null) Pick.Slot(SkillKind.ACTIVE, at) else Pick.Condition(at)) },
                onPeek = slot?.let { { inspect(SkillPeek(it.skill, condition = it.condition)) } },
            )
        }
        val passives = rules.passiveSlots.indices.map { skills.passive.getOrNull(it) }
        Engraved(ui("skills.passives", passives.count { it != null }, index.skillRules.passiveSlots(level)))
        SlotGrid(rules.passiveSlots) { at, opens, modifier ->
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

/**
 * Одна ячейка - строка: знак умения «Неон рун» с тиром [tier] и уровнем [level] в углу внутри, имя и под ним [line] - условие
 * ячейки ([Rune]) или резерв ауры (приглушённо, [quiet]); пустая - пунктир с «+», «Пусто · коснитесь»; закрытая - цепь и «с ур. N».
 * [onPeek] (0.14.4, #25) - долгое нажатие, карточка умения ячейки.
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
                }
                SlotWords(SkillText.title(skill.code), Parchment, line, if (quiet) Muted else Rune)
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
        WordFitText(title, color, MaterialTheme.typography.labelMedium, NAME_MAX, NAME_MIN, Modifier.fillMaxWidth(), maxLines = 1, textAlign = TextAlign.Start)
        line?.let { Text(it, color = lineColor, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/** Кегль имени умения в ячейке: обычный и наименьший, до которого он сходит, чтобы имя встало целиком. */
private val NAME_MAX = 11.sp
private val NAME_MIN = 8.sp

/** Ячейка Гримуара (0.14.5, #38): строка высотой [SLOT_ROW], знак [SLOT_ICON], [SLOT_COLUMNS] колонки с зазором [SLOT_GAP]. */
private val SLOT_ROW = 48.dp
private val SLOT_ICON = 32.dp
private val SLOT_GAP = 6.dp
private const val SLOT_COLUMNS = 2

/**
 * A page of the class's book: what the skill is, its level, where it stands, and what comes next. С 4.4.0 - иконка «Неон рун»
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
 * What stands before the next level of a skill (4.4.0): неизученное - книга или уровень открытия; изученное - потолок, ожидание
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
            ForgeOutlinedButton(enabled = owned >= rule.books && pages.isNotEmpty() && !game.busy, onClick = onExchange) { Text(ui("skills.exchange")) }
        }
    }
}
