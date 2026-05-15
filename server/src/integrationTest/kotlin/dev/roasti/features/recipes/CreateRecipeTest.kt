package dev.roasti.features.recipes

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

class CreateRecipeTest {

  @Test
  fun `create recipe - happy path`() = withApp {
    val client = newAuthenticatedClient()
    val recipe = createRecipe(client)
    assertEquals("Test Recipe", recipe.title)
    assertNotNull(recipe.id)
    assertNotNull(recipe.authorId)
  }

  @Test
  fun `create recipe - empty title returns 422`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.post("/api/v1/recipes") {
          contentType(ContentType.Application.Json)
          setBody(samplePayload(title = ""))
        }
    assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
  }

  @Test
  fun `create recipe - empty description returns 422`() = withApp {
    val client = newAuthenticatedClient()
    val response =
        client.post("/api/v1/recipes") {
          contentType(ContentType.Application.Json)
          setBody(samplePayload().copy(description = ""))
        }
    assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
  }

  @Test
  fun `create recipe - with image`() = withApp {
    val client = newAuthenticatedClient()
    val imageId = uploadImage(client)
    val recipe = createRecipe(client, samplePayload().copy(imageId = imageId))
    assertEquals(imageId, recipe.imageId)
  }
}
