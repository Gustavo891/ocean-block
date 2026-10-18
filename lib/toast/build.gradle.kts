plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.packetevents.spigot)

    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
}
