package dev.roasti

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import dev.roasti.feature.brew.data.alarm.BrewAlarmKeys

class MainActivity : AppCompatActivity() {

    // brewId из уведомления (deep-link). Обновляется в onCreate/onNewIntent, потребляется в App.
    private var pendingBrewId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingBrewId = intent.brewIdExtra()

        setContent {
            App(
                deepLinkBrewId = pendingBrewId,
                onDeepLinkConsumed = { pendingBrewId = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingBrewId = intent.brewIdExtra()
    }
}

private fun Intent.brewIdExtra(): String? = getStringExtra(BrewAlarmKeys.EXTRA_BREW_ID)

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}