package com.projectronin.interop.backfill.server

import com.projectronin.interop.backfill.client.BackfillClient
import com.projectronin.interop.backfill.client.DiscoveryQueueClient
import com.projectronin.interop.backfill.client.QueueClient
import com.projectronin.interop.backfill.client.generated.models.NewBackfill
import com.projectronin.interop.backfill.client.spring.BackfillClientConfig
import com.projectronin.interop.backfill.client.spring.Server
import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.CompletenessDAO
import com.projectronin.interop.backfill.server.data.DiscoveryQueueDAO
import com.projectronin.interop.backfill.server.data.binding.BackfillDOs
import com.projectronin.interop.backfill.server.data.binding.BackfillQueueDOs
import com.projectronin.interop.backfill.server.data.binding.CompletenessDOs
import com.projectronin.interop.backfill.server.data.binding.DiscoveryQueueDOs
import com.projectronin.interop.common.http.auth.AuthMethod
import com.projectronin.interop.common.http.auth.AuthenticationConfig
import com.projectronin.interop.common.http.auth.Client
import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.auth.Token
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.ktorm.database.Database
import org.ktorm.dsl.deleteAll
import java.time.LocalDate
import java.util.UUID

abstract class BaseBackfillIT {
    protected val serverUrl = "http://localhost:8080"
    protected val httpClient = HttpSpringConfig().getHttpClient()
    protected val database = Database.connect(url = "jdbc:mysql://springuser:ThePassword@localhost:3306/backfill-db")
    protected val backfillDAO = BackfillDAO(database)
    protected val queueDAO = BackfillQueueDAO(database)
    protected val discoveryDAO = DiscoveryQueueDAO(database)
    protected val completenessDAO = CompletenessDAO(database)

    @AfterEach
    fun tearDown() {
        purgeData()
    }

    private fun purgeData() {
        database.deleteAll(CompletenessDOs)
        database.deleteAll(DiscoveryQueueDOs)
        database.deleteAll(BackfillQueueDOs)
        database.deleteAll(BackfillDOs)
    }

    protected fun newBackFill(): UUID {
        val backFill =
            NewBackfill(
                locationIds = listOf("123", "456"),
                startDate = LocalDate.of(2023, 9, 1),
                endDate = LocalDate.of(2022, 9, 1),
                tenantId = "tenantId",
            )

        return runBlocking { backfillClient.postBackfill(backFill) }.id!!
    }

    protected val authenticationService =
        InteropAuthenticationService(
            httpClient,
            authConfig =
                AuthenticationConfig(
                    token = Token("http://localhost:8081/backfill/token"),
                    audience = "https://interop-backfill.dev.projectronin.io",
                    client =
                        Client(
                            id = "id",
                            secret = "secret",
                        ),
                    method = AuthMethod.STANDARD,
                ),
        )
    val config = BackfillClientConfig(server = Server("http://localhost:8080"))
    protected val backfillClient = BackfillClient(httpClient, config, authenticationService)
    protected val discoveryClient = DiscoveryQueueClient(httpClient, config, authenticationService)
    protected val queueClient = QueueClient(httpClient, config, authenticationService)
}
