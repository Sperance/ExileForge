package com.sperance.exileforge.ui.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.ItemVisualKind
import com.sperance.exileforge.core.display.equipmentIcon
import com.sperance.exileforge.core.display.itemIcon
import com.sperance.exileforge.core.display.itemVisualKind
import com.sperance.exileforge.rules.content.ItemTemplate
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.ui.theme.Gold

/**
 * Artwork for an equipment copy.
 *
 * The drawing belongs to the server, beside the name: a template carries a code, and the icon set
 * says which outline that code is drawn with. What arrives is path data, never an image — the
 * client paints it itself and tints it by rarity, so the dark theme still owns the colour.
 *
 * Without the set, or for a code it does not cover, the bundled emblem chosen from the template's
 * slot and weapon stands in. That is what was drawn before the server had any, so a hole looks
 * deliberate rather than broken.
 */
@Composable fun ItemIcon(item: ItemView, color: Color, modifier: Modifier = Modifier, tint: Color? = null) =
    ItemIcon(item.code, item.visualKind, color, modifier, tint)

/** The same for a template that is not yet a copy — a reference, a shelf. */
@Composable fun ItemIcon(template: ItemTemplate, color: Color, modifier: Modifier = Modifier, tint: Color? = null) =
    ItemIcon(template.code, itemVisualKind(template), color, modifier, tint)

/** The same by the template's code and the kind it is drawn as when the server has no outline for it. */
@Composable fun ItemIcon(code: String, kind: ItemVisualKind, color: Color, modifier: Modifier = Modifier, tint: Color? = null) {
    val paint = tint ?: color
    val sprite = equipmentIcon(code)?.let(::spriteVector)
    if (sprite != null) Icon(sprite, null, tint = paint, modifier = modifier)
    else ItemEmblem(kind, paint, modifier)
}

/** An empty place of the body: the emblem of what would go there. */
@Composable fun SlotIcon(slot: Slot, color: Color, modifier: Modifier = Modifier, tint: Color? = null) =
    ItemEmblem(slotVisualKind(slot), tint ?: color, modifier)

/** How an item of [slot] is drawn before there is one: the rules' own mapping over a bare template. */
fun slotVisualKind(slot: Slot): ItemVisualKind = itemVisualKind(ItemTemplate(code = "", slot = slot))

/**
 * Artwork for a stacking item of the bag, by its code: an orb is its stained glass, anything else the
 * server's sprite when the set has one, or the bundled glyph of its [kind] — a scroll for a book, a
 * shard for an essence, a gem for a material or a stack the content does not name.
 */
@Composable fun BagIcon(code: String, modifier: Modifier = Modifier, tint: Color = Gold, kind: ItemVisualKind = ItemVisualKind.ITEM) {
    val orb = Orb.of(code)
    if (orb != null) OrbGlyph(orb, modifier)
    else Icon(itemIcon(code)?.let(::spriteVector) ?: bagGlyph(kind), null, tint = tint, modifier = modifier)
}

private fun bagGlyph(kind: ItemVisualKind): ImageVector = when (kind) {
    ItemVisualKind.CURRENCY -> ForgeGlyphs.Orb
    ItemVisualKind.SCROLL -> ForgeGlyphs.Tome
    ItemVisualKind.GEM -> ForgeGlyphs.Shard
    else -> ForgeGlyphs.Gem
}
