plugins {
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.jakarta.validation.api)

    testImplementation(libs.mockk)
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
            "documentationProvider" to "none" // Prevent Swagger annotations
        )
    )
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "models" to ""
        )
    )
}
