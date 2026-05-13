package dev.roasti.feature.recipe.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareDelete
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import dev.roasti.core.network.ApiRoutes
import dev.roasti.core.network.AuthorizedRequestExecutor
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeRequestDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.core.network.PageResponseDto

interface RecipesApiClient {
    suspend fun getRecipes(
        authorId: String? = null,
        query: String? = null,
        brewMethod: BrewMethodDto? = null,
        difficulty: DifficultyDto? = null,
        roastLevel: RoastLevelDto? = null,
        limit: Int = 50,
        page: Int = 1
    ): Result<PageResponseDto<RecipeResponseDto>>

    suspend fun getRecipe(id: String): Result<RecipeResponseDto>
    suspend fun addRecipe(recipe: CreateRecipeRequestDto): Result<RecipeResponseDto>

    suspend fun updateRecipe(id: String, recipe: CreateRecipeRequestDto): Result<RecipeResponseDto>

    suspend fun removeRecipe(id: String): Result<Unit>
}

class RecipesApiClientImpl(
    private val httpClient: HttpClient,
    private val authorizedRequestExecutor: AuthorizedRequestExecutor,
) : RecipesApiClient {

    override suspend fun getRecipes(
        authorId: String?,
        query: String?,
        brewMethod: BrewMethodDto?,
        difficulty: DifficultyDto?,
        roastLevel: RoastLevelDto?,
        limit: Int,
        page: Int
    ): Result<PageResponseDto<RecipeResponseDto>> = authorizedRequestExecutor.execute { _ ->
        httpClient.get(ApiRoutes.Recipes) {
            url {
                authorId?.let { parameters.append("author_id", it) }
                query?.takeIf { it.isNotBlank() }?.let { parameters.append("query", it) }
                brewMethod?.let { parameters.append("brew_method", getSerialName(brewMethod)) }
                difficulty?.let { parameters.append("difficulty", getSerialName(it)) }
                roastLevel?.let { parameters.append("roast_level", getSerialName(it)) }
                parameters.append("limit", limit.toString())
                parameters.append("page", page.toString())
            }
        }.body<PageResponseDto<RecipeResponseDto>>()
    }

    override suspend fun getRecipe(id: String): Result<RecipeResponseDto> =
        authorizedRequestExecutor.execute {
            httpClient.get(ApiRoutes.recipeById(id)).body<RecipeResponseDto>()
        }

    override suspend fun addRecipe(recipe: CreateRecipeRequestDto): Result<RecipeResponseDto> =
        authorizedRequestExecutor.execute { _ ->
            httpClient.post(ApiRoutes.Recipes) {
                setBody(recipe)
            }.body<RecipeResponseDto>()
        }

    override suspend fun updateRecipe(
        id: String, recipe: CreateRecipeRequestDto
    ): Result<RecipeResponseDto> = authorizedRequestExecutor.execute {
        httpClient.put(ApiRoutes.recipeById(id)) {
            setBody(recipe)
        }.body<RecipeResponseDto>()
    }

    override suspend fun removeRecipe(id: String): Result<Unit> =
        authorizedRequestExecutor.execute {
            val status = httpClient.delete(ApiRoutes.recipeById(id)).status

            return@execute if (status.isSuccess()) Unit else throw Throwable("error while deleting recipe")
        }
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Enum<T>> getSerialName(value: T): String {
    return serializer<T>().descriptor.getElementName(value.ordinal)
}
