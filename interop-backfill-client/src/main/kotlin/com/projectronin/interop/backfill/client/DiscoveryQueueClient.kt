package com.projectronin.interop.backfill.client

import com.projectronin.interop.backfill.client.generated.models.DiscoveryQueueEntry
import com.projectronin.interop.backfill.client.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.client.generated.models.UpdateDiscoveryEntry
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
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DiscoveryQueueClient(
    private val client: HttpClient,
    backfillClientConfig: BackfillClientConfig,
    @Qualifier("backfill")
    private val authenticationService: InteropAuthenticationService,
) {
    private val resourceUrl: String = "${backfillClientConfig.server.url}/discovery-queue"

    /**
     * Retrieves all [DiscoveryQueueEntry]s based on the provided criteria
     */
    suspend fun getDiscoveryQueueEntries(
        tenantId: String,
        status: DiscoveryQueueStatus? = null,
        backfillId: UUID? = null,
    ): List<DiscoveryQueueEntry> {
        val authentication = authenticationService.getAuthentication()
        val response =
            client.request("BackFill", resourceUrl) { url ->
                get(url) {
                    bearerAuth(authentication.accessToken)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    parameter("tenant_id", tenantId)
                    status?.let { parameter("status", status) }
                    backfillId?.let { parameter("backfill_id", backfillId) }
                }
            }
        return response.body()
    }

    /**
     * Retrieves a single [DiscoveryQueueEntry] object based on the [UUID]
     */
    suspend fun getDiscoveryQueueEntryById(discoveryQueueId: UUID): DiscoveryQueueEntry {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$discoveryQueueId"
        val response =
            client.request("BackFill", urlString) { url ->
                get(url) {
                    bearerAuth(authentication.accessToken)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
            }
        return response.body()
    }

    /**
     * Updates a [DiscoveryQueueEntry]'s status based on the supplied info. Returns true
     */
    suspend fun updateDiscoveryQueueEntryByID(
        discoveryQueueId: UUID,
        updateDiscoveryEntry: UpdateDiscoveryEntry,
    ): Boolean {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$discoveryQueueId"
        val response =
            client.request("BackFill", urlString) { url ->
                patch(url) {
                    bearerAuth(authentication.accessToken)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(updateDiscoveryEntry)
                }
            }
        return response.body()
    }

    /**
     * Marks a [DiscoveryQueueEntry]'s status as deleted. Returns true. Entry can still be found by direct
     * UUID lookup or lookup of [getDiscoveryQueueEntries] where the status is deleted
     */
    suspend fun deleteDiscoveryQueueEntryById(discoveryQueueId: UUID): Boolean {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$discoveryQueueId"
        val response =
            client.request("BackFill", urlString) { url ->
                delete(url) {
                    bearerAuth(authentication.accessToken)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
            }
        return response.body()
    }
}
