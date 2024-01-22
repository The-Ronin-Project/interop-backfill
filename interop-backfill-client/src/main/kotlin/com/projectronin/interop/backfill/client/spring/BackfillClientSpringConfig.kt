package com.projectronin.interop.backfill.client.spring

import com.projectronin.interop.common.http.auth.AuthenticationConfig
import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import io.ktor.client.HttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@Configuration
@ConfigurationPropertiesScan(basePackageClasses = [BackfillClientConfig::class])
@ComponentScan("com.projectronin.interop.backfill.client")
@Import(HttpSpringConfig::class)
class BackfillClientSpringConfig {
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "backfill.auth")
    fun authConfig(): AuthenticationConfig {
        return AuthenticationConfig()
    }

    @Bean
    @Qualifier("backfill")
    fun authService(
        httpClient: HttpClient,
        @Qualifier("authConfig")
        authenticationConfig: AuthenticationConfig,
    ): InteropAuthenticationService {
        return InteropAuthenticationService(httpClient, authenticationConfig)
    }
}
