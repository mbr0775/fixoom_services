package com.fixoom.customer.ui.discovery

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fixoom.customer.R
import com.fixoom.customer.data.catalog.ServiceFilters
import com.fixoom.customer.data.catalog.ServiceSort
import com.fixoom.customer.data.catalog.filterServiceCatalog
import com.fixoom.customer.ui.theme.FixoomMint
import com.fixoom.customer.ui.theme.FixoomTeal
import com.fixoom.customer.ui.theme.FixoomTealDark
import com.fixoom.customer.ui.theme.Fixoom_ServicesTheme

internal data class ServiceCategoryUi(
    val title: String,
    val description: String,
    @param:DrawableRes @get:DrawableRes val iconRes: Int,
    val accent: Color
)

private enum class ServiceLayout { GRID, LIST }

private val FiltersSaver = listSaver<ServiceFilters, String>(
    save = { listOf(it.sort.name) + it.categories.sorted() },
    restore = { saved ->
        ServiceFilters(
            categories = saved.drop(1).toSet(),
            sort = ServiceSort.entries.firstOrNull { it.name == saved.firstOrNull() }
                ?: ServiceSort.DEFAULT
        )
    }
)

private fun List<ServiceCategoryUi>.matching(query: String, filters: ServiceFilters) =
    filterServiceCatalog(this, query, filters, { it.title }, { it.description })

@Composable
private fun selectionColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) FixoomTeal else FixoomMint

@Composable
private fun selectionContentColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) FixoomMint else FixoomTealDark

