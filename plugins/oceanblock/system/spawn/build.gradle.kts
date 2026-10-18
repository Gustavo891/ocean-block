plugins {
    id("paper-common")
}

dependencies {
    compileOnly(files(rootProject.file("jars/ModelEngine-4.1.0.jar")))
    compileOnly(fileTree("$rootDir/.dep"))
    compileOnly(libs.packetevents.spigot)

    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.rewards)
    compileOnly(projects.plugins.textura)
    compileOnly(projects.plugins.oceanblock.system.islands)
}
