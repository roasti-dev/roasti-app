package dev.roasti.features.users

import dev.roasti.feature.auth.data.network.model.request.UpdateProfileRequest
import dev.roasti.feature.auth.data.network.model.response.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlin.test.assertEquals

suspend fun ApplicationTestBuilder.getMe(client: HttpClient): UserDto =
    client.get("/api/v1/users/me").body()

suspend fun ApplicationTestBuilder.updateMe(
    client: HttpClient,
    body: UpdateProfileRequest,
): UserDto {
  val response =
      client.patch("/api/v1/users/me") {
        contentType(ContentType.Application.Json)
        setBody(body)
      }
  assertEquals(HttpStatusCode.OK, response.status)
  return response.body()
}
