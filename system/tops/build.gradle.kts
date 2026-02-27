plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)
    compileOnly(libs.protocol)

    compileOnly(libs.mongodb)

    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.system.currencies)
    compileOnly(projects.system.playtime)

    compileOnly("me.devnatan:inventory-framework-platform-bukkit:3.7.1")
}
