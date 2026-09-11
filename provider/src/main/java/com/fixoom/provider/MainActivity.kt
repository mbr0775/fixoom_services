package com.fixoom.provider

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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

    private lateinit var authViewModel:
            ProviderAuthViewModel

    private var passwordRecoveryLinkOpened by
    mutableStateOf(false)

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        authViewModel = ViewModelProvider(this)[
            ProviderAuthViewModel::class.java
        ]

        processAuthenticationLink(intent)

        enableEdgeToEdge()

        setContent {
            Fixoom_ServicesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProviderNavGraph(
                        passwordRecoveryLinkOpened =
                            passwordRecoveryLinkOpened,
                        onPasswordRecoveryLinkConsumed = {
                            passwordRecoveryLinkOpened = false
                        },

                        // These values will come from the
                        // provider_profiles table.
                        availability = null,
                        onAvailabilityChange = {
                            showDevelopmentMessage(
                                "Availability will be connected to the provider profile."
                            )
                        },

                        onOpenJobRequests = {
                            showDevelopmentMessage(
                                "Job requests require the bookings database."
                            )
                        },
                        onOpenActiveJobs = {
                            showDevelopmentMessage(
                                "Active jobs require the bookings database."
                            )
                        },
                        onOpenSchedule = {
                            showDevelopmentMessage(
                                "Schedule requires provider availability."
                            )
                        },
                        onOpenEarnings = {
                            showDevelopmentMessage(
                                "Earnings require completed-job payment records."
                            )
                        },
                        onOpenProfile = {
                            showDevelopmentMessage(
                                "Provider profile setup is the next main feature."
                            )
                        },
                        onOpenNotifications = {
                            showDevelopmentMessage(
                                "Notifications will be connected after bookings."
                            )
                        },

                        // Real legal URLs will be connected
                        // before Play Store release.
                        onOpenTerms = {
                            showDevelopmentMessage(
                                "Provider Terms are not published yet."
                            )
                        },
                        onOpenPrivacyPolicy = {
                            showDevelopmentMessage(
                                "Privacy Policy is not published yet."
                            )
                        },
                        onContactSupport = {
                            showDevelopmentMessage(
                                "Fixoom support details are not configured yet."
                            )
                        },

                        authViewModel = authViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(intent)

        setIntent(intent)
        processAuthenticationLink(intent)
    }

    private fun processAuthenticationLink(
        intent: Intent
    ) {
        val uri = intent.data ?: return

        if (
            uri.scheme != PROVIDER_AUTH_SCHEME ||
            uri.host != PROVIDER_AUTH_HOST
        ) {
            return
        }

        val isPasswordRecovery =
            uri.getQueryParameter("type") == "recovery" ||
                    uri.fragment
                        ?.split("&")
                        ?.any { value ->
                            value == "type=recovery"
                        } == true

        FixoomSupabase.client.handleDeeplinks(
            intent = intent,
            onSessionSuccess = {
                runOnUiThread {
                    if (isPasswordRecovery) {
                        passwordRecoveryLinkOpened = true
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
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun showDevelopmentMessage(
        message: String
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private companion object {
        const val PROVIDER_AUTH_SCHEME =
            "fixoom-provider"

        const val PROVIDER_AUTH_HOST =
            "auth"
    }
}