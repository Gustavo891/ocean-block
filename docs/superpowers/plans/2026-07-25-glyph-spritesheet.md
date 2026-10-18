# Glyph sprite-sheet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir que uma entrada de glyph do Textura descreva uma folha vertical de N ícones num único PNG, expondo cada fatia como um glyph independente por id.

**Arquitetura:** `GlyphRegistry` passa a reconhecer entradas com `amount` e expandi-las em N `GlyphData`, cada uma com seu `unicode`, `sheetKey` e `sheetIndex`. O mapa `glyphs` continua sendo `Map<id, GlyphData>`, então todos os consumidores (`GlyphApi`, `TexturaTags`, HUD, `/textura glyphs`) seguem inalterados. No build do pack, `ResourcePackBuilder.addGlyphs` agrupa as fatias por `sheetKey` e emite **um** `FontProvider.bitMap` com a lista de `chars` ordenada — que é o mecanismo nativo do Minecraft para fatiar uma textura em linhas.

**Tech stack:** Kotlin 2.3 / JDK 21, Paper 1.21.x (`YamlConfiguration`), team.unnamed `creative` 1.7.3, Kotest 5.9 em JUnit 5.

## Global Constraints

- Spec de referência: `docs/superpowers/specs/2026-07-25-glyph-spritesheet-design.md`.
- **Não escreva comentários no código.** O código produzido neste plano não leva comentários nem KDoc novo, em nenhuma linguagem.
- Strings de log e nomes de teste em português, seguindo o resto do repositório.
- Suporte apenas a folha **vertical**: uma entrada do array `chars` por unicode. Nada de grade 2D.
- Entradas sem `amount` devem continuar carregando exatamente como hoje — nenhuma regressão no formato antigo.
- Falha de config nunca é fatal: loga `logger.warning(...)` e pula a entrada.
- Todo comando Gradle deste projeto precisa ter `local` no nome da task (ver `CLAUDE.md`), senão o build entra em modo produção e falha resolvendo `com.redescreen:sdk`. Para este módulo use a task `localTest` criada na Task 1.
- **Não commite.** O usuário faz todos os commits. Os passos "Commit" abaixo listam o comando exato para ele rodar; pare no fim de cada task e reporte o que mudou.

---

### Task 1: Tornar `GlyphWidthMeasurer` testável e ligar o classpath de teste

`GlyphWidthMeasurer.measure` chama `Bukkit.getLogger()` no caminho de sucesso e no de falha. Fora de um servidor, `Bukkit.getServer()` é `null` e a chamada estoura `NullPointerException` — qualquer teste de `GlyphRegistry` (que chama `measure` para todo glyph) morre antes de exercitar a lógica. O módulo também não tem `src/test` nem paper-api no classpath de teste.

**Files:**
- Modify: `plugins/textura/build.gradle.kts`
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/util/GlyphWidthMeasurer.kt`
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/registry/GlyphRegistry.kt:115`
- Create: `plugins/textura/src/test/kotlin/one_block/textura/util/GlyphWidthMeasurerTest.kt`

**Interfaces:**
- Consumes: nada.
- Produces: `GlyphWidthMeasurer.measure(file: File, logger: Logger): Int?` — assinatura usada pelas Tasks 2 e 3. Task `:plugins:textura:localTest`.

- [ ] **Step 1: Adicionar paper-api e a task `localTest` ao módulo**

Em `plugins/textura/build.gradle.kts`, acrescente `testImplementation(libs.paperApi)` como primeira linha do bloco de testes:

```kotlin
    testImplementation(libs.paperApi)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
```

E no fim do arquivo, depois de `tasks.test { useJUnitPlatform() }`:

```kotlin
tasks.register("localTest") {
    group = "verification"
    description = "Roda os testes do modulo em modo local."
    dependsOn(tasks.named("test"))
}
```

- [ ] **Step 2: Escrever o teste que falha**

Crie `plugins/textura/src/test/kotlin/one_block/textura/util/GlyphWidthMeasurerTest.kt`:

