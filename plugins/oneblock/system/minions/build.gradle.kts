plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)
    compileOnly(libs.mongodb)
    compileOnly(files(rootProject.file("jars/ModelEngine-4.1.0.jar")))

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.plugins.oneblock.system.islands)
    compileOnly(projects.plugins.oneblock.system.craft)
    compileOnly(projects.plugins.oneblock.system.currencies)
    compileOnly(projects.plugins.textura)

}
