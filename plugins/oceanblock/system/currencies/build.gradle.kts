plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)
    //compileOnly(libs.protocol)

    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.account)
}
