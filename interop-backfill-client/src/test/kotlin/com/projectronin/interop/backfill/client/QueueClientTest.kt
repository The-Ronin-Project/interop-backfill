package com.projectronin.interop.backfill.client

import com.projectronin.interop.backfill.client.generated.models.BackfillStatus
import com.projectronin.interop.backfill.client.generated.models.GeneratedId
import com.projectronin.interop.backfill.client.generated.models.NewQueueEntry
import com.projectronin.interop.backfill.client.generated.models.QueueEntry
import com.projectronin.interop.backfill.client.generated.models.UpdateQueueEntry
import com.projectronin.interop.backfill.client.spring.BackfillClientConfig
import com.projectronin.interop.backfill.client.spring.Server
import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.jackson.JacksonManager
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class QueueClientTest {
    private val mockWebServer = MockWebServer()
    private val hostUrl = mockWebServer.url("/test")
    private val authenticationToken = "123456"
    private val authenticationService =
        mockk<InteropAuthenticationService> {
            every { getAuthentication() } returns
                mockk {
                    every { accessToken } returns authenticationToken
                }
        }
    private val httpClient = HttpSpringConfig().getHttpClient()
    private val client = QueueClient(httpClient, BackfillClientConfig(Server(hostUrl.toString())), authenticationService)
    private val expectedQueueEntry =
        QueueEntry(
            id = UUID.randomUUID(),
            backfillId = UUID.randomUUID(),
            patientId = "123",
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            tenantId = "123",
            status = BackfillStatus.STARTED,
        )

    @Test
    fun `getEntriesByBackfillID - works`() {
        val expectedQueueEntryJson = JacksonManager.objectMapper.writeValueAsString(listOf(expectedQueueEntry))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(expectedQueueEntryJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.getEntriesByBackfillID(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                )
            }
        assertEquals(listOf(expectedQueueEntry), response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(true, request.path?.endsWith("/queue/backfill/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getQueueEntries - works`() {
        val expectedQueueEntryJson = JacksonManager.objectMapper.writeValueAsString(listOf(expectedQueueEntry))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(expectedQueueEntryJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.getQueueEntries(
                    "tenant",
                )
            }
        assertEquals(listOf(expectedQueueEntry), response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(true, request.path?.endsWith("/queue?tenant_id=tenant"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getQueueEntryById - works`() {
        val expectedQueueEntryJson = JacksonManager.objectMapper.writeValueAsString(expectedQueueEntry)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(expectedQueueEntryJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.getQueueEntryById(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                )
            }
        assertEquals(expectedQueueEntry, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(true, request.path?.endsWith("/queue/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `postQueueEntry - works`() {
        val newQueueEntry =
            NewQueueEntry(
                backfillId = UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                patientId = "123",
            )
        val generatedId = GeneratedId(UUID.randomUUID())
        val expectedNewQueueEntryJson = JacksonManager.objectMapper.writeValueAsString(listOf(newQueueEntry))
        val expectedGeneratedIds = JacksonManager.objectMapper.writeValueAsString(listOf(generatedId))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(expectedGeneratedIds)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.postQueueEntry(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                    listOf(newQueueEntry),
                )
            }
        assertEquals(listOf(generatedId), response)
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals(true, request.path?.endsWith("/queue/backfill/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals(expectedNewQueueEntryJson, String(request.body.readByteArray()))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `updateQueueEntryByID - works`() {
        val updatedQueueEntry =
            UpdateQueueEntry(
                status = BackfillStatus.COMPLETED,
            )
        val expectedNewQueueEntryJson = JacksonManager.objectMapper.writeValueAsString(updatedQueueEntry)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody("true")
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.updateQueueEntryByID(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                    updatedQueueEntry,
                )
            }
        assertEquals(true, response)
        val request = mockWebServer.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals(true, request.path?.endsWith("/queue/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals(expectedNewQueueEntryJson, String(request.body.readByteArray()))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `deleteQueueEntryById - works`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody("true")
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.deleteQueueEntryById(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                )
            }
        assertEquals(true, response)
        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals(true, request.path?.endsWith("/queue/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }
}
