plugins {
    alias(libs.plugins.interop.gradle.docker.integration)
}

dependencies {
    api(enforcedPlatform(libs.kotlin.bom))
    implementation(platform(libs.spring.boot.parent))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.oauth)
    implementation(libs.spring.boot.starter.web)

    implementation(libs.spring.kafka)

    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    implementation(libs.interop.commonKtorm)
    implementation(libs.interop.kafka.events.internal)
    implementation(libs.interop.fhir)

    implementation(libs.ktorm.core)
    implementation(libs.ktorm.support.mysql)
    implementation(libs.ronin.kafka)
    implementation(libs.springdoc.openapi.ui)

    runtimeOnly(libs.liquibase.core)
    // Needed to format logs for DataDog
    runtimeOnly(libs.logstash.logback.encoder)
    runtimeOnly(libs.mysql.connector.java)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.interop.fhir.generators)
    testImplementation(libs.interop.commonTestDb)
    testImplementation(libs.rider.core)
    testImplementation(libs.ronin.test.data.generator)

    testRuntimeOnly("org.testcontainers:mysql")

    itImplementation(project(":interop-backfill-client"))
    itImplementation(project)
    itImplementation(platform(libs.testcontainers.bom))
    itImplementation("org.testcontainers:testcontainers")
    itImplementation(libs.bundles.ktor)
    itImplementation(libs.kotlinx.coroutines.core)
    itImplementation(libs.interop.commonHttp)
    itImplementation(libs.interop.commonJackson)
    itImplementation(libs.interop.commonKtorm)
    itImplementation(libs.interop.kafka.events.internal)
    itImplementation(libs.interop.fhir)
    itImplementation(libs.interop.fhir.generators)
    itImplementation(libs.interop.kafka)
    itImplementation(libs.interop.kafka.testing.client)
    itImplementation(libs.ronin.test.data.generator)
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$projectDir/backfill-api.yaml")
    outputDir.set("$buildDir/generated")
    ignoreFileOverride.set("$projectDir/.openapi-generator-ignore")
    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "enumPropertyNaming" to "UPPERCASE",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "packageName" to "com.projectronin.interop.backfill.server.generated",
            "basePackage" to "com.projectronin.interop.backfill.server.generated",
            "gradleBuildFile" to "false",
        ),
    )
}

testing {
    suites {
        // val test by getting(JvmTestSuite::class) {
        //     useJUnitJupiter()
        // }
        //
        // // val it by registering(JvmTestSuite::class) {
        // //     dependencies {
        // //         implementation(project())
        // //     }
        // //     sources {
        // //         java {
        // //             setSrcDirs(listOf("src/it/kotlin"))
        // //         }
        // //         resources {
        // //             setSrcDirs(listOf("src/it/resources"))
        // //         }
        // //     }
        // // }
        val validation by registering(JvmTestSuite::class) {
            dependencies {
                implementation(project())
                implementation(project(":interop-backfill-client"))
                implementation(libs.interop.commonHttp)
                implementation(libs.kotlinx.coroutines.core)
            }
            sources {
                java {
                    setSrcDirs(listOf("src/validation/kotlin"))
                }
                resources {
                    setSrcDirs(listOf("src/validation/resources"))
                }
            }

            targets {
                all {
                    val passthroughProperties =
                        listOf(
                            "token_server_url",
                            "backfill_server_url",
                            "backfill_audience",
                            "backfill_client_id",
                            "backfill_client_secret",
                        )
                    testTask.configure {
                        passthroughProperties.forEach {
                            systemProperties[it] = System.getProperty(it) ?: System.getenv(it)
                        }
                    }
                }
            }
        }
    }
}
