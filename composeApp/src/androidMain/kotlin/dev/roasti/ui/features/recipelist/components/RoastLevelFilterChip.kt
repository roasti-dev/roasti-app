package dev.roasti.ui.features.recipelist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.ui.features.recipe.mapper.labelRes

@Composable
internal fun RoastLevelFilterChip(
    selectedRoastLevel: RoastLevel?,
    onRoastLevelSelected: (RoastLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLabel = selectedRoastLevel?.labelRes()?.let { stringResource(it) }
    val values = RoastLevel.entries.filter { it != RoastLevel.NONE }.associateBy { stringResource(it.labelRes()) }

    FilterChipDropdown(
        modifier = modifier,
        selectedValue = selectedLabel,
        placeholder = stringResource(R.string.recipe_roast_level),
        values = values.map { it.key },
        onSelected = { stringValue ->
            if (stringValue == null) onRoastLevelSelected(null)
            else onRoastLevelSelected(values[stringValue])
        }
    )
}
