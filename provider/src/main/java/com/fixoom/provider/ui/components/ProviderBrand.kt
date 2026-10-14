package com.fixoom.provider.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixoom.provider.R
import com.fixoom.provider.ui.theme.FixoomLime
import com.fixoom.provider.ui.theme.FixoomTeal

@Composable
fun ProviderBrand(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(FixoomTeal, RoundedCornerShape(14.dp))) {
            Text("F", Modifier.align(Alignment.Center), color = Color.White,
                fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 9.dp)
                .size(6.dp).background(FixoomLime, RoundedCornerShape(1.dp)))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(stringResource(R.string.provider_brand), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
            Text(stringResource(R.string.provider_brand_label), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, letterSpacing = 1.2.sp)
        }
    }
}
