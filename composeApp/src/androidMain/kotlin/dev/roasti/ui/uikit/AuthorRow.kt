package dev.roasti.ui.uikit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import dev.roasti.core.datetime.formatRelative
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
fun AuthorRow(imageUrl: String?, name: String, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageComponent(
            url = imageUrl,
            format = ImageFormat.Square,
            size = ImageSize.FixedWidth(24.dp),
            shape = CircleShape
        )

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(max = 100.dp),
        )
    }
}

@Composable
fun AuthorRowWithTime(
    imageUrl: String?,
    name: String,
    postedAt: Instant?,
    modifier: Modifier = Modifier,
) {
    val timeColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageComponent(
            url = imageUrl,
            format = ImageFormat.Square,
            size = ImageSize.FixedWidth(24.dp),
            shape = CircleShape,
        )
        Text(
            text = buildAnnotatedString {
                append(name)
                if (postedAt != null) {
                    append(" · ")
                    withStyle(SpanStyle(color = timeColor)) {
                        append(postedAt.formatRelative())
                    }
                }
            },
            style = MaterialTheme.typography.labelSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthorRowPreview() {
    MaterialTheme {
        AsyncImagePreviewProvider {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                AuthorRow(
                    imageUrl = "",
                    name = "nicky minaj",
                )

                AuthorRowWithTime(
                    imageUrl = "",
                    name = "nicky minaj",
                    postedAt = Clock.System.now() - 10.hours - 5.minutes,
                )
            }
        }
    }
}