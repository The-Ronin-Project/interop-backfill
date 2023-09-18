package com.projectronin.interop.backfill.server.data

import com.projectronin.interop.backfill.server.data.binding.BackfillDOs
import com.projectronin.interop.backfill.server.data.binding.UndiscoveredQueueDOs
import com.projectronin.interop.backfill.server.data.model.UndiscoveredQueueDO
import com.projectronin.interop.backfill.server.generated.models.UndiscoveredQueueStatus
import com.projectronin.interop.common.ktorm.dao.BaseInteropDAO
import com.projectronin.interop.common.ktorm.valueLookup
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.joinReferencesAndSelect
import org.ktorm.dsl.leftJoin
import org.ktorm.dsl.map
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.schema.ColumnDeclaring
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UndiscoveredQueueDAO(database: Database) : BaseInteropDAO<UndiscoveredQueueDO, UUID>(database) {
    override val primaryKeyColumn = UndiscoveredQueueDOs.entryId

    fun getByBackfillID(backfillId: UUID): List<UndiscoveredQueueDO> {
        return database.valueLookup(backfillId, UndiscoveredQueueDOs.backfillId)
    }

    fun getByTenant(tenant: String, status: UndiscoveredQueueStatus? = null): List<UndiscoveredQueueDO> {
        logger.debug { "Searching for UndiscoveredQueue Entries for organization $tenant" }
        status?.let { logger.debug { "with status $status" } }
        return database.from(UndiscoveredQueueDOs)
            .leftJoin(BackfillDOs, on = UndiscoveredQueueDOs.backfillId eq BackfillDOs.id)
            .joinReferencesAndSelect()
            .where {
                val conditions = mutableListOf<ColumnDeclaring<Boolean>>()
                conditions += BackfillDOs.tenantId eq tenant
                status?.let { conditions += UndiscoveredQueueDOs.status eq status }
                conditions.reduce { a, b -> a and b }
            }
            .map { UndiscoveredQueueDOs.createEntity(it) }
    }

    fun insert(undiscoveredQueue: UndiscoveredQueueDO): UUID? {
        val newUUID = UUID.randomUUID()
        logger.info { "Inserting undiscoveredQueue for backfill ${undiscoveredQueue.backfillId} with UUID $newUUID" }
        database.insert(UndiscoveredQueueDOs) {
            set(it.backfillId, undiscoveredQueue.backfillId)
            set(it.entryId, newUUID)
            set(it.locationId, undiscoveredQueue.locationId)
            set(it.status, undiscoveredQueue.status)
        }
        return newUUID
    }

    fun updateStatus(entryID: UUID, status: UndiscoveredQueueStatus) {
        logger.info { "updating undiscoveredQueue $entryID with status $status" }
        database.update(UndiscoveredQueueDOs) {
            set(it.status, status)
            where { it.entryId eq entryID }
        }
    }
}
