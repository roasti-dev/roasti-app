package dev.roasti.features.recipes

import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.features.users.UserTable
import dev.roasti.features.users.model.UserPreview
import dev.roasti.features.users.toUserPreview
import kotlin.uuid.ExperimentalUuidApi
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp

object RecipeTable : UuidTable("recipes") {
  @OptIn(ExperimentalUuidApi::class)
  val authorId = reference("author_id", UserTable, onDelete = ReferenceOption.CASCADE)
  val title = varchar("title", 255)
  val description = text("description")
  val note = text("note").nullable()
  val imageId = varchar("image_id", 255).nullable()
  val brewMethod = enumerationByName<BrewMethod>("brew_method", 50)
  val difficulty = enumerationByName<Difficulty>("difficulty", 50)
  val roastLevel = enumerationByName<RoastLevel>("roast_level", 50)
  val beans = varchar("beans", 255).nullable()
  val public = bool("public").default(true)
  @OptIn(ExperimentalUuidApi::class)
  val originRecipeId =
      reference("origin_recipe_id", RecipeTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")
}

object BrewStepTable : IntIdTable("brew_steps") {
  @OptIn(ExperimentalUuidApi::class)
  val recipeId = reference("recipe_id", RecipeTable.id, onDelete = ReferenceOption.CASCADE)
  val title = varchar("title", 255)
  val description = text("description").nullable()
  val order = integer("order")
  val durationSeconds = integer("duration_seconds").nullable()
  val imageId = varchar("image_id", 255).nullable()
}

internal val OriginRecipes = RecipeTable.alias("origin_recipes")
internal val OriginAuthors = UserTable.alias("origin_authors")

@OptIn(ExperimentalUuidApi::class)
data class RecipeRow(
    val id: RecipeId,
    val author: UserPreview,
    val title: String,
    val description: String,
    val note: String?,
    val imageId: String?,
    val brewMethod: BrewMethod,
    val difficulty: Difficulty,
    val roastLevel: RoastLevel,
    val beans: String?,
    val public: Boolean,
    val origin: RecipeOriginInfo?,
    val createdAt: kotlin.time.Instant,
    val updatedAt: kotlin.time.Instant,
)

@OptIn(ExperimentalUuidApi::class)
private fun ResultRow.toOrigin(): RecipeOriginInfo? {
  val originRecipeId = this[RecipeTable.originRecipeId]?.value?.let { RecipeId(it) } ?: return null
  return RecipeOriginInfo(recipeId = originRecipeId, author = toUserPreview(OriginAuthors))
}

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toRecipeRow() =
    RecipeRow(
        id = RecipeId(this[RecipeTable.id].value),
        author = toUserPreview(),
        title = this[RecipeTable.title],
        description = this[RecipeTable.description],
        note = this[RecipeTable.note],
        imageId = this[RecipeTable.imageId],
        brewMethod = this[RecipeTable.brewMethod],
        difficulty = this[RecipeTable.difficulty],
        roastLevel = this[RecipeTable.roastLevel],
        beans = this[RecipeTable.beans],
        public = this[RecipeTable.public],
        origin = toOrigin(),
        createdAt = this[RecipeTable.createdAt],
        updatedAt = this[RecipeTable.updatedAt],
    )

fun ResultRow.toBrewStep() =
    BrewStep(
        id = this[BrewStepTable.id].value,
        title = this[BrewStepTable.title],
        description = this[BrewStepTable.description],
        order = this[BrewStepTable.order],
        durationSeconds = this[BrewStepTable.durationSeconds],
        imageId = this[BrewStepTable.imageId],
    )
