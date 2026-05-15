package dev.roasti.features.posts

import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostRecipeStatusDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetPostTest {

  @Test
  fun `get post - returns post by id`() = withApp {
    val client = newAuthenticatedClient()
    val created = createPost(client)
    val response = client.get("/api/v1/posts/${created.id}")
    assertEquals(HttpStatusCode.OK, response.status)
    val post = response.body<PostResponseDto>()
    assertEquals(created.id, post.id)
    assertEquals(defaultPostPayload.text, post.text)
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

  @Test
  fun `get post - recipe ref is UNAVAILABLE after recipe deleted`() = withApp {
    val client = newAuthenticatedClient()
    val recipe = createRecipe(client)
    val post = createPost(client, CreatePostRequestDto(text = "with recipe", recipeId = recipe.id))
    deleteRecipe(client, recipe.id)

    val fetched = client.get("/api/v1/posts/${post.id}").body<PostResponseDto>()
    assertEquals(recipe.id, fetched.recipe?.id)
    assertEquals(PostRecipeStatusDto.UNAVAILABLE, fetched.recipe?.status)
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
}
