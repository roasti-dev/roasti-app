package dev.roasti.features.uploads

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@OptIn(ExperimentalUuidApi::class) @Serializable @JvmInline value class ImageId(val value: Uuid)
