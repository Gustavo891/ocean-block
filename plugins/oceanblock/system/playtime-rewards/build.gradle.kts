plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.plugins.textura)
    compileOnly(libs.mongodb)
    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.rewards)

    compileOnly(projects.plugins.oceanblock.system.currencies)
    compileOnly(projects.plugins.oceanblock.system.level)
    compileOnly(projects.plugins.oceanblock.system.playtime)

    testImplementation(libs.paperApi)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("localTest") {
    group = "verification"
    dependsOn(tasks.named("test"))
}
