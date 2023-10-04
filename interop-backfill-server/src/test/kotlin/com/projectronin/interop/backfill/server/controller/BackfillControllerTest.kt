package com.projectronin.interop.backfill.server.controller

import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.DiscoveryQueueDAO
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.backfill.server.data.model.DiscoveryQueueDO
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.server.generated.models.NewBackfill
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class BackfillControllerTest {
    private val dao = mockk<BackfillDAO>()
    private val queueDAO = mockk<BackfillQueueDAO>()
    private val discoveryDAO = mockk<DiscoveryQueueDAO>()

    private val controller = BackfillController(dao, queueDAO, discoveryDAO)

    @Test
    fun `getBackfillById - works`() {
        val mockDO = mockk<BackfillDO> {
            every { backfillId } returns UUID.randomUUID()
            every { tenantId } returns "da tenant"
            every { startDate } returns LocalDate.of(2022, 9, 1)
            every { endDate } returns LocalDate.of(2023, 9, 1)
            every { isDeleted } returns false
        }
        val mockDiscoveryQueueDO = mockk<DiscoveryQueueDO> {
            every { locationId } returns "da location"
        }
        val mockQueueDO = mockk<BackfillQueueDO> {
            every { status } returns BackfillStatus.COMPLETED
            every { updatedDateTime } returns OffsetDateTime.of(2023, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        }
        every { dao.getByID(any()) } returns mockDO
        every { queueDAO.getByBackfillID(any()) } returns listOf(mockQueueDO)
        every { discoveryDAO.getByBackfillID(any()) } returns listOf(mockDiscoveryQueueDO)
        val result = controller.getBackfillById(UUID.randomUUID())
        assertNotNull(result)
    }

    @Test
    fun `getBackfillById - status defaults to not started`() {
        val mockDO = mockk<BackfillDO> {
            every { backfillId } returns UUID.randomUUID()
            every { tenantId } returns "da tenant"
            every { startDate } returns LocalDate.of(2022, 9, 1)
            every { endDate } returns LocalDate.of(2023, 9, 1)
            every { isDeleted } returns false
        }
        val mockDiscoveryQueueDO = mockk<DiscoveryQueueDO> {
            every { locationId } returns "da location"
        }
        every { dao.getByID(any()) } returns mockDO
        every { queueDAO.getByBackfillID(any()) } returns emptyList()
        every { discoveryDAO.getByBackfillID(any()) } returns listOf(mockDiscoveryQueueDO)
        val result = controller.getBackfillById(UUID.randomUUID())
        assertNotNull(result)
        assertEquals(BackfillStatus.NOT_STARTED, result.body?.status)
    }

    @Test
    fun `getBackfillById - status is deleted when deleted`() {
        val mockDO = mockk<BackfillDO> {
            every { backfillId } returns UUID.randomUUID()
            every { tenantId } returns "da tenant"
            every { startDate } returns LocalDate.of(2022, 9, 1)
            every { endDate } returns LocalDate.of(2023, 9, 1)
            every { isDeleted } returns true
        }
        val mockDiscoveryQueueDO = mockk<DiscoveryQueueDO> {
            every { locationId } returns "da location"
        }
        every { dao.getByID(any()) } returns mockDO
        every { queueDAO.getByBackfillID(any()) } returns emptyList()
        every { discoveryDAO.getByBackfillID(any()) } returns listOf(mockDiscoveryQueueDO)
        val result = controller.getBackfillById(UUID.randomUUID())
        assertNotNull(result)
        assertEquals(BackfillStatus.DELETED, result.body?.status)
    }

    @Test
    fun `getBackfillById - status is started when there are differing statuses`() {
        val mockDO = mockk<BackfillDO> {
            every { backfillId } returns UUID.randomUUID()
            every { tenantId } returns "da tenant"
            every { startDate } returns LocalDate.of(2022, 9, 1)
            every { endDate } returns LocalDate.of(2023, 9, 1)
            every { isDeleted } returns false
        }
        val mockDiscoveryQueueDO = mockk<DiscoveryQueueDO> {
            every { locationId } returns "da location"
        }
        val mockQueueDO1 = mockk<BackfillQueueDO> {
            every { status } returns BackfillStatus.COMPLETED
            every { updatedDateTime } returns OffsetDateTime.of(2023, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        }
        val mockQueueDO2 = mockk<BackfillQueueDO> {
            every { status } returns BackfillStatus.NOT_STARTED
            every { updatedDateTime } returns OffsetDateTime.of(2023, 8, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        }
        every { dao.getByID(any()) } returns mockDO
        every { queueDAO.getByBackfillID(any()) } returns listOf(mockQueueDO1, mockQueueDO2)
        every { discoveryDAO.getByBackfillID(any()) } returns listOf(mockDiscoveryQueueDO)
        val result = controller.getBackfillById(UUID.randomUUID())
        assertNotNull(result)
        assertEquals(BackfillStatus.STARTED, result.body?.status)
    }

    @Test
    fun `getBackfillById - returns 404`() {
        every { dao.getByID(any()) } returns null
        val result = controller.getBackfillById(UUID.randomUUID())
        assertNotNull(result)
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `getBackfills - works`() {
        val mockDO = mockk<BackfillDO> {
            every { backfillId } returns UUID.randomUUID()
            every { tenantId } returns "da tenant"
            every { startDate } returns LocalDate.of(2022, 9, 1)
            every { endDate } returns LocalDate.of(2023, 9, 1)
            every { isDeleted } returns false
        }
        val mockDiscoveryQueueDO = mockk<DiscoveryQueueDO> {
            every { locationId } returns "da location"
        }
        val mockQueueDO = mockk<BackfillQueueDO> {
            every { status } returns BackfillStatus.COMPLETED
            every { updatedDateTime } returns OffsetDateTime.of(2023, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        }
        every { dao.getByTenant(any()) } returns listOf(mockDO)
        every { queueDAO.getByBackfillID(any()) } returns listOf(mockQueueDO)
        every { discoveryDAO.getByBackfillID(any()) } returns listOf(mockDiscoveryQueueDO)
        val result = controller.getBackfills("da tenant")
        assertNotNull(result)
        assertEquals(1, result.body?.size)
    }

    @Test
    fun `deleteBackfillById - works`() {
        val entry1Id = UUID.randomUUID()
        val entry2Id = UUID.randomUUID()
        every { dao.delete(any()) } just runs
        every { queueDAO.getByBackfillID(any()) } returns listOf(
            mockk {
                every { entryId } returns entry1Id
            }
        )
        every { queueDAO.updateStatus(entry1Id, BackfillStatus.DELETED) } just runs
        every { discoveryDAO.getByBackfillID(any()) } returns listOf(
            mockk {
                every { entryId } returns entry2Id
            }
        )
        every { discoveryDAO.updateStatus(entry2Id, DiscoveryQueueStatus.DELETED) } just runs
        val result = controller.deleteBackfillById(UUID.randomUUID())
        assertNotNull(result)
        assertTrue(result.body == true)
        assertEquals(HttpStatus.OK, result.statusCode)
    }

    @Test
    fun `postBackfill - works`() {
        val newBackfill = NewBackfill(
            locationIds = listOf("123", "456"),
            startDate = LocalDate.of(2020, 9, 1),
            endDate = LocalDate.of(2023, 9, 1),
            tenantId = "tenant"
        )
        val backfillID = UUID.randomUUID()
        every { dao.insert(match { it.tenantId == "tenant" }) } returns backfillID
        every { discoveryDAO.insert(match { it.backfillId == backfillID }) } returns backfillID
        val result = controller.postBackfill(newBackfill)
        assertNotNull(result)
        assertEquals(backfillID, result.body?.id)
        assertEquals(HttpStatus.CREATED, result.statusCode)
    }
}
