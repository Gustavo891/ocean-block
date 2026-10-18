plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.lib.actionbar)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
}
