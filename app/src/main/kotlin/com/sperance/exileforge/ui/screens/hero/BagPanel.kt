package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemSource
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.bagVisualKind
import com.sperance.exileforge.core.display.chanceText
import com.sperance.exileforge.core.display.essenceGuarantees
import com.sperance.exileforge.core.display.itemDescription
import com.sperance.exileforge.core.display.itemSourceIndex
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.title
import com.sperance.exileforge.core.display.tradeName
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.ItemCode
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RaritySpine
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One stack of the bag: the item's code and how many the hero holds. */
data class BagStack(val code: String, val amount: Long)

/** A shelf of the bag (2.75.0): each has its heading over its own run of cells; books and essences since 2.78.0. */
enum class BagCategory(val key: String) {
    CHESTS("bag.section_chests"),
    ORBS("bag.section_orbs"),
    ESSENCES("bag.section_essences"),
    BOOKS("bag.section_books"),
    MATERIALS("bag.section_materials"),
    PETS("bag.section_pets"),
    OTHER("bag.section_other"),
}

/**
 * The bag by category — orbs, then essences, books and materials, then whatever the content does not
 * name — each by rarity, then by name (2.73.0). A stack has no rarity of its own, so its worth stands
 * for it: the dearer the rarer. An empty category is left out.
 */
fun bagSections(game: GameUi): List<Pair<BagCategory, List<BagStack>>> {
    val index = game.index
    fun category(code: String) = when (index?.item(ItemCode(code))?.category) {
        Item.CURRENCY, Item.OMEN -> BagCategory.ORBS
        Item.ESSENCE -> BagCategory.ESSENCES
        Item.BOOK -> BagCategory.BOOKS
        Item.MATERIAL -> BagCategory.MATERIALS
        Item.PET -> BagCategory.PETS
        Item.CHEST -> BagCategory.CHESTS
        else -> BagCategory.OTHER
    }
    fun worth(code: String) = index?.item(ItemCode(code))?.price ?: 0L
    val byCategory = game.hero?.bag.orEmpty().filterValues { it > 0 }.map { (code, amount) -> BagStack(code, amount) }
        .sortedWith(compareBy({ -worth(it.code) }, { itemTitle(it.code) }))
        .groupBy { category(it.code) }
    return BagCategory.entries.mapNotNull { category -> byCategory[category]?.let { category to it } }
}

/**
 * The bag as a table (2.75.0): a heading per category and under it square cells — the stack's icon
 * and its count in the corner — as many to a row as fit [CELL]. The name, the rule and the ways on
 * are one tap behind a cell, in [BagSheet]; [onOpen] gets the stack's code.
 */
