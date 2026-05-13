package dev.roasti.features.users

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import dev.roasti.feature.auth.data.network.model.request.UpdateProfileRequest
import dev.roasti.feature.auth.data.network.model.response.UserDto
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserTest {

    private suspend fun ApplicationTestBuilder.getMe(client: HttpClient): UserDto =
        client.get("/api/v1/users/me").body()

    private suspend fun ApplicationTestBuilder.updateMe(
        client: HttpClient,
        body: UpdateProfileRequest,
    ): UserDto {
        val response = client.patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return response.body()
    }

    @Test
    fun `get me - returns current user`() = withApp {
        val client = newAuthenticatedClient()
        val response = client.get("/api/v1/users/me")
        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<UserDto>()
        assertNotNull(user.id)
        assertTrue(user.username.isNotEmpty())
        assertTrue(user.email.isNotEmpty())
    }

    @Test
    fun `get me - unauthenticated returns 401`() = withApp {
        assertEquals(HttpStatusCode.Unauthorized, jsonClient().get("/api/v1/users/me").status)
    }

    @Test
    fun `update me - updates username`() = withApp {
        val client = newAuthenticatedClient()
        val newUsername = "upd_${UUID.randomUUID().toString().take(5)}"
        val updated = updateMe(client, UpdateProfileRequest(username = newUsername))
        assertEquals(newUsername, updated.username)
    }

    @Test
    fun `update me - updates bio`() = withApp {
        val client = newAuthenticatedClient()
        val updated = updateMe(client, UpdateProfileRequest(bio = "my bio"))
        assertEquals("my bio", updated.bio)
    }

    @Test
    fun `update me - updates name`() = withApp {
        val client = newAuthenticatedClient()
        val updated = updateMe(client, UpdateProfileRequest(name = "Display Name"))
        assertEquals("Display Name", updated.name)
    }

    @Test
    fun `update me - partial update does not change other fields`() = withApp {
        val client = newAuthenticatedClient()
        val original = getMe(client)
        val updated = updateMe(client, UpdateProfileRequest(bio = "partial bio"))
        assertEquals(original.username, updated.username)
        assertEquals("partial bio", updated.bio)
    }

    @Test
    fun `update me - duplicate username returns 409`() = withApp {
        val c1 = newAuthenticatedClient()
        val c2 = newAuthenticatedClient()
        val takenUsername = getMe(c1).username

        val response = c2.patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(username = takenUsername))
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `update me - unauthenticated returns 401`() = withApp {
        val response = jsonClient().patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(bio = "bio"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `get user profile - returns profile by username`() = withApp {
        val client = newAuthenticatedClient()
        val me = getMe(client)

        val response = jsonClient().get("/api/v1/users/${me.username}")
        assertEquals(HttpStatusCode.OK, response.status)
        val profile = response.body<UserDto>()
        assertEquals(me.id, profile.id)
        assertEquals(me.username, profile.username)
    }

    @Test
    fun `get user profile - returns name when set`() = withApp {
        val client = newAuthenticatedClient()
        updateMe(client, UpdateProfileRequest(name = "Public Name"))
        val me = getMe(client)

        val profile = jsonClient().get("/api/v1/users/${me.username}").body<UserDto>()
        assertEquals("Public Name", profile.name)
    }

    @Test
    fun `get user profile - returns bio when set`() = withApp {
        val client = newAuthenticatedClient()
        updateMe(client, UpdateProfileRequest(bio = "my public bio"))
        val me = getMe(client)

        val profile = jsonClient().get("/api/v1/users/${me.username}").body<UserDto>()
        assertEquals("my public bio", profile.bio)
    }

    // TODO: add test "get user profile - does not expose email" once public profile DTO is introduced

    @Test
    fun `get user profile - not found returns 404`() = withApp {
        assertEquals(HttpStatusCode.NotFound, jsonClient().get("/api/v1/users/nonexistent_user").status)
    }

    @Test
    fun `username availability - available`() = withApp {
        val response = jsonClient().get("/api/v1/users/username-availability?username=never_taken_${UUID.randomUUID().toString().take(8)}")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.body<UsernameAvailabilityResponse>().available)
    }

    @Test
    fun `username availability - taken`() = withApp {
        val client = newAuthenticatedClient()
        val takenUsername = getMe(client).username

        val response = jsonClient().get("/api/v1/users/username-availability?username=$takenUsername")
        assertEquals(HttpStatusCode.OK, response.status)
        assertFalse(response.body<UsernameAvailabilityResponse>().available)
    }
}
