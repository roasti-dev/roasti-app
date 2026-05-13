package dev.roasti.ui.uikit.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roasti.R
import dev.roasti.ui.theme.Spacing

interface CommentComposerListener {
    fun onTextChange(text: String)
    fun onSubmit()
    fun onCancelMode()
}

@Composable
fun CommentComposer(
    text: String,
    isEditing: Boolean,
    replyingToAuthor: String?,
    isSubmitting: Boolean,
    listener: CommentComposerListener,
    modifier: Modifier = Modifier,
) {
    val isContextMode = isEditing || replyingToAuthor != null
    val canSubmit = text.isNotBlank() && !isSubmitting
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing, replyingToAuthor) {
        if (isEditing || replyingToAuthor != null) {
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            if (isContextMode) {
                ComposerContextBanner(
                    label = if (isEditing) {
                        stringResource(R.string.comments_composer_editing)
                    } else {
                        stringResource(
                            R.string.comments_composer_reply_to,
                            replyingToAuthor.orEmpty(),
                        )
                    },
                    enabled = !isSubmitting,
                    onCancel = listener::onCancelMode,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = listener::onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(stringResource(R.string.comments_input_placeholder))
                    },
                    enabled = !isSubmitting,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                SendIconButton(
                    isEditing = isEditing,
                    enabled = canSubmit,
                    isSubmitting = isSubmitting,
                    onClick = listener::onSubmit,
                )
            }
        }
    }
}

@Composable
private fun ComposerContextBanner(
    label: String,
    enabled: Boolean,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onCancel,
            enabled = enabled,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.comments_composer_cancel_label),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SendIconButton(
    isEditing: Boolean,
    enabled: Boolean,
    isSubmitting: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = contentColor,
                    disabledContentColor = contentColor,
                ),
            ) {
                Icon(
                    painter = painterResource(
                        if (isEditing) R.drawable.ic_check else R.drawable.ic_arrow_up,
                    ),
                    contentDescription = stringResource(
                        if (isEditing) R.string.comments_composer_save_label
                        else R.string.comments_composer_send_label,
                    ),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
