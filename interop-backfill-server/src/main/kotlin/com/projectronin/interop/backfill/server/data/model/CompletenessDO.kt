package com.projectronin.interop.backfill.server.data.model

import org.ktorm.entity.Entity
import java.time.OffsetDateTime
import java.util.UUID

interface CompletenessDO : Entity<CompletenessDO> {
    companion object : Entity.Factory<CompletenessDO>()
    var queueId: UUID
    var lastSeen: OffsetDateTime
}
