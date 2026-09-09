package com.sperance.exileforge.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Gold = Color(0xFFC7A66D)
val Ink = Color(0xFF100F12)
val Muted = Color(0xFFA49A8D)
val Rune = Color(0xFF9CACE5)
@Composable fun ForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Gold, onPrimary = Ink, secondary = Rune,
            background = Ink, surface = Color(0xFF1B181D), surfaceVariant = Color(0xFF282228),
            onSurface = Color(0xFFE4DCCF), onBackground = Color(0xFFE4DCCF),
            outline = Color(0xFF514333), error = Color(0xFFE89A91)),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.Medium),
            titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        ), content = content
    )
}
fun rarityColor(value: String) = when (value) {
    "UNCOMMON" -> Color(0xFF8BADE2)
    "RARE" -> Color(0xFFE0C878)
    "EPIC" -> Color(0xFFBC8ED9)
    "LEGENDARY" -> Color(0xFFDD985B)
    "MYTHICAL" -> Color(0xFFE37676)
    else -> Color(0xFFC6C1BA)
}
