package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.server.data.model.BackfillQueueDO
import com.projectronin.interop.backfill.server.generated.models.BackfillStatus
import com.projectronin.interop.backfill.server.generated.models.GeneratedId
import com.projectronin.interop.backfill.server.generated.models.NewQueueEntry
import com.projectronin.interop.backfill.server.generated.models.QueueEntry
import com.projectronin.interop.backfill.server.generated.models.UpdateQueueEntry
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class QueueIT : BaseBackfillIT() {
    private val urlPart = "/queue"

    @Test
    fun `get works`() {
        val id = newBackFill().id!!
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        newPatientQueue(id, BackfillStatus.COMPLETED)
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("tenant_id", "tenantId")
            }
        }
        val result = runBlocking { response.body<List<QueueEntry>>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(id, result.first().backfillId)
        assertEquals(BackfillStatus.NOT_STARTED, result.first().status)
    }

    @Test
    fun `get returns nothing with an started entry`() {
        val id = newBackFill().id!!
        newPatientQueue(id, BackfillStatus.STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("tenant_id", "tenantId")
            }
        }
        val result = runBlocking { response.body<List<QueueEntry>>() }
        val entries = queueDAO.getByBackfillID(id)
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(result)
        assertEquals(0, result.size)
        assertEquals(2, entries.size)
        assertEquals(id, entries.first().backfillId)
        assertNotNull(entries.filter { it.status == BackfillStatus.STARTED })
        assertNotNull(entries.filter { it.status == BackfillStatus.NOT_STARTED })
    }

    @Test
    fun `get works by backfill `() {
        val id = newBackFill().id!!
        newPatientQueue(id, BackfillStatus.STARTED)
        newPatientQueue(id, BackfillStatus.NOT_STARTED)
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart/backfill/$id") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("tenant_id", "tenantId")
            }
        }
        val result = runBlocking { response.body<List<QueueEntry>>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(id, result.first().backfillId)
        assertNotNull(result.filter { it.status == BackfillStatus.STARTED })
        assertNotNull(result.filter { it.status == BackfillStatus.NOT_STARTED })
    }

    @Test
    fun `get works by id`() {
        val id = newBackFill().id!!
        val entryId = newPatientQueue(id, BackfillStatus.STARTED)
        val response = runBlocking {
            httpClient.get("$serverUrl$urlPart/$entryId") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        val result = runBlocking { response.body<QueueEntry>() }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(id, result.backfillId)
        assertEquals(BackfillStatus.STARTED, result.status)
    }

    @Test
    fun `get can return a 404`() {
        val id = newBackFill().id!!
        newPatientQueue(id, BackfillStatus.STARTED)
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
    fun `post works`() {
        val id = newBackFill().id
        val newEntry = NewQueueEntry(
            backfillId = id!!,
            patientId = "123"
        )

        val response = runBlocking {
            httpClient.post("$serverUrl$urlPart/backfill/$id") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(listOf(newEntry))
            }
        }
        val result = runBlocking { response.body<List<GeneratedId>>() }
        val entries = queueDAO.getByBackfillID(id)
        assertEquals(HttpStatusCode.Created, response.status)
        assertNotNull(entries)
        assertEquals(1, entries.size)
        assertEquals(1, result.size)
        assertEquals(id, entries.first().backfillId)
        assertEquals(result.first().id, entries.first().entryId)
        assertNotNull("123", entries.first().patientId)
    }

    @Test
    fun `patch works`() {
        val id = newBackFill().id!!
        val entryId = newPatientQueue(id, BackfillStatus.STARTED)
        val response = runBlocking {
            httpClient.patch("$serverUrl$urlPart/$entryId") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(UpdateQueueEntry(BackfillStatus.COMPLETED))
            }
        }
        val result = runBlocking { response.body<Boolean>() }
        val entry = queueDAO.getByID(entryId)!!
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(result)
        assertEquals(BackfillStatus.COMPLETED, entry.status)
    }

    @Test
    fun `delete works`() {
        val id = newBackFill().id!!
        val entryId = newPatientQueue(id, BackfillStatus.STARTED)
        val response = runBlocking {
            httpClient.delete("$serverUrl$urlPart/$entryId") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        val result = runBlocking { response.body<Boolean>() }
        val entry = queueDAO.getByID(entryId)
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(result)
        assertEquals(BackfillStatus.DELETED, entry?.status)
    }

    private fun newPatientQueue(backfillID: UUID, entryStatus: BackfillStatus = BackfillStatus.STARTED): UUID {
        return queueDAO.insert(
            BackfillQueueDO {
                backfillId = backfillID
                entryId = UUID.randomUUID()
                patientId = Random(10).toString()
                status = entryStatus
            }
        )!!
    }
}
