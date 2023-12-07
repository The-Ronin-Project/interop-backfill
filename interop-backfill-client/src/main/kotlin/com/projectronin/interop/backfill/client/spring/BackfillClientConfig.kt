package com.projectronin.interop.backfill.client.spring

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

@ConfigurationProperties(prefix = "backfill")
@ConstructorBinding
data class BackfillClientConfig(
    @Valid
    val server: Server = Server(),
)

data class Server(
    @NotEmpty val url: String = "",
)
