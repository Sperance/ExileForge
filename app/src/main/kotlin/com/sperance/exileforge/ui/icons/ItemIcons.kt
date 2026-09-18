package com.sperance.exileforge.ui.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.display.itemVisualKind
import kotlinx.serialization.json.JsonObject

/**
 * Artwork for a game entity.
 *
 * This server stores no drawings — an entity carries at most an `image` URL, and the client draws
 * no network images — so every picture is a bundled vector chosen from the document's own fields.
 */
@Composable fun ItemIcon(document: JsonObject, color: Color, modifier: Modifier = Modifier, tint: Color? = null) {
    ItemEmblem(itemVisualKind(document), tint ?: color, modifier)
}

/** A rolled property, a stat or a modifier: the bundled glyph that matches its name. */
@Composable fun PropertyIcon(key: String, tint: Color, modifier: Modifier = Modifier) {
    Icon(propertyIcon(key), null, tint = tint, modifier = modifier)
}
