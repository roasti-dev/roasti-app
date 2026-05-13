package dev.roasti.features.recipes

import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.features.comments.Comment
import dev.roasti.features.comments.CommentId
import dev.roasti.features.comments.CommentService
import dev.roasti.features.comments.CommentTargetType
import dev.roasti.features.comments.CommentThread
import dev.roasti.common.domain.Page
import dev.roasti.features.likes.LikeService
import dev.roasti.features.likes.LikeTargetType
import arrow.core.Either
import arrow.core.left
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import dev.roasti.features.comments.CreateCommentError
import dev.roasti.features.likes.LikeInfo
import dev.roasti.features.users.UserId
import kotlin.uuid.ExperimentalUuidApi

sealed interface GetRecipeError {
    data object NotFound : GetRecipeError
    data object Forbidden : GetRecipeError
}

sealed interface CreateRecipeError {
    data class InvalidInput(val error: RecipeValidationError) : CreateRecipeError
}

sealed interface UpdateRecipeError {
    data object NotFound : UpdateRecipeError
    data object Forbidden : UpdateRecipeError
    data class InvalidInput(val error: RecipeValidationError) : UpdateRecipeError
}

sealed interface ToggleLikeError {
    data object RecipeNotFound : ToggleLikeError
}

sealed interface ListRecipeCommentsError {
    data object RecipeNotFound : ListRecipeCommentsError
}

sealed interface CreateRecipeCommentError {
    data object RecipeNotFound : CreateRecipeCommentError
    data class CommentError(val error: CreateCommentError) : CreateRecipeCommentError
}

sealed interface CloneRecipeError {
    data object NotFound : CloneRecipeError
    data object Forbidden : CloneRecipeError
}


sealed interface DeleteRecipeError {
    data object NotFound : DeleteRecipeError
    data object Forbidden : DeleteRecipeError
}

sealed interface RecipeValidationError {
    data object TitleEmpty : RecipeValidationError
    data object DescriptionEmpty : RecipeValidationError
}

interface RecipeService {
    suspend fun getById(id: RecipeId, userId: UserId?): Either<GetRecipeError, Recipe>
    suspend fun list(
        page: Int,
        limit: Int,
        authorId: UserId?,
        userId: UserId?,
        brewMethod: BrewMethod?,
        difficulty: Difficulty?,
        roastLevel: RoastLevel?,
    ): Page<Recipe>

    suspend fun create(userId: UserId, input: CreateRecipeInput): Either<CreateRecipeError, Recipe>
    suspend fun update(
        userId: UserId,
        id: RecipeId,
        input: CreateRecipeInput
    ): Either<UpdateRecipeError, Recipe>

    suspend fun delete(userId: UserId, id: RecipeId): Either<DeleteRecipeError, Unit>
    suspend fun toggleLike(userId: UserId, id: RecipeId): Either<ToggleLikeError, LikeInfo>
    suspend fun listComments(
        id: RecipeId,
        page: Int,
        limit: Int
    ): Either<ListRecipeCommentsError, Page<CommentThread>>

    suspend fun createComment(
        userId: UserId,
        id: RecipeId,
        text: String,
        parentId: CommentId?
    ): Either<CreateRecipeCommentError, Comment>

    suspend fun clone(userId: UserId, id: RecipeId): Either<CloneRecipeError, Recipe>
}

