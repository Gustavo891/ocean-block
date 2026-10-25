plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.plugins.textura)
    compileOnly(libs.mongodb)
    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)

    compileOnly(projects.plugins.oceanblock.system.craft)
    compileOnly(projects.plugins.oceanblock.system.drift)
    compileOnly(projects.plugins.oceanblock.system.islands)
    compileOnly(projects.plugins.oceanblock.system.level)
    compileOnly(projects.plugins.oceanblock.system.sell)

    testImplementation(libs.paperApi)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(projects.plugins.oceanblock.system.level)
    testImplementation(projects.plugins.oceanblock.system.scoreboard)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("localTest") {
    group = "verification"
    dependsOn(tasks.named("test"))
}
