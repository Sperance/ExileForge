package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.FlaskKind
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.skills.GrimoireViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
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
            val reserved = if (body != null && book != null) Loadout.of(skills, book, classCode, emptyList()).reserved(body) else 0.0
            ScreenHeader(
                ui("skills.title"),
                listOfNotNull(
                    classCode.takeIf { it.isNotBlank() }?.let(::classTitle),
                    ui("skills.level", level),
                    ui("skills.mana", number(mana)) + if (reserved > 0) " " + ui("skills.reserved", number(reserved)) else "",
                ).joinToString(" · "),
                ForgeGlyphs.Grimoire,
                guide = Guide.GRIMOIRE,
            )
        }
        item { Tabs(section) { section = it } }
        if (hero == null || body == null || index == null) {
            item { InfoCard(ui("common.loading"), ui("skills.loading_hint")) }
        } else {
            when (section) {
                GrimoireSection.SKILLS -> {
                    item { Slots(index, skills, level) { pick = it } }
                    item { Caption(ui("skills.book_of", classTitle(classCode))) }
                    // Only what the hero has learned (2.81.0): an unread book's skill appears once its book is read.
                    items(pages.filter { skills.level(it.code) > 0 }, key = { it.code }) { skill ->
                        Page(game, index, skill, skills.level(skill.code), level, slotOf(skills, skill.code)) { page = skill.code }
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
            ConditionPicker(ui("skills.when_fires", SkillText.title(slot.skill)), slot.condition, flask = false, onDismiss = { pick = null }) { condition ->
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
    if (exchanging && hero != null && index != null) Exchange(game, vm, index, hero, pages) { exchanging = false }
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

/** The five slots: three active over two passive, each open from its level, a tap to fill, change or empty it. */
@Composable internal fun Slots(index: ContentIndex, skills: HeroSkills, level: Int, onPick: (Pick) -> Unit) {
    val rules = index.skills.rules
    ForgePanel {
        val actives = rules.activeSlots.indices.map { skills.active.getOrNull(it) }
        Engraved(ui("skills.actives", actives.count { it != null }, index.skillRules.activeSlots(level)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rules.activeSlots.forEachIndexed { at, opens ->
                val slot = actives[at]
                val skill = slot?.let { index.skills.byCode[it.skill] }
                SlotTile(
                    skill,
                    skills.level(skill?.code.orEmpty()),
                    slot?.condition?.let(::conditionTitle),
                    opens,
                    level,
                    Modifier.weight(1f),
                    onTap = { onPick(if (slot == null) Pick.Slot(SkillKind.ACTIVE, at) else Pick.Condition(at)) },
                    onSwap = { onPick(Pick.Slot(SkillKind.ACTIVE, at)) },
                )
            }
        }
        val passives = rules.passiveSlots.indices.map { skills.passive.getOrNull(it) }
        Engraved(ui("skills.passives", passives.count { it != null }, index.skillRules.passiveSlots(level)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rules.passiveSlots.forEachIndexed { at, opens ->
                val skill = passives[at]?.let { index.skills.byCode[it] }
                SlotTile(
                    skill,
                    skills.level(skill?.code.orEmpty()),
                    skill?.takeIf { it.reserve > 0 }?.let { ui("skills.reserve", number(it.reserve)) },
                    opens,
                    level,
                    Modifier.weight(1f),
                    onTap = { onPick(Pick.Slot(SkillKind.PASSIVE, at)) },
                )
            }
        }
    }
}

/**
 * One slot: the skill's mark and level, its name and the line under it — a slot's condition, an aura's
 * reserve — or a plus while empty, or the level it opens at while locked. [onSwap] puts another in.
 */
@Composable internal fun SlotTile(
    skill: SkillDefinition?,
    level: Int,
    line: String?,
    opens: Int,
    heroLevel: Int,
    modifier: Modifier,
    onTap: () -> Unit,
    onSwap: (() -> Unit)? = null,
) {
    val locked = heroLevel < opens
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier.clip(shape).background(if (skill != null) PanelRaised else Abyss, shape)
            .border(1.dp, if (skill != null) Gold.copy(alpha = .7f) else Bronze.copy(alpha = .5f), shape)
            .clickable(enabled = !locked, onClick = onTap).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        when {
            locked -> {
                Icon(ForgeGlyphs.Chain, null, tint = Muted, modifier = Modifier.size(22.dp))
                Text(ui("skills.opens_at", opens), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }

            skill == null -> {
                Icon(ForgeGlyphs.Plus, null, tint = Gold, modifier = Modifier.size(22.dp))
                Text(ui("skills.empty_slot"), color = Muted, style = MaterialTheme.typography.labelSmall)
            }

            else -> {
                Box {
                    SkillGlyph(skill.icon, Modifier.size(28.dp), GoldBright)
                    Text(
                        "$level",
                        color = Ink,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp).background(Gold, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp),
                    )
                }
                Text(SkillText.title(skill.code), color = GoldBright, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                line?.let { Text(it, color = Rune, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                onSwap?.let { Text(ui("skills.swap"), color = Muted, fontSize = 10.sp, modifier = Modifier.clickable(onClick = it)) }
            }
        }
    }
}

/**
 * A page of the class's book: what the skill is, its level, where it stands, and what comes next —
 * the requirements of the next book, a book ready to be read, or the level it opens at.
 */
@Composable internal fun Page(game: GameUi, index: ContentIndex, skill: SkillDefinition, learned: Int, heroLevel: Int, slot: String?, onOpen: () -> Unit) {
    val books = bookCount(game, skill.code)
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Panel, shape).border(1.dp, if (slot != null) Gold.copy(alpha = .6f) else PanelRaised, shape)
            .clickable(onClick = onOpen).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkillGlyph(skill.icon, Modifier.size(34.dp), if (learned > 0) GoldBright else Muted)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(skillKindLine(skill), color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(SkillText.title(skill.code), color = if (learned > 0) GoldBright else Parchment, style = MaterialTheme.typography.titleSmall)
            slot?.let { Text(it, color = Rune, style = MaterialTheme.typography.labelSmall) }
            Text(
                nextLine(index, skill, learned, heroLevel, books),
                color = if (books > 0 && learned < SkillRules.MAX_LEVEL) Vital else Muted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(if (learned > 0) ui("skills.level_short", learned) else "—", color = if (learned > 0) Gold else Muted, style = MaterialTheme.typography.titleMedium)
    }
}

/** What stands before the next level of a skill: the last one, a book to read, what its book asks, or the level it opens at. */
internal fun nextLine(index: ContentIndex, skill: SkillDefinition, learned: Int, heroLevel: Int, books: Long): String = when {
    learned >= SkillRules.MAX_LEVEL -> ui("skills.max_level")
    books > 0 -> ui("skills.book_ready", books, learned + 1)
    learned == 0 && heroLevel < skill.unlock -> ui("skills.no_book_opens", skill.unlock)
    learned == 0 -> ui("skills.no_book")
    else -> ui("skills.next_book", needLine(index, skill, learned + 1))
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
    val owned = books(index).sumOf { game.bagAmount(it.code) ?: 0L }
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
