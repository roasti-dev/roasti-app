package dev.roasti.features.recipes

import dev.roasti.FIREBASE_AUTH
import dev.roasti.FirebasePrincipal
import dev.roasti.common.api.ApiError
import dev.roasti.common.api.ApiErrorCode
import dev.roasti.common.api.respondError
import dev.roasti.common.api.toDto
import dev.roasti.common.domain.Page
import dev.roasti.common.domain.toId
import dev.roasti.core.network.PageResponseDto
import dev.roasti.core.network.PaginationResponseDto
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.feature.likes.data.RecipeLikeDto
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeRequestDto
import dev.roasti.feature.recipe.data.remote.model.response.AuthorResponseDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeOriginResponseDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeStepResponseDto
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.features.comments.CommentId
import dev.roasti.features.comments.CommentThread
import dev.roasti.features.comments.toDto
import dev.roasti.features.comments.toHttp
import dev.roasti.features.likes.LikeInfo
import dev.roasti.features.users.model.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.koin.ktor.ext.inject

@OptIn(ExperimentalUuidApi::class)
fun Route.recipeRoutes() {
  val recipeService by inject<RecipeService>()

  route("/recipes") {
    get {
      val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
      val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
      val authorId =
          call.queryParameters["author_id"]?.let {
            it.toId(::UserId) ?: return@get call.respond(HttpStatusCode.BadRequest)
          }
      val brewMethod =
          call.request.queryParameters["brew_method"]?.let { parseBrewMethodDto(it)?.toDomain() }
      val difficulty =
          call.request.queryParameters["difficulty"]?.let { parseDifficultyDto(it)?.toDomain() }
      val roastLevel =
          call.request.queryParameters["roast_level"]?.let { parseRoastLevelDto(it)?.toDomain() }
      val userId = call.principal<FirebasePrincipal>()?.id
      val recipesPage =
          recipeService.list(page, limit, authorId, userId, brewMethod, difficulty, roastLevel)
      call.respond(recipesPage.toDto())
    }

    get("/{id}") {
      val id =
          call.pathParameters["id"]?.toId(::RecipeId)
              ?: return@get call.respond(HttpStatusCode.BadRequest)
      val userId = call.principal<FirebasePrincipal>()?.id
      recipeService
          .getById(id, userId)
          .fold(
              ifLeft = { call.respondError(it, GetRecipeError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }

    get("/{id}/comments") {
      val id =
          call.pathParameters["id"]?.toId(::RecipeId)
              ?: return@get call.respond(HttpStatusCode.BadRequest)
      val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
      val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
      recipeService
          .listComments(id, page, limit)
          .fold(
              ifLeft = { call.respondError(it, ListRecipeCommentsError::toHttp) },
              ifRight = { result -> call.respond(result.toDto { it.toDto() }) },
          )
    }

    authenticate(FIREBASE_AUTH) {
      post {
        val userId = call.principal<FirebasePrincipal>()!!.id
        val body = call.receive<CreateRecipeRequestDto>()
        recipeService
            .create(userId, body.toInput())
            .fold(
                ifLeft = { call.respondError(it, CreateRecipeError::toHttp) },
                ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
            )
      }

      put("/{id}") {
        val id =
            call.pathParameters["id"]?.toId(::RecipeId)
                ?: return@put call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<FirebasePrincipal>()!!.id
        val body = call.receive<CreateRecipeRequestDto>()
        recipeService
            .update(userId, id, body.toInput())
            .fold(
                ifLeft = { call.respondError(it, UpdateRecipeError::toHttp) },
                ifRight = { call.respond(it.toDto()) },
            )
      }

      delete("/{id}") {
        val id =
            call.pathParameters["id"]?.toId(::RecipeId)
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<FirebasePrincipal>()!!.id
        recipeService
            .delete(userId, id)
            .fold(
                ifLeft = { call.respond(HttpStatusCode.NoContent) },
                ifRight = { call.respond(HttpStatusCode.NoContent) },
            )
      }

      post("/{id}/like") {
        val id =
            call.pathParameters["id"]?.toId(::RecipeId)
                ?: return@post call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<FirebasePrincipal>()!!.id
        recipeService
            .toggleLike(userId, id)
            .fold(
                ifLeft = { call.respondError(it, ToggleLikeError::toHttp) },
                ifRight = { call.respond(it.toDto()) },
            )
      }

      post("/{id}/comments") {
        val id =
            call.pathParameters["id"]?.toId(::RecipeId)
                ?: return@post call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<FirebasePrincipal>()!!.id
        val body = call.receive<CreateCommentRequestDto>()
        val parentId = body.parentId?.let { CommentId(Uuid.parse(it)) }
        recipeService
            .createComment(userId, id, body.text, parentId)
            .fold(
                ifLeft = { call.respondError(it, CreateRecipeCommentError::toHttp) },
                ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
            )
      }

      post("/{id}/clone") {
        val id =
            call.pathParameters["id"]?.toId(::RecipeId)
                ?: return@post call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<FirebasePrincipal>()!!.id
        recipeService
            .clone(userId, id)
            .fold(
                ifLeft = { call.respondError(it, CloneRecipeError::toHttp) },
                ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
            )
      }
    }
  }
}

private fun LikeInfo.toDto() = RecipeLikeDto(isLiked = isLiked, likesCount = count)

private fun CreateRecipeRequestDto.toInput() =
    CreateRecipeInput(
        title = title,
        description = description,
        note = note,
        imageId = imageId,
        brewMethod = brewMethod.toDomain(),
        difficulty = difficulty.toDomain(),
        roastLevel = roastLevel.toDomain(),
        beans = beans,
        public = true,
        steps =
            steps.map { step ->
              CreateBrewStepInput(
                  title = step.title,
                  description = step.description,
                  order = step.order,
                  durationSeconds = step.durationSeconds,
                  imageId = step.imageId,
              )
            },
    )

@OptIn(ExperimentalUuidApi::class)
private fun Recipe.toDto() =
    RecipeResponseDto(
        id = id.value.toString(),
        authorId = author.id.value.toString(),
        author =
            AuthorResponseDto(
                id = author.id.value.toString(),
                username = author.username,
                avatarId = author.avatarId,
            ),
        title = title,
        description = description,
        note = note,
        imageId = imageId,
        brewMethod = brewMethod.toDto(),
        difficulty = difficulty.toDto(),
        roastLevel = roastLevel.toDto(),
        beans = beans,
        steps = steps.map { it.toDto() },
        isLiked = isLiked,
        likesCount = likesCount,
        origin = origin?.toDto(),
        isPublic = public,
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt.toEpochMilliseconds()),
    )

@OptIn(ExperimentalUuidApi::class)
private fun Page<Recipe>.toDto() =
    PageResponseDto(
        items = items.map { it.toDto() },
        pagination =
            PaginationResponseDto(
                currentPage = currentPage,
                itemsCount = itemsCount,
                lastPage = lastPage,
                nextPage = nextPage,
            ),
    )

private fun BrewStep.toDto() =
    RecipeStepResponseDto(
        order = order,
        title = title,
        description = description ?: "",
        durationSeconds = durationSeconds,
        imageId = imageId,
    )

@OptIn(ExperimentalUuidApi::class)
private fun RecipeOriginInfo.toDto() =
    RecipeOriginResponseDto(
        author =
            AuthorResponseDto(
                id = author.id.value.toString(),
                username = author.username,
                avatarId = author.avatarId,
            ),
        recipeId = recipeId.value.toString(),
    )

private fun BrewMethod.toDto() =
    when (this) {
      BrewMethod.V60 -> BrewMethodDto.V60
      BrewMethod.FrenchPress -> BrewMethodDto.FRENCH_PRESS
      BrewMethod.Aeropress -> BrewMethodDto.AEROPRESS
      BrewMethod.Chemex -> BrewMethodDto.CHEMEX
      BrewMethod.ColdBrew -> BrewMethodDto.COLD_BREW
      BrewMethod.EspressoMachine -> BrewMethodDto.EXPRESSO_MACHINE
      BrewMethod.MokaPot -> BrewMethodDto.MOKA_POT
      BrewMethod.NONE -> BrewMethodDto.NONE
    }

private fun Difficulty.toDto() =
    when (this) {
      Difficulty.Easy -> DifficultyDto.EASY
      Difficulty.Medium -> DifficultyDto.MEDIUM
      Difficulty.Hard -> DifficultyDto.HARD
    }

private fun RoastLevel.toDto() =
    when (this) {
      RoastLevel.Light -> RoastLevelDto.LIGHT
      RoastLevel.MediumLight -> RoastLevelDto.MEDIUM_LIGHT
      RoastLevel.Medium -> RoastLevelDto.MEDIUM
      RoastLevel.MediumDark -> RoastLevelDto.MEDIUM_DARK
      RoastLevel.Dark -> RoastLevelDto.DARK
      RoastLevel.NONE -> RoastLevelDto.NONE
    }

private fun BrewMethodDto?.toDomain() =
    when (this) {
      BrewMethodDto.V60 -> BrewMethod.V60
      BrewMethodDto.FRENCH_PRESS -> BrewMethod.FrenchPress
      BrewMethodDto.AEROPRESS -> BrewMethod.Aeropress
      BrewMethodDto.CHEMEX -> BrewMethod.Chemex
      BrewMethodDto.COLD_BREW -> BrewMethod.ColdBrew
      BrewMethodDto.EXPRESSO_MACHINE -> BrewMethod.EspressoMachine
      BrewMethodDto.MOKA_POT -> BrewMethod.MokaPot
      BrewMethodDto.NONE,
      null -> BrewMethod.NONE
    }

private fun DifficultyDto.toDomain() =
    when (this) {
      DifficultyDto.EASY -> Difficulty.Easy
      DifficultyDto.MEDIUM -> Difficulty.Medium
      DifficultyDto.HARD -> Difficulty.Hard
    }

private fun RoastLevelDto?.toDomain() =
    when (this) {
      RoastLevelDto.LIGHT -> RoastLevel.Light
      RoastLevelDto.MEDIUM_LIGHT -> RoastLevel.MediumLight
      RoastLevelDto.MEDIUM -> RoastLevel.Medium
      RoastLevelDto.MEDIUM_DARK -> RoastLevel.MediumDark
      RoastLevelDto.DARK -> RoastLevel.Dark
      RoastLevelDto.NONE,
      null -> RoastLevel.NONE
    }

private fun parseBrewMethodDto(value: String) =
    runCatching { BrewMethodDto.valueOf(value.uppercase()) }.getOrNull()

private fun parseDifficultyDto(value: String) =
    runCatching { DifficultyDto.valueOf(value.uppercase()) }.getOrNull()

private fun parseRoastLevelDto(value: String) =
    runCatching { RoastLevelDto.valueOf(value.uppercase()) }.getOrNull()

private fun CommentThread.toDto() =
    root.toDto().let { r ->
      CommentThreadResponseDto(
          id = r.id,
          isDeleted = r.isDeleted,
          author = r.author,
          text = r.text,
          parentId = r.parentId,
          replies = replies.map { it.toDto() },
          createdAt = r.createdAt,
          updatedAt = r.updatedAt,
      )
    }

object ApiErrors {
  val NotFound =
      HttpStatusCode.NotFound to ApiError(ApiErrorCode.RECIPE_NOT_FOUND, "Recipe not found")
  val Forbidden = HttpStatusCode.Forbidden to ApiError(ApiErrorCode.FORBIDDEN, "Forbidden")
}

private fun GetRecipeError.toHttp() =
    when (this) {
      GetRecipeError.NotFound -> ApiErrors.NotFound
      GetRecipeError.Forbidden -> ApiErrors.NotFound
    }

private fun CreateRecipeError.toHttp() =
    when (this) {
      is CreateRecipeError.InvalidInput ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(ApiErrorCode.INVALID_INPUT, error.message())
    }

private fun UpdateRecipeError.toHttp() =
    when (this) {
      UpdateRecipeError.NotFound -> ApiErrors.NotFound
      UpdateRecipeError.Forbidden -> ApiErrors.Forbidden

      is UpdateRecipeError.InvalidInput ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(ApiErrorCode.INVALID_INPUT, error.message())
    }

private fun ToggleLikeError.toHttp() =
    when (this) {
      ToggleLikeError.RecipeNotFound -> ApiErrors.NotFound
    }

private fun ListRecipeCommentsError.toHttp() =
    when (this) {
      ListRecipeCommentsError.RecipeNotFound -> ApiErrors.NotFound
    }

private fun CreateRecipeCommentError.toHttp() =
    when (this) {
      CreateRecipeCommentError.RecipeNotFound -> ApiErrors.NotFound
      is CreateRecipeCommentError.CommentError -> error.toHttp()
    }

private fun CloneRecipeError.toHttp() =
    when (this) {
      CloneRecipeError.Forbidden -> ApiErrors.NotFound
      CloneRecipeError.NotFound -> ApiErrors.NotFound
    }

private fun RecipeValidationError.message() =
    when (this) {
      RecipeValidationError.DescriptionEmpty -> "The description cannot be empty"
      RecipeValidationError.TitleEmpty -> "The header cannot be empty"
    }
