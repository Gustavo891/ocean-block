plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.plugins.textura)

    compileOnly(projects.plugins.oneblock.system.islands)
    compileOnly(projects.plugins.oneblock.system.craft)
    compileOnly(projects.plugins.oneblock.system.currencies)
    compileOnly(projects.plugins.oneblock.system.minions)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}

// O modo do build vem do nome da task conter "local" (ver CLAUDE.md). Um `test`
// puro resolveria `com.redescreen:sdk`, que só existe em produção — daí este alias.
tasks.register("localTest") {
    group = "verification"
    description = "Roda os testes do módulo em modo local."
    dependsOn(tasks.named("test"))
}
