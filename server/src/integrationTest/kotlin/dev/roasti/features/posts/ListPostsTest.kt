package dev.roasti.features.posts

import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListPostsTest {

  @Test
  fun `list posts - filters by author_id`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    createPost(c1)
    createPost(c2)
    val c1Id = getMyId(c1)

    val page = c1.get("/api/v1/posts?author_id=$c1Id").body<PageResponseDto<PostResponseDto>>()
    assertTrue(page.items.isNotEmpty())
    assertTrue(page.items.all { it.author.id == c1Id })
  }

  @Test
  fun `list posts - returns all posts when no author_id`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    createPost(c1)
    createPost(c2)
    val c1Id = getMyId(c1)
    val c2Id = getMyId(c2)

    val page = c1.get("/api/v1/posts").body<PageResponseDto<PostResponseDto>>()
    val authorIds = page.items.map { it.author.id }.toSet()
    assertTrue(c1Id in authorIds)
    assertTrue(c2Id in authorIds)
  }

  @Test
  fun `list posts - invalid author_id returns 400`() = withApp {
    val client = newAuthenticatedClient()
    assertEquals(
        HttpStatusCode.BadRequest,
        client.get("/api/v1/posts?author_id=not-a-uuid").status,
    )
  }

  @Test
  fun `list posts - respects pagination`() = withApp {
    val client = newAuthenticatedClient()
    val authorId = getMyId(client)
    repeat(3) { createPost(client) }

    val page =
        client
            .get("/api/v1/posts?author_id=$authorId&limit=2")
            .body<PageResponseDto<PostResponseDto>>()
    assertEquals(2, page.items.size)
    assertEquals(2, page.pagination.lastPage)
  }
}