```kotlin
package one_block.textura.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.File
import java.util.logging.Logger
import javax.imageio.ImageIO

class GlyphWidthMeasurerTest : StringSpec({

    val logger = Logger.getLogger("GlyphWidthMeasurerTest")

    fun png(dir: File, name: String, width: Int, height: Int): File {
        val file = File(dir, name)
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", file)
        return file
    }

    "mede a largura da imagem" {
        val dir = createTempDirectory()
        GlyphWidthMeasurer.measure(png(dir, "folha.png", 16, 48), logger) shouldBe 16
    }

    "arquivo inexistente devolve null" {
        val dir = createTempDirectory()
        GlyphWidthMeasurer.measure(File(dir, "nao_existe.png"), logger) shouldBe null
    }

    "arquivo que nao e imagem devolve null" {
        val dir = createTempDirectory()
        val file = File(dir, "quebrado.png")
        file.writeText("nao sou um png")
        GlyphWidthMeasurer.measure(file, logger) shouldBe null
    }
})

private fun createTempDirectory(): File =
    java.nio.file.Files.createTempDirectory("textura-test").toFile().apply { deleteOnExit() }
```

- [ ] **Step 3: Rodar o teste para confirmar que falha**

```bash
./gradlew :plugins:textura:localTest --tests "*GlyphWidthMeasurer*"
```

Esperado: FALHA de compilação — `measure` ainda aceita só um parâmetro.

- [ ] **Step 4: Trocar `Bukkit.getLogger()` pelo logger recebido**

Substitua o corpo inteiro de `GlyphWidthMeasurer.kt` por:

```kotlin
package one_block.textura.util

import java.io.File
import java.util.logging.Logger
import javax.imageio.ImageIO

object GlyphWidthMeasurer {

    fun measure(file: File, logger: Logger): Int? {
        if (!file.exists()) {
            logger.info("[GlyphWidthMeasurer] '${file.name}' nao encontrado -> fallback")
            return null
        }
        val image = runCatching { ImageIO.read(file) }.getOrNull()
        if (image == null) {
            logger.info("[GlyphWidthMeasurer] '${file.name}' nao pode ser lida -> fallback")
            return null
        }

        val size = image.width
        logger.info("[GlyphWidthMeasurer] '${file.name}' width=$size -> size=$size")
        return size
    }
}
```

- [ ] **Step 5: Atualizar a chamada em `GlyphRegistry`**

Em `GlyphRegistry.kt`, a linha que hoje é:

```kotlin
            val size = GlyphWidthMeasurer.measure(File(glyphsFolder, caminho)) ?: GlyphData.DEFAULT_SIZE
```

passa a ser:

```kotlin
            val size = GlyphWidthMeasurer.measure(File(glyphsFolder, caminho), logger) ?: GlyphData.DEFAULT_SIZE
```

- [ ] **Step 6: Rodar o teste para confirmar que passa**

```bash
./gradlew :plugins:textura:localTest --tests "*GlyphWidthMeasurer*"
```

Esperado: PASS, 3 testes.

- [ ] **Step 7: Commit (o usuário roda)**

```bash
git add plugins/textura/build.gradle.kts plugins/textura/src/main/kotlin/one_block/textura/util/GlyphWidthMeasurer.kt plugins/textura/src/main/kotlin/one_block/textura/registry/GlyphRegistry.kt plugins/textura/src/test/kotlin/one_block/textura/util/GlyphWidthMeasurerTest.kt
git commit -m "refactor(textura): injetar logger em GlyphWidthMeasurer e wirar testes"
```

---

### Task 2: Expandir folhas com unicode explícito no `GlyphRegistry`

Uma entrada com `amount` vira N `GlyphData`, uma por id da lista `ids`, cada uma com o unicode da posição correspondente. Entradas inválidas são logadas e puladas. Esta task cobre só o caso em que a lista `unicode` já está no yml; a geração automática é a Task 3.

**Files:**
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/model/GlyphData.kt`
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/registry/GlyphRegistry.kt`
- Create: `plugins/textura/src/test/kotlin/one_block/textura/registry/GlyphRegistrySheetTest.kt`

**Interfaces:**
- Consumes: `GlyphWidthMeasurer.measure(file, logger)` da Task 1.
- Produces:
  - `GlyphData.sheetKey: String?` e `GlyphData.sheetIndex: Int` (default `null` / `0`) — usados na Task 4.
  - `GlyphRegistry.load(glyphsFolder: File)` passa a aceitar entradas com `amount`.

- [ ] **Step 1: Escrever o teste que falha**

Crie `plugins/textura/src/test/kotlin/one_block/textura/registry/GlyphRegistrySheetTest.kt`:

