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
import dev.roasti.testing.FakeLikesApiClient
import dev.roasti.testing.RecipeFixtures
import dev.roasti.testing.fakeUser
import dev.roasti.testing.inMemoryRoastiDatabase
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
class FavoritesRemoteMediatorTest {

    private val pageSize = 3
    private val db: RoastiDatabaseCache = inMemoryRoastiDatabase()
    private val likes = FakeLikesApiClient()
    private val auth = FakeAuthRepository(initialUser = fakeUser(id = "user-1"))
    private val mediator = FavoritesRemoteMediator(likes, auth, db)

    @AfterTest
    fun tearDown() {
        db.recipeListMembershipQueries.clearAllMemberships()
        db.recipeQueries.clearAllRecipes()
        db.recipeRemoteKeyQueries.clearAllRemoteKeys()
    }

    @Test
    fun `REFRESH writes favorites membership and remote key with next page`() = runTest {
        likes.pages[1] = Result.success(
            RecipeFixtures.likedPage(ids = listOf("a", "b"), currentPage = 1, lastPage = 2)
        )

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(false, result.endOfPaginationReached)
        assertEquals(listOf("a", "b"), favoritesIdsInOrder())
        assertEquals(2L, remoteKey(RecipeListType.FAVORITES))
    }

    @Test
    fun `REFRESH on last page sets endOfPagination and null next page`() = runTest {
        likes.pages[1] = Result.success(
            RecipeFixtures.likedPage(ids = listOf("a"), currentPage = 1, lastPage = 1)
        )

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(true, result.endOfPaginationReached)
        assertNull(remoteKey(RecipeListType.FAVORITES))
    }

    @Test
    fun `APPEND continues positions from second page`() = runTest {
        likes.pages[1] = Result.success(
            RecipeFixtures.likedPage(ids = listOf("a", "b", "c"), currentPage = 1, lastPage = 2)
        )
        likes.pages[2] = Result.success(
            RecipeFixtures.likedPage(ids = listOf("d", "e"), currentPage = 2, lastPage = 2)
        )

        mediator.load(LoadType.REFRESH, emptyState())
        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(listOf("a", "b", "c", "d", "e"), favoritesIdsInOrder())
        assertNull(remoteKey(RecipeListType.FAVORITES))  // last page reached
    }

    @Test
    fun `REFRESH on API error returns Error and does not modify db`() = runTest {
        seedFavorites(ids = listOf("old"))
        likes.pages[1] = Result.failure(RuntimeException("network"))

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(1L, favoritesCount())
    }

    @Test
    fun `REFRESH does not touch feed membership`() = runTest {
        seedFeed(ids = listOf("feed-1", "feed-2"))
        likes.pages[1] = Result.success(
            RecipeFixtures.likedPage(ids = listOf("fav-a"), currentPage = 1, lastPage = 1)
        )

        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(2L, feedCount())  // feed untouched
        assertEquals(1L, favoritesCount())
    }

    @Test
    fun `load fails when user is not authenticated`() = runTest {
        auth.setUser(null)

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(0, likes.getLikedRecipesCallCount)
    }

    @Test
    fun `REFRESH overwrites optimistic top entry with server position`() = runTest {
        // simulate user pre-liked something optimistically (position = -1)
        db.recipeListMembershipQueries.insertMembershipAtTop(RecipeListType.FAVORITES, "x")

        likes.pages[1] = Result.success(
            RecipeFixtures.likedPage(ids = listOf("a", "x", "c"), currentPage = 1, lastPage = 1)
        )

        mediator.load(LoadType.REFRESH, emptyState())

        // server order wins, "x" is now in middle, no duplicate
        assertEquals(listOf("a", "x", "c"), favoritesIdsInOrder())
        assertEquals(3L, favoritesCount())
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

    private fun favoritesIdsInOrder(): List<String> =
        db.recipeListMembershipQueries
            .getRecipesByList(RecipeListType.FAVORITES, limit = 1000, offset = 0)
            .executeAsList()
            .map { it.id }

    private fun remoteKey(id: String): Long? =
        db.recipeRemoteKeyQueries.getRemoteKey(id).executeAsOneOrNull()?.next_page

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
}
