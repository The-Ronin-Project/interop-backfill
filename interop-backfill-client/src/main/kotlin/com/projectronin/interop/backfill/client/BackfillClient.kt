package com.projectronin.interop.backfill.client

import com.projectronin.interop.backfill.client.generated.models.Backfill
import com.projectronin.interop.backfill.client.generated.models.GeneratedId
import com.projectronin.interop.backfill.client.generated.models.NewBackfill
import com.projectronin.interop.backfill.client.generated.models.Order
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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BackfillClient(
    private val client: HttpClient,
    backfillClientConfig: BackfillClientConfig,
    @Qualifier("backfill")
    private val authenticationService: InteropAuthenticationService,
) {
    private val resourceUrl: String = "${backfillClientConfig.server.url}/backfill"

    /**
     * Retrieves a single [Backfill] object based on the [UUID]
     */
    suspend fun getBackfillById(backfillId: UUID): Backfill {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$backfillId"
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
     * Retrieves a page of [Backfill] objects based on the tenant id
     */
    suspend fun getBackfills(
        tenantId: String,
        order: Order? = null,
        limit: Int? = null,
        after: UUID? = null,
    ): List<Backfill> {
        val authentication = authenticationService.getAuthentication()
        val response =
            client.request("BackFill", resourceUrl) { url ->
                get(url) {
                    bearerAuth(authentication.accessToken)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    parameter("tenant_id", tenantId)
                    order?.let { parameter("order", it) }
                    limit?.let { parameter("limit", it) }
                    after?.let { parameter("after", it.toString()) }
                }
            }
        return response.body()
    }

    /**
     * Creates a new [Backfill] and returns the [GeneratedId],
     * server will also populate the DiscoveryQueueEntries for each location
     */
    suspend fun postBackfill(newBackfill: NewBackfill): GeneratedId {
        val authentication = authenticationService.getAuthentication()

        val response =
            client.request("BackFill", resourceUrl) { url ->
                post(url) {
                    bearerAuth(authentication.accessToken)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(newBackfill)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
            }
        return response.body()
    }

    /**
     * Marks all queue entries as DELETED and soft-deletes the actual backfill
     * Both can still be discovered by direct UUID lookup but will be hiden by broader searches
     */

    suspend fun deleteBackfill(backfillId: UUID): Boolean {
        val authentication = authenticationService.getAuthentication()
        val urlString = "$resourceUrl/$backfillId"
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
