package com.fixoom.customer.data.catalog

import java.util.Locale

enum class ServiceSort {
    DEFAULT,
    NAME_ASCENDING,
    NAME_DESCENDING
}

data class ServiceFilters(
    val categories: Set<String> = emptySet(),
    val sort: ServiceSort = ServiceSort.DEFAULT
) {
    // Search text and display layout are not counted as filters.
    val activeCount: Int
        get() = categories.size + if (sort == ServiceSort.DEFAULT) 0 else 1

    fun toggleCategory(category: String): ServiceFilters = copy(
        categories = if (category in categories) categories - category else categories + category
    )
}

/** Filters the service catalog, not provider prices, availability or location. */
fun <T> filterServiceCatalog(
    services: List<T>,
    query: String,
    filters: ServiceFilters,
    title: (T) -> String,
    description: (T) -> String
): List<T> {
    val words = query.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
    val matching = services.filter { service ->
        val matchesCategory = filters.categories.isEmpty() || title(service) in filters.categories
        val searchableText = "${title(service)} ${description(service)}"
        matchesCategory && words.all { searchableText.contains(it, ignoreCase = true) }
    }

    return when (filters.sort) {
        ServiceSort.DEFAULT -> matching
        ServiceSort.NAME_ASCENDING -> matching.sortedBy { title(it).lowercase(Locale.ROOT) }
        ServiceSort.NAME_DESCENDING -> matching.sortedByDescending { title(it).lowercase(Locale.ROOT) }
    }
}
