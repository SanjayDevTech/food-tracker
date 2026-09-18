package com.example.foodtracker.ui.screens.addfood

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Non-cancelable overlay shown while the two-pass AI autofill runs. Each step
 * ([AiStep]) gets its own animated illustration (a rotating sweep ring with a
 * pulsing, morphing icon) plus a two-node "Look up → Verify" tracker so the
 * user sees where the (sometimes slow) request is.
 */
@Composable
fun AiProgressDialog(step: AiStep, verifies: Boolean = true) {
    if (step == AiStep.Idle) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AiStepIllustration(step)
                Spacer(Modifier.height(22.dp))
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ai-title",
                ) { s ->
                    Text(
                        titleFor(s),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitleFor(step, verifies),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progressFor(step, verifies) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(22.dp))
                StepTracker(step, verifies)
            }
        }
    }
}

private fun titleFor(step: AiStep): String = when (step) {
    AiStep.Fetching -> "Looking up nutrition"
    AiStep.Verifying -> "Double-checking values"
    AiStep.Done -> "All verified"
    AiStep.Idle -> ""
}

private fun subtitleFor(step: AiStep, verifies: Boolean): String = when (step) {
    AiStep.Fetching -> "Finding a typical serving and all available nutrients…"
    AiStep.Verifying -> "Re-checking the serving size and every nutrient value…"
    AiStep.Done ->
        if (verifies) "Filled the form with the verified values."
        else "Filled the form. Verify pass skipped for this model."
    AiStep.Idle -> ""
}

private fun progressFor(step: AiStep, verifies: Boolean): Float = when (step) {
    AiStep.Fetching -> if (verifies) 0.35f else 0.65f
    AiStep.Verifying -> 0.75f
    AiStep.Done -> 1f
    AiStep.Idle -> 0f
}

private fun iconFor(step: AiStep): ImageVector = when (step) {
    AiStep.Verifying -> Icons.Default.Verified
    AiStep.Done -> Icons.Default.CheckCircle
    else -> Icons.Default.Search
}

@Composable
private fun AiStepIllustration(step: AiStep) {
    val transition = rememberInfiniteTransition(label = "ai-icon-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ai-icon-scale",
    )
    val discColor = MaterialTheme.colorScheme.primaryContainer
    val iconTint = MaterialTheme.colorScheme.onPrimaryContainer

    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(if (step == AiStep.Done) 1f else pulse)
                .clip(CircleShape)
                .background(discColor),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.6f))
                },
                label = "ai-icon",
            ) { s ->
                Icon(
                    iconFor(s),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun StepTracker(step: AiStep, verifies: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        StepNode(
            label = "Look up",
            active = step == AiStep.Fetching,
            done = step != AiStep.Fetching,
        )
        // Premium models skip verification — show a single-step tracker.
        if (verifies) {
            StepConnector(filled = step != AiStep.Fetching)
            StepNode(
                label = "Verify",
                active = step == AiStep.Verifying,
                done = step == AiStep.Done,
            )
        }
    }
}

@Composable
private fun StepNode(label: String, active: Boolean, done: Boolean) {
    val onColor = MaterialTheme.colorScheme.onPrimary
    val discColor = when {
        done || active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(discColor),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(Icons.Default.Check, null, tint = onColor, modifier = Modifier.size(14.dp))
                active -> Box(Modifier.size(8.dp).clip(CircleShape).background(onColor))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active || done) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepConnector(filled: Boolean) {
    Box(
        modifier = Modifier
            .padding(top = 11.dp, start = 6.dp, end = 6.dp)
            .width(30.dp)
            .height(2.dp)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
            ),
    )
}
