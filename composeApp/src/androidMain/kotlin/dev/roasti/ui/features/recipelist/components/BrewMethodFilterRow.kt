package dev.roasti.ui.features.recipelist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.roasti.R
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.ui.features.recipe.mapper.labelRes

@Composable
internal fun BrewMethodFilterChip(
    selectedMethod: BrewMethod?,
    onMethodSelected: (BrewMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedLabel = selectedMethod?.labelRes()?.let { stringResource(it) }
    val values = BrewMethod.entries.associateBy { stringResource(it.labelRes()) }

    FilterChipDropdown(
        modifier = modifier,
        selectedValue = selectedLabel,
        placeholder = stringResource(R.string.recipe_brew_method),
        values = values.map { it.key },
        onSelected = { stringValue ->
            if (stringValue == null) onMethodSelected(BrewMethod.NONE)
            else onMethodSelected(values[stringValue] ?: BrewMethod.NONE)
        }
    )
}
