package com.projectronin.interop.backfill.server

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackfillConfigTest {

    @Test
    fun `defaults work`() {
        val backfillConfig = BackfillConfig()
        assertEquals(300000, backfillConfig.kafka.listenerRefresh)
    }

    @Test
    fun `non defaults work`() {
        val backfillConfig = BackfillConfig(BackfillKafkaConfig(100))
        assertEquals(100, backfillConfig.kafka.listenerRefresh)
    }
}
