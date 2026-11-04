package com.example.foodtracker.ui.screens.addfood

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodtracker.data.AiChatMessage
import com.example.foodtracker.data.AiChatRole

/**
 * Starter prompts for the empty state. They double as documentation: the
 * three things worth asking for are a different portion, a different
 * preparation, and a correction to the serving itself.
 */
private val Suggestions = listOf(
    "Make it 2 servings",
    "This was deep fried",
    "Use a 150 g serving",
)

/**
 * Chat sheet for refining a result the AI just produced.
 *
 * Reachable only after an autofill ([AddFoodFormState.aiAutofilled]) because
 * there is nothing to discuss about a blank form. Every reply rewrites the
 * form behind the sheet, so the user can close it at any point and keep
 * whatever the last turn produced — there is no separate "apply" step, and
 * every field stays hand-editable as always.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRefineSheet(
    foodName: String,
    messages: List<AiChatMessage>,
    busy: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Keep the newest turn in view as the conversation grows.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Text(
                "Refine with AI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Tell it what to change about $foodName. The form updates as you go.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            if (messages.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { onSend(suggestion) },
                            enabled = !busy,
                            label = { Text(suggestion) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(messages) { message -> MessageBubble(message) }
                }
            }

            if (busy) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Working it out…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            error?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("What should change?") },
                    maxLines = 3,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        onSend(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank() && !busy,
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MessageBubble(message: AiChatMessage) {
    val fromUser = message.role == AiChatRole.User
    val background =
        if (fromUser) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant
    val foreground =
        if (fromUser) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        // widthIn rather than fillMaxWidth(fraction): a short bubble should hug
        // its text, a long one should wrap instead of running edge to edge.
        Text(
            message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = foreground,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
