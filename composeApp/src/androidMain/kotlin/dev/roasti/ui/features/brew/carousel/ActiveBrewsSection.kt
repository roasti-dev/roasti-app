package dev.roasti.ui.features.brew.carousel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.roasti.R
import dev.roasti.ui.theme.Spacing
import kotlinx.coroutines.delay

private val CardWidth = 168.dp
private val CardImageHeight = 96.dp
private const val TickIntervalMillis = 30_000L

@Composable
internal fun ActiveBrewsSection(
    brews: List<ActiveBrewCardUiModel>,
    onBrewClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (brews.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm, bottom = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.brew_active_section_title),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Spacing.lg),
        ) {
            items(items = brews, key = { it.brewId }) { brew ->
                ActiveBrewCard(brew = brew, onClick = { onBrewClick(brew.brewId) })
            }
        }
    }
}

@Composable
private fun ActiveBrewCard(
    brew: ActiveBrewCardUiModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(CardWidth)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CardImageHeight)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (brew.imageUrl != null) {
                    AsyncImage(
                        model = brew.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(CardImageHeight),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_coffee),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = brew.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ProgressLabel(progress = brew.progress)
            }
        }
    }
}

@Composable
private fun ProgressLabel(progress: ActiveBrewProgress) {
    val text = when (progress) {
        is ActiveBrewProgress.Brewing ->
            stringResource(R.string.brew_card_step, progress.currentStep, progress.totalSteps)

        is ActiveBrewProgress.Waiting -> {
            val now by produceState(initialValue = System.currentTimeMillis(), progress.waitUntil) {
                while (true) {
                    value = System.currentTimeMillis()
                    delay(TickIntervalMillis)
                }
            }
            val remainingSeconds = ((progress.waitUntil - now) / 1000L).coerceAtLeast(0L)
            if (remainingSeconds <= 0L) {
                stringResource(R.string.brew_waiting_ready)
            } else {
                stringResource(
                    R.string.brew_waiting_remaining,
                    formatRemainingShort(
                        remainingSeconds,
                        stringResource(R.string.brew_duration_days),
                        stringResource(R.string.brew_duration_hours),
                        stringResource(R.string.brew_duration_minutes),
                    ),
                )
            }
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
    )
}

private fun formatRemainingShort(
    totalSeconds: Long,
    days: String,
    hours: String,
    minutes: String,
): String {
    val d = totalSeconds / 86_400
    val h = (totalSeconds % 86_400) / 3_600
    val m = (totalSeconds % 3_600) / 60
    return when {
        d > 0 -> "$d$days $h$hours"
        h > 0 -> "$h$hours $m$minutes"
        else -> "${m + 1}$minutes"
    }
}
