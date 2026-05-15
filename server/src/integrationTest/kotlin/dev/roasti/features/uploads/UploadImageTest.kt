package dev.roasti.features.uploads

import dev.roasti.feature.upload.data.remote.model.response.ImageUploadResponseDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UploadImageTest {

  @Test
  fun `upload image - happy path`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.post("/api/v1/uploads/images") { setBody(multipart(generatePng(), "image/png")) }
    assertEquals(HttpStatusCode.Created, response.status)
    assertNotNull(response.body<ImageUploadResponseDto>().id)
  }

  @Test
  fun `upload image - empty file returns 415`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.post("/api/v1/uploads/images") { setBody(multipart(ByteArray(0), "image/png")) }
    assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
  }

  @Test
  fun `upload image - unsupported mime type returns 415`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.post("/api/v1/uploads/images") {
          setBody(multipart("not an image".toByteArray(), "application/octet-stream", "file.bin"))
        }
    assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
  }

  @Test
  fun `upload image - unauthenticated returns 401`() = withApp {
    val response =
        jsonClient().post("/api/v1/uploads/images") {
          setBody(multipart(generatePng(), "image/png"))
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }
}
