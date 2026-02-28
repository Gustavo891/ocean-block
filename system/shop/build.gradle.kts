plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)


    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.system.currencies)

    compileOnly("me.devnatan:inventory-framework-platform-bukkit:3.7.1")
}
