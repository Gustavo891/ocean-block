plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.plugins.oneblock.system.collections)
    compileOnly(projects.plugins.oneblock.system.skills)
}
