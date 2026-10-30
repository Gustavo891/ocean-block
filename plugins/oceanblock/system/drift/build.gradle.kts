plugins {
    id("paper-common")
}

dependencies {
    compileOnly(projects.plugins.textura)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.account)
    compileOnly(projects.lib.itemBuilder)

    compileOnly(projects.plugins.oceanblock.system.craft)
    compileOnly(projects.plugins.oceanblock.system.customItems)
    compileOnly(projects.plugins.oceanblock.system.islands)

    testImplementation(libs.paperApi)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}
