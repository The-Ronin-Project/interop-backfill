package com.projectronin.interop.backfill.server

import com.projectronin.event.interop.internal.v1.InteropResourcePublishV1
import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.CompletenessDAO
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.fhir.generators.resources.patient
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Patient
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CompletionServiceTest {
    private val completenessDAO = mockk<CompletenessDAO>()
    private val queueDAO = mockk<BackfillQueueDAO>()
    private val event = mockk<InteropResourcePublishV1>()
    private val completionService = CompletionService(queueDAO, completenessDAO)

    @BeforeEach
    fun `mockk em`() {
        mockkObject(JacksonUtil)
        every { JacksonUtil.readJsonObject(any(), InteropResourcePublishV1::class) } returns event
    }

    @AfterEach
    fun `unmockk em`() {
        unmockkAll()
    }

    @Test
    fun `properly handles errors`() {
        every { JacksonUtil.readJsonObject("oopsie doopsie", InteropResourcePublishV1::class) } throws Exception("oops")
        completionService.eventConsumer("oopsie doopsie", 1)
        // since we're not mockking anything else here this is basically making sure we're returning early as expected
        verify(exactly = 0) { queueDAO.getByTenant(any(), any(), any()) }
    }

    @Test
    fun `properly ignores non-backfills`() {
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.adhoc
        completionService.eventConsumer("event", 1)
        verify(exactly = 0) { queueDAO.getByTenant(any(), any(), any()) }
    }

    @Test
    fun `ignores backfills without backfill info`() {
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.backfill
        every { event.metadata } returns mockk {
            every { backfillRequest } returns null
        }
        completionService.eventConsumer("event", 1)
        verify(exactly = 0) { queueDAO.getByTenant(any(), any(), any()) }
    }

    @Test
    fun `ignores event if nothing in db`() {
        val newBackfillId = UUID.randomUUID()
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.backfill
        every { event.metadata } returns mockk {
            every { backfillRequest } returns mockk {
                every { backfillId } returns newBackfillId.toString()
            }
        }
        every { event.tenantId } returns "tenant"
        every {
            queueDAO.getByTenant(
                tenant = "tenant",
                status = BackfillStatus.STARTED,
                backfillId = newBackfillId
            )
        } returns emptyList()
        completionService.eventConsumer("event", 1)
        verify(exactly = 1) { queueDAO.getByTenant(any(), any(), any()) }
    }

    @Test
    fun `finds events in db but fails to find a matching queue`() {
        val newBackfillId = UUID.randomUUID()
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.backfill
        every { event.metadata } returns mockk {
            every { backfillRequest } returns mockk {
                every { backfillId } returns newBackfillId.toString()
            }
        }
        every { event.tenantId } returns "tenant"
        every { event.resourceType } returns ResourceType.Patient
        every { event.resourceJson } returns "a patient record"
        // i can't be bothered to actually mockk the patient record since .findFhirId uses some introspection
        every { JacksonUtil.readJsonObject("a patient record", Patient::class) } returns patient {
            identifier of listOf(
                Identifier(
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                )
            )
        }
        every {
            queueDAO.getByTenant(
                tenant = "tenant",
                status = BackfillStatus.STARTED,
                backfillId = newBackfillId
            )
        } returns listOf(
            mockk {
                every { patientId } returns "not gonna find me"
            }
        )
        completionService.eventConsumer("event", 1)
        verify(exactly = 1) { queueDAO.getByTenant(any(), any(), any()) }
    }

    @Test
    fun `fails to find an event when upstream references are missing or don't provide a patient`() {
        val newBackfillId = UUID.randomUUID()
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.backfill
        every { event.metadata } returns mockk {
            every { backfillRequest } returns mockk {
                every { backfillId } returns newBackfillId.toString()
            }
            every { upstreamReferences } returns null
        }
        every { event.tenantId } returns "tenant"
        every { event.resourceType } returns ResourceType.Appointment

        val queueId = UUID.randomUUID()
        every {
            queueDAO.getByTenant(
                tenant = "tenant",
                status = BackfillStatus.STARTED,
                backfillId = newBackfillId
            )
        } returns listOf(
            mockk {
                every { entryId } returns queueId
                every { patientId } returns "123"
            }
        )

        every { completenessDAO.getByID(queueId) } returns mockk {
            every { lastSeen } returns OffsetDateTime.ofInstant(Instant.ofEpochMilli(2), ZoneOffset.UTC)
        }
        completionService.eventConsumer("event", 1)
        verify(exactly = 1) { queueDAO.getByTenant(any(), any(), any()) }
        verify(exactly = 0) { completenessDAO.update(any(), any()) }
        verify(exactly = 0) { completenessDAO.create(any()) }

        every { event.metadata } returns mockk {
            every { backfillRequest } returns mockk {
                every { backfillId } returns newBackfillId.toString()
            }
            every { upstreamReferences } returns listOf(
                mockk {
                    every { resourceType } returns ResourceType.Account
                }
            )
        }
        completionService.eventConsumer("event", 1)
        verify(exactly = 0) { completenessDAO.update(any(), any()) }
        verify(exactly = 0) { completenessDAO.create(any()) }
    }

    @Test
    fun `finds an event but it's newer`() {
        val newBackfillId = UUID.randomUUID()
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.backfill
        every { event.metadata } returns mockk {
            every { backfillRequest } returns mockk {
                every { backfillId } returns newBackfillId.toString()
            }
            every { upstreamReferences } returns listOf(
                mockk {
                    every { resourceType } returns ResourceType.Patient
                    every { id } returns "tenant-123"
                }
            )
        }
        every { event.tenantId } returns "tenant"
        every { event.resourceType } returns ResourceType.Appointment

        val queueId = UUID.randomUUID()
        every {
            queueDAO.getByTenant(
                tenant = "tenant",
                status = BackfillStatus.STARTED,
                backfillId = newBackfillId
            )
        } returns listOf(
            mockk {
                every { entryId } returns queueId
                every { patientId } returns "123"
            }
        )

        every { completenessDAO.getByID(queueId) } returns mockk {
            every { lastSeen } returns OffsetDateTime.ofInstant(Instant.ofEpochMilli(2), ZoneOffset.UTC)
        }
        completionService.eventConsumer("event", 1)
        verify(exactly = 1) { queueDAO.getByTenant(any(), any(), any()) }
        verify(exactly = 0) { completenessDAO.update(any(), any()) }
        verify(exactly = 0) { completenessDAO.create(any()) }
    }

    @Test
    fun `finds an event and updates it`() {
        val newBackfillId = UUID.randomUUID()
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.backfill
        every { event.metadata } returns mockk {
            every { backfillRequest } returns mockk {
                every { backfillId } returns newBackfillId.toString()
            }
            every { upstreamReferences } returns listOf(
                mockk {
                    every { resourceType } returns ResourceType.Patient
                    every { id } returns "tenant-123"
                }
            )
        }
        every { event.tenantId } returns "tenant"
        every { event.resourceType } returns ResourceType.Appointment

        val newQueueId = UUID.randomUUID()
        every {
            queueDAO.getByTenant(
                tenant = "tenant",
                status = BackfillStatus.STARTED,
                backfillId = newBackfillId
            )
        } returns listOf(
            mockk {
                every { entryId } returns newQueueId
                every { patientId } returns "123"
            }
        )

        every { completenessDAO.getByID(newQueueId) } returns mockk {
            every { lastSeen } returns OffsetDateTime.ofInstant(Instant.ofEpochMilli(2), ZoneOffset.UTC)
            every { queueId } returns newQueueId
        }
        every { completenessDAO.update(newQueueId, any()) } just runs
        completionService.eventConsumer("event", 10)
        verify(exactly = 1) { queueDAO.getByTenant(any(), any(), any()) }
        verify(exactly = 1) { completenessDAO.update(any(), any()) }
        verify(exactly = 0) { completenessDAO.create(any()) }
    }

    @Test
    fun `finds no event, so it creates`() {
        val newBackfillId = UUID.randomUUID()
        every { event.dataTrigger } returns InteropResourcePublishV1.DataTrigger.backfill
        every { event.metadata } returns mockk {
            every { backfillRequest } returns mockk {
                every { backfillId } returns newBackfillId.toString()
            }
            every { upstreamReferences } returns listOf(
                mockk {
                    every { resourceType } returns ResourceType.Patient
                    every { id } returns "tenant-123"
                }
            )
        }
        every { event.tenantId } returns "tenant"
        every { event.resourceType } returns ResourceType.Appointment

        val newQueueId = UUID.randomUUID()
        every {
            queueDAO.getByTenant(
                tenant = "tenant",
                status = BackfillStatus.STARTED,
                backfillId = newBackfillId
            )
        } returns listOf(
            mockk {
                every { entryId } returns newQueueId
                every { patientId } returns "123"
            }
        )

        every { completenessDAO.getByID(newQueueId) } returns null
        every { completenessDAO.create(any()) } just runs
        completionService.eventConsumer("event", 10)
        verify(exactly = 1) { queueDAO.getByTenant(any(), any(), any()) }
        verify(exactly = 1) { completenessDAO.getByID(any()) }
        verify(exactly = 0) { completenessDAO.update(any(), any()) }
        verify(exactly = 1) { completenessDAO.create(any()) }
    }
}
