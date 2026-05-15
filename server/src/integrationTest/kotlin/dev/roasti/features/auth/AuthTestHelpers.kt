package dev.roasti.features.auth

import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.jsonClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.UUID
import kotlin.test.assertEquals

fun credentials(): Triple<String, String, String> {
  val username = "user_${UUID.randomUUID().toString().take(8)}"
  return Triple(username, "$username@test.com", "password123")
}

suspend fun ApplicationTestBuilder.register(
    username: String,
    email: String,
    password: String,
    name: String? = null,
): AuthResponseDto {
  val response =
      jsonClient().post("/api/v1/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(
            RegisterRequestDto(email = email, username = username, password = password, name = name)
        )
      }
  assertEquals(HttpStatusCode.Created, response.status)
  return response.body()
}
