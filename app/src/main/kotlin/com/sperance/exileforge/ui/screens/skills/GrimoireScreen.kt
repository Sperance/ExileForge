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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.sperance.exileforge.core.campaign.Combatant
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.FlaskKind
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.character.Sheet
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.documentTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.inventoryDocument
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.EquipmentInstance
import com.sperance.exileforge.core.model.skills.HeroSkills
import com.sperance.exileforge.core.model.skills.Scale
import com.sperance.exileforge.core.model.skills.SkillDefinition
import com.sperance.exileforge.core.model.skills.SkillKind
import com.sperance.exileforge.core.model.skills.SkillType
import com.sperance.exileforge.core.model.skills.SlotCondition
import com.sperance.exileforge.presentation.ForgeViewModel
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*

/** The grimoire's two sections (2.78.0, the owner's mockup A «Гримуар»): the class's skills, and the belt. */
private enum class GrimoireSection(val title: String, val icon: ImageVector) {
    SKILLS("skills.section_skills", ForgeGlyphs.Grimoire), BELT("skills.section_belt", ForgeGlyphs.Flask)
}

/** What a sheet of the grimoire is choosing: a skill for a slot, when a slot fires, when a flask is drunk. */
private sealed interface Pick {
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
 * the server's; the page says beforehand what it will say.
 */
@Composable fun GrimoireScreen(s: ForgeState, vm: ForgeViewModel) {
    var section by rememberSaveable(s.play.characterId) { mutableStateOf(GrimoireSection.SKILLS) }
    var page by remember(s.play.characterId) { mutableStateOf<String?>(null) }
    var pick by remember(s.play.characterId) { mutableStateOf<Pick?>(null) }
    var exchanging by remember(s.play.characterId) { mutableStateOf(false) }
    var belt by rememberSaveable(s.play.characterId) { mutableIntStateOf(0) }
    LaunchedEffect(s.play.characterId, s.account.sessionEpoch) { vm.ensureHero() }
    val hero = s.play.hero
    val book = s.world.skills
    val classCode = s.heroClass?.code.orEmpty()
    val skills = hero?.character?.skills ?: HeroSkills()
    val level = hero?.character?.level ?: 1
    val body = remember(hero?.sheet) { hero?.let { Combatant(it.sheet.stats, it.sheet.level) } }
    val pages = remember(book, classCode) { book.ofClass(classCode) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val mana = body?.maxMana ?: 0.0
            val reserved = body?.let { Loadout.of(skills, book, classCode, emptyList()).reserved(it) } ?: 0.0
            ScreenHeader(ui("skills.title"), listOfNotNull(s.heroClass?.title, ui("skills.level", level),
                ui("skills.mana", number(mana)) + if (reserved > 0) " " + ui("skills.reserved", number(reserved)) else "").joinToString(" · "), ForgeGlyphs.Grimoire)
        }
        item { Tabs(section) { section = it } }
        if (hero == null || body == null) item { InfoCard(ui("common.loading"), ui("skills.loading_hint")) }
        else when (section) {
            GrimoireSection.SKILLS -> {
                item { Slots(s, skills, level) { pick = it } }
                item { Caption(ui("skills.book_of", s.heroClass?.title.orEmpty())) }
                items(pages, key = { it.code }) { skill ->
                    Page(s, skill, skills.level(skill.code), level, body.stats, slotOf(skills, skill.code)) { page = skill.code }
                }
                item { Books(s, pages) { exchanging = true } }
            }
            GrimoireSection.BELT -> {
                item { MutedText(ui("skills.belt_hint")) }
                item { Belt(s, skills, body, belt) { belt = it } }
                item { BeltDetail(s, skills, body, belt) { pick = Pick.Belt(belt) } }
            }
        }
    }
    page?.let { code -> book.byCode[code] }?.let { skill ->
        if (hero != null && body != null) SkillSheet(s, vm, skill, skills, level, body.stats, onSlot = { pick = it }) { page = null }
    }
    when (val chosen = pick) {
        is Pick.Slot -> SkillPicker(s, chosen, skills, pages, onDismiss = { pick = null }) { code -> pick = null; vm.slotSkill(chosen.kind.name, chosen.index, code) }
        is Pick.Condition -> skills.active.getOrNull(chosen.index)?.let { slot ->
            ConditionPicker(ui("skills.when_fires", SkillText.title(slot.skill)), slot.condition, flask = false, onDismiss = { pick = null }) { condition ->
                pick = null; vm.slotSkill(SkillKind.ACTIVE.name, chosen.index, slot.skill, condition?.name)
            }
        }
        is Pick.Belt -> ConditionPicker(ui("skills.when_drunk"), skills.flasks.getOrNull(chosen.index), flask = true, onDismiss = { pick = null }) { condition ->
            pick = null; vm.flaskCondition(chosen.index, condition?.name)
        }
        null -> Unit
    }
    if (exchanging && hero != null) Exchange(s, vm, pages, level) { exchanging = false }
}

