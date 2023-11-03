package com.projectronin.interop.backfill.server

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "backfill")
@ConstructorBinding
data class BackfillConfig(
    val kafka: BackfillKafkaConfig = BackfillKafkaConfig()
)

data class BackfillKafkaConfig(
    val listenerRefresh: Int = 300000 // used by testing to more quickly discovery topics
)
