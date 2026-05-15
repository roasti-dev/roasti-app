package dev.roasti.features.uploads

import dev.roasti.FIREBASE_AUTH
import dev.roasti.FirebasePrincipal
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.uploadRoutes() {
  val uploadService by inject<UploadService>()

  authenticate(FIREBASE_AUTH) {
    post<UploadImages> { _ ->
      val uploaderId = call.principal<FirebasePrincipal>()!!.id
      val multipart = call.receiveMultipart()
      var saveResult: SaveUploadError? = null
      var meta: UploadMeta? = null
      var invalidMedia = false
      multipart.forEachPart { part ->
        if (part is PartData.FileItem && part.name == "file") {
          val bytes = part.provider().readRemaining().readByteArray()
          val filename = part.originalFileName ?: "upload"
          val contentType = part.contentType?.toString() ?: contentTypeFromFilename(filename)
          if (!contentType.startsWith("image/")) {
            invalidMedia = true
          } else {
            uploadService
                .save(contentType, bytes, uploaderId)
                .fold(
                    ifLeft = { saveResult = it },
                    ifRight = { meta = it },
                )
          }
        }
        part.dispose()
      }
      if (invalidMedia) return@post call.respond(HttpStatusCode.UnsupportedMediaType)
      saveResult?.let { error ->
        return@post when (error) {
          SaveUploadError.EmptyFile -> call.respond(HttpStatusCode.UnsupportedMediaType)
          SaveUploadError.FileTooLarge -> call.respond(HttpStatusCode.PayloadTooLarge)
        }
      }
      val result = meta ?: return@post call.respond(HttpStatusCode.BadRequest, "missing file field")
      call.respond(HttpStatusCode.Created, UploadResponseDto(result.id.value.toString()))
    }
  }

  get<UploadImages.ById> { res ->
    val upload = uploadService.findById(res.id) ?: return@get call.respond(HttpStatusCode.NotFound)
    call.respondBytes(upload.bytes, ContentType.parse(upload.contentType))
  }
}

@Serializable private data class UploadResponseDto(@SerialName("id") val id: String)

private fun contentTypeFromFilename(name: String): String =
    when {
      name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) ->
          "image/jpeg"
      name.endsWith(".png", ignoreCase = true) -> "image/png"
      name.endsWith(".gif", ignoreCase = true) -> "image/gif"
      name.endsWith(".webp", ignoreCase = true) -> "image/webp"
      else -> "application/octet-stream"
    }
