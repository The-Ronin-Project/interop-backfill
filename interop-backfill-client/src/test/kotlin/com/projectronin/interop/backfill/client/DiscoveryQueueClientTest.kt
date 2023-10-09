package com.projectronin.interop.backfill.client

import com.projectronin.interop.backfill.client.generated.models.DiscoveryQueueEntry
import com.projectronin.interop.backfill.client.generated.models.DiscoveryQueueStatus
import com.projectronin.interop.backfill.client.generated.models.UpdateDiscoveryEntry
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
class DiscoveryQueueClientTest {
    private val mockWebServer = MockWebServer()
    private val hostUrl = mockWebServer.url("/test")
    private val authenticationToken = "123456"
    private val authenticationService = mockk<InteropAuthenticationService> {
        every { getAuthentication() } returns mockk {
            every { accessToken } returns authenticationToken
        }
    }
    private val httpClient = HttpSpringConfig().getHttpClient()

    private val client = DiscoveryQueueClient(httpClient, BackfillClientConfig(Server(hostUrl.toString())), authenticationService)
    private val expectedDiscoveryEntry = DiscoveryQueueEntry(
        id = UUID.randomUUID(),
        backfillId = UUID.randomUUID(),
        locationId = "123",
        startDate = LocalDate.now(),
        endDate = LocalDate.now(),
        tenantId = "123",
        status = DiscoveryQueueStatus.UNDISCOVERED
    )

    @Test
    fun `getDiscoveryQueueEntries - works`() {
        val expectedDiscoveryEntryJson = JacksonManager.objectMapper.writeValueAsString(listOf(expectedDiscoveryEntry))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(expectedDiscoveryEntryJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.getDiscoveryQueueEntries(
                "tenant",
                DiscoveryQueueStatus.DISCOVERED,
                UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5")
            )
        }
        assertEquals(listOf(expectedDiscoveryEntry), response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(true, request.path?.endsWith("/discovery-queue?tenant_id=tenant&status=DISCOVERED&backfill_id=1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getDiscoveryQueueEntryById - works`() {
        val expectedDiscoveryEntryJson = JacksonManager.objectMapper.writeValueAsString(expectedDiscoveryEntry)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(expectedDiscoveryEntryJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.getDiscoveryQueueEntryById(
                UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5")
            )
        }
        assertEquals(expectedDiscoveryEntry, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(true, request.path?.endsWith("/discovery-queue/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `updateDiscoveryQueueEntryByID - works`() {
        val updateDiscoveryEntry = UpdateDiscoveryEntry(DiscoveryQueueStatus.UNDISCOVERED)
        val expectedDiscoveryEntryJson = JacksonManager.objectMapper.writeValueAsString(updateDiscoveryEntry)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody("true")
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.updateDiscoveryQueueEntryByID(
                UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                updateDiscoveryEntry
            )
        }
        assertEquals(true, response)
        val request = mockWebServer.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals(true, request.path?.endsWith("/discovery-queue/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals(expectedDiscoveryEntryJson, String(request.body.readByteArray()))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `deleteDiscoveryQueueEntryById - works`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody("true")
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.deleteDiscoveryQueueEntryById(UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"))
        }
        assertEquals(true, response)
        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals(true, request.path?.endsWith("/discovery-queue/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }
}
