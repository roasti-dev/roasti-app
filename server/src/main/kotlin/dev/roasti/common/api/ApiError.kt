package dev.roasti.common.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

interface ApiError {
    val code: Enum<*>
    val message: String
}

suspend inline fun <reified E : ApiError> ApplicationCall.respondError(
    status: HttpStatusCode,
    error: E,
) = respond(status, error)
