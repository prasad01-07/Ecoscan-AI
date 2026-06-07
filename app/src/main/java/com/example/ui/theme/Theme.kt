package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LeafPrimaryGreen,
    secondary = EmeraldAccent,
    tertiary = MintTealTertiary,
    background = DeepForestBackground,
    surface = SpruceCardSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFE8F5E9)
)

private val LightColorScheme = lightColorScheme(
    primary = FreshGreenPrimary,
    secondary = SageLeafSecondary,
    tertiary = TenderSproutAccent,
    background = CleanWhiteBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = DarkWoodText,
    onTertiary = DarkWoodText,
    onBackground = DarkWoodText,
    onSurface = DarkWoodText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamic color false to prioritize our gorgeous botanical design branding identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
