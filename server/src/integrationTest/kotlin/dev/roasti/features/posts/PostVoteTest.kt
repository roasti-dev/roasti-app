package dev.roasti.features.posts

import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import dev.roasti.feature.post.data.remote.model.request.VoteRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostVoteResponseDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PostVoteTest {

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
}
