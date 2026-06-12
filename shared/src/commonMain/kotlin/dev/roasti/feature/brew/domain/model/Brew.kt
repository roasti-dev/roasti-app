package dev.roasti.feature.brew.domain.model

import dev.roasti.feature.recipe.domain.model.BrewMethod

/**
 * Конкретный запуск рецепта во времени. Персистентная локальная сущность (без сети).
 * Recipe — шаблон; Brew — экземпляр со своим состоянием, позицией и снапшотом шагов.
 */
data class Brew(
    val id: String,
    val recipeId: String,
    val recipeTitle: String,            // денормализовано — снапшот
    val brewMethod: BrewMethod,
    val imageId: String?,               // для карточки в карусели
    val status: BrewStatus,
    val currentStepIndex: Int,          // позиция, персистится при каждом advance
    val steps: List<BrewStepSnapshot>,  // копия шагов рецепта на момент старта
    val waitUntil: Long?,               // epochMillis; != null только в WAITING
    val backgroundStepIndex: Int?,      // какой шаг отпущен в фон
    val note: String?,                  // опциональная заметка на финале
    val startedAt: Long,                // epochMillis
    val finishedAt: Long?,              // epochMillis, в COMPLETED
    val updatedAt: Long,
)

/** Источник правды о готовности фонового шага — wall-clock vs waitUntil (не факт доставки уведомления). */
fun Brew.isBackgroundStepReady(nowMillis: Long): Boolean =
    status == BrewStatus.WAITING && waitUntil != null && nowMillis >= waitUntil

/** Сколько осталось ждать, либо null если шаг не в фоне. Никогда не отрицательное. */
fun Brew.remainingMillis(nowMillis: Long): Long? =
    waitUntil?.let { (it - nowMillis).coerceAtLeast(0L) }
