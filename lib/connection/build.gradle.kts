plugins {
    id("paper-common")
}

val isLocal = gradle.startParameter.taskNames.any { it.contains("local", ignoreCase = true) }
val env = if (isLocal) "local" else "production"

sourceSets {
    main {
        java.srcDir("src/$env/kotlin")
    }
}

dependencies {
    compileOnly(libs.mongodb)
    if (env == "production") {
        compileOnly("com.redescreen:sdk:1.0")
    }
}

