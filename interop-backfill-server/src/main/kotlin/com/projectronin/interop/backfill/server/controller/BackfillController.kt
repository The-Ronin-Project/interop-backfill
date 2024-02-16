package com.projectronin.interop.backfill.server.controller

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.DiscoveryQueueDAO
import com.projectronin.interop.backfill.server.data.model.BackfillDO
import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.backfill.server.data.model.DiscoveryQueueDO
import com.projectronin.interop.backfill.server.generated.apis.BackfillApi
import com.projectronin.interop.backfill.server.generated.models.Backfill
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.backfill.server.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.server.generated.models.GeneratedId
import com.projectronin.interop.backfill.server.generated.models.NewBackfill
import com.projectronin.interop.backfill.server.generated.models.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class BackfillController(
    private val backfillDAO: BackfillDAO,
    private val backfillQueueDAO: BackfillQueueDAO,
    private val discoveryQueueDAO: DiscoveryQueueDAO,
) : BackfillApi {
    // GETS
    @PreAuthorize("hasAuthority('SCOPE_read:backfill')")
    override fun getBackfillById(backfillId: UUID): ResponseEntity<Backfill> {
        val backfill = backfillDAO.getByID(backfillId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(backfill.getBackfillModel())
    }

    @PreAuthorize("hasAuthority('SCOPE_read:backfill')")
    override fun getBackfills(
        tenantId: String,
        order: Order,
        limit: Int,
        after: UUID?,
    ): ResponseEntity<List<Backfill>> {
        val backFills = backfillDAO.getByTenant(tenantId, order, limit, after).filterNot { it.isDeleted }
        return ResponseEntity.ok(backFills.map { it.getBackfillModel() })
    }

    // POST
    @PreAuthorize("hasAuthority('SCOPE_create:backfill')")
    override fun postBackfill(newBackfill: NewBackfill): ResponseEntity<GeneratedId> {
        // this is really only used for validating the allowed resources
        val validatedBackfill =
            if (newBackfill.allowedResources?.isNotEmpty() == true &&
                !newBackfill.allowedResources.contains(
                    ResourceType.Patient.name,
                )
            ) {
                newBackfill.copy(
                    allowedResources = newBackfill.allowedResources.plus(ResourceType.Patient.name),
                )
            } else {
                newBackfill
            }

        // check that they spelled everything correctly
        validatedBackfill.allowedResources?.forEach { resourceType ->
            if (!ResourceType.values().map { it.name }.contains(resourceType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
            }
        }

        if (validatedBackfill.locationIds.isNullOrEmpty() && validatedBackfill.patientIds.isNullOrEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
        val newUUID = backfillDAO.insert(validatedBackfill.toDO())
        validatedBackfill.toDiscoveryDOs(newUUID).forEach { discoveryQueueDAO.insert(it) }
        validatedBackfill.toQueueDOs(newUUID).forEach { backfillQueueDAO.insert(it) }
        return ResponseEntity.status(HttpStatus.CREATED).body(GeneratedId(newUUID))
    }

    // DELETE
    @PreAuthorize("hasAuthority('SCOPE_delete:backfill')")
    override fun deleteBackfillById(backfillId: UUID): ResponseEntity<Boolean> {
        val queueEntries = backfillQueueDAO.getByBackfillID(backfillId)
        queueEntries.map {
            backfillQueueDAO.updateStatus(it.entryId, BackfillStatus.DELETED)
        }
        val discoveryQueueEntries = discoveryQueueDAO.getByBackfillID(backfillId)
        discoveryQueueEntries.map {
            discoveryQueueDAO.updateStatus(it.entryId, DiscoveryQueueStatus.DELETED)
        }
        backfillDAO.delete(backfillId)
        return ResponseEntity.ok(true)
    }

    private fun BackfillDO.getBackfillModel(): Backfill {
        val discoveruQueueEntries = discoveryQueueDAO.getByBackfillID(this.backfillId)
        val queueEntries = backfillQueueDAO.getByBackfillID(this.backfillId)
        return this.toModel(discoveruQueueEntries, queueEntries)
    }

    fun BackfillDO.toModel(
        discoveryEntries: List<DiscoveryQueueDO>,
        backfillQueueEntries: List<BackfillQueueDO>,
    ): Backfill {
        val locationIds = discoveryEntries.map { it.locationId }
        val statusList = backfillQueueEntries.associateBy { it.status }
        val status =
            when {
                // it's deleted
                this.isDeleted -> BackfillStatus.DELETED
                // no entries, hasn't made it through discovery yet
                statusList.keys.isEmpty() -> BackfillStatus.NOT_STARTED
                // all entries are the same value, that's the backfill status
                statusList.keys.size == 1 -> statusList.keys.first()
                // some entries are different must mean that we've got a backfill in progress
                else -> BackfillStatus.STARTED
            }
        val lastUpdated = backfillQueueEntries.maxOfOrNull { it.updatedDateTime }

        return Backfill(
            id = this.backfillId,
            tenantId = this.tenantId,
            startDate = this.startDate,
            endDate = this.endDate,
            status = status,
            locationIds = locationIds,
            lastUpdated = lastUpdated,
            allowedResources =
                if (this.allowedResources.isNullOrEmpty()) {
                    emptyList()
                } else {
                    this.allowedResources!!.split(",")
                },
        )
    }

    fun NewBackfill.toDO(): BackfillDO {
        return BackfillDO {
            tenantId = this@toDO.tenantId
            startDate = this@toDO.startDate
            endDate = this@toDO.endDate
            allowedResources = this@toDO.allowedResources?.joinToString(",")
        }
    }

    fun NewBackfill.toDiscoveryDOs(newUUID: UUID): List<DiscoveryQueueDO> {
        return this.locationIds?.map {
            DiscoveryQueueDO {
                backfillId = newUUID
                locationId = it
                status = DiscoveryQueueStatus.UNDISCOVERED
            }
        } ?: emptyList()
    }

    fun NewBackfill.toQueueDOs(newUUID: UUID): List<BackfillQueueDO> {
        return this.patientIds?.distinct()?.map {
            BackfillQueueDO {
                backfillId = newUUID
                patientId = it
                status = BackfillStatus.NOT_STARTED
            }
        } ?: emptyList()
    }
}
