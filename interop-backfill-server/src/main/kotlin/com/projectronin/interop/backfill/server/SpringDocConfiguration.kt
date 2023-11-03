package com.projectronin.interop.backfill.server

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.Generated

// This was originally generated, but copied here to prevent some issues with the generator requiring Spring Boot 3
@Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"])
@Configuration
class SpringDocConfiguration {
    @Bean
    fun apiInfo(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Backfill Service")
                    .description("The Backfill provides APIs loading backfills of data for a tenant.")
                    .version("1.0.0")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "auth0",
                        SecurityScheme() // This is defined as oauth2 within the OpenAPI spec to match the actual use, but is defined as Bearer here to allow for "Try It Out" support that is not available with Swagger and Auth0
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
    }
}
