package dev.roasti.features.posts

import dev.roasti.feature.auth.data.network.model.response.UserDto
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeRequestDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeStepRequestDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlin.test.assertEquals

val defaultPostPayload = CreatePostRequestDto(title = "My first post", text = "My first post text")

suspend fun ApplicationTestBuilder.createPost(
    client: HttpClient,
    payload: CreatePostRequestDto = defaultPostPayload,
): PostResponseDto {
  val response =
      client.post("/api/v1/posts") {
        contentType(ContentType.Application.Json)
        setBody(payload)
      }
  assertEquals(HttpStatusCode.Created, response.status)
  return response.body()
}

suspend fun ApplicationTestBuilder.getMyId(client: HttpClient): String =
    client.get("/api/v1/users/me").body<UserDto>().id

suspend fun ApplicationTestBuilder.createRecipe(client: HttpClient): RecipeResponseDto {
  val response =
      client.post("/api/v1/recipes") {
        contentType(ContentType.Application.Json)
        setBody(
            CreateRecipeRequestDto(
                title = "Test Recipe",
                description = "Test description",
                brewMethod = BrewMethodDto.V60,
                difficulty = DifficultyDto.EASY,
                roastLevel = RoastLevelDto.LIGHT,
                steps = listOf(CreateRecipeStepRequestDto(order = 1, title = "Boil water")),
            )
        )
      }
  assertEquals(HttpStatusCode.Created, response.status)
  return response.body()
}

suspend fun ApplicationTestBuilder.deleteRecipe(client: HttpClient, recipeId: String) {
  assertEquals(HttpStatusCode.NoContent, client.delete("/api/v1/recipes/$recipeId").status)
}
