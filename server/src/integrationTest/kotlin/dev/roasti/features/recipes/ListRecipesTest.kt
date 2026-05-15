package dev.roasti.features.recipes

import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlin.test.Test
import kotlin.test.assertTrue

class ListRecipesTest {

  @Test
  fun `list recipes - returns only public recipes`() = withApp {
    val c1 = newAuthenticatedClient()
    val c2 = newAuthenticatedClient()
    createRecipe(c1)
    val pub = createRecipe(c2)
    c2.post("/api/v1/recipes/${pub.id}/clone")

    val page = c1.get("/api/v1/recipes").body<PageResponseDto<RecipeResponseDto>>()
    assertTrue(page.items.all { it.isPublic })
  }

  @Test
  fun `list recipes - filter by brew method`() = withApp {
    val client = newAuthenticatedClient()
    createRecipe(client, samplePayload().copy(brewMethod = BrewMethodDto.AEROPRESS))

    val page =
        client
            .get("/api/v1/recipes?brew_method=aeropress")
            .body<PageResponseDto<RecipeResponseDto>>()
    assertTrue(page.items.isNotEmpty())
    assertTrue(page.items.all { it.brewMethod == BrewMethodDto.AEROPRESS })
  }

  @Test
  fun `list recipes - filter by roast level`() = withApp {
    val client = newAuthenticatedClient()
    createRecipe(client, samplePayload().copy(roastLevel = RoastLevelDto.DARK))

    val page =
        client.get("/api/v1/recipes?roast_level=dark").body<PageResponseDto<RecipeResponseDto>>()
    assertTrue(page.items.isNotEmpty())
    assertTrue(page.items.all { it.roastLevel == RoastLevelDto.DARK })
  }

  @Test
  fun `list recipes - contains author`() = withApp {
    val client = newAuthenticatedClient()
    createRecipe(client)

    val page = client.get("/api/v1/recipes").body<PageResponseDto<RecipeResponseDto>>()
    assertTrue(page.items.isNotEmpty())
    assertTrue(
        page.items.all {
          it.author != null && it.author!!.id.isNotEmpty() && it.author!!.username.isNotEmpty()
        }
    )
  }
}
