package com.fixoom.provider.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.provider.R
import com.fixoom.provider.ui.components.providerFloat
import com.fixoom.provider.ui.components.rememberProviderMotionAllowed
import com.fixoom.provider.ui.theme.FixoomLime
import com.fixoom.provider.ui.theme.FixoomTealDark

@Composable
fun ProviderSplashScreen(modifier: Modifier = Modifier) {
    val motion = rememberProviderMotionAllowed()
    val float = providerFloat(motion)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).graphicsLayer {
                rotationY = if (motion) float.value * 8f else 0f
                translationY = if (motion) float.value * 5.dp.toPx() else 0f
                cameraDistance = 18 * density
            }.background(FixoomLime, RoundedCornerShape(29.dp))) {
                Text("F", Modifier.align(Alignment.Center), color = FixoomTealDark,
                    fontSize = 66.sp, lineHeight = 76.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(26.dp))
            Text(stringResource(R.string.provider_brand), color = MaterialTheme.colorScheme.onBackground,
                fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Spacer(Modifier.height(7.dp))
            Text(stringResource(R.string.provider_brand_label), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
            Spacer(Modifier.height(22.dp))
            Text(stringResource(R.string.provider_splash_caption), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
            .width(110.dp).height(3.dp).clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant)
    }
}
