plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)
    compileOnly(libs.protocol)

    compileOnly(libs.mongodb)

    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)

    compileOnly(projects.system.currencies)
}
