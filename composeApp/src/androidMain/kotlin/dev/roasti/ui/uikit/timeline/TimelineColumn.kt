package dev.roasti.ui.uikit.timeline

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.roasti.ui.theme.Spacing

/**
 * Vertical timeline list with built-in connector line, badge column,
 * and collapsed/expanded content swap per row.
 *
 * The API mirrors `LazyColumn`: items are declared inside a [TimelineScope]
 * via [TimelineScope.items], and `TimelineColumn` owns the underlying
 * [LazyColumn], scrolling, layout and all per-row animations
 * (badge resize, connector fill, collapsed↔expanded content swap).
 *
 * State that the caller controls:
 * - [activeIndex] — which row is currently in the `Active` state. Rows below
 *   it are `Completed`, rows above it are `Pending`.
 * - [expandedIndex] — which row, if any, should render its `expanded` content
 *   slot. Only the row matching [expandedIndex] renders expanded; everything
 *   else renders the `collapsed` slot.
 *
 * The caller does **not** own the [LazyColumn]; pass a [listState] only if you
 * need to read scroll position externally.
 *
 * ### Usage
 * ```
 * TimelineColumn(
 *     activeIndex = session.currentStepIndex,
 *     expandedIndex = session.expandedIndex,
 *     autoScrollToActive = true,
 *     badge = { state, index, size ->
 *         StepIndicatorBadge(kind = state.toKind(), number = index + 1, size = size)
 *     },
 * ) {
 *     items(
 *         items = session.rows,
 *         key = { it.index },
 *         collapsed = { row -> CollapsedStepContent(row.title, ...) },
 *         expanded = { row -> BrewingActiveCard(row.title, ...) },
 *     )
 * }
 * ```
 *
 * @param activeIndex zero-based index of the currently active row. Drives node
 *   state per row and (if [autoScrollToActive]) the auto-scroll target.
 * @param expandedIndex zero-based index of the row to render its `expanded`
 *   slot. Pass `null` to keep all rows collapsed.
 * @param autoScrollToActive if true, animates the list to keep [activeIndex]
 *   visible whenever it changes.
 * @param colors color tokens for the connector track and fill.
 * @param dimensions size tokens for the badge column, badge sizes,
 *   connector width and animation durations.
 * @param badge slot for the leading badge. Receives the row's node state,
 *   its index, and the animated badge size in dp. The slot must respect
 *   the provided size — the connector geometry is computed from it.
 *   Defaults to [DefaultTimelineBadge].
 * @param content declares rows via [TimelineScope.items].
 */
@Composable
fun TimelineColumn(
    activeIndex: Int,
    modifier: Modifier = Modifier,
    expandedIndex: Int? = null,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    autoScrollToActive: Boolean = true,
    colors: TimelineColors = TimelineDefaults.colors(),
    dimensions: TimelineDimensions = TimelineDefaults.dimensions(),
    badge: @Composable (state: TimelineNodeState, index: Int, size: Dp) -> Unit =
        { state, index, size -> DefaultTimelineBadge(state = state, number = index + 1, size = size) },
    content: TimelineScope.() -> Unit,
) {
    val scope = TimelineScopeImpl().apply(content)
    val entries = scope.entries

    LaunchedEffect(activeIndex, autoScrollToActive) {
        if (autoScrollToActive && activeIndex >= 0 && activeIndex < entries.size) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        itemsIndexed(
            items = entries,
            key = { index, entry -> entry.key ?: index },
        ) { index, entry ->
            val nodeState = nodeStateFor(index, activeIndex)
            val isExpanded = expandedIndex == index
            val itemScope = TimelineItemScopeImpl(
                index = index,
                nodeState = nodeState,
                isExpanded = isExpanded,
            )
            TimelineRow(
                index = index,
                nodeState = nodeState,
                isExpanded = isExpanded,
                isLast = index == entries.lastIndex,
                colors = colors,
                dimensions = dimensions,
                badge = badge,
                collapsedContent = { entry.collapsed(itemScope) },
                expandedContent = entry.expanded?.let { e -> { e(itemScope) } },
            )
        }
    }
}

// ---------- Public types ----------

/** Logical state of a timeline row. Drives default colors and animations. */
enum class TimelineNodeState { Completed, Active, Pending }

