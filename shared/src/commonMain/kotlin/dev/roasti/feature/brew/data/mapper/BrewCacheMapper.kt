package dev.roasti.feature.brew.data.mapper

import dev.roasti.Brew as CachedBrew
import dev.roasti.BrewStepSnapshot as CachedBrewStep
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.brew.domain.model.Brew
import dev.roasti.feature.brew.domain.model.BrewStatus
import dev.roasti.feature.brew.domain.model.BrewStepSnapshot
import dev.roasti.feature.recipe.domain.model.BrewMethod

/** Запись Brew + снапшот шагов в одной транзакции (вызывать внутри db.transaction { }). */
fun RoastiDatabaseCache.insertBrew(brew: Brew) {
    brewQueries.insertBrew(
        id = brew.id,
        recipeId = brew.recipeId,
        recipeTitle = brew.recipeTitle,
        brewMethod = brew.brewMethod.name,
        imageId = brew.imageId,
        status = brew.status.name,
        currentStepIndex = brew.currentStepIndex.toLong(),
        waitUntil = brew.waitUntil,
        backgroundStepIndex = brew.backgroundStepIndex?.toLong(),
        note = brew.note,
        startedAt = brew.startedAt,
        finishedAt = brew.finishedAt,
        updatedAt = brew.updatedAt,
    )
    brewStepSnapshotQueries.deleteStepsByBrewId(brew.id)
    brew.steps.forEach { step ->
        brewStepSnapshotQueries.insertStep(
            brewId = brew.id,
            stepOrder = step.order.toLong(),
            title = step.title,
            durationSeconds = step.durationSeconds?.toLong(),
            imageId = step.imageId,
        )
    }
}

internal fun CachedBrew.toDomain(steps: List<CachedBrewStep> = emptyList()): Brew = Brew(
    id = id,
    recipeId = recipe_id,
    recipeTitle = recipe_title,
    brewMethod = BrewMethod.valueOf(brew_method),
    imageId = image_id,
    status = BrewStatus.valueOf(status),
    currentStepIndex = current_step_index.toInt(),
    steps = steps.map(CachedBrewStep::toDomain),
    waitUntil = wait_until,
    backgroundStepIndex = background_step_index?.toInt(),
    note = note,
    startedAt = started_at,
    finishedAt = finished_at,
    updatedAt = updated_at,
)

private fun CachedBrewStep.toDomain(): BrewStepSnapshot = BrewStepSnapshot(
    order = step_order.toInt(),
    title = title,
    durationSeconds = duration_seconds?.toInt(),
    imageId = image_id,
)
