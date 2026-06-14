package dev.roasti

import dev.roasti.config.loadConfig
import dev.roasti.features.uploads.UploadService
import dev.roasti.plugins.configureAuthentication
import dev.roasti.plugins.configureCORS
import dev.roasti.plugins.configureDI
import dev.roasti.plugins.configureDatabases
import dev.roasti.plugins.configureLogging
import dev.roasti.plugins.configureRouting
import dev.roasti.plugins.configureSerialization
import dev.roasti.plugins.configureStatusPages
import dev.roasti.plugins.initFirebase
import io.ktor.server.application.Application
import io.ktor.server.application.log
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
  val config = loadConfig()
  configureDatabases(config.database)
  initFirebase(config.firebase)
  configureDI(config.firebase, config.uploads)
  configureAuthentication()
  configureLogging()
  configureSerialization()
  configureStatusPages()
  configureRouting()
  configureCORS()
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
