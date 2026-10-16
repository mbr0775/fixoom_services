package com.fixoom.provider.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import com.fixoom.marketplace.ui.rememberMarketplaceMotionAllowed

@Composable fun rememberProviderMotionAllowed(): Boolean = rememberMarketplaceMotionAllowed()

@Composable
internal fun providerFloat(enabled: Boolean): State<Float> {
    if (!enabled) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "provider_depth_motion")
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "provider_float",
    )
}
