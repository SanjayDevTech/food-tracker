package com.example.foodtracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = MintPrimaryDeep,
    onPrimary = Color.White,
    primaryContainer = MintPrimaryContainerLight,
    onPrimaryContainer = MintOnPrimaryContainerLight,
    secondary = MintSecondaryDeep,
    onSecondary = Color.White,
    secondaryContainer = MintSecondaryContainerLight,
    onSecondaryContainer = MintOnSecondaryContainerLight,
    tertiary = GoldTertiaryDeep,
    onTertiary = Color.White,
    tertiaryContainer = MintPrimaryContainerLight,
    onTertiaryContainer = MintOnPrimaryContainerLight,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
)

// AMOLED-optimised dark scheme. Mint-teal accents on pitch-black backgrounds —
// the surfaceContainer family supplies a subtle cool lift for cards without
// burning OLED pixels.
private val AmoledColors = darkColorScheme(
    primary = MintPrimary,
    onPrimary = MintOnPrimary,
    primaryContainer = MintPrimaryContainer,
    onPrimaryContainer = MintOnPrimaryContainer,
    secondary = MintSecondary,
    onSecondary = MintOnSecondary,
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = MintOnSecondaryContainer,
    tertiary = GoldTertiary,
    onTertiary = GoldOnTertiary,
    tertiaryContainer = GoldTertiaryContainer,
    onTertiaryContainer = GoldOnTertiaryContainer,
    error = ErrorRedBright,
    onError = Color(0xFF4F0017),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    background = AmoledBlack,
    onBackground = AmoledOnSurface,
    surface = AmoledBlack,
    onSurface = AmoledOnSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledOnSurfaceVariant,
    surfaceTint = MintPrimary,

    surfaceBright = AmoledSurfaceContainerHighest,
    surfaceDim = AmoledBlack,
    surfaceContainerLowest = AmoledSurfaceContainerLowest,
    surfaceContainerLow = AmoledSurfaceContainerLow,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    surfaceContainerHighest = AmoledSurfaceContainerHighest,

    outline = AmoledOutline,
    outlineVariant = AmoledOutlineVariant,

    inverseSurface = AmoledOnSurface,
    inverseOnSurface = AmoledBlack,
    inversePrimary = MintPrimaryDeep,

    scrim = Color(0xFF000000),
)

/**
 * Make a wallpaper-derived dynamic scheme AMOLED-safe without erasing the
 * wallpaper hue from the cards. Material You's dynamic dark scheme already
 * gives us darker-than-default container tones tinted to the user's wallpaper
 * (e.g. mauve cards if the wallpaper is purple). We keep those tones and only
 * drop the page background to true `#000000` so OLED pixels actually turn off.
 *
 * What we override:
 *   - `background` / `surface`             → pure black (the canvas)
 *   - `surfaceContainerLowest` / `surfaceDim` → pure black (the lowest tier)
 *
 * What we deliberately keep from the dynamic scheme:
 *   - `surfaceContainerLow / Container / High / Highest` — these are the
 *     wallpaper-tinted lifts that make cards visible. Overriding them was the
 *     original bug; it forced a fixed mint tint regardless of wallpaper.
 *   - `surfaceBright` — used by elevated chrome; comes hue-coordinated.
 *   - `outline` / `outlineVariant` / `surfaceVariant` — already calibrated.
 */
private fun ColorScheme.withAmoledSurfaces(): ColorScheme = copy(
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceDim = AmoledBlack,
)

@Composable
fun FoodTrackerTheme(
    themeMode: ThemeMode = ThemeMode.Amoled,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        darkTheme && themeMode == ThemeMode.AmoledDynamic && supportsDynamic ->
            dynamicDarkColorScheme(context).withAmoledSurfaces()
        darkTheme -> AmoledColors
        themeMode == ThemeMode.AmoledDynamic && supportsDynamic ->
            dynamicLightColorScheme(context)
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
