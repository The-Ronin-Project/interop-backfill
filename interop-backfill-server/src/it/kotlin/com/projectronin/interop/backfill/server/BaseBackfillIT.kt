package com.projectronin.interop.backfill.server

import com.fasterxml.jackson.databind.JsonNode
import com.projectronin.interop.backfill.server.data.BackfillDAO
import com.projectronin.interop.backfill.server.data.BackfillQueueDAO
import com.projectronin.interop.backfill.server.data.DiscoveryQueueDAO
import com.projectronin.interop.backfill.server.data.binding.BackfillDOs
import com.projectronin.interop.backfill.server.data.binding.BackfillQueueDOs
import com.projectronin.interop.backfill.server.data.binding.DiscoveryQueueDOs
import com.projectronin.interop.backfill.server.generated.models.GeneratedId
import com.projectronin.interop.backfill.server.generated.models.NewBackfill
import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.ktorm.database.Database
import org.ktorm.dsl.deleteAll
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Instant
import java.time.LocalDate

abstract class BaseBackfillIT {
    companion object {
        val docker = DockerComposeContainer(File(BaseBackfillIT::class.java.getResource("/docker-compose-it.yaml")!!.file))
            .withExposedService("backfill-server", 8080)
            .withExposedService("mysql-server", 3306)
            .withExposedService("mock-oauth2", 8080)

        val start = docker
            .waitingFor("backfill-server", Wait.forLogMessage(".*Started BackfillServerKt.*", 1))
            .start()
    }
    private val serverPort by lazy {
        docker.getServicePort("backfill-server", 8080)
    }

    private val dbPort by lazy {
        docker.getServicePort("mysql-server", 3306)
    }

    private val mockAuthPort by lazy {
        docker.getServicePort("mock-oauth2", 8080)
    }

    protected val serverUrl = "http://localhost:$serverPort"
    protected val httpClient = HttpSpringConfig().getHttpClient()
    protected val database = Database.connect(url = "jdbc:mysql://springuser:ThePassword@localhost:$dbPort/backfill-db")
    protected val backfillDAO = BackfillDAO(database)
    protected val queueDAO = BackfillQueueDAO(database)
    protected val discoveryDAO = DiscoveryQueueDAO(database)

    @AfterEach
    fun tearDown() {
        purgeData()
    }

    private fun purgeData() {
        database.deleteAll(DiscoveryQueueDOs)
        database.deleteAll(BackfillQueueDOs)
        database.deleteAll(BackfillDOs)
    }

    // This should really live in the client, but putting this here for now
    protected fun retrieveFormBasedAuthentication(): Authentication = runBlocking {
        val json: JsonNode = httpClient.submitForm(
            url = "http://localhost:$mockAuthPort/backfill/token",
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", "id")
                append("client_secret", "secret")
            }
        ).body()
        val accessToken = json.get("access_token").asText()
        FormBasedAuthentication(accessToken)
    }

    protected fun newBackFill(): GeneratedId {
        val backFill = NewBackfill(
            locationIds = listOf("123", "456"),
            startDate = LocalDate.of(2023, 9, 1),
            endDate = LocalDate.of(2022, 9, 1),
            tenantId = "tenantId"
        )

        val response = runBlocking {
            httpClient.post("$serverUrl/backfill") {
                bearerAuth(retrieveFormBasedAuthentication().accessToken)
                setBody(backFill)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
        return runBlocking { response.body<GeneratedId>() }
    }

    data class FormBasedAuthentication(override val accessToken: String) : Authentication {
        override val tokenType: String = "Bearer"
        override val expiresAt: Instant? = null
        override val refreshToken: String? = null
        override val scope: String? = null
    }
}
