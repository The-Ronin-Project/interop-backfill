package com.projectronin.interop.backfill.server.data

import com.projectronin.interop.backfill.server.data.binding.BackfillDOs
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.common.ktorm.dao.BaseInteropDAO
import com.projectronin.interop.common.ktorm.valueLookup
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class BackfillDAO(database: Database) : BaseInteropDAO<BackfillDO, UUID>(database) {
    override val primaryKeyColumn = BackfillDOs.id

    fun getByTenant(tenantId: String): List<BackfillDO> {
        return database.valueLookup(tenantId, BackfillDOs.tenantId)
    }

    fun insert(backfillDO: BackfillDO): UUID {
        val newUUID = UUID.randomUUID()
        logger.info { "Inserting backfill for organization ${backfillDO.tenantId} with UUID $newUUID" }
        database.insert(BackfillDOs) {
            set(it.id, newUUID)
            set(it.tenantId, backfillDO.tenantId)
            set(it.startDate, backfillDO.startDate)
            set(it.endDate, backfillDO.endDate)
        }

        logger.info { "Backfill $newUUID inserted" }
        return newUUID
    }

    fun delete(backfillId: UUID) {
        database.update(BackfillDOs) {
            set(it.isDeleted, true)
            where { it.id eq backfillId }
        }
    }
}
