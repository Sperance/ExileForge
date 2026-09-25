package com.sperance.exileforge.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sperance.exileforge.core.display.Glyph
import com.sperance.exileforge.core.display.IconKey
import com.sperance.exileforge.core.display.icon

/**
 * The drawing of each meaning: the one table a new [Glyph] has to be added to.
 *
 * It is a `when` over the enum, so a glyph without a drawing does not compile — which is the whole
 * difference from the keyword search it replaced, where a missing case silently became a sigil.
 */
val Glyph.vector: ImageVector get() = when (this) {
    Glyph.LIFE -> Icons.Outlined.FavoriteBorder
    Glyph.MANA -> Icons.Outlined.WaterDrop
    Glyph.SHIELD -> ForgeGlyphs.Kite
    Glyph.FIRE -> Icons.Outlined.LocalFireDepartment
    Glyph.COLD -> Icons.Outlined.AcUnit
    Glyph.LIGHTNING -> Icons.Outlined.Bolt
    Glyph.CHAOS -> ForgeGlyphs.Skull
    Glyph.ATTACK -> ForgeGlyphs.Swords
    Glyph.DEFENCE -> ForgeGlyphs.Kite
    Glyph.EVASION -> Icons.Outlined.DirectionsRun
    Glyph.SPEED -> Icons.Outlined.Speed
    Glyph.CRITICAL -> Icons.Outlined.GpsFixed
    Glyph.ATTRIBUTE -> Icons.Outlined.FitnessCenter
    Glyph.LEECH -> Icons.Outlined.Opacity
    Glyph.LEVEL -> Icons.Outlined.TrendingUp
    Glyph.CURRENCY -> ForgeGlyphs.Orb
    Glyph.CHARACTER -> ForgeGlyphs.Exile
    Glyph.ITEM -> ForgeGlyphs.Stash
    Glyph.RARITY -> Icons.Outlined.AutoAwesome
    Glyph.STATE -> ForgeGlyphs.Sigil
    Glyph.TREE -> ForgeGlyphs.Constellation
    Glyph.TEXT -> ForgeGlyphs.Scroll
    Glyph.RULE -> ForgeGlyphs.Tome
    Glyph.REFERENCE -> ForgeGlyphs.Chain
    Glyph.IDENTITY -> Icons.Outlined.Fingerprint
    Glyph.SERVER -> ForgeGlyphs.Portal
    Glyph.CRAFT -> ForgeGlyphs.Anvil
    Glyph.MAP -> ForgeGlyphs.Atlas
    Glyph.GEM -> ForgeGlyphs.Gem
    Glyph.IMAGE -> Icons.Outlined.Image
    Glyph.INFO -> ForgeGlyphs.Sigil
    Glyph.ALERT -> Icons.Outlined.ErrorOutline
}

@Composable fun GlyphIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier) = Icon(glyph.vector, null, tint = tint, modifier = modifier)

/**
 * A characteristic: the server's drawing when its icon set covers the stat, the bundled glyph for
 * its meaning otherwise — the same fallback every icon here makes.
 */
@Composable fun StatIcon(stat: String, tint: Color, modifier: Modifier = Modifier) =
    Icon(icon(IconKey.stat(stat))?.let(::spriteVector) ?: Glyph.ofStat(stat).vector, null, tint = tint, modifier = modifier)
