package dev.roasti.features.recipes

import dev.roasti.feature.likes.data.RecipeLikeDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecipeLikeTest {

  @Test
  fun `toggle like - like a recipe`() = withApp {
    val client = newAuthenticatedClient()
    val recipe = createRecipe(client)
    val like = client.post("/api/v1/recipes/${recipe.id}/like").body<RecipeLikeDto>()
    assertTrue(like.isLiked)
    assertEquals(1, like.likesCount)
  }

  @Test
  fun `toggle like - unlike a recipe`() = withApp {
    val client = newAuthenticatedClient()
    val recipe = createRecipe(client)
    toggleLike(client, recipe.id)
    val like = client.post("/api/v1/recipes/${recipe.id}/like").body<RecipeLikeDto>()
    assertFalse(like.isLiked)
    assertEquals(0, like.likesCount)
  }

  @Test
  fun `toggle like - two users like same recipe`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val recipe = createRecipe(c1)
    toggleLike(c1, recipe.id)
    val like = c2.post("/api/v1/recipes/${recipe.id}/like").body<RecipeLikeDto>()
    assertTrue(like.isLiked)
    assertEquals(2, like.likesCount)
  }

  @Test
  fun `toggle like - does not affect other recipes`() = withApp {
    val client = newAuthenticatedClient()
    val r1 = createRecipe(client)
    val r2 = createRecipe(client)
    toggleLike(client, r1.id)
    val like = client.post("/api/v1/recipes/${r2.id}/like").body<RecipeLikeDto>()
    assertTrue(like.isLiked)
    assertEquals(1, like.likesCount)
  }

  @Test
  fun `toggle like - unauthenticated returns 401`() = withApp {
    val client = newAuthenticatedClient()
    val recipe = createRecipe(client)
    assertEquals(
        HttpStatusCode.Unauthorized,
        jsonClient().post("/api/v1/recipes/${recipe.id}/like").status,
    )
  }

  @Test
  fun `toggle like - non-existent recipe returns 404`() = withApp {
    val client = newAuthenticatedClient()
    assertEquals(
        HttpStatusCode.NotFound,
        client.post("/api/v1/recipes/${UUID.randomUUID()}/like").status,
    )
  }
}
