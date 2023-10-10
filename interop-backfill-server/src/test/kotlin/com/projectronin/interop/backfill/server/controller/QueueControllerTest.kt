package com.projectronin.interop.backfill.server.controller

import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.backfill.server.generated.models.NewQueueEntry
import com.projectronin.interop.backfill.server.generated.models.UpdateQueueEntry
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class QueueControllerTest {
    private val backfillDao = mockk<BackfillDAO>()
    private val dao = mockk<BackfillQueueDAO>()

    private val controller = QueueController(backfillDao, dao)
    private val mockBackfill = mockk<BackfillDO> {
        every { tenantId } returns "da tenant"
        every { startDate } returns LocalDate.of(2020, 9, 1)
        every { endDate } returns LocalDate.of(2023, 9, 1)
    }

    @Test
    fun `getEntriesByBackfillID - works`() {
        val backfillID = UUID.randomUUID()
        val mockEntry1 = mockk<BackfillQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns UUID.randomUUID()
            every { patientId } returns "123"
            every { status } returns BackfillStatus.NOT_STARTED
            every { updatedDateTime } returns OffsetDateTime.now()
        }
        val mockEntry2 = mockk<BackfillQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns UUID.randomUUID()
            every { patientId } returns "456"
            every { status } returns BackfillStatus.NOT_STARTED
            every { updatedDateTime } returns OffsetDateTime.now()
        }
        every { dao.getByBackfillID(backfillID) } returns listOf(mockEntry1, mockEntry2)
        every { backfillDao.getByID(backfillID) } returns mockBackfill
        val result = controller.getEntriesByBackfillID(backfillID)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
    }

    @Test
    fun `getQueueEntries - works`() {
        val backfillID = UUID.randomUUID()
        val mockEntry1 = mockk<BackfillQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns UUID.randomUUID()
            every { patientId } returns "123"
            every { status } returns BackfillStatus.NOT_STARTED
            every { updatedDateTime } returns OffsetDateTime.now()
        }
        val mockEntry2 = mockk<BackfillQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns UUID.randomUUID()
            every { patientId } returns "456"
            every { status } returns BackfillStatus.NOT_STARTED
            every { updatedDateTime } returns OffsetDateTime.now()
        }
        every { dao.getByTenant("tenant", status = BackfillStatus.STARTED) } returns emptyList()
        every { dao.getByTenant("tenant", status = BackfillStatus.NOT_STARTED) } returns listOf(mockEntry1, mockEntry2)
        every { backfillDao.getByID(backfillID) } returns mockBackfill
        every { dao.updateStatus(any(), BackfillStatus.STARTED) } just runs
        val result = controller.getQueueEntries("tenant")
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(2, result.body?.size)
    }

    @Test
    fun `getQueueEntries - returns nothing if there are still in progress entries`() {
        val backfillID = UUID.randomUUID()
        val mockEntry = mockk<BackfillQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns UUID.randomUUID()
            every { patientId } returns "123"
            every { status } returns BackfillStatus.STARTED
            every { updatedDateTime } returns OffsetDateTime.now()
        }
        every { dao.getByTenant("tenant", status = BackfillStatus.STARTED) } returns listOf(mockEntry)
        val result = controller.getQueueEntries("tenant")
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(0, result.body?.size)
    }

    @Test
    fun `getQueueEntryById - works`() {
        val backfillID = UUID.randomUUID()
        val entryID = UUID.randomUUID()
        val mockEntry = mockk<BackfillQueueDO> {
            every { backfillId } returns backfillID
            every { entryId } returns entryID
            every { patientId } returns "123"
            every { status } returns BackfillStatus.NOT_STARTED
            every { updatedDateTime } returns OffsetDateTime.now()
        }

        every { dao.getByID(entryID) } returns mockEntry
        every { backfillDao.getByID(backfillID) } returns mockBackfill
        val result = controller.getQueueEntryById(entryID)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(entryID, result.body?.id)
    }

    @Test
    fun `getQueueEntryById - 404`() {
        val entryID = UUID.randomUUID()
        every { dao.getByID(entryID) } returns null
        val result = controller.getQueueEntryById(entryID)
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `postQueueEntry - works`() {
        val backfillID = UUID.randomUUID()
        val entryID1 = UUID.randomUUID()
        val entryID2 = UUID.randomUUID()
        val newQueueEntry1 = NewQueueEntry(
            backfillId = backfillID,
            patientId = "123"
        )
        val newQueueEntry2 = NewQueueEntry(
            backfillId = backfillID,
            patientId = "456"
        )

        every { dao.getByBackfillID(backfillID) } returns emptyList()
        every { dao.insert(any()) } returns entryID1 andThen entryID2
        val result = controller.postQueueEntry(backfillID, listOf(newQueueEntry1, newQueueEntry2))
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(2, result.body?.size)
        assertEquals(true, result.body?.any { it.id == entryID1 })
        assertEquals(true, result.body?.any { it.id == entryID2 })
    }

    @Test
    fun `postQueueEntry - duplicate patients aren't stored`() {
        val backfillID = UUID.randomUUID()
        val entryID1 = UUID.randomUUID()
        val entryID2 = UUID.randomUUID()
        val newQueueEntry1 = NewQueueEntry(
            backfillId = backfillID,
            patientId = "123"
        )
        val newQueueEntry2 = NewQueueEntry(
            backfillId = backfillID,
            patientId = "456"
        )
        val newQueueEntry3 = NewQueueEntry(
            backfillId = backfillID,
            patientId = "456"
        )
        val newQueueEntry4 = NewQueueEntry(
            backfillId = backfillID,
            patientId = "existing"
        )
        val mockDO = mockk<BackfillQueueDO> {
            every { patientId } returns "existing"
        }

        every { dao.getByBackfillID(backfillID) } returns listOf(mockDO)
        every { dao.insert(any()) } returns entryID1 andThen entryID2
        val result = controller.postQueueEntry(backfillID, listOf(newQueueEntry1, newQueueEntry2, newQueueEntry3, newQueueEntry4))
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(2, result.body?.size)
        assertEquals(true, result.body?.any { it.id == entryID1 })
        assertEquals(true, result.body?.any { it.id == entryID2 })
    }

    @Test
    fun `updateQueueEntryByID - works`() {
        val entryID = UUID.randomUUID()
        every { dao.updateStatus(entryID, BackfillStatus.COMPLETED) } just runs
        val result = controller.updateQueueEntryByID(entryID, UpdateQueueEntry(BackfillStatus.COMPLETED))
        assertEquals(HttpStatus.OK, result.statusCode)
        assertTrue(result.body == true)
    }

    @Test
    fun `deleteQueueEntryById - works`() {
        val entryID = UUID.randomUUID()
        every { dao.updateStatus(entryID, BackfillStatus.DELETED) } just runs
        val result = controller.deleteQueueEntryById(entryID)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertTrue(result.body == true)
    }
}