```kotlin
package one_block.textura.registry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.logging.Logger
import javax.imageio.ImageIO

class GlyphRegistrySheetTest : StringSpec({

    val logger = Logger.getLogger("GlyphRegistrySheetTest")

    fun pasta(yml: String): File {
        val dir = Files.createTempDirectory("textura-glyphs").toFile()
        dir.deleteOnExit()
        val png = File(dir, "mouse_clicks.png")
        ImageIO.write(BufferedImage(16, 48, BufferedImage.TYPE_INT_ARGB), "png", png)
        File(dir, "icons.yml").writeText(yml)
        return dir
    }

    "folha valida expande em um glyph por id" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              ids: [mouse_left, mouse_right, mouse_middle]
              unicode:
                - \uE200
                - \uE201
                - \uE202
              ascent: 5
              height: 8
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.keys shouldContainExactly listOf("mouse_left", "mouse_right", "mouse_middle")

        val left = registry.glyphs.getValue("mouse_left")
        left.unicode shouldBe "\\uE200"
        left.sheetKey shouldBe "mouse_clicks"
        left.sheetIndex shouldBe 0
        left.ascent shouldBe 5
        left.height shouldBe 8
        left.caminho shouldBe "mouse_clicks.png"
        left.size shouldBe 16

        registry.glyphs.getValue("mouse_middle").sheetIndex shouldBe 2
        registry.glyphs.getValue("mouse_middle").unicode shouldBe "\\uE202"
    }

    "folha sem ids e ignorada" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              unicode:
                - \uE200
                - \uE201
                - \uE202
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.shouldNotContainKey("mouse_clicks")
        registry.glyphs.size shouldBe 0
    }

    "folha com ids de tamanho diferente de amount e ignorada" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              ids: [mouse_left, mouse_right]
              unicode:
                - \uE200
                - \uE201
                - \uE202
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.size shouldBe 0
    }

    "folha com unicodes de tamanho diferente de amount e ignorada" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              ids: [mouse_left, mouse_right, mouse_middle]
              unicode:
                - \uE200
                - \uE201
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.size shouldBe 0
    }

    "amount zero e ignorado" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 0
              ids: [mouse_left]
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs.size shouldBe 0
    }

    "glyph simples continua carregando sem sheetKey" {
        val dir = pasta(
            """
            floating_islands:
              id: floating_islands
              caminho: mouse_clicks.png
              unicode: \uE103
              ascent: 8
              height: 8
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs shouldContainKey "floating_islands"
        val glyph = registry.glyphs.getValue("floating_islands")
        glyph.sheetKey shouldBe null
        glyph.sheetIndex shouldBe 0
        glyph.unicode shouldBe "\\uE103"
    }
})
```

- [ ] **Step 2: Rodar o teste para confirmar que falha**

```bash
./gradlew :plugins:textura:localTest --tests "*GlyphRegistrySheet*"
```

Esperado: FALHA de compilação — `GlyphData` não tem `sheetKey` nem `sheetIndex`.

- [ ] **Step 3: Adicionar os campos de folha ao `GlyphData`**

Em `GlyphData.kt`, acrescente dois parâmetros no fim do construtor primário, depois de `fonts`:

```kotlin
    val fonts: Map<String, GlyphFont> = emptyMap(),
    val sheetKey: String? = null,
    val sheetIndex: Int = 0
) {
```

Os KDoc existentes dos outros campos ficam como estão; não adicione documentação nova.

- [ ] **Step 4: Separar leitura de atributos e ramificar folha vs glyph simples**

Em `GlyphRegistry.kt`, substitua o método `loadYaml` inteiro (linhas 80-141) por:

