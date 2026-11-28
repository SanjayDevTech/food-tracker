package com.example.foodtracker.ui.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The device locale, read so that Compose notices when it changes.
 *
 * `Locale.getDefault()` is a plain static read. A composable that formats a
 * date with it keeps the old language until something unrelated happens to
 * recompose it — and on a header whose only text is that date, that can be
 * never. Going through the configuration makes the locale part of the
 * composition, so switching the phone's language redraws the screen.
 *
 * Numbers deliberately do **not** come through here. [formatValue] pins
 * Locale.US so the thousands mark and the decimal point cannot disagree; this
 * is for dates, day names and month names, which should follow the device.
 */
@Composable
@ReadOnlyComposable
fun deviceLocale(): Locale = LocalConfiguration.current.locales[0]
