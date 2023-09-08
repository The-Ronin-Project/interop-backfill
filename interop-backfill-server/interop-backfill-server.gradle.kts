plugins {
    alias(libs.plugins.interop.gradle.junit)
}

dependencies {
    implementation(libs.interop.common)
    implementation(libs.interop.commonJackson)
    testImplementation(libs.mockk)
}
