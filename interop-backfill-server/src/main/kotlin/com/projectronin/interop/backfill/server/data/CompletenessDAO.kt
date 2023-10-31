package com.projectronin.interop.backfill.server.data

import com.projectronin.interop.backfill.server.data.binding.CompletenessDOs
import com.projectronin.interop.backfill.server.data.model.CompletenessDO
import com.projectronin.interop.common.ktorm.dao.BaseInteropDAO
import org.ktorm.database.Database
import org.ktorm.dsl.delete
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class CompletenessDAO(database: Database) : BaseInteropDAO<CompletenessDO, UUID>(database) {
    override val primaryKeyColumn = CompletenessDOs.queueId

    fun create(completenessDO: CompletenessDO) {
        database.insert(CompletenessDOs) {
            set(it.queueId, completenessDO.queueId)
            set(it.lastSeen, completenessDO.lastSeen)
        }
    }

    fun update(completenessDO: CompletenessDO) {
        database.update(CompletenessDOs) {
            set(it.lastSeen, completenessDO.lastSeen)
            where { it.queueId eq completenessDO.queueId }
        }
    }

    fun delete(id: UUID) {
        database.delete(CompletenessDOs) {
            it.queueId eq id
        }
    }
}
