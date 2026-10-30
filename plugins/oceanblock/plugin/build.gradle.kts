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


    implementation(libs.fastboard)

    implementation(projects.lib.account)
    implementation(projects.lib.actionbar)
    implementation(projects.lib.connection)
    implementation(projects.lib.coordinate)
    implementation(projects.lib.formatter)
    implementation(projects.lib.instance)
    implementation(projects.lib.itemBuilder)
    implementation(projects.lib.rewards)
    implementation(projects.lib.scoreboard)
    implementation(projects.lib.toast)
    implementation(projects.lib.translation)

    rootProject.subprojects.filter { it.path.contains(":system:") }.forEach {
        implementation(it)
    }

    implementation("de.rapha149.signgui:signgui:2.5.4")
    implementation(files(rootProject.file("jars/inventory-framework-paper-3.7.1.jar")))
}

bukkit {
    name = "ocean-block"
    apiVersion = "26.3"
    main = "io.github.badnotice.ocean_block.OceanBlockPlugin"
    author = "badoff"
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    depend = listOf("PlaceholderAPI", "SkinsRestorer", "Textura", "FastAsyncWorldEdit")
    softDepend = listOf("packetevents", "ModelEngine")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("ocean-block")
}

