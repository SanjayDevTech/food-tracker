package com.example.foodtracker.ui.screens.barcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.BarcodeProduct
import com.example.foodtracker.data.BarcodeSource
import com.example.foodtracker.data.MealType
import com.example.foodtracker.data.NutritionBasis
import com.example.foodtracker.data.Nutrients
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.rememberAppHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScreen(
    onBack: () -> Unit,
    onAddManually: (barcode: String) -> Unit,
) {
    val vm: BarcodeViewModel = appViewModel { BarcodeViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()
    val context = LocalContext.current

    // A single quiet tick the moment a scan resolves — confirms the lookup landed
    // without the user having to read the screen.
    val resolved = state.product != null || state.cached != null
    LaunchedEffect(resolved) {
        if (resolved) haptics.light()
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    var selectedMeal by rememberSaveable { mutableStateOf(MealType.Snack) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.retry() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan again")
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !hasPermission -> PermissionPrompt(onGrant = { permLauncher.launch(Manifest.permission.CAMERA) })
                state.scanning && !resolved && !state.isLoading && !state.notFound && state.error == null ->
                    BarcodeScannerView(onBarcode = vm::onBarcode, modifier = Modifier.fillMaxSize())

                state.isLoading -> CenterLoading("Looking up ${state.lastBarcode}…")

                state.product != null -> ResolvedPanel(
                    product = state.product!!,
                    basis = state.basis,
                    onBasisChange = { vm.setBasis(it); haptics.light() },
                    source = state.resolvedSource,
                    selectedMeal = selectedMeal,
                    onMealChange = { selectedMeal = it },
                    onAdd = {
                        haptics.confirm()
                        vm.quickLog(selectedMeal, onBack)
                    },
                    onRetry = vm::retry,
                )

                state.cached != null -> CachedPanel(
                    food = state.cached!!,
                    source = state.resolvedSource,
                    selectedMeal = selectedMeal,
                    onMealChange = { selectedMeal = it },
                    onAdd = {
                        haptics.confirm()
                        vm.quickLog(selectedMeal, onBack)
                    },
                    onRetry = vm::retry,
                )

                state.notFound -> NotFoundPanel(
                    barcode = state.lastBarcode,
                    onRetry = vm::retry,
                    onAddManually = { state.lastBarcode?.let(onAddManually) },
                )

                state.error != null -> NotFoundPanel(
                    barcode = state.lastBarcode,
                    onRetry = vm::retry,
                    onAddManually = { state.lastBarcode?.let(onAddManually) },
                    message = state.error,
                )
            }

            if (state.scanning && hasPermission && !resolved && !state.isLoading) {
                ScannerOverlay()
            }
        }
    }
}

@Composable
private fun ScannerOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .height(220.dp)
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33000000)),
            )
            Spacer(Modifier.height(24.dp))
            Surface(
                color = Color(0xAA000000),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    "Align the barcode within the frame",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera permission needed", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow camera access to scan product barcodes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGrant) { Text("Grant permission") }
    }
}

@Composable
private fun CenterLoading(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResolvedPanel(
    product: BarcodeProduct,
    basis: NutritionBasis,
    onBasisChange: (NutritionBasis) -> Unit,
    source: BarcodeSource?,
    selectedMeal: MealType,
    onMealChange: (MealType) -> Unit,
    onAdd: () -> Unit,
    onRetry: () -> Unit,
) {
    val per100g = product.perHundredG
    val perServing = product.perServing
    val nutrients = product.nutrientsAs(basis)
    val basisLabel = when (basis) {
        NutritionBasis.Per100g -> "per 100 g"
        NutritionBasis.PerServing -> "per serving (${product.servingGrams.toInt()} g)"
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Found it!", style = MaterialTheme.typography.titleLarge)
        Text(product.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (!product.brand.isNullOrBlank()) {
            Text(
                product.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${nutrients.calories.toInt()} kcal $basisLabel",
            style = MaterialTheme.typography.bodyLarge,
        )
        source?.let {
            Text(
                "Source: ${it.display} · serving: ${product.servingLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Basis picker — only meaningful if the source exposes both, but we always
        // show it so the user can switch in case the auto-detected basis is wrong.
        Spacer(Modifier.height(4.dp))
        Text("Values shown are:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            BasisChip(
                selected = basis == NutritionBasis.Per100g,
                enabled = per100g != null || perServing != null,
                label = "Per 100 g",
                onClick = { onBasisChange(NutritionBasis.Per100g) },
            )
            BasisChip(
                selected = basis == NutritionBasis.PerServing,
                enabled = perServing != null || per100g != null,
                label = "Per ${product.servingGrams.toInt()} g serving",
                onClick = { onBasisChange(NutritionBasis.PerServing) },
            )
        }
        if (per100g == null && perServing != null) {
            Text(
                "Only per-serving values were published — we'll scale to 100 g for storage.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (per100g != null && perServing != null) {
            val ratio = if (per100g.calories > 0 && perServing.calories > 0 && product.servingGrams > 0)
                (perServing.calories * 100.0 / product.servingGrams) / per100g.calories
            else 1.0
            if (ratio !in 0.90..1.10) {
                Text(
                    "Note: this product's per-100 g and per-serving values disagree. The label on the pack is usually correct — pick per-serving if the values above match the pack.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        MacroPreview(nutrients)

        Spacer(Modifier.height(8.dp))
        Text("Add to:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            MealType.entries.forEach { meal ->
                FilterChip(
                    selected = selectedMeal == meal,
                    onClick = { onMealChange(meal) },
                    label = { Text(meal.displayName) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Add to ${selectedMeal.displayName}")
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Scan another")
        }
    }
}

@Composable
private fun BasisChip(
    selected: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
    )
}

@Composable
private fun MacroPreview(n: Nutrients) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MacroPill("Carbs", n.carbs)
        MacroPill("Protein", n.protein)
        MacroPill("Fat", n.fat)
    }
}

@Composable
private fun MacroPill(label: String, grams: Double) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${"%.1f".format(grams)} g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Fallback view for offline cached hits — already per-100g, no basis picker needed. */
@Composable
private fun CachedPanel(
    food: com.example.foodtracker.data.db.FoodEntity,
    source: BarcodeSource?,
    selectedMeal: MealType,
    onMealChange: (MealType) -> Unit,
    onAdd: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Found it!", style = MaterialTheme.typography.titleLarge)
        Text(food.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (!food.brand.isNullOrBlank()) {
            Text(
                food.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "${food.calories.toInt()} kcal per 100 g · ${food.servingLabel}",
            style = MaterialTheme.typography.bodyLarge,
        )
        source?.let {
            Text(
                "Source: ${it.display} — offline, values may be stale",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Add to:", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            MealType.entries.forEach { meal ->
                FilterChip(
                    selected = selectedMeal == meal,
                    onClick = { onMealChange(meal) },
                    label = { Text(meal.displayName) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Add to ${selectedMeal.displayName}")
        }
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Scan another")
        }
    }
}

@Composable
private fun NotFoundPanel(
    barcode: String?,
    onRetry: () -> Unit,
    onAddManually: () -> Unit,
    message: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Product not found", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            message ?: "OpenFoodFacts and USDA both came back empty for $barcode.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAddManually, enabled = barcode != null) {
            Text("Add manually with this barcode")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) { Text("Try another scan") }
    }
}
