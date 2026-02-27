plugins {
    kotlin("jvm") version "2.1.0"
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://mvnrepository.com/artifact/org.apache.commons/commons-math4-core")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://mvnrepository.com/artifact/org.projectlombok/lombok")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://nexus.frengor.com/repository/public/")
        maven("https://maven.devs.beer/")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://maven.maxhenkel.de/repository/public")
        maven("https://repo.tcoded.com/releases")
    }
}
