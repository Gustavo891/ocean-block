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

    compileOnly("de.rapha149.signgui:signgui:2.5.4")

    compileOnly(fileTree("$rootDir/.dep"))
}
