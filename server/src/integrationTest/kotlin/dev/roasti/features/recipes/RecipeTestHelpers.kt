package dev.roasti.features.recipes

import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeRequestDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeStepRequestDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlin.test.assertEquals

fun samplePayload(title: String = "Test Recipe") =
    CreateRecipeRequestDto(
        title = title,
        description = "Test description",
        brewMethod = BrewMethodDto.V60,
        difficulty = DifficultyDto.EASY,
        roastLevel = RoastLevelDto.LIGHT,
        steps = listOf(CreateRecipeStepRequestDto(order = 1, title = "Boil water")),
    )

suspend fun ApplicationTestBuilder.createRecipe(
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

suspend fun ApplicationTestBuilder.toggleLike(client: HttpClient, recipeId: String) {
  assertEquals(HttpStatusCode.OK, client.post("/api/v1/recipes/$recipeId/like").status)
}
