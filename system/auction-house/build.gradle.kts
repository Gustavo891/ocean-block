plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)

    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.formatter)

    compileOnly(projects.system.currencies)

    compileOnly(files(rootProject.file("jars/inventory-framework-paper-3.7.1.jar")))
}
