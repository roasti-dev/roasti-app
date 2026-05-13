package dev.roasti.ui.uikit.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.theme.Spacing

interface PostOptionsRowClickListener {
    fun onChangeRating(newRatingIntent: PostUserReaction)
    fun onCommentsClick()
    fun onShareClick()
}

private val EmptyListener = object : PostOptionsRowClickListener {
    override fun onChangeRating(newRatingIntent: PostUserReaction) = Unit
    override fun onCommentsClick() = Unit
    override fun onShareClick() = Unit
}

@Composable
fun PostOptionsRow(
    ratingValue: PostRatingStateUi,
    commentsCount: Int,
    modifier: Modifier = Modifier,
    listener: PostOptionsRowClickListener = EmptyListener,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PostRatingBar(ratingValue, onClick = { listener.onChangeRating(it) })
            PostCommentsButton(commentsCount, onClick = { listener.onCommentsClick() })
        }
        PostShareButton { listener.onShareClick() }
    }
}

@Composable
private fun PostCommentsButton(
    count: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chat),
            contentDescription = "open post comments button",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatValue(count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PostShareButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Icon(
        painter = painterResource(R.drawable.ic_share),
        contentDescription = "open post comments button",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() }
            .padding(6.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun PostCommentsButtonPreview() {
    MaterialTheme {
        PostCommentsButton(12)
    }
}

@Preview(showBackground = true)
@Composable
private fun PostShareButtonPreview() {
    MaterialTheme {
        PostShareButton()
    }
}

@Preview(showBackground = true)
@Composable
private fun PostOptionsRowPreview() {
    MaterialTheme {
        PostOptionsRow(
            ratingValue = PostRatingStateUi.empty(),
            commentsCount = 12,
        )
    }
}

