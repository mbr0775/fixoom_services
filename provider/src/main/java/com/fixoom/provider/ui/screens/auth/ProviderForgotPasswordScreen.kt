package com.fixoom.provider.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fixoom.core.model.AuthResult
import com.fixoom.provider.ui.auth.ProviderAuthUiState

@Composable
fun ProviderForgotPasswordScreen(
    uiState: ProviderAuthUiState,
    onRequestReset: (email: String) -> Unit,
    onBackToSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }

    val emailSent =
        uiState.result is AuthResult.PasswordResetEmailSent

    val errorMessage =
        (uiState.result as? AuthResult.Error)?.message

    val canSubmit =
        email.isNotBlank() &&
                email.contains("@") &&
                !uiState.isLoading

    fun submitRequest() {
        if (canSubmit) {
            onRequestReset(email.trim())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                )
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Go back"
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(
                        color =
                            MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector =
                        if (emailSent) {
                            Icons.Rounded.MarkEmailRead
                        } else {
                            Icons.Rounded.Email
                        },
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint =
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text =
                    if (emailSent) {
                        "Check your email"
                    } else {
                        "Reset your password"
                    },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text =
                    if (emailSent) {
                        "If an account exists for this email address, Fixoom has sent a secure password recovery link."
                    } else {
                        "Enter the email address registered with your Provider account."
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(34.dp))

            if (!emailSent) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Email address")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            submitRequest()
                        }
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(18.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = errorMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color =
                                    MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        color =
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        submitRequest()
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color =
                                MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.size(10.dp))

                        Text(
                            text = "Send recovery email",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "For your security, Fixoom does not confirm whether an email address is registered.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color =
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(18.dp),
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = onBackToSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "Back to sign in",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!emailSent) {
                OutlinedButton(
                    onClick = onBackToSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "Return to sign in",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}