package com.projectronin.interop.backfill.server

import com.projectronin.event.interop.internal.v1.InteropResourcePublishV1
import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.CompletenessDAO
import com.projectronin.interop.backfill.server.data.model.CompletenessDO
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.fhir.r4.resource.Patient
import mu.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class CompletionService(
    val queueDAO: BackfillQueueDAO,
    val completenessDAO: CompletenessDAO
) {
    val logger = KotlinLogging.logger { }

    @KafkaListener(
        topicPattern = "oci.us-phoenix-1.interop-mirth.*-publish-adhoc.v1",
        groupId = "interop-backfill",
        properties = ["metadata.max.age.ms:\${backfill.kafka.listenerRefresh:300000}"]
    )
    fun eventConsumer(
        message: String,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timeStamp: Long
    ) {
        logger.debug { "received message $message with received time of $timeStamp" }
        val event = try {
            JacksonUtil.readJsonObject(message, InteropResourcePublishV1::class)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse as InteropResourcePublishV1" }
            return
        }

        // don't bother processing non-backfill events
        if (event.dataTrigger != InteropResourcePublishV1.DataTrigger.backfill) return
        val backfillInfo = event.metadata.backfillRequest ?: return

        val tenant = event.tenantId
        logger.debug { "searching for queue entries with tenant $tenant, backfillId: ${backfillInfo.backfillId}" }
        val queueEntries = queueDAO.getByTenant(
            tenant = tenant,
            backfillId = UUID.fromString(backfillInfo.backfillId),
            status = BackfillStatus.STARTED
        )
        if (queueEntries.isEmpty()) return
        logger.debug { "Found ${queueEntries.size} entries" }

        // what was the patient that originally caused this interop event?
        val eventPatientReference =
            if (event.resourceType == ResourceType.Patient) {
                JacksonUtil.readJsonObject(event.resourceJson, Patient::class).findFhirId()
            } else {
                val patientUpstreamReference = event.metadata.upstreamReferences?.firstOrNull {
                    it.resourceType == ResourceType.Patient
                }
                // these references are localized :(
                patientUpstreamReference?.id?.removePrefix("$tenant-")
            }
        val entry = queueEntries
            .find { it.patientId == eventPatientReference }
            ?: return

        logger.debug { "Found matching queue entry with id ${entry.entryId}" }

        val eventCreatedTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneOffset.UTC)
        val existingCompletenessDO = completenessDAO.getByID(entry.entryId)

        if (existingCompletenessDO == null) {
            logger.debug { "Creating new entry" }
            completenessDAO.create(
                CompletenessDO {
                    queueId = entry.entryId
                    lastSeen = eventCreatedTime
                }
            )
            // if we've got an existing entry update it, unless for some reason we're reprocessing old events
        } else if (existingCompletenessDO.lastSeen < eventCreatedTime) {
            logger.debug { "Updating existing entry" }
            completenessDAO.update(
                existingCompletenessDO.queueId,
                lastSeen = eventCreatedTime
            )
        }
    }
}
