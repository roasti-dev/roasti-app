package dev.roasti.features.users

import dev.roasti.feature.auth.data.network.model.request.UpdateProfileRequest
import dev.roasti.feature.auth.data.network.model.response.UserDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class GetUserProfileTest {

  @Test
  fun `returns profile by username`() = withApp {
    val client = newAuthenticatedClient()
    val me = getMe(client)

    val response = jsonClient().get("/api/v1/users/${me.username}")
    assertEquals(HttpStatusCode.OK, response.status)
    val profile = response.body<UserDto>()
    assertEquals(me.id, profile.id)
    assertEquals(me.username, profile.username)
  }

  @Test
  fun `returns name when set`() = withApp {
    val client = newAuthenticatedClient()
    updateMe(client, UpdateProfileRequest(name = "Public Name"))
    val me = getMe(client)

    val profile = jsonClient().get("/api/v1/users/${me.username}").body<UserDto>()
    assertEquals("Public Name", profile.name)
  }

  @Test
  fun `returns bio when set`() = withApp {
    val client = newAuthenticatedClient()
    updateMe(client, UpdateProfileRequest(bio = "my public bio"))
    val me = getMe(client)

    val profile = jsonClient().get("/api/v1/users/${me.username}").body<UserDto>()
    assertEquals("my public bio", profile.bio)
  }

  // TODO: add test "does not expose email" once public profile DTO is introduced

  @Test
  fun `not found returns 404`() = withApp {
    assertEquals(
        HttpStatusCode.NotFound,
        jsonClient().get("/api/v1/users/nonexistent_user").status,
    )
  }
}
