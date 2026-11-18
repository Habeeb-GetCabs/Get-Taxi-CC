package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val TaxiDarkColorScheme = darkColorScheme(
    primary = TaxiYellow,
    onPrimary = TaxiBlack,
    primaryContainer = TaxiCharcoalElevated,
    onPrimaryContainer = TaxiYellowBright,
    secondary = TaxiWhite,
    onSecondary = TaxiBlack,
    secondaryContainer = TaxiCharcoalSurface,
    onSecondaryContainer = TaxiWhite,
    tertiary = TaxiYellowBright,
    onTertiary = TaxiBlack,
    background = TaxiCharcoal,
    onBackground = TaxiWhite,
    surface = TaxiCharcoalSurface,
    onSurface = TaxiWhite,
    surfaceVariant = TaxiCharcoalElevated,
    onSurfaceVariant = TaxiMutedText,
    outline = TaxiCharcoalBorder,
    error = TaxiRed,
    onError = TaxiWhite
)

private val TaxiLightColorScheme = lightColorScheme(
    primary = TaxiYellow,
    onPrimary = TaxiBlack,
    primaryContainer = TaxiCharcoal,
    onPrimaryContainer = TaxiYellowBright,
    secondary = TaxiCharcoal,
    onSecondary = TaxiWhite,
    secondaryContainer = TaxiOffWhite,
    onSecondaryContainer = TaxiCharcoal,
    tertiary = TaxiYellowBright,
    onTertiary = TaxiBlack,
    background = TaxiOffWhite,
    onBackground = TaxiBlack,
    surface = TaxiWhite,
    onSurface = TaxiBlack,
    surfaceVariant = TaxiOffWhite,
    onSurfaceVariant = TaxiDarkMutedText,
    outline = TaxiLightBorder,
    error = TaxiRed,
    onError = TaxiWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TaxiDarkColorScheme
        else -> TaxiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
