package com.projectronin.interop.backfill.server.data.binding

import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.common.ktorm.binding.binaryUuid
import com.projectronin.interop.common.ktorm.binding.utcDateTime
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.varchar

object BackfillQueueDOs : Table<BackfillQueueDO>("backfill_queue") {
    var backfillId = binaryUuid("backfill_id").bindTo { it.backfillId }
    var entryId = binaryUuid("entry_id").primaryKey().bindTo { it.entryId }
    var patientId = varchar("patient_id").bindTo { it.patientId }
    var status = enum<BackfillStatus>("status").bindTo { it.status }
    var updatedDateTime = utcDateTime("update_dt_tm").bindTo { it.updatedDateTime }
}
