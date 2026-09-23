package com.sperance.exileforge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import com.sperance.exileforge.ui.icons.drawToken
import com.sperance.exileforge.ui.screens.expedition.scene.Portraits
import com.sperance.exileforge.ui.theme.Gold

/**
 * A class's portrait outside a fight (since 2.31.0): three by four, or [round] as the token the map
 * draws. The server's drawing when it has one, the client's own bust otherwise. [portraits] is how
 * many the server has sent so far — read here so the picture is redrawn the moment they arrive.
 */
@Composable fun ClassPortrait(classCode: String?, portraits: Int, modifier: Modifier = Modifier, round: Boolean = false, ring: Color = Gold) {
    key(portraits) { Canvas(if (round) modifier.aspectRatio(1f) else modifier.aspectRatio(3f / 4f)) {
        if (round) drawToken(Offset(size.width / 2, size.height / 2), size.minDimension / 2 * .9f, ring) { Portraits.hero(this, classCode, 0f) }
        else clipRect { Portraits.hero(this, classCode, 0f) }
    } }
}
