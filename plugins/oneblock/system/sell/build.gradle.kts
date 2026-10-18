plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)
    compileOnly(libs.mongodb)

    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.plugins.oneblock.system.currencies)
    compileOnly(projects.plugins.oneblock.system.islands)
}