@OptIn(ExperimentalUuidApi::class)
class RecipeServiceImpl(
    private val repo: RecipeRepository,
    private val likeService: LikeService,
    private val commentService: CommentService,
) : RecipeService {

    override suspend fun getById(id: RecipeId, userId: UserId?): Either<GetRecipeError, Recipe> =
        either {
            val row = repo.findById(id) ?: raise(GetRecipeError.NotFound)
            if (!row.public && row.author.id != userId) raise(GetRecipeError.Forbidden)
            val steps = repo.getSteps(id)
            row.toRecipe(userId, steps)
        }

    override suspend fun list(
        page: Int,
        limit: Int,
        authorId: UserId?,
        userId: UserId?,
        brewMethod: BrewMethod?,
        difficulty: Difficulty?,
        roastLevel: RoastLevel?,
    ): Page<Recipe> {
        val (rows, total) = repo.list(
            page,
            limit,
            authorId,
            userId,
            brewMethod,
            difficulty,
            roastLevel
        )
        val recipeIds = rows.map { it.id }
        val stepsMap = repo.getStepsBatch(recipeIds)
        val targetIds = recipeIds.map { it.value }
        val likeInfos = likeService.getInfoBatch(userId, targetIds, LikeTargetType.RECIPE)

        val recipes = rows.map { row ->
            val id = row.id.value
            val likeInfo = likeInfos[id]
            row.toRecipeWithLikes(
                steps = stepsMap[row.id] ?: emptyList(),
                isLiked = likeInfo?.isLiked ?: false,
                likesCount = likeInfo?.count ?: 0,
            )
        }

        return Page.of(recipes, page, total, limit)
    }

    override suspend fun create(
        userId: UserId,
        input: CreateRecipeInput
    ): Either<CreateRecipeError, Recipe> = either {
        validateRecipe(input).mapLeft { CreateRecipeError.InvalidInput(it) }.bind()
        val row = repo.create(userId, input)
        val steps = repo.getSteps(row.id)
        row.toRecipe(userId, steps)
    }

    override suspend fun update(
        userId: UserId,
        id: RecipeId,
        input: CreateRecipeInput
    ): Either<UpdateRecipeError, Recipe> = either {
        validateRecipe(input).mapLeft { UpdateRecipeError.InvalidInput(it) }.bind()
        val existing = repo.findById(id) ?: raise(UpdateRecipeError.NotFound)
        ensure(existing.author.id == userId) { UpdateRecipeError.Forbidden }
        repo.update(id, input)
        val steps = repo.getSteps(id)
        val row = repo.findById(id)!!
        row.toRecipe(userId, steps)
    }

    fun validateRecipe(input: CreateRecipeInput): Either<RecipeValidationError, Unit> = either {
        ensure(input.title.isNotBlank()) { RecipeValidationError.TitleEmpty }
        ensure(input.description.isNotBlank()) { RecipeValidationError.DescriptionEmpty }
    }

    override suspend fun delete(userId: UserId, id: RecipeId): Either<DeleteRecipeError, Unit> =
        either {
            val existing = repo.findById(id) ?: raise(DeleteRecipeError.NotFound)
            if (existing.author.id != userId) raise(DeleteRecipeError.Forbidden)
            repo.delete(id)
        }

    override suspend fun toggleLike(
        userId: UserId,
        id: RecipeId
    ): Either<ToggleLikeError, LikeInfo> = either {
        repo.findById(id) ?: raise(ToggleLikeError.RecipeNotFound)
        likeService.toggle(userId, id.value, LikeTargetType.RECIPE)
    }

    override suspend fun listComments(
        id: RecipeId,
        page: Int,
        limit: Int
    ): Either<ListRecipeCommentsError, Page<CommentThread>> = either {
        repo.findById(id) ?: raise(ListRecipeCommentsError.RecipeNotFound)
        commentService.list(id.value, CommentTargetType.RECIPE, page, limit)
    }

    override suspend fun createComment(
        userId: UserId,
        id: RecipeId,
        text: String,
        parentId: CommentId?,
    ): Either<CreateRecipeCommentError, Comment> = either {
        repo.findById(id) ?: raise(CreateRecipeCommentError.RecipeNotFound)
        commentService.create(userId, id.value, CommentTargetType.RECIPE, text, parentId)
            .mapLeft { CreateRecipeCommentError.CommentError(it) }.bind()
    }

    override suspend fun clone(userId: UserId, id: RecipeId): Either<CloneRecipeError, Recipe> =
        either {
            val original = repo.findById(id) ?: raise(CloneRecipeError.NotFound)
            if (!original.public && original.author.id != userId) raise(CloneRecipeError.Forbidden)

            val originalSteps = repo.getSteps(id)
            val newRow = repo.create(userId, original.toCloneInput(originalSteps))
            val steps = repo.getSteps(newRow.id)
            newRow.toRecipe(userId, steps)
        }

    fun RecipeRow.toCloneInput(steps: List<BrewStep>) = CreateRecipeInput(
        title = title,
        description = description,
        note = note,
        imageId = imageId,
        brewMethod = brewMethod,
        difficulty = difficulty,
        roastLevel = roastLevel,
        beans = beans,
        public = false,
        steps = steps.map { it.toCreateInput() },
        originRecipeId = id,
    )

    private fun BrewStep.toCreateInput() = CreateBrewStepInput(
        title = title,
        description = description,
        order = order,
        durationSeconds = durationSeconds,
        imageId = imageId,
    )

    private suspend fun RecipeRow.toRecipe(userId: UserId?, steps: List<BrewStep>): Recipe {
        val likeInfo = likeService.getInfo(userId, id.value, LikeTargetType.RECIPE)
        return toRecipeWithLikes(steps, likeInfo.isLiked, likeInfo.count)
    }

    private fun RecipeRow.toRecipeWithLikes(
        steps: List<BrewStep>,
        isLiked: Boolean,
        likesCount: Int,
    ) = Recipe(
        id = id,
        author = author,
        title = title,
        description = description,
        note = note,
        imageId = imageId,
        brewMethod = brewMethod,
        difficulty = difficulty,
        roastLevel = roastLevel,
        beans = beans,
        public = public,
        steps = steps,
        isLiked = isLiked,
        likesCount = likesCount,
        origin = origin,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

}

