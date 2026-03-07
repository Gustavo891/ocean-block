plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)


    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.system.currencies)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.translation)

    compileOnly(projects.system.mobDrops)
}
