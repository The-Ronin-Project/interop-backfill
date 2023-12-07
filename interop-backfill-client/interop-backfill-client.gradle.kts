plugins {
    alias(libs.plugins.interop.gradle.junit)
    alias(libs.plugins.interop.gradle.spring.framework)
}

dependencies {
    implementation(platform(libs.spring.boot.parent))

    implementation(platform(libs.spring.boot.parent))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.interop.common)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.commonJackson)
    implementation(libs.jakarta.validation.api)

    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$projectDir/../interop-backfill-server/backfill-api.yaml")
    outputDir.set("$buildDir/generated")
    configOptions.set(
        mapOf(
            "enumPropertyNaming" to "UPPERCASE",
            "packageName" to "com.projectronin.interop.backfill.client.generated",
            "gradleBuildFile" to "false",
            // Prevent Swagger annotations
            "documentationProvider" to "none",
        ),
    )
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "models" to "",
        ),
    )
}
