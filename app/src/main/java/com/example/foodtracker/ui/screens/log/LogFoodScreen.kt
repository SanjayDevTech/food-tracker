package com.example.foodtracker.ui.screens.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.OnlineSearchResult
import com.example.foodtracker.data.OnlineSource
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.ExclusiveButtonGroup
import com.example.foodtracker.ui.components.FoodRow
import com.example.foodtracker.ui.components.HapticFilterChip
import com.example.foodtracker.ui.components.ServingSheet
import com.example.foodtracker.ui.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogFoodScreen(
    initialMeal: MealType,
    onBack: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddCustomFood: () -> Unit,
) {
    val vm: LogFoodViewModel = appViewModel { LogFoodViewModel(it.repository, initialMeal) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    var pendingDelete by remember { mutableStateOf<FoodEntity?>(null) }
    var pendingHidden by remember { mutableStateOf<FoodEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to ${state.mealType.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onScanBarcode) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan barcode")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search foods…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
            )

            // Meal selector — connected segmented look. Tells the user where
            // the food they pick will land.
            ExclusiveButtonGroup(
                options = MealType.entries,
                selected = state.mealType,
                onSelect = vm::setMealType,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { it.displayName },
            )
            Spacer(Modifier.height(4.dp))

            // Which list of foods to browse. Uses the text + underline idiom
            // (different from the segmented meal picker above so they don't
            // visually compete), and scrolls horizontally so no label ever
            // gets clipped on narrow screens.
            PrimaryScrollableTabRow(
                selectedTabIndex = LogTab.entries.indexOf(state.tab),
                edgePadding = 12.dp,
                containerColor = Color.Transparent,
                divider = {},
            ) {
                LogTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = {
                            haptics.light()
                            vm.setTab(tab)
                        },
                        text = {
                            Text(
                                tab.label(),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }

            // Visible/Hidden is a binary toggle for one rare workflow — a
            // single "Show hidden" chip reads more naturally than a 2-button
            // segmented control would, and only appears on the Custom tab.
            if (state.tab == LogTab.Custom) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HapticFilterChip(
                        selected = state.customFilter == CustomFilter.Hidden,
                        onClick = {
                            vm.setCustomFilter(
                                if (state.customFilter == CustomFilter.Hidden) CustomFilter.Visible
                                else CustomFilter.Hidden
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = { Text("Show hidden") },
                    )
                }
            }

            val isHiddenView = state.tab == LogTab.Custom && state.customFilter == CustomFilter.Hidden
            val listForTab: List<FoodEntity> = when (state.tab) {
                LogTab.Search -> state.results
                LogTab.Favourites -> state.favourites
                LogTab.Recent -> state.recent
                LogTab.Frequent -> state.frequent
                LogTab.Custom -> if (isHiddenView) state.customHidden else state.customVisible
            }
            val showCustomAddRow = state.tab == LogTab.Custom && !isHiddenView

            // The same LazyListState is reused across tabs, so without this
            // reset a deep scroll on (say) Search carries over to Recent and
            // the user lands mid-list. Key on tab + the Custom Visible/Hidden
            // subfilter + the active query so any list-content swap rewinds
            // to the first row.
            val listState = rememberLazyListState()
            LaunchedEffect(state.tab, state.customFilter, state.query) {
                listState.scrollToItem(0)
            }

            if (listForTab.isNotEmpty()) {
                Text(
                    text = "${listForTab.size} ${if (listForTab.size == 1) "food" else "foods"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            } else {
                Spacer(Modifier.height(4.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (showCustomAddRow) {
                    item {
                        AddCustomFoodRow(onClick = onAddCustomFood)
                    }
                }
                items(listForTab, key = { it.id }) { food ->
                    FoodRow(
                        food = food,
                        onClick = {
                            haptics.light()
                            if (isHiddenView) pendingHidden = food else vm.select(food)
                        },
                        onLongClick = food.takeIf { it.isCustom && !isHiddenView }?.let {
                            {
                                haptics.confirm()
                                pendingDelete = it
                            }
                        },
                        trailing = {
                            if (isHiddenView) {
                                Text(
                                    "Hidden",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            } else {
                                IconButton(onClick = {
                                    haptics.light()
                                    vm.toggleFavourite(food)
                                }) {
                                    if (food.isFavourite) Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "Unfavourite",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    else Icon(
                                        Icons.Outlined.StarOutline,
                                        contentDescription = "Favourite",
                                    )
                                }
                            }
                        },
                    )
                }
                if (listForTab.isEmpty()) {
                    item {
                        EmptyTabHint(
                            tab = state.tab,
                            isHiddenView = isHiddenView,
                            onAddCustomFood = onAddCustomFood,
                        )
                    }
                }
                if (state.tab == LogTab.Search && state.query.isNotBlank()) {
                    item {
                        OnlineSearchSection(
                            phase = state.onlinePhase,
                            results = state.onlineResults,
                            onSearch = {
                                haptics.light()
                                vm.searchOnline()
                            },
                            onPick = { vm.pickOnline(it) },
                        )
                    }
                }
            }
        }
    }

    if (state.selected != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { vm.select(null) },
            sheetState = sheetState,
        ) {
            ServingSheet(
                food = state.selected!!,
                meal = state.mealType,
                variant = state.selectedVariant,
                servings = state.selectedServings,
                onVariantChange = vm::setVariant,
                onServingsChange = vm::setServings,
                onMealChange = vm::setMealType,
                onConfirm = {
                    haptics.confirm()
                    vm.confirmLog(onDone = onBack)
                },
            )
        }
    }

    pendingDelete?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { pendingDelete = null },
            sheetState = sheetState,
        ) {
            DeleteCustomFoodSheet(
                food = target,
                onCascade = {
                    vm.deleteCustomFood(target, cascadeLogs = true)
                    pendingDelete = null
                },
                onKeepHistory = {
                    vm.deleteCustomFood(target, cascadeLogs = false)
                    pendingDelete = null
                },
                onCancel = { pendingDelete = null },
            )
        }
    }

    pendingHidden?.let { target ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { pendingHidden = null },
            sheetState = sheetState,
        ) {
            HiddenCustomFoodSheet(
                food = target,
                onRestore = {
                    vm.restoreCustomFood(target)
                    pendingHidden = null
                },
                onDeleteForever = {
                    vm.deleteCustomFood(target, cascadeLogs = true)
                    pendingHidden = null
                },
                onCancel = { pendingHidden = null },
            )
        }
    }
}

private fun LogTab.label(): String = when (this) {
    LogTab.Search -> "Search"
    LogTab.Favourites -> "Favourites"
    LogTab.Recent -> "Recent"
    LogTab.Frequent -> "Frequent"
    LogTab.Custom -> "Custom"
}

/**
 * Prominent outlined-card affordance pinned to the top of the Custom Visible
 * tab. Replaces the older "Edit" icon-button buried in the topbar — that
 * icon read as "edit profile" and was easy to miss. Keeping it in the list
 * means the user discovers the action the moment they land on Custom.
 */
@Composable
private fun AddCustomFoodRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Add a custom food",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "Enter your own nutrition values",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlineSearchSection(
    phase: OnlineSearchPhase,
    results: List<OnlineSearchResult>,
    onSearch: () -> Unit,
    onPick: (OnlineSearchResult) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            "Can't find it locally?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        when (phase) {
            OnlineSearchPhase.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadingIndicator()
                    Spacer(Modifier.size(12.dp))
                    Text("Searching online food databases…")
                }
            }
            OnlineSearchPhase.Empty -> {
                Text(
                    "No online matches. Try a simpler search term or add a custom food.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                SearchOnlineButton(onSearch)
            }
            OnlineSearchPhase.Error -> {
                Text(
                    "Couldn't reach the food databases. Check connection and retry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                SearchOnlineButton(onSearch)
            }
            OnlineSearchPhase.Idle -> {
                if (results.isEmpty()) {
                    SearchOnlineButton(onSearch)
                }
            }
        }
        if (results.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${results.size} online result${if (results.size == 1) "" else "s"} — tap to save and log:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            results.forEach { result ->
                FoodRow(
                    food = result.food,
                    onClick = { onPick(result) },
                    trailing = { MinimalSourceTag(source = result.source) },
                )
            }
        }
    }
}

