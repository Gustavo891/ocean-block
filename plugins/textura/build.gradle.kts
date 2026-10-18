plugins {
    id("paper-plugin")
}

dependencies {
    implementation(libs.creative.api)
    implementation(libs.creative.serializer.minecraft)
    implementation(libs.creative.server)
    implementation(libs.examination.api)
    implementation(libs.examination.string)

    implementation(files(rootProject.file("jars/inventory-framework-paper-3.7.1.jar")))
    compileOnly(libs.placeholder)

    testImplementation(libs.paperApi)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

bukkit {
    name = "Textura"
    apiVersion = "26.3"
    main = "one_block.textura.TexturaPlugin"
    author = "badoff"
    description = "Sistema dinamico de resource packs e glyphs"
    softDepend = listOf("PlaceholderAPI")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("textura")

    // O Paper ja fornece o Adventure. Shadear a copia que vem do `creative` colocava
    // uma versao antiga na frente da do paper-api para quem depende deste modulo.
    exclude("net/kyori/adventure/**")
    exclude("net/kyori/option/**")
    exclude("META-INF/services/net.kyori.**")
    relocate("net.kyori.examination", "one_block.textura.libs.examination")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("localTest") {
    group = "verification"
    description = "Roda os testes do modulo em modo local."
    dependsOn(tasks.named("test"))
}
