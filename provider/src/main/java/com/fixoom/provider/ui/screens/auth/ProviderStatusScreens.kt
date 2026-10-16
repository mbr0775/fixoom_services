package com.fixoom.provider.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixoom.marketplace.ui.WorkspaceDepthHeader

@Composable
fun ProviderEmailVerificationScreen(
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProviderStatusLayout(
        icon = Icons.Rounded.MarkEmailRead,
        iconColor = MaterialTheme.colorScheme.primary,
        title = "Check your email",
        description =
            "Verify your email to finish setting up your account. Check the confirmation email from Fixoom before signing in.",
        information =
            listOf(
                "Open the email sent by Fixoom.",
                "Tap the verification link.",
                "Return to the Provider app and sign in.",
            ),
        primaryButtonText = "Back to sign in",
        primaryButtonIcon = Icons.Rounded.MarkEmailRead,
        onPrimaryAction = onBackToSignIn,
        modifier = modifier,
    )
}

@Composable
fun ProviderPendingApprovalScreen(
    isRefreshing: Boolean,
    onRefreshStatus: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProviderStatusLayout(
        icon = Icons.Rounded.HourglassTop,
        iconColor = MaterialTheme.colorScheme.tertiary,
        title = "Application under review",
        description =
            "Your provider account was created successfully. The Fixoom team must verify your professional information before you can receive jobs.",
        information =
            listOf(
                "Complete your professional profile.",
                "Add a description for each skill in Skills & services.",
                "Your account and each skill need administrator approval.",
            ),
        primaryButtonText = "Check approval status",
        primaryButtonIcon = Icons.Rounded.Refresh,
        onPrimaryAction = onRefreshStatus,
        secondaryButtonText = "Sign out",
        secondaryButtonIcon = Icons.Rounded.Logout,
        onSecondaryAction = onSignOut,
        isLoading = isRefreshing,
        modifier = modifier,
    )
}

@Composable
fun ProviderSuspendedScreen(
    onContactSupport: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProviderStatusLayout(
        icon = Icons.Rounded.Block,
        iconColor = MaterialTheme.colorScheme.error,
        title = "Account suspended",
        description =
            "Access to this provider account has been temporarily suspended. Contact Fixoom support if you believe this is a mistake.",
        information =
            listOf(
                "You cannot accept new bookings.",
                "Existing access is restricted.",
                "Support can review your account status.",
            ),
        primaryButtonText = "Contact support",
        primaryButtonIcon = Icons.Rounded.SupportAgent,
        onPrimaryAction = onContactSupport,
        secondaryButtonText = "Back to sign in",
        secondaryButtonIcon = Icons.Rounded.Logout,
        onSecondaryAction = onBackToSignIn,
        modifier = modifier,
    )
}

@Composable
fun ProviderRejectedScreen(
    onContactSupport: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProviderStatusLayout(
        icon = Icons.Rounded.Cancel,
        iconColor = MaterialTheme.colorScheme.error,
        title = "Application not approved",
        description =
            "Your provider application could not be approved with the submitted information.",
        information =
            listOf(
                "Review the information you submitted.",
                "Contact support for the rejection reason.",
                "Support will explain whether you can apply again.",
            ),
        primaryButtonText = "Contact support",
        primaryButtonIcon = Icons.Rounded.SupportAgent,
        onPrimaryAction = onContactSupport,
        secondaryButtonText = "Back to sign in",
        secondaryButtonIcon = Icons.Rounded.Logout,
        onSecondaryAction = onBackToSignIn,
        modifier = modifier,
    )
}

@Composable
private fun ProviderStatusLayout(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    information: List<String>,
    primaryButtonText: String,
    primaryButtonIcon: ImageVector,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryButtonText: String? = null,
    secondaryButtonIcon: ImageVector? = null,
    onSecondaryAction: (() -> Unit)? = null,
    isLoading: Boolean = false,
) {
    val scroll = rememberScrollState()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding()
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(
                        horizontal = 24.dp,
                        vertical = 32.dp,
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WorkspaceDepthHeader(title, description, icon, { scroll.value.toFloat() })
            Spacer(Modifier.height(24.dp))

            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                information.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp).padding(top = 2.dp),
                            tint = iconColor,
                        )

                        Text(
                            text = item,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPrimaryAction,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = primaryButtonIcon,
                        contentDescription = null,
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        text = primaryButtonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (
                secondaryButtonText != null &&
                    secondaryButtonIcon != null &&
                    onSecondaryAction != null
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onSecondaryAction,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(
                        imageVector = secondaryButtonIcon,
                        contentDescription = null,
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        text = secondaryButtonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
