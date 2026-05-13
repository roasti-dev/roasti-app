package dev.roasti.ui.uikit.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.uikit.RoastiBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOwnerActionsSheet(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    RoastiBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.post_owner_menu_title),
    ) {
        ActionRow(
            label = stringResource(R.string.post_owner_menu_edit),
            onClick = onEdit,
        )
        ActionRow(
            label = stringResource(R.string.post_owner_menu_delete),
            onClick = onDelete,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
    color: Color = Color.Unspecified,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
