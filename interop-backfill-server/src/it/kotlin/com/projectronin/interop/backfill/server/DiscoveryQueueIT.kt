package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.client.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.client.generated.models.UpdateDiscoveryEntry
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
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

        val entries = runBlocking { discoveryClient.getDiscoveryQueueEntries("tenantId") }

        assertNotNull(entries)
        assertEquals(2, entries.size)
        assertEquals(id, entries.first().backfillId)
        assertNotNull(entries.first().locationId)
    }

    @Test
    fun `get works by status and tenant and backfill `() {
        newBackFill()
        val id = newBackFill()

        val entries = runBlocking {
            discoveryClient.getDiscoveryQueueEntries("tenantId", DiscoveryQueueStatus.UNDISCOVERED, id)
        }

        assertNotNull(entries)
        assertEquals(2, entries.size)
        assertEquals(id, entries.first().backfillId)
        assertNotNull(entries.first().locationId)
    }

    @Test
    fun `get works by id`() {
        newBackFill()
        val entryId = discoveryDAO.getByTenant("tenantID").first().entryId
        val entry = runBlocking { discoveryClient.getDiscoveryQueueEntryById(entryId) }

        assertNotNull(entry)
    }

    @Test
    fun `get can return a 404`() {
        newBackFill()
        val result = runCatching {
            runBlocking { discoveryClient.getDiscoveryQueueEntryById(UUID.randomUUID()) }
        }

        assertTrue(result.isFailure)
        assertInstanceOf(ClientFailureException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `patch works`() {
        newBackFill()
        val entryId = discoveryDAO.getByTenant("tenantID").first().entryId
        runBlocking {
            discoveryClient.updateDiscoveryQueueEntryByID(entryId, UpdateDiscoveryEntry(DiscoveryQueueStatus.DISCOVERED))
        }
        val entries = discoveryDAO.getByTenant("tenantID")
        val discovered = entries.filter { it.status.toString() == DiscoveryQueueStatus.DISCOVERED.toString() }
        assertTrue(discovered.isNotEmpty())
        assertEquals(1, discovered.size)
        assertEquals(entryId, discovered.first().entryId)
    }

    @Test
    fun `delete works`() {
        newBackFill()
        val entryId = discoveryDAO.getByTenant("tenantID").first().entryId

        runBlocking { discoveryClient.deleteDiscoveryQueueEntryById(entryId) }

        val entries = discoveryDAO.getByTenant("tenantID")
        assertTrue(entries.isNotEmpty())
        assertEquals(1, entries.size)
        assertNotEquals(entryId, entries.first().entryId)
    }
}
