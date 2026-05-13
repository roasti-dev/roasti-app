package dev.roasti.ui.uikit

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.roasti.ui.theme.RoastiTheme
import dev.roasti.ui.theme.Spacing

private val FieldShape = RoundedCornerShape(12.dp)
private val FieldMinHeight = 44.dp

@Composable
fun SearchInput(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!enabled && onClick != null) {
            ClickableSearchField(
                placeholder = placeholder,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            EditableSearchField(
                state = state,
                placeholder = placeholder,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EditableSearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    )

    SearchFieldSurface(
        borderColor = borderColor,
        modifier = modifier,
    ) {
        SearchLeadingIcon()

        Box(modifier = Modifier.weight(1f)) {
            if (state.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ClickableSearchField(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchFieldSurface(
        borderColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(FieldShape)
            .clickable { onClick() },
    ) {
        SearchLeadingIcon()
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchFieldSurface(
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = FieldMinHeight)
            .clip(FieldShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 1.dp, color = borderColor, shape = FieldShape)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content,
    )
}

@Composable
private fun SearchLeadingIcon() {
    // TODO: replace with magnifier drawable
    Text(
        text = "⌕",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchInputIdlePreview() {
    RoastiTheme {
        SearchInput(
            state = rememberTextFieldState(),
            placeholder = "Search blends, roasts...",
            label = "Search",
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchInputDisabledPreview() {
    RoastiTheme {
        SearchInput(
            state = rememberTextFieldState(),
            placeholder = "Search blends, roasts...",
            label = "Search",
            enabled = false,
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
        )
    }
}
