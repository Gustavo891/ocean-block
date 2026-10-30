plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.account)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.plugins.oceanblock.system.currencies)
    compileOnly(projects.plugins.oceanblock.system.islands)
    compileOnly(projects.plugins.oceanblock.system.sell)
    compileOnly(projects.plugins.oceanblock.system.craft)
}
