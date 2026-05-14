package dev.roasti

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import dev.roasti.features.auth.FirebaseSigner
import dev.roasti.features.auth.FirebaseSignerImpl
import dev.roasti.features.auth.RevokedTokenRepository
import dev.roasti.features.auth.RevokedTokenRepositoryImpl
import dev.roasti.features.auth.RevokedTokenTable
import dev.roasti.features.auth.authRoutes
import dev.roasti.features.auth.usecase.Login
import dev.roasti.features.auth.usecase.Logout
import dev.roasti.features.auth.usecase.RefreshToken
import dev.roasti.features.auth.usecase.Register
import dev.roasti.features.comments.CommentRepository
import dev.roasti.features.comments.CommentRepositoryImpl
import dev.roasti.features.comments.CommentService
import dev.roasti.features.comments.CommentServiceImpl
import dev.roasti.features.comments.CommentTable
import dev.roasti.features.comments.commentRoutes
import dev.roasti.features.likes.LikeRepository
import dev.roasti.features.likes.LikeRepositoryImpl
import dev.roasti.features.likes.LikeService
import dev.roasti.features.likes.LikeServiceImpl
import dev.roasti.features.likes.LikeTable
import dev.roasti.features.posts.PostRepository
import dev.roasti.features.posts.PostRepositoryImpl
import dev.roasti.features.posts.PostService
import dev.roasti.features.posts.PostServiceImpl
import dev.roasti.features.posts.PostTable
import dev.roasti.features.posts.postRoutes
import dev.roasti.features.recipes.BrewStepTable
import dev.roasti.features.recipes.RecipeRepository
import dev.roasti.features.recipes.RecipeRepositoryImpl
import dev.roasti.features.recipes.RecipeService
import dev.roasti.features.recipes.RecipeServiceImpl
import dev.roasti.features.recipes.RecipeTable
import dev.roasti.features.recipes.recipeRoutes
import dev.roasti.features.uploads.FileStorage
import dev.roasti.features.uploads.LocalFileStorage
import dev.roasti.features.uploads.UploadRepository
import dev.roasti.features.uploads.UploadRepositoryImpl
import dev.roasti.features.uploads.UploadService
import dev.roasti.features.uploads.UploadServiceImpl
import dev.roasti.features.uploads.UploadTable
import dev.roasti.features.uploads.uploadRoutes
import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.UserRepositoryImpl
import dev.roasti.features.users.UserTable
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.usecase.CheckUsernameAvailability
import dev.roasti.features.users.usecase.GetCurrentUser
import dev.roasti.features.users.usecase.GetUserProfile
import dev.roasti.features.users.usecase.UpdateProfile
import dev.roasti.features.users.userRoutes
import dev.roasti.features.votes.VoteRepository
import dev.roasti.features.votes.VoteRepositoryImpl
import dev.roasti.features.votes.VoteService
import dev.roasti.features.votes.VoteServiceImpl
import dev.roasti.features.votes.VoteTable
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.resources.Resources
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.event.Level

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@OptIn(ExperimentalUuidApi::class)
fun Application.module() {
  val dbUrl = environment.config.property("database.url").getString()
  val dbDriver = environment.config.property("database.driver").getString()

  Database.connect(url = dbUrl, driver = dbDriver)
  transaction {
    SchemaUtils.create(
        UserTable,
        RevokedTokenTable,
        VoteTable,
        LikeTable,
        CommentTable,
        PostTable,
        RecipeTable,
        BrewStepTable,
        UploadTable,
    )
  }

  initFirebase()

  val uploadsDir =
      File(environment.config.propertyOrNull("uploads.dir")?.getString() ?: "./uploads")
  val firebaseApiKey = environment.config.property("firebase.apiKey").getString()
  val identityBaseUrl =
      environment.config.propertyOrNull("firebase.identityBaseUrl")?.getString()
          ?: "https://identitytoolkit.googleapis.com/v1/accounts"
  val tokenBaseUrl =
      environment.config.propertyOrNull("firebase.tokenBaseUrl")?.getString()
          ?: "https://securetoken.googleapis.com/v1/token"

  install(Koin) {
    modules(
        module {
          single<UserRepository> { UserRepositoryImpl() }
          single { GetCurrentUser(get()) }
          single { GetUserProfile(get()) }
          single { UpdateProfile(get(), get()) }
          single { CheckUsernameAvailability(get()) }
          single<FirebaseSigner> {
            FirebaseSignerImpl(firebaseApiKey, identityBaseUrl, tokenBaseUrl)
          }
          single<RevokedTokenRepository> { RevokedTokenRepositoryImpl() }
          single { FirebaseAuth.getInstance() }
          single<CommentRepository> { CommentRepositoryImpl() }
          single<CommentService> { CommentServiceImpl(get()) }
          single<PostRepository> { PostRepositoryImpl() }
          single<PostService> { PostServiceImpl(get(), get(), get(), get(), get()) }
          single<LikeRepository> { LikeRepositoryImpl() }
          single<LikeService> { LikeServiceImpl(get()) }
          single<VoteRepository> { VoteRepositoryImpl() }
          single<VoteService> { VoteServiceImpl(get()) }
          single<RecipeRepository> { RecipeRepositoryImpl() }
          single<RecipeService> { RecipeServiceImpl(get(), get(), get(), get()) }
          single<FileStorage> { LocalFileStorage(uploadsDir) }
          single<UploadRepository> { UploadRepositoryImpl() }
          single<UploadService> { UploadServiceImpl(get(), get()) }
          single { Register(get(), get(), get()) }
          single { Login(get(), get()) }
          single { RefreshToken(get(), get()) }
          single { Logout(get()) }
        }
    )
  }

  val firebaseAuth = FirebaseAuth.getInstance()
  install(Authentication) {
    bearer(FIREBASE_AUTH) {
      authenticate { credential ->
        try {
          val decoded = firebaseAuth.verifyIdToken(credential.token)
          FirebasePrincipal(UserId(Uuid.parse(decoded.claims["id"]!! as String)))
        } catch (_: FirebaseAuthException) {
          // TODO: handle exception
          null
        }
      }
    }
  }

  install(CallId) { generate { UUID.randomUUID().toString() } }
  install(CallLogging) {
    level = Level.INFO
    callIdMdc("requestId")
    format { call ->
      "${call.request.httpMethod.value} ${call.request.path()} -> ${call.response.status()}"
    }
  }
  install(Resources)
  install(ContentNegotiation) { json(kotlinx.serialization.json.Json { explicitNulls = false }) }
  install(StatusPages) {
    exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
      call.respond(HttpStatusCode.BadRequest)
    }
    exception<IllegalArgumentException> { call, cause ->
      call.respond(HttpStatusCode.UnprocessableEntity, cause.message ?: "validation error")
    }
    exception<IllegalStateException> { call, cause ->
      call.respond(HttpStatusCode.Conflict, cause.message ?: "conflict")
    }
    exception<Throwable> { call, cause ->
      call.application.log.error("Unhandled exception", cause)
      call.respond(HttpStatusCode.InternalServerError)
    }
  }
  routing {
    route("/api/v1") {
      authRoutes()
      userRoutes()
      postRoutes()
      recipeRoutes()
      commentRoutes()
      uploadRoutes()
    }
  }

  startUploadGcJob()
}

@OptIn(DelicateCoroutinesApi::class)
private fun Application.startUploadGcJob() {
  val uploadService by inject<UploadService>()
  GlobalScope.launch {
    while (true) {
      delay(1.hours)
      try {
        uploadService.cleanupExpired()
      } catch (e: Exception) {
        log.error("Upload GC job failed", e)
      }
    }
  }
}

private fun Application.initFirebase() {
  if (FirebaseApp.getApps().isNotEmpty()) return

  val credsBase64 = environment.config.propertyOrNull("firebase.credentialsBase64")?.getString()
  val isEmulator = System.getenv("FIREBASE_AUTH_EMULATOR_HOST") != null

  val credentials =
      when {
        !credsBase64.isNullOrBlank() ->
            GoogleCredentials.fromStream(
                ByteArrayInputStream(Base64.getDecoder().decode(credsBase64))
            )
        isEmulator -> GoogleCredentials.newBuilder().build()
        else -> GoogleCredentials.getApplicationDefault()
      }

  val projectId = environment.config.propertyOrNull("firebase.projectId")?.getString()

  FirebaseApp.initializeApp(
      FirebaseOptions.builder()
          .setCredentials(credentials)
          .apply { if (projectId != null) setProjectId(projectId) }
          .build()
  )
}
