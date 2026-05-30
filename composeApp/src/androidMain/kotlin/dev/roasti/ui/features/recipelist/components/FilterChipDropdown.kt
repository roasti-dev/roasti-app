package dev.roasti.ui.features.recipelist.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Bold
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.bold.ArrowDown
import com.adamglin.phosphoricons.fill.ArrowDown
import com.adamglin.phosphoricons.regular.ArrowDown
import dev.roasti.R

@Composable
internal fun FilterChipDropdown(
    modifier: Modifier = Modifier,
    selectedValue: String? = null,
    placeholder: String,
    values: List<String>,
    onSelected: (String?) -> Unit,
) {
    var dropdownOpened by remember { mutableStateOf(false) }
    val labelText = selectedValue ?: placeholder
    val animatedRotationDegree by animateFloatAsState(if (dropdownOpened) 180f else 0f)

    Box(modifier) {
        FilterChip(
            selected = selectedValue != null,
            onClick = { dropdownOpened = true },
            label = { Text(text = labelText, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = {
                Icon(
                    imageVector = PhosphorIcons.Bold.ArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).rotate(animatedRotationDegree)
                )
            },
            shape = CircleShape,
            border = null,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                selectedTrailingIconColor = MaterialTheme.colorScheme.tertiary,
            ),
        )

        DropdownMenu(
            expanded = dropdownOpened,
            onDismissRequest = { dropdownOpened = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            values.forEach { level ->
                DropdownMenuItem(
                    text = { Text(text = level, style = MaterialTheme.typography.labelMedium) },
                    onClick = {
                        onSelected(level)
                        dropdownOpened = false
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.filter_clear),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                onClick = {
                    onSelected(null)
                    dropdownOpened = false
                },
            )
        }
    }
}