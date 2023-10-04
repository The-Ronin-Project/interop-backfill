package com.projectronin.interop.backfill.server.controller

import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.DiscoveryQueueDAO
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.backfill.server.data.model.DiscoveryQueueDO
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.server.generated.models.UpdateDiscoveryEntry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID

class DiscoveryQueueControllerTest {
    private val backfillDao = mockk<BackfillDAO>()
    private val dao = mockk<DiscoveryQueueDAO>()

    private val controller = DiscoveryQueueController(backfillDao, dao)
    private val mockBackfill = mockk<BackfillDO> {
        every { tenantId } returns "da tenant"
        every { startDate } returns LocalDate.of(2020, 9, 1)
        every { endDate } returns LocalDate.of(2023, 9, 1)
    }

    @Test
    fun `getDiscoveryQueueEntries - works`() {
        val backfillID = UUID.randomUUID()
        val mockEntry1 = mockk<DiscoveryQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns UUID.randomUUID()
            every { locationId } returns "123"
            every { status } returns DiscoveryQueueStatus.DISCOVERED
        }
        val mockEntry2 = mockk<DiscoveryQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns UUID.randomUUID()
            every { locationId } returns "456"
            every { status } returns DiscoveryQueueStatus.UNDISCOVERED
        }
        every { dao.getByTenant(any(), any(), any()) } returns listOf(mockEntry1, mockEntry2)
        every { backfillDao.getByID(backfillID) } returns mockBackfill

        val result = controller.getDiscoveryQueueEntries("tenant", status = null, backfillId = null)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
    }

    @Test
    fun `getDiscoveryQueueEntryById - works`() {
        val backfillID = UUID.randomUUID()
        val entryID = UUID.randomUUID()
        val mockEntry = mockk<DiscoveryQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns entryID
            every { locationId } returns "123"
            every { status } returns DiscoveryQueueStatus.DISCOVERED
        }
        every { dao.getByID(entryID) } returns mockEntry
        every { backfillDao.getByID(backfillID) } returns mockBackfill

        val result = controller.getDiscoveryQueueEntryById(entryID)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(backfillID, result.body?.backfillId)
    }

    @Test
    fun `getDiscoveryQueueEntryById - returns 404`() {
        val entryID = UUID.randomUUID()
        every { dao.getByID(entryID) } returns null
        val result = controller.getDiscoveryQueueEntryById(entryID)
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `updateDiscoveryQueueEntryByID - works`() {
        val entryID = UUID.randomUUID()
        every { dao.updateStatus(entryID, DiscoveryQueueStatus.DISCOVERED) } just runs
        val result = controller.updateDiscoveryQueueEntryByID(entryID, UpdateDiscoveryEntry(DiscoveryQueueStatus.DISCOVERED))
        assertEquals(HttpStatus.OK, result.statusCode)
        assertTrue(result.body == true)
    }

    @Test
    fun `deleteDiscoveryQueueEntryById - works`() {
        val entryID = UUID.randomUUID()
        every { dao.updateStatus(entryID, DiscoveryQueueStatus.DELETED) } just runs
        val result = controller.deleteDiscoveryQueueEntryById(entryID)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertTrue(result.body == true)
    }
}
