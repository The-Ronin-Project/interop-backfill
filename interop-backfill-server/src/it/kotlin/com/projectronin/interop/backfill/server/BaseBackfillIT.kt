package com.projectronin.interop.backfill.server

import com.projectronin.interop.common.http.spring.HttpSpringConfig
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

abstract class BaseBackfillIT {
    companion object {
        val docker = DockerComposeContainer(File(BaseBackfillIT::class.java.getResource("/docker-compose-it.yaml")!!.file))
            .withExposedService("backfill-server", 8080)

        val start = docker
            .waitingFor("backfill-server", Wait.forLogMessage(".*Started BackfillServerKt.*", 1))
            .start()
    }
    private val port by lazy {
        docker.getServicePort("backfill-server", 8080)
    }
    protected val serverUrl = "http://localhost:$port"
    protected val httpClient = HttpSpringConfig().getHttpClient()
}
