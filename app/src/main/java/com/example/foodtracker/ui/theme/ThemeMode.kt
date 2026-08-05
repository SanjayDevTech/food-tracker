package com.example.foodtracker.ui.theme

/**
 * Light / dark routing for the app.
 *
 *  - [System] : Follow the OS dark-mode setting. Default for new installs.
 *  - [Light]  : Force the light scheme regardless of OS.
 *  - [Dark]   : Force the AMOLED dark scheme regardless of OS.
 *
 * The dark side is always AMOLED pitch-black; there is no separate "dark
 * (non-AMOLED)" option by design.
 */
enum class ThemeMode(val displayName: String) {
    System("System"),
    Light("Light"),
    Dark("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: System
    }
}

/**
 * Whether to derive accent colors from the brand palette or the user's
 * wallpaper (Material You).
 *
 *  - [Normal]  : Hand-curated mint + gold brand colors.
 *  - [Dynamic] : Wallpaper-tinted colors via `dynamicLightColorScheme` /
 *                `dynamicDarkColorScheme` (Android 12+). Falls back to
 *                [Normal] on older devices.
 */
enum class ColorPalette(val displayName: String) {
    Normal("Normal"),
    Dynamic("Dynamic");

    companion object {
        fun fromName(name: String?): ColorPalette =
            entries.firstOrNull { it.name == name } ?: Normal
    }
}