/** The slot a skill stands in, as a word — «Слот 2 · Как готово» — or null. */
private fun slotOf(skills: HeroSkills, code: String): String? {
    skills.active.indexOfFirst { it?.skill == code }.takeIf { it >= 0 }?.let { index ->
        return ui("skills.in_slot", index + 1) + " · " + conditionTitle(skills.active[index]!!.condition)
    }
    return skills.passive.indexOfFirst { it == code }.takeIf { it >= 0 }?.let { ui("skills.in_passive", it + 1) }
}

@Composable private fun Tabs(selected: GrimoireSection, onSelect: (GrimoireSection) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().background(Abyss)) {
            GrimoireSection.entries.forEach { entry ->
                val on = entry == selected
                Column(Modifier.weight(1f).selectable(selected = on, role = Role.Tab, onClick = { onSelect(entry) }).padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
@Composable private fun Slots(s: ForgeState, skills: HeroSkills, level: Int, onPick: (Pick) -> Unit) {
    val rules = s.world.skills.rules
    ForgePanel {
        val actives = rules.activeSlots.indices.map { skills.active.getOrNull(it) }
        Engraved(ui("skills.actives", actives.count { it != null }, s.world.skills.activeSlots(level)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rules.activeSlots.forEachIndexed { index, opens ->
                val slot = actives[index]
                val skill = slot?.let { s.world.skills.byCode[it.skill] }
                SlotTile(skill, skills.level(skill?.code.orEmpty()), slot?.condition?.let(::conditionTitle), opens, level, Modifier.weight(1f),
                    onTap = { onPick(if (slot == null) Pick.Slot(SkillKind.ACTIVE, index) else Pick.Condition(index)) },
                    onSwap = { onPick(Pick.Slot(SkillKind.ACTIVE, index)) })
            }
        }
        val passives = rules.passiveSlots.indices.map { skills.passive.getOrNull(it) }
        Engraved(ui("skills.passives", passives.count { it != null }, s.world.skills.passiveSlots(level)))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rules.passiveSlots.forEachIndexed { index, opens ->
                val skill = passives[index]?.let { s.world.skills.byCode[it] }
                SlotTile(skill, skills.level(skill?.code.orEmpty()), skill?.takeIf { it.reserve > 0 }?.let { ui("skills.reserve", number(it.reserve)) },
                    opens, level, Modifier.weight(1f), onTap = { onPick(Pick.Slot(SkillKind.PASSIVE, index)) })
            }
        }
    }
}

/**
 * One slot: the skill's mark and level, its name and the line under it — a slot's condition, an aura's
 * reserve — or a plus while empty, or the level it opens at while locked. [onSwap] puts another in.
 */
@Composable private fun SlotTile(skill: SkillDefinition?, level: Int, line: String?, opens: Int, heroLevel: Int, modifier: Modifier,
                                 onTap: () -> Unit, onSwap: (() -> Unit)? = null) {
    val locked = heroLevel < opens
    val shape = RoundedCornerShape(8.dp)
    Column(modifier.clip(shape).background(if (skill != null) PanelRaised else Abyss, shape)
        .border(1.dp, if (skill != null) Gold.copy(alpha = .7f) else Bronze.copy(alpha = .5f), shape)
        .clickable(enabled = !locked, onClick = onTap).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                    Text("$level", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 4.dp).background(Gold, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp))
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
@Composable private fun Page(s: ForgeState, skill: SkillDefinition, learned: Int, heroLevel: Int, stats: Map<String, Double>, slot: String?, onOpen: () -> Unit) {
    val books = bookCount(s, skill.code)
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(Panel, shape).border(1.dp, if (slot != null) Gold.copy(alpha = .6f) else PanelRaised, shape)
        .clickable(onClick = onOpen).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        SkillGlyph(skill.icon, Modifier.size(34.dp), if (learned > 0) GoldBright else Muted)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(skillKindLine(skill), color = Muted, style = MaterialTheme.typography.labelSmall)
            Text(SkillText.title(skill.code), color = if (learned > 0) GoldBright else Parchment, style = MaterialTheme.typography.titleSmall)
            slot?.let { Text(it, color = Rune, style = MaterialTheme.typography.labelSmall) }
            Text(nextLine(s, skill, learned, heroLevel, stats, books), color = if (books > 0 && learned < Scale.MAX_LEVEL) Vital else Muted,
                style = MaterialTheme.typography.labelSmall)
        }
        Text(if (learned > 0) ui("skills.level_short", learned) else "—", color = if (learned > 0) Gold else Muted, style = MaterialTheme.typography.titleMedium)
    }
}

/** What stands before the next level of a skill: the last one, a book to read, what its book asks, or the level it opens at. */
private fun nextLine(s: ForgeState, skill: SkillDefinition, learned: Int, heroLevel: Int, stats: Map<String, Double>, books: Long): String = when {
    learned >= Scale.MAX_LEVEL -> ui("skills.max_level")
    books > 0 -> ui("skills.book_ready", books, learned + 1)
    learned == 0 && heroLevel < skill.unlock -> ui("skills.no_book_opens", skill.unlock)
    learned == 0 -> ui("skills.no_book")
    else -> ui("skills.next_book", needLine(s, skill, learned + 1))
}

/** A level's requirements as the page prints them: the hero's level and each attribute. */
private fun needLine(s: ForgeState, skill: SkillDefinition, level: Int): String {
    val need = s.world.skills.need(skill, level)
    return (listOf(ui("skills.need_level", need.heroLevel)) + need.attributes.map { (stat, amount) -> "${statTitle(stat).lowercase()} $amount" }).joinToString(" · ")
}

/** How many books of [code] lie in the bag. */
private fun bookCount(s: ForgeState, code: String): Long =
    s.world.books.firstOrNull { it.code == SkillDefinition.BOOK_PREFIX + code }?.let { s.bagAmount(it.id) } ?: 0L

/** The books in the bag, of every class, and the trade of three for one of the class's own. */
@Composable private fun Books(s: ForgeState, pages: List<SkillDefinition>, onExchange: () -> Unit) {
    val owned = s.world.books.sumOf { s.bagAmount(it.id) ?: 0L }
    val rule = s.world.skills.rules.exchange
    ForgePanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(ForgeGlyphs.Tome, null, tint = Gold, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(ui("skills.books_owned", owned), color = GoldBright, style = MaterialTheme.typography.titleSmall)
                MutedText(ui("skills.exchange_hint", rule.books))
            }
            OutlinedButton(enabled = owned >= rule.books && pages.isNotEmpty() && !s.busy, onClick = onExchange) { Text(ui("skills.exchange")) }
        }
    }
}