```kotlin
    private fun loadYaml(file: File, glyphsFolder: File) {
        val config = YamlConfiguration.loadConfiguration(file)
        var changed = false

        for (key in config.getKeys(false)) {
            val caminho = config.getString("$key.caminho") ?: continue
            val carregou = if (config.contains("$key.amount")) {
                loadSheet(config, key, file, caminho, glyphsFolder)
            } else {
                loadSingle(config, key, file, caminho, glyphsFolder)
            }
            if (carregou) changed = true
        }

        if (changed) save(config, file)
    }

    private data class GlyphAttributes(
        val ascent: Int,
        val height: Int,
        val size: Int,
        val default: Boolean,
        val showInGui: Boolean
    )

    private fun readAttributes(
        config: YamlConfiguration,
        key: String,
        caminho: String,
        glyphsFolder: File,
        onChange: () -> Unit
    ): GlyphAttributes {
        val ascent = if (config.contains("$key.ascent")) {
            config.getInt("$key.ascent")
        } else {
            config.set("$key.ascent", GlyphData.DEFAULT_ASCENT)
            onChange()
            GlyphData.DEFAULT_ASCENT
        }

        val height = if (config.contains("$key.height")) {
            config.getInt("$key.height")
        } else {
            config.set("$key.height", GlyphData.DEFAULT_HEIGHT)
            onChange()
            GlyphData.DEFAULT_HEIGHT
        }

        val size = GlyphWidthMeasurer.measure(File(glyphsFolder, caminho), logger) ?: GlyphData.DEFAULT_SIZE

        val default = if (config.contains("$key.default")) {
            config.getBoolean("$key.default")
        } else {
            config.set("$key.default", true)
            onChange()
            true
        }

        val showInGui = if (config.contains("$key.show_in_gui")) {
            config.getBoolean("$key.show_in_gui")
        } else {
            config.set("$key.show_in_gui", true)
            onChange()
            true
        }

        return GlyphAttributes(ascent, height, size, default, showInGui)
    }

    private fun loadSingle(
        config: YamlConfiguration,
        key: String,
        file: File,
        caminho: String,
        glyphsFolder: File
    ): Boolean {
        val id = config.getString("$key.id") ?: return false
        var changed = false

        var unicode = config.getString("$key.unicode")
        if (unicode.isNullOrEmpty()) {
            unicode = nextFreeUnicode()
            config.set("$key.unicode", unicode)
            changed = true
        }
        usedUnicodes[normalize(unicode)] = describe(file, id)

        val attrs = readAttributes(config, key, caminho, glyphsFolder) { changed = true }
        val fonts = loadFonts(config, key, attrs.ascent, attrs.height, unicode) { changed = true }

        _glyphs[id] = GlyphData(
            key, id, caminho, unicode,
            attrs.ascent, attrs.height, attrs.size, attrs.default, attrs.showInGui, fonts
        )

        return changed
    }

    private fun loadSheet(
        config: YamlConfiguration,
        key: String,
        file: File,
        caminho: String,
        glyphsFolder: File
    ): Boolean {
        val amount = config.getInt("$key.amount")
        if (amount <= 0) {
            logger.warning("Folha '$key' (${file.name}) tem amount=$amount; ignorada.")
            return false
        }

        val ids = config.getStringList("$key.ids")
        if (ids.isEmpty()) {
            logger.warning("Folha '$key' (${file.name}) nao define 'ids'; ignorada.")
            return false
        }
        if (ids.size != amount) {
            logger.warning("Folha '$key' (${file.name}) tem ${ids.size} ids para amount=$amount; ignorada.")
            return false
        }

        val unicodes = config.getStringList("$key.unicode").filter { it.isNotEmpty() }
        if (unicodes.size != amount) {
            logger.warning("Folha '$key' (${file.name}) tem ${unicodes.size} unicodes para amount=$amount; ignorada.")
            return false
        }

        var changed = false
        val attrs = readAttributes(config, key, caminho, glyphsFolder) { changed = true }

        ids.forEachIndexed { index, id ->
            val unicode = unicodes[index]
            usedUnicodes[normalize(unicode)] = describe(file, id)
            val fonts = loadFonts(config, key, attrs.ascent, attrs.height, unicode) { changed = true }
            _glyphs[id] = GlyphData(
                key, id, caminho, unicode,
                attrs.ascent, attrs.height, attrs.size, attrs.default, attrs.showInGui, fonts,
                sheetKey = key,
                sheetIndex = index
            )
        }

        return changed
    }
```

O `loadFonts` existente (linhas 148-190) fica intacto.

- [ ] **Step 5: Rodar os testes para confirmar que passam**

```bash
./gradlew :plugins:textura:localTest
```

Esperado: PASS. `GlyphRegistrySheetTest` com 6 testes verdes, `GlyphWidthMeasurerTest` ainda com 3.

Nota: o teste `"folha com unicodes de tamanho diferente de amount e ignorada"` ainda passa aqui porque `unicodes.size != amount` cai no warning. O caso de lista **ausente** (`unicodes.isEmpty()`) também é rejeitado nesta task — a Task 3 é quem troca esse ramo por geração automática.

- [ ] **Step 6: Commit (o usuário roda)**

```bash
git add plugins/textura/src/main/kotlin/one_block/textura/model/GlyphData.kt plugins/textura/src/main/kotlin/one_block/textura/registry/GlyphRegistry.kt plugins/textura/src/test/kotlin/one_block/textura/registry/GlyphRegistrySheetTest.kt
git commit -m "feat(textura): expandir folha de glyphs com unicode explicito"
```

---

### Task 3: Gerar unicodes ausentes e reservar listas

