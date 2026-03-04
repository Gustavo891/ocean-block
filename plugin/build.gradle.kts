import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("paper-plugin")
}

dependencies {
    compileOnly(libs.placeholder)
    compileOnly("com.redescreen:sdk:1.0")
    compileOnly(libs.mongodb)

    implementation(projects.lib.connection)
    implementation(projects.lib.coordinate)
    implementation(projects.lib.formatter)
    implementation(projects.lib.instance)
    implementation(projects.lib.itemBuilder)
    implementation(projects.lib.translation)

    implementation(projects.system.arena)
    implementation(projects.system.auctionHouse)
    implementation(projects.system.chats)
    implementation(projects.system.coinflip)
    implementation(projects.system.currencies)
    implementation(projects.system.islands)
    implementation(projects.system.minions)
    implementation(projects.system.playtime)
    implementation(projects.system.sell)
    implementation(projects.system.shop)
    implementation(projects.system.starterKit)
    implementation(projects.system.tops)
    implementation(projects.system.worlds)
    implementation(projects.system.specialItems)

    implementation("de.rapha149.signgui:signgui:2.5.4")
    implementation(files(rootProject.file("jars/inventory-framework-paper-3.7.1.jar")))

}

bukkit {
    name = "one-block"
    apiVersion = "1.21"
    main = "io.github.badnotice.one_block.OneBlockPlugin"
    author = "badoff"
    depend = listOf("PlaceholderAPI")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("one-block")
}
