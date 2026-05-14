package dev.roasti.features.recipes

import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.likes.data.RecipeLikeDto
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeRequestDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeStepRequestDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.uploadImage
import dev.roasti.withApp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RecipeTest {

  private fun samplePayload(title: String = "Test Recipe") =
      CreateRecipeRequestDto(
          title = title,
          description = "Test description",
          brewMethod = BrewMethodDto.V60,
          difficulty = DifficultyDto.EASY,
          roastLevel = RoastLevelDto.LIGHT,
          steps = listOf(CreateRecipeStepRequestDto(order = 1, title = "Boil water")),
      )

  private suspend fun ApplicationTestBuilder.createRecipe(
      client: HttpClient,
      payload: CreateRecipeRequestDto = samplePayload(),
  ): RecipeResponseDto {
    val response =
        client.post("/api/v1/recipes") {
          contentType(ContentType.Application.Json)
          setBody(payload)
        }
    assertEquals(HttpStatusCode.Created, response.status)
    return response.body()
  }

  private suspend fun ApplicationTestBuilder.toggleLike(client: HttpClient, recipeId: String) {
    val response = client.post("/api/v1/recipes/$recipeId/like")
    assertEquals(HttpStatusCode.OK, response.status)
  }

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
    assertEquals(
        HttpStatusCode.NotFound,
        client.get("/api/v1/recipes/${UUID.randomUUID()}").status,
    )
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

  @Test
  fun `delete recipe - happy path`() = withApp {
    val client = newAuthenticatedClient()
    val created = createRecipe(client)
    assertEquals(
        HttpStatusCode.NoContent,
        client.delete("/api/v1/recipes/${created.id}").status,
    )
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
    assertEquals(
        HttpStatusCode.NoContent,
        client.delete("/api/v1/recipes/${created.id}").status,
    )
  }

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
            .get("/api/v1/recipes?brew_method=AEROPRESS")
            .body<PageResponseDto<RecipeResponseDto>>()
    assertTrue(page.items.isNotEmpty())
    assertTrue(page.items.all { it.brewMethod == BrewMethodDto.AEROPRESS })
  }

  @Test
  fun `list recipes - filter by roast level`() = withApp {
    val client = newAuthenticatedClient()
    createRecipe(client, samplePayload().copy(roastLevel = RoastLevelDto.DARK))

    val page =
        client.get("/api/v1/recipes?roast_level=DARK").body<PageResponseDto<RecipeResponseDto>>()
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

  @Test
  fun `create recipe with image`() = withApp {
    val client = newAuthenticatedClient()
    val imageId = uploadImage(client)
    val recipe = createRecipe(client, samplePayload().copy(imageId = imageId))
    assertEquals(imageId, recipe.imageId)
    assertEquals(HttpStatusCode.OK, client.get("/api/v1/uploads/images/$imageId").status)
  }

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

  @Test
  fun `recipe note - author sees own note`() = withApp {
    val client = newAuthenticatedClient()
    val note = "my private note"
    val recipe = createRecipe(client, samplePayload().copy(note = note))
    val fetched = client.get("/api/v1/recipes/${recipe.id}").body<RecipeResponseDto>()
    assertEquals(note, fetched.note)
  }

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
