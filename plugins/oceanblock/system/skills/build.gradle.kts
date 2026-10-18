plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.mongodb)
    compileOnly(libs.placeholder)
    compileOnly(libs.packetevents.spigot)

    compileOnly(projects.lib.actionbar)
    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.rewards)

    compileOnly(projects.plugins.textura)

    compileOnly(projects.plugins.oceanblock.system.craft)
    compileOnly(projects.plugins.oceanblock.system.currencies)
}
