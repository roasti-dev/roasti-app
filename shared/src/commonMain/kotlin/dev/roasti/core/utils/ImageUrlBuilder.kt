package dev.roasti.core.utils

import dev.roasti.core.config.AppConfig
import dev.roasti.core.network.ApiRoutes

fun imageUrl(imageIdOrUrl: String): String =
    if (imageIdOrUrl.startsWith("http://") || imageIdOrUrl.startsWith("https://")) {
        imageIdOrUrl
    } else {
        "${AppConfig.BASE_URL}${ApiRoutes.UploadsImages}/$imageIdOrUrl"
    }
