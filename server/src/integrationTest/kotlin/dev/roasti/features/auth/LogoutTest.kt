package dev.roasti.features.auth

import dev.roasti.feature.auth.data.network.model.request.LogoutRequestDto
import dev.roasti.feature.auth.data.network.model.request.RefreshRequestDto
import dev.roasti.jsonClient
import dev.roasti.withApp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals

class LogoutTest {

  @Test
  fun `logout - happy path`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password)

    val response =
        jsonClient(auth.accessToken).post("/api/v1/auth/logout") {
          contentType(ContentType.Application.Json)
          setBody(LogoutRequestDto(refreshToken = auth.refreshToken))
        }
    assertEquals(HttpStatusCode.NoContent, response.status)
  }

  @Test
  fun `logout - double logout is idempotent`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password)

    repeat(2) {
      val response =
          jsonClient(auth.accessToken).post("/api/v1/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(LogoutRequestDto(refreshToken = auth.refreshToken))
          }
      assertEquals(HttpStatusCode.NoContent, response.status)
    }
  }

  @Test
  fun `logout - revoked token cannot be used to refresh`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password)

    jsonClient(auth.accessToken).post("/api/v1/auth/logout") {
      contentType(ContentType.Application.Json)
      setBody(LogoutRequestDto(refreshToken = auth.refreshToken))
    }

    val response =
        jsonClient().post("/api/v1/auth/refresh") {
          contentType(ContentType.Application.Json)
          setBody(RefreshRequestDto(refreshToken = auth.refreshToken))
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }
}
