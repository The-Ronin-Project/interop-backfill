package com.projectronin.interop.backfill.server.data

import com.projectronin.interop.backfill.server.data.binding.BackfillDOs
import com.projectronin.interop.backfill.server.data.binding.DiscoveryQueueDOs
import com.projectronin.interop.backfill.server.data.model.DiscoveryQueueDO
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.common.ktorm.dao.BaseInteropDAO
import com.projectronin.interop.common.ktorm.valueLookup
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.inList
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
class DiscoveryQueueDAO(database: Database) : BaseInteropDAO<DiscoveryQueueDO, UUID>(database) {
    override val primaryKeyColumn = DiscoveryQueueDOs.entryId
    fun getByBackfillID(backfillId: UUID): List<DiscoveryQueueDO> {
        return database.valueLookup(backfillId, DiscoveryQueueDOs.backfillId)
    }

    fun getByTenant(tenant: String, status: DiscoveryQueueStatus? = null, backfillId: UUID? = null): List<DiscoveryQueueDO> {
        logger.debug { "Searching for UndiscoveredQueue Entries for organization $tenant" }
        status?.let { logger.debug { "with status $status" } }
        val statusList = if (status == null) {
            listOf(DiscoveryQueueStatus.UNDISCOVERED, DiscoveryQueueStatus.DISCOVERED)
        } else {
            listOf(status)
        }
        return database.from(DiscoveryQueueDOs)
            .leftJoin(BackfillDOs, on = DiscoveryQueueDOs.backfillId eq BackfillDOs.id)
            .joinReferencesAndSelect()
            .where {
                val conditions = mutableListOf<ColumnDeclaring<Boolean>>()
                conditions += BackfillDOs.tenantId eq tenant
                conditions += DiscoveryQueueDOs.status inList statusList
                backfillId?.let { conditions += DiscoveryQueueDOs.backfillId eq backfillId }
                conditions.reduce { a, b -> a and b }
            }
            .map { DiscoveryQueueDOs.createEntity(it) }
    }

    fun insert(undiscoveredQueue: DiscoveryQueueDO): UUID? {
        val newUUID = UUID.randomUUID()
        logger.info { "Inserting undiscoveredQueue for backfill ${undiscoveredQueue.backfillId} with UUID $newUUID" }
        database.insert(DiscoveryQueueDOs) {
            set(it.backfillId, undiscoveredQueue.backfillId)
            set(it.entryId, newUUID)
            set(it.locationId, undiscoveredQueue.locationId)
            set(it.status, undiscoveredQueue.status)
        }
        return newUUID
    }

    fun updateStatus(entryID: UUID, status: DiscoveryQueueStatus) {
        logger.info { "updating undiscoveredQueue $entryID with status $status" }
        database.update(DiscoveryQueueDOs) {
            set(it.status, status)
            where { it.entryId eq entryID }
        }
    }
}
