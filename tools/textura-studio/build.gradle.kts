plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Usa o motor do Textura (parser, compilador, layout) offline, como os testes dele.
    implementation(projects.plugins.textura)
    implementation(libs.paperApi)

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("textura_studio.MainKt")
}

val web = layout.projectDirectory.dir("web")
val windows = System.getProperty("os.name").lowercase().contains("windows")

fun npm(vararg args: String): List<String> = if (windows) listOf("cmd", "/c", "npm", *args) else listOf("npm", *args)

val npmInstall by tasks.registering(Exec::class) {
    group = "studio"
    description = "Instala as dependências do frontend"
    workingDir = web.asFile
    commandLine(npm("install"))
    inputs.file(web.file("package.json"))
    outputs.dir(web.dir("node_modules"))
}

val webBuild by tasks.registering(Exec::class) {
    group = "studio"
    description = "Compila o frontend (web/dist)"
    dependsOn(npmInstall)
    workingDir = web.asFile
    commandLine(npm("run", "build"))
    inputs.dir(web.dir("src"))
    inputs.file(web.file("index.html"))
    inputs.file(web.file("package.json"))
    outputs.dir(web.dir("dist"))
}

tasks.named<JavaExec>("run") {
    group = "studio"
    description = "Sobe o Textura Studio em http://127.0.0.1:7070"
    dependsOn(webBuild)
    workingDir = rootProject.projectDir
    systemProperty("studio.data", rootProject.file("server/plugins/Textura").absolutePath)
    systemProperty("studio.web", web.dir("dist").asFile.absolutePath)
    System.getenv("TEXTURA_DATA")?.let { systemProperty("studio.data", it) }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("studio.data", rootProject.file("server/plugins/Textura").absolutePath)
}
