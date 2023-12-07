package com.projectronin.interop.backfill.server.data.model
import org.ktorm.entity.Entity
import java.time.LocalDate
import java.util.UUID

interface BackfillDO : Entity<BackfillDO> {
    companion object : Entity.Factory<BackfillDO>()

    var backfillId: UUID
    var tenantId: String
    var startDate: LocalDate
    var endDate: LocalDate
    var isDeleted: Boolean
}
