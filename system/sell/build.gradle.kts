plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)
    compileOnly(libs.mongodb)

    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.formatter)
    compileOnly(files(rootProject.file("jars/inventory-framework-paper-3.7.1.jar")))

    compileOnly(projects.system.currencies)
}
