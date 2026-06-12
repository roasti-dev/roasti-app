package dev.roasti.feature.brew.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.roasti.RoastiDatabaseCache
import dev.roasti.core.datetime.WallClock
import dev.roasti.feature.brew.data.mapper.insertBrew
import dev.roasti.feature.brew.data.mapper.toDomain
import dev.roasti.feature.brew.domain.BrewAlarmScheduler
import dev.roasti.feature.brew.domain.BrewRepository
import dev.roasti.feature.brew.domain.model.Brew
import dev.roasti.feature.brew.domain.model.BrewStatus
import dev.roasti.feature.brew.domain.model.BrewStepSnapshot
import dev.roasti.feature.recipe.domain.model.Recipe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BrewRepositoryImpl(
    private val db: RoastiDatabaseCache,
    private val alarmScheduler: BrewAlarmScheduler,
    private val wallClock: WallClock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BrewRepository {

    override fun observeActive(): Flow<List<Brew>> =
        db.brewQueries.observeActive()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { row -> row.toDomain(stepsOf(row.id)) } }

    override fun observeCompleted(): Flow<List<Brew>> =
        db.brewQueries.observeCompleted()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { row -> row.toDomain(stepsOf(row.id)) } }

    override fun observeById(id: String): Flow<Brew?> {
        val brewFlow = db.brewQueries.observeById(id).asFlow().mapToOneOrNull(ioDispatcher)
        val stepsFlow = db.brewStepSnapshotQueries.getStepsByBrewId(id).asFlow().mapToList(ioDispatcher)
        return combine(brewFlow, stepsFlow) { brew, steps -> brew?.toDomain(steps) }
    }

    override suspend fun startBrew(recipe: Recipe, startStep: Int): Result<Brew> = runCatching {
        require(recipe.steps.isNotEmpty()) { "Recipe ${recipe.id} has no steps" }
        val now = wallClock.nowMillis()
        val safeStart = startStep.coerceIn(0, recipe.steps.lastIndex)
        val brew = Brew(
            id = Uuid.random().toString(),
            recipeId = recipe.id,
            recipeTitle = recipe.title,
            brewMethod = recipe.brewMethod,
            imageId = recipe.imageId,
            status = BrewStatus.BREWING,
            currentStepIndex = safeStart,
            steps = recipe.steps.map { step ->
                BrewStepSnapshot(
                    order = step.order,
                    title = step.title,
                    durationSeconds = step.durationSeconds,
                    imageId = step.imageId,
                )
            },
            waitUntil = null,
            backgroundStepIndex = null,
            note = null,
            startedAt = now,
            finishedAt = null,
            updatedAt = now,
        )
        db.transaction { db.insertBrew(brew) }
        brew
    }

    override suspend fun advanceToStep(brewId: String, index: Int): Result<Unit> = runCatching {
        db.brewQueries.updateStepIndex(
            stepIndex = index.toLong(),
            updatedAt = wallClock.nowMillis(),
            id = brewId,
        )
    }

    override suspend fun backgroundStep(
        brewId: String,
        stepIndex: Int,
        durationMillis: Long,
    ): Result<Unit> = runCatching {
        val brew = cachedBrew(brewId) ?: error("Brew $brewId is not in cache")
        val now = wallClock.nowMillis()
        val waitUntil = now + durationMillis
        db.brewQueries.setWaiting(
            waitUntil = waitUntil,
            stepIndex = stepIndex.toLong(),
            updatedAt = now,
            id = brewId,
        )
        alarmScheduler.schedule(
            brewId = brewId,
            triggerAtEpochMillis = waitUntil,
            recipeId = brew.recipeId,
            recipeTitle = brew.recipeTitle,
            nextStepIndex = stepIndex + 1,
        )
    }

    override suspend fun resumeFromWait(brewId: String): Result<Unit> = runCatching {
        val brew = cachedBrew(brewId) ?: error("Brew $brewId is not in cache")
        val nextStep = (brew.backgroundStepIndex ?: brew.currentStepIndex) + 1
        db.brewQueries.setBrewing(
            stepIndex = nextStep.toLong(),
            updatedAt = wallClock.nowMillis(),
            id = brewId,
        )
        alarmScheduler.cancel(brewId)
    }

    override suspend fun finishBrew(brewId: String, note: String?): Result<Unit> = runCatching {
        val now = wallClock.nowMillis()
        db.brewQueries.setCompleted(
            finishedAt = now,
            note = note,
            updatedAt = now,
            id = brewId,
        )
        alarmScheduler.cancel(brewId)
    }

    override suspend fun cancelBrew(brewId: String): Result<Unit> = runCatching {
        db.brewQueries.setCancelled(updatedAt = wallClock.nowMillis(), id = brewId)
        alarmScheduler.cancel(brewId)
    }

    override suspend fun rescheduleActiveAlarms(): Result<Unit> = runCatching {
        val now = wallClock.nowMillis()
        db.brewQueries.selectWaiting().executeAsList().forEach { row ->
            val waitUntil = row.wait_until ?: return@forEach
            // Прошедшие не планируем: wall-clock покажет «готово» при открытии (PRODUCT §5).
            if (waitUntil <= now) return@forEach
            val brew = row.toDomain()
            alarmScheduler.schedule(
                brewId = brew.id,
                triggerAtEpochMillis = waitUntil,
                recipeId = brew.recipeId,
                recipeTitle = brew.recipeTitle,
                nextStepIndex = (brew.backgroundStepIndex ?: brew.currentStepIndex) + 1,
            )
        }
    }

    private fun stepsOf(brewId: String) =
        db.brewStepSnapshotQueries.getStepsByBrewId(brewId).executeAsList()

    private fun cachedBrew(id: String): Brew? =
        db.brewQueries.getById(id).executeAsOneOrNull()?.toDomain(stepsOf(id))
}
