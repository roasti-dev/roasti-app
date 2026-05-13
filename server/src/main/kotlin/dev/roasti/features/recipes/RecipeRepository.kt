package dev.roasti.features.recipes

import dev.roasti.common.domain.pageOffset
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.features.users.UserTable
import dev.roasti.features.users.model.UserId
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class CreateRecipeInput(
    val title: String,
    val description: String,
    val note: String?,
    val imageId: String?,
    val brewMethod: BrewMethod,
    val difficulty: Difficulty,
    val roastLevel: RoastLevel,
    val beans: String?,
    val public: Boolean,
    val steps: List<CreateBrewStepInput>,
    val originRecipeId: RecipeId? = null,
)

data class CreateBrewStepInput(
    val title: String,
    val description: String?,
    val order: Int,
    val durationSeconds: Int?,
    val imageId: String?,
)

interface RecipeRepository {
  suspend fun findById(id: RecipeId): RecipeRow?

  suspend fun list(
      page: Int,
      limit: Int,
      authorId: UserId?,
      requestingUserId: UserId?,
      brewMethod: BrewMethod?,
      difficulty: Difficulty?,
      roastLevel: RoastLevel?,
  ): Pair<List<RecipeRow>, Int>

  suspend fun create(authorId: UserId, input: CreateRecipeInput): RecipeRow

  suspend fun update(id: RecipeId, input: CreateRecipeInput): Unit

  suspend fun delete(id: RecipeId)

  suspend fun getSteps(recipeId: RecipeId): List<BrewStep>

  suspend fun getStepsBatch(recipeIds: List<RecipeId>): Map<RecipeId, List<BrewStep>>
}

@OptIn(ExperimentalUuidApi::class)
class RecipeRepositoryImpl : RecipeRepository {

  override suspend fun findById(id: RecipeId): RecipeRow? =
      withContext(Dispatchers.IO) {
        transaction {
          recipeQuery().where { RecipeTable.id eq id.value }.singleOrNull()?.toRecipeRow()
        }
      }

  override suspend fun list(
      page: Int,
      limit: Int,
      authorId: UserId?,
      requestingUserId: UserId?,
      brewMethod: BrewMethod?,
      difficulty: Difficulty?,
      roastLevel: RoastLevel?,
  ): Pair<List<RecipeRow>, Int> =
      withContext(Dispatchers.IO) {
        transaction {
          var cond: org.jetbrains.exposed.v1.core.Op<Boolean> =
              if (requestingUserId != null) {
                (RecipeTable.public eq true) or (RecipeTable.authorId eq requestingUserId.value)
              } else {
                RecipeTable.public eq true
              }
          if (authorId != null) cond = cond and (RecipeTable.authorId eq authorId.value)
          if (brewMethod != null) cond = cond and (RecipeTable.brewMethod eq brewMethod)
          if (difficulty != null) cond = cond and (RecipeTable.difficulty eq difficulty)
          if (roastLevel != null) cond = cond and (RecipeTable.roastLevel eq roastLevel)

          val total = recipeQuery().where { cond }.count().toInt()
          val offset = pageOffset(page, limit)

          val items =
              recipeQuery()
                  .where { cond }
                  .orderBy(RecipeTable.createdAt, SortOrder.DESC)
                  .limit(limit)
                  .offset(offset.toLong())
                  .map { it.toRecipeRow() }

          items to total
        }
      }

  override suspend fun create(authorId: UserId, input: CreateRecipeInput): RecipeRow =
      withContext(Dispatchers.IO) {
        val id = Uuid.random()
        val now = Clock.System.now()
        transaction {
          RecipeTable.insert {
            it[RecipeTable.id] = org.jetbrains.exposed.v1.core.dao.id.EntityID(id, RecipeTable)
            it[RecipeTable.authorId] = authorId.value
            it[RecipeTable.title] = input.title
            it[RecipeTable.description] = input.description
            it[RecipeTable.note] = input.note
            it[RecipeTable.imageId] = input.imageId
            it[RecipeTable.brewMethod] = input.brewMethod
            it[RecipeTable.difficulty] = input.difficulty
            it[RecipeTable.roastLevel] = input.roastLevel
            it[RecipeTable.beans] = input.beans
            it[RecipeTable.public] = input.public
            it[RecipeTable.originRecipeId] =
                input.originRecipeId?.let {
                  org.jetbrains.exposed.v1.core.dao.id.EntityID(it.value, RecipeTable)
                }
            it[RecipeTable.createdAt] = now
            it[RecipeTable.updatedAt] = now
          }
          insertSteps(RecipeId(id), input.steps)
        }
        findById(RecipeId(id))!!
      }

  override suspend fun update(id: RecipeId, input: CreateRecipeInput) =
      withContext(Dispatchers.IO) {
        val now = Clock.System.now()
        transaction {
          RecipeTable.update({ RecipeTable.id eq id.value }) {
            it[title] = input.title
            it[description] = input.description
            it[note] = input.note
            it[imageId] = input.imageId
            it[brewMethod] = input.brewMethod
            it[difficulty] = input.difficulty
            it[roastLevel] = input.roastLevel
            it[beans] = input.beans
            it[public] = input.public
            it[updatedAt] = now
          }
          BrewStepTable.deleteWhere { BrewStepTable.recipeId eq id.value }
          insertSteps(id, input.steps)
        }
      }

  override suspend fun delete(id: RecipeId): Unit =
      withContext(Dispatchers.IO) {
        transaction { RecipeTable.deleteWhere { RecipeTable.id eq id.value } }
      }

  override suspend fun getSteps(recipeId: RecipeId): List<BrewStep> =
      withContext(Dispatchers.IO) {
        transaction {
          BrewStepTable.selectAll()
              .where { BrewStepTable.recipeId eq recipeId.value }
              .orderBy(BrewStepTable.order)
              .map { it.toBrewStep() }
        }
      }

  override suspend fun getStepsBatch(recipeIds: List<RecipeId>): Map<RecipeId, List<BrewStep>> =
      withContext(Dispatchers.IO) {
        if (recipeIds.isEmpty()) return@withContext emptyMap()
        val uuids = recipeIds.map { it.value }
        transaction {
              BrewStepTable.selectAll()
                  .where { BrewStepTable.recipeId inList uuids }
                  .orderBy(BrewStepTable.order)
                  .map { it[BrewStepTable.recipeId].value to it.toBrewStep() }
            }
            .groupBy({ RecipeId(it.first) }, { it.second })
      }

  private fun recipeQuery() =
      RecipeTable.innerJoin(UserTable)
          .join(
              OriginRecipes,
              JoinType.LEFT,
              RecipeTable.originRecipeId,
              OriginRecipes[RecipeTable.id],
          )
          .join(
              OriginAuthors,
              JoinType.LEFT,
              OriginRecipes[RecipeTable.authorId],
              OriginAuthors[UserTable.id],
          )
          .selectAll()

  private fun insertSteps(recipeId: RecipeId, steps: List<CreateBrewStepInput>) {
    steps.forEach { step ->
      BrewStepTable.insert {
        it[BrewStepTable.recipeId] =
            org.jetbrains.exposed.v1.core.dao.id.EntityID(recipeId.value, RecipeTable)
        it[BrewStepTable.title] = step.title
        it[BrewStepTable.description] = step.description
        it[BrewStepTable.order] = step.order
        it[BrewStepTable.durationSeconds] = step.durationSeconds
        it[BrewStepTable.imageId] = step.imageId
      }
    }
  }
}
