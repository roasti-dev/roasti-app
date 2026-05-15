import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.ktor)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.spotless)
  application
}

application {
  mainClass.set("dev.roasti.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
  implementation(projects.shared)
  implementation(libs.koin.ktor)
  implementation(libs.logback)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.ktor.server.contentNegotiation)
  implementation(libs.ktor.server.resources)
  implementation(libs.ktor.server.statusPages)
  implementation(libs.ktor.server.callLogging)
  implementation(libs.ktor.server.callId)
  implementation(libs.arrow.core)
  implementation(libs.exposed.core)
  implementation(libs.exposed.jdbc)
  implementation(libs.exposed.kotlinDatetime)
  implementation(libs.h2)
  implementation(libs.firebase.admin)
  implementation(libs.ktor.server.auth)
  testImplementation(libs.kotlin.testJunit)
}

kotlin {
  jvmToolchain(21)
  compilerOptions {
    // TODO: Remove when kotlin.uuid.Uuid is no longer experimental.
    freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
  }
}

spotless {
  kotlin { ktfmt() }
  kotlinGradle {
    target("*.gradle.kts")
    ktfmt()
  }
}

val firebaseEmulatorHost = "localhost:9099"

val firebaseEmulatorEnv =
    mapOf(
        "FIREBASE_AUTH_EMULATOR_HOST" to firebaseEmulatorHost,
        "FIREBASE_API_KEY" to "test",
        "FIREBASE_PROJECT_ID" to "roasti-dev-project",
        "FIREBASE_IDENTITY_BASE_URL" to
            "http://$firebaseEmulatorHost/identitytoolkit.googleapis.com/v1/accounts",
        "FIREBASE_TOKEN_BASE_URL" to
            "http://$firebaseEmulatorHost/securetoken.googleapis.com/v1/token",
    )

tasks.register<Exec>("firebaseEmulator") {
  workingDir = rootProject.projectDir
  commandLine(
      "sh",
      "-c",
      "firebase emulators:start --only auth --project roasti-dev-project",
  )
}

tasks.named<JavaExec>("run") { environment(firebaseEmulatorEnv) }

@Suppress("UnstableApiUsage")
testing {
  suites {
    val test by getting(JvmTestSuite::class) { useJUnitJupiter() }

    register<JvmTestSuite>("integrationTest") {
      dependencies {
        implementation(project())
        implementation(projects.shared)
        implementation(libs.kotlin.testJunit)
        implementation(libs.ktor.server.testHost)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.kotlinx.json)
      }
      targets {
        all {
          testTask.configure {
            shouldRunAfter(test)
            environment(firebaseEmulatorEnv)
            testLogging {
              events(TestLogEvent.FAILED)

              exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

              showExceptions = true
              showCauses = true
              showStackTraces = true
            }
          }
        }
      }
    }
  }
}
