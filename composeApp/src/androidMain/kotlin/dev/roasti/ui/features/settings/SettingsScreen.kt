package dev.roasti.ui.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Translate
import dev.roasti.R

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    SettingsScreen(onBackClick = onBackClick, contentPadding = contentPadding)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    var showLanguageSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowLeft,
                            contentDescription = stringResource(R.string.back_label),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(contentPadding),
        ) {
            SettingsRow(
                icon = painterResource(R.drawable.ic_language),
                title = stringResource(R.string.settings_language),
                onClick = { showLanguageSheet = true },
            )
        }

        if (showLanguageSheet) {
            LanguageBottomSheet(onDismiss = { showLanguageSheet = false })
        }
    }
}