/** Owns applied state; the bottom sheet edits a separate draft until Apply is tapped. */
@Composable
internal fun HomeServiceDiscovery(
    services: List<ServiceCategoryUi>,
    onServiceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filters by rememberSaveable(stateSaver = FiltersSaver) { mutableStateOf(ServiceFilters()) }
    var layout by rememberSaveable { mutableStateOf(ServiceLayout.GRID) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val results = remember(services, query, filters) { services.matching(query, filters) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.discovery_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.discovery_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )
        ServiceSearchField(
            value = query,
            onValueChange = { query = it },
            activeCount = filters.activeCount,
            onFilterClick = {
                focusManager.clearFocus()
                showFilters = true
            },
            // Results already update as the user types. Never choose an arbitrary first match.
            onSearch = { focusManager.clearFocus() }
        )
        AnimatedVisibility(visible = filters.activeCount > 0) {
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                services.filter { it.title in filters.categories }.forEach { service ->
                    RemovableFilterChip(service.title) {
                        filters = filters.toggleCategory(service.title)
                    }
                }
                if (filters.sort != ServiceSort.DEFAULT) {
                    RemovableFilterChip(sortLabel(filters.sort)) {
                        filters = filters.copy(sort = ServiceSort.DEFAULT)
                    }
                }
                TextButton(onClick = { filters = ServiceFilters() }) {
                    Text(stringResource(R.string.discovery_clear_filters))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pluralStringResource(R.plurals.discovery_service_count, results.size, results.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
            )
            LayoutSelector(layout = layout, onSelect = { layout = it })
        }
        Column(modifier = Modifier.animateContentSize()) {
            if (results.isEmpty()) {
                EmptyServiceResults(onReset = {
                    query = ""
                    filters = ServiceFilters()
                })
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (layout == ServiceLayout.GRID) {
                        results.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { service ->
                                    ServiceResult(service, false, Modifier.weight(1f)) {
                                        onServiceClick(service.title)
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        results.forEach { service ->
                            ServiceResult(service, true, Modifier.fillMaxWidth()) {
                                onServiceClick(service.title)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ServiceFilterSheet(
            services = services,
            query = query,
            appliedFilters = filters,
            appliedLayout = layout,
            onApply = { newFilters, newLayout ->
                filters = newFilters
                layout = newLayout
                showFilters = false
            },
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun ServiceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    activeCount: Int,
    onFilterClick: () -> Unit,
    onSearch: () -> Unit
) {
    val searchDescription = stringResource(R.string.discovery_search_hint)
    val filterDescription = if (activeCount == 0) stringResource(R.string.discovery_open_filters)
    else pluralStringResource(R.plurals.discovery_open_active_filters, activeCount, activeCount)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .testTag("service_search").semantics { contentDescription = searchDescription },
                placeholder = { Text(stringResource(R.string.discovery_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = if (value.isNotEmpty()) {
                    {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.discovery_clear_search))
                        }
                    }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        BadgedBox(badge = {
            if (activeCount > 0) {
                Badge(
                    containerColor = FixoomMint,
                    contentColor = FixoomTealDark,
                    modifier = Modifier.clearAndSetSemantics { }
                ) { Text(activeCount.toString(), fontWeight = FontWeight.Bold) }
            }
        }) {
            Surface(
                onClick = onFilterClick,
                modifier = Modifier.size(56.dp).testTag("service_filters")
                    .semantics { contentDescription = filterDescription },
                shape = RoundedCornerShape(18.dp),
                color = FixoomTeal,
                contentColor = FixoomMint
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun RemovableFilterChip(label: String, onRemove: () -> Unit) {
    val removeDescription = stringResource(R.string.discovery_remove_filter, label)
    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        modifier = Modifier.semantics { contentDescription = removeDescription },
        trailingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectionColor(),
            selectedLabelColor = selectionContentColor(),
            selectedTrailingIconColor = selectionContentColor()
        ),
        border = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceFilterSheet(
    services: List<ServiceCategoryUi>,
    query: String,
    appliedFilters: ServiceFilters,
    appliedLayout: ServiceLayout,
    onApply: (ServiceFilters, ServiceLayout) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by rememberSaveable(stateSaver = FiltersSaver) { mutableStateOf(appliedFilters) }
    var draftLayout by rememberSaveable { mutableStateOf(appliedLayout) }
    val count = remember(services, query, draft) { services.matching(query, draft).size }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 780.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = selectionColor()) {
                    Icon(Icons.Rounded.Tune, null, Modifier.padding(12.dp), selectionContentColor())
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.discovery_filter_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.discovery_filter_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.discovery_close_filters))
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).testTag("service_filter_options"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                if (query.isNotBlank()) {
                    item {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                stringResource(R.string.discovery_search_context, query.trim()),
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterSectionHeading(R.string.discovery_categories, R.string.discovery_categories_hint)
                        services.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { service ->
                                    CategoryOption(
                                        service = service,
                                        selected = service.title in draft.categories,
                                        modifier = Modifier.weight(1f),
                                        onToggle = { draft = draft.toggleCategory(service.title) }
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterSectionHeading(R.string.discovery_sort_title, R.string.discovery_sort_hint)
                        Column(
                            modifier = Modifier.selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ServiceSort.entries.forEach { sort ->
                                SortOption(sort, draft.sort == sort) { draft = draft.copy(sort = sort) }
                            }
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.discovery_layout),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        LayoutSelector(draftLayout) { draftLayout = it }
                    }
                }
            }
            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    if (count == 0) {
                        Text(
                            stringResource(R.string.discovery_no_matches_hint),
                            modifier = Modifier.padding(bottom = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { draft = ServiceFilters(); draftLayout = ServiceLayout.GRID }) {
                            Text(stringResource(R.string.discovery_reset))
                        }
                        Button(
                            onClick = { onApply(draft, draftLayout) },
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("apply_service_filters"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FixoomTeal, contentColor = FixoomMint)
                        ) {
                            Text(
                                pluralStringResource(R.plurals.discovery_show_services, count, count),
                                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
                            )
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeading(titleRes: Int, subtitleRes: Int) {
    Column {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            stringResource(subtitleRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CategoryOption(service: ServiceCategoryUi, selected: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val background by animateColorAsState(
        if (selected) selectionColor() else MaterialTheme.colorScheme.surfaceVariant,
        label = "categorySelection"
    )
    val contentColor = if (selected) selectionContentColor() else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.testTag("filter_category_${service.title}").clip(RoundedCornerShape(18.dp))
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() }),
        color = background,
        contentColor = contentColor,
        shape = RoundedCornerShape(18.dp),
        border = if (selected) BorderStroke(1.dp, selectionContentColor().copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(service.iconRes), null, Modifier.size(32.dp))
                Spacer(Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = if (selected) FixoomTealDark else Color.Transparent,
                    border = BorderStroke(1.dp, contentColor.copy(alpha = 0.45f))
                ) {
                    if (selected) Icon(Icons.Rounded.Check, null, Modifier.padding(3.dp), FixoomMint)
                }
            }
            Text(service.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun sortLabel(sort: ServiceSort): String = stringResource(
    when (sort) {
        ServiceSort.DEFAULT -> R.string.discovery_sort_default
        ServiceSort.NAME_ASCENDING -> R.string.discovery_sort_az
        ServiceSort.NAME_DESCENDING -> R.string.discovery_sort_za
    }
)

@Composable
private fun SortOption(sort: ServiceSort, selected: Boolean, onSelect: () -> Unit) {
    val content = if (selected) selectionContentColor() else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) selectionColor() else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp).heightIn(min = 50.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(sortLabel(sort), Modifier.weight(1f).padding(vertical = 10.dp), color = content, style = MaterialTheme.typography.bodyMedium)
            RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = content))
        }
    }
}

@Composable
private fun LayoutSelector(layout: ServiceLayout, onSelect: (ServiceLayout) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(modifier = Modifier.padding(3.dp).selectableGroup()) {
            ServiceLayout.entries.forEach { option ->
                val selected = option == layout
                val description = stringResource(if (option == ServiceLayout.GRID) R.string.discovery_grid_view else R.string.discovery_list_view)
                Surface(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                        .selectable(selected, role = Role.RadioButton, onClick = { onSelect(option) })
                        .semantics { contentDescription = description },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) selectionColor() else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (option == ServiceLayout.GRID) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.ViewList,
                            contentDescription = null,
                            tint = if (selected) selectionContentColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceResult(service: ServiceCategoryUi, compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("service_result_${service.title}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        if (compact) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceImage(service)
                Column(modifier = Modifier.weight(1f)) { ServiceText(service) }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(modifier = Modifier.heightIn(min = 172.dp).padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ServiceImage(service)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(14.dp))
                ServiceText(service)
            }
        }
    }
}

@Composable
private fun ServiceImage(service: ServiceCategoryUi) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(16.dp), color = service.accent.copy(alpha = if (dark) 0.2f else 0.5f)) {
        Image(painterResource(service.iconRes), null, Modifier.padding(9.dp))
    }
}

@Composable
private fun ServiceText(service: ServiceCategoryUi) {
    Text(service.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Text(
        service.description,
        modifier = Modifier.padding(top = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmptyServiceResults(onReset: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.SearchOff, null, Modifier.size(32.dp), MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.discovery_no_matches), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.discovery_no_matches_hint), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onReset) { Text(stringResource(R.string.discovery_reset_search)) }
        }
    }
}

@Preview(name = "Service discovery - light", showBackground = true, widthDp = 360)
@Preview(name = "Service discovery - compact", showBackground = true, widthDp = 320, fontScale = 1.3f)
@Composable
private fun DiscoveryLightPreview() = DiscoveryPreview(dark = false)

@Preview(name = "Service discovery - dark", showBackground = true, widthDp = 360)
@Composable
private fun DiscoveryDarkPreview() = DiscoveryPreview(dark = true)

@Composable
private fun DiscoveryPreview(dark: Boolean) {
    Fixoom_ServicesTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeServiceDiscovery(
                services = listOf(
                    ServiceCategoryUi("Plumbing", "Leaks and pipe repairs", R.drawable.ic_service_plumbing, FixoomMint),
                    ServiceCategoryUi("Electrical", "Wiring and electrical faults", R.drawable.ic_service_electrical, FixoomMint)
                ),
                onServiceClick = {},
                modifier = Modifier.padding(vertical = 20.dp)
            )
        }
    }
}
