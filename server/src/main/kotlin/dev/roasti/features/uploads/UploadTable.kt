package dev.roasti.features.uploads

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object UploadTable : UuidTable("uploads") {
  val contentType = varchar("content_type", 100)
  val createdAt = timestamp("created_at")
}
