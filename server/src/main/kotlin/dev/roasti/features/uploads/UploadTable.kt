package dev.roasti.features.uploads

import dev.roasti.features.users.UserTable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object UploadTable : UuidTable("uploads") {
  val contentType = varchar("content_type", 100)
  val uploaderId = reference("uploader_id", UserTable)
  val confirmed = bool("confirmed").default(false)
  val createdAt = timestamp("created_at")
}
