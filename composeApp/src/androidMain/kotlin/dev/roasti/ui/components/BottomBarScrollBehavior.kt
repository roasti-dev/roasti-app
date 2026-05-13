package dev.roasti.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import kotlin.math.abs
import kotlinx.coroutines.delay

private const val SnapDurationMillis = 250
private const val SnapIdleDelayMillis = 120L
private const val MinDeltaPx = 0.5f

/**
 * Drives a bottom navigation bar's collapse/reveal as a pure observer of a [LazyListState].
 *
 * The bar never intercepts scroll dispatch — list fling and drag physics are untouched.
 * `heightOffsetPx` ∈ `[-heightPx, 0]` — `0` is fully visible, `-heightPx` is fully hidden.
 *
 * The offset is backed by an [Animatable] so concurrent drag updates ([attachTo]) and
 * idle snaps ([snapOnIdle]) interrupt cleanly: a new `snapTo` cancels any running
 * `animateTo`, so the bar never fights itself.
 */
@Stable
class BottomBarScrollBehavior internal constructor() {
    var heightPx: Float by mutableFloatStateOf(0f)
    internal val heightOffsetAnim = Animatable(0f)
    val heightOffsetPx: Float get() = heightOffsetAnim.value
}

@Composable
fun rememberBottomBarScrollBehavior(): BottomBarScrollBehavior =
    remember { BottomBarScrollBehavior() }

/**
 * Provides the active [BottomBarScrollBehavior] to descendant composables. Screens with
 * scrollable content opt in via [Modifier.bottomBarAware]; screens without lists ignore it.
 */
val LocalBottomBarScrollBehavior = staticCompositionLocalOf<BottomBarScrollBehavior?> { null }

/**
 * Wires a [LazyListState] to the ambient [LocalBottomBarScrollBehavior] (if any), so the
 * bottom bar collapses/reveals with this list and snaps to the nearest extreme on idle.
 * No-op when no behavior is provided in the composition tree.
 */
@Composable
fun Modifier.bottomBarAware(listState: LazyListState): Modifier {
    val behavior = LocalBottomBarScrollBehavior.current ?: return this
    behavior.attachTo(listState)
    behavior.snapOnIdle(listState)
    return this
}

/**
 * Snaps the bar to the nearest extreme once the list has been idle for a short window.
 * The debounce avoids snapping on micro-pauses inside a slow drag, where
 * [LazyListState.isScrollInProgress] briefly toggles to false.
 */
@Composable
internal fun BottomBarScrollBehavior.snapOnIdle(listState: LazyListState) {
    LaunchedEffect(this, listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (inProgress) return@collect
            delay(SnapIdleDelayMillis)
            if (listState.isScrollInProgress) return@collect
            if (heightPx <= 0f) return@collect
            val current = heightOffsetAnim.value
            if (current <= -heightPx || current >= 0f) return@collect
            val target = if (current < -heightPx / 2f) -heightPx else 0f
            heightOffsetAnim.animateTo(target, tween(durationMillis = SnapDurationMillis))
        }
    }
}

/**
 * Translates list scroll into bar offset 1:1, in real pixels.
 *
 * On every layout we look for any item present in both the previous and the current
 * `visibleItemsInfo` snapshot and use its `offset` diff as the exact px delta. This
 * removes the discontinuities you get from diffing only `firstVisibleItemIndex` /
 * `firstVisibleItemScrollOffset`, which jumps when the first item changes.
 *
 * If nothing overlaps (a large fling/jump cleared the previous viewport), we snap to
 * the nearest extreme based on direction. Sub-pixel deltas are ignored to suppress
 * snapshot jitter.
 */
@Composable
internal fun BottomBarScrollBehavior.attachTo(listState: LazyListState) {
    val lastOffsets = remember { mutableMapOf<Int, Int>() }

    LaunchedEffect(this, listState) {
        snapshotFlow { listState.layoutInfo }.collect { info ->
            if (heightPx <= 0f) return@collect
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) return@collect

            var pxDelta: Float? = null
            for (item in visible) {
                val prev = lastOffsets[item.index] ?: continue
                pxDelta = (prev - item.offset).toFloat()
                break
            }

            when {
                pxDelta == null && lastOffsets.isNotEmpty() -> {
                    val forward = visible.first().index > (lastOffsets.keys.minOrNull() ?: Int.MAX_VALUE)
                    heightOffsetAnim.snapTo(if (forward) -heightPx else 0f)
                }
                pxDelta != null && abs(pxDelta) >= MinDeltaPx -> {
                    val target = (heightOffsetAnim.value - pxDelta).coerceIn(-heightPx, 0f)
                    heightOffsetAnim.snapTo(target)
                }
            }

            lastOffsets.clear()
            for (item in visible) lastOffsets[item.index] = item.offset
        }
    }
}
