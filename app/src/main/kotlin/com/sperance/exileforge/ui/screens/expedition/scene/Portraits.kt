package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sperance.exileforge.core.display.classPortrait
import com.sperance.exileforge.core.display.monsterPortrait
import com.sperance.exileforge.ui.icons.drawPortrait
import kotlin.math.sin

/**
 * The busts in the arena's frames (2.28.0): the hero and every monster form, head and shoulders,
 * drawn from shapes into whatever box the frame gives — rule 17, no picture is ever loaded.
 *
 * Every drawing measures from the box: `w`/`h` are its size, and a figure is placed by shares of
 * them so a wider or a taller frame keeps the bust whole. A [wash] colours the whole portrait (an
 * ailment on the fighter), [flash] whitens it (a blow just landed) and [time] animates a wraith's
 * drift or a bat's ears. Eyes are the one thing every monster shares: two red points.
 */
object Portraits {

    /**
     * The hero's bust: the server's portrait of the class when there is one (since 2.31.0), breathing
     * a little, and the client's own helm otherwise.
     */
    fun hero(scope: DrawScope, classCode: String?, time: Float, wash: Color? = null, washAmount: Float = 0f, flash: Float = 0f) = with(scope) {
        classPortrait(classCode)?.let {
            drawPortrait(it, sin(time * 1.5f) * size.height * .006f)
            return@with finish(scope, wash, washAmount, flash)
        }
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2F3A), Color(0xFF0B0E13))))
        drawRect(Brush.radialGradient(listOf(Palettes.hero.copy(alpha = .3f), Color.Transparent), Offset(w / 2, h * .4f), h * .6f))
        // Faint rays behind the shoulders, as on a saint's icon.
        val ray = Palettes.hero.copy(alpha = .1f)
        listOf(Offset(0f, 0f), Offset(w, 0f), Offset(0f, h * .8f), Offset(w, h * .8f)).forEach { drawLine(ray, Offset(w / 2, h * .4f), it, 1.5f) }
        val bob = sin(time * 1.5f) * h * .006f
        // Cape, pauldrons, chest and neck.
        path { moveTo(-w * .05f, h); quadraticTo(w * .1f, h * .68f, w * .25f, h * .62f); lineTo(w * .39f, h * .7f); lineTo(w * .61f, h * .7f); lineTo(w * .75f, h * .62f); quadraticTo(w * .9f, h * .68f, w * 1.05f, h); close() }
            .also { drawPath(it, Palettes.heroCape) }
        path { moveTo(w * .02f, h); quadraticTo(w * .1f, h * .74f, w * .28f, h * .66f); lineTo(w * .5f, h * .78f); lineTo(w * .72f, h * .66f); quadraticTo(w * .9f, h * .74f, w * .98f, h); close() }
            .also { drawPath(it, Palettes.hero) }
        path { moveTo(w * .02f, h); quadraticTo(w * .1f, h * .74f, w * .28f, h * .66f); lineTo(w * .34f, h * .69f); quadraticTo(w * .18f, h * .79f, w * .12f, h); close() }
            .also { drawPath(it, Color(0xFFE0C88A)) }
        path { moveTo(w * .98f, h); quadraticTo(w * .9f, h * .74f, w * .72f, h * .66f); lineTo(w * .66f, h * .69f); quadraticTo(w * .82f, h * .79f, w * .88f, h); close() }
            .also { drawPath(it, Color(0xFFE0C88A)) }
        drawRect(Color(0xFFB39466), Offset(w * .44f, h * .6f), Size(w * .12f, h * .14f))
        // Head, helm, visor, crest.
        val cy = h * .46f + bob
        val r = h * .18f
        drawCircle(Color(0xFFE4DCCF), r, Offset(w / 2, cy))
        path { moveTo(w / 2 - r * 1.05f, cy); arcTo(androidx.compose.ui.geometry.Rect(w / 2 - r * 1.05f, cy - r * 1.05f, w / 2 + r * 1.05f, cy + r * 1.05f), 180f, 180f, false); lineTo(w / 2 + r * 1.05f, cy + r * .3f); lineTo(w / 2 - r * 1.05f, cy + r * .3f); close() }
            .also { drawPath(it, Palettes.steel) }
        drawRect(Color(0xFF9A9A9A), Offset(w / 2 - r * 1.05f, cy + r * .3f), Size(r * 2.1f, r * .3f))
        drawRect(Color(0xFF0B0E13), Offset(w / 2 - r * .75f, cy + r * .35f), Size(r * .5f, r * .16f))
        drawRect(Color(0xFF0B0E13), Offset(w / 2 + r * .25f, cy + r * .35f), Size(r * .5f, r * .16f))
        drawRect(Color(0xFF7A7A7A), Offset(w / 2 - r * .15f, cy + r * .3f), Size(r * .3f, r * .55f))
        drawLine(Palettes.hero, Offset(w / 2, cy - r * 1.05f), Offset(w / 2, cy + r * .3f), r * .12f)
        path { moveTo(w / 2 - r * .3f, cy - r * 1.05f); lineTo(w / 2 + r * .3f, cy - r * 1.05f); lineTo(w / 2 + r * .55f, cy - r * 1.9f); lineTo(w / 2 - r * .55f, cy - r * 1.9f); close() }
            .also { drawPath(it, Palettes.heroCape) }
        finish(scope, wash, washAmount, flash)
    }

    /** A monster's bust: its own portrait, then its form's, then the client's own drawing of the form. */
    fun monster(scope: DrawScope, code: String, form: String, accent: Color, time: Float, wash: Color? = null, washAmount: Float = 0f, flash: Float = 0f) = with(scope) {
        monsterPortrait(code, form)?.let {
            drawPortrait(it, sin(time * 1.2f + 1f) * size.height * .006f)
            return@with finish(scope, wash, washAmount, flash)
        }
        val w = size.width
        val h = size.height
        val body = Palettes.body(form)
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2A2030), Color(0xFF0B0A10))))
        drawRect(Brush.radialGradient(listOf(accent.copy(alpha = .2f), Color.Transparent), Offset(w / 2, h * .4f), h * .6f))
        val cx = w / 2
        val cy = h * .46f
        val r = h * .17f
        fun shoulders(width: Float = 1f, top: Float = .66f) {
            path { moveTo(w * (.5f - .5f * width), h); quadraticTo(w * (.5f - .42f * width), h * (top + .1f), w * (.5f - .24f * width), h * top); lineTo(cx, h * (top + .13f)); lineTo(w * (.5f + .24f * width), h * top); quadraticTo(w * (.5f + .42f * width), h * (top + .1f), w * (.5f + .5f * width), h); close() }
                .also { drawPath(it, body) }
        }
        fun head(radius: Float = r, y: Float = cy, tint: Color = body) = drawCircle(tint, radius, Offset(cx, y))
        fun eyes(y: Float = cy + r * .1f, gap: Float = r * .7f, size: Float = r * .13f) {
            drawCircle(Palettes.blood, size * 1.6f, Offset(cx - gap / 2, y))
            drawCircle(Palettes.blood, size * 1.6f, Offset(cx + gap / 2, y))
            drawCircle(Color(0xFFFF5A4A), size, Offset(cx - gap / 2, y))
            drawCircle(Color(0xFFFF5A4A), size, Offset(cx + gap / 2, y))
        }
        when (form) {
            "CRAB" -> {
                drawOval(body, Offset(cx - w * .42f, h * .5f), Size(w * .84f, h * .42f))
                drawOval(tone(body, 1.2f), Offset(cx - w * .3f, h * .56f), Size(w * .6f, h * .2f))
                for (side in listOf(-1, 1)) {
                    drawLine(body, Offset(cx + side * w * .12f, h * .52f), Offset(cx + side * w * .16f, h * .34f), r * .25f)
                    drawCircle(Color(0xFFFF5A4A), r * .18f, Offset(cx + side * w * .16f, h * .33f))
                    drawCircle(tone(body, 1.1f), r * .55f, Offset(cx + side * w * .38f, h * .5f))
                }
            }
            "BAT" -> {
                val flap = sin(time * 8f) * h * .03f
                path { moveTo(cx, h * .6f); lineTo(w * .02f, h * .35f + flap); lineTo(w * .2f, h * .7f); close() }.also { drawPath(it, tone(body, .8f)) }
                path { moveTo(cx, h * .6f); lineTo(w * .98f, h * .35f + flap); lineTo(w * .8f, h * .7f); close() }.also { drawPath(it, tone(body, .8f)) }
                head(r * 1.1f, h * .5f)
                path { moveTo(cx - r * .9f, h * .5f - r * .6f); lineTo(cx - r * 1.2f, h * .5f - r * 2.2f); lineTo(cx - r * .3f, h * .5f - r * .9f); close() }.also { drawPath(it, body) }
                path { moveTo(cx + r * .9f, h * .5f - r * .6f); lineTo(cx + r * 1.2f, h * .5f - r * 2.2f); lineTo(cx + r * .3f, h * .5f - r * .9f); close() }.also { drawPath(it, body) }
                eyes(h * .5f, r * .6f)
            }
            "SERPENT" -> {
                for (i in 0..3) drawCircle(tone(body, .8f + i * .05f), r * (1.3f - i * .15f), Offset(cx + sin(time + i) * w * .05f + (i - 1.5f) * w * .14f, h * (.95f - i * .12f)))
                drawOval(tone(body, 1.15f), Offset(cx - r * 1.5f, cy - r * .7f), Size(r * 3f, r * 1.6f))
                eyes(cy - r * .2f, r * 1.2f)
                drawLine(Color(0xFFFF5A4A), Offset(cx, cy + r * .9f), Offset(cx, cy + r * 1.5f), r * .12f)
            }
            "SLUG" -> {
                drawOval(body, Offset(w * .05f, h * .42f), Size(w * .9f, h * .62f))
                for (side in listOf(-1, 1)) {
                    drawLine(tone(body, 1.2f), Offset(cx + side * r * .5f, h * .5f), Offset(cx + side * r * .9f, h * .28f), r * .22f)
                    drawCircle(Color(0xFFFF5A4A), r * .2f, Offset(cx + side * r * .9f, h * .27f))
                }
            }
            "BEAST" -> {
                shoulders(1.1f)
                head(r * 1.05f)
                path { moveTo(cx - r * .9f, cy - r * .5f); lineTo(cx - r * 1.1f, cy - r * 1.7f); lineTo(cx - r * .3f, cy - r * .9f); close() }.also { drawPath(it, tone(body, .9f)) }
                path { moveTo(cx + r * .9f, cy - r * .5f); lineTo(cx + r * 1.1f, cy - r * 1.7f); lineTo(cx + r * .3f, cy - r * .9f); close() }.also { drawPath(it, tone(body, .9f)) }
                drawOval(tone(body, 1.2f), Offset(cx - r * .55f, cy + r * .2f), Size(r * 1.1f, r * .9f))
                drawCircle(Color(0xFF0B0E13), r * .2f, Offset(cx, cy + r * .5f))
                eyes(cy - r * .15f, r * .8f)
            }
            "SPIDER" -> {
                for (i in 0..3) {
                    val spread = r * (1.4f + i * .5f)
                    val lift = h * (.72f - i * .06f)
                    drawLine(tone(body, .8f), Offset(cx, h * .6f), Offset(cx - spread, lift), r * .12f)
                    drawLine(tone(body, .8f), Offset(cx, h * .6f), Offset(cx + spread, lift), r * .12f)
                }
                drawOval(body, Offset(cx - r * 1.6f, h * .55f), Size(r * 3.2f, h * .5f))
                head(r * .9f, cy + r * .2f)
                for (i in 0..3) drawCircle(Color(0xFFFF5A4A), r * .1f, Offset(cx - r * .55f + i * r * .37f, cy - r * .1f))
                eyes(cy + r * .25f, r * .6f)
                drawLine(Color(0xFFE4DCCF), Offset(cx - r * .3f, cy + r * .7f), Offset(cx - r * .4f, cy + r * 1.1f), r * .1f)
                drawLine(Color(0xFFE4DCCF), Offset(cx + r * .3f, cy + r * .7f), Offset(cx + r * .4f, cy + r * 1.1f), r * .1f)
            }
            "GOLEM" -> {
                drawRect(body, Offset(w * .05f, h * .62f), Size(w * .9f, h * .4f))
                drawRect(tone(body, .8f), Offset(w * .05f, h * .58f), Size(w * .22f, h * .3f))
                drawRect(tone(body, .8f), Offset(w * .73f, h * .58f), Size(w * .22f, h * .3f))
                drawRect(tone(body, 1.1f), Offset(cx - r * 1.1f, cy - r * 1.1f), Size(r * 2.2f, r * 2.1f))
                drawRect(Color(0xFF0B0E13), Offset(cx - r * .9f, cy - r * .2f), Size(r * 1.8f, r * .5f))
                eyes(cy + r * .05f, r * 1f, r * .16f)
            }
            "WRAITH" -> {
                val drift = sin(time * 2f) * h * .02f
                path { moveTo(w * .1f, h + drift); quadraticTo(w * .2f, h * .5f, cx, h * .32f); quadraticTo(w * .8f, h * .5f, w * .9f, h + drift); close() }
                    .also { drawPath(it, body.copy(alpha = .55f)) }
                path { moveTo(cx - r * 1.3f, cy + r * .6f); quadraticTo(cx, cy - r * 1.9f, cx + r * 1.3f, cy + r * .6f); close() }.also { drawPath(it, body.copy(alpha = .85f)) }
                drawOval(Color(0xFF0B0E13), Offset(cx - r * .8f, cy - r * .7f), Size(r * 1.6f, r * 1.4f))
                eyes(cy - r * .05f, r * .7f)
            }
            "UNDEAD" -> {
                path { moveTo(w * .0f, h); quadraticTo(w * .12f, h * .7f, w * .3f, h * .64f); lineTo(cx, h * .78f); lineTo(w * .7f, h * .64f); quadraticTo(w * .88f, h * .7f, w, h); close() }.also { drawPath(it, Color(0xFF4A4335)) }
                shoulders(.9f, .68f)
                for (i in 0..2) drawLine(Color(0xFF6F6A5A), Offset(cx - r * 1f, h * (.8f + i * .07f)), Offset(cx + r * 1f, h * (.8f + i * .07f)), r * .08f)
                drawRect(Color(0xFFA49A82), Offset(cx - r * .3f, cy + r * .7f), Size(r * .6f, r * .7f))
                head(r, cy, Color(0xFFE4DCCF))
                drawRect(Color(0xFFD5CDB8), Offset(cx - r * .8f, cy + r * .5f), Size(r * 1.6f, r * .45f))
                drawOval(Color(0xFF0B0E13), Offset(cx - r * .75f, cy - r * .3f), Size(r * .6f, r * .5f))
                drawOval(Color(0xFF0B0E13), Offset(cx + r * .15f, cy - r * .3f), Size(r * .6f, r * .5f))
                path { moveTo(cx, cy + r * .1f); lineTo(cx - r * .15f, cy + r * .45f); lineTo(cx + r * .15f, cy + r * .45f); close() }.also { drawPath(it, Color(0xFF0B0E13)) }
                for (i in 0..4) drawLine(Color(0xFF0B0E13), Offset(cx - r * .5f + i * r * .25f, cy + r * .55f), Offset(cx - r * .5f + i * r * .25f, cy + r * .85f), r * .05f)
                drawCircle(Color(0xFFFF5A4A), r * .12f, Offset(cx - r * .45f, cy - r * .05f))
                drawCircle(Color(0xFFFF5A4A), r * .12f, Offset(cx + r * .45f, cy - r * .05f))
                path { moveTo(cx - r * 1.05f, cy - r * .2f); quadraticTo(cx, cy - r * 1.9f, cx + r * 1.05f, cy - r * .2f); close() }.also { drawPath(it, Color(0xFF5C5548)) }
            }
            "BRUTE" -> {
                shoulders(1.35f, .6f)
                drawOval(tone(body, .75f), Offset(cx - w * .5f, h * .55f), Size(w, h * .18f))
                head(r * 1.15f, cy + r * .1f)
                drawOval(tone(body, 1.2f), Offset(cx - r * .6f, cy + r * .4f), Size(r * 1.2f, r * .8f))
                drawLine(Color(0xFFE4DCCF), Offset(cx - r * .5f, cy + r * .9f), Offset(cx - r * .7f, cy + r * 1.4f), r * .14f)
                drawLine(Color(0xFFE4DCCF), Offset(cx + r * .5f, cy + r * .9f), Offset(cx + r * .7f, cy + r * 1.4f), r * .14f)
                eyes(cy - r * .1f, r * .9f)
            }
            // The forms of the lands past the Drowned Temple (2.77.0, server 0.68.0).
            "SCORPION" -> {
                for (i in 0..4) drawCircle(tone(body, .8f + i * .05f), r * (.55f - i * .05f), Offset(cx + r * (1.6f - i * .45f), h * (.78f - i * .13f)))
                drawCircle(Color(0xFFE0C080), r * .2f, Offset(cx - r * .4f, h * .2f))
                for (side in listOf(-1, 1)) {
                    drawLine(tone(body, .85f), Offset(cx + side * r * .8f, h * .62f), Offset(cx + side * w * .36f, h * .45f), r * .3f)
                    drawCircle(tone(body, 1.1f), r * .45f, Offset(cx + side * w * .38f, h * .4f))
                }
                drawOval(body, Offset(cx - w * .36f, h * .6f), Size(w * .72f, h * .44f))
                head(r * .95f, cy + r * .4f)
                eyes(cy + r * .3f, r * .6f)
            }
            "INSECT" -> {
                val buzz = sin(time * 20f) * h * .01f
                drawOval(Color.White.copy(alpha = .15f), Offset(w * .02f, h * .15f + buzz), Size(w * .42f, h * .5f))
                drawOval(Color.White.copy(alpha = .15f), Offset(w * .56f, h * .15f - buzz), Size(w * .42f, h * .5f))
                drawOval(body, Offset(cx - r * 1.3f, h * .62f), Size(r * 2.6f, h * .45f))
                for (i in 0..1) drawRect(Color(0xFFC8A030).copy(alpha = .6f), Offset(cx - r * 1.1f, h * (.72f + i * .1f)), Size(r * 2.2f, h * .035f))
                for (side in listOf(-1, 1)) drawLine(tone(body, .8f), Offset(cx + side * r * .3f, cy - r * .9f), Offset(cx + side * r * .9f, cy - r * 2.1f), r * .08f)
                path { moveTo(cx, cy + r * 1.4f); lineTo(cx - r * 1.2f, cy - r * .2f); quadraticTo(cx, cy - r * 1.4f, cx + r * 1.2f, cy - r * .2f); close() }
                    .also { drawPath(it, tone(body, 1.1f)) }
                drawOval(Color(0xFF9AFF3A), Offset(cx - r * 1.05f, cy - r * .55f), Size(r * .7f, r * .9f))
                drawOval(Color(0xFF9AFF3A), Offset(cx + r * .35f, cy - r * .55f), Size(r * .7f, r * .9f))
            }
            "DEMON" -> {
                shoulders(1.25f, .64f)
                drawCircle(Color(0xFFFF6A20).copy(alpha = .5f), r * .45f, Offset(cx, h * .84f))
                for (side in listOf(-1, 1)) {
                    path { moveTo(cx + side * r * .6f, cy - r * .6f); quadraticTo(cx + side * r * 1.9f, cy - r * 1.2f, cx + side * r * 1.6f, cy - r * 2.4f)
                        quadraticTo(cx + side * r * 1.2f, cy - r * 1.3f, cx + side * r * .2f, cy - r * .9f); close() }.also { drawPath(it, Color(0xFFE0D2B0)) }
                }
                head(r * 1.05f)
                for (side in listOf(-1, 1)) {
                    path { moveTo(cx + side * r * .7f, cy + r * .05f); lineTo(cx + side * r * .15f, cy + r * .25f); lineTo(cx + side * r * .6f, cy + r * .35f); close() }
                        .also { drawPath(it, Color(0xFFFFD030)) }
                }
                drawLine(Color(0xFF140404), Offset(cx - r * .45f, cy + r * .7f), Offset(cx + r * .45f, cy + r * .7f), r * .14f)
            }
            "FUNGUS" -> {
                drawRect(Color(0xFFD8CCB4), Offset(cx - r * .7f, cy + r * .3f), Size(r * 1.4f, h - cy - r * .3f))
                path { moveTo(cx - r * 2.6f, cy + r * .4f); quadraticTo(cx - r * 2.4f, cy - r * 2.2f, cx, cy - r * 2.3f); quadraticTo(cx + r * 2.4f, cy - r * 2.2f, cx + r * 2.6f, cy + r * .4f); close() }
                    .also { drawPath(it, body) }
                listOf(-1.4f to -1.2f, .2f to -1.7f, 1.3f to -.9f, -.4f to -.6f).forEach { (dx, dy) ->
                    drawCircle(Color(0xFFECDCF4).copy(alpha = .7f), r * .28f, Offset(cx + r * dx, cy + r * dy))
                }
                drawCircle(Color(0xFFC0FF5A), r * .15f, Offset(cx - r * .35f, cy + r * .9f))
                drawCircle(Color(0xFFC0FF5A), r * .15f, Offset(cx + r * .35f, cy + r * .9f))
            }
            else -> {
                shoulders()
                drawRect(tone(body, 1.1f), Offset(cx - r * .3f, cy + r * .6f), Size(r * .6f, r * .8f))
                head(r, cy, Color(0xFFD9B48A))
                path { moveTo(cx - r * 1.25f, cy + r * .3f); quadraticTo(cx, cy - r * 2.1f, cx + r * 1.25f, cy + r * .3f); quadraticTo(cx, cy - r * .3f, cx - r * 1.25f, cy + r * .3f); close() }
                    .also { drawPath(it, tone(body, .7f)) }
                eyes(cy + r * .05f, r * .8f)
            }
        }
        finish(scope, wash, washAmount, flash)
    }

    /** The rarity ring of a magic or rare monster, drawn round the portrait's edge. */
    fun ring(scope: DrawScope, color: Color, time: Float) = with(scope) {
        val pulse = .35f + .25f * sin(time * 4f)
        drawRect(Brush.radialGradient(listOf(Color.Transparent, color.copy(alpha = pulse)), Offset(size.width / 2, size.height / 2), size.maxDimension * .7f))
        drawRect(color.copy(alpha = .6f), style = Stroke(2f))
    }

    private fun finish(scope: DrawScope, wash: Color?, washAmount: Float, flash: Float) = with(scope) {
        if (wash != null && washAmount > 0) drawRect(wash.copy(alpha = washAmount.coerceIn(0f, .7f)))
        if (flash > 0) drawRect(Color.White.copy(alpha = flash.coerceIn(0f, .85f)))
        // Vignette: the frame's dark closes in on the bust.
        drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xCC000000)), Offset(size.width / 2, size.height / 2), size.maxDimension * .75f))
    }

    private inline fun path(build: Path.() -> Unit): Path = Path().apply(build)
}
