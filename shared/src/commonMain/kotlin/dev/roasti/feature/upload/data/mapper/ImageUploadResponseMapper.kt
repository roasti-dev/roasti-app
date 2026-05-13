package dev.roasti.feature.upload.data.mapper

import dev.roasti.feature.upload.data.remote.model.response.ImageUploadResponseDto
import dev.roasti.feature.upload.domain.UploadedImage

fun ImageUploadResponseDto.toDomain() = UploadedImage(id = id)
