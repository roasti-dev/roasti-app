package dev.roasti.features.recipes

import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetRecipeTest {

  @Test
  fun `get recipe - own recipe`() = withApp {
    val client = newAuthenticatedClient()
    val created = createRecipe(client)
    val response = client.get("/api/v1/recipes/${created.id}")
    assertEquals(HttpStatusCode.OK, response.status)
    val recipe = response.body<RecipeResponseDto>()
    assertEquals(created.id, recipe.id)
    assertNotNull(recipe.authorId)
  }

  @Test
  fun `get recipe - another user's public recipe`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val created = createRecipe(c1)
    val response = c2.get("/api/v1/recipes/${created.id}")
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals(created.id, response.body<RecipeResponseDto>().id)
  }

  @Test
  fun `get recipe - not found`() = withApp {
    val client = newAuthenticatedClient()
    assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/recipes/${UUID.randomUUID()}").status)
  }

  @Test
  fun `get recipe - another user's private recipe returns 404`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val original = createRecipe(c1)
    val clone = c2.post("/api/v1/recipes/${original.id}/clone").body<RecipeResponseDto>()
    assertEquals(HttpStatusCode.NotFound, c1.get("/api/v1/recipes/${clone.id}").status)
  }

  @Test
  fun `get recipe - contains author info`() = withApp {
    val client = newAuthenticatedClient()
    val created = createRecipe(client)
    val recipe = client.get("/api/v1/recipes/${created.id}").body<RecipeResponseDto>()
    assertNotNull(recipe.author)
    assertEquals(created.authorId, recipe.author!!.id)
    assertTrue(recipe.author!!.username.isNotEmpty())
  }

  @Test
  fun `get recipe - author sees own note`() = withApp {
    val client = newAuthenticatedClient()
    val note = "my private note"
    val created = createRecipe(client, samplePayload().copy(note = note))
    val fetched = client.get("/api/v1/recipes/${created.id}").body<RecipeResponseDto>()
    assertEquals(note, fetched.note)
  }
}
