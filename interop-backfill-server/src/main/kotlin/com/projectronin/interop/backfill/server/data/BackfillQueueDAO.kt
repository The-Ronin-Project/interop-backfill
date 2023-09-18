package com.projectronin.interop.backfill.server.data

import com.projectronin.interop.backfill.server.data.binding.BackfillDOs
import com.projectronin.interop.backfill.server.data.binding.BackfillQueueDOs
import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
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
class BackfillQueueDAO(database: Database) : BaseInteropDAO<BackfillQueueDO, UUID>(database) {
    override val primaryKeyColumn = BackfillQueueDOs.entryId

    fun getByBackfillID(backfillId: UUID): List<BackfillQueueDO> {
        return database.valueLookup(backfillId, BackfillQueueDOs.backfillId)
    }

    fun getByTenant(tenant: String, status: BackfillStatus? = null): List<BackfillQueueDO> {
        return database.from(BackfillQueueDOs)
            .leftJoin(BackfillDOs, on = BackfillQueueDOs.backfillId eq BackfillDOs.id)
            .joinReferencesAndSelect()
            .where {
                val conditions = mutableListOf<ColumnDeclaring<Boolean>>()
                conditions += BackfillDOs.tenantId eq tenant
                status?.let { conditions += BackfillQueueDOs.status eq status }
                conditions.reduce { a, b -> a and b }
            }
            .map { BackfillQueueDOs.createEntity(it) }
    }

    fun insert(backfillQueue: BackfillQueueDO): UUID? {
        val newUUID = UUID.randomUUID()
        database.insert(BackfillQueueDOs) {
            set(it.backfillId, backfillQueue.backfillId)
            set(it.entryId, newUUID)
            set(it.patientId, backfillQueue.patientId)
            set(it.status, backfillQueue.status)
        }
        return newUUID
    }

    fun updateStatus(entryID: UUID, status: BackfillStatus) {
        database.update(BackfillQueueDOs) {
            set(it.status, status)
            where { it.entryId eq entryID }
        }
    }
}
