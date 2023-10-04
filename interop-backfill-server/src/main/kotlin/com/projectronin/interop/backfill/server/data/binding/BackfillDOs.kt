package com.projectronin.interop.backfill.server.data.binding

import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.common.ktorm.binding.binaryUuid
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.date
import org.ktorm.schema.varchar

object BackfillDOs : Table<BackfillDO>("backfill") {
    var id = binaryUuid("backfill_id").primaryKey().bindTo { it.backfillId }
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var startDate = date("start_dt").bindTo { it.startDate }
    var endDate = date("end_dt").bindTo { it.endDate }
    var isDeleted = boolean("is_deleted").bindTo { it.isDeleted }
}
