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
import dev.roasti.features.comments.CommentService
import dev.roasti.features.comments.CommentTarget
import dev.roasti.features.comments.CommentThread
import dev.roasti.features.comments.CreateCommentError
import dev.roasti.features.comments.toDto
import dev.roasti.features.comments.toHttp
import dev.roasti.features.likes.LikeInfo
import dev.roasti.features.likes.LikeService
import dev.roasti.features.likes.LikeTarget
import dev.roasti.features.likes.LikeTargetError
import dev.roasti.features.uploads.ImageId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlin.uuid.ExperimentalUuidApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalUuidApi::class)
fun Route.recipeRoutes() {
  val recipeService by inject<RecipeService>()
  val commentService by inject<CommentService>()
  val likeService by inject<LikeService>()

  get<Recipes> { res ->
    val userId = call.principal<FirebasePrincipal>()?.id
    val recipesPage =
        recipeService.list(
            res.page,
            res.limit,
            res.authorId,
            userId,
            res.brewMethod?.toDomain(),
            res.difficulty?.toDomain(),
            res.roastLevel?.toDomain(),
        )
    call.respond(recipesPage.toDto())
  }

  get<Recipes.ById> { res ->
    val userId = call.principal<FirebasePrincipal>()?.id
    recipeService
        .getById(res.id, userId)
        .fold(
            ifLeft = { call.respondError(it, GetRecipeError::toHttp) },
            ifRight = { call.respond(it.toDto()) },
        )
  }

  get<Recipes.ById.Comments> { res ->
    val result = commentService.list(CommentTarget.Recipe(res.parent.id), res.page, res.limit)
    call.respond(result.toDto { it.toDto() })
  }

  authenticate(FIREBASE_AUTH) {
    post<Recipes> { _ ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      val body = call.receive<CreateRecipeRequestDto>()
      val input = body.toInput() ?: return@post call.respond(HttpStatusCode.BadRequest)
      recipeService
          .create(userId, input)
          .fold(
              ifLeft = { call.respondError(it, CreateRecipeError::toHttp) },
              ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
          )
    }

    put<Recipes.ById> { res ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      val body = call.receive<CreateRecipeRequestDto>()
      val input = body.toInput() ?: return@put call.respond(HttpStatusCode.BadRequest)
      recipeService
          .update(userId, res.id, input)
          .fold(
              ifLeft = { call.respondError(it, UpdateRecipeError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }

    delete<Recipes.ById> { res ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      recipeService
          .delete(userId, res.id)
          .fold(
              ifLeft = { call.respond(HttpStatusCode.NoContent) },
              ifRight = { call.respond(HttpStatusCode.NoContent) },
          )
    }

    post<Recipes.ById.Like> { res ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      likeService
          .toggle(userId, LikeTarget.Recipe(res.parent.id))
          .fold(
              ifLeft = { call.respondError(it, LikeTargetError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }

    post<Recipes.ById.Comments> { res ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      val body = call.receive<CreateCommentRequestDto>()
      val parentId = body.parentId?.toId(::CommentId)
      commentService
          .create(userId, CommentTarget.Recipe(res.parent.id), body.text, parentId)
          .fold(
              ifLeft = { call.respondError(it, CreateCommentError::toHttp) },
              ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
          )
    }

    post<Recipes.ById.Clone> { res ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      recipeService
          .clone(userId, res.parent.id)
          .fold(
              ifLeft = { call.respondError(it, CloneRecipeError::toHttp) },
              ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
          )
    }
  }
}

private fun LikeInfo.toDto() = RecipeLikeDto(isLiked = isLiked, likesCount = count)

private fun CreateRecipeRequestDto.toInput(): CreateRecipeInput? {
  val imageId = imageId?.let { it.toId(::ImageId) ?: return null }
  val steps =
      steps.map { step ->
        CreateBrewStepInput(
            title = step.title,
            description = step.description,
            order = step.order,
            durationSeconds = step.durationSeconds,
            imageId = step.imageId?.let { it.toId(::ImageId) ?: return null },
        )
      }
  return CreateRecipeInput(
      title = title,
      description = description,
      note = note,
      imageId = imageId,
      brewMethod = brewMethod.toDomain(),
      difficulty = difficulty.toDomain(),
      roastLevel = roastLevel.toDomain(),
      beans = beans,
      public = true,
      steps = steps,
  )
}

@OptIn(ExperimentalUuidApi::class)
private fun Recipe.toDto() =
    RecipeResponseDto(
        id = id.value.toString(),
        authorId = author.id.value.toString(),
        author =
            AuthorResponseDto(
                id = author.id.value.toString(),
                username = author.username,
                avatarId = author.avatarId?.value.toString(),
            ),
        title = title,
        description = description,
        note = note,
        imageId = imageId?.value.toString(),
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
        imageId = imageId?.value.toString(),
    )

@OptIn(ExperimentalUuidApi::class)
private fun RecipeOriginInfo.toDto() =
    RecipeOriginResponseDto(
        author =
            AuthorResponseDto(
                id = author.id.value.toString(),
                username = author.username,
                avatarId = author.avatarId?.value.toString(),
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

private fun BrewMethodDto.toDomain() =
    when (this) {
      BrewMethodDto.V60 -> BrewMethod.V60
      BrewMethodDto.FRENCH_PRESS -> BrewMethod.FrenchPress
      BrewMethodDto.AEROPRESS -> BrewMethod.Aeropress
      BrewMethodDto.CHEMEX -> BrewMethod.Chemex
      BrewMethodDto.COLD_BREW -> BrewMethod.ColdBrew
      BrewMethodDto.EXPRESSO_MACHINE -> BrewMethod.EspressoMachine
      BrewMethodDto.MOKA_POT -> BrewMethod.MokaPot
      BrewMethodDto.NONE -> BrewMethod.NONE
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

      is CreateRecipeError.ImagesNotUploaded ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(
                  ApiErrorCode.INVALID_INPUT,
                  "The following images were not uploaded: ${ids.joinToString(", ")}",
              )
    }

private fun UpdateRecipeError.toHttp() =
    when (this) {
      UpdateRecipeError.NotFound -> ApiErrors.NotFound
      UpdateRecipeError.Forbidden -> ApiErrors.Forbidden

      is UpdateRecipeError.InvalidInput ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(ApiErrorCode.INVALID_INPUT, error.message())

      is UpdateRecipeError.ImagesNotUploaded ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(
                  ApiErrorCode.INVALID_INPUT,
                  "The following images were not uploaded: ${ids.joinToString(", ")}",
              )
    }

private fun LikeTargetError.toHttp() =
    when (this) {
      LikeTargetError.NotFound -> ApiErrors.NotFound
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
