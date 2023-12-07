package com.projectronin.interop.backfill.server.controller

import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.DiscoveryQueueDAO
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.backfill.server.data.model.DiscoveryQueueDO
import com.projectronin.interop.backfill.server.generated.apis.DiscoveryQueueApi
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueEntry
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.server.generated.models.UpdateDiscoveryEntry
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DiscoveryQueueController(
    private val backfillDAO: BackfillDAO,
    private val discoveryQueueDAO: DiscoveryQueueDAO,
) : DiscoveryQueueApi {
    val logger = KotlinLogging.logger { }

    // GET
    @PreAuthorize("hasAuthority('SCOPE_read:discovery')")
    override fun getDiscoveryQueueEntries(
        tenantId: String,
        status: DiscoveryQueueStatus?,
        backfillId: UUID?,
    ): ResponseEntity<List<DiscoveryQueueEntry>> {
        val entries = discoveryQueueDAO.getByTenant(tenantId, status, backfillId)
        // some information is stored on the actual backfill object
        val backfillsById = entries.map { it.backfillId }.associateWith { backfillDAO.getByID(it)!! }

        return ResponseEntity.ok(entries.map { it.toModel(backfillsById[it.backfillId]!!) })
    }

    @PreAuthorize("hasAuthority('SCOPE_read:discovery')")
    override fun getDiscoveryQueueEntryById(discoveryQueueId: UUID): ResponseEntity<DiscoveryQueueEntry> {
        val entry = discoveryQueueDAO.getByID(discoveryQueueId) ?: return ResponseEntity.notFound().build()
        val backfill = backfillDAO.getByID(entry.backfillId)!!
        return ResponseEntity.ok(entry.toModel(backfill))
    }

    // PATCH
    @PreAuthorize("hasAuthority('SCOPE_update:discovery')")
    override fun updateDiscoveryQueueEntryByID(
        discoveryQueueId: UUID,
        updateDiscoveryEntry: UpdateDiscoveryEntry,
    ): ResponseEntity<Boolean> {
        discoveryQueueDAO.updateStatus(discoveryQueueId, updateDiscoveryEntry.status)
        return ResponseEntity.ok(true)
    }

    // DELETE
    @PreAuthorize("hasAuthority('SCOPE_delete:discovery')")
    override fun deleteDiscoveryQueueEntryById(discoveryQueueId: UUID): ResponseEntity<Boolean> {
        discoveryQueueDAO.updateStatus(discoveryQueueId, DiscoveryQueueStatus.DELETED)
        return ResponseEntity.ok(true)
    }

    private fun DiscoveryQueueDO.toModel(backfillDO: BackfillDO): DiscoveryQueueEntry {
        return DiscoveryQueueEntry(
            id = this.entryId,
            backfillId = this.backfillId,
            tenantId = backfillDO.tenantId,
            startDate = backfillDO.startDate,
            endDate = backfillDO.endDate,
            locationId = this.locationId,
            status = this.status,
        )
    }
}
