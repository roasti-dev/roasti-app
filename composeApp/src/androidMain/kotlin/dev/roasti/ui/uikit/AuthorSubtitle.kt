package dev.roasti.ui.uikit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AuthorSubtitle(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    avatarModifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick != null) { onClick?.invoke() }
            .padding(vertical = 2.dp, horizontal = 2.dp)
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageComponent(
            url = imageUrl,
            format = ImageFormat.Square,
            size = ImageSize.FixedWidth(16.dp),
            shape = CircleShape,
            modifier = avatarModifier,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
