package dev.roasti

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import kotlinx.serialization.json.Json
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import java.util.UUID

private val testJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

fun withApp(block: suspend ApplicationTestBuilder.() -> Unit) {
    val tempDir = Files.createTempDirectory("roasti-test-uploads")
    try {
        testApplication {
            environment {
                config = ApplicationConfig("application.conf")
                    .mergeWith(MapApplicationConfig("uploads.dir" to tempDir.toAbsolutePath().toString()))
            }
            block()
        }
    } finally {
        tempDir.toFile().deleteRecursively()
    }
}

fun ApplicationTestBuilder.jsonClient(token: String? = null): HttpClient = createClient {
    install(ContentNegotiation) { json(testJson) }
    if (token != null) defaultRequest { bearerAuth(token) }
}

suspend fun ApplicationTestBuilder.newAuthenticatedClient(): HttpClient {
    val email = "${UUID.randomUUID()}@test.com"
    val username = "user_${UUID.randomUUID().toString().take(8)}"

    val auth = jsonClient().post("/api/v1/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(RegisterRequestDto(email = email, password = "password123", username = username))
    }.body<AuthResponseDto>()

    return jsonClient(token = auth.accessToken)
}
