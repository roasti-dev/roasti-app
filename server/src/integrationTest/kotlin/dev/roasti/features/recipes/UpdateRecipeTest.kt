package dev.roasti.features.recipes

import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateRecipeTest {

  @Test
  fun `update recipe - happy path`() = withApp {
    val client = newAuthenticatedClient()
    val created = createRecipe(client)
    val response =
        client.put("/api/v1/recipes/${created.id}") {
          contentType(ContentType.Application.Json)
          setBody(samplePayload(title = "Updated Title"))
        }
    assertEquals(HttpStatusCode.OK, response.status)
    assertEquals("Updated Title", response.body<RecipeResponseDto>().title)
  }

  @Test
  fun `update recipe - forbidden for non-author`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val created = createRecipe(c1)
    val response =
        c2.put("/api/v1/recipes/${created.id}") {
          contentType(ContentType.Application.Json)
          setBody(samplePayload())
        }
    assertEquals(HttpStatusCode.Forbidden, response.status)
  }

  @Test
  fun `update recipe - not found`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.put("/api/v1/recipes/${UUID.randomUUID()}") {
          contentType(ContentType.Application.Json)
          setBody(samplePayload())
        }
    assertEquals(HttpStatusCode.NotFound, response.status)
  }
}
