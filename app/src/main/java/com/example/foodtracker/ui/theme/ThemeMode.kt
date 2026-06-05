package com.example.foodtracker.ui.theme

/**
 * How the dark color scheme is built.
 *
 *  - [Amoled]        : Static mint-teal palette, pitch-black AMOLED surfaces.
 *  - [AmoledDynamic] : Wallpaper-based Material You accents (Android 12+),
 *                      still rendered on pitch-black AMOLED surfaces. Falls
 *                      back to [Amoled] on older devices.
 */
enum class ThemeMode(val displayName: String) {
    Amoled("AMOLED"),
    AmoledDynamic("AMOLED dynamic");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: Amoled
    }
}
