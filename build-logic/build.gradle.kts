plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:${libs.versions.shadowJar.get()}")
    implementation("net.minecrell:plugin-yml:${libs.versions.pluginYml.get()}")
}
