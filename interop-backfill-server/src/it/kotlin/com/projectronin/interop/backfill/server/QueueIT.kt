package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.client.generated.models.BackfillStatus
import com.projectronin.interop.backfill.client.generated.models.NewQueueEntry
import com.projectronin.interop.backfill.client.generated.models.Order
import com.projectronin.interop.backfill.client.generated.models.UpdateQueueEntry
import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class QueueIT : BaseBackfillIT() {
    @Test
    fun `get works`() {
        val id = newBackFill()
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.COMPLETED)

        val result = runBlocking { queueClient.getQueueEntries("tenantId") }

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(id, result.first().backfillId)
        assertEquals(BackfillStatus.NOT_STARTED, result.first().status)
    }

    @Test
    fun `get works with default queueSize returns empty list queueSize equals started-size`() {
        val id = newBackFill()
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.STARTED)

        val result = runBlocking { queueClient.getQueueEntries("tenantId") }

        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `get works with default queueSize returns 1`() {
        val id = newBackFill()
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.COMPLETED)

        val result = runBlocking { queueClient.getQueueEntries("tenantId") }

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(BackfillStatus.NOT_STARTED, result.first().status)
    }

    @Test
    fun `get works with queueSize returns 4`() {
        val id = newBackFill()
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.STARTED)

        val result = runBlocking { queueClient.getQueueEntries("tenantId", 5) }

        assertNotNull(result)
        assertEquals(4, result.size)
        result.all { it.status == BackfillStatus.NOT_STARTED }
    }

    @Test
    fun `get returns nothing with an started entry`() {
        val id = newBackFill()
        newPatientQueue(id, BackfillStatus.STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)

        val result = runBlocking { queueClient.getQueueEntries("tenantId") }

        val entries = queueDAO.getByBackfillID(id)
        assertNotNull(result)
        assertEquals(0, result.size)
        assertEquals(2, entries.size)
        assertEquals(id, entries.first().backfillId)
        assertNotNull(entries.filter { it.status.toString() == BackfillStatus.STARTED.toString() })
        assertNotNull(entries.filter { it.status.toString() == BackfillStatus.NOT_STARTED.toString() })
    }

    @Test
    fun `get works by backfill `() {
        val id = newBackFill()
        newPatientQueue(id, BackfillStatus.STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)

        val result = runBlocking { queueClient.getEntriesByBackfillID(id, Order.ASC, 10, null) }

        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(id, result.first().backfillId)
        assertNotNull(result.filter { it.status == BackfillStatus.STARTED })
        assertNotNull(result.filter { it.status == BackfillStatus.NOT_STARTED })
    }

    @Test
    fun `get works by id`() {
        val id = newBackFill()
        val entryId = newPatientQueue(id, BackfillStatus.STARTED)

        val result = runBlocking { queueClient.getQueueEntryById(entryId) }

        assertEquals(id, result.backfillId)
        assertEquals(BackfillStatus.STARTED, result.status)
    }

    @Test
    fun `get can return a 404`() {
        val id = newBackFill()
        newPatientQueue(id, BackfillStatus.STARTED)

        val result = runCatching { runBlocking { queueClient.getQueueEntryById(UUID.randomUUID()) } }

        assertTrue(result.isFailure)
        assertInstanceOf(ClientFailureException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `post works`() {
        val id = newBackFill()
        val newEntry =
            NewQueueEntry(
                backfillId = id,
                patientId = "123",
            )

        val result = runBlocking { queueClient.postQueueEntry(id, listOf(newEntry)) }

        val entries = queueDAO.getByBackfillID(id)
        assertNotNull(entries)
        assertEquals(1, entries.size)
        assertEquals(1, result.size)
        assertEquals(id, entries.first().backfillId)
        assertEquals(result.first().id, entries.first().entryId)
        assertNotNull("123", entries.first().patientId)
    }

    @Test
    fun `patch works`() {
        val id = newBackFill()
        val entryId = newPatientQueue(id, BackfillStatus.STARTED)

        val result =
            runBlocking { queueClient.updateQueueEntryByID(entryId, UpdateQueueEntry(BackfillStatus.COMPLETED)) }

        val entry = queueDAO.getByID(entryId)!!
        assertTrue(result)
        assertEquals(BackfillStatus.COMPLETED.toString(), entry.status.toString())
    }

    @Test
    fun `delete works`() {
        val id = newBackFill()
        val entryId = newPatientQueue(id, BackfillStatus.STARTED)

        val result = runBlocking { queueClient.deleteQueueEntryById(entryId) }

        val entry = queueDAO.getByID(entryId)
        assertTrue(result)
        assertEquals(BackfillStatus.DELETED.toString(), entry?.status.toString())
    }

    private fun newPatientQueue(
        backfillID: UUID,
        entryStatus: BackfillStatus = BackfillStatus.STARTED,
    ): UUID {
        return queueDAO.insert(
            BackfillQueueDO {
                backfillId = backfillID
                entryId = UUID.randomUUID()
                patientId = Random(10).toString()
                status =
                    com.projectronin.interop.backfill.server.generated.models.BackfillStatus.valueOf(entryStatus.toString())
            },
        )!!
    }
}
