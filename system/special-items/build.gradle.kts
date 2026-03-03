plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)


    compileOnly(libs.mongodb)

    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.system.sell)

    compileOnly(projects.system.currencies)
    compileOnly(projects.system.islands)
}
