package dev.roasti.feature.brew.domain

import dev.roasti.feature.brew.domain.model.Brew
import dev.roasti.feature.recipe.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

/**
 * Единственная точка управления завариваниями (Brew). Локальная сущность, без сети.
 * Все переходы статусов идут только отсюда (аналог RecipeRepository.toggleLike).
 * Атомарность: БД-транзакция + вызов [BrewAlarmScheduler] в одной операции.
 */
interface BrewRepository {
    fun observeActive(): Flow<List<Brew>>       // карусель «Сейчас завариваются» + бейдж
    fun observeCompleted(): Flow<List<Brew>>    // экран Истории
    fun observeById(id: String): Flow<Brew?>

    /** Создаёт Brew (status=BREWING, снапшот шагов рецепта). Персист с момента старта. */
    suspend fun startBrew(recipe: Recipe, startStep: Int = 0): Result<Brew>

    /** Персистит позицию при переходе по шагам. */
    suspend fun advanceToStep(brewId: String, index: Int): Result<Unit>

    /** Отпускает длинный шаг в фон: WAITING + waitUntil=now+duration + планирует будильник. */
    suspend fun backgroundStep(brewId: String, stepIndex: Int, durationMillis: Long): Result<Unit>

    /** Возврат из ожидания (по будильнику или досрочно): BREWING + след. шаг + отмена будильника. */
    suspend fun resumeFromWait(brewId: String): Result<Unit>

    /** Финал: COMPLETED + finished_at + опциональная заметка. */
    suspend fun finishBrew(brewId: String, note: String?): Result<Unit>

    /** Отмена: CANCELLED + отмена будильника. */
    suspend fun cancelBrew(brewId: String): Result<Unit>

    /** Перепланирование будильников всех WAITING-брю (вызывается из BootReceiver после ребута). */
    suspend fun rescheduleActiveAlarms(): Result<Unit>
}
