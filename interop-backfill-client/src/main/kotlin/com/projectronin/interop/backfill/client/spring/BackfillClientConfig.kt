package com.projectronin.interop.backfill.client.spring

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "backfill")
data class BackfillClientConfig(
    val server: Server
)

data class Server(val url: String)
