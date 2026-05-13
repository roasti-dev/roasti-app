package dev.roasti.common.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

enum class ApiErrorCode {
    RECIPE_NOT_FOUND,
    POST_NOT_FOUND,
    PARENT_COMMENT_NOT_FOUND,
    COMMENT_NOT_FOUND,
    USER_NOT_FOUND,
    FORBIDDEN,
    // TODO: use specific codes
    INVALID_INPUT,

    // auth
    INVALID_CREDENTIALS,
    USERNAME_TAKEN,
    EMAIL_TAKEN,
    USER_DISABLED,
    INVALID_REFRESH_TOKEN
}

@Serializable
data class  ApiError(
    val code: ApiErrorCode,
    val message: String,
)

suspend fun <E> ApplicationCall.respondError(
    error: E,
    mapper: (E) -> Pair<HttpStatusCode, ApiError>
) {
    val (status, response) = mapper(error)
    respond(status, response)
}