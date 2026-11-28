package com.example.foodtracker.ui.screens.addfood

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.AiLog
import com.example.foodtracker.data.AiLogEntry
import com.example.foodtracker.data.AiLogOutcome
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.components.atoms.AppTextButton
import com.example.foodtracker.ui.components.atoms.Section
import com.example.foodtracker.ui.components.atoms.AppSheet
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * What the AI actually did, readable on the device that it failed on.
 *
 * **Why a sheet over the form and not a page.** The thing you want when a
 * lookup fails is to read the reason and then change one input — the model, the
 * brand, the instructions — and go again. A page would take the half-filled
 * form off screen to show you four lines of text. A sheet raises them over it
 * and gives it straight back.
 *
 * **Why it exists at all.** `android.util.Log` needs a cable and a desktop,
 * which is precisely what you do not have when the release build on your own
 * phone says "that model is rate-limited right now". The user-facing error
 * string covers the common causes, but it cannot say which model answered, how
 * many of the three attempts were spent, how long each waited, or what the
 * provider literally replied — and those are the four facts that separate "my
 * key is wrong" from "Google moved the alias again".
 *
 * **The API key is not here and cannot get here.** The key travels in a request
 * header; headers are not recorded at all. What is recorded is the status, the
 * timing and the provider's reply body, and that body goes through
 * [AiLog.redact] on the way in. See [AiLog] for the rest of that argument.
 *
 * [AiLog] is in-memory, so this is empty after a restart. That is deliberate: a
 * trace that survived would be a file of provider replies about the user's
 * food, which is not a thing this app keeps.
 */
@Composable
fun AiDevLogSheet(onDismiss: () -> Unit) {
    val entries by AiLog.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    AppSheet(title = "AI request log", onDismiss = onDismiss) {
        if (entries.isEmpty()) {
            Text(
                "Nothing yet. Every AI lookup since the app started is listed here — " +
                    "each attempt, its status and what the provider replied.",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
            )
            return@AppSheet
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${entries.size} ${if (entries.size == 1) "attempt" else "attempts"}, newest first",
                style = AppText.bodyS,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            AppTextButton(text = "Copy", onClick = { copyToClipboard(context, entries) })
            AppTextButton(text = "Clear", onClick = { AiLog.clear() })
        }

        entries.forEach { entry ->
            Spacer(Modifier.height(Spacing.xl))
            Section(
                title = "${entry.pass} · attempt ${entry.attempt} of ${entry.attemptsAllowed}",
                trailing = {
                    Text(
                        statusText(entry),
                        style = AppText.label,
                        color = if (entry.outcome == AiLogOutcome.Ok) scheme.onSurface
                        else scheme.error,
                    )
                },
            ) {
                Text(
                    buildString {
                        append(entry.provider)
                        append(" · ")
                        append(entry.model)
                        if (entry.durationMs > 0) append(" · ${"%.1f".format(Locale.US, entry.durationMs / 1000.0)} s")
                        append(" · ")
                        append(clockTime(entry.atMillis))
                    },
                    style = AppText.bodyS,
                    color = scheme.onSurfaceVariant,
                )
                entry.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                    Spacer(Modifier.height(Spacing.xs))
                    Text(subject, style = AppText.body, color = scheme.onSurface)
                }
                entry.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                    Spacer(Modifier.height(Spacing.s))
                    // Monospace: this is a JSON body with its own structure, and
                    // proportional type turns a nested error object into a wall.
                    Text(
                        detail,
                        style = AppText.bodyS.copy(fontFamily = FontFamily.Monospace),
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        AppDivider()
        Spacer(Modifier.height(Spacing.m))
        Text(
            "Your API key is never recorded here — request headers are not logged, and " +
                "anything key-shaped in a reply is stripped before it is stored.",
            style = AppText.bodyS,
            color = scheme.onSurfaceVariant,
        )
    }
}

private fun statusText(entry: AiLogEntry): String = when (entry.outcome) {
    AiLogOutcome.Ok -> "OK ${entry.httpCode ?: 200}"
    AiLogOutcome.HttpError -> "HTTP ${entry.httpCode}"
    AiLogOutcome.NetworkError -> "Network"
    AiLogOutcome.ParseError -> "Unreadable"
}

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun clockTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(CLOCK)

/**
 * The whole trace as plain text, so it can be pasted into a bug report without
 * retyping a JSON error body off a phone screen.
 */
private fun copyToClipboard(context: Context, entries: List<AiLogEntry>) {
    val text = entries.joinToString("\n\n") { e ->
        buildString {
            append("[${clockTime(e.atMillis)}] ${e.pass} attempt ${e.attempt}/${e.attemptsAllowed} — ")
            append(statusText(e))
            append("\n${e.provider} · ${e.model} · ${e.durationMs} ms")
            e.subject?.let { append("\nSubject: $it") }
            e.detail?.let { append("\n$it") }
        }
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("AI request log", text))
}
