package com.projectronin.interop.backfill.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.backfill.server.data.model.UndiscoveredQueueDO
import com.projectronin.interop.backfill.server.generated.models.UndiscoveredQueueStatus
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

@LiquibaseTest(changeLog = "backfill/db/changelog/backfill.db.changelog-master.yaml")
class UndiscoveredQueueDAOTest {

    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/BaseBackfill.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/undiscoveredqueue/SingleQueueEntry.yaml"], ignoreCols = ["entry_id"])
    fun `insert works`() {
        val dao = UndiscoveredQueueDAO(KtormHelper.database())
        val newEntry = UndiscoveredQueueDO {
            backfillId = UUID.fromString("b4e8e80a-297a-4b19-bd59-4b8072db9cc4")
            locationId = "123"
            status = UndiscoveredQueueStatus.UNDISCOVERED
        }
        val uuid = dao.insert(newEntry)
        assertNotNull(uuid)
    }

    @Test
    @DataSet(value = ["/dbunit/undiscoveredqueue/MultipleQueueEntries.yaml"], cleanAfter = true)
    fun `getByID works`() {
        val dao = UndiscoveredQueueDAO(KtormHelper.database())
        val entry = dao.getByID(UUID.fromString("981d2048-eb49-4bfd-ba96-8291288641c3"))
        assertNotNull(entry)
        assertEquals("123", entry?.locationId)
    }

    @Test
    @DataSet(value = ["/dbunit/undiscoveredqueue/MultipleQueueEntries.yaml"], cleanAfter = true)
    fun `getByID return null`() {
        val dao = UndiscoveredQueueDAO(KtormHelper.database())
        val entry = dao.getByID(UUID.fromString("b4e8e80a-297a-4b19-bd59-4b8072db9cc5"))
        assertNull(entry)
    }

    @Test
    @DataSet(value = ["/dbunit/undiscoveredqueue/MultipleQueueEntries.yaml"], cleanAfter = true)
    fun `getByTenant works`() {
        val dao = UndiscoveredQueueDAO(KtormHelper.database())
        val entry = dao.getByTenant("tenant1")
        assertEquals(2, entry.size)
    }

    @Test
    @DataSet(value = ["/dbunit/undiscoveredqueue/MultipleQueueEntries.yaml"], cleanAfter = true)
    fun `getByTenant works with filtering`() {
        val dao = UndiscoveredQueueDAO(KtormHelper.database())
        val entry = dao.getByTenant("tenant1", UndiscoveredQueueStatus.DISCOVERED)
        assertEquals(1, entry.size)
        assertEquals("456", entry.first().locationId)
    }

    @Test
    @DataSet(value = ["/dbunit/undiscoveredqueue/MultipleQueueEntries.yaml"], cleanAfter = true)
    fun `getByBackfillID works `() {
        val dao = UndiscoveredQueueDAO(KtormHelper.database())
        val entry = dao.getByBackfillID(UUID.fromString("b4e8e80a-297a-4b19-bd59-4b8072db9cc4"))
        assertEquals(2, entry.size)
    }

    @Test
    @DataSet(value = ["/dbunit/undiscoveredqueue/MultipleQueueEntries.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/undiscoveredqueue/MultipleQueueEntriesAfterUpdate.yaml"], orderBy = ["backfill_id", "location_id"])
    fun `update works`() {
        val dao = UndiscoveredQueueDAO(KtormHelper.database())
        dao.updateStatus(UUID.fromString("981d2048-eb49-4bfd-ba96-8291288641c3"), UndiscoveredQueueStatus.DISCOVERED)
    }
}
