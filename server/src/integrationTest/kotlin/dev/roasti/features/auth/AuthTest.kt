package dev.roasti.features.auth

import dev.roasti.feature.auth.data.network.model.request.LoginRequestDto
import dev.roasti.feature.auth.data.network.model.request.LogoutRequestDto
import dev.roasti.feature.auth.data.network.model.request.RefreshRequestDto
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.jsonClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthTest {

  private fun credentials(): Triple<String, String, String> {
    val username = "user_${UUID.randomUUID().toString().take(8)}"
    return Triple(username, "$username@test.com", "password123")
  }

  private suspend fun ApplicationTestBuilder.register(
      username: String,
      email: String,
      password: String,
      name: String? = null,
  ): AuthResponseDto {
    val response =
        jsonClient().post("/api/v1/auth/register") {
          contentType(ContentType.Application.Json)
          setBody(
              RegisterRequestDto(
                  email = email,
                  username = username,
                  password = password,
                  name = name,
              )
          )
        }
    assertEquals(HttpStatusCode.Created, response.status)
    return response.body()
  }

  @Test
  fun `register - happy path`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password)
    assertTrue(auth.accessToken.isNotEmpty())
    assertTrue(auth.refreshToken.isNotEmpty())
    assertEquals(username, auth.user.username)
    assertNotNull(auth.user.id)
    assertEquals(email, auth.user.email)
  }

  @Test
  fun `register - duplicate username returns 409`() = withApp {
    val (username, email, password) = credentials()
    register(username, email, password)

    val response =
        jsonClient().post("/api/v1/auth/register") {
          contentType(ContentType.Application.Json)
          setBody(
              RegisterRequestDto(email = "other_$email", username = username, password = password)
          )
        }
    assertEquals(HttpStatusCode.Conflict, response.status)
  }

  @Test
  fun `register - duplicate email returns 409`() = withApp {
    val (username, email, password) = credentials()
    register(username, email, password)

    val response =
        jsonClient().post("/api/v1/auth/register") {
          contentType(ContentType.Application.Json)
          setBody(RegisterRequestDto(email = email, username = "f_$username", password = password))
        }
    assertEquals(HttpStatusCode.Conflict, response.status)
  }

  @Test
  fun `register - weak password returns 422`() = withApp {
    val (username, email, _) = credentials()
    val response =
        jsonClient().post("/api/v1/auth/register") {
          contentType(ContentType.Application.Json)
          setBody(RegisterRequestDto(email = email, username = username, password = "123"))
        }
    assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
  }

  @Test
  fun `register - with optional name`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password, name = "John Doe")
    assertEquals("John Doe", auth.user.name)
  }

  @Test
  fun `register - invalid username format returns 422`() = withApp {
    val (_, email, password) = credentials()
    val response =
        jsonClient().post("/api/v1/auth/register") {
          contentType(ContentType.Application.Json)
          setBody(
              RegisterRequestDto(
                  email = email,
                  username = "invalid username!",
                  password = password,
              )
          )
        }
    assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
  }

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
