package com.projectronin.interop.backfill.server.data.model

import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import org.ktorm.entity.Entity
import java.time.OffsetDateTime
import java.util.UUID

interface BackfillQueueDO : Entity<BackfillQueueDO> {
    companion object : Entity.Factory<BackfillQueueDO>()
    var backfillId: UUID
    var entryId: UUID
    var patientId: String
    var status: BackfillStatus
    var updatedDateTime: OffsetDateTime
}
