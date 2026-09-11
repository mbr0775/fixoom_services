package com.fixoom.customer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun HomeHero(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            bottomStart = 36.dp,
            bottomEnd = 36.dp
        ),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                top = 28.dp,
                end = 20.dp,
                bottom = 30.dp
            )
        ) {
            Text(
                text = "FIXOOM",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme
                    .colorScheme
                    .secondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "What can we fix for you?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme
                    .colorScheme
                    .onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Find trusted professionals for your home.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme
                    .colorScheme
                    .onPrimaryContainer
                    .copy(alpha = 0.78f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search plumbing, painting..."
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor =
                        MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor =
                        MaterialTheme.colorScheme.surface,
                    focusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor =
                        MaterialTheme.colorScheme.onSurface,
                    focusedLeadingIconColor =
                        MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}