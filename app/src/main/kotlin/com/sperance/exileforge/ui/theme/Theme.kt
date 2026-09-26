package com.sperance.exileforge.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.Dp
import com.sperance.exileforge.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * «Эфир» (2.80.0, the owner's pick of six mockups): thin lines over dark basalt, air between things,
 * and light only on what is alive — the chosen, the ready, the mana. The names are the roles the app
 * has always drawn with; since 2.80.0 `Gold` is the ether accent and `Parchment` the reading text.
 */
val Ink = Color(0xFF0C1115)
val Abyss = Color(0xFF0A0E12)
val Panel = Color(0xFF0F161B)
val PanelRaised = Color(0xFF16212A)
/** The accent: ether, a pale sea-glass cyan. */
val Gold = Color(0xFF7FF0E0)
/** Titles and the lit state of the accent. */
val GoldBright = Color(0xFFF0FBFB)
/** Hairlines and frames. */
val Bronze = Color(0xFF1F2C35)
/** Reading text. */
val Parchment = Color(0xFFD5E0E6)
val Muted = Color(0xFF7A8D99)
/** Frost: a second, cooler light for secondary marks. */
val Rune = Color(0xFF8FD3FF)
/** Every modifier's sentence — item, monster, map or atlas — in one sky blue (2.73.0). */
val ModBlue = Color(0xFF8EC5FF)
val LifeRed = Color(0xFFD24A43)
val ManaBlue = Color(0xFF6FB6FF)
val ShieldCyan = Color(0xFF63B7C4)
val Blood = Color(0xFF8A2E2E)
/** Regeneration and success. */
val Vital = Color(0xFF7BE0A6)
/** Fire and other burning damage on the arena stage. */
val Ember = Color(0xFFD9642E)
/** The colours Path of Exile gives a crafted and a fractured modifier, and the two influences. */
val Crafted = Color(0xFFB4B4FF)
val Fractured = Color(0xFFA29162)
/** The smith's handcrafted lines (2.42.0): forge copper. A map's alchemy lines read in `Vital`. */
val Handcrafted = Color(0xFFD08A4E)
val Shaper = Color(0xFF9FD2F0)
val Elder = Color(0xFFA77BCF)

/** The three faces of «Эфир»: a wide display for titles, a quiet grotesque to read, a mono for numbers. */
val Display = FontFamily(Font(R.font.unbounded_medium, FontWeight.Medium))
val Reading = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal), Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold), Font(R.font.onest_semibold, FontWeight.Bold),
)
val Numeric = FontFamily(Font(R.font.jetbrains_mono_medium, FontWeight.Medium))

/** Item frames follow Path of Exile rarity colours; unknown values stay bone white. */
fun rarityColor(value: String) = when (value) {
    "UNCOMMON" -> Color(0xFF8888FF)
    "RARE" -> Color(0xFFFFFF77)
    "UNIQUE" -> Color(0xFFAF6025)
    "MYTHICAL" -> Color(0xFFE05A4E)
    else -> Color(0xFFC8C8C8)
}

/** A panel's ground: a faint lift at the top edge, the accent only as a breath of light. */
fun panelBrush(accent: Color = Gold) = Brush.verticalGradient(listOf(Color(0xFF111A20), accent.copy(alpha = .025f).compositeOver(Panel), Panel))
fun voidBrush() = Brush.verticalGradient(listOf(Color(0xFF0E151A), Ink, Abyss))

/**
 * Light around what is alive (2.80.0): a coloured halo under the shape, nothing drawn when [on] is false.
 * On Android 9+ the halo takes the colour; below it falls back to a soft dark shadow.
 */
fun Modifier.glow(color: Color = Gold, on: Boolean = true, radius: Dp = 10.dp, shape: Shape = RoundedCornerShape(8.dp)): Modifier =
    if (!on) this else this.shadow(radius, shape, clip = false, ambientColor = color, spotColor = color)

@Composable fun ForgeTheme(content: @Composable () -> Unit) {
    fun text(family: FontFamily, size: Float, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f, line: Float = size * 1.35f) =
        TextStyle(fontFamily = family, fontSize = size.sp, fontWeight = weight, letterSpacing = tracking.sp, lineHeight = line.sp)
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Gold, onPrimary = Color(0xFF032320), secondary = Rune, onSecondary = Ink,
            secondaryContainer = PanelRaised, onSecondaryContainer = Parchment, tertiary = GoldBright,
            background = Ink, surface = Panel, surfaceVariant = PanelRaised, surfaceContainerHighest = PanelRaised,
            surfaceContainer = Panel, surfaceContainerHigh = PanelRaised, surfaceContainerLow = Panel,
            onSurface = Parchment, onSurfaceVariant = Muted, onBackground = Parchment,
            outline = Bronze, outlineVariant = Color(0xFF18232B), error = LifeRed, onError = Ink),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp), extraLarge = RoundedCornerShape(18.dp)
        ),
        typography = Typography(
            displaySmall = text(Display, 24f, FontWeight.Medium, .5f),
            headlineLarge = text(Display, 20f, FontWeight.Medium, 1.2f),
            headlineMedium = text(Display, 18f, FontWeight.Medium, 1f),
            headlineSmall = text(Display, 16f, FontWeight.Medium, .8f),
            titleLarge = text(Display, 15f, FontWeight.Medium, 1f),
            titleMedium = text(Reading, 15f, FontWeight.SemiBold, .1f),
            titleSmall = text(Reading, 14f, FontWeight.SemiBold, .1f),
            bodyLarge = text(Reading, 15f),
            bodyMedium = text(Reading, 14f),
            bodySmall = text(Reading, 12.5f),
            labelLarge = text(Reading, 13f, FontWeight.Medium, .4f),
            labelMedium = text(Reading, 12f, FontWeight.Medium, .3f),
            labelSmall = text(Reading, 10.5f, FontWeight.SemiBold, .8f),
        ), content = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground, content = content)
        }
    )
}
