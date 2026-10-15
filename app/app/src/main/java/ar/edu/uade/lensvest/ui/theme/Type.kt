package ar.edu.uade.lensvest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ar.edu.uade.lensvest.R

// Both families are variable fonts. Android doesn't apply optical size on its own,
// so each style sets "opsz" explicitly (variation axes need API 26+).

private fun frauncesSettings(opticalSize: Float) = FontVariation.Settings(
    FontVariation.weight(600),
    FontVariation.Setting("opsz", opticalSize),
    FontVariation.Setting("SOFT", 0f),
    FontVariation.Setting("WONK", 1f),
)

/** Fraunces SemiBold (+ italic) tuned for one text size. */
fun fraunces(size: Float) = FontFamily(
    Font(R.font.fraunces, FontWeight.SemiBold, FontStyle.Normal, variationSettings = frauncesSettings(size)),
    Font(R.font.fraunces_italic, FontWeight.SemiBold, FontStyle.Italic, variationSettings = frauncesSettings(size)),
)

private fun dmSans(weight: FontWeight) = Font(
    R.font.dm_sans,
    weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("opsz", 14f),
    ),
)

val DmSans = FontFamily(dmSans(FontWeight.Normal), dmSans(FontWeight.Medium), dmSans(FontWeight.SemiBold))

private fun serif(size: Int, tracking: Float) = TextStyle(
    fontFamily = fraunces(size.toFloat()),
    fontWeight = FontWeight.SemiBold,
    fontSize = size.sp,
    lineHeight = 1.12.em,
    letterSpacing = tracking.sp,
)

private fun sans(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Double? = null) = TextStyle(
    fontFamily = DmSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight?.em ?: TextStyle.Default.lineHeight,
)

val LensVestTypography = Typography(
    displayLarge = serif(40, -0.6f),
    headlineLarge = serif(30, -0.45f),
    headlineMedium = serif(28, -0.42f),
    headlineSmall = serif(24, -0.36f),
    titleLarge = serif(22, -0.33f),
    titleMedium = serif(20, -0.2f),
    bodyLarge = sans(16, lineHeight = 1.5),
    bodyMedium = sans(15, lineHeight = 1.5),
    bodySmall = sans(13, lineHeight = 1.4),
    labelLarge = sans(16, FontWeight.SemiBold),
    labelMedium = sans(13, FontWeight.SemiBold),
    labelSmall = sans(11, FontWeight.SemiBold).copy(letterSpacing = 1.1.sp),
)
