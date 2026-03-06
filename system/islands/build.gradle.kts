plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)


    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.system.currencies)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.translation)

    compileOnly("io.github.toxicity188:BetterHud-standard-api:1.14.1")
    compileOnly("io.github.toxicity188:BetterHud-bukkit-api:1.14.1")
    compileOnly("io.github.toxicity188:BetterCommand:1.4.3")

    compileOnly(files(rootProject.file("jars/inventory-framework-paper-3.7.1.jar")))
}