/**
 * A skill's page opened: what it does now — or at its first level, unlearned — and at the next, what the
 * next book asks and whether it is in the bag, and the slots it may go in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SkillSheet(s: ForgeState, vm: ForgeViewModel, skill: SkillDefinition, skills: HeroSkills, heroLevel: Int, stats: Map<String, Double>,
                                   onSlot: (Pick) -> Unit, onDismiss: () -> Unit) {
    val learned = skills.level(skill.code)
    val books = bookCount(s, skill.code)
    val next = (learned + 1).coerceAtMost(Scale.MAX_LEVEL)
    val need = s.world.skills.need(skill, next)
    val unmet = need.unmet(heroLevel, stats)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkillGlyph(skill.icon, Modifier.size(40.dp), GoldBright)
                Column(Modifier.weight(1f)) {
                    Text(SkillText.title(skill.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                    Text(skillKindLine(skill), color = Muted, style = MaterialTheme.typography.labelMedium)
                }
                Text(if (learned > 0) ui("skills.level_short", learned) else "—", color = Gold, style = MaterialTheme.typography.titleLarge)
            }
            val shown = learned.coerceAtLeast(1)
            Price(skill, shown)
            SkillLines(SkillText.lines(skill, shown))
            if (learned in 1 until Scale.MAX_LEVEL) {
                Caption(ui("skills.at_level", next))
                Price(skill, next)
                SkillLines(SkillText.lines(skill, next), ModBlue.copy(alpha = .75f))
            }
            if (learned < Scale.MAX_LEVEL) {
                Text(ui("skills.requires", next, needLine(s, skill, next)), color = if (unmet.isEmpty()) Parchment else LifeRed, style = MaterialTheme.typography.bodySmall)
                if (books > 0) Button(enabled = unmet.isEmpty() && !s.busy, onClick = { onDismiss(); vm.learnSkill(skill.code) }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright)) {
                    Text(ui(if (learned == 0) "skills.learn" else "skills.read", books))
                }
                else MutedText(ui(if (heroLevel < skill.unlock && learned == 0) "skills.no_book_opens" else "skills.no_book", skill.unlock))
            }
            if (learned > 0) SlotActions(s, skill, skills, heroLevel, onSlot, onDismiss) { kind, index ->
                onDismiss(); vm.slotSkill(kind.name, index, null)
            }
        }
    }
}

/** An active skill's price and pace, an aura's reserve: the chips over its lines. */
@Composable private fun Price(skill: SkillDefinition, level: Int) {
    val chips = listOfNotNull(
        SkillText.cost(skill.mana, level)?.let { ui("skills.cost", it) },
        skill.cooldown.takeIf { it > 0 }?.let { ui("skills.cooldown", fineNumber(it)) },
        skill.condition.takeIf { skill.kind == SkillKind.ACTIVE }?.let { ui("skills.default_condition", conditionTitle(it)) },
    )
    if (chips.isNotEmpty()) Text(chips.joinToString(" · "), color = Rune, style = MaterialTheme.typography.labelMedium)
}

