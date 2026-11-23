package com.example.foodtracker.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Spacing

/**
 * A standing head with a rule under it, then its content.
 *
 * **Why a rule rather than a gap.** Whitespace is the weakest separator and the
 * most expensive one: 32dp between sections cost a third of a screen and still
 * read as one undifferentiated column, because nothing told the eye where a
 * section *ended*. A hairline under the head is the oldest device in editorial
 * layout for exactly this, and it does the job in 1dp — so the gap between
 * sections comes **down** to 24dp rather than up.
 *
 * It is also not a card. The rule belongs to the heading, not to a container
 * drawn around the content, so sections stay open to the page and the content
 * keeps bleeding to the screen gutter. That distinction is the whole difference
 * between this and the stack of outlined boxes the rewrite removed.
 *
 * [trailing] is the section's summary figure — `2 981 kcal`, `18 of 29 on
 * track` — which sits on the head's baseline, right-aligned, so every section
 * states its total in the same place.
 *
 * **The head is `onSurface`, the rows under it are `onSurfaceVariant`.** It was
 * `onSurfaceVariant` too, which meant a section head and a row label inside it
 * were the same 11sp uppercase in the same colour: on Today, `INTAKE` and the
 * `WATER` / `WEIGHT` labels beneath it were typographically identical, so three
 * stacked sections read as one undifferentiated list of eyebrows and nothing
 * said which of them was the heading. The rule under the head could not fix
 * that on its own — a rule says *where* a section starts, not *what* is the
 * head. One step of contrast does, and it costs no space.
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                style = AppText.eyebrow,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke(this)
        }
        Spacer(Modifier.height(Spacing.s))
        AppDivider()
        Spacer(Modifier.height(Spacing.l))
        content()
    }
}
