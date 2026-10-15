package ar.edu.uade.lensvest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The design only has a light theme.
private val LensVestColorScheme = lightColorScheme(
    primary = LensVestColors.Navy,
    onPrimary = LensVestColors.Cream,
    secondary = LensVestColors.Forest,
    onSecondary = LensVestColors.Cream,
    secondaryContainer = LensVestColors.Mint,
    onSecondaryContainer = LensVestColors.Forest,
    background = LensVestColors.Cream,
    onBackground = LensVestColors.Navy,
    surface = LensVestColors.Paper,
    onSurface = LensVestColors.Navy,
    surfaceVariant = LensVestColors.Sand,
    onSurfaceVariant = LensVestColors.Muted,
    outline = LensVestColors.Line,
    outlineVariant = LensVestColors.Line,
)

@Composable
fun LensVestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LensVestColorScheme,
        typography = LensVestTypography,
        content = content,
    )
}
