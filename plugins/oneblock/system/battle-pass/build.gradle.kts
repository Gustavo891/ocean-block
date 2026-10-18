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
    compileOnly(projects.plugins.oneblock.system.currencies)
    compileOnly(projects.plugins.oneblock.system.islands)
    compileOnly(projects.plugins.oneblock.system.sell)
    compileOnly(projects.plugins.oneblock.system.craft)
}
