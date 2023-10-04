package com.projectronin.interop.backfill.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

@LiquibaseTest(changeLog = "backfill/db/changelog/backfill.db.changelog-master.yaml")
class BackfillDAOTest {
    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/BaseBackfill.yaml"], cleanAfter = true)
    fun `getByTenant works`() {
        val dao = BackfillDAO(KtormHelper.database())
        val backfill = dao.getByTenant("tenant1")
        assertNotNull(backfill)
        assertEquals(LocalDate.of(2022, 8, 1), backfill.first().startDate)
    }

    @Test
    @DataSet(value = ["/dbunit/BaseBackfill.yaml"], cleanAfter = true)
    fun `getByTenant works for wrong tenant`() {
        val dao = BackfillDAO(KtormHelper.database())
        val backfill = dao.getByTenant("tenant2")
        assertEquals(0, backfill.size)
    }

    @Test
    @DataSet(value = ["/dbunit/BaseBackfill.yaml"], cleanAfter = true)
    fun `getByID works`() {
        val dao = BackfillDAO(KtormHelper.database())
        val backfill = dao.getByID(UUID.fromString("b4e8e80a-297a-4b19-bd59-4b8072db9cc4"))
        assertNotNull(backfill)
        assertEquals(LocalDate.of(2022, 8, 1), backfill?.startDate)
    }

    @Test
    @DataSet(value = ["/dbunit/backfill/Empty.yaml"], cleanAfter = true)
    fun `getByID works when nothing`() {
        val dao = BackfillDAO(KtormHelper.database())
        val backfill = dao.getByID(UUID.fromString("b4e8e80a-297a-4b19-bd59-4b8072db9cc4"))
        assertNull(backfill)
    }

    @Test
    @DataSet(value = ["/dbunit/backfill/Empty.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/BaseBackfill.yaml"], ignoreCols = ["backfill_id"])
    fun `insert works`() {
        val dao = BackfillDAO(KtormHelper.database())
        val newBackfill = BackfillDO {
            tenantId = "tenant1"
            startDate = LocalDate.of(2022, 8, 1)
            endDate = LocalDate.of(2023, 9, 1)
        }
        val uuid = dao.insert(newBackfill)
        assertNotNull(uuid)
    }

    @Test
    @DataSet(value = ["/dbunit/BaseBackfill.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/backfill/Deleted.yaml"])
    fun `delete works`() {
        val dao = BackfillDAO(KtormHelper.database())
        dao.delete(UUID.fromString("b4e8e80a-297a-4b19-bd59-4b8072db9cc4"))
    }
}
