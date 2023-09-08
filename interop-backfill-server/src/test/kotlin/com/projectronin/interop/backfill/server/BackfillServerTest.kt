package com.projectronin.interop.backfill.server

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BackfillServerTest {

    @Test
    fun `returns true`() {
        Assertions.assertTrue(BackfillServer().isTrue())
    }
}
