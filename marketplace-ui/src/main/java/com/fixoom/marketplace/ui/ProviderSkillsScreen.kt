package com.fixoom.marketplace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fixoom.core.data.repository.MarketplaceState
import com.fixoom.core.model.serviceStatus
import kotlinx.coroutines.launch

@Composable
fun ProviderSkillsScreen(
    state: MarketplaceState,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onSubmit: (String, String, () -> Unit) -> Unit,
    onWithdraw: (String) -> Unit,
) {
    val focus = LocalFocusManager.current
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val skills = state.data.ownService?.skills.orEmpty()
    var selected by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var withdraw by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    MarketplacePage("Skills & services", state, onRefresh, onBack, listState = list) {
        item {
            WorkspaceDepthHeader(
                "More skills.\nMore possibilities.",
                "Every service is reviewed by your administrator before customers can book it.",
                Icons.Rounded.Handyman,
                { list.firstVisibleItemScrollOffset.toFloat() },
            )
        }
        item {
            InfoCard(
                "Add or update a skill request",
                "Describe what you can offer in each category. Account approval and skill approval are separate.",
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.data.categories
                        .filter { c ->
                            skills.none { it.categorySlug == c.slug && it.status == "approved" }
                        }
                        .forEach { c ->
                            FilterChip(
                                selected == c.slug,
                                {
                                    selected = c.slug
                                    description =
                                        skills
                                            .find { it.categorySlug == c.slug }
                                            ?.description
                                            .orEmpty()
                                    error = null
                                },
                                label = { Text(c.name) },
                                modifier = Modifier.testTag("request_skill_${c.slug}"),
                                enabled = !state.saving,
                            )
                        }
                }
                if (state.data.categories.isEmpty())
                    Text("No active service categories are available. Refresh or contact support.")
                OutlinedTextField(
                    description,
                    { if (it.length <= 2000) description = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Experience and service offered") },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    enabled = !state.saving,
                    shape = RoundedCornerShape(16.dp),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    {
                        error =
                            if (selected.isBlank() || description.trim().length < 10)
                                "Choose a category and describe your service in at least 10 characters."
                            else null
                        if (error == null)
                            onSubmit(selected, description) {
                                selected = ""
                                description = ""
                            }
                    },
                    enabled = !state.saving && state.data.ownService != null,
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit for approval")
                }
            }
        }
        if (skills.none { it.status != "withdrawn" })
            item {
                InfoCard(
                    "Your first skill starts here",
                    "Select the services you actually offer. Submitted skills remain private until approved.",
                )
            }
        items(skills.filter { it.status != "withdrawn" }, key = { it.categorySlug }) { skill ->
            val category =
                state.data.categories.find { it.slug == skill.categorySlug }?.name
                    ?: serviceStatus(skill.categorySlug)
            InfoCard(
                category,
                skill.description.ifBlank {
                    "No experience or service details supplied yet. Use Update request to help the administrator review this skill."
                },
            ) {
                Text(
                    serviceStatus(skill.status),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (skill.status != "approved" && skill.description.trim().length < 10)
                    Text(
                        "Help your administrator review this service: use Update request to add at least 10 characters about your experience and services.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                if (skill.status == "approved") {
                    if (state.data.ownService?.categorySlug == skill.categorySlug)
                        Text("Primary skill", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "To change an approved offering, remove it and submit it again for review. Existing bookings stay in your history.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else
                    TextButton(
                        {
                            selected = skill.categorySlug
                            description = skill.description
                            scope.launch { list.animateScrollToItem(1) }
                        },
                        enabled = !state.saving,
                    ) {
                        Text("Update request")
                    }
                TextButton({ withdraw = skill.categorySlug }, enabled = !state.saving) {
                    Text(if (skill.status == "approved") "Remove service" else "Withdraw request")
                }
            }
        }
    }
    withdraw?.let { category ->
        AlertDialog(
            onDismissRequest = { withdraw = null },
            title = { Text("Remove this skill?") },
            text = {
                Text(
                    "Customers will no longer be able to book this skill. Existing jobs remain in your history. Adding it again requires administrator approval."
                )
            },
            confirmButton = {
                TextButton(
                    {
                        withdraw = null
                        onWithdraw(category)
                    },
                    enabled = !state.saving,
                ) {
                    Text("Remove")
                }
            },
            dismissButton = { TextButton({ withdraw = null }) { Text("Cancel") } },
        )
    }
}
