package dev.roasti.feature.recipe.domain.session

data class AutoAdvanceCountdown(
    val targetStepIndex: Int,
    val startedAtMillis: Long,
    val totalMillis: Long,
) {
    fun remainingMillis(nowMillis: Long): Long =
        (totalMillis - (nowMillis - startedAtMillis)).coerceAtLeast(0L)

    fun progress(nowMillis: Long): Float =
        if (totalMillis <= 0L) 1f else remainingMillis(nowMillis) / totalMillis.toFloat()
}
