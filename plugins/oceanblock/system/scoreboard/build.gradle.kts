plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)

    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.scoreboard)
    compileOnly(projects.plugins.oceanblock.system.spawn)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}
