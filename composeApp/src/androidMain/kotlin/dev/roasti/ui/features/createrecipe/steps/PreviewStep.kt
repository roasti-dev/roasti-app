package dev.roasti.ui.features.createrecipe.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import dev.roasti.ui.features.createrecipe.model.CreateRecipeUiState
import dev.roasti.ui.features.recipe.mapper.labelRes
import dev.roasti.ui.theme.Spacing

@Composable
internal fun PreviewStep(
    state: CreateRecipeUiState,
    onBack: () -> Unit,
    onUpload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PreviewField("Recipe Name", state.name.ifBlank { "—" })
            HorizontalDivider()
            PreviewField("Brew Method", state.brewMethod?.let { stringResource(it.labelRes()) } ?: "—")
            if (state.beans.isNotBlank()) {
                HorizontalDivider()
                PreviewField("Roaster", state.beans)
            }
            HorizontalDivider()
            PreviewField("Difficulty", stringResource(state.difficulty.labelRes()))
            if (state.description.isNotBlank()) {
                HorizontalDivider()
                PreviewField("Description", state.description)
            }
            if (state.brewSteps.isNotEmpty()) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        "Steps (${state.brewSteps.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.brewSteps.forEachIndexed { index, step ->
                        Text(
                            "${index + 1}. ${step.title}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(onClick = onUpload, modifier = Modifier.weight(1f)) {
                Text("Publish Recipe")
            }
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PreviewField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
