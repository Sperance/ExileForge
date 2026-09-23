package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.Color

/** One biome's colours: the void around the map, the ground, the rock, and what grows on it. */
class Palette(val void: Color, val floor: Color, val wallTop: Color, val wallSide: Color, val decor: Color, val accent: Color)

/**
 * How each biome looks — dark, in the app's Path of Exile palette, and different enough that a
 * player knows which map they are on without reading its name. A biome the client has never seen
 * gets the shore's colours rather than nothing.
 */
object Palettes {
    private val table = mapOf(
        "SHORE" to Palette(Color(0xFF07090C), Color(0xFF3A3528), Color(0xFF2B3036), Color(0xFF1B1F24), Color(0xFF5E7F86), Color(0xFF7FA9C8)),
        "CAVE" to Palette(Color(0xFF050709), Color(0xFF2A2E33), Color(0xFF3B4148), Color(0xFF22262B), Color(0xFF4E8A93), Color(0xFF63B7C4)),
        "MIRE" to Palette(Color(0xFF060806), Color(0xFF2C3322), Color(0xFF2F3A26), Color(0xFF1C2317), Color(0xFF5C6E2F), Color(0xFF8A9A3B)),
        "FOREST" to Palette(Color(0xFF050805), Color(0xFF2B3A24), Color(0xFF1F3A22), Color(0xFF132414), Color(0xFF3F6B32), Color(0xFF5E9E5A)),
        "RUINS" to Palette(Color(0xFF08080A), Color(0xFF3D3A33), Color(0xFF57524A), Color(0xFF35322D), Color(0xFF6B5836), Color(0xFFC8AA6E)),
        "CRYPT" to Palette(Color(0xFF050507), Color(0xFF2B2A30), Color(0xFF3E3C46), Color(0xFF24232A), Color(0xFF6A6480), Color(0xFFA77BCF)),
        "MINES" to Palette(Color(0xFF06070A), Color(0xFF2E2B28), Color(0xFF3A3530), Color(0xFF221F1C), Color(0xFF7FC8E8), Color(0xFF9FD2F0)),
        "ASH" to Palette(Color(0xFF0A0605), Color(0xFF3A2A22), Color(0xFF40302A), Color(0xFF261A15), Color(0xFFD9642E), Color(0xFFE08A3C)),
        "FROST" to Palette(Color(0xFF070A0E), Color(0xFF46505C), Color(0xFF6C7A88), Color(0xFF3E4852), Color(0xFFBFD8EA), Color(0xFFDDEEFF)),
        "TEMPLE" to Palette(Color(0xFF04070A), Color(0xFF23343A), Color(0xFF2E4750), Color(0xFF192A30), Color(0xFF3B6FA8), Color(0xFF63B7C4)),
    )

    fun of(biome: String): Palette = table[biome] ?: table.getValue("SHORE")

    val hero = Color(0xFFC8AA6E)
    val heroCape = Color(0xFF8A2E2E)
    val magic = Color(0xFF8888FF)
    val rare = Color(0xFFFFFF77)
    val portal = Color(0xFF7FA9C8)
    val blood = Color(0xFFB03A3A)
    val steel = Color(0xFFC8C8C8)

    /** A monster's body by its form, so a crab and a wraith differ before any name is read. */
    fun body(form: String): Color = when (form) {
        "CRAB" -> Color(0xFFB0533A)
        "BAT" -> Color(0xFF4A3E4F)
        "SERPENT" -> Color(0xFF3F7A6E)
        "SLUG" -> Color(0xFF8A7A3B)
        "BEAST" -> Color(0xFF6E5A48)
        "SPIDER" -> Color(0xFF3B3335)
        "UNDEAD" -> Color(0xFFCFC7B0)
        "GOLEM" -> Color(0xFF7D8590)
        "WRAITH" -> Color(0xFF9FD2F0)
        "BRUTE" -> Color(0xFF7A8C9A)
        else -> Color(0xFF7E8A6A)
    }
}

/** [base] darkened or lightened by [factor], washed toward white by [flash], at [alpha]. */
internal fun tone(base: Color, factor: Float = 1f, flash: Float = 0f, alpha: Float = 1f): Color {
    val f = flash.coerceIn(0f, 1f)
    fun channel(c: Float) = ((c * factor).coerceIn(0f, 1f) * (1 - f) + f)
    return Color(channel(base.red), channel(base.green), channel(base.blue), alpha.coerceIn(0f, 1f))
}
