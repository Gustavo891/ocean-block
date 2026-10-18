plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)


    compileOnly(libs.mongodb)

    compileOnly(projects.lib.actionbar)
    compileOnly(projects.lib.connection)
    compileOnly(projects.plugins.textura)
    compileOnly(projects.plugins.oneblock.system.islands)
    compileOnly(projects.plugins.oneblock.system.spawn)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.plugins.oneblock.system.currencies)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.translation)
    compileOnly(projects.plugins.oneblock.system.mobDrops)
}
