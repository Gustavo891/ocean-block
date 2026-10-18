plugins {
    id("paper-common")
}

dependencies {
    // Sombreado pelo módulo do plugin; aqui só precisa compilar.
    compileOnly(libs.fastboard)

    compileOnly(projects.lib.instance)
}
