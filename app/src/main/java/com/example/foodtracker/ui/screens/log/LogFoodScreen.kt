package com.example.foodtracker.ui.screens.log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.data.OnlineSearchResult
import com.example.foodtracker.data.OnlineSource
import com.example.foodtracker.data.ServingVariant
import com.example.foodtracker.data.db.FoodEntity
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.components.FoodRow
import com.example.foodtracker.ui.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodScreen(
    initialMeal: MealType,
    onBack: () -> Unit,
    onScanBarcode: () -> Unit,
    onAddCustomFood: () -> Unit,
) {
    val vm: LogFoodViewModel = appViewModel { LogFoodViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()

    LaunchedEffect(initialMeal) { vm.setMealType(initialMeal) }

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
                    IconButton(onClick = onAddCustomFood) {
                        Icon(Icons.Default.Edit, contentDescription = "Add custom food")
                    }
                },
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search dishes (idli, dosa, dal…)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MealType.entries.forEach { meal ->
                    FilterChip(
                        selected = state.mealType == meal,
                        onClick = {
                            haptics.light()
                            vm.setMealType(meal)
                        },
                        label = { Text(meal.displayName, maxLines = 1) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LogTab.entries.forEach { tab ->
                    FilterChip(
                        selected = state.tab == tab,
                        onClick = {
                            haptics.light()
                            vm.setTab(tab)
                        },
                        label = { Text(tab.label(), maxLines = 1) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val listForTab: List<FoodEntity> = when (state.tab) {
                LogTab.Search -> state.results
                LogTab.Favourites -> state.favourites
                LogTab.Recent -> state.recent
                LogTab.Frequent -> state.frequent
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(listForTab, key = { it.id }) { food ->
                    FoodRow(
                        food = food,
                        onClick = {
                            haptics.light()
                            vm.select(food)
                        },
                        trailing = {
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
                        },
                    )
                }
                if (listForTab.isEmpty()) {
                    item { EmptyTabHint(tab = state.tab, onAddCustomFood = onAddCustomFood) }
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
}

private fun LogTab.label(): String = when (this) {
    LogTab.Search -> "Search"
    LogTab.Favourites -> "Favourites"
    LogTab.Recent -> "Recent"
    LogTab.Frequent -> "Frequent"
}

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
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
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
    // Minimal: monochrome labelSmall text only — no fill, no outline.
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
private fun EmptyTabHint(tab: LogTab, onAddCustomFood: () -> Unit) {
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
private fun ServingSheet(
    food: FoodEntity,
    meal: MealType,
    variant: ServingVariant,
    servings: Double,
    onVariantChange: (ServingVariant) -> Unit,
    onServingsChange: (Double) -> Unit,
    onMealChange: (MealType) -> Unit,
    onConfirm: () -> Unit,
) {
    val variants = food.variantsOrDefault()
    val n = food.nutrientsPer100g() * (variant.grams * servings / 100.0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(food.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (!food.brand.isNullOrBlank()) {
            Text(
                food.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("Serving size", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        // Variants live in a horizontally-scrollable row so longer labels like
        // "1 masala dosa (150 g)" never get clipped.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            variants.forEach { v ->
                FilterChip(
                    selected = v.label == variant.label && v.grams == variant.grams,
                    onClick = { onVariantChange(v) },
                    label = { Text("${v.label} · ${v.grams.toInt()} g", maxLines = 1) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Servings: ${"%.2f".format(servings)} · ${(variant.grams * servings).toInt()} g",
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = servings.toFloat().coerceIn(0.25f, 5f),
            onValueChange = { onServingsChange(it.toDouble()) },
            valueRange = 0.25f..5f,
            steps = 18,
        )

        Spacer(Modifier.height(8.dp))
        Text("Meal", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MealType.entries.forEach { m ->
                FilterChip(
                    selected = meal == m,
                    onClick = { onMealChange(m) },
                    label = { Text(m.displayName, maxLines = 1) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        NutrientPreview(n)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Add ${"%.1f".format(servings)} × ${variant.label}")
        }
    }
}

@Composable
private fun NutrientPreview(n: Nutrients) {
    val items = listOf(
        "Calories" to "${n.calories.toInt()} kcal",
        "Carbs" to "${"%.1f".format(n.carbs)} g",
        "Protein" to "${"%.1f".format(n.protein)} g",
        "Fat" to "${"%.1f".format(n.fat)} g",
        "Fiber" to "${"%.1f".format(n.fiber)} g",
        "Sodium" to "${n.sodium.toInt()} mg",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { (k, v) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
