package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import com.sperance.exileforge.ui.components.MutedText
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.display.icon
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.display.displayName
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import com.sperance.exileforge.core.model.hero.CharacterItem
import com.sperance.exileforge.presentation.state.ForgeState
import com.sperance.exileforge.ui.components.RaritySpine
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.spriteVector
import com.sperance.exileforge.ui.theme.*

/** The catalogue's plain stacks of the bag: materials, and since 2.78.0 the skill books and the essences. */
private fun stacks(s: ForgeState) = s.world.materials + s.world.books + s.world.essences

/** A bag stack's name: the orb's own, or the tail of an id the catalogue does not name. */
fun bagTitle(s: ForgeState, itemId: String): String =
    s.world.orbs.firstOrNull { it.id == itemId }?.title(s.lang)
        ?: stacks(s).firstOrNull { it.id == itemId }?.let { locOr(LocaleKey.itemName(it.code), displayName(it.code)) }
        ?: (ui("common.item") + " …${itemId.takeLast(6)}")

/** What a stack is, from the dictionary: the orb's rule, or a material's, a book's or an essence's description (since 2.41.0). */
private fun bagDetails(s: ForgeState, itemId: String): String? =
    s.world.orbs.firstOrNull { it.id == itemId }?.details(s.lang)
        ?: stacks(s).firstOrNull { it.id == itemId }?.let { locOr(LocaleKey.itemDescription(it.code), "") }

/** A shelf of the bag (2.75.0): each has its heading over its own run of cells; books and essences since 2.78.0. */
enum class BagCategory(val key: String) {
    ORBS("bag.section_orbs"), ESSENCES("bag.section_essences"), BOOKS("bag.section_books"), MATERIALS("bag.section_materials"), OTHER("bag.section_other")
}

/**
 * The bag by category — orbs, then materials, then whatever the catalogue does not name — each by
 * rarity, then by name (2.73.0). A stack has no rarity of its own, so its worth stands for it: the
 * dearer the rarer. An empty category is left out.
 */
fun bagSections(s: ForgeState): List<Pair<BagCategory, List<CharacterItem>>> {
    val orbs = s.world.orbs.associateBy { it.id }
    val materials = s.world.materials.associateBy { it.id }
    val books = s.world.books.associateBy { it.id }
    val essences = s.world.essences.associateBy { it.id }
    fun category(id: String) = when (id) {
        in orbs -> BagCategory.ORBS
        in essences -> BagCategory.ESSENCES
        in books -> BagCategory.BOOKS
        in materials -> BagCategory.MATERIALS
        else -> BagCategory.OTHER
    }
    fun worth(id: String) = orbs[id]?.price ?: (materials[id] ?: books[id] ?: essences[id])?.price ?: 0L
    val byCategory = s.play.hero?.bag.orEmpty().sortedWith(compareBy<CharacterItem>({ -worth(it.itemId) }, { bagTitle(s, it.itemId) }))
        .groupBy { category(it.itemId) }
    return BagCategory.entries.mapNotNull { category -> byCategory[category]?.let { category to it } }
}

/**
 * The bag as a table (2.75.0): a heading per category and under it square cells — the stack's icon
 * and its count in the corner — as many to a row as fit [CELL]. The name, the rule and the ways on
 * are one tap behind a cell, in [BagSheet].
 */
@Composable fun BagGrid(s: ForgeState, sections: List<Pair<BagCategory, List<CharacterItem>>>, onOpen: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + GAP) / (CELL + GAP)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sections.forEach { (category, stacks) ->
                Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                    MutedText(ui(category.key), style = MaterialTheme.typography.labelMedium)
                    stacks.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                            row.forEach { stack -> BagCell(s, stack, Modifier.weight(1f)) { onOpen(stack.itemId) } }
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
@Composable private fun BagCell(s: ForgeState, stack: CharacterItem, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    val title = bagTitle(s, stack.itemId)
    Box(modifier.aspectRatio(1f).background(Panel, shape).border(1.dp, PanelRaised, shape)
        .clickable(role = Role.Button, onClickLabel = title, onClick = onClick), contentAlignment = Alignment.Center) {
        StackIcon(stackCode(s, stack.itemId), s.world.orbs.firstOrNull { it.id == stack.itemId }, 36)
        Text(compactCount(stack.amount), color = GoldBright, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 4.dp, vertical = 2.dp))
    }
}

