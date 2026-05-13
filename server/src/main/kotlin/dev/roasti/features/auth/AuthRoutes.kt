package dev.roasti.features.auth

import dev.roasti.FIREBASE_AUTH
import dev.roasti.common.api.ApiError
import dev.roasti.common.api.ApiErrorCode
import dev.roasti.common.api.respondError
import dev.roasti.common.api.toHttp
import dev.roasti.feature.auth.data.network.model.request.LoginRequestDto
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.features.auth.usecase.Login
import dev.roasti.features.auth.usecase.LoginError
import dev.roasti.features.auth.usecase.Logout
import dev.roasti.features.auth.usecase.RefreshError
import dev.roasti.features.auth.usecase.RefreshToken
import dev.roasti.features.auth.usecase.Register
import dev.roasti.features.auth.usecase.RegisterError
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

@Serializable data class RefreshRequestBody(@SerialName("refresh_token") val refreshToken: String)

@Serializable data class LogoutRequestBody(@SerialName("refresh_token") val refreshToken: String)

fun Route.authRoutes() {
  val register by inject<Register>()
  val login by inject<Login>()
  val refreshToken by inject<RefreshToken>()
  val logout by inject<Logout>()

  route("/auth") {
    post("/register") {
      val request = call.receive<RegisterRequestDto>()
      register(request)
          .fold(
              ifLeft = { call.respondError(it, RegisterError::toHttp) },
              ifRight = { call.respond(HttpStatusCode.Created, it) },
          )
    }

    post("/login") {
      val request = call.receive<LoginRequestDto>()
      login(request.username, request.password)
          .fold(
              ifLeft = { call.respondError(it, LoginError::toHttp) },
              ifRight = { call.respond(it) },
          )
    }

    post("/refresh") {
      val body = call.receive<RefreshRequestBody>()
      refreshToken(body.refreshToken)
          .fold(
              ifLeft = { call.respondError(it, RefreshError::toHttp) },
              ifRight = { call.respond(it) },
          )
    }

    authenticate(FIREBASE_AUTH) {
      post("/logout") {
        val body = call.receive<LogoutRequestBody>()
        logout(body.refreshToken)
        call.respond(HttpStatusCode.NoContent)
      }
    }
  }
}

private fun RegisterError.toHttp() =
    when (this) {
      RegisterError.UsernameTaken ->
          HttpStatusCode.Conflict to
              ApiError(ApiErrorCode.USERNAME_TAKEN, "username is already taken")

      RegisterError.EmailTaken ->
          HttpStatusCode.Conflict to ApiError(ApiErrorCode.EMAIL_TAKEN, "email is already taken")

      is RegisterError.InvalidInput -> errors.toHttp()
    }

private fun LoginError.toHttp() =
    when (this) {
      LoginError.InvalidCredentials ->
          HttpStatusCode.Unauthorized to
              ApiError(ApiErrorCode.INVALID_CREDENTIALS, "invalid credentials")

      LoginError.UserDisabled ->
          HttpStatusCode.Forbidden to ApiError(ApiErrorCode.USER_DISABLED, "user is disabled")
    }

private fun RefreshError.toHttp() =
    when (this) {
      RefreshError.InvalidRefreshToken ->
          HttpStatusCode.Unauthorized to
              ApiError(ApiErrorCode.INVALID_REFRESH_TOKEN, "invalid refresh token")
    }
