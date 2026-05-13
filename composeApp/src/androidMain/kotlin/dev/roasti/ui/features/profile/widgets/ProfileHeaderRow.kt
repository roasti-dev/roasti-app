package dev.roasti.ui.features.profile.widgets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.core.utils.imageUrl
import dev.roasti.ui.features.profile.ProfileRowListener
import dev.roasti.ui.features.profile.ProfileUserUiModel
import dev.roasti.ui.uikit.AsyncImagePreviewProvider
import dev.roasti.ui.uikit.ImageComponent
import dev.roasti.ui.uikit.ImageFormat
import dev.roasti.ui.uikit.ImageSize
import dev.roasti.utils.compressImage
import java.util.UUID

@Composable
fun ProfileHeaderRow(
    userUiModel: ProfileUserUiModel,
    listener: ProfileRowListener,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val bytes = compressImage(context.contentResolver, uri)
                listener.onImagePicked("${UUID.randomUUID()}.jpg", bytes)
            }
        }
    )

    Column(modifier) {
        Row(Modifier.padding(bottom = 16.dp)) {
            ProfileImage(
                url = userUiModel.imageId?.let { imageUrl(it) },
                modifier = Modifier
                    .clickable(!userUiModel.isImageUploadInProgress) {
                        singlePhotoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                isImageUploadInProgress = userUiModel.isImageUploadInProgress,
            )
            Spacer(Modifier.weight(1f))

            ActionIconButton(
                painter = painterResource(R.drawable.ic_edit),
                onClick = listener::onEditClick,
                modifier = Modifier.align(Alignment.Top),
                contentDescription = "edit_button"
            )
            ActionIconButton(
                painter = painterResource(R.drawable.ic_settings),
                onClick = listener::onSettingsClick,
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(start = 4.dp),
                contentDescription = "edit_button"
            )
            ActionIconButton(
                painter = painterResource(R.drawable.ic_logout),
                onClick = listener::onLogoutClick,
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(start = 30.dp),
                contentDescription = "logout_button"
            )
        }

        Text("@${userUiModel.nickname}", style = MaterialTheme.typography.headlineLarge)
        Text(
            userUiModel.email.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            userUiModel.bio.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ActionIconButton(
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = modifier,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .clickable { onClick() }
                .padding(8.dp)
                .size(24.dp),
        )
    }
}


@Composable
private fun ProfileImage(
    url: String?,
    modifier: Modifier = Modifier,
    isImageUploadInProgress: Boolean = false,
) {

    AnimatedContent(isImageUploadInProgress) { isLoading: Boolean ->
        Box {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(20.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                ImageComponent(
                    url = url,
                    format = ImageFormat.Square,
                    size = ImageSize.FixedWidth(120.dp),
                    modifier = modifier.clip(CircleShape),
                    contentDescription = "profile picture"
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ProfileHeaderRowPreview() {
    MaterialTheme() {
        AsyncImagePreviewProvider {
            ProfileHeaderRow(
                userUiModel = ProfileUserUiModel(
                    imageId = "audire",
                    nickname = "Heath Patel",
                    bio = "dicunt",
                    email = "alonzo.vincent@example.com"
                ),
                object : ProfileRowListener {
                    override fun onEditClick() {}
                    override fun onImagePicked(fileName: String, bytes: ByteArray) {}
                    override fun onSettingsClick() {}
                    override fun onLogoutClick() {}
                },
            )
        }
    }
}
