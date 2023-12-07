plugins {
    alias(libs.plugins.interop.gradle.docker.integration) apply false
    alias(libs.plugins.interop.gradle.junit) apply false
    alias(libs.plugins.interop.gradle.spring.boot) apply false
    alias(libs.plugins.interop.gradle.spring.framework) apply false
    alias(libs.plugins.interop.gradle.server.version)
    alias(libs.plugins.interop.gradle.version.catalog)
    alias(libs.plugins.interop.gradle.sonarqube)

    alias(libs.plugins.openapi.generator) apply false
    // We need to force IntelliJ to do some actions they expose through this plugin.
    alias(libs.plugins.idea.ext)
}

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.base")
    if (project.name != "interop-backfill-server") {
        apply(plugin = "com.projectronin.interop.gradle.server-publish")
    }
}

val openapiProjects = listOf(project(":interop-backfill-client"), project(":interop-backfill-server"))

configure(openapiProjects) {
    apply(plugin = "org.openapi.generator")

    afterEvaluate {
        tasks {
            val openApiGenerate by getting

            sourceSets {
                main {
                    java {
                        srcDir(openApiGenerate)
                    }
                }
            }

            val compileJava by getting

            // Fixes some implicit dependency mess caused by the above
            val sourcesJar by getting {
                dependsOn(compileJava)
            }
        }

        ktlint {
            filter {
                exclude {
                    it.file.path.contains("/generated/")
                }
            }
        }

        tasks.withType(JacocoReport::class.java).forEach {
            afterEvaluate {
                it.classDirectories.setFrom(
                    files(
                        it.classDirectories.files.map {
                            fileTree(it).apply {
                                exclude(
                                    "**/generated/**",
                                )
                            }
                        },
                    ),
                )
            }
        }
    }
}