@Composable fun BagGrid(game: GameUi, sections: List<Pair<BagCategory, List<BagStack>>>, onOpen: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + GAP) / (CELL + GAP)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { (category, stacks) ->
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    MutedText(ui(category.key), style = MaterialTheme.typography.labelMedium)
                    stacks.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                            row.forEach { stack -> BagCell(game, stack, Modifier.weight(1f)) { onOpen(stack.code) } }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

private val CELL = 60.dp
private val GAP = 6.dp

/** One cell of the bag: the stack's icon in a panel square, its count in the bottom corner. */
@Composable private fun BagCell(game: GameUi, stack: BagStack, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    val title = itemTitle(stack.code)
    Box(
        modifier.aspectRatio(1f).background(Panel, shape).border(1.dp, PanelRaised, shape)
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        StackIcon(game, stack.code, 36)
        Text(
            compactCount(stack.amount),
            color = GoldBright,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

/** A count that fits a cell's corner: 12 345 is «12k», a million and more «1.2M». */
internal fun compactCount(amount: Long): String = when {
    amount >= 1_000_000 -> String.format(java.util.Locale.ROOT, "%.1fM", amount / 1_000_000.0).replace(".0M", "M")
    amount >= 10_000 -> "${amount / 1_000}k"
    else -> amount.toString()
}

/**
 * An orb, a material or an unnamed stack, drawn in a gold frame the way an item row frames its icon.
 * An orb is its stained glass since 2.69.0; anything else is the server's sprite when the set has one,
 * the bundled glyph of its category otherwise — muted for a code the content does not know.
 */
@Composable internal fun StackIcon(game: GameUi, code: String, size: Int) {
    if (Orb.of(code) != null) {
        BagIcon(code, Modifier.size(size.dp))
        return
    }
    val frame = RoundedCornerShape(6.dp)
    val known = game.index?.item(ItemCode(code))
    Box(Modifier.size(size.dp).background(Gold.copy(alpha = .08f), frame).border(1.dp, Gold.copy(alpha = .55f), frame), contentAlignment = Alignment.Center) {
        BagIcon(code, Modifier.size((size * .55f).dp), tint = if (known != null) Gold else Muted, kind = known?.let(::bagVisualKind) ?: ItemVisualKind.ITEM)
    }
}

/**
 * A stack of the bag, opened: its name, how many there are, what it does, and where it goes next.
 *
 * The forge is offered for an orb that is applied to items — Regret is spent on the tree and the
 * server refuses it on an item (`CR_009`), so it has no way in. The auction takes any stack.
 * Every callback gets the stack's code, but [onRead], which gets the skill's.
 */
@Composable fun BagSheet(
    game: GameUi,
    stack: BagStack,
    onDismiss: () -> Unit,
    onForge: (String) -> Unit,
    onAuction: (String) -> Unit,
    /** A skill book of the class read at once (2.78.0), by the skill's code. */
    onRead: (String) -> Unit = {},
    /** An essence taken to the forge (2.78.0). */
    onEssence: (String) -> Unit = {},
    /** A loot chest opened where it lies (3.76.0). */
    onOpenChest: (String) -> Unit = {},
) {
    val code = stack.code
    val orb = Orb.of(code)
    val forgeable = orb != null && orb != Orb.ORB_OF_REGRET
    val skill = game.index?.skills?.byBook(code)
    val readable = skill != null && skill.heroClass == game.hero?.heroClass
    val essence = game.index?.essence(code) != null
    // A loot chest (3.76.0) is opened here; since 3.77.0 it may also go to the auction, never to the merchant.
    val chest = game.index?.item(ItemCode(code))?.category == Item.CHEST
    StackPanel(onDismiss) {
        StackFace(game, code, stack.amount)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (forgeable) {
                ForgeButton(enabled = !game.busy, onClick = { onForge(code) }, modifier = Modifier.weight(1f)) {
                    Text(ui("bag.to_forge"))
                }
            }
            if (essence) {
                ForgeButton(enabled = !game.busy, onClick = { onEssence(code) }, modifier = Modifier.weight(1f)) {
                    Text(ui("bag.to_forge"))
                }
            }
            if (readable && skill != null) {
                ForgeButton(enabled = !game.busy, onClick = { onRead(skill.code) }, modifier = Modifier.weight(1f)) {
                    Text(ui(if ((game.hero?.skills?.level(skill.code) ?: 0) > 0) "bag.read_book" else "bag.learn_book"))
                }
            }
            if (chest) {
                ForgeButton(enabled = !game.busy, onClick = { onOpenChest(code) }, modifier = Modifier.weight(1f)) {
                    Text(ui("chest.open"))
                }
            }
            ForgeOutlinedButton(enabled = !game.busy, onClick = { onAuction(code) }, modifier = Modifier.weight(1f)) {
                Text(ui("hero.action_auction"))
            }
        }
    }
}

/**
 * A stack seen away from the bag — a fight's spoils: its face alone, with no way on, since the run is still underway.
 * [code] is the stack's item code; the count is what the hero holds now.
 */
@Composable fun StackInfoSheet(game: GameUi, code: String, onDismiss: () -> Unit) {
    StackPanel(onDismiss) { StackFace(game, code, game.hero?.bag?.get(code) ?: 0L) }
}

/** The sheet a stack opens in: the gold spine along its edge and [content] beside it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StackPanel(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ForgeSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).navigationBarsPadding()) {
            RaritySpine(Gold, 4.dp)
            Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/** A stack's face: its icon, name and English trade name, how many the hero holds, and what it is for. */
@Composable private fun StackFace(game: GameUi, code: String, owned: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        StackIcon(game, code, 56)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(itemTitle(code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            // The English trade name (2.51.0), under the translated one.
            tradeName(code, equipment = false)?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
            MutedText(ui("bag.owned", owned), style = MaterialTheme.typography.labelSmall)
        }
    }
    itemDescription(code).takeIf { it.isNotBlank() }?.let { Text(it, color = Parchment, style = MaterialTheme.typography.bodyMedium) }
    game.index?.let { StackLore(it, code) }
}

/** What the content says of a stack under its description: an essence's guaranteed line by kind of item, then where it is found. */
@Composable private fun StackLore(index: ContentIndex, code: String) {
    val guarantees = remember(index, code) { essenceGuarantees(index, code) }
    // The reverse of the whole content, built once per content off the main thread; the card shows none until it is.
    val sourceIndex by produceState<Map<String, List<ItemSource>>>(emptyMap(), index) {
        value = withContext(Dispatchers.Default) { itemSourceIndex(index) }
    }
    val sources = sourceIndex[code].orEmpty().take(MAX_SOURCES)
    if (guarantees.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Engraved(ui("bag.essence_guarantee"))
            guarantees.forEach { guarantee ->
                Column {
                    MutedText(guarantee.slots, style = MaterialTheme.typography.labelSmall)
                    Text(guarantee.line, color = ModBlue, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (sources.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Engraved(ui("bag.sources"))
            sources.forEach { source ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(source.title(), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    source.chanceText()?.let { MutedText(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

/** A card lists this many sources at most: the first by kind — works, monsters, bosses, chests — say enough. */
private const val MAX_SOURCES = 5
