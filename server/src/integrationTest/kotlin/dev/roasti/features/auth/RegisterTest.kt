package dev.roasti.features.auth

import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.jsonClient
import dev.roasti.withApp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegisterTest {

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
  fun `register - with optional name`() = withApp {
    val (username, email, password) = credentials()
    val auth = register(username, email, password, name = "John Doe")
    assertEquals("John Doe", auth.user.name)
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
  fun `register - invalid username format returns 422`() = withApp {
    val (_, email, password) = credentials()
    val response =
        jsonClient().post("/api/v1/auth/register") {
          contentType(ContentType.Application.Json)
          setBody(
              RegisterRequestDto(email = email, username = "invalid username!", password = password)
          )
        }
    assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
  }
}
