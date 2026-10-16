package com.fixoom.customer

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.data.repository.AuthRepository
import com.fixoom.customer.data.ThemePreferences
import com.fixoom.customer.navigation.NavGraph
import com.fixoom.customer.ui.components.BottomNavigationBar
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var authReturn by mutableIntStateOf(0)
    private var notificationBooking by mutableStateOf<String?>(null)

    private fun readNotification(incoming: Intent) {
        incoming
            .getStringExtra("fixoom_booking_id")
            ?.takeIf { runCatching { java.util.UUID.fromString(it) }.isSuccess }
            ?.let {
                notificationBooking = it
                incoming.removeExtra("fixoom_booking_id")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        readNotification(intent)
        processAuthenticationLink(intent)

        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)

            val coroutineScope = rememberCoroutineScope()

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkMode
                    isAppearanceLightNavigationBars = !isDarkMode
                }
            }

            Fixoom_ServicesTheme(darkTheme = isDarkMode) {
                FixoomApp(
                    authReturn = authReturn,
                    notificationBooking = notificationBooking,
                    onNotificationConsumed = { notificationBooking = null },
                    isDarkMode = isDarkMode,
                    onDarkModeChange = { enabled ->
                        coroutineScope.launch {
                            themePreferences.setDarkMode(enabled)
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readNotification(intent)
        processAuthenticationLink(intent)
    }

    private fun processAuthenticationLink(incoming: Intent) {
        val uri = incoming.data ?: return
        if (uri.scheme != "fixoom-customer" || uri.host != "auth" || uri.path != "/confirmed")
            return
        setIntent(Intent(incoming).setData(null))
        val openSignIn: () -> Unit = {
            lifecycleScope.launch {
                AuthRepository().returnToSignIn()
                authReturn++
            }
        }
        if (uri.fragment?.contains("access_token=") == true) {
            FixoomSupabase.client.handleDeeplinks(
                intent = incoming,
                onSessionSuccess = { runOnUiThread { openSignIn() } },
                onError = {
                    runOnUiThread {
                        Toast.makeText(
                                this,
                                "Unable to open the confirmation link. Use the latest email and sign in.",
                                Toast.LENGTH_LONG,
                            )
                            .show()
                    }
                },
            )
        } else openSignIn()
    }
}

@Composable
fun FixoomApp(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    authReturn: Int = 0,
    notificationBooking: String? = null,
    onNotificationConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
    ) { innerPadding ->
        NavGraph(
            authReturn = authReturn,
            notificationBooking = notificationBooking,
            onNotificationConsumed = onNotificationConsumed,
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            isDarkMode = isDarkMode,
            onDarkModeChange = onDarkModeChange,
        )
    }
}
