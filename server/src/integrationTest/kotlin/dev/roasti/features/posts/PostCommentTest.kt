package dev.roasti.features.posts

import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PostCommentTest {

  @Test
  fun `comment - create on own post`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)
    val response =
        client.post("/api/v1/posts/${post.id}/comments") {
          contentType(ContentType.Application.Json)
          setBody(CreateCommentRequestDto(text = "nice post"))
        }
    assertEquals(HttpStatusCode.Created, response.status)
    val comment = response.body<CommentResponseDto>()
    assertEquals("nice post", comment.text)
    assertNotNull(comment.id)
  }

  @Test
  fun `comment - another user can comment`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val post = createPost(c1)
    val response =
        c2.post("/api/v1/posts/${post.id}/comments") {
          contentType(ContentType.Application.Json)
          setBody(CreateCommentRequestDto(text = "great!"))
        }
    assertEquals(HttpStatusCode.Created, response.status)
  }

  @Test
  fun `comment - reply has parent id set`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)

    val root =
        client
            .post("/api/v1/posts/${post.id}/comments") {
              contentType(ContentType.Application.Json)
              setBody(CreateCommentRequestDto(text = "root"))
            }
            .body<CommentResponseDto>()

    val reply =
        client
            .post("/api/v1/posts/${post.id}/comments") {
              contentType(ContentType.Application.Json)
              setBody(CreateCommentRequestDto(text = "reply", parentId = root.id))
            }
            .body<CommentResponseDto>()

    assertEquals(root.id, reply.parentId)
  }

  // TODO: add test "comment - non-existent post returns 404" once post existence is validated in
  // comment creation

  @Test
  fun `comment - unauthenticated returns 401`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)
    val response =
        jsonClient().post("/api/v1/posts/${post.id}/comments") {
          contentType(ContentType.Application.Json)
          setBody(CreateCommentRequestDto(text = "hi"))
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }

  // TODO: add test "list comments - non-existent post returns 404" once post existence is
  // validated in comment listing

  @Test
  fun `list comments - empty page when no comments`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)
    val page =
        client
            .get("/api/v1/posts/${post.id}/comments")
            .body<PageResponseDto<CommentThreadResponseDto>>()
    assertTrue(page.items.isEmpty())
    assertEquals(0, page.pagination.itemsCount)
  }

  @Test
  fun `list comments - returns root comments with replies`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)

    val root =
        client
            .post("/api/v1/posts/${post.id}/comments") {
              contentType(ContentType.Application.Json)
              setBody(CreateCommentRequestDto(text = "root"))
            }
            .body<CommentResponseDto>()

    client.post("/api/v1/posts/${post.id}/comments") {
      contentType(ContentType.Application.Json)
      setBody(CreateCommentRequestDto(text = "reply", parentId = root.id))
    }

    val page =
        client
            .get("/api/v1/posts/${post.id}/comments")
            .body<PageResponseDto<CommentThreadResponseDto>>()
    assertEquals(1, page.items.size)
    assertEquals("root", page.items[0].text)
    assertEquals(1, page.items[0].replies.size)
    assertEquals("reply", page.items[0].replies[0].text)
  }

  @Test
  fun `list comments - respects pagination`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)
    repeat(3) { i ->
      client.post("/api/v1/posts/${post.id}/comments") {
        contentType(ContentType.Application.Json)
        setBody(CreateCommentRequestDto(text = "comment $i"))
      }
    }

    val page =
        client
            .get("/api/v1/posts/${post.id}/comments?limit=2")
            .body<PageResponseDto<CommentThreadResponseDto>>()
    assertEquals(2, page.items.size)
    assertEquals(2, page.pagination.lastPage)
  }
}
