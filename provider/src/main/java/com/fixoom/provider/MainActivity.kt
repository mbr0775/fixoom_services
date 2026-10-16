package com.fixoom.provider

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.provider.navigation.ProviderNavGraph
import com.fixoom.provider.ui.auth.ProviderAuthViewModel
import com.fixoom.provider.ui.theme.Fixoom_ServicesTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: ProviderAuthViewModel

    private var passwordRecoveryLinkOpened by mutableStateOf(false)
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

        authViewModel = ViewModelProvider(this)[ProviderAuthViewModel::class.java]

        readNotification(intent)
        processAuthenticationLink(intent)

        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )

        setContent {
            Fixoom_ServicesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ProviderNavGraph(
                        passwordRecoveryLinkOpened = passwordRecoveryLinkOpened,
                        onPasswordRecoveryLinkConsumed = {
                            passwordRecoveryLinkOpened = false
                        },

                        // Real legal URLs will be connected
                        // before Play Store release.
                        onOpenTerms = {
                            showInformationMessage("Provider Terms are not published yet.")
                        },
                        onOpenPrivacyPolicy = {
                            showInformationMessage("Privacy Policy is not published yet.")
                        },
                        onContactSupport = ::openSupport,
                        authViewModel = authViewModel,
                        notificationBooking = notificationBooking,
                        onNotificationConsumed = { notificationBooking = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        readNotification(intent)
        processAuthenticationLink(intent)
    }

    private fun processAuthenticationLink(intent: Intent) {
        val uri = intent.data ?: return

        if (uri.scheme != PROVIDER_AUTH_SCHEME || uri.host != PROVIDER_AUTH_HOST) {
            return
        }
        setIntent(Intent(intent).setData(null))

        val isEmailConfirmation = uri.path == "/confirmed"
        if (isEmailConfirmation && uri.fragment?.contains("access_token=") != true) {
            // Older emails can arrive through the web handoff without a session.
            // Login still verifies the credentials and confirmation on the server.
            authViewModel.returnToSignIn()
            return
        }

        val isPasswordRecovery =
            uri.getQueryParameter("type") == "recovery" ||
                uri.fragment?.split("&")?.any { value ->
                    value == "type=recovery"
                } == true

        FixoomSupabase.client.handleDeeplinks(
            intent = intent,
            onSessionSuccess = {
                runOnUiThread {
                    if (isPasswordRecovery) {
                        passwordRecoveryLinkOpened = true
                    } else if (isEmailConfirmation) {
                        authViewModel.returnToSignIn()
                    } else {
                        authViewModel.restoreSession()
                    }
                }
            },
            onError = {
                runOnUiThread {
                    Toast.makeText(
                            this,
                            "Unable to process the authentication link.",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                }
            },
        )
    }

    private fun openSupport() {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_SENDTO,
                    android.net.Uri.parse("mailto:fixoomservices@gmail.com"),
                )
            )
        } catch (_: android.content.ActivityNotFoundException) {
            showInformationMessage("Contact Fixoom at fixoomservices@gmail.com.")
        }
    }

    private fun showInformationMessage(message: String) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT,
            )
            .show()
    }

    private companion object {
        const val PROVIDER_AUTH_SCHEME = "fixoom-provider"

        const val PROVIDER_AUTH_HOST = "auth"
    }
}
