package dev.roasti.features.posts

import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.auth.data.network.model.response.UserDto
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.UpdatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.VoteRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostVoteResponseDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
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

class PostTest {

  private val defaultPayload =
      CreatePostRequestDto(title = "My first post", text = "My first post text")

  private suspend fun ApplicationTestBuilder.createPost(
      client: HttpClient,
      payload: CreatePostRequestDto = defaultPayload,
  ): PostResponseDto {
    val response =
        client.post("/api/v1/posts") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }
    assertEquals(HttpStatusCode.Created, response.status)
    return response.body()
  }

  private suspend fun ApplicationTestBuilder.getMyId(client: HttpClient): String =
      client.get("/api/v1/users/me").body<UserDto>().id

  @Test
  fun `create post - happy path text only`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)
    assertNotNull(post.id)
    assertEquals(defaultPayload.text, post.text)
    assertEquals(0, post.rating)
    assertEquals(VoteDirectionDto.NONE, post.userVote)
  }

  @Test
  fun `create post - happy path images only`() = withApp {
    val client = newAuthenticatedClient()
    val images = listOf("img-1", "img-2")
    val post = createPost(client, CreatePostRequestDto(title = "Images only", images = images))
    assertEquals(images, post.images)
  }

  @Test
  fun `create post - empty post returns 422`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.post("/api/v1/posts") {
          contentType(ContentType.Application.Json)
          setBody(CreatePostRequestDto())
        }
    assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
  }

  @Test
  fun `create post - unauthenticated returns 401`() = withApp {
    val response =
        jsonClient().post("/api/v1/posts") {
          contentType(ContentType.Application.Json)
          setBody(defaultPayload)
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }

  @Test
  fun `get post - returns post by id`() = withApp {
    val client = newAuthenticatedClient()
    val created = createPost(client)
    val response = client.get("/api/v1/posts/${created.id}")
    assertEquals(HttpStatusCode.OK, response.status)
    val post = response.body<PostResponseDto>()
    assertEquals(created.id, post.id)
    assertEquals(defaultPayload.text, post.text)
    assertTrue(post.author.username.isNotEmpty())
  }

  @Test
  fun `get post - zero rating and no user vote`() = withApp {
    val client = newAuthenticatedClient()
    val created = createPost(client)
    val post = client.get("/api/v1/posts/${created.id}").body<PostResponseDto>()
    assertEquals(0, post.rating)
    assertEquals(VoteDirectionDto.NONE, post.userVote)
  }

  // TODO: GET /posts/{id} is public in Kotlin — decide if auth should be required (Go returns 401
  // for unauthenticated)

  @Test
  fun `get post - not found returns 404`() = withApp {
    val client = newAuthenticatedClient()
    assertEquals(
        HttpStatusCode.NotFound,
        client.get("/api/v1/posts/${UUID.randomUUID()}").status,
    )
  }

  @Test
  fun `list posts - returns created posts filtered by author`() = withApp {
    val client = newAuthenticatedClient()
    createPost(client)
    val authorId = getMyId(client)

    val page =
        client.get("/api/v1/posts?author_id=$authorId").body<PageResponseDto<PostResponseDto>>()
    assertTrue(page.items.isNotEmpty())
    assertTrue(page.items.all { it.author.id == authorId })
  }

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

  @Test
  fun `vote - upvote increases rating`() = withApp {
    val author = newAuthenticatedClient()
    val voter = newAuthenticatedClient()
    val post = createPost(author)

    val response =
        voter.put("/api/v1/posts/${post.id}/vote") {
          contentType(ContentType.Application.Json)
          setBody(VoteRequestDto(type = VoteDirectionDto.UP))
        }
    assertEquals(HttpStatusCode.OK, response.status)
    val vote = response.body<PostVoteResponseDto>()
    assertEquals(1, vote.rating)
    assertEquals(VoteDirectionDto.UP, vote.userVote)
  }

  @Test
  fun `vote - downvote decreases rating`() = withApp {
    val author = newAuthenticatedClient()
    val voter = newAuthenticatedClient()
    val post = createPost(author)

    val vote =
        voter
            .put("/api/v1/posts/${post.id}/vote") {
              contentType(ContentType.Application.Json)
              setBody(VoteRequestDto(type = VoteDirectionDto.DOWN))
            }
            .body<PostVoteResponseDto>()
    assertEquals(-1, vote.rating)
    assertEquals(VoteDirectionDto.DOWN, vote.userVote)
  }

  @Test
  fun `vote - changing vote replaces previous`() = withApp {
    val author = newAuthenticatedClient()
    val voter = newAuthenticatedClient()
    val post = createPost(author)

    voter.put("/api/v1/posts/${post.id}/vote") {
      contentType(ContentType.Application.Json)
      setBody(VoteRequestDto(type = VoteDirectionDto.UP))
    }
    val vote =
        voter
            .put("/api/v1/posts/${post.id}/vote") {
              contentType(ContentType.Application.Json)
              setBody(VoteRequestDto(type = VoteDirectionDto.DOWN))
            }
            .body<PostVoteResponseDto>()
    assertEquals(-1, vote.rating)
  }

  @Test
  fun `vote - reflects in get post`() = withApp {
    val author = newAuthenticatedClient()
    val voter = newAuthenticatedClient()
    val post = createPost(author)

    voter.put("/api/v1/posts/${post.id}/vote") {
      contentType(ContentType.Application.Json)
      setBody(VoteRequestDto(type = VoteDirectionDto.UP))
    }
    val fetched = voter.get("/api/v1/posts/${post.id}").body<PostResponseDto>()
    assertEquals(1, fetched.rating)
    assertEquals(VoteDirectionDto.UP, fetched.userVote)
  }

  @Test
  fun `vote - non-existent post returns 404`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.put("/api/v1/posts/${UUID.randomUUID()}/vote") {
          contentType(ContentType.Application.Json)
          setBody(VoteRequestDto(type = VoteDirectionDto.UP))
        }
    assertEquals(HttpStatusCode.NotFound, response.status)
  }

  @Test
  fun `vote - unauthenticated returns 401`() = withApp {
    val author = newAuthenticatedClient()
    val post = createPost(author)
    val response =
        jsonClient().put("/api/v1/posts/${post.id}/vote") {
          contentType(ContentType.Application.Json)
          setBody(VoteRequestDto(type = VoteDirectionDto.UP))
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }

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
  // validated
  // in comment listing

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
