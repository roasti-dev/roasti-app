package dev.roasti.ui.uikit

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.roasti.R

data class OverflowMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
)

@Composable
fun TopBarOverflowMenu(
    items: List<OverflowMenuItem>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_three_dots),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.label,
                            color = if (item.isDestructive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                )
            }
        }
    }
}
