package com.fixoom.customer.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceFiltersTest {
    private data class Service(val title: String, val description: String)

    private val catalog = listOf(
        Service("Plumbing", "Leaks and pipe repairs"),
        Service("Electrical", "Wiring and electrical faults"),
        Service("Carpentry", "Furniture and woodwork"),
        Service("Painting", "Interior and exterior painting"),
        Service("Masonry", "Walls and cement repairs"),
        Service("Welding", "Gates and metal fabrication")
    )

    private fun results(query: String = "", filters: ServiceFilters = ServiceFilters()) =
        filterServiceCatalog(catalog, query, filters, { it.title }, { it.description }).map { it.title }

    @Test fun `default includes all services in original order`() {
        assertEquals(catalog.map { it.title }, results())
    }

    @Test fun `multiple categories are combined with OR`() {
        assertEquals(listOf("Plumbing", "Painting"), results(filters = ServiceFilters(setOf("Plumbing", "Painting"))))
    }

    @Test fun `search intersects selected categories`() {
        val filters = ServiceFilters(setOf("Plumbing", "Electrical"))
        assertEquals(listOf("Plumbing"), results("repairs", filters))
        assertTrue(results("painting", filters).isEmpty())
    }

    @Test fun `search matches titles and descriptions ignoring case and extra whitespace`() {
        assertEquals(listOf("Electrical"), results("  ELECTRICAL   wiring  "))
        assertEquals(listOf("Plumbing"), results("pipe\nleaks"))
        assertEquals(catalog.map { it.title }, results(" \t \n "))
    }

    @Test fun `ascending and descending sort preserve search and category filters`() {
        assertEquals(
            listOf("Carpentry", "Electrical", "Masonry", "Painting", "Plumbing", "Welding"),
            results(filters = ServiceFilters(sort = ServiceSort.NAME_ASCENDING))
        )
        assertEquals(
            listOf("Plumbing", "Masonry"),
            results("repairs", ServiceFilters(setOf("Plumbing", "Masonry"), ServiceSort.NAME_DESCENDING))
        )
    }

    @Test fun `unmatched search and unknown categories return empty without fallback`() {
        assertTrue(results("xyz-not-a-service").isEmpty())
        assertTrue(results(filters = ServiceFilters(setOf("Unknown"))).isEmpty())
    }

    @Test fun `toggle is immutable and toggling final selection off restores all services`() {
        val applied = ServiceFilters(setOf("Plumbing"))
        val draft = applied.toggleCategory("Electrical")
        assertEquals(setOf("Plumbing"), applied.categories)
        assertEquals(setOf("Plumbing", "Electrical"), draft.categories)
        assertEquals(catalog.map { it.title }, results(filters = applied.toggleCategory("Plumbing")))
    }

    @Test fun `badge counts selected categories and nondefault sort`() {
        assertEquals(0, ServiceFilters().activeCount)
        assertEquals(2, ServiceFilters(setOf("Plumbing", "Electrical")).activeCount)
        assertEquals(3, ServiceFilters(setOf("Plumbing", "Electrical"), ServiceSort.NAME_ASCENDING).activeCount)
        assertEquals(1, ServiceFilters(sort = ServiceSort.NAME_DESCENDING).activeCount)
    }

    @Test fun `reset returns default ordering and all categories`() {
        var filters = ServiceFilters(setOf("Plumbing"), ServiceSort.NAME_DESCENDING)
        assertEquals(listOf("Plumbing"), results(filters = filters))
        filters = ServiceFilters()
        assertEquals(0, filters.activeCount)
        assertEquals(catalog.map { it.title }, results(filters = filters))
    }

    @Test fun `empty catalog always returns no results`() {
        assertTrue(filterServiceCatalog(emptyList<Service>(), "", ServiceFilters(), { it.title }, { it.description }).isEmpty())
    }
}
