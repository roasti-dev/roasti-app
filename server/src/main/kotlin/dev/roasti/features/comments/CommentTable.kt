package dev.roasti.features.comments

import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import dev.roasti.features.users.UserTable
import kotlin.uuid.ExperimentalUuidApi

enum class CommentTargetType { POST, RECIPE }

@OptIn(ExperimentalUuidApi::class)
object CommentTable : UuidTable("comments") {
    val targetId = uuid("target_id")
    val targetType = enumerationByName<CommentTargetType>("target_type", 50)
    val authorId = reference("author_id", UserTable)
    val text = text("text")
    val parentId = uuid("parent_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
}
