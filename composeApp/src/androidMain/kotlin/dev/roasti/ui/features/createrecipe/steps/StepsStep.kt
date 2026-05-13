package dev.roasti.ui.features.createrecipe.steps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.roasti.ui.features.createrecipe.model.CreateRecipeStepUiModel
import dev.roasti.ui.features.createrecipe.model.CreateRecipeUiState
import dev.roasti.ui.theme.Spacing
import dev.roasti.core.utils.imageUrl
import dev.roasti.utils.compressImage
import java.util.UUID

@Composable
internal fun StepsStep(
    state: CreateRecipeUiState,
    onAddStep: (CreateRecipeStepUiModel) -> Unit,
    onRemoveStep: (Int) -> Unit,
    onUploadStepImage: (String, ByteArray) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var isAdding by remember { mutableStateOf(false) }
    var stepTitle by remember { mutableStateOf("") }
    var stepDescription by remember { mutableStateOf("") }
    var stepMinutes by remember { mutableStateOf("") }
    var stepSeconds by remember { mutableStateOf("") }
    var showTitleError by remember { mutableStateOf(false) }

    fun submitStep() {
        if (stepTitle.isBlank()) {
            showTitleError = true
            return
        }
        val totalSeconds = (stepMinutes.toIntOrNull() ?: 0) * 60 + (stepSeconds.toIntOrNull() ?: 0)
        onAddStep(CreateRecipeStepUiModel(stepTitle.trim(), stepDescription.trim(), totalSeconds, imageId = state.pendingStepImageId))
        stepTitle = ""
        stepDescription = ""
        stepMinutes = ""
        stepSeconds = ""
        showTitleError = false
        isAdding = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(vertical = Spacing.md),
        ) {
            if (state.brewSteps.isEmpty() && !isAdding) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xxxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            Text(
                                "No steps yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Add brewing steps to guide others through your recipe",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            itemsIndexed(state.brewSteps) { index, step ->
                BrewStepCard(
                    index = index,
                    step = step,
                    onRemove = { onRemoveStep(index) },
                )
            }

            if (isAdding) {
                item {
                    AddStepForm(
                        title = stepTitle,
                        description = stepDescription,
                        minutes = stepMinutes,
                        seconds = stepSeconds,
                        showTitleError = showTitleError,
                        stepImageId = state.pendingStepImageId,
                        isUploadingStepImage = state.isUploadingStepImage,
                        onTitleChange = { stepTitle = it; if (showTitleError) showTitleError = false },
                        onDescriptionChange = { stepDescription = it },
                        onMinutesChange = { if (it.length <= 2 && it.all(Char::isDigit)) stepMinutes = it },
                        onSecondsChange = { if (it.length <= 2 && it.all(Char::isDigit)) stepSeconds = it },
                        onUploadImage = onUploadStepImage,
                        onSubmit = ::submitStep,
                        onCancel = { isAdding = false; showTitleError = false },
                    )
                }
            }
        }

        if (!isAdding) {
            OutlinedButton(
                onClick = { isAdding = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm),
            ) {
                Text("+", Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Add Step")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) {
                Text("Continue to Preview")
            }
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun BrewStepCard(
    index: Int,
    step: CreateRecipeStepUiModel,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(step.title, style = MaterialTheme.typography.bodyMedium)
                if (step.description.isNotBlank()) {
                    Text(
                        step.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (step.durationInSeconds > 0) {
                    val mins = step.durationInSeconds / 60
                    val secs = step.durationInSeconds % 60
                    val label = when {
                        mins > 0 && secs > 0 -> "${mins}m ${secs}s"
                        mins > 0 -> "${mins}m"
                        else -> "${secs}s"
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Text(
                    text = "--",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )

            }
        }
    }
}

@Composable
private fun AddStepForm(
    title: String,
    description: String,
    minutes: String,
    seconds: String,
    showTitleError: Boolean,
    stepImageId: String?,
    isUploadingStepImage: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onUploadImage: (String, ByteArray) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bytes = compressImage(context.contentResolver, it)
            onUploadImage("${UUID.randomUUID()}.jpg", bytes)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text("New Step", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !isUploadingStepImage) { imageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isUploadingStepImage -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        stepImageId != null -> AsyncImage(
                            model = imageUrl(stepImageId),
                            contentDescription = "Step image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Text("📷", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Step Title *") },
                placeholder = { Text("e.g., Bloom the coffee") },
                isError = showTitleError && title.isBlank(),
                supportingText = if (showTitleError && title.isBlank()) {
                    { Text("Title is required") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (optional)") },
                placeholder = { Text("Describe what to do in this step...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "Duration (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = onMinutesChange,
                        label = { Text("Min") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = onSecondsChange,
                        label = { Text("Sec") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(onClick = onSubmit, modifier = Modifier.weight(1f)) {
                    Text("Add")
                }
            }
        }
    }
}
