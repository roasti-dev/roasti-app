package dev.roasti.features.auth

import dev.roasti.feature.auth.data.network.model.request.RefreshRequestDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.jsonClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshTokenTest {

  @Test
  fun `refresh - happy path`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password)

    val response =
        jsonClient().post("/api/v1/auth/refresh") {
          contentType(ContentType.Application.Json)
          setBody(RefreshRequestDto(refreshToken = auth.refreshToken))
        }
    assertEquals(HttpStatusCode.OK, response.status)
    val refreshed = response.body<RefreshResponseDto>()
    assertTrue(refreshed.accessToken.isNotEmpty())
    assertTrue(refreshed.refreshToken.isNotEmpty())
  }

  @Test
  fun `refresh - new token is usable`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password)

    val first =
        jsonClient()
            .post("/api/v1/auth/refresh") {
              contentType(ContentType.Application.Json)
              setBody(RefreshRequestDto(refreshToken = auth.refreshToken))
            }
            .body<RefreshResponseDto>()

    val second =
        jsonClient().post("/api/v1/auth/refresh") {
          contentType(ContentType.Application.Json)
          setBody(RefreshRequestDto(refreshToken = first.refreshToken))
        }
    assertEquals(HttpStatusCode.OK, second.status)
    assertTrue(second.body<RefreshResponseDto>().accessToken.isNotEmpty())
  }

  @Test
  fun `refresh - invalid token returns 401`() = withApp {
    val response =
        jsonClient().post("/api/v1/auth/refresh") {
          contentType(ContentType.Application.Json)
          setBody(RefreshRequestDto(refreshToken = "invalid-token"))
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }
}
