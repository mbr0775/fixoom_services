package com.fixoom.customer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val FixoomDarkColorScheme = darkColorScheme(
    primary = FixoomLime,
    onPrimary = FixoomTealDark,
    primaryContainer = FixoomTeal,
    onPrimaryContainer = Color.White,

    secondary = FixoomLavender,
    onSecondary = FixoomTealDark,
    secondaryContainer = FixoomDarkSurfaceSoft,
    onSecondaryContainer = FixoomDarkText,

    tertiary = FixoomOrange,
    onTertiary = Color.White,
    tertiaryContainer = FixoomDarkSurfaceSoft,
    onTertiaryContainer = FixoomDarkText,

    background = FixoomDarkBackground,
    onBackground = FixoomDarkText,

    surface = FixoomDarkSurface,
    onSurface = FixoomDarkText,
    surfaceVariant = FixoomDarkSurfaceSoft,
    onSurfaceVariant = FixoomDarkSecondaryText,

    outline = FixoomDarkBorder,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun Fixoom_ServicesTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            FixoomDarkColorScheme
        } else {
            FixoomLightColorScheme
        },
        typography = Typography,
        content = content
    )
}