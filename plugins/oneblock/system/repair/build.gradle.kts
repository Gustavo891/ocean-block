plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.plugins.oneblock.system.craft)
    compileOnly(projects.plugins.oneblock.system.mobDrops)
}
