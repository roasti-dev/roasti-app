package dev.roasti.feature.recipe.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import dev.roasti.Recipe
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.recipe.data.mapper.upsertRecipe
import dev.roasti.testing.FakeLikesApiClient
import dev.roasti.testing.FakeRecipesApiClient
import dev.roasti.testing.RecipeFixtures
import dev.roasti.testing.inMemoryRoastiDatabase
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests of the architectural property "one Recipe entity, many list memberships":
 * a single toggleLike call must atomically propagate to both feed-pager subscribers
 * (Recipe.is_liked changed) and favorites-pager subscribers (membership added/removed).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListReactivityTest {

    private val db: RoastiDatabaseCache = inMemoryRoastiDatabase()
    private val api = FakeRecipesApiClient()
    private val likes = FakeLikesApiClient()
    private val repo = RecipeRepositoryImpl(api, db, likes)

    @AfterTest
    fun tearDown() {
        db.recipeListMembershipQueries.clearAllMemberships()
        db.recipeQueries.clearAllRecipes()
    }

    @Test
    fun `toggleLike on recipe in feed updates is_liked for feed subscribers`() = runTest {
        seedRecipe(id = "r1", isLiked = false)
        addToFeed(id = "r1", position = 0)

        feedFlow().test {
            val initial = awaitItem()
            assertEquals(listOf("r1" to 0L), initial.map { it.id to it.is_liked })

            repo.toggleLike("r1")

            val updated = awaitItem()
            assertEquals(listOf("r1" to 1L), updated.map { it.id to it.is_liked })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleLike adds recipe to favorites for favorites subscribers`() = runTest {
        seedRecipe(id = "r1", isLiked = false)
        addToFeed(id = "r1", position = 0)  // recipe is currently only in feed

        favoritesFlow().test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            repo.toggleLike("r1")

            val updated = awaitItem()
            assertEquals(listOf("r1"), updated.map { it.id })
            assertEquals(1L, updated.first().is_liked)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unlike removes from favorites but keeps recipe in feed`() = runTest {
        seedRecipe(id = "r1", isLiked = true)
        addToFeed(id = "r1", position = 0)
        addToFavorites(id = "r1", position = 0)

        favoritesFlow().test favorites@{
            assertEquals(listOf("r1"), awaitItem().map { it.id })

            feedFlow().test feed@{
                assertEquals(listOf("r1" to 1L), awaitItem().map { it.id to it.is_liked })

                repo.toggleLike("r1")

                // favorites: emptied
                val updatedFavorites = this@favorites.awaitItem()
                assertTrue(updatedFavorites.isEmpty())

                // feed: recipe still there but with is_liked = 0
                val updatedFeed = this@feed.awaitItem()
                assertEquals(listOf("r1" to 0L), updatedFeed.map { it.id to it.is_liked })

                this@feed.cancelAndIgnoreRemainingEvents()
            }
            this@favorites.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single toggleLike triggers atomic update visible to both feed and favorites subscribers`() = runTest {
        seedRecipe(id = "r1", isLiked = false)
        addToFeed(id = "r1", position = 0)

        favoritesFlow().test favorites@{
            assertTrue(awaitItem().isEmpty())

            feedFlow().test feed@{
                assertEquals(listOf("r1" to 0L), awaitItem().map { it.id to it.is_liked })

                repo.toggleLike("r1")

                // both subscribers see the change after a single toggle:
                val updatedFavorites = this@favorites.awaitItem()
                val updatedFeed = this@feed.awaitItem()

                assertEquals(listOf("r1"), updatedFavorites.map { it.id })
                assertEquals(listOf("r1" to 1L), updatedFeed.map { it.id to it.is_liked })

                this@feed.cancelAndIgnoreRemainingEvents()
            }
            this@favorites.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `revert on API failure rolls back both subscribers`() = runTest {
        seedRecipe(id = "r1", isLiked = false)
        addToFeed(id = "r1", position = 0)
        likes.toggleResult = Result.failure(RuntimeException("network"))

        favoritesFlow().test favorites@{
            assertTrue(awaitItem().isEmpty())

            feedFlow().test feed@{
                assertEquals(listOf("r1" to 0L), awaitItem().map { it.id to it.is_liked })

                repo.toggleLike("r1")

                // optimistic apply: both see the like
                val optimisticFavorites = this@favorites.awaitItem()
                val optimisticFeed = this@feed.awaitItem()
                assertEquals(listOf("r1"), optimisticFavorites.map { it.id })
                assertEquals(listOf("r1" to 1L), optimisticFeed.map { it.id to it.is_liked })

                // revert: both see rollback
                val revertedFavorites = this@favorites.awaitItem()
                val revertedFeed = this@feed.awaitItem()
                assertTrue(revertedFavorites.isEmpty())
                assertEquals(listOf("r1" to 0L), revertedFeed.map { it.id to it.is_liked })

                this@feed.cancelAndIgnoreRemainingEvents()
            }
            this@favorites.cancelAndIgnoreRemainingEvents()
        }
    }

    private fun feedFlow(): Flow<List<Recipe>> =
        db.recipeListMembershipQueries
            .getRecipesByList(RecipeListType.FEED, limit = 100, offset = 0)
            .asFlow()
            .mapToList(Dispatchers.IO)

    private fun favoritesFlow(): Flow<List<Recipe>> =
        db.recipeListMembershipQueries
            .getRecipesByList(RecipeListType.FAVORITES, limit = 100, offset = 0)
            .asFlow()
            .mapToList(Dispatchers.IO)

    private fun seedRecipe(id: String, isLiked: Boolean) {
        db.transaction {
            db.upsertRecipe(RecipeFixtures.dto(id = id, isLiked = isLiked))
        }
    }

    private fun addToFeed(id: String, position: Long) {
        db.recipeListMembershipQueries.insertMembership(
            listType = RecipeListType.FEED,
            recipeId = id,
            position = position,
        )
    }

    private fun addToFavorites(id: String, position: Long) {
        db.recipeListMembershipQueries.insertMembership(
            listType = RecipeListType.FAVORITES,
            recipeId = id,
            position = position,
        )
    }
}
