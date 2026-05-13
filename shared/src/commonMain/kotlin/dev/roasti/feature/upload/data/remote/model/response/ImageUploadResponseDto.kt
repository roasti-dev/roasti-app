package dev.roasti.feature.upload.data.remote.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageUploadResponseDto(
    @SerialName("id")
    val id: String,
)
