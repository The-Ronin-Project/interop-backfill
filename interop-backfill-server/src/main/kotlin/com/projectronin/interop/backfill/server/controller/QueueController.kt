package com.projectronin.interop.backfill.server.controller

import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.backfill.server.generated.apis.QueueApi
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.backfill.server.generated.models.GeneratedId
import com.projectronin.interop.backfill.server.generated.models.NewQueueEntry
import com.projectronin.interop.backfill.server.generated.models.QueueEntry
import com.projectronin.interop.backfill.server.generated.models.UpdateQueueEntry
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class QueueController(
    private val backfillDAO: BackfillDAO,
    private val backfillQueueDAO: BackfillQueueDAO,
) : QueueApi {
    val logger = KotlinLogging.logger { }

    // GET
    @PreAuthorize("hasAuthority('SCOPE_read:queue')")
    override fun getEntriesByBackfillID(backfillId: UUID): ResponseEntity<List<QueueEntry>> {
        val entries = backfillQueueDAO.getByBackfillID(backfillId)
        val backFill = backfillDAO.getByID(backfillId)!!
        return ResponseEntity.ok(entries.map { it.toModel(backFill) })
    }

    @PreAuthorize("hasAuthority('SCOPE_read:queue')")
    override fun getQueueEntries(tenantId: String): ResponseEntity<List<QueueEntry>> {
        val startedEntries = backfillQueueDAO.getByTenant(tenantId, status = BackfillStatus.STARTED)
        if (startedEntries.isNotEmpty()) {
            return ResponseEntity.ok(emptyList())
        }
        val entries = backfillQueueDAO.getByTenant(tenantId, status = BackfillStatus.NOT_STARTED)
        // some information is stored on the actual backfill object
        val backfillsById = entries.map { it.backfillId }.associateWith { backfillDAO.getByID(it)!! }
        val returnModels = entries.map { it.toModel(backfillsById[it.backfillId]!!) }
        return ResponseEntity.ok(returnModels)
    }

    @PreAuthorize("hasAuthority('SCOPE_read:queue')")
    override fun getQueueEntryById(queueId: UUID): ResponseEntity<QueueEntry> {
        val entry = backfillQueueDAO.getByID(queueId) ?: return ResponseEntity.notFound().build()
        val backFill = backfillDAO.getByID(entry.backfillId)!!
        return ResponseEntity.ok(entry.toModel(backFill))
    }

    // POST
    @PreAuthorize("hasAuthority('SCOPE_create:queue')")
    override fun postQueueEntry(
        backfillId: UUID,
        newQueueEntry: List<NewQueueEntry>,
    ): ResponseEntity<List<GeneratedId>> {
        val currentPatients = backfillQueueDAO.getByBackfillID(backfillId).map { it.patientId }
        // Don't insert patients we already have and also filter out any duplicate patients sent
        val newEntries = newQueueEntry.filterNot { currentPatients.contains(it.patientId) }.distinctBy { it.patientId }
        val existingNumber = newQueueEntry.size - newEntries.size
        if (existingNumber > 0) {
            logger.info { "Found $existingNumber patients for backfill $backfillId" }
        }
        val ids = newEntries.map { backfillQueueDAO.insert(it.toDo())!! }
        return ResponseEntity.status(HttpStatus.CREATED).body(ids.map { GeneratedId(it) })
    }

    // PATCH
    @PreAuthorize("hasAuthority('SCOPE_update:queue')")
    override fun updateQueueEntryByID(
        queueId: UUID,
        updateQueueEntry: UpdateQueueEntry,
    ): ResponseEntity<Boolean> {
        backfillQueueDAO.updateStatus(queueId, updateQueueEntry.status)
        return ResponseEntity.ok(true)
    }

    // DELETE
    @PreAuthorize("hasAuthority('SCOPE_delete:queue')")
    override fun deleteQueueEntryById(queueId: UUID): ResponseEntity<Boolean> {
        backfillQueueDAO.updateStatus(queueId, BackfillStatus.DELETED)
        return ResponseEntity.ok(true)
    }

    private fun BackfillQueueDO.toModel(backfillDO: BackfillDO): QueueEntry {
        return QueueEntry(
            id = this.entryId,
            backfillId = this.backfillId,
            tenantId = backfillDO.tenantId,
            patientId = this.patientId,
            startDate = backfillDO.startDate,
            endDate = backfillDO.endDate,
            status = this.status,
            lastUpdated = this.updatedDateTime,
        )
    }

    private fun NewQueueEntry.toDo(): BackfillQueueDO {
        return BackfillQueueDO {
            backfillId = this@toDo.backfillId
            patientId = this@toDo.patientId
            status = BackfillStatus.NOT_STARTED
        }
    }
}
