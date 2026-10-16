package com.fixoom.provider.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FixoomLightColorScheme = lightColorScheme(
    primary = FixoomTeal,
    onPrimary = Color.White,
    primaryContainer = FixoomTealDark,
    onPrimaryContainer = Color.White,

    secondary = FixoomLime,
    onSecondary = FixoomTealDark,
    secondaryContainer = FixoomLavender,
    onSecondaryContainer = FixoomText,

    tertiary = FixoomOrange,
    onTertiary = Color.White,
    tertiaryContainer = FixoomPeach,
    onTertiaryContainer = FixoomText,

    background = FixoomBackground,
    onBackground = FixoomText,

    surface = FixoomCard,
    onSurface = FixoomText,
    surfaceVariant = FixoomSurfaceSoft,
    onSurfaceVariant = FixoomSecondaryText,

    outline = FixoomBorder,

    error = FixoomRed,
    onError = Color.White
)

@Composable
fun Fixoom_ServicesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        // Provider workspaces always use light mode, including on a dark-mode device.
        colorScheme = FixoomLightColorScheme,
        typography = Typography,
        content = content
    )
}
