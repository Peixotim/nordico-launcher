package app.nordico.launcher.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.nordico.launcher.data.prefs.Accent

/** Tokens de cor da spec 9.1 (inspirados no Apple HIG). */
data class NordicColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val label: Color,
    val label2: Color,
    val label3: Color,
    val separator: Color,
    val fill3: Color,
    val fill2: Color,
    val material: Color,
    val segmentOn: Color,
    val accent: Color,
)

private fun accentColor(dark: Boolean, accent: Accent): Color = when (accent) {
    Accent.Orange -> if (dark) Color(0xFFFF7A2E) else Color(0xFFF2620F)
    Accent.Blue -> if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    Accent.Green -> if (dark) Color(0xFF30D158) else Color(0xFF248A3D)
    Accent.Graphite -> if (dark) Color(0xFFAEAEB2) else Color(0xFF3A3A3C)
}

fun nordicColors(dark: Boolean, accent: Accent): NordicColors =
    if (dark) {
        NordicColors(
            dark = true,
            bg = Color.Black,
            surface = Color(0xFF1C1C1E),
            label = Color.White,
            label2 = Color(0x99EBEBF5),
            label3 = Color(0x4DEBEBF5),
            separator = Color(0x8C545458),
            fill3 = Color(0x3D767680),
            fill2 = Color(0x57767680),
            material = Color(0xC728282A),
            segmentOn = Color(0xFF48484A),
            accent = accentColor(true, accent),
        )
    } else {
        NordicColors(
            dark = false,
            bg = Color(0xFFF2F2F7),
            surface = Color.White,
            label = Color.Black,
            label2 = Color(0x993C3C43),
            label3 = Color(0x4D3C3C43),
            separator = Color(0x293C3C43),
            fill3 = Color(0x24767680),
            fill2 = Color(0x38767680),
            material = Color(0xC7FFFFFF),
            segmentOn = Color.White,
            accent = accentColor(false, accent),
        )
    }

val LocalNordic = staticCompositionLocalOf { nordicColors(false, Accent.Orange) }

/** Cores que não mudam com o tema (convenções). */
object FixedColors {
    val Puck = Color(0xFF0A84FF)
    val Cool = Color(0xFF0A84FF)
    val Warm = Color(0xFFFF9F0A)
}
