package dev.roasti.features.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import dev.roasti.FIREBASE_AUTH
import dev.roasti.feature.auth.data.network.model.request.LoginRequestDto
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.common.api.respondError

@Serializable
data class RefreshRequestBody(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class LogoutRequestBody(@SerialName("refresh_token") val refreshToken: String)

fun Route.authRoutes() {
    val authService by inject<AuthService>()

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequestDto>()
            authService.register(request).fold(
                ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                ifRight = { call.respond(HttpStatusCode.Created, it) },
            )
        }

        post("/login") {
            val request = call.receive<LoginRequestDto>()
            authService.login(request.username, request.password).fold(
                ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                ifRight = { call.respond(it) },
            )
        }

        post("/refresh") {
            val body = call.receive<RefreshRequestBody>()
            authService.refresh(body.refreshToken).fold(
                ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                ifRight = { call.respond(it) },
            )
        }

        authenticate(FIREBASE_AUTH) {
            post("/logout") {
                val body = call.receive<LogoutRequestBody>()
                authService.logout(body.refreshToken)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
