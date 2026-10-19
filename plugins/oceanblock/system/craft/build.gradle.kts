plugins {
    id("paper-common")
}

dependencies {
    testImplementation(libs.paperApi)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}
