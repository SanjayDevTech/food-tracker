package com.example.foodtracker.ui.screens.you.sheets

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
import com.example.foodtracker.ui.components.atoms.AppDivider
import com.example.foodtracker.ui.theme.AppText
import com.example.foodtracker.ui.theme.Radius
import com.example.foodtracker.ui.theme.Spacing

/**
 * The chrome every You sheet shares.
 *
 * Three of the six App rows open a sheet rather than a page, because each is a
 * drill-down from one row of one section: a read-only list, a toggle with two
 * pickers, and a count with one destructive action. None of them earns a back
 * stack entry, and a page would have to invent a title bar and a back arrow to
 * get you out of somewhere you did not really go.
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
fun YouSheet(
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
