plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.mongodb)
    compileOnly(libs.placeholder)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.plugins.oneblock.system.craft)
}
