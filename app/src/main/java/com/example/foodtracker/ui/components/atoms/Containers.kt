package com.example.foodtracker.ui.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Elevation
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Spacing

/**
 * Containers — design-plan §2.
 *
 * [AppCard] is the one container in the app. It replaces six implementations of
 * the same idea: `SectionCard` (22dp, `OutlinedCard`), `FormSection` (18dp,
 * hand-drawn border) and four private clones — `HomeSection`, `WaterSection`,
 * `WeightSection`, `ProgressSection` — which between them carried four radii
 * and three padding pairs for a distinction no reader could see. `FormSection`'s
 * docstring even defended the difference; that was the tell.
 */

/**
 * An outline on black. Never a fill, never a shadow.
 *
 * **Currently invoked nowhere, deliberately.** It replaced six near-identical
 * card implementations and was then itself removed from every screen by the
 * editorial pass: house rule 1 separates sections with a standing head and a
 * rule, because a stack of full-width bordered boxes was the single biggest
 * reason the rewritten app still read as the old one.
 *
 * It is kept rather than deleted because rule 1 leaves one opening — content
 * that must be set apart from an *adjacent different thing*, rather than merely
 * grouped — and when that comes up, this is the shape it should take. If it is
 * still unused the next time someone reads this, delete it.
 *
 * Pass `padding = 0.dp` when its rows need to bleed to the edge, as a divided
 * list does; the rows then own their insets.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: Dp = Spacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .border(
                BorderStroke(Elevation.hairline, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(Radius.lg),
            )
            .padding(padding),
        content = content,
    )
}

/**
 * Eyebrow caption with an optional trailing slot.
 *
 * Replaces `SectionCard`/`FormSection`'s built-in `titleMedium` heading and
 * `FormLabel`. Splitting the heading out of the card is deliberate: a card with
 * a mandatory title cannot be used for the many blocks that do not want one,
 * which is exactly how the app ended up with six cards.
 *
 * [title] is uppercased here rather than at the call site, so the one place
 * that decides what an eyebrow looks like is this function.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = AppText.eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

/** Hairline. The `outlineVariant.copy(alpha = 0.35f)` divider appeared in four files. */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Elevation.hairline)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * Tinted disc behind an icon or an emoji. Four copies today — `EmptyState`'s
 * 72dp disc, `FoodRow`'s category bubble, `AddCustomFoodRow`'s and the profile
 * `AvatarEmoji` — at four sizes and three fills.
 *
 * The default fill is `surfaceContainerHigh` rather than a tint of the content:
 * on `#000000` a 12%-alpha accent disc is indistinguishable from the canvas,
 * which is what made `FoodRow`'s twelve pastel bubbles vanish.
 */
@Composable
fun IconCircle(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** [IconCircle] around a vector icon, which is the common case. */
@Composable
fun IconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.45f,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconCircle(modifier = modifier, size = size, container = container) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}
