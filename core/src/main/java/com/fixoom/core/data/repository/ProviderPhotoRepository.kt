package com.fixoom.core.data.repository

import com.fixoom.core.data.remote.FixoomSupabase
import com.fixoom.core.model.ServiceProfessional
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProviderPhotoRepository(
    private val client: SupabaseClient = FixoomSupabase.client,
    private val customer: Boolean = false,
) {
    suspend fun save(jpeg: ByteArray?): String? {
        require(jpeg == null || isValidUpload(jpeg)) { "Choose a JPEG photo smaller than 1 MB." }
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull() ?: error("Sign in required")
        val previous =
            client
                .from(if (customer) "profiles" else "provider_profiles")
                .select { filter { eq("id", user.id) } }
                .decodeList<ServiceProfessional>()
                .firstOrNull()
                ?.avatarPath
        val path = jpeg?.let { "${user.id}/${UUID.randomUUID()}.jpg" }
        val bucket = client.storage.from(if (customer) CUSTOMER_BUCKET else BUCKET)
        if (path != null)
            bucket.upload(path, jpeg!!) {
                contentType = ContentType.Image.JPEG
                upsert = false
            }
        // Do not delete the new object if an RPC response is lost: the commit may
        // already have succeeded. A subsequent refresh resolves the saved path.
        client.postgrest.rpc(
            if (customer) "set_customer_photo" else "set_provider_photo",
            buildJsonObject {
                put("p_avatar_path", path?.let(::JsonPrimitive) ?: JsonNull)
            },
        )
        if (previous != null && previous != path) {
            try {
                bucket.delete(previous)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                /* The saved photo remains valid if old-file cleanup fails. */
            }
        }
        return path
    }

    suspend fun download(path: String): ByteArray {
        require(PATH.matches(path)) { "Invalid photo path" }
        return client.storage.from(if (customer) CUSTOMER_BUCKET else BUCKET).downloadAuthenticated(path)
    }

    companion object {
        const val BUCKET = "provider-photos"
        const val CUSTOMER_BUCKET = "customer-photos"
        const val MAX_BYTES = 1_048_576
        private val PATH = Regex("[0-9a-f-]{36}/[0-9a-f-]{36}\\.jpg")

        fun isValidUpload(bytes: ByteArray) =
            bytes.size in 4..MAX_BYTES &&
                bytes[0] == 0xff.toByte() &&
                bytes[1] == 0xd8.toByte() &&
                bytes[bytes.lastIndex - 1] == 0xff.toByte() &&
                bytes.last() == 0xd9.toByte()
    }
}
