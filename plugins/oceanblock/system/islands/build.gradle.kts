plugins {
    id("paper-common")
}

dependencies {
    compileOnly(libs.placeholder)
    compileOnly(libs.mongodb)

    compileOnly(projects.lib.connection)
    compileOnly(projects.lib.coordinate)
    compileOnly(projects.lib.formatter)
    compileOnly(projects.plugins.oceanblock.system.currencies)
    compileOnly(projects.lib.instance)
    compileOnly(projects.lib.itemBuilder)
    compileOnly(projects.lib.rewards)
    compileOnly(projects.lib.toast)
    compileOnly(projects.lib.translation)
    compileOnly(projects.plugins.textura)
    compileOnly(files(rootProject.file("jars/FastAsyncWorldEdit-Bukkit-2.15.1.jar")))

    compileOnly("de.rapha149.signgui:signgui:2.5.4")

    compileOnly(fileTree("$rootDir/.dep"))

    // paper-api em runtime de teste: IslandSetting referencia org.bukkit.Material.
    testImplementation(libs.paperApi)
    testImplementation(projects.lib.coordinate)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}
