package dev.roasti

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.roasti.navigation.AppNavHost
import dev.roasti.ui.theme.RoastiTheme

@Composable
fun App() {
    RoastiTheme {
        AppNavHost()
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
