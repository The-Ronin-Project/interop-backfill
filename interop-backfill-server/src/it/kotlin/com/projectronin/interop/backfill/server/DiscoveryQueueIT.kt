package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueEntry
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.server.generated.models.UpdateDiscoveryEntry
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DiscoveryQueueIT : BaseBackfillIT() {
    private val urlPart = "/discovery-queue"

    @Test
    fun `get works`() {
        val id = newBackFill()
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("tenant_id", "tenantId")
            }
        }
        val entries = runBlocking { response.body<List<DiscoveryQueueEntry>>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(entries)
        assertEquals(2, entries.size)
        assertEquals(id.id, entries.first().backfillId)
        assertNotNull(entries.first().locationid)
    }

    @Test
    fun `get works by status and tenant and backfill `() {
        newBackFill()
        val id = newBackFill()
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("tenant_id", "tenantId")
                parameter("status", DiscoveryQueueStatus.UNDISCOVERED)
                parameter("backfillId", id.id.toString())
            }
        }
        val entries = runBlocking { response.body<List<DiscoveryQueueEntry>>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(entries)
        assertEquals(2, entries.size)
        assertEquals(id.id, entries.first().backfillId)
        assertNotNull(entries.first().locationid)
    }

    @Test
    fun `get works by id`() {
        newBackFill()
        val entryId = discoveryDAO.getByTenant("tenantID").first().entryId

        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart/$entryId") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val entry = runBlocking { response.body<DiscoveryQueueEntry>() }
        assertNotNull(entry)
    }

    @Test
    fun `get can return a 404`() {
        newBackFill()
        val response2 = runBlocking {
            httpClient.get("$serverUrl$urlPart/${UUID.randomUUID()}") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        assertEquals(HttpStatusCode.NotFound, response2.status)
    }

    @Test
    fun `patch works`() {
        newBackFill()
        val entryId = discoveryDAO.getByTenant("tenantID").first().entryId

        val response = runBlocking {
            httpClient.patch("$serverUrl$urlPart/$entryId") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(UpdateDiscoveryEntry(DiscoveryQueueStatus.DISCOVERED))
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val entries = discoveryDAO.getByTenant("tenantID")
        val discovered = entries.filter { it.status == DiscoveryQueueStatus.DISCOVERED }
        assertTrue(discovered.isNotEmpty())
        assertEquals(1, discovered.size)
        assertEquals(entryId, discovered.first().entryId)
    }

    @Test
    fun `delete works`() {
        newBackFill()
        val entryId = discoveryDAO.getByTenant("tenantID").first().entryId

        val response = runBlocking {
            httpClient.delete("$serverUrl$urlPart/$entryId") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)

        val entries = discoveryDAO.getByTenant("tenantID")
        assertTrue(entries.isNotEmpty())
        assertEquals(1, entries.size)
        assertNotEquals(entryId, entries.first().entryId)
    }
}
