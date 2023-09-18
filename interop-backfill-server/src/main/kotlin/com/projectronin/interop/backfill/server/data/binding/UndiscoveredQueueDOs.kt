package com.projectronin.interop.backfill.server.data.binding

import com.projectronin.interop.backfill.server.data.model.UndiscoveredQueueDO
import com.projectronin.interop.backfill.server.generated.models.UndiscoveredQueueStatus
import com.projectronin.interop.common.ktorm.binding.binaryUuid
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.varchar

object UndiscoveredQueueDOs : Table<UndiscoveredQueueDO>("undiscovered_queue") {
    var backfillId = binaryUuid("backfill_id").bindTo { it.backfillId }
    var entryId = binaryUuid("entry_id").primaryKey().bindTo { it.entryId }
    var locationId = varchar("location_id").bindTo { it.locationId }
    var status = enum<UndiscoveredQueueStatus>("status").bindTo { it.status }
}