@Composable private fun SkillLines(lines: List<String>, tone: Color = ModBlue) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Rhombus(tone, 4.dp)
                Text(line, color = tone, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Where a learned skill may go: each open slot of its kind, and out of the one it stands in. */
@Composable private fun SlotActions(s: ForgeState, skill: SkillDefinition, skills: HeroSkills, heroLevel: Int, onSlot: (Pick) -> Unit, onDismiss: () -> Unit,
                                    onEmpty: (SkillKind, Int) -> Unit) {
    val open = if (skill.kind == SkillKind.ACTIVE) s.world.skills.activeSlots(heroLevel) else s.world.skills.passiveSlots(heroLevel)
    val standing = if (skill.kind == SkillKind.ACTIVE) skills.active.indexOfFirst { it?.skill == skill.code } else skills.passive.indexOf(skill.code)
    Caption(ui(if (skill.kind == SkillKind.ACTIVE) "skills.slots_active" else "skills.slots_passive"))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (0 until open).forEach { index ->
            val here = index == standing
            OutlinedButton(enabled = !s.busy, onClick = {
                if (here) onEmpty(skill.kind, index) else { onDismiss(); onSlot(Pick.Slot(skill.kind, index)) }
            }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(if (here) ui("skills.take_out") else ui("skills.to_slot", index + 1), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

/** A skill for a slot: the class's learned ones of its kind, or nothing to empty it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SkillPicker(s: ForgeState, pick: Pick.Slot, skills: HeroSkills, pages: List<SkillDefinition>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    val offered = pages.filter { it.kind == pick.kind && skills.level(it.code) > 0 }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui(if (pick.kind == SkillKind.ACTIVE) "skills.pick_active" else "skills.pick_passive", pick.index + 1), color = GoldBright,
                style = MaterialTheme.typography.titleMedium)
            if (offered.isEmpty()) MutedText(ui("skills.nothing_learned"))
            offered.forEach { skill ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(PanelRaised).clickable(enabled = !s.busy) { onPick(skill.code) }
                    .padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    SkillGlyph(skill.icon, Modifier.size(26.dp), GoldBright)
                    Column(Modifier.weight(1f)) {
                        Text(SkillText.title(skill.code), color = GoldBright, style = MaterialTheme.typography.bodyMedium)
                        Text(skillKindLine(skill), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(ui("skills.level_short", skills.level(skill.code)), color = Gold)
                }
            }
            TextButton(enabled = !s.busy, onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) { Text(ui("skills.empty_it"), color = LifeRed) }
        }
    }
}

/** When a slot fires, or a flask is drunk, by itself: every condition — a flask's own kind's too, and the mana's only for a flask. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ConditionPicker(title: String, current: SlotCondition?, flask: Boolean, onDismiss: () -> Unit, onPick: (SlotCondition?) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = GoldBright, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
            val options: List<SlotCondition?> = (if (flask) listOf(null) else emptyList<SlotCondition?>()) + SlotCondition.entries.filter { flask || !it.flaskOnly }
            options.forEach { condition ->
                val on = condition == current
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (on) PanelRaised else Color.Transparent).clickable { onPick(condition) }
                    .padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RadioButton(selected = on, onClick = { onPick(condition) }, colors = RadioButtonDefaults.colors(selectedColor = Gold))
                    Text(condition?.let(::conditionTitle) ?: ui("skills.condition.KIND"), color = if (on) GoldBright else Parchment)
                }
            }
        }
    }
}

/** The belt as three flasks side by side, each filled to its charges, the chosen one lit; an empty place is a dashed outline. */
@Composable private fun Belt(s: ForgeState, skills: HeroSkills, body: Combatant, selected: Int, onSelect: (Int) -> Unit) {
    val flasks = worn(s, skills)
    Row(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF3A2A1A), Color(0xFF24190F))), RoundedCornerShape(10.dp))
        .border(1.dp, Bronze, RoundedCornerShape(10.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        flasks.forEachIndexed { index, pair ->
            val flask = pair?.second
            val on = index == selected
            Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) Gold.copy(alpha = .12f) else Color.Transparent)
                .border(1.dp, if (on) GoldBright else Color.Transparent, RoundedCornerShape(8.dp)).clickable { onSelect(index) }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlaskBottle(flask?.kind, 1f, flask != null, Modifier.size(34.dp, 56.dp))
                Text(if (flask != null) "${flask.maxCharges.toInt()} · ${flask.perUse(body).toInt()}" else ui("skills.belt_empty"),
                    color = if (flask != null) GoldBright else Muted, style = MaterialTheme.typography.labelSmall)
                Text(flask?.condition?.let(::conditionTitle) ?: "", color = Rune, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The belt's three places: each worn flask with the fight's reading of it; null where a place is empty. */
private fun worn(s: ForgeState, skills: HeroSkills): List<Pair<EquipmentInstance, Flask>?> {
    val hero = s.play.hero ?: return List(3) { null }
    val definitions = s.world.definitions.associateBy { it.code }
    return Sheet.FLASK_SLOTS.mapIndexed { i, slot ->
        hero.equipped[slot]?.let { item -> s.world.inventoryBases[item.equipmentId]?.let { item to Flask.of(item, it, definitions, skills.flasks.getOrNull(i)) } }
    }
}

/** A flask drawn as a bottle whose liquid stands at [fill], in the colour of what it brings. */
@Composable fun FlaskBottle(kind: FlaskKind?, fill: Float, present: Boolean, modifier: Modifier) {
    val tint = kind?.let(::flaskTint) ?: Muted
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val neck = Path().apply {
            moveTo(w * .38f, 0f); lineTo(w * .62f, 0f); lineTo(w * .62f, h * .28f); lineTo(w * .95f, h * .62f); lineTo(w * .88f, h); lineTo(w * .12f, h)
            lineTo(w * .05f, h * .62f); lineTo(w * .38f, h * .28f); close()
        }
        if (present) clipPath(neck) {
            val top = h * (1 - fill.coerceIn(0f, 1f) * .7f)
            drawRect(tint.copy(alpha = .8f), topLeft = Offset(0f, top), size = Size(w, h - top))
        }
        drawPath(neck, if (present) GoldBright.copy(alpha = .8f) else Muted.copy(alpha = .6f),
            style = Stroke(width = 2.dp.toPx(), pathEffect = if (present) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
    }
}

/**
 * The chosen place of the belt: the flask's name, what a draught of it gives with the hero's sheet —
 * life or mana and how fast, how long it lasts, what it costs of its charges — when it is drunk by
 * itself, and its card.
 */
@Composable private fun BeltDetail(s: ForgeState, skills: HeroSkills, body: Combatant, index: Int, onCondition: () -> Unit) {
    val (item, flask) = worn(s, skills).getOrNull(index) ?: run {
        InfoCard(ui("skills.belt_place", index + 1), ui("skills.belt_empty_hint"))
        return
    }
    val document = inventoryDocument(item, s.world.inventoryBases[item.equipmentId])
    val draught = flask.draught(body, body.maxLife, body.maxMana)
    ForgePanel(accent = flaskTint(flask.kind)) {
        Text(documentTitle(document), color = rarityColor(item.rarity), style = MaterialTheme.typography.titleMedium)
        val gives = when (flask.kind) {
            FlaskKind.LIFE -> ui("skills.flask_life", number(draught.life + draught.lifeRate * draught.duration), fineNumber(draught.duration))
            FlaskKind.MANA -> ui("skills.flask_mana", number(draught.mana + draught.manaRate * draught.duration), fineNumber(draught.duration))
            FlaskKind.UTILITY -> ui("skills.flask_utility", fineNumber(draught.duration))
        }
        Text(gives, color = Parchment, style = MaterialTheme.typography.bodyMedium)
        Text(ui("skills.flask_charges", number(flask.perUse(body)), number(flask.maxCharges)) +
            (if (item.quality > 0) " · " + ui("skills.flask_quality", item.quality) else ""), color = Muted, style = MaterialTheme.typography.labelMedium)
        if (draught.lines.isNotEmpty()) SkillLines(draught.lines.map { SkillText.statLine(it.stat, it.operation, it.value) })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("skills.drunk_when"), color = Muted, style = MaterialTheme.typography.labelMedium)
            AssistChip(onClick = onCondition, label = { Text(conditionTitle(flask.condition)) })
        }
    }
    ItemCard(document, enabled = false, detailed = true, definitions = s.world.definitions)
}

/**
 * Three books for one (2.78.0): any three of the bag — a book counted as often as it lies there — and the
 * server's gold for a book of the class's own, opened by the hero's level.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Exchange(s: ForgeState, vm: ForgeViewModel, pages: List<SkillDefinition>, heroLevel: Int, onDismiss: () -> Unit) {
    val rule = s.world.skills.rules.exchange
    val owned = s.world.books.mapNotNull { book -> (s.bagAmount(book.id) ?: 0L).takeIf { it > 0 }?.let { book.code.removePrefix(SkillDefinition.BOOK_PREFIX) to it } }
    var chosen by remember { mutableStateOf(listOf<String>()) }
    var target by remember { mutableStateOf<String?>(null) }
    val price = rule.goldPerLevel * heroLevel
    val money = s.play.hero?.character?.money ?: 0L
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("skills.exchange_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("skills.exchange_rule", rule.books, number(price.toDouble())))
            Caption(ui("skills.exchange_give", chosen.size, rule.books))
            owned.forEach { (code, amount) ->
                val taken = chosen.count { it == code }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (taken > 0) PanelRaised else Color.Transparent)
                    .clickable { chosen = if (taken < amount && chosen.size < rule.books) chosen + code else chosen - code }.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    s.world.skills.byCode[code]?.let { SkillGlyph(it.icon, Modifier.size(22.dp), if (taken > 0) GoldBright else Muted) }
                    Text(SkillText.title(code), color = if (taken > 0) GoldBright else Parchment, modifier = Modifier.weight(1f))
                    Text(if (taken > 0) "$taken / $amount" else "× $amount", color = Gold)
                }
            }
            Caption(ui("skills.exchange_take"))
            pages.filter { it.unlock <= heroLevel }.forEach { skill ->
                val on = target == skill.code
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (on) PanelRaised else Color.Transparent)
                    .clickable { target = skill.code }.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = on, onClick = { target = skill.code }, colors = RadioButtonDefaults.colors(selectedColor = Gold))
                    Text(SkillText.title(skill.code), color = if (on) GoldBright else Parchment)
                }
            }
            val ready = chosen.size == rule.books && target != null && money >= price && !s.busy
            if (money < price) Text(ui("skills.exchange_gold", number(price.toDouble())), color = LifeRed, style = MaterialTheme.typography.bodySmall)
            Button(enabled = ready, onClick = { target?.let { vm.exchangeBooks(chosen, it) }; onDismiss() }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright)) { Text(ui("skills.exchange")) }
        }
    }
}
