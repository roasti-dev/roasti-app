package dev.roasti.features.auth

import dev.roasti.feature.auth.data.network.model.request.LoginRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
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

class LoginTest {

  @Test
  fun `login - happy path`() = withApp {
    val (username, email, password) = credentials()
    register(username, email, password)

    val response =
        jsonClient().post("/api/v1/auth/login") {
          contentType(ContentType.Application.Json)
          setBody(LoginRequestDto(username = username, password = password))
        }
    assertEquals(HttpStatusCode.OK, response.status)
    val auth = response.body<AuthResponseDto>()
    assertTrue(auth.accessToken.isNotEmpty())
    assertTrue(auth.refreshToken.isNotEmpty())
    assertEquals(email, auth.user.email)
  }

  @Test
  fun `login - invalid credentials returns 401`() = withApp {
    val (username, _, _) = credentials()
    val response =
        jsonClient().post("/api/v1/auth/login") {
          contentType(ContentType.Application.Json)
          setBody(LoginRequestDto(username = username, password = "wrongpassword"))
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }
}
