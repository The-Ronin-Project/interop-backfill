package com.projectronin.interop.backfill.client

import com.projectronin.interop.backfill.client.generated.models.Backfill
import com.projectronin.interop.backfill.client.generated.models.BackfillStatus
import com.projectronin.interop.backfill.client.generated.models.GeneratedId
import com.projectronin.interop.backfill.client.generated.models.NewBackfill
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BackfillClientTest {
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
    private val client =
        BackfillClient(httpClient, BackfillClientConfig(Server(hostUrl.toString())), authenticationService)
    private val expectedBackfill =
        Backfill(
            id = UUID.randomUUID(),
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            tenantId = "123",
            locationIds = listOf("1", "2"),
            status = BackfillStatus.STARTED,
        )

    @Test
    fun `getBackfillById works`() {
        val backFillJson = JacksonManager.objectMapper.writeValueAsString(expectedBackfill)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(backFillJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.getBackfillById(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                )
            }
        assertEquals(expectedBackfill, response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(true, request.path?.endsWith("/backfill/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getBackfills works`() {
        val backfillJson = JacksonManager.objectMapper.writeValueAsString(listOf(expectedBackfill))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(backfillJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.getBackfills(
                    "tenant",
                )
            }
        assertEquals(listOf(expectedBackfill), response)
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals(true, request.path?.endsWith("/backfill?tenant_id=tenant"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `postBackfill works`() {
        val newUUID =
            JacksonManager.objectMapper.writeValueAsString(GeneratedId(UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5")))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(newUUID)
                .setHeader("Content-Type", "application/json"),
        )
        val newBackfill = NewBackfill("tenant", listOf("1", "2"), LocalDate.now(), LocalDate.now())

        val response =
            runBlocking {
                client.postBackfill(
                    newBackfill = newBackfill,
                )
            }
        val expectedResponse = GeneratedId(UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals(expectedResponse, response)
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals(true, request.path?.endsWith("/backfill"))
        assertEquals(JacksonManager.objectMapper.writeValueAsString(newBackfill), String(request.body.readByteArray()))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `deleteBackFill works`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody("true")
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.deleteBackfill(UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"))
            }
        assertTrue(response)
        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals(true, request.path?.endsWith("/backfill/1d531a31-49a9-af74-03d5-573b456efca5"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }
}
