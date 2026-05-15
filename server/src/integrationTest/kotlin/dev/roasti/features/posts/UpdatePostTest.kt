package dev.roasti.features.posts

import dev.roasti.feature.post.data.remote.model.request.UpdatePostRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdatePostTest {

  @Test
  fun `update post - author can update`() = withApp {
    val client = newAuthenticatedClient()
    val created = createPost(client)
    val response =
        client.put("/api/v1/posts/${created.id}") {
          contentType(ContentType.Application.Json)
          setBody(UpdatePostRequestDto(title = "Updated title", text = "Updated text"))
        }
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Updated text", response.body<PostResponseDto>().text)
  }

  @Test
  fun `update post - non-author returns 403`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val created = createPost(c1)
    val response =
        c2.put("/api/v1/posts/${created.id}") {
          contentType(ContentType.Application.Json)
          setBody(UpdatePostRequestDto(title = "Hijack", text = "hijack"))
        }
    assertEquals(HttpStatusCode.Forbidden, response.status)
  }

  @Test
  fun `update post - not found returns 404`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.put("/api/v1/posts/${UUID.randomUUID()}") {
          contentType(ContentType.Application.Json)
          setBody(UpdatePostRequestDto(title = "X", text = "x"))
        }
    assertEquals(HttpStatusCode.NotFound, response.status)
  }

  @Test
  fun `update post - unauthenticated returns 401`() = withApp {
    val client = newAuthenticatedClient()
    val created = createPost(client)
    val response =
        jsonClient().put("/api/v1/posts/${created.id}") {
          contentType(ContentType.Application.Json)
          setBody(UpdatePostRequestDto(title = "X", text = "x"))
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }
}
