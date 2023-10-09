package com.projectronin.interop.backfill.client

import com.projectronin.interop.backfill.client.generated.models.GeneratedId
import com.projectronin.interop.backfill.client.generated.models.NewQueueEntry
import com.projectronin.interop.backfill.client.generated.models.QueueEntry
import com.projectronin.interop.backfill.client.generated.models.UpdateQueueEntry
import com.projectronin.interop.backfill.client.spring.BackfillClientConfig
import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.request
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class QueueClient(
    private val client: HttpClient,
    backfillClientConfig: BackfillClientConfig,
    @Qualifier("backfill")
    private val authenticationService: InteropAuthenticationService
) {
    private val resourceUrl: String = "${backfillClientConfig.server.url}/queue"

    /**
     * Retrieves all [QueueEntry]s for a given [backfillId]
     */
    suspend fun getEntriesByBackfillID(backfillId: UUID): List<QueueEntry> {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/backfill/$backfillId"
        val response = client.request("BackFill", urlString) { url ->
            get(url) {
                bearerAuth(authentication.accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        return response.body()
    }

    /**
     * Retrieves all [QueueEntry]s for a given [tenantId] that are ready for processing.
     * If any entries are still being processed this returns nothing.
     */
    suspend fun getQueueEntries(tenantId: String): List<QueueEntry> {
        val authentication = authenticationService.getAuthentication()
        val response = client.request("BackFill", resourceUrl) { url ->
            get(url) {
                bearerAuth(authentication.accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("tenant_id", tenantId)
            }
        }
        return response.body()
    }

    /**
     * Retrieves a single [QueueEntry] object based on the [UUID]
     */
    suspend fun getQueueEntryById(queueId: UUID): QueueEntry {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$queueId"
        val response = client.request("BackFill", urlString) { url ->
            get(url) {
                bearerAuth(authentication.accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        return response.body()
    }

    /**
     * Creates new [QueueEntry]s. Returns a list of [GeneratedId]s for the new entries.
     */
    suspend fun postQueueEntry(backfillId: UUID, newQueueEntries: List<NewQueueEntry>): List<GeneratedId> {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/backfill/$backfillId"
        val response = client.request("BackFill", urlString) { url ->
            post(url) {
                bearerAuth(authentication.accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(newQueueEntries)
            }
        }
        return response.body()
    }

    /**
     * Updates a [QueueEntry]'s status based on the provided criteria. Returns true
     */
    suspend fun updateQueueEntryByID(queueId: UUID, updateQueueEntry: UpdateQueueEntry): Boolean {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$queueId"
        val response = client.request("BackFill", urlString) { url ->
            patch(url) {
                bearerAuth(authentication.accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(updateQueueEntry)
            }
        }
        return response.body()
    }

    /**
     * Marks a [QueueEntry]'s status as deleted. Returns true. Entry can still be found by direct
     * UUID lookup or lookup of [getEntriesByBackfillID]
     */
    suspend fun deleteQueueEntryById(queueId: UUID): Boolean {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$queueId"
        val response = client.request("BackFill", urlString) { url ->
            delete(url) {
                bearerAuth(authentication.accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        return response.body()
    }
}
