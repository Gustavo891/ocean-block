import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("paper-common")
    id("net.minecrell.plugin-yml.bukkit")
}

// shadowJar is the shippable artifact; the thin jar collides on the same
// output name (classifier ""), so disable it to avoid ambiguous producers.
tasks.named<Jar>("jar") {
    enabled = false
}

// Com o jar fino desligado, o projeto ficaria sem artefato consumível: um
// `compileOnly(projects.plugins.textura)` resolvia para nada e só compilava por
// causa de um jar velho em build/libs. Publica o shadowJar no lugar dele.
listOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name) {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.named<ShadowJar>("shadowJar"))
    }
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

tasks.register<Copy>("localDeploy") {
    group = "env"
    description = "Builds the shaded jar and copies it into server/plugins/"
    doNotTrackState("server/plugins holds other plugins' runtime data (e.g. LuckPerms H2 DB)")
    dependsOn("shadowJar")
    from(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    into(rootProject.file("server/plugins"))
}
