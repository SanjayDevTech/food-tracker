package com.example.foodtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The app theme — design-plan §1, phase 1.
 *
 * One scheme, always dark, no arguments. What used to be here:
 *
 *  - `MaterialExpressiveTheme`, which installed the expressive motion scheme
 *    (spring overshoot, shape-morph on press). Removed with the rest of
 *    Expressive; the plan's motion table bans both.
 *  - A light scheme and an `isSystemInDarkTheme()` branch. Decision 11 dropped
 *    light mode: one palette to author, one to verify, and every later phase
 *    gets checked once instead of twice.
 *  - `dynamicLight/DarkColorScheme` plus `withAmoledSurfaces()`, which existed
 *    to make wallpaper-derived colour OLED-safe. A wallpaper cannot be held to
 *    the contrast ratios §1 is built on, so dynamic colour is gone.
 *
 * `ColorPalette` and `ThemeMode` went with them. Their DataStore keys are left
 * in place and simply unread — no migration, and a downgrade still finds them.
 */
private val AppColors = darkColorScheme(
    primary = AppPrimary,
    onPrimary = AppOnPrimary,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AppOnPrimaryContainer,
    secondary = AppPrimary,
    onSecondary = AppOnPrimary,
    secondaryContainer = AppPrimaryContainer,
    onSecondaryContainer = AppOnPrimaryContainer,
    tertiary = AppTertiary,
    onTertiary = AppOnTertiary,
    tertiaryContainer = AppTertiaryContainer,
    onTertiaryContainer = AppOnTertiaryContainer,
    error = AppError,
    onError = AppOnError,
    errorContainer = AppErrorContainer,
    onErrorContainer = AppOnErrorContainer,

    background = AppBlack,
    onBackground = AppOnSurface,
    surface = AppBlack,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppOnSurfaceVariant,
    surfaceTint = AppPrimary,

    surfaceBright = AppSurfaceHighest,
    surfaceDim = AppBlack,
    surfaceContainerLowest = AppBlack,
    surfaceContainerLow = AppSurfaceLow,
    surfaceContainer = AppSurface,
    surfaceContainerHigh = AppSurfaceHigh,
    surfaceContainerHighest = AppSurfaceHighest,

    outline = AppOutline,
    outlineVariant = AppOutlineVariant,

    inverseSurface = AppOnSurface,
    inverseOnSurface = AppBlack,
    inversePrimary = AppPrimaryDeep,

    scrim = AppBlack,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = Typography,
        content = content,
    )
}