/** Scope passed to [TimelineColumn]'s content lambda. Mirrors `LazyListScope`. */
@Stable
interface TimelineScope {
    /**
     * Declare a contiguous block of typed rows.
     *
     * @param collapsed required slot rendered when the row is not expanded.
     * @param expanded optional slot rendered when the row matches
     *   `TimelineColumn(expandedIndex = ...)`. If `null`, the row never
     *   expands and clicks have no effect.
     */
    fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)? = null,
        expanded: (@Composable TimelineItemScope.(item: T) -> Unit)? = null,
        collapsed: @Composable TimelineItemScope.(item: T) -> Unit,
    )

    /** Index-based variant for callers without a backing list. */
    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        expanded: (@Composable TimelineItemScope.(index: Int) -> Unit)? = null,
        collapsed: @Composable TimelineItemScope.(index: Int) -> Unit,
    )
}

/** Scope exposed to each row's content slot. Read-only view of row state. */
@Stable
interface TimelineItemScope {
    val index: Int
    val nodeState: TimelineNodeState
    val isExpanded: Boolean
}

@Immutable
data class TimelineColors(
    val connectorTrack: Color,
    val connectorFill: Color,
)

@Immutable
data class TimelineDimensions(
    val columnWidth: Dp,
    val badgeSize: Dp,
    val activeBadgeSize: Dp,
    val connectorWidth: Dp,
    val topPadding: Dp,
    val badgeSpacing: Dp,
    val connectorAnimMillis: Int,
    val badgeAnimMillis: Int,
    val badgeActivationDelayMillis: Int,
    val cardSwapEnterMillis: Int,
    val cardSwapExitMillis: Int,
    val cardSwapSizeMillis: Int,
)

object TimelineDefaults {
    @Composable
    fun colors(
        connectorTrack: Color = MaterialTheme.colorScheme.outlineVariant,
        connectorFill: Color = MaterialTheme.colorScheme.tertiary,
    ): TimelineColors = TimelineColors(connectorTrack, connectorFill)

    fun dimensions(
        columnWidth: Dp = 48.dp,
        badgeSize: Dp = 28.dp,
        activeBadgeSize: Dp = 40.dp,
        connectorWidth: Dp = 2.dp,
        topPadding: Dp = Spacing.sm,
        badgeSpacing: Dp = Spacing.xs,
        connectorAnimMillis: Int = 520,
        badgeAnimMillis: Int = 320,
        badgeActivationDelayMillis: Int = 400,
        cardSwapEnterMillis: Int = 320,
        cardSwapExitMillis: Int = 180,
        cardSwapSizeMillis: Int = 360,
    ): TimelineDimensions = TimelineDimensions(
        columnWidth = columnWidth,
        badgeSize = badgeSize,
        activeBadgeSize = activeBadgeSize,
        connectorWidth = connectorWidth,
        topPadding = topPadding,
        badgeSpacing = badgeSpacing,
        connectorAnimMillis = connectorAnimMillis,
        badgeAnimMillis = badgeAnimMillis,
        badgeActivationDelayMillis = badgeActivationDelayMillis,
        cardSwapEnterMillis = cardSwapEnterMillis,
        cardSwapExitMillis = cardSwapExitMillis,
        cardSwapSizeMillis = cardSwapSizeMillis,
    )
}

/**
 * Minimal fallback badge: filled circle for `Completed`/`Active`, outlined
 * circle with the row number for `Pending`. Override via the `badge` slot
 * on [TimelineColumn] when you need richer visuals (e.g. icons, pulse rings).
 */
