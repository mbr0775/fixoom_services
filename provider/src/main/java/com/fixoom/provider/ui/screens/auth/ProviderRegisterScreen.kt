package com.fixoom.provider.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fixoom.core.model.AuthResult
import com.fixoom.core.model.ServiceCategory
import com.fixoom.provider.ui.auth.ProviderAuthUiState
import com.fixoom.provider.ui.components.ProviderDepthHero

@Composable
fun ProviderRegisterScreen(
    uiState: ProviderAuthUiState,
    onRegister:
        (
            fullName: String,
            email: String,
            phone: String,
            password: String,
            skills: List<String>,
            skillDescriptions: Map<String, String>,
        ) -> Unit,
    onSignIn: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    categories: List<ServiceCategory> = emptyList(),
    categoryError: String? = null,
    onRetryCategories: () -> Unit = {},
) {
    var selectedSkills by rememberSaveable { mutableStateOf(listOf<String>()) }
    var skillDescriptions by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val focus = LocalFocusManager.current
    val scroll = rememberScrollState()
    var fullName by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var phone by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var passwordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var acceptedPolicies by rememberSaveable {
        mutableStateOf(false)
    }

    var localError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val backendError =
        when (val result = uiState.result) {
            is AuthResult.Error -> result.message
            else -> null
        }

    val canSubmit =
        fullName.isNotBlank() &&
            email.isNotBlank() &&
            phone.isNotBlank() &&
            password.length >= 8 &&
            confirmPassword.isNotBlank() &&
            acceptedPolicies &&
            selectedSkills.isNotEmpty() &&
            selectedSkills.all { skillDescriptions[it]?.trim()?.length in 10..2000 } &&
            !uiState.isLoading

    fun submitRegistration() {
        localError =
            when {
                fullName.trim().length < 2 -> "Please enter your full name."

                !email.contains("@") -> "Please enter a valid email address."

                phone.trim().length < 7 -> "Please enter a valid phone number."

                password.length < 8 -> "Password must contain at least 8 characters."

                password != confirmPassword -> "Passwords do not match."

                selectedSkills.isEmpty() -> "Choose at least one service skill."
                selectedSkills.any { skillDescriptions[it]?.trim()?.length !in 10..2000 } ->
                    "Describe your experience and services for every selected skill (10–2,000 characters)."
                !acceptedPolicies -> "Please accept the Terms and Privacy Policy."

                else -> null
            }

        if (localError == null) {
            onRegister(
                fullName.trim(),
                email.trim(),
                phone.trim(),
                password,
                selectedSkills,
                skillDescriptions.filterKeys { it in selectedSkills },
            )
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
                .imePadding()
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(
                        horizontal = 24.dp,
                        vertical = 16.dp,
                    )
        ) {
            IconButton(
                onClick = onBack,
                modifier =
                    Modifier.size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                        ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Go back",
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            ProviderDepthHero(
                "Make your skills\nwork for you.",
                "JOIN FIXOOM",
                "Describe your services, create your account and verify your email. Your account and offerings are reviewed separately.",
                { scroll.value.toFloat() },
                compact = true,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    localError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Full name")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = providerRegisterFieldColors(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Email address")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = providerRegisterFieldColors(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    localError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Phone number")
                },
                placeholder = {
                    Text("+94 77 123 4567")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Phone,
                        contentDescription = null,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = providerRegisterFieldColors(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Password")
                },
                supportingText = {
                    Text("Use at least 8 characters")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
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
                                },
                        )
                    }
                },
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                singleLine = true,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = providerRegisterFieldColors(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Confirm password")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                    )
                },
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            submitRegistration()
                        }
                    ),
                singleLine = true,
                enabled = !uiState.isLoading,
                isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                shape = RoundedCornerShape(18.dp),
                colors = providerRegisterFieldColors(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()
            Spacer(Modifier.height(20.dp))
            Text(
                "Your service skills",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Select up to 12 services and describe your experience for each. Your account and each skill need administrator approval before customers can book you.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (categoryError != null) {
                Text(categoryError, color = MaterialTheme.colorScheme.error)
                TextButton(onRetryCategories) { Text("Retry service categories") }
            } else if (categories.isEmpty()) {
                Text("Loading service categories...")
                TextButton(onRetryCategories) { Text("Refresh categories") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        category.slug in selectedSkills,
                        {
                            selectedSkills =
                                if (category.slug in selectedSkills) selectedSkills - category.slug
                                else selectedSkills + category.slug
                        },
                        label = { Text(category.name) },
                        enabled =
                            !uiState.isLoading &&
                                (category.slug in selectedSkills || selectedSkills.size < 12),
                    )
                }
            }
            categories
                .filter { it.slug in selectedSkills }
                .forEach { category ->
                    val description = skillDescriptions[category.slug].orEmpty()
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            if (it.length <= 2000)
                                skillDescriptions = skillDescriptions + (category.slug to it)
                            localError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${category.name} experience and services") },
                        supportingText = {
                            Text(
                                "Describe work you can do and your experience. ${description.trim().length}/2,000 characters · at least 10"
                            )
                        },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        isError = description.isNotEmpty() && description.trim().length < 10,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(18.dp),
                        colors = providerRegisterFieldColors(),
                    )
                }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = acceptedPolicies,
                    onCheckedChange = {
                        acceptedPolicies = it
                        localError = null
                    },
                    enabled = !uiState.isLoading,
                )

                Column(modifier = Modifier.padding(top = 5.dp)) {
                    Text(
                        text = "I agree to Fixoom’s provider policies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row {
                        TextButton(
                            onClick = onOpenTerms,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Text("Terms")
                        }

                        TextButton(
                            onClick = onOpenPrivacyPolicy,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                        ) {
                            Text("Privacy Policy")
                        }
                    }
                }
            }

            val displayedError = localError ?: backendError

            if (displayedError != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayedError,
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    submitRegistration()
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        text = "Create account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Already registered?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TextButton(
                    onClick = onSignIn,
                    enabled = !uiState.isLoading,
                ) {
                    Text(
                        text = "Sign in",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun providerRegisterFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        disabledContainerColor = Color.Transparent,
    )
