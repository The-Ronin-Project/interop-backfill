package com.projectronin.interop.backfill.server.data.model

import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import org.ktorm.entity.Entity
import java.util.UUID

interface DiscoveryQueueDO : Entity<DiscoveryQueueDO> {
    companion object : Entity.Factory<DiscoveryQueueDO>()
    var backfillId: UUID
    var entryId: UUID
    var locationId: String
    var status: DiscoveryQueueStatus
}
