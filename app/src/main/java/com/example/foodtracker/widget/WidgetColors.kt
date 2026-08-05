package com.example.foodtracker.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import com.example.foodtracker.R

/**
 * Widget color tokens — single source of truth for both live widgets and the
 * static preview drawables.
 *
 * The live values come from [GlanceTheme.colors], which on Android 12+ pulls
 * Material You dynamic colors extracted from the user's wallpaper (same system
 * other widgets like Gdrive / Calendar use). The static preview drawables
 * reference matching `@android:color/system_*` tokens via the `@color/widget_*`
 * resource shim — so the picker preview and the placed widget render with the
 * exact same hues on any device.
 *
 * Macro chip tints are fixed brand colors — these are data-viz and need
 * distinguishable hues that DON'T change with wallpaper.
 */
object WidgetColors {
    val Surface @Composable @ReadOnlyComposable get() = GlanceTheme.colors.surfaceVariant
    val OnSurface @Composable @ReadOnlyComposable get() = GlanceTheme.colors.onSurface
    val OnSurfaceMuted @Composable @ReadOnlyComposable get() = GlanceTheme.colors.onSurfaceVariant

    val PrimaryContainer @Composable @ReadOnlyComposable get() = GlanceTheme.colors.primaryContainer
    val OnPrimaryContainer @Composable @ReadOnlyComposable get() = GlanceTheme.colors.onPrimaryContainer

    val Primary @Composable @ReadOnlyComposable get() = GlanceTheme.colors.primary
    val OnPrimary @Composable @ReadOnlyComposable get() = GlanceTheme.colors.onPrimary

    // Meal pill / recent food chip — referenced from resource so we can pick
    // brighter night-mode indices than the M3 baseline primaryContainer. The
    // baseline uses `system_accent1_700` in dark, which is too close to the
    // `system_neutral2_700` surfaceVariant we use as the widget background —
    // pills collapse into the surface visually.
    val MealPill = androidx.glance.unit.ColorProvider(R.color.widget_pill)
    val OnMealPill = androidx.glance.unit.ColorProvider(R.color.widget_on_pill)
    val Chip = androidx.glance.unit.ColorProvider(R.color.widget_chip)
    val OnChip = androidx.glance.unit.ColorProvider(R.color.widget_on_chip)

    // Tap-feedback tint for the recent-food chip. Mapped to Material You
    // tertiary (`system_accent3`) so it stays distinct from the primary
    // family even when the user's wallpaper extracts to a green primary.
    val Success = androidx.glance.unit.ColorProvider(R.color.widget_success)
    val OnSuccess = androidx.glance.unit.ColorProvider(R.color.widget_on_success)

    val ErrorBg @Composable @ReadOnlyComposable get() = GlanceTheme.colors.errorContainer
    val OnErrorBg @Composable @ReadOnlyComposable get() = GlanceTheme.colors.onErrorContainer

    // Subtle fill — semi-transparent overlay of primary that reads as a level
    // indicator rather than a loud accent. Kept as a fixed alpha overlay since
    // Glance doesn't expose "primary at X% alpha" as a theme token; we still
    // tint with the system primary tone via the alpha blend visible underneath.
    val PrimarySubtle = ColorProvider(
        day = Color(0x55000000),
        night = Color(0x55FFFFFF),
    )
    val ErrorSubtle = ColorProvider(
        day = Color(0x55BA1A1A),
        night = Color(0x55FFB4AB),
    )

    // Macro chip palette — fixed (not wallpaper-derived) because data viz needs
    // hue separation. Each macro chip is a two-layer level meter:
    //   - `*Base` (~20% alpha): always-on subtle macro tint over the widget
    //     surface — so even an empty chip reads as "this one is the carbs chip".
    //   - `*Fill` (~60% alpha): intensified version that grows from left to
    //     right with progress. The ~40% alpha gap is what creates the visible
    //     battery boundary.
    val MacroCarbsBase = ColorProvider(
        day = Color(0x338BC34A),
        night = Color(0x338BC34A),
    )
    val MacroCarbsFill = ColorProvider(
        day = Color(0x998BC34A),
        night = Color(0x998BC34A),
    )
    val MacroProteinBase = ColorProvider(
        day = Color(0x33FF7043),
        night = Color(0x33FF7043),
    )
    val MacroProteinFill = ColorProvider(
        day = Color(0x99FF7043),
        night = Color(0x99FF7043),
    )
    val MacroFatBase = ColorProvider(
        day = Color(0x33FFC107),
        night = Color(0x33FFC107),
    )
    val MacroFatFill = ColorProvider(
        day = Color(0x99FFC107),
        night = Color(0x99FFC107),
    )
}
