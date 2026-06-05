package com.example.foodtracker.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Two haptic tones used across the app — anything more granular is overkill.
 *
 *  - [confirm] : something happened (food logged, weight saved, reminder set).
 *    Use on primary action buttons and one-tap quick-log.
 *  - [light]   : a quiet toggle (favourite star, reminder switch). Skip on
 *    chips, sliders, FAB navigation — those already have visual feedback.
 *
 * We use the platform [Vibrator] directly rather than Compose's [androidx.compose.ui.hapticfeedback.HapticFeedback].
 * Compose's HapticFeedbackType.LongPress / TextHandleMove maps to HapticFeedbackConstants that many devices
 * (and especially privacy-hardened ROMs like GrapheneOS) treat as no-ops if the haptic-feedback-enabled
 * system flag is off. Predefined VibrationEffects bypass that flag and play reliably.
 *
 * Gating: every dispatch respects the user-controlled `hapticsEnabled` flag stored in
 * ProfileStore — see [rememberAppHaptics].
 */
class AppHaptics(
    private val vibrator: Vibrator?,
    private val enabled: Boolean,
) {
    fun confirm() {
        if (!enabled) return
        vibrator?.takeIf { it.hasVibrator() }
            ?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    fun light() {
        if (!enabled) return
        vibrator?.takeIf { it.hasVibrator() }
            ?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val enabled by container.profileStore.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)
    return remember(context, enabled) {
        AppHaptics(vibrator = context.systemVibrator(), enabled = enabled)
    }
}

private fun Context.systemVibrator(): Vibrator? {
    // VibratorManager is the API 31+ way; the legacy Context.VIBRATOR_SERVICE
    // still works as a fallback but is deprecated. minSdk = 33 here.
    val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    return manager?.defaultVibrator
}
