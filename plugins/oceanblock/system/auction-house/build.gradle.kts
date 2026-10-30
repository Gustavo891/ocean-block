plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)

    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.account)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.formatter)

    compileOnly(projects.plugins.oceanblock.system.currencies)

}
