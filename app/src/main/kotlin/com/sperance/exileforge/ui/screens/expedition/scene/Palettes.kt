package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.rules.content.DesecrationGroup

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
        // The lands past the Drowned Temple (2.77.0, server 0.68.0): sand and red rock, the green
        // dark and the amber hive, fire and its fortress, the void and the rot.
        "DESERT" to Palette(Color(0xFF0B0906), Color(0xFF5A4A30), Color(0xFF7A6340), Color(0xFF4A3B26), Color(0xFFB8965A), Color(0xFFE8C878)),
        "CANYON" to Palette(Color(0xFF0A0605), Color(0xFF4E3024), Color(0xFF7A4530), Color(0xFF4A2A1E), Color(0xFF9A5A3A), Color(0xFFE07A4A)),
        "JUNGLE" to Palette(Color(0xFF030704), Color(0xFF24331E), Color(0xFF1E4424), Color(0xFF122A16), Color(0xFF3A8A3E), Color(0xFF7ADE6A)),
        "HIVE" to Palette(Color(0xFF080602), Color(0xFF3A2E16), Color(0xFF5A4518), Color(0xFF352810), Color(0xFFC8962A), Color(0xFFF0B83A)),
        "VOLCANO" to Palette(Color(0xFF0A0303), Color(0xFF2E1E1A), Color(0xFF3A2622), Color(0xFF1E1210), Color(0xFFFF5A20), Color(0xFFFF7A2A)),
        "CITADEL" to Palette(Color(0xFF070405), Color(0xFF3A2A2A), Color(0xFF5A3E3A), Color(0xFF2E1E1E), Color(0xFF8A3030), Color(0xFFFF6A3A)),
        "ABYSS" to Palette(Color(0xFF020104), Color(0xFF1E1A2A), Color(0xFF2E2640), Color(0xFF161222), Color(0xFF6A3AA0), Color(0xFFA070FF)),
        "BLIGHT" to Palette(Color(0xFF050604), Color(0xFF2E3022), Color(0xFF3A3A28), Color(0xFF22241A), Color(0xFF7A8A30), Color(0xFFB0D040)),
        // The lands 71–100 (3.42.0, server 1.40.0): the broken sky, the drowned empire, the halls of the gods.
        "SKYREACH" to Palette(Color(0xFF060A10), Color(0xFF4A5868), Color(0xFF6E8298), Color(0xFF3A4656), Color(0xFFBFE3EF), Color(0xFFE8F4FF)),
        "GLASSWASTE" to Palette(Color(0xFF050A0B), Color(0xFF3E5A5C), Color(0xFF5E8A8E), Color(0xFF304A4C), Color(0xFF9FD8E8), Color(0xFFCFF4FF)),
        "STORMPEAK" to Palette(Color(0xFF05050A), Color(0xFF2E3040), Color(0xFF454860), Color(0xFF22243A), Color(0xFF8A6AE0), Color(0xFFD0C0FF)),
        "SUNKEN" to Palette(Color(0xFF02070A), Color(0xFF1E3A40), Color(0xFF2A4E56), Color(0xFF14292E), Color(0xFF3A8A9A), Color(0xFF6FE0E8)),
        "CORAL" to Palette(Color(0xFF07040A), Color(0xFF4A2A36), Color(0xFF6A3A48), Color(0xFF3A2028), Color(0xFFE86A7A), Color(0xFFFFB0A0)),
        "TIDEVAULT" to Palette(Color(0xFF030808), Color(0xFF263A36), Color(0xFF3A5650), Color(0xFF1C2A28), Color(0xFFB8964A), Color(0xFF38B8B0)),
        "GODHALL" to Palette(Color(0xFF0A0804), Color(0xFF5A5040), Color(0xFF8A7A5A), Color(0xFF4A4232), Color(0xFFE8C860), Color(0xFFFFF0B0)),
        "ASTRAL" to Palette(Color(0xFF020210), Color(0xFF1E1E40), Color(0xFF2E2E60), Color(0xFF16163A), Color(0xFF5A62E0), Color(0xFFB0B8FF)),
        "OBLIVION" to Palette(Color(0xFF030303), Color(0xFF26262A), Color(0xFF38383E), Color(0xFF1A1A1E), Color(0xFF6A6A78), Color(0xFFD8D8E8)),
        // The Vaal zones (2.65.0): black obsidian and scarlet, whatever map the portal stood on.
        "VAAL" to Palette(Color(0xFF050102), Color(0xFF1E1416), Color(0xFF3A2A2C), Color(0xFF2A1D1F), Color(0xFF5A1A1A), Color(0xFFFF3C28)),
    )

    fun of(biome: String): Palette = table[biome] ?: table.getValue("SHORE")

    val hero = Color(0xFFC8AA6E)
    val heroCape = Color(0xFF8A2E2E)
    val bronze = Color(0xFF8A6A3A)
    /** The warm light of a torch or a brazier. */
    val torch = Color(0xFFFFB060)

    /** The stain of desecrated ground by its group (3.4.0). */
    fun desecration(group: DesecrationGroup): Color = when (group) {
        DesecrationGroup.ELEMENTAL -> Color(0xFFFF6A3D)
        DesecrationGroup.POISON -> Color(0xFF7BC44A)
        DesecrationGroup.DEFENCE -> Color(0xFF9AA7B4)
        DesecrationGroup.WEAKNESS -> Color(0xFF5B4A8C)
        DesecrationGroup.CURSE -> Color(0xFFC04BC8)
    }
    val magic = Color(0xFF8888FF)
    val rare = Color(0xFFFFFF77)
    /** A boss (server 0.32.0), in the unique items' brown-orange. */
    val unique = Color(0xFFAF6025)
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
        "SCORPION" -> Color(0xFFA07A4A)
        "INSECT" -> Color(0xFF6A7A2A)
        "DEMON" -> Color(0xFF8A2E22)
        "FUNGUS" -> Color(0xFF8A4A7A)
        // The forms of the lands 71–100 (3.42.0): the gods' constructs, beasts of crystal, fallen angels and dragons.
        "CONSTRUCT" -> Color(0xFFB8A880)
        "CRYSTAL" -> Color(0xFF9FB8E8)
        "ANGEL" -> Color(0xFFE8E0C8)
        "DRAGON" -> Color(0xFF9A3A2A)
        else -> Color(0xFF7E8A6A)
    }
}

/** [base] darkened or lightened by [factor], washed toward white by [flash], at [alpha]. */
internal fun tone(base: Color, factor: Float = 1f, flash: Float = 0f, alpha: Float = 1f): Color {
    val f = flash.coerceIn(0f, 1f)
    fun channel(c: Float) = ((c * factor).coerceIn(0f, 1f) * (1 - f) + f)
    return Color(channel(base.red), channel(base.green), channel(base.blue), alpha.coerceIn(0f, 1f))
}
