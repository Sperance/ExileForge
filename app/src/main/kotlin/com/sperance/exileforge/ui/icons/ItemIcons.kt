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
import com.sperance.exileforge.core.display.slotIcon
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
    if (!SpriteIcon(equipmentIcon(code), paint, modifier)) ItemEmblem(kind, paint, modifier)
}

/** An empty place of the body: the emblem of what would go there. */
@Composable fun SlotIcon(slot: Slot, color: Color, modifier: Modifier = Modifier, tint: Color? = null) {
    // The server's shadow glass since 3.7.0; the bundled emblem when the set has none for the slot.
    if (!SpriteIcon(slotIcon(slot.name), tint ?: color, modifier, halo = false)) ItemEmblem(slotVisualKind(slot), tint ?: color, modifier)
}

/** How an item of [slot] is drawn before there is one: the rules' own mapping over a bare template. */
fun slotVisualKind(slot: Slot): ItemVisualKind = itemVisualKind(ItemTemplate(code = "", slot = slot))

/**
 * Artwork for a stacking item of the bag, by its code: the server's glass when the set has it (3.6.0),
 * else an orb's bundled glass, the server's mono sprite, or the bundled glyph of its [kind] — a scroll
 * for a book, a shard for an essence, a gem for a material or a stack the content does not name.
 * A stack has no rarity, so its glass goes without a halo.
 */
@Composable fun BagIcon(code: String, modifier: Modifier = Modifier, tint: Color = Gold, kind: ItemVisualKind = ItemVisualKind.ITEM) {
    val sprite = itemIcon(code)
    val orb = Orb.of(code)
    when {
        sprite?.isGlass == true && SpriteIcon(sprite, tint, modifier, halo = false) -> Unit
        orb != null -> OrbGlyph(orb, modifier)
        !SpriteIcon(sprite, tint, modifier) -> Icon(bagGlyph(kind), null, tint = tint, modifier = modifier)
    }
}

private fun bagGlyph(kind: ItemVisualKind): ImageVector = when (kind) {
    ItemVisualKind.CURRENCY -> ForgeGlyphs.Orb
    ItemVisualKind.SCROLL -> ForgeGlyphs.Tome
    ItemVisualKind.GEM -> ForgeGlyphs.Shard
    else -> ForgeGlyphs.Gem
}
