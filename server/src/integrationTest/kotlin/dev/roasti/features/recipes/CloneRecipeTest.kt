package dev.roasti.features.recipes

import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CloneRecipeTest {

  @Test
  fun `clone recipe - happy path`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val original = createRecipe(c1)
    toggleLike(c2, original.id)

    val response = c2.post("/api/v1/recipes/${original.id}/clone")
    assertEquals(HttpStatusCode.Created, response.status)
    val clone = response.body<RecipeResponseDto>()
    assertTrue(clone.id != original.id)
    assertEquals(original.title, clone.title)
    assertNotNull(clone.origin)
    assertEquals(original.id, clone.origin!!.recipeId)
    assertEquals(0, clone.likesCount)
    assertFalse(clone.isPublic)
  }

  @Test
  fun `clone recipe - cannot clone private recipe`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val original = createRecipe(c1)
    val privateClone = c1.post("/api/v1/recipes/${original.id}/clone").body<RecipeResponseDto>()

    assertEquals(
        HttpStatusCode.NotFound,
        c2.post("/api/v1/recipes/${privateClone.id}/clone").status,
    )
  }

  @Test
  fun `clone recipe - unauthenticated returns 401`() = withApp {
    val client = newAuthenticatedClient()
    val recipe = createRecipe(client)
    assertEquals(
        HttpStatusCode.Unauthorized,
        jsonClient().post("/api/v1/recipes/${recipe.id}/clone").status,
    )
  }

  @Test
  fun `clone recipe - non-existent returns 404`() = withApp {
    val client = newAuthenticatedClient()
    assertEquals(
        HttpStatusCode.NotFound,
        client.post("/api/v1/recipes/${UUID.randomUUID()}/clone").status,
    )
  }
}
