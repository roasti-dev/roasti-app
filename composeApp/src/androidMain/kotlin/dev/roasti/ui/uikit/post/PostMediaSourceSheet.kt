package dev.roasti.ui.uikit.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.uikit.RoastiBottomSheet

enum class PostMediaSource { CAMERA, GALLERY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostMediaSourceSheet(
    onPick: (PostMediaSource) -> Unit,
    onDismiss: () -> Unit,
) {
    RoastiBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.post_media_source_title),
    ) {
        SheetOptionRow(
            label = stringResource(R.string.post_media_source_camera),
            onClick = { onPick(PostMediaSource.CAMERA) },
        )
        SheetOptionRow(
            label = stringResource(R.string.post_media_source_gallery),
            onClick = { onPick(PostMediaSource.GALLERY) },
        )
    }
}

@Composable
private fun SheetOptionRow(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
