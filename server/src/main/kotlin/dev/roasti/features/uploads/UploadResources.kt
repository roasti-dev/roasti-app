package dev.roasti.features.uploads

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/uploads/images")
class UploadImages {
  @Serializable
  @Resource("{id}")
  data class ById(val parent: UploadImages = UploadImages(), val id: ImageId)
}