/** A count that fits a cell's corner: 12 345 is «12k», a million and more «1.2M». */
internal fun compactCount(amount: Long): String = when {
    amount >= 1_000_000 -> String.format(java.util.Locale.ROOT, "%.1fM", amount / 1_000_000.0).replace(".0M", "M")
    amount >= 10_000 -> "${amount / 1_000}k"
    else -> amount.toString()
}

/** The code of a stack the client knows — an orb or a material — for its icon. */
private fun stackCode(s: ForgeState, itemId: String): String? =
    s.world.orbs.firstOrNull { it.id == itemId }?.code ?: stacks(s).firstOrNull { it.id == itemId }?.code

/**
 * An orb, a material or an unnamed stack, drawn in a gold frame the way an item row frames its icon.
 * An orb is its stained glass since 2.69.0; a material is the server's sprite when the set has one,
 * the bundled glyph otherwise.
 */
@Composable private fun StackIcon(code: String?, currency: com.sperance.exileforge.core.model.currency.CurrencyItem?, size: Int) {
    if (currency != null) { com.sperance.exileforge.ui.icons.OrbGlyph(currency.orb, Modifier.size(size.dp)); return }
    val frame = RoundedCornerShape(6.dp)
    val art = code?.let { icon(IconKey.item(it))?.let(::spriteVector) }
    Box(Modifier.size(size.dp).background(Gold.copy(alpha = .08f), frame).border(1.dp, Gold.copy(alpha = .55f), frame), contentAlignment = Alignment.Center) {
        Icon(art ?: ForgeGlyphs.Gem, null, tint = if (code != null) Gold else Muted,
            modifier = Modifier.size((size * .55f).dp))
    }
}

/**
 * A stack of the bag, opened: its name, how many there are, what it does, and where it goes next.
 *
 * The forge is offered for an orb that is applied to items — Regret is spent on the tree and the
 * server refuses it on an item (`CR_009`), so it has no way in. The auction takes any stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun BagSheet(s: ForgeState, stack: CharacterItem, onDismiss: () -> Unit, onForge: (String) -> Unit, onAuction: (String) -> Unit,
                         /** A skill book of the class read at once (2.78.0), by the skill's code. */
                         onRead: (String) -> Unit = {},
                         /** An essence taken to the forge (2.78.0). */
                         onEssence: (String) -> Unit = {}) {
    val orb = s.world.orbs.firstOrNull { it.id == stack.itemId }
    val forgeable = orb != null && orb.orb != CurrencyOrb.ORB_OF_REGRET
    val skill = s.world.books.firstOrNull { it.id == stack.itemId }?.let { s.world.skills.byBook(it.code) }
    val readable = skill != null && skill.heroClass == s.heroClass?.code
    val essence = s.world.essences.any { it.id == stack.itemId }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).navigationBarsPadding()) {
            RaritySpine(Gold, 4.dp)
            Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StackIcon(stackCode(s, stack.itemId), orb, 56)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(bagTitle(s, stack.itemId), color = GoldBright, style = MaterialTheme.typography.titleLarge)
                        // The English trade name (2.51.0), under the translated one.
                        stackCode(s, stack.itemId)?.let { code -> locOr(LocaleKey.itemTrade(code), "") }
                            ?.takeIf { it.isNotBlank() && it != bagTitle(s, stack.itemId) }
                            ?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
                        MutedText(ui("bag.owned", stack.amount), style = MaterialTheme.typography.labelSmall)
                    }
                }
                bagDetails(s, stack.itemId)?.takeIf { it.isNotBlank() }?.let { Text(it, color = Parchment, style = MaterialTheme.typography.bodyMedium) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (forgeable) Button(enabled = !s.busy, onClick = { onForge(stack.itemId) }, modifier = Modifier.weight(1f)) {
                        Text(ui("bag.to_forge"))
                    }
                    if (essence) Button(enabled = !s.busy, onClick = { onEssence(stack.itemId) }, modifier = Modifier.weight(1f)) {
                        Text(ui("bag.to_forge"))
                    }
                    if (readable && skill != null) Button(enabled = !s.busy, onClick = { onRead(skill.code) }, modifier = Modifier.weight(1f)) {
                        Text(ui(if ((s.play.hero?.character?.skills?.level(skill.code) ?: 0) > 0) "bag.read_book" else "bag.learn_book"))
                    }
                    OutlinedButton(enabled = !s.busy, onClick = { onAuction(stack.itemId) }, modifier = Modifier.weight(1f)) {
                        Text(ui("hero.action_auction"))
                    }
                }
            }
        }
    }
}
