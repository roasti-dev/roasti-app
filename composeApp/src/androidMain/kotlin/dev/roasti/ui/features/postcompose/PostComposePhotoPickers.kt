package dev.roasti.ui.features.postcompose

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import dev.roasti.utils.compressImage
import java.io.File
import java.util.UUID

typealias OnPhotosPicked = (picks: List<PickedImage>) -> Unit

@Composable
fun rememberGalleryPicker(onResult: OnPhotosPicked): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_POST_PHOTOS),
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val picks = uris.map { uri ->
            PickedImage(
                fileName = "${UUID.randomUUID()}.jpg",
                bytes = compressImage(context.contentResolver, uri),
            )
        }
        onResult(picks)
    }
    return remember(launcher) {
        {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}

@Composable
fun rememberCameraPicker(onResult: OnPhotosPicked): () -> Unit {
    val context = LocalContext.current
    val pendingFileHolder = remember { arrayOfNulls<File>(1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved: Boolean ->
        val file = pendingFileHolder[0]
        pendingFileHolder[0] = null
        if (saved && file != null && file.exists()) {
            val uri = Uri.fromFile(file)
            val bytes = compressImage(context.contentResolver, uri)
            onResult(listOf(PickedImage(fileName = file.name, bytes = bytes)))
            file.delete()
        }
    }

    return remember(launcher) {
        {
            val dir = File(context.cacheDir, "post_camera").apply { mkdirs() }
            val file = File(dir, "post_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            pendingFileHolder[0] = file
            launcher.launch(uri)
        }
    }
}
