package dev.roasti.ui.features.profile.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.features.profile.ProfileStatisticsUiModel
import dev.roasti.ui.uikit.LoadingStub

@Composable
fun StatisticsRow(item: ProfileStatisticsUiModel, modifier: Modifier = Modifier) {
    AnimatedContent(item.isLoading, modifier) { isLoading ->
        if (isLoading) {
            LoadingStub(Modifier.fillMaxWidth())
        } else {
            StatisticsBlock(item)
        }
    }
}

@Composable
private fun StatisticsBlock(item: ProfileStatisticsUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatisticsItem(item.brewsCount, stringResource(R.string.profile_statistics_brew_count))
        VerticalDivider(
            Modifier
                .padding(vertical = 4.dp)
                .fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.tertiary,
        )
        StatisticsItem(item.brewsCount, stringResource(R.string.profile_statistics_likes))
        VerticalDivider(
            Modifier
                .padding(vertical = 4.dp)
                .fillMaxHeight(),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.tertiary,
        )
        StatisticsItem(item.brewsCount, stringResource(R.string.profile_statistics_posts))
    }
}

@Composable
private fun StatisticsItem(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun StatisticsRowPreview() {
    MaterialTheme {
        StatisticsRow(item = ProfileStatisticsUiModel())
    }
}