@Composable
private fun MinimalSourceTag(source: OnlineSource) {
    Text(
        text = source.display,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    )
}

@Composable
private fun SearchOnlineButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.CloudDownload, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Search online")
    }
}

@Composable
private fun EmptyTabHint(tab: LogTab, isHiddenView: Boolean, onAddCustomFood: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val message = when (tab) {
                LogTab.Search -> "No matching foods found."
                LogTab.Favourites -> "No favourites yet. Tap the star on any food to save it."
                LogTab.Recent -> "Nothing logged yet — your recent foods will appear here."
                LogTab.Frequent -> "Once you've logged regularly, your most-used foods will show up here."
                LogTab.Custom -> if (isHiddenView) {
                    "No hidden custom foods. Soft-deleted entries will show up here so you can restore or wipe them."
                } else {
                    "No custom foods yet — use the row above to add one."
                }
            }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tab == LogTab.Search) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onAddCustomFood) { Text("Add a custom food") }
            }
        }
    }
}

@Composable
private fun DeleteCustomFoodSheet(
    food: FoodEntity,
    onCascade: () -> Unit,
    onKeepHistory: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            "Delete ${food.name}?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Custom foods you added can be removed from your catalog. Choose what to do with any past log entries that used this food.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        DeleteOptionRow(
            icon = Icons.Outlined.VisibilityOff,
            title = "Delete, keep history",
            subtitle = "Hide from search and favourites. Past diary entries still show the name and nutrients.",
            onClick = onKeepHistory,
        )
        Spacer(Modifier.height(8.dp))
        DeleteOptionRow(
            icon = Icons.Filled.DeleteForever,
            title = "Delete with history",
            subtitle = "Remove this food and every past log entry that used it. Daily totals will change.",
            destructive = true,
            onClick = onCascade,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Cancel") }
    }
}

@Composable
private fun HiddenCustomFoodSheet(
    food: FoodEntity,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            food.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "This custom food is hidden from the picker but past diary entries still reference it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        DeleteOptionRow(
            icon = Icons.Filled.Restore,
            title = "Restore",
            subtitle = "Bring it back into search and favourites.",
            onClick = onRestore,
        )
        Spacer(Modifier.height(8.dp))
        DeleteOptionRow(
            icon = Icons.Filled.DeleteForever,
            title = "Delete forever",
            subtitle = "Wipe this food and every past log entry that used it. Daily totals will change.",
            destructive = true,
            onClick = onDeleteForever,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Cancel") }
    }
}

@Composable
private fun DeleteOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val border = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    val iconTint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
