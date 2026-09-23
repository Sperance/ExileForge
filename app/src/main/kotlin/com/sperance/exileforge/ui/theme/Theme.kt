package com.sperance.exileforge.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Wraith-lit stone and tarnished gold: the palette of an exile's stash. */
val Ink = Color(0xFF07090C)
val Abyss = Color(0xFF0B0E13)
val Panel = Color(0xFF14181F)
val PanelRaised = Color(0xFF1C222B)
val Gold = Color(0xFFC8AA6E)
val GoldBright = Color(0xFFF0E2C0)
val Bronze = Color(0xFF6B5836)
val Parchment = Color(0xFFE4DCCF)
val Muted = Color(0xFF8E8676)
val Rune = Color(0xFF7FA9C8)
val LifeRed = Color(0xFFB03A3A)
val ManaBlue = Color(0xFF3B6FA8)
val ShieldCyan = Color(0xFF63B7C4)
val Blood = Color(0xFF8A2E2E)
/** Regeneration and flasks; the only green the palette allows. */
val Vital = Color(0xFF5E9E5A)
/** Fire and other burning damage on the arena stage. */
val Ember = Color(0xFFD9642E)
/** The colours Path of Exile gives a crafted and a fractured modifier, and the two influences. */
val Crafted = Color(0xFFB4B4FF)
val Fractured = Color(0xFFA29162)
val Shaper = Color(0xFF9FD2F0)
val Elder = Color(0xFFA77BCF)

/** Item frames follow Path of Exile rarity colours; unknown values stay bone white. */
fun rarityColor(value: String) = when (value) {
    "UNCOMMON" -> Color(0xFF8888FF)
    "RARE" -> Color(0xFFFFFF77)
    "EPIC" -> Color(0xFFBC8ED9)
    "UNIQUE" -> Color(0xFFAF6025)
    "MYTHICAL" -> Color(0xFFE05A4E)
    else -> Color(0xFFC8C8C8)
}

/** Stone plate behind every panel: a lit top edge fading into the void. */
fun panelBrush(accent: Color = Gold) = Brush.verticalGradient(
    listOf(accent.copy(alpha = .10f), PanelRaised, Panel)
)
fun voidBrush() = Brush.verticalGradient(listOf(Abyss, Ink, Color(0xFF0A0D12)))

/** The dark plate the whole app is drawn on; titles are carved wide, as if cut in stone. */
@Composable fun ForgeTheme(content: @Composable () -> Unit) {
    val carved: (Float) -> TextUnit = { it.sp }
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Gold, onPrimary = Ink, secondary = Rune, onSecondary = Ink,
            secondaryContainer = PanelRaised, onSecondaryContainer = Parchment, tertiary = GoldBright,
            background = Ink, surface = Panel, surfaceVariant = PanelRaised, surfaceContainerHighest = PanelRaised,
            onSurface = Parchment, onSurfaceVariant = Muted, onBackground = Parchment,
            outline = Bronze, outlineVariant = Color(0xFF2A3038), error = Color(0xFFE07B6F), onError = Ink),
        shapes = Shapes(
            extraSmall = CutCornerShape(3.dp), small = CutCornerShape(4.dp), medium = CutCornerShape(6.dp),
            large = RoundedCornerShape(4.dp), extraLarge = RoundedCornerShape(6.dp)
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = carved(2f)),
            headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = carved(1f)),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = carved(1f)),
            titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = carved(.5f)),
            labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = carved(1.6f)),
            labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, letterSpacing = carved(.8f)),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = carved(1f))
        ), content = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground, content = content)
        }
    )
}