Folha sem `unicode` gera `amount` unicodes livres na Private Use Area e persiste a lista no yml, igual ao glyph simples. E a primeira passada (`reserveExplicitUnicodes`) precisa enxergar listas, senão os unicodes de folha não são reservados e podem ser reemitidos como "livres" para outro glyph.

**Files:**
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/registry/GlyphRegistry.kt`
- Modify: `plugins/textura/src/test/kotlin/one_block/textura/registry/GlyphRegistrySheetTest.kt`

**Interfaces:**
- Consumes: `loadSheet` / `nextFreeUnicode` / `normalize` / `describe` da Task 2.
- Produces: nenhuma assinatura nova pública.

- [ ] **Step 1: Escrever os testes que falham**

Em `GlyphRegistrySheetTest.kt`, **remova** o teste `"folha com unicodes de tamanho diferente de amount e ignorada"` e, no lugar dele, acrescente os três testes abaixo antes do fechamento `})`. Acrescente também os imports `io.kotest.matchers.collections.shouldHaveSize` e `org.bukkit.configuration.file.YamlConfiguration` no topo do arquivo.

```kotlin
    "folha sem unicode gera um por fatia e persiste no yml" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 3
              ids: [mouse_left, mouse_right, mouse_middle]
              ascent: 5
              height: 8
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        registry.glyphs shouldHaveSize 3
        val gerados = listOf("mouse_left", "mouse_right", "mouse_middle")
            .map { registry.glyphs.getValue(it).unicode }
        gerados.toSet() shouldHaveSize 3
        gerados.all { it.startsWith("\\u") } shouldBe true

        val persistidos = YamlConfiguration
            .loadConfiguration(File(dir, "icons.yml"))
            .getStringList("mouse_clicks.unicode")
        persistidos shouldContainExactly gerados
    }

    "unicode gerado nao colide com unicode reservado de outro glyph" {
        val dir = pasta(
            """
            reservado:
              id: reservado
              caminho: mouse_clicks.png
              unicode: \uE000
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 2
              ids: [mouse_left, mouse_right]
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        val folha = listOf("mouse_left", "mouse_right").map { registry.glyphs.getValue(it).unicode }
        folha.contains("\\uE000") shouldBe false
        folha.toSet() shouldHaveSize 2
    }

    "unicode de folha reservado na primeira passada bloqueia glyph simples" {
        val dir = pasta(
            """
            mouse_clicks:
              caminho: mouse_clicks.png
              amount: 2
              ids: [mouse_left, mouse_right]
              unicode:
                - \uE000
                - \uE001
            tardio:
              id: tardio
              caminho: mouse_clicks.png
            """.trimIndent()
        )

        val registry = GlyphRegistry(logger)
        registry.load(dir)

        val tardio = registry.glyphs.getValue("tardio").unicode
        tardio shouldBe "\\uE002"
    }
```

- [ ] **Step 2: Rodar os testes para confirmar que falham**

```bash
./gradlew :plugins:textura:localTest --tests "*GlyphRegistrySheet*"
```

Esperado: FALHA. `"folha sem unicode gera um por fatia e persiste no yml"` e `"unicode gerado nao colide..."` falham porque `loadSheet` rejeita a lista vazia e o registry fica com 0 glyphs. `"unicode de folha reservado na primeira passada..."` falha porque `reserveExplicitUnicodes` não lê listas e devolve `\uE000` para `tardio`.

- [ ] **Step 3: Gerar a lista quando ausente**

Em `loadSheet`, substitua o bloco de leitura de `unicodes`:

```kotlin
        val unicodes = config.getStringList("$key.unicode").filter { it.isNotEmpty() }
        if (unicodes.size != amount) {
            logger.warning("Folha '$key' (${file.name}) tem ${unicodes.size} unicodes para amount=$amount; ignorada.")
            return false
        }

        var changed = false
```

por:

```kotlin
        var changed = false
        val declarados = config.getStringList("$key.unicode").filter { it.isNotEmpty() }
        val unicodes = if (declarados.isEmpty()) {
            val gerados = List(amount) { nextFreeUnicode() }
            config.set("$key.unicode", gerados)
            changed = true
            gerados
        } else {
            declarados
        }
        if (unicodes.size != amount) {
            logger.warning("Folha '$key' (${file.name}) tem ${unicodes.size} unicodes para amount=$amount; ignorada.")
            return false
        }
```

- [ ] **Step 4: Fazer `reserveExplicitUnicodes` entender listas**

Substitua o método `reserveExplicitUnicodes` inteiro por:

```kotlin
    private fun reserveExplicitUnicodes(file: File) {
        val config = YamlConfiguration.loadConfiguration(file)
        var changed = false

        for (key in config.getKeys(false)) {
            val declarados = readUnicodes(config, key)
            if (declarados.isEmpty()) continue

            val folha = config.contains("$key.amount")
            val ids = if (folha) {
                config.getStringList("$key.ids")
            } else {
                listOf(config.getString("$key.id") ?: key)
            }

            val resolvidos = declarados.toMutableList()
            var trocou = false

            declarados.forEachIndexed { index, unicode ->
                val id = ids.getOrNull(index) ?: key
                val norm = normalize(unicode)
                val owner = usedUnicodes[norm]

                if (owner == null) {
                    usedUnicodes[norm] = describe(file, id)
                    return@forEachIndexed
                }

                val replacement = nextFreeUnicode()
                logger.warning(
                    "Conflito de unicode '$unicode' entre ${describe(file, id)} e $owner. " +
                        "Trocando ${describe(file, id)} para '$replacement'."
                )
                resolvidos[index] = replacement
                usedUnicodes[normalize(replacement)] = describe(file, id)
                trocou = true
            }

            if (trocou) {
                config.set("$key.unicode", if (folha) resolvidos else resolvidos.first())
                changed = true
            }
        }

        if (changed) save(config, file)
    }

    private fun readUnicodes(config: YamlConfiguration, key: String): List<String> {
        val single = config.getString("$key.unicode")
        if (!single.isNullOrEmpty()) return listOf(single)
        return config.getStringList("$key.unicode").filter { it.isNotEmpty() }
    }
```

- [ ] **Step 5: Rodar os testes para confirmar que passam**

```bash
./gradlew :plugins:textura:localTest
```

Esperado: PASS, 8 testes em `GlyphRegistrySheetTest` e 3 em `GlyphWidthMeasurerTest`.

- [ ] **Step 6: Commit (o usuário roda)**

```bash
git add plugins/textura/src/main/kotlin/one_block/textura/registry/GlyphRegistry.kt plugins/textura/src/test/kotlin/one_block/textura/registry/GlyphRegistrySheetTest.kt
git commit -m "feat(textura): gerar e reservar unicodes de folha de glyphs"
```

---

### Task 4: Emitir um único provider por folha no pack

`PackBuilder` ganha `addGlyphSheet`, que emite um `FontProvider.bitMap` com a lista completa de `chars` — é essa lista de N entradas que faz o cliente fatiar a textura em N linhas. `addGlyph` passa a delegar para ele com uma lista de um elemento. `ResourcePackBuilder.addGlyphs` agrupa as fatias por `sheetKey` antes de emitir.

**Files:**
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/util/PackBuilder.kt`
- Modify: `plugins/textura/src/main/kotlin/one_block/textura/util/ResourcePackBuilder.kt:308-352`
- Create: `plugins/textura/src/test/kotlin/one_block/textura/util/PackBuilderSheetTest.kt`

**Interfaces:**
- Consumes: `GlyphData.sheetKey` / `GlyphData.sheetIndex` da Task 2.
- Produces: `PackBuilder.addGlyphSheet(textureKey: Key, texture: Writable, chars: List<String>, height: Int, ascent: Int, fontKey: Key, writeTexture: Boolean)`.

- [ ] **Step 1: Escrever o teste que falha**

Crie `plugins/textura/src/test/kotlin/one_block/textura/util/PackBuilderSheetTest.kt`:

```kotlin
package one_block.textura.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.font.Font

class PackBuilderSheetTest : StringSpec({

    val textureKey = Key.key("oneblock", "icons/mouse_clicks.png")
    val texture = Writable.stringUtf8("png-falso")

    "folha vira um provider unico com todos os chars" {
        val builder = PackBuilder()
        builder.addGlyphSheet(
            textureKey = textureKey,
            texture = texture,
            chars = listOf("a", "b", "c"),
            height = 8,
            ascent = 5
        )

        val providers = builder.pack.font(Font.MINECRAFT_DEFAULT)!!.providers()
        providers shouldHaveSize 1

        val bitmap = providers.first() as BitMapFontProvider
        bitmap.characters() shouldContainExactly listOf("a", "b", "c")
        bitmap.file() shouldBe textureKey
        bitmap.height() shouldBe 8
        bitmap.ascent() shouldBe 5
    }

    "a textura da folha entra uma vez so mesmo com duas fontes" {
        val builder = PackBuilder()
        builder.addGlyphSheet(textureKey, texture, listOf("a", "b"), 8, 5, Font.MINECRAFT_DEFAULT)
        builder.addGlyphSheet(textureKey, texture, listOf("a", "b"), 10, 3, Key.key("minecraft", "bossbar"))

        builder.pack.textures() shouldHaveSize 1
        builder.pack.font(Font.MINECRAFT_DEFAULT)!!.providers() shouldHaveSize 1
        builder.pack.font(Key.key("minecraft", "bossbar"))!!.providers() shouldHaveSize 1
    }

    "glyph simples continua sendo um provider de um char" {
        val builder = PackBuilder()
        builder.addGlyph(textureKey, texture, "z", 8, 5)

        val bitmap = builder.pack.font(Font.MINECRAFT_DEFAULT)!!.providers().first() as BitMapFontProvider
        bitmap.characters() shouldContainExactly listOf("z")
    }
})
```

- [ ] **Step 2: Rodar o teste para confirmar que falha**

```bash
./gradlew :plugins:textura:localTest --tests "*PackBuilderSheet*"
```

Esperado: FALHA de compilação — `addGlyphSheet` não existe.

- [ ] **Step 3: Adicionar `addGlyphSheet` e fazer `addGlyph` delegar**

Em `PackBuilder.kt`, substitua o método `addGlyph` inteiro (linhas 27-42, mantendo o KDoc existente acima dele) por:

```kotlin
    fun addGlyph(
        textureKey: Key,
        texture: Writable,
        char: String,
        height: Int = DEFAULT_GLYPH_HEIGHT,
        ascent: Int = DEFAULT_GLYPH_ASCENT,
        fontKey: Key = Font.MINECRAFT_DEFAULT,
        writeTexture: Boolean = true
    ) = addGlyphSheet(textureKey, texture, listOf(char), height, ascent, fontKey, writeTexture)

    fun addGlyphSheet(
        textureKey: Key,
        texture: Writable,
        chars: List<String>,
        height: Int = DEFAULT_GLYPH_HEIGHT,
        ascent: Int = DEFAULT_GLYPH_ASCENT,
        fontKey: Key = Font.MINECRAFT_DEFAULT,
        writeTexture: Boolean = true
    ) {
        if (writeTexture && pack.texture(textureKey) == null) {
            pack.texture(Texture.texture(textureKey, texture))
        }
        val provider = FontProvider.bitMap(textureKey, height, ascent, chars)
        val existing = pack.font(fontKey)?.providers().orEmpty()
        pack.font(Font.font(fontKey, existing + provider))
    }
```

- [ ] **Step 4: Rodar o teste para confirmar que passa**

```bash
./gradlew :plugins:textura:localTest --tests "*PackBuilderSheet*"
```

Esperado: PASS, 3 testes.

- [ ] **Step 5: Agrupar as fatias no `ResourcePackBuilder`**

Em `ResourcePackBuilder.kt`, substitua o método `addGlyphs` inteiro (linhas 308-352, mantendo o KDoc de uma linha acima dele) por:

```kotlin
    private fun addGlyphs(builder: PackBuilder) {
        val glyphsFolder = assets.configFolder("glyphs")
        val (fatias, simples) = glyphRegistry.glyphs.values.partition { it.sheetKey != null }

        for (glyph in simples) {
            val asset = resolveGlyphAsset(glyph.caminho, glyphsFolder) ?: continue
            val textureKey = asset.keyWithExtension
            val writable = Writable.file(asset.file)
            val writeTexture = !asset.inResource

            if (glyph.default) {
                builder.addGlyph(
                    textureKey = textureKey,
                    texture = writable,
                    char = glyph.char,
                    ascent = glyph.ascent,
                    height = glyph.height,
                    fontKey = Font.MINECRAFT_DEFAULT,
                    writeTexture = writeTexture
                )
            }

            for (font in glyph.fonts.values) {
                builder.addGlyph(
                    textureKey = textureKey,
                    texture = writable,
                    char = font.char,
                    ascent = font.ascent,
                    height = font.height,
                    fontKey = font.fontKey,
                    writeTexture = writeTexture
                )
            }
        }

        for ((_, grupo) in fatias.groupBy { it.sheetKey }) {
            val ordenadas = grupo.sortedBy { it.sheetIndex }
            val primeira = ordenadas.first()
            val asset = resolveGlyphAsset(primeira.caminho, glyphsFolder) ?: continue
            val textureKey = asset.keyWithExtension
            val writable = Writable.file(asset.file)
            val writeTexture = !asset.inResource

            if (primeira.default) {
                builder.addGlyphSheet(
                    textureKey = textureKey,
                    texture = writable,
                    chars = ordenadas.map { it.char },
                    ascent = primeira.ascent,
                    height = primeira.height,
                    fontKey = Font.MINECRAFT_DEFAULT,
                    writeTexture = writeTexture
                )
            }

            for (nome in primeira.fonts.keys) {
                val fontes = ordenadas.mapNotNull { it.fonts[nome] }
                if (fontes.size != ordenadas.size) {
                    logger.warning("Folha '${primeira.sheetKey}' nao registra a fonte '$nome' em todas as fatias; fonte ignorada.")
                    continue
                }
                val referencia = fontes.first()
                builder.addGlyphSheet(
                    textureKey = textureKey,
                    texture = writable,
                    chars = fontes.map { it.char },
                    ascent = referencia.ascent,
                    height = referencia.height,
                    fontKey = referencia.fontKey,
                    writeTexture = writeTexture
                )
            }
        }
    }

    private fun resolveGlyphAsset(caminho: String, glyphsFolder: File) = assets.resolve(
        declared = caminho,
        legacyRoot = glyphsFolder,
        legacyPackPath = "assets/minecraft/textures/$caminho"
    )
```

- [ ] **Step 6: Compilar o módulo inteiro**

```bash
./gradlew :plugins:textura:localTest
```

Esperado: compila e todos os testes passam (3 + 8 + 3).

- [ ] **Step 7: Commit (o usuário roda)**

```bash
git add plugins/textura/src/main/kotlin/one_block/textura/util/PackBuilder.kt plugins/textura/src/main/kotlin/one_block/textura/util/ResourcePackBuilder.kt plugins/textura/src/test/kotlin/one_block/textura/util/PackBuilderSheetTest.kt
git commit -m "feat(textura): emitir um provider unico por folha de glyphs"
```

---

### Task 5: Declarar `mouse_clicks` como folha e verificar em jogo

Primeiro uso real. `mouse_clicks.png` já está em `server/plugins/Textura/resource/assets/oneblock/textures/icons/` com 16x48 (três ícones de 16x16 empilhados).

**Files:**
- Modify: `server/plugins/Textura/configs/glyphs/icons.yml`

**Interfaces:**
- Consumes: todo o suporte a folha das Tasks 2-4.
- Produces: os ids de glyph `mouse_left`, `mouse_right`, `mouse_middle`.

- [ ] **Step 1: Acrescentar a entrada da folha ao `icons.yml`**

No fim de `server/plugins/Textura/configs/glyphs/icons.yml`, acrescente:

```yaml
mouse_clicks:
  caminho: assets/oneblock/textures/icons/mouse_clicks.png
  amount: 3
  ids: [mouse_left, mouse_right, mouse_middle]
  ascent: 8
  height: 16
  default: true
  show_in_gui: true
```

Sem `unicode`: o registry gera os três na Private Use Area e grava a lista de volta no arquivo no primeiro carregamento. Confira a ordem dos ids contra a imagem — de cima para baixo — e ajuste os nomes se a ordem real dos ícones for outra.

- [ ] **Step 2: Buildar e deployar**

Pare o servidor de desenvolvimento antes (o Windows trava o jar em uso).

```bash
./gradlew :plugins:textura:localDeploy
```

- [ ] **Step 3: Verificar o pack gerado**

Suba o servidor, rode `/textura reload` e confirme:

- `icons.yml` ganhou a lista `unicode` com três entradas `\uEXXX` distintas, nenhuma repetindo unicode de outro glyph do arquivo;
- `/textura glyphs` lista `mouse_left`, `mouse_right` e `mouse_middle`, cada um com um ícone **diferente** — se os três mostrarem a imagem inteira, o agrupamento por `sheetKey` não está sendo aplicado;
- `generated/resourcepack.zip`, em `assets/minecraft/font/default.json`, tem **um** provider apontando para `oneblock:icons/mouse_clicks.png` com `chars` de três entradas.

- [ ] **Step 4: Verificar o render no cliente**

Entre no servidor com o pack aplicado e mande no chat `<glyph:mouse_left><glyph:mouse_right><glyph:mouse_middle>`. Esperado: três ícones distintos lado a lado, na ordem da imagem.

- [ ] **Step 5: Commit (o usuário roda)**

```bash
git add server/plugins/Textura/configs/glyphs/icons.yml
git commit -m "feat(textura): declarar mouse_clicks como folha de glyphs"
```
