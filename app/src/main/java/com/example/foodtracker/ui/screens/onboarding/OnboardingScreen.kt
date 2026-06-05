package com.example.foodtracker.ui.screens.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodtracker.data.ActivityLevel
import com.example.foodtracker.data.Goal
import com.example.foodtracker.data.Sex
import com.example.foodtracker.ui.appViewModel
import com.example.foodtracker.ui.rememberAppHaptics

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm: OnboardingViewModel = appViewModel { OnboardingViewModel(it.profileStore) }
    val state by vm.state.collectAsStateWithLifecycle()
    val haptics = rememberAppHaptics()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text("Let's personalise your goals", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tell us about you and we'll calculate your daily calorie and nutrient targets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.profile.name,
                onValueChange = vm::setName,
                label = { Text("Your name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Sex")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { sex ->
                    FilterChip(
                        selected = state.profile.sex == sex,
                        onClick = { vm.setSex(sex) },
                        label = { Text(sex.displayName) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Age: ${state.profile.ageYears} years")
            Slider(
                value = state.profile.ageYears.toFloat(),
                onValueChange = { vm.setAge(it.toInt()) },
                valueRange = 10f..90f,
                steps = 79,
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel("Height: ${state.profile.heightCm} cm")
            Slider(
                value = state.profile.heightCm.toFloat(),
                onValueChange = { vm.setHeight(it.toInt()) },
                valueRange = 120f..220f,
            )

            Spacer(Modifier.height(8.dp))
            SectionLabel("Weight: ${"%.1f".format(state.profile.weightKg)} kg")
            Slider(
                value = state.profile.weightKg.toFloat(),
                onValueChange = { vm.setWeight(it.toDouble()) },
                valueRange = 35f..200f,
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel("Activity level")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    SelectableRow(
                        label = level.displayName,
                        selected = state.profile.activityLevel == level,
                        onClick = { vm.setActivityLevel(level) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Goal")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Goal.entries.forEach { g ->
                    FilterChip(
                        selected = state.profile.goal == g,
                        onClick = { vm.setGoal(g) },
                        label = { Text(g.displayName) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your estimated daily targets", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TargetRow("Calories", "${state.previewGoals.calories.toInt()} kcal")
                    TargetRow("Protein", "${state.previewGoals.protein.toInt()} g")
                    TargetRow("Carbs", "${state.previewGoals.carbs.toInt()} g")
                    TargetRow("Fat", "${state.previewGoals.fat.toInt()} g")
                    TargetRow("Fiber", "${state.previewGoals.fiber.toInt()} g")
                    TargetRow("Water", "${state.previewGoals.waterMl} ml")
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    haptics.confirm()
                    vm.save()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text("Get started", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            if (selected) Box(
                modifier = Modifier
                    .size(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(10.dp),
                ) {}
            }
        }
    }
}

@Composable
private fun TargetRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
