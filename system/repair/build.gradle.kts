plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.system.craft)
    compileOnly(projects.system.mobDrops)
}
