package dev.roasti.ui.features.userprofile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import dev.roasti.R
import dev.roasti.ui.features.userprofile.model.UserProfileUiModel
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.ui.theme.Spacing
import dev.roasti.ui.uikit.AppIcons
import dev.roasti.ui.uikit.AsyncImagePreviewProvider
import dev.roasti.ui.uikit.ImageComponent
import dev.roasti.ui.uikit.ImageFormat
import dev.roasti.ui.uikit.ImageSize
import dev.roasti.ui.uikit.state.ContentScaffold
import dev.roasti.ui.util.userAvatarSharedElementModifier

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserProfileRoute(
    userId: String,
    username: String,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    avatarTag: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: UserProfileViewModel =
        koinViewModel(parameters = { parametersOf(userId, username) })
    val state by viewModel.state.collectAsStateWithLifecycle()

    ContentScaffold(
        state = state,
        onRetry = viewModel::onRetry,
    ) { profile ->
        UserProfileScreen(
            profile = profile,
            onBackClick = onBackClick,
            contentPadding = contentPadding,
            avatarModifier = userAvatarSharedElementModifier(
                tag = avatarTag,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileScreen(
    profile: UserProfileUiModel,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    avatarModifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(profile.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = AppIcons.Regular.ArrowLeft,
                            contentDescription = stringResource(R.string.back_label),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(bottom = contentPadding.calculateBottomPadding())
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ImageComponent(
                url = profile.avatarUrl,
                format = ImageFormat.Square,
                size = ImageSize.FixedWidth(96.dp),
                shape = CircleShape,
                modifier = avatarModifier,
            )
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.user_profile_username_format, profile.username),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profile.bio != null) {
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserProfileScreenPreview() {
    RoastiTheme {
        AsyncImagePreviewProvider {
            UserProfileScreen(
                profile = UserProfileUiModel(
                    id = "u1",
                    displayName = "Sarah J.",
                    username = "sarah_j",
                    avatarUrl = null,
                    bio = "Coffee enthusiast. V60 evangelist. Roasting since 2018.",
                ),
                onBackClick = {},
            )
        }
    }
}
