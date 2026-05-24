package dev.roasti.ui.uikit

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

@Composable
@ReadOnlyComposable
fun requiredLabel(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
        append(" *")
    }
}
