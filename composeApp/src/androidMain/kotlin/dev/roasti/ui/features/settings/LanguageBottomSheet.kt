package dev.roasti.ui.features.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import dev.roasti.R
import dev.roasti.ui.uikit.RoastiBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageBottomSheet(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val applyLocale: (String) -> Unit = { tag ->
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
        onDismiss()
    }

    RoastiBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.language_sheet_title),
    ) {
        LanguageOptionRow(
            label = stringResource(R.string.settings_language_system_default),
            onClick = { applyLocale("") },
        )
        LanguageOptionRow(
            label = stringResource(R.string.settings_language_english),
            onClick = { applyLocale("en") },
        )
        LanguageOptionRow(
            label = stringResource(R.string.settings_language_russian),
            onClick = { applyLocale("ru") },
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            LanguageOptionRow(
                label = stringResource(R.string.settings_open_system_language),
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
