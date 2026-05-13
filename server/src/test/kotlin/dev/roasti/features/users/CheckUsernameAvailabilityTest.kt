package dev.roasti.features.users

import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckUsernameAvailabilityTest {

  @Test
  fun `available username returns true`() = withApp {
    val response =
        jsonClient()
            .get(
                "/api/v1/users/username-availability?username=free_${UUID.randomUUID().toString().replace("-", "").take(11)}"
            )
    assertEquals(HttpStatusCode.OK, response.status)
    assertTrue(response.body<UsernameAvailabilityResponse>().available)
  }

  @Test
  fun `taken username returns false`() = withApp {
    val client = newAuthenticatedClient()
    val takenUsername = getMe(client).username

    val response = jsonClient().get("/api/v1/users/username-availability?username=$takenUsername")
    assertEquals(HttpStatusCode.OK, response.status)
    assertFalse(response.body<UsernameAvailabilityResponse>().available)
  }

  @Test
  fun `invalid username returns false`() = withApp {
    val response = jsonClient().get("/api/v1/users/username-availability?username=a")
    assertEquals(HttpStatusCode.OK, response.status)
    assertFalse(response.body<UsernameAvailabilityResponse>().available)
  }
}
