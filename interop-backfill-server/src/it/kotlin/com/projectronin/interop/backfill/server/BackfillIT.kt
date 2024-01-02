package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.client.generated.models.NewBackfill
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BackfillIT : BaseBackfillIT() {
    @Test
    fun `post works`() {
        val backFill =
            NewBackfill(
                locationIds = listOf("123", "456"),
                startDate = LocalDate.of(2022, 9, 1),
                endDate = LocalDate.of(2023, 9, 1),
                tenantId = "tenantId",
            )

        val id = runBlocking { backfillClient.postBackfill(backFill) }

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
        val backfill = runBlocking { backfillClient.getBackfillById(id) }
        assertNotNull(backfill)
        assertEquals(id, backfill.id)
        assertFalse(backfill.locationIds.isEmpty())
    }

    @Test
    fun `get by id works`() {
        val id = newBackFill()
        val backfill = runBlocking { backfillClient.getBackfillById(id) }
        assertNotNull(backfill)
        assertEquals(id, backfill.id)
        assertFalse(backfill.locationIds.isEmpty())
    }

    @Test
    fun `get by id can 404`() {
        newBackFill()
        val result =
            runCatching {
                runBlocking { backfillClient.getBackfillById(UUID.randomUUID()) }
            }

        assertTrue(result.isFailure)
        assertInstanceOf(ClientFailureException::class.java, result.exceptionOrNull())
    }

    @Test
    fun `delete works`() {
        val id = newBackFill()
        val result = runBlocking { backfillClient.deleteBackfill(id) }
        assertTrue(result)
    }
}
