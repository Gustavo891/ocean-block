plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://nexus.frengor.com/repository/public/")
    maven("https://maven.devs.beer/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://maven.maxhenkel.de/repository/public")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("paperApi").get())
    //compileOnly(libs.findLibrary("sdk").get())
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    implementation(libs.findLibrary("aikar").get())
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("")
}
