plugins {
    id("paper-common")
}

dependencies {
    // Textura é plugin separado (jar próprio, não shadado). compileOnly dá visibilidade
    // da API em compilação; em runtime vem via `depend` no plugin.yml.
    compileOnly(projects.plugins.textura)
}
