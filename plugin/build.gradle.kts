import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("paper-plugin")
}

val isLocal = gradle.startParameter.taskNames.any { it.contains("local", ignoreCase = true) }

dependencies {
    compileOnly(libs.placeholder)

    if (isLocal) {
        implementation(libs.mongodb)
    } else {
        compileOnly("com.redescreen:sdk:1.0")
        compileOnly(libs.mongodb)
    }


    implementation(projects.lib.connection)
    implementation(projects.lib.coordinate)
    implementation(projects.lib.formatter)
    implementation(projects.lib.instance)
    implementation(projects.lib.itemBuilder)
    implementation(projects.lib.translation)

    rootProject.subprojects.filter { it.path.startsWith(":system:") }.forEach {
        implementation(it)
    }

    implementation("de.rapha149.signgui:signgui:2.5.4")
    implementation(files(rootProject.file("jars/inventory-framework-paper-3.7.1.jar")))
}

bukkit {
    name = "one-block"
    apiVersion = "1.21"
    main = "io.github.badnotice.one_block.OneBlockPlugin"
    author = "badoff"
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    depend = listOf("PlaceholderAPI", "SkinsRestorer")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("one-block")
}

tasks.register("localJar") {
    group = "env"
    description = "Builds the plugin for local testing"
    dependsOn("shadowJar")
}

tasks.register("productionJar") {
    group = "env"
    description = "Builds the plugin for production"
    dependsOn("shadowJar")
}