@Composable
fun DefaultTimelineBadge(
    state: TimelineNodeState,
    number: Int,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onTertiary = MaterialTheme.colorScheme.onTertiary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val container by animateColorAsState(
        targetValue = if (state == TimelineNodeState.Pending) Color.Transparent else tertiary,
        animationSpec = tween(durationMillis = 320),
        label = "default_badge_container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (state == TimelineNodeState.Pending) outline else Color.Transparent,
        animationSpec = tween(durationMillis = 320),
        label = "default_badge_border",
    )
    val contentColor = if (state == TimelineNodeState.Pending) onSurfaceVariant else onTertiary
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

// ---------- Internals ----------

private fun nodeStateFor(index: Int, activeIndex: Int): TimelineNodeState = when {
    index < activeIndex -> TimelineNodeState.Completed
    index == activeIndex -> TimelineNodeState.Active
    else -> TimelineNodeState.Pending
}

private class TimelineEntry(
    val key: Any?,
    val collapsed: @Composable (TimelineItemScope) -> Unit,
    val expanded: (@Composable (TimelineItemScope) -> Unit)?,
)

private class TimelineScopeImpl : TimelineScope {
    val entries: MutableList<TimelineEntry> = mutableListOf()

    override fun <T> items(
        items: List<T>,
        key: ((item: T) -> Any)?,
        expanded: (@Composable TimelineItemScope.(item: T) -> Unit)?,
        collapsed: @Composable TimelineItemScope.(item: T) -> Unit,
    ) {
        items.forEach { item ->
            entries += TimelineEntry(
                key = key?.invoke(item),
                collapsed = { scope -> collapsed(scope, item) },
                expanded = expanded?.let { slot -> { scope -> slot(scope, item) } },
            )
        }
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        expanded: (@Composable TimelineItemScope.(index: Int) -> Unit)?,
        collapsed: @Composable TimelineItemScope.(index: Int) -> Unit,
    ) {
        repeat(count) { i ->
            entries += TimelineEntry(
                key = key?.invoke(i),
                collapsed = { scope -> collapsed(scope, i) },
                expanded = expanded?.let { slot -> { scope -> slot(scope, i) } },
            )
        }
    }
}

private class TimelineItemScopeImpl(
    override val index: Int,
    override val nodeState: TimelineNodeState,
    override val isExpanded: Boolean,
) : TimelineItemScope

@Composable
private fun TimelineRow(
    index: Int,
    nodeState: TimelineNodeState,
    isExpanded: Boolean,
    isLast: Boolean,
    colors: TimelineColors,
    dimensions: TimelineDimensions,
    badge: @Composable (state: TimelineNodeState, index: Int, size: Dp) -> Unit,
    collapsedContent: @Composable () -> Unit,
    expandedContent: (@Composable () -> Unit)?,
) {
    val expandable = expandedContent != null
    val grow = expandable && nodeState == TimelineNodeState.Active && isExpanded
    val animatedBadgeSize by animateDpAsState(
        targetValue = if (grow) dimensions.activeBadgeSize else dimensions.badgeSize,
        animationSpec = tween(
            durationMillis = dimensions.badgeAnimMillis,
            delayMillis = if (grow) dimensions.badgeActivationDelayMillis else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "timeline_badge_size",
    )
    val fillProgress by animateFloatAsState(
        targetValue = if (nodeState == TimelineNodeState.Completed) 1f else 0f,
        animationSpec = tween(
            durationMillis = dimensions.connectorAnimMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "timeline_connector_fill",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (isLast) return@drawBehind
                val centerX = dimensions.columnWidth.toPx() / 2
                val startY =
                    dimensions.topPadding.toPx() +
                            animatedBadgeSize.toPx() +
                            dimensions.badgeSpacing.toPx()
                val endY = size.height
                if (endY <= startY) return@drawBehind
                val stroke = dimensions.connectorWidth.toPx()
                drawLine(
                    color = colors.connectorTrack,
                    start = Offset(centerX, startY),
                    end = Offset(centerX, endY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                if (fillProgress > 0f) {
                    drawLine(
                        color = colors.connectorFill,
                        start = Offset(centerX, startY),
                        end = Offset(centerX, startY + (endY - startY) * fillProgress),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(dimensions.columnWidth)
                .padding(top = dimensions.topPadding),
        ) {
            Box(
                modifier = Modifier.size(animatedBadgeSize),
                contentAlignment = Alignment.Center,
            ) {
                badge(nodeState, index, animatedBadgeSize)
            }
        }
        AnimatedContent(
            targetState = isExpanded && expandable,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Spacing.xs),
            transitionSpec = {
                (fadeIn(tween(durationMillis = dimensions.cardSwapEnterMillis)) togetherWith
                        fadeOut(tween(durationMillis = dimensions.cardSwapExitMillis)))
                    .using(
                        SizeTransform { _, _ ->
                            tween(
                                durationMillis = dimensions.cardSwapSizeMillis,
                                easing = FastOutSlowInEasing,
                            )
                        },
                    )
            },
            label = "timeline_card_swap",
        ) { showExpanded ->
            if (showExpanded && expandedContent != null) {
                expandedContent()
            } else {
                collapsedContent()
            }
        }
    }
}
