package com.projectronin.interop.backfill.client.spring

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "backfill")
data class BackfillClientConfig(
    @Valid
    val server: Server = Server(),
)

data class Server(
    @NotEmpty val url: String = "",
)
