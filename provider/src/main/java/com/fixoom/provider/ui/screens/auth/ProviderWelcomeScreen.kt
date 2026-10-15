package com.fixoom.provider.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixoom.provider.R
import com.fixoom.provider.ui.components.ProviderBrand
import com.fixoom.provider.ui.components.ProviderDepthHero
import com.fixoom.provider.ui.theme.FixoomLime
import com.fixoom.provider.ui.theme.FixoomTealDark

@Composable
fun ProviderWelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Scaffold(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(onClick = onCreateAccount,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FixoomLime, contentColor = FixoomTealDark)) {
                    Text(stringResource(R.string.provider_become), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp))
                }
                OutlinedButton(onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp)) {
                    Text(stringResource(R.string.provider_sign_in), fontWeight = FontWeight.SemiBold)
                }
                Text(stringResource(R.string.provider_approval_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("provider_welcome_scroll").verticalScroll(scroll)
            .padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            ProviderBrand(Modifier.padding(top = 14.dp))
            ProviderDepthHero(
                title = stringResource(R.string.provider_welcome_title),
                eyebrow = stringResource(R.string.provider_hero_eyebrow),
                caption = stringResource(R.string.provider_welcome_caption),
                scrollOffset = { scroll.value.toFloat() }
            )
            Column {
                Text(stringResource(R.string.provider_workflow_title),
                    style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(9.dp))
                Text(stringResource(R.string.provider_workflow_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WelcomeFeature(Icons.Rounded.Handyman, stringResource(R.string.provider_requests), Color(0xFFCDEED8), Modifier.weight(1f))
                WelcomeFeature(Icons.Rounded.CalendarMonth, stringResource(R.string.provider_schedule), Color(0xFFD9CCFF), Modifier.weight(1f))
                WelcomeFeature(Icons.Rounded.PersonOutline, stringResource(R.string.provider_profile), Color(0xFFFFE5D6), Modifier.weight(1f))
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, label: String, tint: Color, modifier: Modifier) {
    Surface(modifier, color = tint, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, null, tint = FixoomTealDark, modifier = Modifier.size(25.dp))
            Text(label, color = FixoomTealDark, style = MaterialTheme.typography.labelMedium)
        }
    }
}
