package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.client.BackfillClient
import com.projectronin.interop.backfill.client.spring.BackfillClientConfig
import com.projectronin.interop.backfill.client.spring.Server
import com.projectronin.interop.common.http.auth.AuthMethod
import com.projectronin.interop.common.http.auth.AuthenticationConfig
import com.projectronin.interop.common.http.auth.Client
import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.auth.Token
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.jackson.JacksonUtil
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.util.UUID

class ValidationTest {
    private val serverUrl= System.getProperty("backfill_server_url")
    private val tokenUrl= System.getProperty("token_server_url")
    private val audience= System.getProperty("backfill_audience")
    private val clientId= System.getProperty("backfill_client_id")
    private val clientSecret = System.getProperty("backfill_client_secret")
    protected val httpClient = HttpSpringConfig().getHttpClient()

    protected val authenticationService =  InteropAuthenticationService(
        httpClient,
        authConfig = AuthenticationConfig(
            token = Token(tokenUrl),
            audience = audience,
            client = Client(
                id = clientId,
                secret = clientSecret
            ),
            method = AuthMethod.AUTH0
        )
    )
    val config = BackfillClientConfig(server = Server(serverUrl))
    protected val backfillClient = BackfillClient(httpClient, config, authenticationService)

    @Test
    fun `test`(){
        val backfill = runBlocking { backfillClient.getBackfills("ronin") }
        assertNotNull(backfill)
        println(JacksonUtil.writeJsonValue(backfill))
    }
}