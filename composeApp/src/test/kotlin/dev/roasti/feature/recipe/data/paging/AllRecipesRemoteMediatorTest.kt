package dev.roasti.feature.recipe.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import kotlinx.coroutines.test.runTest
import org.junit.Test
import dev.roasti.Recipe
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.recipe.data.RecipeListType
import dev.roasti.testing.FakeAuthRepository
import dev.roasti.testing.FakeRecipesApiClient
import dev.roasti.testing.RecipeFixtures
import dev.roasti.testing.fakeUser
import dev.roasti.testing.inMemoryRoastiDatabase
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
class AllRecipesRemoteMediatorTest {

    private val pageSize = 3
    private val db: RoastiDatabaseCache = inMemoryRoastiDatabase()
    private val api = FakeRecipesApiClient()
    private val auth = FakeAuthRepository(initialUser = fakeUser())
    private val mediator = AllRecipesRemoteMediator(auth, api, db)

    @AfterTest
    fun tearDown() {
        db.recipeListMembershipQueries.clearAllMemberships()
        db.recipeQueries.clearAllRecipes()
        db.recipeRemoteKeyQueries.clearAllRemoteKeys()
    }

    @Test
    fun `REFRESH writes recipes feed membership and remote key with next page`() = runTest {
        api.pages[1] = Result.success(
            RecipeFixtures.page(ids = listOf("a", "b", "c"), currentPage = 1, lastPage = 3)
        )

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(false, result.endOfPaginationReached)

        assertEquals(listOf("a", "b", "c"), feedIdsInOrder())
        assertEquals(2L, remoteKey(RecipeListType.FEED))
    }

    @Test
    fun `REFRESH on last page sets endOfPagination and null next page`() = runTest {
        api.pages[1] = Result.success(
            RecipeFixtures.page(ids = listOf("a", "b"), currentPage = 1, lastPage = 1)
        )

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(true, result.endOfPaginationReached)
        assertNull(remoteKey(RecipeListType.FEED))
    }

    @Test
    fun `APPEND continues positions from second page`() = runTest {
        api.pages[1] = Result.success(
            RecipeFixtures.page(ids = listOf("a", "b", "c"), currentPage = 1, lastPage = 3)
        )
        api.pages[2] = Result.success(
            RecipeFixtures.page(ids = listOf("d", "e", "f"), currentPage = 2, lastPage = 3)
        )

        mediator.load(LoadType.REFRESH, emptyState())
        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), feedIdsInOrder())
        assertEquals(3L, remoteKey(RecipeListType.FEED))
    }

    @Test
    fun `APPEND without remote key returns endOfPagination`() = runTest {
        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(true, result.endOfPaginationReached)
        assertEquals(0, api.getRecipesCallCount)
    }

    @Test
    fun `REFRESH on API error returns Error and does not modify db`() = runTest {
        seedFeed(ids = listOf("old1", "old2"))
        api.pages[1] = Result.failure(RuntimeException("network"))

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(2L, feedCount())  // pre-seeded data untouched
    }

    @Test
    fun `REFRESH does not touch favorites membership`() = runTest {
        seedFavorites(ids = listOf("fav-1", "fav-2"))
        api.pages[1] = Result.success(
            RecipeFixtures.page(ids = listOf("a", "b"), currentPage = 1, lastPage = 1)
        )

        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(2L, feedCount())
        assertEquals(2L, favoritesCount())  // favorites untouched
    }

    @Test
    fun `REFRESH twice rewrites feed membership in place`() = runTest {
        api.pages[1] = Result.success(
            RecipeFixtures.page(ids = listOf("a", "b", "c"), currentPage = 1, lastPage = 1)
        )
        mediator.load(LoadType.REFRESH, emptyState())

        api.pages[1] = Result.success(
            RecipeFixtures.page(ids = listOf("x", "y"), currentPage = 1, lastPage = 1)
        )
        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(listOf("x", "y"), feedIdsInOrder())
    }

    private fun emptyState(): PagingState<Int, Recipe> = PagingState(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = pageSize),
        leadingPlaceholderCount = 0,
    )

    private fun feedCount(): Long =
        db.recipeListMembershipQueries.countRecipesByList(RecipeListType.FEED).executeAsOne()

    private fun favoritesCount(): Long =
        db.recipeListMembershipQueries.countRecipesByList(RecipeListType.FAVORITES).executeAsOne()

    private fun feedIdsInOrder(): List<String> =
        db.recipeListMembershipQueries
            .getRecipesByList(RecipeListType.FEED, limit = 1000, offset = 0)
            .executeAsList()
            .map { it.id }

    private fun remoteKey(id: String): Long? =
        db.recipeRemoteKeyQueries.getRemoteKey(id).executeAsOneOrNull()?.next_page

    private fun seedFeed(ids: List<String>) {
        db.transaction {
            ids.forEachIndexed { i, id ->
                db.recipeListMembershipQueries.insertMembership(
                    listType = RecipeListType.FEED,
                    recipeId = id,
                    position = i.toLong(),
                )
            }
        }
    }

    private fun seedFavorites(ids: List<String>) {
        db.transaction {
            ids.forEachIndexed { i, id ->
                db.recipeListMembershipQueries.insertMembership(
                    listType = RecipeListType.FAVORITES,
                    recipeId = id,
                    position = i.toLong(),
                )
            }
        }
    }
}
