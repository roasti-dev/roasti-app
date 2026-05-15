package dev.roasti.features.posts

import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeletePostTest {

  @Test
  fun `delete post - author can delete`() = withApp {
    val client = newAuthenticatedClient()
    val created = createPost(client)
    assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/posts/${created.id}").status)
  }

  @Test
  fun `delete post - deleted post no longer appears in list`() = withApp {
    val client = newAuthenticatedClient()
    val authorId = getMyId(client)
    val created = createPost(client)
    client.delete("/api/v1/posts/${created.id}")

    val page =
        client.get("/api/v1/posts?author_id=$authorId").body<PageResponseDto<PostResponseDto>>()
    assertTrue(page.items.none { it.id == created.id })
  }

  @Test
  fun `delete post - non-author returns 204`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val created = createPost(c1)
    // TODO: or return 403
    assertEquals(HttpStatusCode.NoContent, c2.delete("/api/v1/posts/${created.id}").status)
  }

  @Test
  fun `delete post - non-existent returns 204`() = withApp {
    val client = newAuthenticatedClient()
    assertEquals(
        HttpStatusCode.NoContent,
        client.delete("/api/v1/posts/${UUID.randomUUID()}").status,
    )
  }

  @Test
  fun `delete post - unauthenticated returns 401`() = withApp {
    val client = newAuthenticatedClient()
    val created = createPost(client)
    assertEquals(
        HttpStatusCode.Unauthorized,
        jsonClient().delete("/api/v1/posts/${created.id}").status,
    )
  }
}
