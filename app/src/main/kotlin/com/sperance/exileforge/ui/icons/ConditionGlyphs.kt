package com.sperance.exileforge.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.rules.content.SlotConditionKind

/** Часть знака: путь [d] на поле 16×16 - заливкой ([width] 0) или линией толщины [width]. */
private class GlyphPart(val d: String, val width: Float = 0f)

private fun circle(cx: Float, cy: Float, r: Float): String = "M${cx - r} ${cy}a$r $r 0 1 1 ${2 * r} 0a$r $r 0 1 1 ${-2 * r} 0z"

private fun conditionGlyph(name: String, vararg parts: GlyphPart): ImageVector = ImageVector.Builder(name, 16.dp, 16.dp, 16f, 16f).apply {
    parts.forEach { part ->
        val line = part.width > 0f
        addPath(
            addPathNodes(part.d),
            fill = if (line) null else SolidColor(Color.White),
            stroke = if (line) SolidColor(Color.White) else null,
            strokeLineWidth = part.width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

/** Знаки видов условия слота: ячейка Гримуара, место пояса и плитки листа выбора; цвет даёт место. */
object ConditionGlyphs {
    private val Ready = conditionGlyph("Ready", GlyphPart("M3 8.5l3 3 7-7", 1.8f))
    private val Manual = conditionGlyph("Manual", GlyphPart(circle(8f, 8f, 5.5f), 1.5f), GlyphPart(circle(8f, 8f, 2f)))
    private val Life = conditionGlyph("Life", GlyphPart("M8 13.5S2.5 10 2.5 6.2A2.9 2.9 0 0 1 8 4.6a2.9 2.9 0 0 1 5.5 1.6C13.5 10 8 13.5 8 13.5z"))
    private val Mana = conditionGlyph("Mana", GlyphPart("M8 2s4.5 5 4.5 8a4.5 4.5 0 0 1-9 0C3.5 7 8 2 8 2z"))
    private val Shield = conditionGlyph("Shield", GlyphPart("M8 1.8l5 1.9v4c0 3.2-2.2 5.4-5 6.5-2.8-1.1-5-3.3-5-6.5v-4z", 1.6f))
    private val Crowd = conditionGlyph(
        "Crowd",
        GlyphPart(circle(4f, 6f, 2f)),
        GlyphPart(circle(8f, 5f, 2.2f)),
        GlyphPart(circle(12f, 6f, 2f)),
        GlyphPart("M1.5 13c.3-2.4 1.3-3.6 2.5-3.6M14.5 13c-.3-2.4-1.3-3.6-2.5-3.6M4.8 13.5c.4-3 1.6-4.3 3.2-4.3s2.8 1.3 3.2 4.3", 1.4f),
    )
    private val Target = conditionGlyph("Target", GlyphPart(circle(8f, 8f, 5.2f), 1.5f), GlyphPart("M8 1v3.2M8 11.8V15M1 8h3.2M11.8 8H15", 1.5f), GlyphPart(circle(8f, 8f, 1.4f)))
    private val Clock = conditionGlyph("Clock", GlyphPart(circle(8f, 8f, 6f), 1.5f), GlyphPart("M8 4.5V8l2.5 1.6", 1.5f))
    private val Flame = conditionGlyph("Flame", GlyphPart("M8 1.5c.5 2.6 4.5 4.3 4.5 8A4.5 4.5 0 0 1 3.5 9.5c0-2 1.2-3.2 2-4 .2 1.5.8 2.3 1.6 2.6C7 6 7 4 8 1.5z"))
    private val Crown = conditionGlyph("Crown", GlyphPart("M2 12.5h12M2.5 11l-1-6.5 3.8 3L8 3l2.7 4.5 3.8-3-1 6.5z", 1.5f))
    private val Burst = conditionGlyph("Burst", GlyphPart("M9.5 1L4 9h3.5L6 15l6-8.5H8.3z"))
    private val Cast = conditionGlyph("Cast", GlyphPart("M8 1.5l1.5 4.9L14.5 8l-5 1.6L8 14.5 6.5 9.6 1.5 8l5-1.6z"))
    private val NoFlask = conditionGlyph("NoFlask", GlyphPart("M6.3 1.8h3.4v3.6l3 3.8v4.5H3.3V9.2l3-3.8z", 1.4f), GlyphPart("M2 14L14 2", 1.6f))

    /** Знак вида [kind]. */
    fun of(kind: SlotConditionKind): ImageVector = when (kind) {
        SlotConditionKind.READY -> Ready
        SlotConditionKind.MANUAL -> Manual
        SlotConditionKind.LIFE -> Life
        SlotConditionKind.MANA -> Mana
        SlotConditionKind.SHIELD -> Shield
        SlotConditionKind.AILING -> Flame
        SlotConditionKind.BURST -> Burst
        SlotConditionKind.NO_EFFECT -> NoFlask
        SlotConditionKind.ENEMIES -> Crowd
        SlotConditionKind.TARGET -> Target
        SlotConditionKind.RARITY -> Crown
        SlotConditionKind.CAST -> Cast
        SlotConditionKind.TIME -> Clock
    }
}
