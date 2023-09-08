package com.projectronin.interop.backfill.client

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BackfillClientTest {

    @Test
    fun `returns true`() {
        assertTrue(BackfillClient().isTrue())
    }
}
