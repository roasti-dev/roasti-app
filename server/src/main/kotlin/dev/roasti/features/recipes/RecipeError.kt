package dev.roasti.features.recipes

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.common.api.ApiError

enum class RecipeErrorCode { NOT_FOUND, FORBIDDEN, INVALID_INPUT }

@Serializable
data class RecipeError(
    @SerialName("code") override val code: RecipeErrorCode,
    @SerialName("message") override val message: String,
) : ApiError

fun RecipeErrorCode.toError() = when (this) {
    RecipeErrorCode.NOT_FOUND -> RecipeError(this, "Recipe not found")
    RecipeErrorCode.FORBIDDEN -> RecipeError(this, "Forbidden")
    RecipeErrorCode.INVALID_INPUT -> RecipeError(this, "Invalid input")
}

fun RecipeErrorCode.toHttpStatus() = when (this) {
    RecipeErrorCode.NOT_FOUND -> HttpStatusCode.NotFound
    RecipeErrorCode.FORBIDDEN -> HttpStatusCode.Forbidden
    RecipeErrorCode.INVALID_INPUT -> HttpStatusCode.UnprocessableEntity
}
