package dev.roasti.features.users

import dev.roasti.FIREBASE_AUTH
import dev.roasti.FirebasePrincipal
import dev.roasti.common.api.ApiError
import dev.roasti.common.api.ApiErrorCode
import dev.roasti.common.api.respondError
import dev.roasti.common.api.toHttp
import dev.roasti.feature.auth.data.network.model.request.UpdateProfileRequest
import dev.roasti.feature.auth.data.network.model.response.UserDto
import dev.roasti.features.users.model.User
import dev.roasti.features.users.usecase.CheckUsernameAvailability
import dev.roasti.features.users.usecase.GetCurrentUser
import dev.roasti.features.users.usecase.GetUserError
import dev.roasti.features.users.usecase.GetUserProfile
import dev.roasti.features.users.usecase.UpdateProfile
import dev.roasti.features.users.usecase.UpdateProfileError
import dev.roasti.features.users.usecase.UpdateProfileInput
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class UsernameAvailabilityResponse(@SerialName("available") val available: Boolean)

fun Route.userRoutes() {
  val getCurrentUser by inject<GetCurrentUser>()
  val getUserProfile by inject<GetUserProfile>()
  val updateProfile by inject<UpdateProfile>()
  val checkUsernameAvailability by inject<CheckUsernameAvailability>()

  route("/users") {
    authenticate(FIREBASE_AUTH) {
      route("me") {
        get {
          val userId =
              call.principal<FirebasePrincipal>()?.id
                  ?: return@get call.respond(HttpStatusCode.Unauthorized)
          getCurrentUser(userId)
              .fold(
                  ifLeft = { call.respondError(it, GetUserError::toHttp) },
                  ifRight = { call.respond(it.toDto()) },
              )
        }

        patch {
          val userId =
              call.principal<FirebasePrincipal>()?.id
                  ?: return@patch call.respond(HttpStatusCode.Unauthorized)
          // TODO: support explicit null to clear nullable fields (name, bio, avatarId)
          val body = call.receive<UpdateProfileRequest>()
          val input =
              UpdateProfileInput(
                  username = body.username,
                  name = body.name,
                  bio = body.bio,
                  avatarId = body.imageId,
              )
          updateProfile(userId, input)
              .fold(
                  ifLeft = { call.respondError(it, UpdateProfileError::toHttp) },
                  ifRight = { call.respond(it.toDto()) },
              )
        }
      }
    }

    get("/username-availability") {
      val username =
          call.request.queryParameters["username"]
              ?: return@get call.respond(
                  HttpStatusCode.BadRequest,
                  "username query param required",
              )
      call.respond(UsernameAvailabilityResponse(checkUsernameAvailability(username)))
    }

    get("/{username}") {
      val username = call.parameters["username"]!!
      // TODO: public profile should not expose email — use a separate DTO without email field
      getUserProfile(username)
          .fold(
              ifLeft = { call.respondError(it, GetUserError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }
  }
}

@OptIn(ExperimentalUuidApi::class)
fun User.toDto() =
    UserDto(
        id = id.value.toString(),
        email = email.value,
        username = username.value,
        name = name,
        avatarId = avatarId,
        bio = bio,
    )

private fun GetUserError.toHttp() =
    when (this) {
      GetUserError.NotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.USER_NOT_FOUND, "user not found")
    }

private fun UpdateProfileError.toHttp() =
    when (this) {
      is UpdateProfileError.InvalidInput -> errors.toHttp()

      UpdateProfileError.NotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.USER_NOT_FOUND, "user not found")

      UpdateProfileError.UsernameTaken ->
          HttpStatusCode.Conflict to ApiError(ApiErrorCode.USERNAME_TAKEN, "name is unavailable")

      UpdateProfileError.AvatarNotUploaded ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(ApiErrorCode.INVALID_INPUT, "The avatar image was not uploaded")
    }
