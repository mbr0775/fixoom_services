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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fixoom.core.model.AuthResult
import com.fixoom.provider.ui.auth.ProviderAuthUiState

@Composable
fun ProviderResetPasswordScreen(
    uiState: ProviderAuthUiState,
    onUpdatePassword: (newPassword: String) -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var passwordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var localError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val passwordUpdated =
        uiState.result is AuthResult.PasswordUpdated

    val backendError =
        (uiState.result as? AuthResult.Error)?.message

    val canSubmit =
        password.length >= 8 &&
                confirmPassword.isNotBlank() &&
                !uiState.isLoading

    fun submitPassword() {
        localError = when {
            password.length < 8 ->
                "Password must contain at least 8 characters."

            password != confirmPassword ->
                "Passwords do not match."

            else -> null
        }

        if (localError == null) {
            onUpdatePassword(password)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 42.dp
                )
        ) {
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
                        if (passwordUpdated) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.Password
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
                    if (passwordUpdated) {
                        "Password updated"
                    } else {
                        "Create a new password"
                    },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text =
                    if (passwordUpdated) {
                        "Your password was changed successfully. Sign in using your new password."
                    } else {
                        "Choose a strong password that you have not used for this account before."
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(34.dp))

            if (!passwordUpdated) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("New password")
                    },
                    supportingText = {
                        Text("Use at least 8 characters")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            }
                        ) {
                            Icon(
                                imageVector =
                                    if (passwordVisible) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                contentDescription =
                                    if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    }
                            )
                        }
                    },
                    visualTransformation =
                        if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(18.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Confirm new password")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null
                        )
                    },
                    visualTransformation =
                        if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            submitPassword()
                        }
                    ),
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    isError =
                        confirmPassword.isNotEmpty() &&
                                password != confirmPassword,
                    shape = RoundedCornerShape(18.dp)
                )

                val displayedError =
                    localError ?: backendError

                if (displayedError != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayedError,
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

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = {
                        submitPassword()
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
                        Text(
                            text = "Update password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onBackToSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "Continue to sign in",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
