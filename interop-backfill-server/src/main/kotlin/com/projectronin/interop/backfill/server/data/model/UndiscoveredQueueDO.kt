package com.projectronin.interop.backfill.server.data.model

import com.projectronin.interop.backfill.server.generated.models.UndiscoveredQueueStatus
import org.ktorm.entity.Entity
import java.util.UUID

interface UndiscoveredQueueDO : Entity<UndiscoveredQueueDO> {
    companion object : Entity.Factory<UndiscoveredQueueDO>()
    var backfillId: UUID
    var entryId: UUID
    var locationId: String
    var status: UndiscoveredQueueStatus
}
