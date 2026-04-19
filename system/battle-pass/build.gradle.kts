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
    compileOnly(projects.system.currencies)
    compileOnly(projects.system.islands)
    compileOnly(projects.system.sell)
    compileOnly(projects.system.craft)
}
