package com.example.foodtracker.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Spacing

/**
 * The chrome every full-width sheet in the app shares.
 *
 * It began as `YouSheet`, the chrome behind three of the six App rows, because
 * each was a drill-down from one row of one section: a read-only list, a toggle
 * with two pickers, a count with one destructive action. None of them earns a
 * back stack entry, and a page would have to invent a title bar and a back
 * arrow to get you out of somewhere you did not really go. The plan's nutrition
 * breakdown and the AI request log are the same shape of thing, so it moved
 * here rather than being copied twice under a name that lied about where it
 * belonged.
 *
 * It is not the chrome for the *compact* sheets — `ServingSheet` and the water
 * and weigh-in sheets each own a footer and a keyboard-aware layout that this
 * does not try to generalise.
 *
 * Fixed here so the three cannot drift: `surfaceContainer` fill (sheets are one
 * of the five surfaces the rules let fill), [Radius.sheet] top corners only, title in
 * [AppText.titleL] over a hairline, and the content scrolls with the navigation
 * bar inset applied — a sheet that ends under the gesture bar eats its own last
 * row.
 *
 * The rule under the title is house rule 1 arriving here. The title is a
 * standing head like any other — it just reads at [AppText.titleL] rather than
 * as an eyebrow, because a sheet has exactly one of them and it is the thing
 * that says where you are. A heading separated from what it heads by nothing
 * but a gap was the last of that pattern left in the slice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .navigationBarsPadding(),
        ) {
            Text(title, style = AppText.titleL, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Spacing.m))
            AppDivider()
            Spacer(Modifier.height(Spacing.l))
            content()
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}
