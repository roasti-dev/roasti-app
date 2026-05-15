package dev.roasti.features.posts

import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostRecipeStatusDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.uploadImage
import dev.roasti.withApp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreatePostTest {

  @Test
  fun `create post - happy path text only`() = withApp {
    val client = newAuthenticatedClient()
    val post = createPost(client)
    assertNotNull(post.id)
    assertEquals(defaultPostPayload.text, post.text)
    assertEquals(0, post.rating)
    assertEquals(VoteDirectionDto.NONE, post.userVote)
  }

  @Test
  fun `create post - happy path images only`() = withApp {
    val client = newAuthenticatedClient()
    val images = listOf(uploadImage(client), uploadImage(client))
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
  fun `create post - with recipe sets ref as AVAILABLE`() = withApp {
    val client = newAuthenticatedClient()
    val recipe = createRecipe(client)
    val post = createPost(client, CreatePostRequestDto(text = "with recipe", recipeId = recipe.id))
    assertEquals(recipe.id, post.recipe?.id)
    assertEquals(PostRecipeStatusDto.AVAILABLE, post.recipe?.status)
  }

  @Test
  fun `create post - unauthenticated returns 401`() = withApp {
    val response =
        jsonClient().post("/api/v1/posts") {
          contentType(ContentType.Application.Json)
          setBody(defaultPostPayload)
        }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
  }
}
