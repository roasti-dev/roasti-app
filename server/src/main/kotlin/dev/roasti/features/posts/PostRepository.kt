package dev.roasti.features.posts

import kotlinx.coroutines.Dispatchers
import dev.roasti.common.domain.pageOffset
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import dev.roasti.features.recipes.RecipeId
import dev.roasti.features.users.UserTable
import dev.roasti.features.users.UserId
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class PostInput(
    val title: String?,
    val text: String?,
    val images: List<String>,
    val recipeId:  RecipeId?,
)

interface PostRepository {
    suspend fun findById(id: PostId): PostRow?
    suspend fun list(page: Int, limit: Int, authorId: UserId?): Pair<List<PostRow>, Int>
    suspend fun create(authorId: UserId, input: PostInput): PostRow
    suspend fun update(id: PostId, input: PostInput): PostRow?
    suspend fun delete(id: PostId)
}

@OptIn(ExperimentalUuidApi::class)
class PostRepositoryImpl : PostRepository {

    override suspend fun findById(id: PostId): PostRow? = withContext(Dispatchers.IO) {
        transaction {
            PostTable.innerJoin(UserTable)
                .selectAll()
                .where { PostTable.id eq id.value }
                .singleOrNull()
                ?.toPostRow()
        }
    }

    override suspend fun list(page: Int, limit: Int, authorId: UserId?): Pair<List<PostRow>, Int> =
        withContext(Dispatchers.IO) {
            transaction {
                val query = PostTable.innerJoin(UserTable).selectAll()
                if (authorId != null) query.where { PostTable.authorId eq authorId.value }

                val total = query.count().toInt()
                val offset = pageOffset(page, limit)

                val items = PostTable.innerJoin(UserTable)
                    .selectAll()
                    .apply { if (authorId != null) where { PostTable.authorId eq authorId.value } }
                    .orderBy(PostTable.createdAt, SortOrder.DESC)
                    .limit(limit)
                    .offset(offset.toLong())
                    .map { it.toPostRow() }

                items to total
            }
        }

    override suspend fun create(authorId: UserId, input: PostInput): PostRow = withContext(Dispatchers.IO) {
        val id = Uuid.random()
        val now = Clock.System.now()
        transaction {
            PostTable.insert {
                it[PostTable.id] = org.jetbrains.exposed.v1.core.dao.id.EntityID(id, PostTable)
                it[PostTable.authorId] = authorId.value
                it[PostTable.title] = input.title
                it[PostTable.text] = input.text
                it[PostTable.images] = input.images
                it[PostTable.recipeId] = input.recipeId?.value
                it[PostTable.createdAt] = now
                it[PostTable.updatedAt] = now
            }
        }
        findById(PostId(id))!!
    }

    override suspend fun update(id: PostId, input: PostInput): PostRow? = withContext(Dispatchers.IO) {
        transaction {
            PostTable.update({ PostTable.id eq id.value }) {
                it[title] = input.title
                it[text] = input.text
                it[images] = input.images
                it[recipeId] = input.recipeId?.value
                it[updatedAt] = Clock.System.now()
            }
        }
        findById(id)
    }

    override suspend fun delete(id: PostId): Unit = withContext(Dispatchers.IO) {
        transaction {
            PostTable.deleteWhere { PostTable.id eq id.value }
        }
    }
}
