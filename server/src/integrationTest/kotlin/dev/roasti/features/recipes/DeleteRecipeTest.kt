package dev.roasti.features.recipes

import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteRecipeTest {

  @Test
  fun `delete recipe - happy path`() = withApp {
    val client = newAuthenticatedClient()
    val created = createRecipe(client)
    assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/recipes/${created.id}").status)
  }

  @Test
  fun `delete recipe - non-author on public recipe returns 204`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    val created = createRecipe(c1)
    // TODO: or return 403
    assertEquals(HttpStatusCode.NoContent, c2.delete("/api/v1/recipes/${created.id}").status)
  }

  @Test
  fun `delete recipe - non-existent returns 204`() = withApp {
    val client = newAuthenticatedClient()
    // or return 404
    assertEquals(
        HttpStatusCode.NoContent,
        client.delete("/api/v1/recipes/${UUID.randomUUID()}").status,
    )
  }

  @Test
  fun `delete recipe - already deleted returns 204`() = withApp {
    val client = newAuthenticatedClient()
    val created = createRecipe(client)
    client.delete("/api/v1/recipes/${created.id}")
    assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/recipes/${created.id}").status)
  }
}
