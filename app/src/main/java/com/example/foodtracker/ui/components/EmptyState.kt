package com.example.foodtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * The shared "there is nothing here" block: a short title, a sentence of
 * explanation, and optionally one action.
 *
 * **The 72dp icon disc is gone.** It was the clearest piece of Direction A
 * anatomy left in the shared components — a tinted circle whose only job was to
 * stop the block looking bare, drawn at the largest diameter in the app for the
 * screen with the least to say. House rule 1 is that structure comes from type
 * and whitespace rather than from shapes, and an empty state is the easiest
 * case to prove it on: there is no data to frame, so a frame around nothing is
 * pure decoration.
 *
 * What carries it instead is the type pair the rest of the app already uses —
 * a [AppText.titleL] line saying what happened over a [AppText.body] line
 * saying why, left-aligned on the screen gutter like every other block. Centred
 * text was a consequence of the disc (a circle has to be centred, and text
 * under a centred circle has to follow); without it the block can share the
 * page's one left margin, which is what makes it read as part of the screen
 * rather than as a dialog that lost its box.
 *
 * [title] says what happened in a few words; [message] says why, and
 * [action] — when there is a single obvious fix — offers it, so the user is
 * never left with an explanation they cannot act on.
 *
 * [icon] is deliberately still in the signature and deliberately unused. Four
 * screens pass one, two of them outside this pass's ownership, and a signature
 * change would be a diff across files other hands are in for no visible gain.
 * It also costs nothing to keep: if a later pass finds a place where a glyph
 * genuinely says something the title cannot, the call sites already name one.
 */
@Composable
fun EmptyState(
    @Suppress("UNUSED_PARAMETER") icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxxl),
    ) {
        Text(
            title,
            style = AppText.titleL,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.s))
        Text(
            message,
            style = AppText.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Long explanations read badly across the full width of a phone —
            // the eye loses the start of the next line. Capped to a comfortable
            // measure; a raw dp is a size here, not a gap, which is what rule 5
            // allows it for.
            modifier = Modifier.widthIn(max = 360.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.l))
            action()
        }
    }
}
