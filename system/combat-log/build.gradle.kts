plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)
}
