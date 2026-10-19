plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.plugins.textura)

    testImplementation(libs.paperApi)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}
