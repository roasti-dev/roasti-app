package dev.roasti.features.uploads

import dev.roasti.feature.upload.data.remote.model.response.ImageUploadResponseDto
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class GetImageTest {

  @Test
  fun `get image - happy path`() = withApp {
    val client = newAuthenticatedClient()
    val upload =
        client
            .post("/api/v1/uploads/images") { setBody(multipart(generatePng(), "image/png")) }
            .body<ImageUploadResponseDto>()

    val response = client.get("/api/v1/uploads/images/${upload.id}")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(
        ContentType.Image.PNG,
        response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) },
    )
  }

  @Test
  fun `get image - not found`() = withApp {
    val client = newAuthenticatedClient()
    assertEquals(
        HttpStatusCode.NotFound,
        client.get("/api/v1/uploads/images/${Uuid.random()}").status,
    )
  }
}
