package dev.roasti.feature.recipe.data

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.likes.data.RecipeLikeDto
import dev.roasti.feature.recipe.data.mapper.upsertRecipe
import dev.roasti.testing.FakeLikesApiClient
import dev.roasti.testing.FakeRecipesApiClient
import dev.roasti.testing.RecipeFixtures
import dev.roasti.testing.inMemoryRoastiDatabase
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeRepositoryImplTest {

    private val db: RoastiDatabaseCache = inMemoryRoastiDatabase()
    private val api = FakeRecipesApiClient()
    private val likes = FakeLikesApiClient()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repo = RecipeRepositoryImpl(api, db, likes, ioDispatcher = testDispatcher)

    @AfterTest
    fun tearDown() {
        db.recipeListMembershipQueries.clearAllMemberships()
        db.recipeQueries.clearAllRecipes()
    }

    @Test
    fun `toggleLike on unliked recipe likes it and adds to favorites`() = runTest {
        seedRecipe(id = "r1", isLiked = false)

        val result = repo.toggleLike("r1")

        assertTrue(result.isSuccess)
        val cached = db.recipeQueries.getRecipeById("r1").executeAsOne()
        assertEquals(1L, cached.is_liked)
        assertEquals(1L, favoritesCount())
        assertEquals(1, likes.toggleCallCount)
    }

    @Test
    fun `toggleLike on liked recipe unlikes it and removes from favorites`() = runTest {
        seedRecipe(id = "r1", isLiked = true)
        db.recipeListMembershipQueries.insertMembership(
            listType = RecipeListType.FAVORITES,
            recipeId = "r1",
            position = 0,
        )

        val result = repo.toggleLike("r1")

        assertTrue(result.isSuccess)
        val cached = db.recipeQueries.getRecipeById("r1").executeAsOne()
        assertEquals(0L, cached.is_liked)
        assertEquals(0L, favoritesCount())
    }

    @Test
    fun `toggleLike applies optimistic update before API responds`() = runTest {
        seedRecipe(id = "r1", isLiked = false)
        likes.toggleGate = CompletableDeferred()

        val job = launch { repo.toggleLike("r1") }
        runCurrent()

        val cached = db.recipeQueries.getRecipeById("r1").executeAsOne()
        assertEquals(1L, cached.is_liked)
        assertEquals(1L, favoritesCount())

        likes.toggleGate?.complete(Unit)
        job.join()
    }

    @Test
    fun `toggleLike reverts on API failure when liking`() = runTest {
        seedRecipe(id = "r1", isLiked = false)
        likes.toggleResult = Result.failure(RuntimeException("network"))

        val result = repo.toggleLike("r1")

        assertTrue(result.isFailure)
        val cached = db.recipeQueries.getRecipeById("r1").executeAsOne()
        assertEquals(0L, cached.is_liked)
        assertEquals(0L, favoritesCount())
    }

    @Test
    fun `toggleLike reverts on API failure when unliking`() = runTest {
        seedRecipe(id = "r1", isLiked = true)
        db.recipeListMembershipQueries.insertMembership(
            listType = RecipeListType.FAVORITES,
            recipeId = "r1",
            position = 0,
        )
        likes.toggleResult = Result.failure(RuntimeException("network"))

        val result = repo.toggleLike("r1")

        assertTrue(result.isFailure)
        val cached = db.recipeQueries.getRecipeById("r1").executeAsOne()
        assertEquals(1L, cached.is_liked)
        assertEquals(1L, favoritesCount())
    }

    @Test
    fun `toggleLike fails when recipe is not in cache`() = runTest {
        val result = repo.toggleLike("missing")

        assertTrue(result.isFailure)
        assertEquals(0, likes.toggleCallCount)
    }

    @Test
    fun `toggleLike does not modify feed membership`() = runTest {
        seedRecipe(id = "r1", isLiked = false)
        db.recipeListMembershipQueries.insertMembership(
            listType = RecipeListType.FEED,
            recipeId = "r1",
            position = 0,
        )

        repo.toggleLike("r1")

        assertEquals(1L, feedCount())
        assertEquals(1L, favoritesCount())
    }

    @Test
    fun `toggleLike on revert restores favorites membership`() = runTest {
        seedRecipe(id = "r1", isLiked = true)
        db.recipeListMembershipQueries.insertMembership(
            listType = RecipeListType.FAVORITES,
            recipeId = "r1",
            position = 5,
        )
        likes.toggleResult = Result.failure(RuntimeException("network"))

        repo.toggleLike("r1")

        assertEquals(1L, favoritesCount())
        // position is reset to bottom via insertMembershipAtBottom on revert — that's expected
    }

    @Test
    fun `observeById emits null when recipe is not cached`() = runTest {
        repo.observeById("missing").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById emits recipe when upserted`() = runTest {
        repo.observeById("r1").test {
            assertNull(awaitItem())

            seedRecipe(id = "r1", isLiked = false)

            val item = awaitItem()
            assertNotNull(item)
            assertEquals("r1", item.id)
            assertFalse(item.isLiked)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById reflects toggleLike change`() = runTest {
        seedRecipe(id = "r1", isLiked = false)

        repo.observeById("r1").test {
            val initial = awaitItem()
            assertNotNull(initial)
            assertFalse(initial.isLiked)

            repo.toggleLike("r1")

            val toggled = awaitItem()
            assertNotNull(toggled)
            assertTrue(toggled.isLiked)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshById writes to cache`() = runTest {
        api.recipeById["r1"] = Result.success(RecipeFixtures.dto(id = "r1", title = "Updated"))

        val result = repo.refreshById("r1")

        assertTrue(result.isSuccess)
        val cached = db.recipeQueries.getRecipeById("r1").executeAsOneOrNull()
        assertNotNull(cached)
        assertEquals("Updated", cached.title)
    }

    @Test
    fun `getById falls back to cache when network fails`() = runTest {
        seedRecipe(id = "r1", title = "Cached")
        api.recipeById["r1"] = Result.failure(RuntimeException("network"))

        val result = repo.getById("r1")

        assertTrue(result.isSuccess)
        assertEquals("Cached", result.getOrThrow().title)
    }

    @Test
    fun `getById returns failure when network fails and no cache`() = runTest {
        api.recipeById["r1"] = Result.failure(RuntimeException("network"))

        val result = repo.getById("r1")

        assertTrue(result.isFailure)
    }

    private fun seedRecipe(
        id: String,
        title: String = "Title $id",
        isLiked: Boolean = false,
        likesCount: Int = 0,
    ) {
        db.transaction {
            db.upsertRecipe(
                RecipeFixtures.dto(
                    id = id,
                    title = title,
                    isLiked = isLiked,
                    likesCount = likesCount,
                )
            )
        }
    }

    private fun favoritesCount(): Long =
        db.recipeListMembershipQueries.countRecipesByList(RecipeListType.FAVORITES).executeAsOne()

    private fun feedCount(): Long =
        db.recipeListMembershipQueries.countRecipesByList(RecipeListType.FEED).executeAsOne()
}
