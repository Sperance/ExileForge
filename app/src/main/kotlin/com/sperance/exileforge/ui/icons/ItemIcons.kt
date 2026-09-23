package com.sperance.exileforge.ui.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.display.documentIcon
import com.sperance.exileforge.core.display.itemVisualKind
import kotlinx.serialization.json.JsonObject

/**
 * Artwork for a game entity.
 *
 * The drawing belongs to the server, beside the name: a document carries a code, and the icon set
 * says which outline that code is drawn with. What arrives is path data, never an image — the
 * client paints it itself and tints it by rarity, so the dark theme still owns the colour.
 *
 * Without the set, or for a code it does not cover, the bundled emblem chosen from the document's
 * own fields stands in. That is what was drawn before the server had any, so a hole looks
 * deliberate rather than broken.
 */
@Composable fun ItemIcon(document: JsonObject, color: Color, modifier: Modifier = Modifier, tint: Color? = null) {
    val paint = tint ?: color
    val sprite = documentIcon(document)?.let(::spriteVector)
    if (sprite != null) Icon(sprite, null, tint = paint, modifier = modifier)
    else ItemEmblem(itemVisualKind(document), paint, modifier)
}
