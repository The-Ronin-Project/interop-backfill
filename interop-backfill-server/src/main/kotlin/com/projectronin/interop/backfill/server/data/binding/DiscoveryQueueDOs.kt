package com.projectronin.interop.backfill.server.data.binding

import com.projectronin.interop.backfill.server.data.model.DiscoveryQueueDO
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.common.ktorm.binding.binaryUuid
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.varchar

object DiscoveryQueueDOs : Table<DiscoveryQueueDO>("undiscovered_queue") {
    var backfillId = binaryUuid("backfill_id").bindTo { it.backfillId }
    var entryId = binaryUuid("entry_id").primaryKey().bindTo { it.entryId }
    var locationId = varchar("location_id").bindTo { it.locationId }
    var status = enum<DiscoveryQueueStatus>("status").bindTo { it.status }
}
