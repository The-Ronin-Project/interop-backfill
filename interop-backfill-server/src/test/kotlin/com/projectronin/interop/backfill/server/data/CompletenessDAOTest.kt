package com.projectronin.interop.backfill.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.backfill.server.data.model.CompletenessDO
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID

@LiquibaseTest(changeLog = "backfill/db/changelog/backfill.db.changelog-master.yaml")
class CompletenessDAOTest {
    init {
        // We have to set this to prevent some DBUnit weirdness around UTC and the local time zone that DST seems to cause.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    @Test
    @DataSet(value = ["/dbunit/completeness/NoCompletenessEntry.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/completeness/SingleCompletenessEntry.yaml"], orderBy = ["backfill_id", "patient_id"])
    fun `insert works`() {
        val dao = CompletenessDAO(KtormHelper.database())
        val newEntry =
            CompletenessDO {
                queueId = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
                lastSeen = OffsetDateTime.of(2023, 10, 27, 12, 12, 12, 0, ZoneOffset.UTC)
            }
        dao.create(newEntry)
    }

    @Test
    @DataSet(value = ["/dbunit/completeness/SingleCompletenessEntry.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/completeness/UpdatedCompletenessEntry.yaml"], orderBy = ["backfill_id", "patient_id"])
    fun `update works`() {
        val queueId = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val lastSeen = OffsetDateTime.of(2023, 10, 28, 12, 12, 12, 0, ZoneOffset.UTC)
        val dao = CompletenessDAO(KtormHelper.database())
        dao.update(queueId, lastSeen)
    }

    @Test
    @DataSet(value = ["/dbunit/completeness/SingleCompletenessEntry.yaml"], cleanAfter = true)
    fun `get works`() {
        val dao = CompletenessDAO(KtormHelper.database())
        val result = dao.getByID(UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663"))
        assertNotNull(result)
    }

    @Test
    @DataSet(value = ["/dbunit/completeness/SingleCompletenessEntry.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/completeness/NoCompletenessEntry.yaml"], orderBy = ["backfill_id", "patient_id"])
    fun `delete works`() {
        val dao = CompletenessDAO(KtormHelper.database())
        dao.delete(UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663"))
    }
}
