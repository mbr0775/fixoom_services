package com.fixoom.core.data.repository

import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.json.*

class ServiceEvidenceRepository(private val client: SupabaseClient = FixoomSupabase.client) {
    suspend fun attachments(contextId: String, complaint: Boolean): List<ServiceAttachment> =
        client
            .from("service_attachments")
            .select {
                filter { eq(if (complaint) "complaint_id" else "booking_id", contextId) }
                order("created_at", Order.ASCENDING)
                limit(100)
            }
            .decodeList()

    suspend fun complaints(bookingId: String?): List<ServiceComplaint> =
        client
            .from("complaints")
            .select {
                // Always scope explicitly, in addition to RLS; administrators use the web panel.
                filter {
                    eq(
                        "reporter_id",
                        client.auth.currentUserOrNull()?.id ?: error("Sign in required"),
                    )
                    if (bookingId != null) eq("booking_id", bookingId)
                }
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList()

    suspend fun submit(
        id: String,
        bookingId: String?,
        subject: String,
        description: String,
    ): ServiceComplaint {
        require(subject.trim().length in 5..120 && description.trim().length in 10..4000)
        return client.postgrest
            .rpc(
                "submit_complaint",
                buildJsonObject {
                    put("p_id", id)
                    put("p_booking_id", bookingId?.let(::JsonPrimitive) ?: JsonNull)
                    put("p_subject", subject.trim())
                    put("p_description", description.trim())
                },
            )
            .decodeAs()
    }

    suspend fun upload(
        contextId: String,
        complaint: Boolean,
        id: String,
        bytes: ByteArray,
        video: Boolean,
    ): ServiceAttachment {
        require(validMedia(bytes, video)) { "Choose a JPEG photo or MP4 video up to 25 MB." }
        val uid = client.auth.currentUserOrNull()?.id ?: error("Sign in required")
        val path = "$uid/$contextId/$id.${if (video) "mp4" else "jpg"}"
        require(PATH.matches(path))
        try {
            client.storage.from(BUCKET).upload(path, bytes) {
                contentType = if (video) ContentType.Video.MP4 else ContentType.Image.JPEG
                upsert = false
            }
        } catch (e: io.github.jan.supabase.exceptions.RestException) {
            // A timed-out first attempt may already have uploaded this immutable object.
            // Storage versions use 400 or 409 for duplicate objects. Publication
            // independently validates the uploaded object, ownership and context;
            // a rejected fresh upload cannot become a successful attachment.
            if (e.statusCode !in listOf(400, 409)) throw e
        }
        return client.postgrest
            .rpc(
                "publish_service_attachment",
                buildJsonObject {
                    put("p_id", id)
                    put("p_booking_id", if (complaint) JsonNull else JsonPrimitive(contextId))
                    put("p_complaint_id", if (complaint) JsonPrimitive(contextId) else JsonNull)
                    put("p_path", path)
                },
            )
            .decodeAs()
    }

    suspend fun preview(path: String): String {
        require(PATH.matches(path))
        return client.storage.from(BUCKET).createSignedUrl(path, 5.minutes)
    }

    companion object {
        const val BUCKET = "service-evidence"
        const val MAX_BYTES = 26_214_400
        private val PATH = Regex("[0-9a-f-]{36}/[0-9a-f-]{36}/[0-9a-f-]{36}[.](jpg|mp4)")

        fun validMedia(bytes: ByteArray, video: Boolean): Boolean =
            if (video)
                bytes.size in 12..MAX_BYTES &&
                    bytes.copyOfRange(4, 8).contentEquals("ftyp".toByteArray())
            else ProviderPhotoRepository.isValidUpload(bytes)
    }
}
