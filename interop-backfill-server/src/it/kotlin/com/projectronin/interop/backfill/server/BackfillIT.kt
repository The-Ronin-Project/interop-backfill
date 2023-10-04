package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.server.generated.models.Backfill
import com.projectronin.interop.backfill.server.generated.models.GeneratedId
import com.projectronin.interop.backfill.server.generated.models.NewBackfill
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BackfillIT : BaseBackfillIT() {
    private val urlPart = "/backfill"

    @Test
    fun `post works`() {
        val backFill = NewBackfill(
            locationIds = listOf("123", "456"),
            startDate = LocalDate.of(2022, 9, 1),
            endDate = LocalDate.of(2023, 9, 1),
            tenantId = "tenantId"
        )

        val response = runBlocking {
            httpClient.post("$serverUrl$urlPart") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                setBody(backFill)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        val id = runBlocking { response.body<GeneratedId>() }
        assertEquals(HttpStatusCode.Created, response.status)
        assertNotNull(id)
        val backfill = backfillDAO.getByTenant("tenantId")
        assertEquals(1, backfill.size)
        assertEquals(id.id, backfill.first().backfillId)
        val entries = discoveryDAO.getByTenant("tenantId")
        assertEquals(2, entries.size)
    }

    @Test
    fun `get works`() {
        val id = newBackFill()
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("tenant_id", "tenantId")
            }
        }
        val backfill = runBlocking { response.body<List<Backfill>>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(backfill)
        assertEquals(1, backfill.size)
        assertEquals(id.id, backfill.first().id)
        assertFalse(backfill.first().locationIds.isEmpty())
    }

    @Test
    fun `get requires tenant`() {
        newBackFill()
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `get by id works`() {
        val id = newBackFill()
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart/${id.id}") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        val backfill = runBlocking { response.body<Backfill>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(backfill)
        assertEquals(id.id, backfill.id)
        assertFalse(backfill.locationIds.isEmpty())
    }

    @Test
    fun `get by id can 404`() {
        newBackFill()
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart/${UUID.randomUUID()}") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `delete works`() {
        val id = newBackFill()
        val response = runBlocking {
            httpClient.delete("$serverUrl$urlPart/${id.id}") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        val result = runBlocking { response.body<Boolean>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(result)
    }
}
