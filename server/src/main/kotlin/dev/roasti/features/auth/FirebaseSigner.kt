package dev.roasti.features.auth

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class SignInResult(val idToken: String, val refreshToken: String)

interface FirebaseSigner {
    suspend fun signInWithPassword(email: String, password: String): SignInResult
    suspend fun refreshToken(refreshToken: String): SignInResult
}

private val json = Json { ignoreUnknownKeys = true }

class FirebaseSignerImpl(
    private val apiKey: String,
    private val identityBaseUrl: String = "https://identitytoolkit.googleapis.com/v1/accounts",
    private val tokenBaseUrl: String = "https://securetoken.googleapis.com/v1/token",
) : FirebaseSigner {
    private val client = HttpClient.newHttpClient()

    override suspend fun signInWithPassword(email: String, password: String): SignInResult = withContext(Dispatchers.IO) {
        val body = """{"email":"$email","password":"$password","returnSecureToken":true}"""
        val response = post("$identityBaseUrl:signInWithPassword?key=$apiKey", body)

        if (response.statusCode() != HttpStatusCode.OK.value) {
            val err = parseError(response.body())
            throw when (err) {
                "INVALID_PASSWORD", "EMAIL_NOT_FOUND" -> AuthException.InvalidCredentials
                "USER_DISABLED" -> AuthException.UserDisabled
                else -> RuntimeException("Firebase error: $err")
            }
        }

        val result = json.decodeFromString<SignInResponse>(response.body())
        SignInResult(idToken = result.idToken, refreshToken = result.refreshToken)
    }

    override suspend fun refreshToken(refreshToken: String): SignInResult = withContext(Dispatchers.IO) {
        val body = """{"grant_type":"refresh_token","refresh_token":"$refreshToken"}"""
        val response = post("$tokenBaseUrl?key=$apiKey", body)

        if (response.statusCode() != HttpStatusCode.OK.value) {
            val err = parseError(response.body())
            throw when (err) {
                "TOKEN_EXPIRED", "INVALID_REFRESH_TOKEN", "TOKEN_REVOKED" -> AuthException.InvalidRefreshToken
                "USER_DISABLED" -> AuthException.UserDisabled
                "USER_NOT_FOUND" -> AuthException.UserNotFound
                else -> RuntimeException("Firebase error: $err")
            }
        }

        val result = json.decodeFromString<RefreshResponse>(response.body())
        SignInResult(idToken = result.idToken, refreshToken = result.refreshToken)
    }

    private fun post(url: String, body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun parseError(body: String): String = runCatching {
        json.decodeFromString<FirebaseErrorResponse>(body).error.message
    }.getOrElse { "UNKNOWN" }
}

@Serializable
private data class SignInResponse(
    @SerialName("idToken") val idToken: String,
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
private data class RefreshResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
private data class FirebaseErrorResponse(
    val error: FirebaseError,
)

@Serializable
private data class FirebaseError(
    val message: String,
)
