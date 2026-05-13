package dev.roasti.ui.features.createrecipe.steps

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.createrecipe.model.CreateRecipeUiState
import dev.roasti.ui.features.recipe.mapper.labelRes
import dev.roasti.ui.theme.Spacing
import dev.roasti.core.utils.imageUrl
import dev.roasti.utils.compressImage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BasicsStep(
    state: CreateRecipeUiState,
    onNameChange: (String) -> Unit,
    onBrewMethodChange: (BrewMethod?) -> Unit,
    onBeansChange: (String) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onRoastLevelChange: (RoastLevel) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onUploadImage: (String, ByteArray) -> Unit,
    onContinue: () -> Unit,
    onDiscardRequest: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler {
        if (state.isDirty) onDiscardRequest() else onDismiss()
    }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    val context = LocalContext.current
    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val bytes = compressImage(context.contentResolver, it)
                onUploadImage("${UUID.randomUUID()}.jpg", bytes)
            }
        }

    var brewMethodExpanded by remember { mutableStateOf(false) }
    var showNameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Cover Photo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !state.isUploadingImage) { imageLauncher.launch("image/*") },
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isUploadingImage -> CircularProgressIndicator()
                state.imageId != null -> AsyncImage(
                    model = imageUrl(state.imageId),
                    contentDescription = "Cover photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text("📷", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "Add Cover Photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // Recipe Name
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Row {
                    Text(
                        "Recipe Name",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.focusable()
                    )
                    Text(
                        " *",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedTextField(
                    value = state.name,
                    onValueChange = {
                        onNameChange(it)
                        if (showNameError) showNameError = false
                    },
                    placeholder = { Text("e.g., Perfect Morning Pour Over") },
                    isError = showNameError && state.name.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Brew Method
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row {
                    Text("Brew Method", style = MaterialTheme.typography.labelLarge)
                    Text(
                        " *",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = brewMethodExpanded,
                    onExpandedChange = { brewMethodExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.brewMethod?.takeUnless { it == BrewMethod.NONE }?.let { stringResource(it.labelRes()) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Select brewing method...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brewMethodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = brewMethodExpanded,
                        onDismissRequest = { brewMethodExpanded = false },
                    ) {
                        BrewMethod.entries.filterNot { it == BrewMethod.NONE }.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(stringResource(method.labelRes())) },
                                onClick = {
                                    onBrewMethodChange(method)
                                    brewMethodExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Roaster / Beans
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("Roaster (Optional)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = state.beans,
                    onValueChange = onBeansChange,
                    placeholder = { Text("e.g., Blue Bottle, Local Roaster") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Roast level
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("Roast level", style = MaterialTheme.typography.labelLarge)
                RoastLevelPickerRow(
                    state.roastLevel,
                    { onRoastLevelChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )

            }
            // Difficulty
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                DifficultySelector(
                    selected = state.difficulty,
                    onSelect = { onDifficultyChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Description
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row {
                    Text("Description", style = MaterialTheme.typography.labelLarge)
                    Text(
                        " *",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    placeholder = { Text("Describe the flavor profile, what makes this recipe special...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
            }
        }

        Button(
            onClick = {
                if (state.canContinueToSteps) {
                    onContinue()
                } else {
                    showNameError = true
                }
            },
            enabled = state.canContinueToSteps,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Text("Continue to Steps")
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}


@Composable
private fun DifficultySelector(
    selected: Difficulty?,
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = Difficulty.entries.toTypedArray()
    val selectedIndex = items.indexOfFirst { it == selected }

    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { index, difficulty ->
            SelectorItem(stringResource(difficulty.labelRes()), index == selectedIndex, { onSelect(difficulty) })
        }
    }
}

@Composable
private fun RoastLevelPickerRow(
    selected: RoastLevel?,
    onSelect: (RoastLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = RoastLevel.entries.filterNot { it == RoastLevel.NONE }.toTypedArray()
    val selectedIndex = items.indexOfFirst { it == selected }

    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEachIndexed { index, difficulty ->
            SelectorItem(stringResource(difficulty.labelRes()), index == selectedIndex, { onSelect(difficulty) })
        }
    }
}

@Composable
private fun SelectorItem(
    text: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable { onSelect() }
            .padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}


@Preview(widthDp = 300)
@Composable
private fun DifficultySelectorPreview() {
    DifficultySelector(Difficulty.Hard, {})
}
