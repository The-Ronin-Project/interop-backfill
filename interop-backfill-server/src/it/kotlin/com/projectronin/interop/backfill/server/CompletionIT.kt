package com.projectronin.interop.backfill.server

import com.projectronin.event.interop.internal.v1.Metadata
import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.backfill.client.generated.models.BackfillStatus
import com.projectronin.interop.backfill.client.generated.models.NewBackfill
import com.projectronin.interop.backfill.client.generated.models.NewQueueEntry
import com.projectronin.interop.backfill.client.generated.models.UpdateQueueEntry
import com.projectronin.interop.backfill.server.data.model.CompletenessDO
import com.projectronin.interop.fhir.generators.resources.appointment
import com.projectronin.interop.fhir.generators.resources.patient
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.kafka.model.DataTrigger
import com.projectronin.interop.kafka.model.PublishResourceWrapper
import com.projectronin.interop.kafka.testing.client.KafkaTestingClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class CompletionIT : BaseBackfillIT() {
    @Test
    fun `picks up events`() {
        val newBackfillId = runBlocking {
            backfillClient.postBackfill(
                NewBackfill(
                    tenantId = "tenant",
                    locationIds = listOf("123"),
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now()
                )
            )
        }.id!!
        val entries = runBlocking {
            queueClient.postQueueEntry(
                backfillId = newBackfillId,
                newQueueEntries = listOf(
                    NewQueueEntry(newBackfillId, patientId = "123"),
                    NewQueueEntry(newBackfillId, patientId = "456"),
                    NewQueueEntry(newBackfillId, patientId = "789")
                )
            )
        }
        entries.forEach {
            runBlocking {
                queueClient.updateQueueEntryByID(
                    it.id!!,
                    UpdateQueueEntry(BackfillStatus.STARTED)
                )
            }
        }
        val kfka = KafkaTestingClient("localhost:9092")

        // publish some events so the server can get a commit started
        kfka.kafkaPublishService.publishResourceWrappers(
            tenantId = "tenant",
            trigger = DataTrigger.BACKFILL,
            resourceWrappers = listOf(
                PublishResourceWrapper(
                    resource = appointment {
                        id of Id("yeee")
                    }
                )
            ),
            metadata = Metadata(
                runId = UUID.randomUUID().toString(),
                runDateTime = OffsetDateTime.now(),
                upstreamReferences = emptyList(),
                backfillRequest = null
            )
        )
        kfka.kafkaPublishService.publishResourceWrappers(
            tenantId = "tenant",
            trigger = DataTrigger.BACKFILL,
            resourceWrappers = listOf(
                PublishResourceWrapper(
                    resource = patient {
                        id of Id("yeee")
                    }
                )
            ),
            metadata = Metadata(
                runId = UUID.randomUUID().toString(),
                runDateTime = OffsetDateTime.now(),
                upstreamReferences = emptyList(),
                backfillRequest = null
            )
        )
        runBlocking { delay(7000) }
        kfka.kafkaPublishService.publishResourceWrappers(
            tenantId = "tenant",
            trigger = DataTrigger.BACKFILL,
            resourceWrappers = listOf(
                PublishResourceWrapper(
                    resource = patient {
                        id of Id("tenant-123")
                        identifier of listOf(
                            Identifier(
                                system = CodeSystem.RONIN_FHIR_ID.uri,
                                value = "123".asFHIR()
                            )
                        )
                    }
                )
            ),
            metadata = Metadata(
                runId = UUID.randomUUID().toString(),
                runDateTime = OffsetDateTime.now(),
                upstreamReferences = null,
                backfillRequest = Metadata.BackfillRequest(
                    backfillId = newBackfillId.toString(),
                    backfillStartDate = OffsetDateTime.now(),
                    backfillEndDate = OffsetDateTime.now()
                )
            )
        )

        kfka.kafkaPublishService.publishResourceWrappers(
            tenantId = "tenant",
            trigger = DataTrigger.BACKFILL,
            resourceWrappers = listOf(
                PublishResourceWrapper(
                    resource = appointment {
                        id of Id("yeee")
                    }
                )
            ),
            metadata = Metadata(
                runId = UUID.randomUUID().toString(),
                runDateTime = OffsetDateTime.now(),
                upstreamReferences = listOf(
                    Metadata.UpstreamReference(resourceType = ResourceType.Patient, id = "tenant-456")
                ),
                backfillRequest = Metadata.BackfillRequest(
                    backfillId = newBackfillId.toString(),
                    backfillStartDate = OffsetDateTime.now(),
                    backfillEndDate = OffsetDateTime.now()
                )
            )
        )
        runBlocking {
            // c'mon it's a good server it just needs a moment to pick up the events, bah gawd
            delay(1000)
        }

        assertNotNull(completenessDAO.getByID(entries[0].id!!))
        assertNotNull(completenessDAO.getByID(entries[1].id!!))
        assertNull(completenessDAO.getByID(entries[2].id!!))
    }

    @Test
    fun `resolver resolves`() {
        val newBackfillId = runBlocking {
            backfillClient.postBackfill(
                NewBackfill(
                    tenantId = "tenant",
                    locationIds = listOf("123"),
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now()
                )
            )
        }.id!!
        val entries = runBlocking {
            queueClient.postQueueEntry(
                backfillId = newBackfillId,
                newQueueEntries = listOf(
                    NewQueueEntry(newBackfillId, patientId = "123"),
                    NewQueueEntry(newBackfillId, patientId = "456"),
                    NewQueueEntry(newBackfillId, patientId = "789")
                )
            )
        }
        entries.forEach {
            runBlocking {
                queueClient.updateQueueEntryByID(
                    it.id!!,
                    UpdateQueueEntry(BackfillStatus.STARTED)
                )
            }
        }
        completenessDAO.create(
            CompletenessDO {
                queueId = entries[0].id!!
                lastSeen = OffsetDateTime.now().minusDays(1)
            }
        )
        completenessDAO.create(
            CompletenessDO {
                queueId = entries[1].id!!
                lastSeen = OffsetDateTime.now().minusDays(1)
            }
        )

        assertNotNull(completenessDAO.getByID(entries[0].id!!))
        assertNotNull(completenessDAO.getByID(entries[1].id!!))
        assertNull(completenessDAO.getByID(entries[2].id!!))
        assertEquals(3, queueDAO.getAllInProgressEntries().size)

        runBlocking { delay(10.seconds) }

        assertNull(completenessDAO.getByID(entries[0].id!!))
        assertNull(completenessDAO.getByID(entries[1].id!!))
        assertNull(completenessDAO.getByID(entries[2].id!!))
        assertEquals(1, queueDAO.getAllInProgressEntries().size)

        // compare names because the dao returns the server model and we have the client model
        assertEquals(BackfillStatus.COMPLETED.name, queueDAO.getByID(entries[0].id!!)?.status?.name)
        assertEquals(BackfillStatus.COMPLETED.name, queueDAO.getByID(entries[1].id!!)?.status?.name)
        assertEquals(BackfillStatus.STARTED.name, queueDAO.getByID(entries[2].id!!)?.status?.name)
    }
}
