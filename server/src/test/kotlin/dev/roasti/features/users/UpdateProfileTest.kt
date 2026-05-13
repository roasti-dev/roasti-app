package dev.roasti.features.users

import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import dev.roasti.feature.auth.data.network.model.request.UpdateProfileRequest
import dev.roasti.jsonClient
import dev.roasti.newAuthenticatedClient
import dev.roasti.withApp
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateProfileTest {

    @Test
    fun `updates username`() = withApp {
        val client = newAuthenticatedClient()
        val newUsername = "upd_${UUID.randomUUID().toString().take(5)}"
        val updated = updateMe(client, UpdateProfileRequest(username = newUsername))
        assertEquals(newUsername, updated.username)
    }

    @Test
    fun `updates bio`() = withApp {
        val client = newAuthenticatedClient()
        val updated = updateMe(client, UpdateProfileRequest(bio = "my bio"))
        assertEquals("my bio", updated.bio)
    }

    @Test
    fun `updates name`() = withApp {
        val client = newAuthenticatedClient()
        val updated = updateMe(client, UpdateProfileRequest(name = "Display Name"))
        assertEquals("Display Name", updated.name)
    }

    @Test
    fun `partial update does not change other fields`() = withApp {
        val client = newAuthenticatedClient()
        val original = getMe(client)
        val updated = updateMe(client, UpdateProfileRequest(bio = "partial bio"))
        assertEquals(original.username, updated.username)
        assertEquals("partial bio", updated.bio)
    }

    @Test
    fun `duplicate username returns 409`() = withApp {
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
    fun `invalid username returns 422`() = withApp {
        val client = newAuthenticatedClient()
        val response = client.patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(username = "a"))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
    }

    @Test
    fun `unauthenticated returns 401`() = withApp {
        val response = jsonClient().patch("/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(bio = "bio"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
