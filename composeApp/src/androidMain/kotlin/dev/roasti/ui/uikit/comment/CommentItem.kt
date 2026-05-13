package dev.roasti.ui.uikit.comment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import dev.roasti.R
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.AuthorRowWithTime

@Composable
fun CommentItem(
    isDeleted: Boolean,
    authorName: String?,
    authorAvatarUrl: String?,
    postedAt: Instant?,
    body: String,
    modifier: Modifier = Modifier,
    isOwn: Boolean = false,
    showReply: Boolean = false,
    onMoreClick: (() -> Unit)? = null,
    onReplyClick: (() -> Unit)? = null,
) {
    val displayName = if (isDeleted || authorName == null) {
        stringResource(R.string.comments_deleted_placeholder)
    } else {
        authorName
    }
    val displayBody = if (isDeleted) stringResource(R.string.comments_deleted_body) else body
    val bodyColor = if (isDeleted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier.padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuthorRowWithTime(
                imageUrl = if (isDeleted) null else authorAvatarUrl,
                name = displayName,
                postedAt = postedAt,
                modifier = Modifier.weight(1f),
            )
            if (isOwn && !isDeleted && onMoreClick != null) {
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_three_dots),
                        contentDescription = stringResource(R.string.comments_action_more),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Text(
            text = displayBody,
            style = MaterialTheme.typography.bodyMedium,
            color = bodyColor,
            fontStyle = if (isDeleted) FontStyle.Italic else FontStyle.Normal,
        )
        if (showReply && !isDeleted && onReplyClick != null) {
            Text(
                text = stringResource(R.string.comments_action_reply),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onReplyClick)
                    .padding(vertical = Spacing.xs),
            )
        }
    }
}
