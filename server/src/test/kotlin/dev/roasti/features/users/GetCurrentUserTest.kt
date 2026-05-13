package dev.roasti.features.users

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import dev.roasti.feature.auth.data.network.model.response.UserDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetCurrentUserTest {

    @Test
    fun `returns current user`() = withApp {
        val client = newAuthenticatedClient()
        val response = client.get("/api/v1/users/me")
        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<UserDto>()
        assertNotNull(user.id)
        assertTrue(user.username.isNotEmpty())
        assertTrue(user.email.isNotEmpty())
    }

    @Test
    fun `unauthenticated returns 401`() = withApp {
        assertEquals(HttpStatusCode.Unauthorized, jsonClient().get("/api/v1/users/me").status)
    }
}
