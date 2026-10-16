package com.fixoom.core.data.repository

import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class MarketplaceSnapshot(
    val profile: Profile? = null,
    val categories: List<ServiceCategory> = emptyList(),
    val professionals: List<ServiceProfessional> = emptyList(),
    val ownService: ServiceProfessional? = null,
    val bookings: List<ServiceBooking> = emptyList(),
    val skills: List<ProviderSkill> = emptyList(),
)

class MarketplaceRepository(private val client: SupabaseClient = FixoomSupabase.client) {

    suspend fun snapshot(): MarketplaceSnapshot = coroutineScope {
        client.auth.awaitInitialization()
        val profile = async { AuthRepository(client).getCurrentProfile() }
        val categories = async {
            client
                .from("service_categories")
                .select {
                    filter { eq("is_active", true) }
                    order("sort_order", Order.ASCENDING)
                }
                .decodeList<ServiceCategory>()
        }
        val professionals = async {
            client
                .from("provider_profiles")
                .select(Columns.raw("*,provider_skills(*)")) {
                    filter { eq("is_available", true) }
                    order("display_name", Order.ASCENDING)
                    limit(500)
                }
                .decodeList<ServiceProfessional>()
        }
        val identity = profile.await()
        val own =
            if (identity?.role == AppRole.PROVIDER)
                client
                    .from("provider_profiles")
                    .select(Columns.raw("*,provider_skills(*)")) {
                        filter { eq("id", identity.id) }
                    }
                    .decodeList<ServiceProfessional>()
                    .firstOrNull()
            else null

        val bookings =
            if (identity?.accountStatus == AccountStatus.ACTIVE)
                client
                    .from("bookings")
                    .select {
                        filter {
                            eq(
                                if (identity.role == AppRole.PROVIDER) "provider_id"
                                else "customer_id",
                                identity.id,
                            )
                        }
                        order("created_at", Order.DESCENDING)
                        limit(200)
                    }
                    .decodeList<ServiceBooking>()
            else emptyList()
        val enabled = categories.await().filter { it.isActive }
        val directory = professionals.await()
        MarketplaceSnapshot(
            identity,
            enabled,
            directory.filter { p ->
                p.isAvailable &&
                    p.displayName.isNotBlank() &&
                    p.serviceArea.isNotBlank() &&
                    enabled.any { p.offers(it.slug) }
            },
            own,
            bookings,
            (directory.flatMap { it.skills } + own?.skills.orEmpty()).distinctBy {
                it.providerId to it.categorySlug
            },
        )
    }

    suspend fun categories(): List<ServiceCategory> =
        client
            .from("service_categories")
            .select {
                filter { eq("is_active", true) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList()

    suspend fun requestSkill(category: String, description: String) {
        client.postgrest.rpc(
            "request_provider_skill",
            buildJsonObject {
                put("p_category_slug", category)
                put("p_description", description)
            },
        )
    }

    suspend fun withdrawSkill(category: String) {
        client.postgrest.rpc(
            "withdraw_provider_skill",
            buildJsonObject { put("p_category_slug", category) },
        )
    }

    suspend fun booking(id: String) =
        client
            .from("bookings")
            .select { filter { eq("id", id) } }
            .decodeList<ServiceBooking>()
            .firstOrNull()

    suspend fun events(id: String) =
        client
            .from("booking_events")
            .select {
                filter { eq("booking_id", id) }
                order("created_at", Order.ASCENDING)
                limit(200)
            }
            .decodeList<BookingEvent>()

    suspend fun messages(id: String) =
        client
            .from("booking_messages")
            .select {
                filter { eq("booking_id", id) }
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<BookingMessage>()
            .reversed()

    suspend fun saveProfessional(
        name: String,
        category: String,
        area: String,
        bio: String,
        available: Boolean,
    ) {
        client.postgrest.rpc(
            "save_provider_profile",
            buildJsonObject {
                put("p_display_name", name)
                put("p_category_slug", category)
                put("p_service_area", area)
                put("p_bio", bio)
                put("p_is_available", available)
            },
        )
    }

    suspend fun createBooking(
        key: String,
        provider: String,
        time: String,
        address: String,
        description: String,
        category: String,
        durationMinutes: Int = 60,
    ): ServiceBooking =
        client.postgrest
            .rpc(
                "create_service_booking_for_skill",
                buildJsonObject {
                    put("p_category_slug", category)
                    put("p_duration_minutes", durationMinutes)
                    put("p_request_id", key)
                    put("p_provider_id", provider)
                    put("p_scheduled_at", time)
                    put("p_address", address)
                    put("p_description", description)
                },
            )
            .decodeAs()

    suspend fun changeStatus(id: String, status: String) {
        client.postgrest.rpc(
            "change_booking_status",
            buildJsonObject {
                put("booking_id", id)
                put("next_status", status)
            },
        )
    }

    suspend fun complete(id: String, amount: Double) {
        client.postgrest.rpc(
            "complete_service_booking",
            buildJsonObject {
                put("p_booking_id", id)
                put("p_final_amount", amount)
            },
        )
    }

    suspend fun confirmPayment(id: String) {
        client.postgrest.rpc("confirm_direct_payment", buildJsonObject { put("p_booking_id", id) })
    }

    suspend fun sendMessage(id: String, body: String) {
        client.postgrest.rpc(
            "send_booking_message",
            buildJsonObject {
                put("p_booking_id", id)
                put("p_body", body)
            },
        )
    }

    suspend fun saveAccount(name: String, phone: String) {
        val normalizedPhone = ContactPhone.normalize(phone)
        require(phone.isBlank() || normalizedPhone != null) { "Enter a valid phone number." }
        val id = client.auth.currentUserOrNull()?.id ?: error("Sign in required")
        client
            .from("profiles")
            .update(
                buildJsonObject {
                    put("full_name", name.trim())
                    put("phone", normalizedPhone?.let(::JsonPrimitive) ?: JsonNull)
                }
            ) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<Profile>()
    }
}
