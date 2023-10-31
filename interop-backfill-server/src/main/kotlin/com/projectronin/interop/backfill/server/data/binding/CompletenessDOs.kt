package com.projectronin.interop.backfill.server.data.binding

import com.projectronin.interop.backfill.server.data.model.CompletenessDO
import com.projectronin.interop.common.ktorm.binding.binaryUuid
import com.projectronin.interop.common.ktorm.binding.utcDateTime
import org.ktorm.schema.Table

object CompletenessDOs : Table<CompletenessDO>("completeness") {
    var queueId = binaryUuid("queue_id").bindTo { it.queueId }
    var lastSeen = utcDateTime("last_seen_dt_tm").bindTo { it.lastSeen }
}
