package dev.roasti.features.uploads

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import dev.roasti.feature.upload.data.remote.model.response.ImageUploadResponseDto
import dev.roasti.newAuthenticatedClient
import dev.roasti.jsonClient
import dev.roasti.withApp
import io.ktor.client.call.body
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UploadTest {

    private fun generatePng(): ByteArray {
        val img = java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val baos = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(img, "png", baos)
        return baos.toByteArray()
    }

    private fun multipart(bytes: ByteArray, contentType: String, filename: String = "image.png") =
        MultiPartFormDataContent(formData {
            append("file", bytes, Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                append(HttpHeaders.ContentType, contentType)
            })
        })

    @Test
    fun `upload image - happy path`() = withApp {
        val client = newAuthenticatedClient()
        val response = client.post("/api/v1/uploads/images") {
            setBody(multipart(generatePng(), "image/png"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertNotNull(response.body<ImageUploadResponseDto>().id)
    }

    @Test
    fun `upload image - empty file returns 415`() = withApp {
        val client = newAuthenticatedClient()
        val response = client.post("/api/v1/uploads/images") {
            setBody(multipart(ByteArray(0), "image/png"))
        }
        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `upload image - unsupported mime type returns 415`() = withApp {
        val client = newAuthenticatedClient()
        val response = client.post("/api/v1/uploads/images") {
            setBody(multipart("not an image".toByteArray(), "application/octet-stream", "file.bin"))
        }
        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test
    fun `upload image - unauthenticated returns 401`() = withApp {
        val response = jsonClient().post("/api/v1/uploads/images") {
            setBody(multipart(generatePng(), "image/png"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `get image - happy path`() = withApp {
        val client = newAuthenticatedClient()
        val upload = client.post("/api/v1/uploads/images") {
            setBody(multipart(generatePng(), "image/png"))
        }.body<ImageUploadResponseDto>()

        val response = client.get("/api/v1/uploads/images/${upload.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Image.PNG, response.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) })
    }

    @Test
    fun `get image - not found`() = withApp {
        val client = newAuthenticatedClient()
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/uploads/images/non-existent-id").status)
    }
}
