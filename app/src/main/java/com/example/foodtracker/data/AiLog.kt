package com.example.foodtracker.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What happened on one AI request — one attempt, not one lookup, so a call that
 * retried twice before failing leaves three lines.
 *
 * [detail] is the provider's own error text or the exception, already run
 * through [AiLog.redact]. Nothing here is ever written to disk.
 */
data class AiLogEntry(
    val atMillis: Long,
    /** "Look up" or "Verify" — which of the two passes this attempt belongs to. */
    val pass: String,
    val provider: String,
    val model: String,
    /** 1-based, so "2 of 3" reads straight out of the entry. */
    val attempt: Int,
    val attemptsAllowed: Int,
    val outcome: AiLogOutcome,
    /** HTTP status, or null for a network error / a reply that would not parse. */
    val httpCode: Int? = null,
    val durationMs: Long = 0,
    /** Food name (+ brand) the request asked about — the user's own data. */
    val subject: String? = null,
    val detail: String? = null,
)

enum class AiLogOutcome { Ok, HttpError, NetworkError, ParseError }

/**
 * A short in-memory trace of the last few AI autofill requests.
 *
 * **Why this exists.** The only record of a failed lookup used to be
 * `android.util.Log`, which needs a cable and a desktop — exactly what you do
 * not have when a release build fails on your own phone in a restaurant. The
 * user-facing error string says what to do about the common cases, but it
 * cannot say which model answered, how long it waited, how many attempts it
 * burned, or what the provider actually replied. This holds all of that until
 * the process dies.
 *
 * **It never holds the API key.** The key travels in a request *header* and
 * headers are not recorded here at all — only the URL, the status and the
 * provider's reply body. [redact] is the second line of defence for the reply
 * body, on the chance a provider echoes a credential back in an error. Nothing
 * in this file writes to disk, to a backup, or to `Log`; `ai_prefs` stays the
 * only place a key lives, and it is excluded from backup for the same reason.
 *
 * **In-memory and capped.** [MAX] entries, oldest dropped. A trace that
 * survived a restart would be a log file holding provider replies about the
 * user's food, which is not a thing this app keeps.
 */
object AiLog {

    private const val MAX = 40
    private const val DETAIL_CHARS = 800

    private val _entries = MutableStateFlow<List<AiLogEntry>>(emptyList())

    /** Newest first — the order the sheet reads them in. */
    val entries: StateFlow<List<AiLogEntry>> = _entries.asStateFlow()

    fun record(entry: AiLogEntry) {
        _entries.update { current -> (listOf(entry) + current).take(MAX) }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    /**
     * Blank out anything shaped like an API credential, then truncate.
     *
     * The patterns cover the three providers' own key formats plus a bearer
     * token, because those are what could plausibly come back in an echoed
     * request. This is belt-and-braces: the key is not passed to this function
     * in the first place, and the bodies that reach it are error replies, not
     * requests.
     */
    fun redact(text: String?): String? {
        if (text.isNullOrBlank()) return null
        var out: String = text
        KEY_SHAPES.forEach { pattern -> out = pattern.replace(out, "[redacted]") }
        return out.trim().take(DETAIL_CHARS)
    }

    private val KEY_SHAPES = listOf(
        // OpenAI and Anthropic: sk-…, sk-ant-…, sk-proj-…
        Regex("""sk-[A-Za-z0-9_\-]{10,}"""),
        // Google AI Studio
        Regex("""AIza[A-Za-z0-9_\-]{10,}"""),
        Regex("""(?i)bearer\s+[A-Za-z0-9._\-]{10,}"""),
        // A JSON field that names itself a key, whatever the value looks like.
        Regex("""(?i)"(api[_-]?key|x-api-key|authorization)"\s*:\s*"[^"]*""""),
    )
}
