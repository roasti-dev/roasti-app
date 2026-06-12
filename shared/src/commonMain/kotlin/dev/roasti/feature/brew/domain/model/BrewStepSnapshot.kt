package dev.roasti.feature.brew.domain.model

/**
 * Копия шага рецепта на момент старта заваривания. Снапшот нужен, чтобы запущенный
 * Brew не сломался, если рецепт отредактируют/удалят во время заваривания.
 * Зеркалит [dev.roasti.feature.recipe.domain.model.BrewStep] (без description — его
 * нет в доменной модели шага рецепта).
 */
data class BrewStepSnapshot(
    val order: Int,
    val title: String,
    val durationSeconds: Int?,
    val imageId: String? = null,
)
