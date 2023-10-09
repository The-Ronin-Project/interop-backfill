package com.projectronin.interop.backfill.client.spring

import com.projectronin.interop.common.http.auth.AuthenticationSpringConfig
import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import io.ktor.client.HttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.validation.annotation.Validated

@ConfigurationPropertiesScan(basePackageClasses = [BackfillClientConfig::class])
@ComponentScan("com.projectronin.interop.backfill.client")
@Import(HttpSpringConfig::class)
class BackfillClientSpringConfig {
    @Bean
    @Qualifier("backfill")
    @ConfigurationProperties(prefix = "backfill.auth")
    @Validated
    fun authConfig(): AuthenticationSpringConfig {
        return AuthenticationSpringConfig()
    }

    @Bean
    @Qualifier("backfill")
    fun authService(httpClient: HttpClient): InteropAuthenticationService {
        return InteropAuthenticationService(httpClient, authConfig())
    }
}
