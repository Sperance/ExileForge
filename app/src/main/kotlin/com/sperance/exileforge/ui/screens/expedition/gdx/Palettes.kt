package com.sperance.exileforge.ui.screens.expedition.gdx

import com.badlogic.gdx.graphics.Color

/** One biome's colours: the void around the map, the ground, the rock, and what grows on it. */
class Palette(val void: Color, val floor: Color, val wallTop: Color, val wallSide: Color, val decor: Color, val accent: Color)

private fun rgb(hex: Long) = Color(((hex shl 8) or 0xFF).toInt())

/**
 * How each biome looks — dark, in the app's Path of Exile palette, and different enough that a
 * player knows which map they are on without reading its name. A biome the client has never seen
 * gets the shore's colours rather than nothing.
 */
object Palettes {
    private val table = mapOf(
        "SHORE" to Palette(rgb(0x07090C), rgb(0x3A3528), rgb(0x2B3036), rgb(0x1B1F24), rgb(0x5E7F86), rgb(0x7FA9C8)),
        "CAVE" to Palette(rgb(0x050709), rgb(0x2A2E33), rgb(0x3B4148), rgb(0x22262B), rgb(0x4E8A93), rgb(0x63B7C4)),
        "MIRE" to Palette(rgb(0x060806), rgb(0x2C3322), rgb(0x2F3A26), rgb(0x1C2317), rgb(0x5C6E2F), rgb(0x8A9A3B)),
        "FOREST" to Palette(rgb(0x050805), rgb(0x2B3A24), rgb(0x1F3A22), rgb(0x132414), rgb(0x3F6B32), rgb(0x5E9E5A)),
        "RUINS" to Palette(rgb(0x08080A), rgb(0x3D3A33), rgb(0x57524A), rgb(0x35322D), rgb(0x6B5836), rgb(0xC8AA6E)),
        "CRYPT" to Palette(rgb(0x050507), rgb(0x2B2A30), rgb(0x3E3C46), rgb(0x24232A), rgb(0x6A6480), rgb(0xA77BCF)),
        "MINES" to Palette(rgb(0x06070A), rgb(0x2E2B28), rgb(0x3A3530), rgb(0x221F1C), rgb(0x7FC8E8), rgb(0x9FD2F0)),
        "ASH" to Palette(rgb(0x0A0605), rgb(0x3A2A22), rgb(0x40302A), rgb(0x261A15), rgb(0xD9642E), rgb(0xE08A3C)),
        "FROST" to Palette(rgb(0x070A0E), rgb(0x46505C), rgb(0x6C7A88), rgb(0x3E4852), rgb(0xBFD8EA), rgb(0xDDEEFF)),
        "TEMPLE" to Palette(rgb(0x04070A), rgb(0x23343A), rgb(0x2E4750), rgb(0x192A30), rgb(0x3B6FA8), rgb(0x63B7C4)),
    )

    fun of(biome: String): Palette = table[biome] ?: table.getValue("SHORE")

    val hero: Color = rgb(0xC8AA6E)
    val heroCape: Color = rgb(0x8A2E2E)
    val magic: Color = rgb(0x8888FF)
    val rare: Color = rgb(0xFFFF77)
    val portal: Color = rgb(0x7FA9C8)
    val blood: Color = rgb(0xB03A3A)
    val steel: Color = rgb(0xC8C8C8)

    /** A monster's body by its form, so a crab and a wraith differ before any name is read. */
    fun body(form: String): Color = when (form) {
        "CRAB" -> rgb(0xB0533A)
        "BAT" -> rgb(0x4A3E4F)
        "SERPENT" -> rgb(0x3F7A6E)
        "SLUG" -> rgb(0x8A7A3B)
        "BEAST" -> rgb(0x6E5A48)
        "SPIDER" -> rgb(0x3B3335)
        "UNDEAD" -> rgb(0xCFC7B0)
        "GOLEM" -> rgb(0x7D8590)
        "WRAITH" -> rgb(0x9FD2F0)
        "BRUTE" -> rgb(0x7A8C9A)
        else -> rgb(0x7E8A6A)
    }
}
