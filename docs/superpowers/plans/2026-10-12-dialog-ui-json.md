# Telas de dialog em JSON — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Abrir telas descritas em `configs/ui/<id>.json` dentro do dialog vanilla, desenhadas com glyphs de fonte, com switches/segmented/botões clicáveis cujo estado vem de quem abre a tela.

**Architecture:** Porta do engine do doc "Drawn UI inside the vanilla dialog" para o plugin `textura`, no pacote `one_block.textura.dialog`. A cadeia é: arte → kit → layout em px → linhas de 9 px → Paper `Dialog`. Um compilador traduz o JSON + tema + estado da sessão em nós do engine. O `/textura gerar` monta as fontes de UI no pack e publica um `UiKit` imutável. As surfaces (fundos 9-slice) e os ícones escalados são pré-renderizados por tamanho usado.

**Tech Stack:** Kotlin 2.3 / JDK 21, Paper API 1.21.11 (`io.papermc.paper.dialog`, `registry.data.dialog.*`), Adventure + MiniMessage, team.unnamed `creative` 1.7.3, Gson (vem com o Paper), Aikar ACF, `javax.imageio`.

**Spec:** `docs/superpowers/specs/2026-10-12-dialog-ui-json-design.md`. O doc de referência é `C:\Users\gushd\Downloads\dialog-ui.md`; as seções §N citadas abaixo são dele.

## Global Constraints

- **Sem comentários no código.** Nada de KDoc, comentário inline ou comentário em YAML/JSON gerado (CLAUDE.md).
- **Sem testes automatizados.** O projeto não quer testes salvo pedido explícito. A verificação de cada task é compilar; a do conjunto é in-game (Task 9).
- **Sem commits.** O usuário faz todos os commits e pushes. Cada task termina com o build verde, sem `git commit`.
- Strings visíveis ao jogador e mensagens de log em **português**.
- Tudo novo fica em `plugins/textura`. O Textura não depende de módulo nenhum do one-block.
- MiniMessage com as tags do Textura: `MiniMessage.miniMessage().deserialize(texto, TexturaTags.resolver)`.
- Limites de imagem: largura ≤ 399 px, altura ≤ 256 px. Ids de arte em `[a-z0-9_/-]`.
- Fontes de UI: `oneblock:ui/image_0..8` (ascent `7 - k`), `oneblock:ui/text_0..8` (ascent do provider `- k`), `oneblock:ui/space`.
- Arte de UI: `server/plugins/Textura/resource/assets/oneblock/textures/ui/<path>.png` vira o id `ui:<path>`. O glyph existente `<id>` vira o id `glyph:<id>`.
- Comando de build: na raiz `D:\one-block`, rodar `./gradlew :plugins:textura:localJar`. Ele precisa ter "local" no nome; veja "isLocal" no CLAUDE.md.
- Deploy: `./gradlew :plugins:textura:localDeploy` com o servidor **parado**, depois reiniciar o servidor.

## Review Focus

Não há testes automatizados para fixar estes casos. Cada linha vira um item do checklist in-game da Task 9 (passo 9.6), conferido manualmente:

1. **JSON editado sem `/textura gerar` (surface com tamanho novo).** A tela abre, o fundo novo não aparece e o console mostra `Surface '…' WxH não pré-gerada — rode /textura gerar`. Nenhuma exceção.
2. **Clique numa tela antiga** (depois de reabrir, ou de fechar e abrir outra). O clique é ignorado, porque o token não é mais o atual.
3. **Texto maior que o espaço disponível.** É cortado com `...` na mesma linha. Linhas nunca quebram nem deslocam.
4. **Jogador sem o pack carregado.** Recebe o dialog `Textura necessária` com o botão `Aplicar textura`, e não uma tela quebrada.
5. **Tema, ícone ou surface inexistente.** O console avisa uma vez com o caminho do nó (`Social > Heading > …`), a caixa fica vazia e o resto da tela desenha normalmente.

---

## Mapa de arquivos

Os caminhos são relativos a `plugins/textura/src/main/kotlin/one_block/textura/`.

| Arquivo | Responsabilidade |
|---|---|
| `dialog/kit/UiKit.kt` | contrato do kit (`UiKit`, `UiImage`, `UiSlice`, `UiPiece`, `SurfaceSize`, `IconSize`) + `UiFonts` (chaves e chars de espaço) |
| `dialog/kit/UiArtCatalog.kt` | resolve `ui:`/`glyph:` → PNG + `.slice`, valida |
| `dialog/kit/GlyphAdvance.kt` | advance de bitmap igual ao do cliente |
| `dialog/kit/IconScale.kt` | redimensiona ícone para `size` px de altura |
| `dialog/kit/UiTextFont.kt` | providers de texto + larguras medidas de `custom.json` |
| `dialog/kit/BaseUiKit.kt` | kit sem chars, usado para medir antes do pack existir |
| `dialog/kit/UiCodepoints.kt` | codepoints estáveis no plano 15 (doc §4.6) |
| `dialog/kit/UiPieces.kt` | `UiPieceSource`, padding, fatias horizontais, `SurfaceComposer` |
| `dialog/kit/PackedUiKit.kt` | kit final publicado após o `gerar` |
| `dialog/kit/UiUsage.kt` | roda o layout de todas as telas e coleta imagens, ícones e surfaces usados |
| `dialog/node/UiNode.kt` | árvore de nós, tamanhos, `UiAction` |
| `dialog/layout/Placed.kt` | `PlacedImage`/`PlacedText`/`PlacedHotspot`/`LaidOut`/`ImageVariant` |
| `dialog/layout/UiLayout.kt` | nós → caixas absolutas (doc §6) |
| `dialog/render/TextMeasurer.kt` | largura de texto, corte com `...`, `Spaces` |
| `dialog/render/UiRenderer.kt` | caixas → linhas de dialog (doc §8) |
| `dialog/theme/UiTheme.kt` | modelo do tema + `UiColors` |
| `dialog/json/ScreenDefinition.kt` | árvore imutável lida do JSON |
| `dialog/json/ScreenParser.kt` | JSON → `ScreenDefinition`, com avisos |
| `dialog/json/ScreenCompiler.kt` | definição + tema + sessão → `UiScreen` |
| `dialog/DialogSession.kt` | estado da tela aberta (binds + valores locais) |
| `dialog/DialogTokens.kt` | tokens anti-clique-velho (doc §9.4) |
| `dialog/DialogOpener.kt` | abre o Dialog, callbacks, fallback |
| `api/DialogBinds.kt` | builder público de binds |
| `api/DialogApi.kt` | ponto de entrada público |
| `registry/UiThemeRegistry.kt` | carrega `configs/ui/*.yml` |
| `registry/UiScreenRegistry.kt` | carrega `configs/ui/*.json` |
| `listener/PackStatusTracker.kt` | quem carregou o pack |
| `pack/UiPackWriter.kt` | escreve texturas e fontes de UI, devolve o kit |
| `pack/ResourcePackBuilder.kt` (modificar) | chama o `UiPackWriter`, ignora `*.slice`, devolve `BuiltPack` |
| `TexturaPlugin.kt` (modificar) | registries, kit, opener, listener, completion |
| `command/TexturaCommand.kt` (modificar) | `/textura dialog <id> [jogador]` |

---

### Task 1: Spike — confirmar o comportamento do cliente 1.21.11

Código **descartável**: é apagado no fim desta task. Usa só a fonte `minecraft:spacing` que já existe (`TextMetrics.getSpacingComponent`).

**Files:**
- Modify (temporário): `command/TexturaCommand.kt`
- Modify: `docs/superpowers/specs/2026-10-12-dialog-ui-json-design.md` (registrar o resultado)

**Interfaces:** nenhuma.

- [ ] **Step 1: Adicionar o subcomando temporário**

Em `TexturaCommand`, adicione os imports `io.papermc.paper.dialog.Dialog`, `io.papermc.paper.registry.data.dialog.DialogBase`, `io.papermc.paper.registry.data.dialog.body.DialogBody`, `io.papermc.paper.registry.data.dialog.type.DialogType`, `net.kyori.adventure.text.JoinConfiguration` e `one_block.textura.text.TextMetrics`, e este método:

```kotlin
    @Subcommand("uispike")
    fun uiSpike(player: Player) {
        fun bar(shift: Int) = Component.empty().append(TextMetrics.getSpacingComponent(shift)).append(Component.text("|"))
        val hover = HoverEvent.showText(Component.text("span"))
        val lines = listOf(
            bar(398),
            bar(399),
            Component.text("|"),
            Component.empty().append(TextMetrics.getSpacingComponent(100).hoverEvent(hover)),
            Component.empty().append(TextMetrics.getSpacingComponent(100).hoverEvent(hover)).append(Component.text("|")),
            Component.text("||||||||||"),
            Component.text("||||||||||"),
        )
        val body = DialogBody.plainMessage(Component.join(JoinConfiguration.newlines(), lines), 408)
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(DialogBase.builder(Component.text("spike")).body(listOf(body)).build())
                .type(DialogType.notice())
        }
        player.showDialog(dialog)
    }
```

- [ ] **Step 2: Build e deploy**

Com o servidor parado, rode `./gradlew :plugins:textura:localDeploy`. Esperado: `BUILD SUCCESSFUL`. Depois inicie o servidor.

- [ ] **Step 3: Pedir ao usuário que rode `/textura uispike` com o cliente 1.21.11 e responda**

O cliente roda só do lado do usuário. Pergunte e anote cada resposta:
1. Linha 1 (`398 + |` = 400 px): o `|` fica na borda direita, na mesma linha? (esperado: sim)
2. Linha 2 (`399 + |` = 401 px): o `|` cai para uma linha extra? (esperado: sim, quebra)
3. Linha 3 (`|` sozinho): aparece centralizado? (esperado: sim, centralização por linha)
4. Linha 4: o mouse sobre os primeiros 100 px mostra o tooltip "span"? (esperado: **não**, porque não há glyph desenhado na linha)
5. Linha 5: o mesmo mouse mostra o tooltip? (esperado: **sim**)
6. Linhas 6 e 7: a distância vertical entre elas é igual à de texto normal (9 px)? (esperado: sim)
7. Existe um contorno branco de 1 px em volta do corpo? (decide se a Task 10 é necessária)

- [ ] **Step 4: Decidir**

Se 1–6 batem com o esperado, as constantes do doc valem (`LINE = 9`, largura 400 + 8, spans primeiro, `#hit`). Se algo divergir, **pare** e leve o resultado ao usuário antes da Task 2.

- [ ] **Step 5: Registrar na spec**

Acrescente ao fim de `docs/superpowers/specs/2026-10-12-dialog-ui-json-design.md`:

```markdown
## Resultado do spike (cliente 1.21.11)

| Comportamento | Observado |
|---|---|
| 400 px na mesma linha / 401 px quebra | <resposta 1 e 2> |
| Centralização por linha | <resposta 3> |
| Hit-test só com glyph desenhado | <resposta 4 e 5> |
| Linha de 9 px | <resposta 6> |
| Contorno branco no corpo | <resposta 7> |
```

- [ ] **Step 6: Apagar o código do spike**

Remova o método `uiSpike` e os imports que ele trouxe. Rode `./gradlew :plugins:textura:localJar`. Esperado: `BUILD SUCCESSFUL`.

---

### Task 2: Kit — contrato, catálogo de arte e fonte de texto

**Files:**
- Create: `dialog/kit/UiKit.kt`, `dialog/kit/GlyphAdvance.kt`, `dialog/kit/IconScale.kt`, `dialog/kit/UiArtCatalog.kt`, `dialog/kit/UiTextFont.kt`, `dialog/kit/BaseUiKit.kt`

**Interfaces:**
- Produces:
  - `interface UiKit { fun has(id: String): Boolean; fun image(id: String): UiImage?; fun surface(size: SurfaceSize): UiImage?; fun icon(size: IconSize): UiImage?; fun imageFont(offset: Int): Key; fun textFont(offset: Int): Key; val spaceFont: Key; fun spaceChar(amount: Int): String?; fun charAdvance(codePoint: Int): Int; val hit: UiPiece? }`
  - `data class UiPiece(char, advance)`, `UiSlice(left, right, leftCap: UiPiece?, rightCap: UiPiece?, middles: Map<Int, UiPiece>)`, `UiImage(char, advance, width, height, slice: UiSlice? = null)`, `SurfaceSize(id, width, height)`, `IconSize(id, size)`
  - `object UiFonts { VARIANTS = 9; BASE_ASCENT = 7; SPACE: Key; image(offset): Key; text(offset): Key; spaceChars(): Map<Int, String> }`
  - `object GlyphAdvance { fun of(image: BufferedImage): Int }`
  - `object IconScale { fun width(width: Int, height: Int, size: Int): Int; fun resize(image: BufferedImage, size: Int): BufferedImage }`
  - `data class ArtSlice(left, right, top, bottom, nine: Boolean)`, `data class UiArt(id, image: BufferedImage, slice: ArtSlice?)`
  - `class UiArtCatalog(uiRoot: File, glyphFile: (String) -> File?, logger: Logger) { fun art(id: String): UiArt?; companion: UI_PREFIX = "ui:", GLYPH_PREFIX = "glyph:", MAX_WIDTH = 399, MAX_HEIGHT = 256 }`
  - `data class TextProvider(file: Key, height: Int, ascent: Int, chars: List<String>)`, `class UiTextFont(providers: List<TextProvider>, widths: Map<Int, Int>) { fun advance(codePoint: Int): Int; companion: DEFAULT_ADVANCE = 6, SPACE_ADVANCE = 4 }`, `object UiTextFontLoader { fun load(json: File, resourceRoot: File, logger: Logger): UiTextFont }`
  - `class BaseUiKit(catalog: UiArtCatalog, textFont: UiTextFont) : UiKit`

- [ ] **Step 1: `dialog/kit/UiKit.kt`**

```kotlin
package one_block.textura.dialog.kit

import net.kyori.adventure.key.Key

data class UiPiece(val char: String, val advance: Int)

data class UiSlice(
    val left: Int,
    val right: Int,
    val leftCap: UiPiece?,
    val rightCap: UiPiece?,
    val middles: Map<Int, UiPiece>,
)

data class UiImage(val char: String, val advance: Int, val width: Int, val height: Int, val slice: UiSlice? = null)

data class SurfaceSize(val id: String, val width: Int, val height: Int)

data class IconSize(val id: String, val size: Int)

interface UiKit {
    fun has(id: String): Boolean
    fun image(id: String): UiImage?
    fun surface(size: SurfaceSize): UiImage?
    fun icon(size: IconSize): UiImage?
    fun imageFont(offset: Int): Key
    fun textFont(offset: Int): Key
    val spaceFont: Key
    fun spaceChar(amount: Int): String?
    fun charAdvance(codePoint: Int): Int
    val hit: UiPiece?
}

object UiFonts {

    const val VARIANTS = 9
    const val BASE_ASCENT = 7
    private const val NAMESPACE = "oneblock"
    private val SPACE_STEPS = (0..8).map { 1 shl it }

    val SPACE: Key = Key.key(NAMESPACE, "ui/space")

    fun image(offset: Int): Key = Key.key(NAMESPACE, "ui/image_$offset")

    fun text(offset: Int): Key = Key.key(NAMESPACE, "ui/text_$offset")

    fun spaceChars(): Map<Int, String> = buildMap {
        SPACE_STEPS.forEachIndexed { index, step ->
            put(step, String(Character.toChars(0xE000 + index)))
            put(-step, String(Character.toChars(0xE000 + SPACE_STEPS.size + index)))
        }
    }
}
```

- [ ] **Step 2: `dialog/kit/GlyphAdvance.kt` e `dialog/kit/IconScale.kt`**

```kotlin
package one_block.textura.dialog.kit

import java.awt.image.BufferedImage

object GlyphAdvance {

    fun of(image: BufferedImage): Int {
        for (x in image.width - 1 downTo 0) {
            for (y in 0 until image.height) {
                if ((image.getRGB(x, y) ushr 24) != 0) return x + 2
            }
        }
        return 1
    }
}
```

```kotlin
package one_block.textura.dialog.kit

import java.awt.RenderingHints
import java.awt.image.BufferedImage

object IconScale {

    fun width(width: Int, height: Int, size: Int): Int =
        maxOf(1, Math.round(width * size / height.coerceAtLeast(1).toDouble()).toInt())

    fun resize(image: BufferedImage, size: Int): BufferedImage {
        val width = width(image.width, image.height, size)
        val result = BufferedImage(width, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        graphics.drawImage(image, 0, 0, width, size, null)
        graphics.dispose()
        return result
    }
}
```

- [ ] **Step 3: `dialog/kit/UiArtCatalog.kt`**

```kotlin
package one_block.textura.dialog.kit

import java.awt.image.BufferedImage
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.imageio.ImageIO

data class ArtSlice(val left: Int, val right: Int, val top: Int, val bottom: Int, val nine: Boolean)

data class UiArt(val id: String, val image: BufferedImage, val slice: ArtSlice?)

class UiArtCatalog(
    private val uiRoot: File,
    private val glyphFile: (String) -> File?,
    private val logger: Logger,
) {

    private val cache = ConcurrentHashMap<String, Optional<UiArt>>()

    fun art(id: String): UiArt? = cache.computeIfAbsent(id) { Optional.ofNullable(load(it)) }.orElse(null)

    private fun load(id: String): UiArt? {
        val file = when {
            id.startsWith(UI_PREFIX) -> uiFile(id.removePrefix(UI_PREFIX))
            id.startsWith(GLYPH_PREFIX) -> glyphFile(id.removePrefix(GLYPH_PREFIX))
            else -> null
        } ?: return null

        val image = runCatching { ImageIO.read(file) }.getOrNull() ?: run {
            logger.warning("[ui] Imagem '$id' ilegível: ${file.path}")
            return null
        }
        if (image.width > MAX_WIDTH || image.height > MAX_HEIGHT) {
            logger.warning("[ui] Imagem '$id' tem ${image.width}x${image.height}; o máximo é ${MAX_WIDTH}x$MAX_HEIGHT")
            return null
        }
        return UiArt(id, image, readSlice(id, file, image))
    }

    private fun uiFile(path: String): File? {
        if (!NAME.matches(path)) return null
        return File(uiRoot, "$path.png").takeIf { it.isFile }
    }

    private fun readSlice(id: String, file: File, image: BufferedImage): ArtSlice? {
        val sliceFile = File(file.parentFile, file.name + ".slice")
        if (!sliceFile.isFile) return null

        val parts = sliceFile.readText().trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
        val slice = when (parts.size) {
            2 -> ArtSlice(parts[0], parts[1], 0, 0, nine = false)
            4 -> ArtSlice(parts[0], parts[1], parts[2], parts[3], nine = true)
            else -> null
        }
        val valid = slice != null && parts.all { it >= 0 } &&
            slice.left + slice.right < image.width &&
            (!slice.nine || slice.top + slice.bottom < image.height)
        if (!valid) {
            logger.warning("[ui] '${sliceFile.name}' de '$id': esperado 'esquerda direita' ou 'esquerda direita topo base' com tampas menores que ${image.width}x${image.height}")
            return null
        }
        return slice
    }

    companion object {
        const val UI_PREFIX = "ui:"
        const val GLYPH_PREFIX = "glyph:"
        const val MAX_WIDTH = 399
        const val MAX_HEIGHT = 256
        private val NAME = Regex("[a-z0-9_/-]+")
    }
}
```

- [ ] **Step 4: `dialog/kit/UiTextFont.kt`**

```kotlin
package one_block.textura.dialog.kit

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.kyori.adventure.key.Key
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileReader
import java.util.logging.Logger
import javax.imageio.ImageIO

data class TextProvider(val file: Key, val height: Int, val ascent: Int, val chars: List<String>)

class UiTextFont(val providers: List<TextProvider>, private val widths: Map<Int, Int>) {

    fun advance(codePoint: Int): Int = widths[codePoint] ?: DEFAULT_ADVANCE

    companion object {
        const val DEFAULT_ADVANCE = 6
        const val SPACE_ADVANCE = 4
    }
}

object UiTextFontLoader {

    private const val DEFAULT_HEIGHT = 8
    private const val DEFAULT_ASCENT = 7

    fun load(json: File, resourceRoot: File, logger: Logger): UiTextFont {
        val widths = mutableMapOf(' '.code to UiTextFont.SPACE_ADVANCE)
        if (!json.isFile) {
            logger.warning("[ui] Fonte de texto '${json.path}' não encontrada; larguras padrão de ${UiTextFont.DEFAULT_ADVANCE} px")
            return UiTextFont(emptyList(), widths)
        }

        val root = FileReader(json, Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
        val providers = mutableListOf<TextProvider>()

        root.getAsJsonArray("providers")?.forEach { element ->
            val provider = element.asJsonObject
            when (provider.get("type")?.asString) {
                "space" -> provider.getAsJsonObject("advances")?.entrySet()?.forEach { (char, advance) ->
                    char.codePoints().findFirst().ifPresent { widths.putIfAbsent(it, advance.asInt) }
                }
                "bitmap" -> bitmap(provider, resourceRoot, logger)?.let { (textProvider, measured) ->
                    providers += textProvider
                    measured.forEach { (codePoint, advance) -> widths.putIfAbsent(codePoint, advance) }
                }
            }
        }
        return UiTextFont(providers, widths)
    }

    private fun bitmap(provider: JsonObject, resourceRoot: File, logger: Logger): Pair<TextProvider, Map<Int, Int>>? {
        val file = provider.get("file")?.asString ?: return null
        val chars = provider.getAsJsonArray("chars")?.map { it.asString } ?: return null
        val key = Key.key(file)
        val height = provider.get("height")?.asInt ?: DEFAULT_HEIGHT
        val textProvider = TextProvider(key, height, provider.get("ascent")?.asInt ?: DEFAULT_ASCENT, chars)

        val texture = File(resourceRoot, "assets/${key.namespace()}/textures/${key.value()}")
        val image = if (texture.isFile) runCatching { ImageIO.read(texture) }.getOrNull() else null
        if (image == null) {
            logger.warning("[ui] Textura '$key' não está em resource/; larguras desse provider usam ${UiTextFont.DEFAULT_ADVANCE} px")
            return textProvider to emptyMap()
        }

        val rows = chars.map { it.codePoints().toArray() }
        val columns = rows.maxOfOrNull { it.size } ?: 0
        if (columns == 0) return textProvider to emptyMap()

        val cellWidth = image.width / columns
        val cellHeight = image.height / rows.size
        val scale = height.toFloat() / cellHeight
        val widths = mutableMapOf<Int, Int>()

        rows.forEachIndexed { row, codePoints ->
            codePoints.forEachIndexed { column, codePoint ->
                if (codePoint != 0 && codePoint != ' '.code) {
                    val actual = actualWidth(image, column * cellWidth, row * cellHeight, cellWidth, cellHeight)
                    widths.putIfAbsent(codePoint, (0.5f + actual * scale).toInt() + 1)
                }
            }
        }
        return textProvider to widths
    }

    private fun actualWidth(image: BufferedImage, x0: Int, y0: Int, width: Int, height: Int): Int {
        for (x in width - 1 downTo 0) {
            for (y in 0 until height) {
                if ((image.getRGB(x0 + x, y0 + y) ushr 24) != 0) return x + 1
            }
        }
        return 0
    }
}
```

- [ ] **Step 5: `dialog/kit/BaseUiKit.kt`**

```kotlin
package one_block.textura.dialog.kit

import net.kyori.adventure.key.Key

class BaseUiKit(private val catalog: UiArtCatalog, private val textFont: UiTextFont) : UiKit {

    override fun has(id: String): Boolean = catalog.art(id) != null

    override fun image(id: String): UiImage? = catalog.art(id)?.let { art ->
        val slice = art.slice?.takeUnless { it.nine }?.let { UiSlice(it.left, it.right, null, null, emptyMap()) }
        UiImage("", GlyphAdvance.of(art.image), art.image.width, art.image.height, slice)
    }

    override fun surface(size: SurfaceSize): UiImage? = null

    override fun icon(size: IconSize): UiImage? = null

    override fun imageFont(offset: Int): Key = UiFonts.image(offset)

    override fun textFont(offset: Int): Key = UiFonts.text(offset)

    override val spaceFont: Key = UiFonts.SPACE

    override fun spaceChar(amount: Int): String? = null

    override fun charAdvance(codePoint: Int): Int = textFont.advance(codePoint)

    override val hit: UiPiece? = null
}
```

- [ ] **Step 6: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 3: Nós, medição de texto e layout

**Files:**
- Create: `dialog/node/UiNode.kt`, `dialog/render/TextMeasurer.kt`, `dialog/layout/Placed.kt`, `dialog/layout/UiLayout.kt`

**Interfaces:**
- Consumes: `UiKit`, `IconSize`, `IconScale` (Task 2)
- Produces:
  - `sealed interface UiSize { Px(value: Int); Grow; Fit }`, `enum UiAlign { Start, Center, End }`, `enum UiTextAlign { Left, Center, Right }`, `enum ContainerKind { Row, Column, Box, Grid }`, `data class UiPosition(x, y)`
  - `class UiAction(val reopen: Boolean, val run: (Player) -> Unit)`
  - `sealed interface UiNode { w; h; position }`; `ImageNode(id, w = Fit, position = null)`, `IconNode(id, size, position = null)`, `TextNode(text, color: TextColor? = null, bold = false, align = Left, w = Fit, position = null)` (`TextNode.LINE_HEIGHT = 9`), `HotspotNode(action: UiAction?, tooltip: Component?, w = Grow, h = Grow, position = null)`, `ContainerNode(kind, children, gap = 0, pad = 0, align = Start, cross = Start, columns = 1, snap = 0, surface: String? = null, w = Fit, h = Fit, position = null)` + `ContainerNode.space(px)`, `class UiScreen(width, root: ContainerNode)`
  - `class TextMeasurer(kit) { fun width(text, bold): Int; fun fit(text, bold, max): String }`, `class Spaces(kit) { fun of(amount: Int): String }`
  - `sealed interface ImageVariant { Plain; Icon(size); Surface }`, `PlacedImage(id, x, y, width, height, variant)`, `PlacedText(text, color, bold, x, y, maxWidth)`, `PlacedHotspot(x, y, w, h, action: UiAction?, tooltip: Component?)`, `LaidOut(width, height, images, texts, hotspots)`
  - `class UiLayout(kit) { fun layout(screen: UiScreen): LaidOut; fun intrinsicWidth(node: UiNode): Int }`

- [ ] **Step 1: `dialog/node/UiNode.kt`**

```kotlin
package one_block.textura.dialog.node

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

sealed interface UiSize {
    data class Px(val value: Int) : UiSize
    data object Grow : UiSize
    data object Fit : UiSize
}

enum class UiAlign { Start, Center, End }

enum class UiTextAlign { Left, Center, Right }

enum class ContainerKind { Row, Column, Box, Grid }

data class UiPosition(val x: Int, val y: Int)

class UiAction(val reopen: Boolean, val run: (Player) -> Unit)

sealed interface UiNode {
    val w: UiSize
    val h: UiSize
    val position: UiPosition?
}

data class ImageNode(
    val id: String,
    override val w: UiSize = UiSize.Fit,
    override val position: UiPosition? = null,
) : UiNode {
    override val h: UiSize get() = UiSize.Fit
}

data class IconNode(val id: String, val size: Int, override val position: UiPosition? = null) : UiNode {
    override val w: UiSize get() = UiSize.Px(size)
    override val h: UiSize get() = UiSize.Px(size)
}

data class TextNode(
    val text: String,
    val color: TextColor? = null,
    val bold: Boolean = false,
    val align: UiTextAlign = UiTextAlign.Left,
    override val w: UiSize = UiSize.Fit,
    override val position: UiPosition? = null,
) : UiNode {
    override val h: UiSize get() = UiSize.Px(LINE_HEIGHT)

    companion object {
        const val LINE_HEIGHT = 9
    }
}

data class HotspotNode(
    val action: UiAction?,
    val tooltip: Component?,
    override val w: UiSize = UiSize.Grow,
    override val h: UiSize = UiSize.Grow,
    override val position: UiPosition? = null,
) : UiNode

data class ContainerNode(
    val kind: ContainerKind,
    val children: List<UiNode>,
    val gap: Int = 0,
    val pad: Int = 0,
    val align: UiAlign = UiAlign.Start,
    val cross: UiAlign = UiAlign.Start,
    val columns: Int = 1,
    val snap: Int = 0,
    val surface: String? = null,
    override val w: UiSize = UiSize.Fit,
    override val h: UiSize = UiSize.Fit,
    override val position: UiPosition? = null,
) : UiNode {

    companion object {
        fun space(px: Int): ContainerNode =
            ContainerNode(ContainerKind.Box, emptyList(), w = UiSize.Px(px), h = UiSize.Px(px))
    }
}

class UiScreen(val width: Int, val root: ContainerNode)
```

- [ ] **Step 2: `dialog/render/TextMeasurer.kt`**

```kotlin
package one_block.textura.dialog.render

import one_block.textura.dialog.kit.UiKit

class TextMeasurer(private val kit: UiKit) {

    fun width(text: String, bold: Boolean): Int =
        text.codePoints().toArray().sumOf { advance(it, bold) }

    fun fit(text: String, bold: Boolean, max: Int): String {
        if (width(text, bold) <= max) return text

        val ellipsis = width(ELLIPSIS, bold)
        if (ellipsis > max) return ""

        val codePoints = text.codePoints().toArray()
        var used = ellipsis
        var count = 0
        while (count < codePoints.size) {
            val next = advance(codePoints[count], bold)
            if (used + next > max) break
            used += next
            count++
        }
        return String(codePoints, 0, count).trimEnd() + ELLIPSIS
    }

    private fun advance(codePoint: Int, bold: Boolean): Int = kit.charAdvance(codePoint) + if (bold) 1 else 0

    companion object {
        const val ELLIPSIS = "..."
    }
}

class Spaces(private val kit: UiKit) {

    private val steps = (8 downTo 0).map { 1 shl it }

    fun of(amount: Int): String {
        if (amount == 0) return ""

        val sign = if (amount > 0) 1 else -1
        var left = amount * sign
        val out = StringBuilder()

        while (left > 0) {
            val step = steps.first { it <= left }
            out.append(checkNotNull(kit.spaceChar(step * sign)) { "Espaço ${step * sign} ausente no kit" })
            left -= step
        }
        return out.toString()
    }
}
```

- [ ] **Step 3: `dialog/layout/Placed.kt`**

```kotlin
package one_block.textura.dialog.layout

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import one_block.textura.dialog.node.UiAction

sealed interface ImageVariant {
    data object Plain : ImageVariant
    data class Icon(val size: Int) : ImageVariant
    data object Surface : ImageVariant
}

data class PlacedImage(val id: String, val x: Int, val y: Int, val width: Int, val height: Int, val variant: ImageVariant)

data class PlacedText(val text: String, val color: TextColor?, val bold: Boolean, val x: Int, val y: Int, val maxWidth: Int)

data class PlacedHotspot(val x: Int, val y: Int, val w: Int, val h: Int, val action: UiAction?, val tooltip: Component?)

data class LaidOut(
    val width: Int,
    val height: Int,
    val images: List<PlacedImage>,
    val texts: List<PlacedText>,
    val hotspots: List<PlacedHotspot>,
)
```

- [ ] **Step 4: `dialog/layout/UiLayout.kt`**

Porta de doc §6. Há três diferenças. Primeira: a surface é inserida na posição da lista em que o container começou, para pintar antes dos filhos. Segunda: o ícone é centralizado na caixa `size`×`size`. Terceira: um hotspot `Grow`/`Fit` medido sem pai tem altura 0, para não inflar o box.

```kotlin
package one_block.textura.dialog.layout

import one_block.textura.dialog.kit.IconScale
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.ContainerNode
import one_block.textura.dialog.node.HotspotNode
import one_block.textura.dialog.node.IconNode
import one_block.textura.dialog.node.ImageNode
import one_block.textura.dialog.node.TextNode
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiNode
import one_block.textura.dialog.node.UiScreen
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.node.UiTextAlign
import one_block.textura.dialog.render.TextMeasurer

class UiLayout(private val kit: UiKit) {

    private val measurer = TextMeasurer(kit)

    private class Sink {
        val images = mutableListOf<PlacedImage>()
        val texts = mutableListOf<PlacedText>()
        val hotspots = mutableListOf<PlacedHotspot>()
    }

    private data class Area(val x: Int, val y: Int, val width: Int, val height: Int?)

    fun layout(screen: UiScreen): LaidOut {
        val sink = Sink()
        val height = place(screen.root, 0, 0, screen.width, null, sink)
        return LaidOut(screen.width, height, sink.images.toList(), sink.texts.toList(), sink.hotspots.toList())
    }

    fun intrinsicWidth(node: UiNode): Int {
        (node.w as? UiSize.Px)?.let { return it.value }

        return when (node) {
            is ImageNode -> kit.image(node.id)?.let { image -> image.slice?.let { it.left + it.right } ?: image.width } ?: 0
            is IconNode -> node.size
            is TextNode -> measurer.width(node.text, node.bold)
            is HotspotNode -> 0
            is ContainerNode -> {
                val flow = node.children.filter { it.position == null }.map(::intrinsicWidth)
                node.pad * 2 + when (node.kind) {
                    ContainerKind.Row -> flow.sum() + node.gap * (flow.size - 1).coerceAtLeast(0)
                    ContainerKind.Column, ContainerKind.Box -> flow.maxOrNull() ?: 0
                    ContainerKind.Grid -> (flow.maxOrNull() ?: 0) * node.columns + node.gap * (node.columns - 1)
                }
            }
        }
    }

    private fun widthIn(child: UiNode, available: Int): Int = when (val w = child.w) {
        is UiSize.Px -> w.value
        UiSize.Grow -> available
        UiSize.Fit -> intrinsicWidth(child).coerceAtMost(available)
    }

    private fun measureHeight(node: UiNode, y: Int, width: Int, parentHeight: Int?): Int =
        place(node, 0, y, width, parentHeight, Sink())

    private fun place(node: UiNode, x: Int, y: Int, width: Int, parentHeight: Int?, sink: Sink): Int = when (node) {
        is ImageNode -> placeImage(node, x, y, width, sink)
        is IconNode -> placeIcon(node, x, y, sink)
        is TextNode -> placeText(node, x, y, width, sink)
        is HotspotNode -> placeHotspot(node, x, y, width, parentHeight, sink)
        is ContainerNode -> placeContainer(node, x, y, width, parentHeight, sink)
    }

    private fun placeImage(node: ImageNode, x: Int, y: Int, width: Int, sink: Sink): Int {
        val height = kit.image(node.id)?.height ?: 0
        sink.images += PlacedImage(node.id, x, y, width, height, ImageVariant.Plain)
        return height
    }

    private fun placeIcon(node: IconNode, x: Int, y: Int, sink: Sink): Int {
        val drawn = kit.icon(IconSize(node.id, node.size))?.width
            ?: kit.image(node.id)?.let { IconScale.width(it.width, it.height, node.size) }
            ?: node.size
        val offset = ((node.size - drawn) / 2).coerceAtLeast(0)
        sink.images += PlacedImage(node.id, x + offset, y, drawn, node.size, ImageVariant.Icon(node.size))
        return node.size
    }

    private fun placeText(node: TextNode, x: Int, y: Int, width: Int, sink: Sink): Int {
        val textWidth = measurer.width(node.text, node.bold)
        val offset = when (node.align) {
            UiTextAlign.Left -> 0
            UiTextAlign.Center -> ((width - textWidth + 1) / 2).coerceAtLeast(0)
            UiTextAlign.Right -> (width - textWidth).coerceAtLeast(0)
        }
        sink.texts += PlacedText(node.text, node.color, node.bold, x + offset, y, width - offset)
        return TextNode.LINE_HEIGHT
    }

    private fun placeHotspot(node: HotspotNode, x: Int, y: Int, width: Int, parentHeight: Int?, sink: Sink): Int {
        val height = when (val size = node.h) {
            is UiSize.Px -> size.value
            else -> parentHeight ?: 0
        }
        sink.hotspots += PlacedHotspot(x, y, width, height, node.action, node.tooltip)
        return height
    }

    private fun placeContainer(node: ContainerNode, x: Int, y: Int, width: Int, parentHeight: Int?, sink: Sink): Int {
        val fixedHeight = when (val h = node.h) {
            is UiSize.Px -> h.value
            UiSize.Grow -> parentHeight
            UiSize.Fit -> null
        }
        val surfaceAt = sink.images.size
        val area = Area(
            x + node.pad,
            y + node.pad,
            (width - node.pad * 2).coerceAtLeast(0),
            fixedHeight?.let { (it - node.pad * 2).coerceAtLeast(0) },
        )
        val flow = node.children.filter { it.position == null }
        val content = when (node.kind) {
            ContainerKind.Row -> placeRow(node, flow, area, sink)
            ContainerKind.Column -> placeColumn(node, flow, area, sink)
            ContainerKind.Box -> placeBox(node, flow, area, sink)
            ContainerKind.Grid -> placeGrid(node, flow, area, sink)
        }
        val positionedBottom = node.children.mapNotNull { child ->
            val position = child.position ?: return@mapNotNull null
            val available = (width - position.x).coerceAtLeast(0)
            position.y + place(child, x + position.x, y + position.y, widthIn(child, available), null, sink)
        }.maxOrNull() ?: 0

        val height = fixedHeight ?: maxOf(content + node.pad * 2, positionedBottom)
        node.surface?.let { sink.images.add(surfaceAt, PlacedImage(it, x, y, width, height, ImageVariant.Surface)) }
        return height
    }

    private fun placeRow(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        val gaps = node.gap * (flow.size - 1).coerceAtLeast(0)
        val grows = flow.count { it.w == UiSize.Grow }
        var remaining = (area.width - gaps).coerceAtLeast(0)
        val assigned = flow.map { child ->
            if (child.w == UiSize.Grow) 0 else widthIn(child, remaining).also { remaining = (remaining - it).coerceAtLeast(0) }
        }
        val share = if (grows == 0) 0 else remaining / grows
        val extra = if (grows == 0) 0 else remaining % grows
        var growIndex = 0
        val widths = flow.mapIndexed { i, child ->
            if (child.w == UiSize.Grow) share + (if (growIndex++ < extra) 1 else 0) else assigned[i]
        }
        val heights = flow.mapIndexed { i, child -> measureHeight(child, area.y, widths[i], area.height) }
        val rowHeight = area.height ?: (heights.maxOrNull() ?: 0)
        var cx = area.x + if (grows == 0) align(area.width - widths.sum() - gaps, node.align) else 0

        flow.forEachIndexed { i, child ->
            val childHeight = if (child.h == UiSize.Grow) rowHeight else heights[i]
            place(child, cx, area.y + align(rowHeight - childHeight, node.cross), widths[i], rowHeight, sink)
            cx += widths[i] + node.gap
        }
        return rowHeight
    }

    private fun placeColumn(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        var cy = area.y
        flow.forEach { child ->
            cy = snapped(cy, node.snap)
            val w = widthIn(child, area.width)
            val h = place(child, area.x + align(area.width - w, node.cross), cy, w, area.height, sink)
            cy += h + node.gap
        }
        return (cy - area.y - if (flow.isEmpty()) 0 else node.gap).coerceAtLeast(0)
    }

    private fun placeBox(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        val widths = flow.map { widthIn(it, area.width) }
        val heights = flow.mapIndexed { i, child -> measureHeight(child, area.y, widths[i], area.height) }
        val boxHeight = area.height ?: (heights.maxOrNull() ?: 0)

        flow.forEachIndexed { i, child ->
            val childHeight = if (child.h == UiSize.Grow) boxHeight else heights[i]
            place(
                child,
                area.x + align(area.width - widths[i], node.align),
                area.y + align(boxHeight - childHeight, node.cross),
                widths[i],
                boxHeight,
                sink,
            )
        }
        return boxHeight
    }

    private fun placeGrid(node: ContainerNode, flow: List<UiNode>, area: Area, sink: Sink): Int {
        val room = (area.width - node.gap * (node.columns - 1)).coerceAtLeast(0)
        val cells = List(node.columns) { room / node.columns + if (it < room % node.columns) 1 else 0 }
        val starts = cells.runningFold(0) { acc, cell -> acc + cell + node.gap }
        var cy = area.y

        flow.chunked(node.columns).forEach { line ->
            cy = snapped(cy, node.snap)
            val heights = line.mapIndexed { i, child -> measureHeight(child, cy, widthIn(child, cells[i]), null) }
            val lineHeight = heights.maxOrNull() ?: 0
            line.forEachIndexed { i, child -> place(child, area.x + starts[i], cy, widthIn(child, cells[i]), lineHeight, sink) }
            cy += lineHeight + node.gap
        }
        return (cy - area.y - if (flow.isEmpty()) 0 else node.gap).coerceAtLeast(0)
    }

    private fun snapped(y: Int, snap: Int): Int = if (snap > 0) (y + snap - 1) / snap * snap else y

    private fun align(free: Int, align: UiAlign): Int = when (align) {
        UiAlign.Start -> 0
        UiAlign.Center -> (free + 1) / 2
        UiAlign.End -> free
    }.coerceAtLeast(0)
}
```

- [ ] **Step 5: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Renderer

**Files:**
- Create: `dialog/render/UiRenderer.kt`

**Interfaces:**
- Consumes: `UiKit`, `UiImage`, `UiSlice`, `IconSize`, `SurfaceSize` (Task 2); `LaidOut`, `PlacedImage`, `PlacedText`, `PlacedHotspot`, `ImageVariant`, `UiAction`, `TextMeasurer`, `Spaces` (Task 3)
- Produces: `class UiRenderer(kit: UiKit, click: (UiAction) -> ClickEvent, warn: (String) -> Unit) { fun render(laid: LaidOut): List<Component>; companion: LINE = 9, fun bodyWidth(width: Int): Int, fun dialogWidth(width: Int): Int }`

- [ ] **Step 1: `dialog/render/UiRenderer.kt`**

Porta de doc §8. As imagens saem na **ordem de emissão** do layout, sem ordenar por x. Spans continuam primeiro e ordenados por x.

```kotlin
package one_block.textura.dialog.render

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.SurfaceSize
import one_block.textura.dialog.kit.UiImage
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiSlice
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.LaidOut
import one_block.textura.dialog.layout.PlacedHotspot
import one_block.textura.dialog.layout.PlacedImage
import one_block.textura.dialog.layout.PlacedText
import one_block.textura.dialog.node.UiAction

class UiRenderer(
    private val kit: UiKit,
    private val click: (UiAction) -> ClickEvent,
    private val warn: (String) -> Unit,
) {

    private val spaces = Spaces(kit)
    private val measurer = TextMeasurer(kit)
    private val clicks = HashMap<PlacedHotspot, ClickEvent>()

    private data class Span(val start: Int, val end: Int, val hotspot: PlacedHotspot)

    private inner class Line {
        val builder = Component.text()
        var cursor = 0

        fun moveTo(target: Int) {
            val text = spaces.of(target - cursor)
            if (text.isNotEmpty()) builder.append(Component.text(text).font(kit.spaceFont))
            cursor = target
        }

        fun append(part: Component, advance: Int) {
            builder.append(part)
            cursor += advance
        }
    }

    fun render(laid: LaidOut): List<Component> {
        val body = bodyWidth(laid.width)
        val margin = (body - laid.width) / 2
        val count = maxOf(1, (laid.height + LINE - 1) / LINE)

        return (0 until count).map { n ->
            val line = Line()
            val spans = spans(laid, n)
            spans.forEach { hotspot(line, margin, it) }
            markers(line, margin, spans)
            line.moveTo(0)
            laid.images.filter { it.y / LINE == n }.forEach { image(line, margin, it, laid.width) }
            laid.texts.filter { it.y / LINE == n }.sortedBy { it.x }.forEach { text(line, margin, it, laid.width) }
            line.moveTo(body)
            line.builder.build()
        }
    }

    private fun spans(laid: LaidOut, n: Int): List<Span> {
        val top = n * LINE
        val bottom = top + LINE
        val ordered = laid.hotspots
            .filter { it.y < bottom && it.y + it.h > top && (it.action != null || it.tooltip != null) }
            .sortedWith(
                compareByDescending<PlacedHotspot> { minOf(it.y + it.h, bottom) - maxOf(it.y, top) }
                    .thenBy { it.y }
                    .thenBy { it.x },
            )
        val claimed = mutableListOf<Span>()

        ordered.forEach { hotspot ->
            if (hotspot.x + hotspot.w > laid.width) warn("Área clicável em x=${hotspot.x} passa da largura ${laid.width} e foi cortada")
            if (claimed.any { overlaps(it.hotspot, hotspot) }) warn("Área clicável em x=${hotspot.x} sobrepõe outra na mesma linha")

            claimed += free(maxOf(hotspot.x, 0), minOf(hotspot.x + hotspot.w, laid.width), claimed)
                .map { (start, end) -> Span(start, end, hotspot) }
        }
        return claimed.sortedBy { it.start }
    }

    private fun overlaps(a: PlacedHotspot, b: PlacedHotspot): Boolean = a.x < b.x + b.w && b.x < a.x + a.w

    private fun free(start: Int, end: Int, claimed: List<Span>): List<Pair<Int, Int>> =
        claimed.fold(listOf(start to end)) { parts, span ->
            parts.flatMap { (s, e) ->
                if (span.end <= s || span.start >= e) {
                    listOf(s to e)
                } else {
                    listOf(s to span.start, span.end to e).filter { it.first < it.second }
                }
            }
        }.filter { it.first < it.second }

    private fun hotspot(line: Line, margin: Int, span: Span) {
        line.moveTo(margin + span.start)
        var part: TextComponent = Component.text(spaces.of(span.end - span.start)).font(kit.spaceFont)
        span.hotspot.action?.let { action -> part = part.clickEvent(clicks.getOrPut(span.hotspot) { click(action) }) }
        span.hotspot.tooltip?.let { part = part.hoverEvent(HoverEvent.showText(it)) }
        line.append(part, span.end - span.start)
    }

    private fun markers(line: Line, margin: Int, spans: List<Span>) {
        val hit = kit.hit ?: return
        if (spans.isEmpty()) return

        val font = kit.imageFont(0)
        line.moveTo(margin + spans.minOf { it.start })
        line.append(Component.text(hit.char).font(font), hit.advance)
        line.moveTo(margin + spans.maxOf { it.end } - 1)
        line.append(Component.text(hit.char).font(font), hit.advance)
    }

    private fun image(line: Line, margin: Int, placed: PlacedImage, width: Int) {
        val image = resolve(placed) ?: return
        val drawWidth = if (image.slice != null) placed.width else image.width

        if (placed.x < 0 || placed.x + drawWidth > width) {
            warn("Imagem '${placed.id}' em x=${placed.x} passa da largura $width e foi descartada")
            return
        }

        val font = kit.imageFont(placed.y % LINE)
        line.moveTo(margin + placed.x)

        val slice = image.slice
        if (slice == null) {
            line.append(Component.text(image.char).font(font), image.advance)
            return
        }
        sliced(line, margin, placed, slice, font)
    }

    private fun resolve(placed: PlacedImage): UiImage? = when (val variant = placed.variant) {
        ImageVariant.Plain -> kit.image(placed.id) ?: run {
            warn("Imagem '${placed.id}' não existe")
            null
        }
        is ImageVariant.Icon -> kit.icon(IconSize(placed.id, variant.size))
            ?: kit.image(placed.id)?.also { warn("Ícone '${placed.id}' em ${variant.size} px não pré-gerado — rode /textura gerar") }
            ?: run {
                warn("Ícone '${placed.id}' não existe")
                null
            }
        ImageVariant.Surface -> kit.surface(SurfaceSize(placed.id, placed.width, placed.height)) ?: run {
            warn("Surface '${placed.id}' ${placed.width}x${placed.height} não pré-gerada — rode /textura gerar")
            null
        }
    }

    private fun sliced(line: Line, margin: Int, placed: PlacedImage, slice: UiSlice, font: Key) {
        if (placed.width < slice.left + slice.right) {
            warn("Imagem '${placed.id}' mais estreita que as tampas")
            return
        }

        slice.leftCap?.let { line.append(Component.text(it.char).font(font), it.advance) }

        var x = placed.x + slice.left
        var left = placed.width - slice.left - slice.right
        val sizes = slice.middles.keys.sortedDescending()

        while (left > 0) {
            val size = sizes.firstOrNull { it <= left } ?: break
            val piece = slice.middles.getValue(size)
            line.moveTo(margin + x)
            line.append(Component.text(piece.char).font(font), piece.advance)
            x += size
            left -= size
        }

        slice.rightCap?.let { cap ->
            line.moveTo(margin + placed.x + placed.width - slice.right)
            line.append(Component.text(cap.char).font(font), cap.advance)
        }
    }

    private fun text(line: Line, margin: Int, placed: PlacedText, width: Int) {
        val room = minOf(placed.maxWidth, width - placed.x)
        if (room <= 0) {
            warn("Texto '${placed.text}' começa fora da largura e foi descartado")
            return
        }

        val content = measurer.fit(placed.text, placed.bold, room)
        if (content.isEmpty()) {
            if (placed.text.isNotEmpty()) warn("Texto '${placed.text}' não cabe em $room px e foi descartado")
            return
        }
        if (content != placed.text) warn("Texto '${placed.text}' cortado para caber em $room px")

        var part = Component.text(content).font(kit.textFont(placed.y % LINE))
        placed.color?.let { part = part.color(it) }
        if (placed.bold) part = part.decoration(TextDecoration.BOLD, true)

        line.moveTo(margin + placed.x)
        line.append(part, measurer.width(content, placed.bold))
    }

    companion object {
        const val LINE = 9
        private const val WIDGET_PADDING = 8
        private const val SMALL_BODY = 256
        private const val LARGE_BODY = 400

        fun bodyWidth(width: Int): Int = if (width < SMALL_BODY) SMALL_BODY else LARGE_BODY

        fun dialogWidth(width: Int): Int = bodyWidth(width) + WIDGET_PADDING
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 5: Tema, definição de tela, parser e registries

**Files:**
- Create: `dialog/theme/UiTheme.kt`, `dialog/json/ScreenDefinition.kt`, `dialog/json/ScreenParser.kt`, `registry/UiThemeRegistry.kt`, `registry/UiScreenRegistry.kt`

**Interfaces:**
- Consumes: `UiSize`, `UiAlign`, `UiTextAlign`, `ContainerKind`, `UiPosition` (Task 3)
- Produces:
  - `object UiColors { fun parse(raw: String?): TextColor? }`
  - `data class UiTextStyle(color: TextColor?, bold: Boolean)`, `SwitchTheme(on, off, onLabel, offLabel)`, `SegmentedTheme(art, selectedArt, textStyle, selectedTextStyle, pad)`, `ButtonTheme(art, textStyle)`, `data class UiTheme(id, iconGap, iconSize, styles, surfaces, switch, segmented, button, icons) { fun style(name: String?): UiTextStyle; fun controlArt(): Set<String>; companion: DEFAULT_STYLE = "body", TOOLTIP_STYLE = "tooltip" }`
  - `sealed interface DefNode { val path: String }`, `DefContainer`, `DefSpace`, `DefText`, `DefIcon`, `DefImage`, `DefSwitch`, `DefSegmented`, `DefButton` (campos abaixo), `data class ScreenDefinition(id, theme, title: String?, close: String?, root: DefContainer)`
  - `class ScreenParser(logger) { fun parse(file: File): ScreenDefinition? }`
  - `class UiThemeRegistry(logger) { val themes: Map<String, UiTheme>; fun load(folder: File) }`
  - `class UiScreenRegistry(logger) { val screens: Map<String, ScreenDefinition>; fun load(folder: File) }`

- [ ] **Step 1: `dialog/theme/UiTheme.kt`**

```kotlin
package one_block.textura.dialog.theme

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

object UiColors {

    fun parse(raw: String?): TextColor? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (value.startsWith("#")) TextColor.fromHexString(value) else NamedTextColor.NAMES.value(value.lowercase())
    }
}

data class UiTextStyle(val color: TextColor?, val bold: Boolean)

data class SwitchTheme(val on: String, val off: String, val onLabel: String, val offLabel: String)

data class SegmentedTheme(
    val art: String,
    val selectedArt: String,
    val textStyle: String,
    val selectedTextStyle: String,
    val pad: Int,
)

data class ButtonTheme(val art: String, val textStyle: String)

data class UiTheme(
    val id: String,
    val iconGap: Int,
    val iconSize: Int,
    val styles: Map<String, UiTextStyle>,
    val surfaces: Map<String, String>,
    val switch: SwitchTheme,
    val segmented: SegmentedTheme,
    val button: ButtonTheme,
    val icons: Map<String, String>,
) {

    fun style(name: String?): UiTextStyle =
        styles[name ?: DEFAULT_STYLE] ?: styles[DEFAULT_STYLE] ?: UiTextStyle(null, false)

    fun controlArt(): Set<String> =
        setOf(switch.on, switch.off, segmented.art, segmented.selectedArt, button.art)

    companion object {
        const val DEFAULT_STYLE = "body"
        const val TOOLTIP_STYLE = "tooltip"
    }
}
```

- [ ] **Step 2: `registry/UiThemeRegistry.kt`**

```kotlin
package one_block.textura.registry

import one_block.textura.dialog.theme.ButtonTheme
import one_block.textura.dialog.theme.SegmentedTheme
import one_block.textura.dialog.theme.SwitchTheme
import one_block.textura.dialog.theme.UiColors
import one_block.textura.dialog.theme.UiTextStyle
import one_block.textura.dialog.theme.UiTheme
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Logger

class UiThemeRegistry(private val logger: Logger) {

    @Volatile
    var themes: Map<String, UiTheme> = emptyMap()
        private set

    fun load(folder: File) {
        val files = folder.listFiles { file -> file.isFile && file.extension == "yml" }.orEmpty().sortedBy { it.name }
        themes = files.flatMap { file ->
            val yaml = YamlConfiguration.loadConfiguration(file)
            yaml.getKeys(false).mapNotNull { key -> yaml.getConfigurationSection(key)?.let { key to read(key, it) } }
        }.toMap()
    }

    private fun read(id: String, section: ConfigurationSection): UiTheme = UiTheme(
        id = id,
        iconGap = section.getInt("icon-gap", 3),
        iconSize = section.getInt("icon-size", 8),
        styles = styles(id, section.getConfigurationSection("text")),
        surfaces = strings(section.getConfigurationSection("surfaces")),
        switch = SwitchTheme(
            on = section.getString("switch.on") ?: "ui:$id/switch_on",
            off = section.getString("switch.off") ?: "ui:$id/switch_off",
            onLabel = section.getString("switch.labels.on") ?: "Ligado",
            offLabel = section.getString("switch.labels.off") ?: "Desligado",
        ),
        segmented = SegmentedTheme(
            art = section.getString("segmented.art") ?: "ui:$id/segment",
            selectedArt = section.getString("segmented.selected-art") ?: "ui:$id/segment_selected",
            textStyle = section.getString("segmented.text-style") ?: "muted",
            selectedTextStyle = section.getString("segmented.selected-text-style") ?: UiTheme.DEFAULT_STYLE,
            pad = section.getInt("segmented.pad", 4),
        ),
        button = ButtonTheme(
            art = section.getString("button.art") ?: "ui:$id/button",
            textStyle = section.getString("button.text-style") ?: UiTheme.DEFAULT_STYLE,
        ),
        icons = strings(section.getConfigurationSection("icons")),
    )

    private fun styles(themeId: String, section: ConfigurationSection?): Map<String, UiTextStyle> {
        if (section == null) return emptyMap()
        return section.getKeys(false).associateWith { name ->
            val raw = section.getString("$name.color")
            val color = UiColors.parse(raw)
            if (raw != null && color == null) logger.warning("[ui] Tema '$themeId': cor '$raw' do estilo '$name' inválida")
            UiTextStyle(color, section.getBoolean("$name.bold", false))
        }
    }

    private fun strings(section: ConfigurationSection?): Map<String, String> =
        section?.getKeys(false)?.mapNotNull { key -> section.getString(key)?.let { key to it } }?.toMap().orEmpty()
}
```

- [ ] **Step 3: `dialog/json/ScreenDefinition.kt`**

```kotlin
package one_block.textura.dialog.json

import net.kyori.adventure.text.format.TextColor
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiPosition
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.node.UiTextAlign

sealed interface DefNode {
    val path: String
}

data class DefContainer(
    val kind: ContainerKind,
    val children: List<DefNode>,
    val w: UiSize,
    val h: UiSize,
    val gap: Int,
    val pad: Int,
    val align: UiAlign,
    val cross: UiAlign,
    val snap: Int,
    val columns: Int,
    val surface: String?,
    val position: UiPosition?,
    override val path: String,
) : DefNode

data class DefSpace(val px: Int, override val path: String) : DefNode

data class DefText(
    val text: String,
    val style: String?,
    val align: UiTextAlign,
    val w: UiSize,
    val icon: String?,
    val color: TextColor?,
    val bold: Boolean?,
    val tooltip: String?,
    val position: UiPosition?,
    override val path: String,
) : DefNode

data class DefIcon(val id: String, val size: Int, val position: UiPosition?, override val path: String) : DefNode

data class DefImage(val id: String, val w: UiSize, val position: UiPosition?, override val path: String) : DefNode

data class DefSwitch(val value: Boolean, val bind: String?, val tooltip: String?, override val path: String) : DefNode

data class DefSegmented(
    val options: List<String>,
    val selected: Int,
    val bind: String?,
    val w: UiSize,
    val tooltip: String?,
    override val path: String,
) : DefNode

data class DefButton(
    val label: String,
    val bind: String?,
    val w: UiSize,
    val icon: String?,
    val tooltip: String?,
    override val path: String,
) : DefNode

data class ScreenDefinition(
    val id: String,
    val theme: String,
    val title: String?,
    val close: String?,
    val root: DefContainer,
)
```

- [ ] **Step 4: `dialog/json/ScreenParser.kt`**

```kotlin
package one_block.textura.dialog.json

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiPosition
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.node.UiTextAlign
import one_block.textura.dialog.theme.UiColors
import java.io.File
import java.io.FileReader
import java.util.logging.Logger

class ScreenParser(private val logger: Logger) {

    fun parse(file: File): ScreenDefinition? {
        val id = file.nameWithoutExtension
        val json = runCatching { FileReader(file, Charsets.UTF_8).use { JsonParser.parseReader(it) } }.getOrElse {
            logger.warning("[ui:$id] JSON inválido: ${it.message}")
            return null
        }
        val obj = json.takeIf { it.isJsonObject }?.asJsonObject ?: run {
            logger.warning("[ui:$id] o arquivo precisa ser um objeto JSON")
            return null
        }
        val theme = obj.string("theme") ?: run {
            logger.warning("[ui:$id] falta 'theme' no nó raiz")
            return null
        }
        val root = Reader(id).node(obj, "", 0, root = true) as? DefContainer ?: run {
            logger.warning("[ui:$id] o nó raiz precisa ser row, column, box ou grid")
            return null
        }
        val close = if (obj.get("close")?.isJsonNull == true) null else obj.string("close") ?: DEFAULT_CLOSE

        return ScreenDefinition(id, theme, obj.string("title"), close, root)
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private inner class Reader(private val id: String) {

        fun node(obj: JsonObject, parent: String, index: Int, root: Boolean = false): DefNode? {
            val shape = CONTAINERS.keys.firstOrNull { obj.has(it) } ?: LEAVES.firstOrNull { obj.has(it) }
            val label = obj.string("name") ?: shape ?: "#$index"
            val path = if (parent.isEmpty()) label else "$parent > $label"

            if (shape == null) {
                warn(path, "nó sem forma reconhecível, descartado")
                return null
            }

            val node = when (shape) {
                "space" -> DefSpace(obj.int("space", 0, path), path)
                "text" -> text(obj, path)
                "icon" -> DefIcon(obj.string("icon").orEmpty(), obj.int("size", DEFAULT_ICON_SIZE, path), position(obj, path), path)
                "image" -> DefImage(obj.string("image").orEmpty(), size(obj, "w", path), position(obj, path), path)
                "switch" -> DefSwitch(obj.bool("switch", path), obj.string("bind"), obj.string("tooltip"), path)
                "segmented" -> segmented(obj, path)
                "button" -> DefButton(obj.string("button").orEmpty(), obj.string("bind"), size(obj, "w", path), obj.string("icon"), obj.string("tooltip"), path)
                else -> container(obj, CONTAINERS.getValue(shape), shape, path)
            }

            val allowed = KNOWN.getValue(shape) + COMMON + if (root) ROOT else emptySet()
            obj.keySet().filterNot { it in allowed }.forEach { warn(path, "chave desconhecida '$it'") }
            return node
        }

        private fun container(obj: JsonObject, kind: ContainerKind, key: String, path: String): DefContainer {
            val children = obj.array(key, path).mapIndexedNotNull { index, element ->
                if (element.isJsonObject) {
                    node(element.asJsonObject, path, index)
                } else {
                    warn(path, "filho #$index não é um objeto, descartado")
                    null
                }
            }
            return DefContainer(
                kind = kind,
                children = children,
                w = size(obj, "w", path),
                h = size(obj, "h", path),
                gap = obj.int("gap", 0, path),
                pad = obj.int("pad", 0, path),
                align = align(obj, "align", path),
                cross = align(obj, "cross", path),
                snap = obj.int("snap", 0, path).coerceAtLeast(0),
                columns = obj.int("columns", 1, path).coerceAtLeast(1),
                surface = obj.string("surface"),
                position = position(obj, path),
                path = path,
            )
        }

        private fun text(obj: JsonObject, path: String): DefText {
            val scale = obj.get("scale")?.let { runCatching { it.asDouble }.getOrNull() }
            if (scale != null && scale != 1.0) warn(path, "scale $scale não suportado, usando 1")

            val color = obj.string("color")?.let { raw ->
                UiColors.parse(raw) ?: run {
                    warn(path, "cor '$raw' inválida")
                    null
                }
            }
            val bold = obj.get("bold")?.let { runCatching { it.asBoolean }.getOrNull() }

            return DefText(
                text = obj.string("text").orEmpty(),
                style = obj.string("style"),
                align = textAlign(obj, path),
                w = size(obj, "w", path),
                icon = obj.string("icon"),
                color = color,
                bold = bold,
                tooltip = obj.string("tooltip"),
                position = position(obj, path),
                path = path,
            )
        }

        private fun segmented(obj: JsonObject, path: String): DefSegmented {
            val options = obj.array("segmented", path).mapNotNull { runCatching { it.asString }.getOrNull() }
            return DefSegmented(options, obj.int("selected", 0, path), obj.string("bind"), size(obj, "w", path), obj.string("tooltip"), path)
        }

        private fun size(obj: JsonObject, key: String, path: String): UiSize {
            val element = obj.get(key) ?: return UiSize.Fit
            val primitive = element.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
            return when {
                primitive?.isNumber == true -> UiSize.Px(primitive.asInt.coerceAtLeast(0))
                primitive?.isString == true && primitive.asString == "grow" -> UiSize.Grow
                primitive?.isString == true && primitive.asString == "fit" -> UiSize.Fit
                else -> {
                    warn(path, "'$key' inválido ($element), usando fit")
                    UiSize.Fit
                }
            }
        }

        private fun align(obj: JsonObject, key: String, path: String): UiAlign = when (val value = obj.string(key)) {
            null, "start" -> UiAlign.Start
            "center" -> UiAlign.Center
            "end" -> UiAlign.End
            else -> {
                warn(path, "'$key' inválido ($value), usando start")
                UiAlign.Start
            }
        }

        private fun textAlign(obj: JsonObject, path: String): UiTextAlign = when (val value = obj.string("align")) {
            null, "left" -> UiTextAlign.Left
            "center" -> UiTextAlign.Center
            "right" -> UiTextAlign.Right
            else -> {
                warn(path, "'align' inválido ($value), usando left")
                UiTextAlign.Left
            }
        }

        private fun position(obj: JsonObject, path: String): UiPosition? {
            if (!obj.has("x") && !obj.has("y")) return null
            return UiPosition(obj.int("x", 0, path), obj.int("y", 0, path))
        }

        private fun JsonObject.int(key: String, default: Int, path: String): Int {
            val element = get(key) ?: return default
            return runCatching { element.asInt }.getOrElse {
                warn(path, "'$key' deveria ser número")
                default
            }
        }

        private fun JsonObject.bool(key: String, path: String): Boolean {
            val element = get(key) ?: return false
            return runCatching { element.asBoolean }.getOrElse {
                warn(path, "'$key' deveria ser true ou false")
                false
            }
        }

        private fun JsonObject.array(key: String, path: String): JsonArray =
            get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray().also { warn(path, "'$key' deveria ser uma lista") }

        private fun warn(path: String, message: String) {
            logger.warning("[ui:$id] $path: $message")
        }
    }

    companion object {
        const val DEFAULT_CLOSE = "Fechar"
        const val DEFAULT_ICON_SIZE = 8

        private val CONTAINERS = linkedMapOf(
            "column" to ContainerKind.Column,
            "row" to ContainerKind.Row,
            "box" to ContainerKind.Box,
            "grid" to ContainerKind.Grid,
        )
        private val LEAVES = listOf("space", "text", "icon", "image", "switch", "segmented", "button")
        private val COMMON = setOf("name")
        private val ROOT = setOf("theme", "title", "close")
        private val CONTAINER_KEYS = setOf("w", "h", "gap", "pad", "align", "cross", "snap", "columns", "surface", "x", "y")
        private val KNOWN: Map<String, Set<String>> = CONTAINERS.keys.associateWith { CONTAINER_KEYS + it } + mapOf(
            "space" to setOf("space"),
            "text" to setOf("text", "style", "align", "w", "icon", "color", "bold", "scale", "tooltip", "x", "y"),
            "icon" to setOf("icon", "size", "x", "y"),
            "image" to setOf("image", "w", "x", "y"),
            "switch" to setOf("switch", "bind", "tooltip"),
            "segmented" to setOf("segmented", "selected", "bind", "w", "tooltip"),
            "button" to setOf("button", "bind", "w", "icon", "tooltip"),
        )
    }
}
```

- [ ] **Step 5: `registry/UiScreenRegistry.kt`**

```kotlin
package one_block.textura.registry

import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.json.ScreenParser
import java.io.File
import java.util.logging.Logger

class UiScreenRegistry(private val logger: Logger) {

    @Volatile
    var screens: Map<String, ScreenDefinition> = emptyMap()
        private set

    fun load(folder: File) {
        val parser = ScreenParser(logger)
        screens = folder.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty()
            .sortedBy { it.name }
            .mapNotNull(parser::parse)
            .associateBy { it.id }
    }
}
```

- [ ] **Step 6: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 6: Binds, sessão e compilador

**Files:**
- Create: `api/DialogBinds.kt`, `dialog/DialogSession.kt`, `dialog/json/ScreenCompiler.kt`

**Interfaces:**
- Consumes: Tasks 2–5. `UiKit.has`, `TextMeasurer`, `UiLayout.intrinsicWidth`, `UiTheme`, `ScreenDefinition`/`Def*`, `TexturaTags.resolver` (existente em `api/TexturaTags.kt`)
- Produces:
  - `class DialogBinds { fun switch(key: String, get: () -> Boolean, set: (Boolean) -> Unit); fun segmented(key: String, get: () -> Int, set: (Int) -> Unit); fun button(key: String, onClick: (Player) -> Unit) }` (estado `internal`: `switches`, `segments`, `buttons`)
  - `class DialogSession(screenId: String, binds: DialogBinds) { fun switch(key, default): Boolean; fun setSwitch(key, value); fun segment(key, default): Int; fun setSegment(key, value); fun press(key, player) }`
  - `class ScreenCompiler(kit: UiKit, warn: (String) -> Unit) { fun compile(definition: ScreenDefinition, theme: UiTheme, session: DialogSession): UiScreen; companion: MAX_WIDTH = 399, VALUE = "{value}" }`

- [ ] **Step 1: `api/DialogBinds.kt`**

```kotlin
package one_block.textura.api

import org.bukkit.entity.Player

class DialogBinds {

    internal class SwitchBind(val get: () -> Boolean, val set: (Boolean) -> Unit)

    internal class SegmentBind(val get: () -> Int, val set: (Int) -> Unit)

    internal val switches = mutableMapOf<String, SwitchBind>()
    internal val segments = mutableMapOf<String, SegmentBind>()
    internal val buttons = mutableMapOf<String, (Player) -> Unit>()

    fun switch(key: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        switches[key] = SwitchBind(get, set)
    }

    fun segmented(key: String, get: () -> Int, set: (Int) -> Unit) {
        segments[key] = SegmentBind(get, set)
    }

    fun button(key: String, onClick: (Player) -> Unit) {
        buttons[key] = onClick
    }
}
```

- [ ] **Step 2: `dialog/DialogSession.kt`**

```kotlin
package one_block.textura.dialog

import one_block.textura.api.DialogBinds
import org.bukkit.entity.Player

class DialogSession(val screenId: String, private val binds: DialogBinds) {

    private val switches = mutableMapOf<String, Boolean>()
    private val segments = mutableMapOf<String, Int>()

    fun switch(key: String, default: Boolean): Boolean =
        binds.switches[key]?.get?.invoke() ?: switches[key] ?: default

    fun setSwitch(key: String, value: Boolean) {
        val bind = binds.switches[key]
        if (bind != null) bind.set(value) else switches[key] = value
    }

    fun segment(key: String, default: Int): Int =
        binds.segments[key]?.get?.invoke() ?: segments[key] ?: default

    fun setSegment(key: String, value: Int) {
        val bind = binds.segments[key]
        if (bind != null) bind.set(value) else segments[key] = value
    }

    fun press(key: String, player: Player) {
        binds.buttons[key]?.invoke(player)
    }
}
```

- [ ] **Step 3: `dialog/json/ScreenCompiler.kt`**

```kotlin
package one_block.textura.dialog.json

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.DialogSession
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.node.ContainerKind
import one_block.textura.dialog.node.ContainerNode
import one_block.textura.dialog.node.HotspotNode
import one_block.textura.dialog.node.IconNode
import one_block.textura.dialog.node.ImageNode
import one_block.textura.dialog.node.TextNode
import one_block.textura.dialog.node.UiAction
import one_block.textura.dialog.node.UiAlign
import one_block.textura.dialog.node.UiNode
import one_block.textura.dialog.node.UiPosition
import one_block.textura.dialog.node.UiScreen
import one_block.textura.dialog.node.UiSize
import one_block.textura.dialog.render.TextMeasurer
import one_block.textura.dialog.theme.UiTheme

class ScreenCompiler(private val kit: UiKit, private val warn: (String) -> Unit) {

    private val measurer = TextMeasurer(kit)
    private val layout = UiLayout(kit)
    private val mini = MiniMessage.miniMessage()

    fun compile(definition: ScreenDefinition, theme: UiTheme, session: DialogSession): UiScreen {
        val root = Context(theme, session).container(definition.root)
        val wanted = when (val w = root.w) {
            is UiSize.Px -> w.value
            UiSize.Grow -> MAX_WIDTH
            UiSize.Fit -> layout.intrinsicWidth(root)
        }
        if (wanted > MAX_WIDTH) warn("largura $wanted passa de $MAX_WIDTH px e foi limitada")
        val width = wanted.coerceIn(1, MAX_WIDTH)
        return UiScreen(width, ContainerNode(ContainerKind.Box, listOf(root), w = UiSize.Px(width)))
    }

    private inner class Context(private val theme: UiTheme, private val session: DialogSession) {

        fun node(def: DefNode): UiNode = when (def) {
            is DefContainer -> container(def)
            is DefSpace -> ContainerNode.space(def.px)
            is DefText -> text(def)
            is DefIcon -> IconNode(icon(def.id), def.size, def.position)
            is DefImage -> ImageNode(def.id, def.w, def.position)
            is DefSwitch -> switch(def)
            is DefSegmented -> segmented(def)
            is DefButton -> button(def)
        }

        fun container(def: DefContainer): ContainerNode = ContainerNode(
            kind = def.kind,
            children = def.children.map(::node),
            gap = def.gap,
            pad = def.pad,
            align = def.align,
            cross = def.cross,
            columns = def.columns,
            snap = def.snap,
            surface = def.surface?.let { name ->
                theme.surfaces[name] ?: run {
                    warn("${def.path}: surface '$name' não existe no tema '${theme.id}'")
                    null
                }
            },
            w = def.w,
            h = def.h,
            position = def.position,
        )

        private fun text(def: DefText): UiNode {
            val tooltip = def.tooltip?.let { tooltip(it, null) }
            val content = textContent(def, if (tooltip == null) def.position else null)
            if (tooltip == null) return content
            return ContainerNode(ContainerKind.Box, listOf(content, HotspotNode(null, tooltip)), w = def.w, position = def.position)
        }

        private fun textContent(def: DefText, position: UiPosition?): UiNode {
            val style = theme.style(def.style)
            val color = def.color ?: style.color
            val bold = def.bold ?: style.bold
            if (def.icon == null) return TextNode(def.text, color, bold, def.align, def.w, position)

            val label = TextNode(def.text, color, bold, def.align, if (def.w == UiSize.Grow) UiSize.Grow else UiSize.Fit)
            return ContainerNode(
                ContainerKind.Row,
                listOf(IconNode(icon(def.icon), theme.iconSize), label),
                gap = theme.iconGap,
                cross = UiAlign.Center,
                w = def.w,
                position = position,
            )
        }

        private fun switch(def: DefSwitch): UiNode {
            val key = def.bind ?: def.path
            val value = session.switch(key, def.value)
            val label = if (value) theme.switch.onLabel else theme.switch.offLabel
            val action = UiAction(reopen = true) { session.setSwitch(key, !value) }
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(if (value) theme.switch.on else theme.switch.off),
                    HotspotNode(action, def.tooltip?.let { tooltip(it, label) }),
                ),
            )
        }

        private fun segmented(def: DefSegmented): UiNode {
            if (def.options.isEmpty()) {
                warn("${def.path}: segmented sem opções")
                return ContainerNode.space(0)
            }
            val key = def.bind ?: def.path
            val selected = session.segment(key, def.selected).coerceIn(0, def.options.lastIndex)
            val normal = theme.style(theme.segmented.textStyle)
            val active = theme.style(theme.segmented.selectedTextStyle)
            val tooltip = def.tooltip?.let { tooltip(it, def.options[selected]) }

            val segments = def.options.mapIndexed { index, option ->
                val chosen = index == selected
                val style = if (chosen) active else normal
                val width = maxOf(measurer.width(option, normal.bold), measurer.width(option, active.bold)) + theme.segmented.pad * 2
                ContainerNode(
                    ContainerKind.Box,
                    listOf(
                        ImageNode(if (chosen) theme.segmented.selectedArt else theme.segmented.art, w = UiSize.Grow),
                        TextNode(option, style.color, style.bold),
                        HotspotNode(UiAction(reopen = true) { session.setSegment(key, index) }, tooltip),
                    ),
                    align = UiAlign.Center,
                    cross = UiAlign.Center,
                    w = UiSize.Px(width),
                )
            }
            return ContainerNode(ContainerKind.Row, segments, w = def.w)
        }

        private fun button(def: DefButton): UiNode {
            val key = def.bind ?: def.path
            val style = theme.style(theme.button.textStyle)
            val label = listOfNotNull(
                ContainerNode.space(1),
                def.icon?.let { IconNode(icon(it), theme.iconSize) },
                TextNode(def.label, style.color, style.bold),
                ContainerNode.space(1),
            )
            val action = UiAction(reopen = false) { player -> session.press(key, player) }
            return ContainerNode(
                ContainerKind.Box,
                listOf(
                    ImageNode(theme.button.art, w = UiSize.Grow),
                    ContainerNode(ContainerKind.Row, label, gap = theme.iconGap, cross = UiAlign.Center),
                    HotspotNode(action, def.tooltip?.let { tooltip(it, null) }),
                ),
                align = UiAlign.Center,
                cross = UiAlign.Center,
                w = def.w,
            )
        }

        private fun icon(id: String): String {
            if (!id.startsWith(UiArtCatalog.UI_PREFIX) || kit.has(id)) return id
            val name = id.removePrefix(UiArtCatalog.UI_PREFIX)
            return UiArtCatalog.GLYPH_PREFIX + (theme.icons[name] ?: name)
        }

        private fun tooltip(text: String, value: String?): Component {
            val resolved = if (value == null) text else text.replace(VALUE, value)
            return mini.deserialize(resolved, TexturaTags.resolver)
                .colorIfAbsent(theme.style(UiTheme.TOOLTIP_STYLE).color)
        }
    }

    companion object {
        const val MAX_WIDTH = 399
        const val VALUE = "{value}"
    }
}
```

- [ ] **Step 4: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 7: Geração no pack e kit publicado

**Files:**
- Create: `dialog/kit/UiCodepoints.kt`, `dialog/kit/UiPieces.kt`, `dialog/kit/PackedUiKit.kt`, `dialog/kit/UiUsage.kt`, `pack/UiPackWriter.kt`
- Modify: `pack/ResourcePackBuilder.kt` (construtor, `build()`, `addResourceFiles`)
- Modify: `TexturaPlugin.kt` (campos, `onEnable`, `reloadConfigData`, `generatePack`)

**Interfaces:**
- Consumes: Tasks 2–6
- Produces:
  - `object UiCodepoints { fun assign(names: Collection<String>): Map<String, String> }`
  - `class UiPieceSource(val name: String, val image: BufferedImage)`, `object UiPieces { MIDDLE_SIZES; fun padded(image): BufferedImage; fun slices(id, image, left, right): List<UiPieceSource> }`, `object SurfaceComposer { fun compose(image, slice: ArtSlice, width, height): BufferedImage? }`
  - `class PackedUiKit(images: Map<String, UiImage>, surfaces: Map<SurfaceSize, UiImage>, icons: Map<IconSize, UiImage>, textFont: UiTextFont, hit: UiPiece?) : UiKit`
  - `data class UiUsage(images: Set<String>, icons: Set<IconSize>, surfaces: Set<SurfaceSize>) { companion fun collect(screens, themes, kit, warn): UiUsage }`
  - `class UiPackWriter(catalog, textFont, screens: Collection<ScreenDefinition>, themes: Map<String, UiTheme>, logger) { fun write(builder: PackBuilder): UiKit }`
  - `data class BuiltPack(val zip: File, val uiKit: UiKit)` em `ResourcePackBuilder.kt`; `ResourcePackBuilder.build(): BuiltPack`
  - Em `TexturaPlugin`: `uiScreenRegistry`, `uiThemeRegistry`, `@Volatile var uiKit: UiKit?` (leitura pública), `fun uiCatalog(): UiArtCatalog`, `fun uiTextFont(): UiTextFont`

- [ ] **Step 1: `dialog/kit/UiCodepoints.kt`** (doc §4.6)

```kotlin
package one_block.textura.dialog.kit

import java.security.MessageDigest

object UiCodepoints {

    private const val BASE = 0xF0000
    private const val SPAN = 0xFFFE

    fun assign(names: Collection<String>): Map<String, String> {
        val unique = names.toSortedSet()
        require(unique.size <= SPAN) { "Peças de ui demais: ${unique.size} para $SPAN codepoints" }

        val taken = BooleanArray(SPAN)
        val result = sortedMapOf<String, String>()

        unique.map { it to hashSlot(it) }
            .sortedWith(compareBy({ it.second }, { it.first }))
            .forEach { (name, start) ->
                var index = start
                while (taken[index]) index = (index + 1) % SPAN
                taken[index] = true
                result[name] = String(Character.toChars(BASE + index))
            }
        return result
    }

    private fun hashSlot(name: String): Int {
        val digest = MessageDigest.getInstance("SHA-1").digest(name.toByteArray(Charsets.UTF_8))
        val value = ((digest[0].toInt() and 0xFF) shl 24) or ((digest[1].toInt() and 0xFF) shl 16) or
            ((digest[2].toInt() and 0xFF) shl 8) or (digest[3].toInt() and 0xFF)
        return Math.floorMod(value, SPAN)
    }
}
```

- [ ] **Step 2: `dialog/kit/UiPieces.kt`**

```kotlin
package one_block.textura.dialog.kit

import java.awt.image.BufferedImage

class UiPieceSource(val name: String, val image: BufferedImage)

object UiPieces {

    private const val MIN_HEIGHT = 8

    val MIDDLE_SIZES: List<Int> = (0..8).map { 1 shl it }

    fun padded(image: BufferedImage): BufferedImage {
        if (image.height >= MIN_HEIGHT) return image

        val result = BufferedImage(image.width, MIN_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.drawImage(image, 0, 0, null)
        graphics.dispose()
        return result
    }

    fun slices(id: String, image: BufferedImage, left: Int, right: Int): List<UiPieceSource> {
        val middle = image.getSubimage(left, 0, image.width - left - right, image.height)
        val caps = listOfNotNull(
            if (left > 0) UiPieceSource("$id#left", image.getSubimage(0, 0, left, image.height)) else null,
            if (right > 0) UiPieceSource("$id#right", image.getSubimage(image.width - right, 0, right, image.height)) else null,
        )
        return caps + MIDDLE_SIZES.map { size -> UiPieceSource("$id#mid$size", tiled(middle, size)) }
    }

    private fun tiled(middle: BufferedImage, size: Int): BufferedImage {
        val result = BufferedImage(size, middle.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        var x = 0
        while (x < size) {
            graphics.drawImage(middle, x, 0, null)
            x += middle.width
        }
        graphics.dispose()
        return result
    }
}

object SurfaceComposer {

    fun compose(image: BufferedImage, slice: ArtSlice, width: Int, height: Int): BufferedImage? {
        if (width < slice.left + slice.right || height < slice.top + slice.bottom) return null

        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val sx = intArrayOf(0, slice.left, image.width - slice.right, image.width)
        val sy = intArrayOf(0, slice.top, image.height - slice.bottom, image.height)
        val dx = intArrayOf(0, slice.left, width - slice.right, width)
        val dy = intArrayOf(0, slice.top, height - slice.bottom, height)

        for (row in 0..2) {
            val sourceHeight = sy[row + 1] - sy[row]
            if (sourceHeight == 0) continue
            for (column in 0..2) {
                val sourceWidth = sx[column + 1] - sx[column]
                if (sourceWidth == 0) continue
                for (y in dy[row] until dy[row + 1]) {
                    for (x in dx[column] until dx[column + 1]) {
                        val pixel = image.getRGB(sx[column] + (x - dx[column]) % sourceWidth, sy[row] + (y - dy[row]) % sourceHeight)
                        result.setRGB(x, y, pixel)
                    }
                }
            }
        }
        return result
    }
}
```

- [ ] **Step 3: `dialog/kit/PackedUiKit.kt`**

```kotlin
package one_block.textura.dialog.kit

import net.kyori.adventure.key.Key

class PackedUiKit(
    private val images: Map<String, UiImage>,
    private val surfaces: Map<SurfaceSize, UiImage>,
    private val icons: Map<IconSize, UiImage>,
    private val textFont: UiTextFont,
    override val hit: UiPiece?,
) : UiKit {

    private val spaces = UiFonts.spaceChars()
    private val known: Set<String> = images.keys + icons.keys.map { it.id } + surfaces.keys.map { it.id }

    override fun has(id: String): Boolean = id in known

    override fun image(id: String): UiImage? = images[id]

    override fun surface(size: SurfaceSize): UiImage? = surfaces[size]

    override fun icon(size: IconSize): UiImage? = icons[size]

    override fun imageFont(offset: Int): Key = UiFonts.image(offset)

    override fun textFont(offset: Int): Key = UiFonts.text(offset)

    override val spaceFont: Key = UiFonts.SPACE

    override fun spaceChar(amount: Int): String? = spaces[amount]

    override fun charAdvance(codePoint: Int): Int = textFont.advance(codePoint)
}
```

- [ ] **Step 4: `dialog/kit/UiUsage.kt`**

```kotlin
package one_block.textura.dialog.kit

import one_block.textura.api.DialogBinds
import one_block.textura.dialog.DialogSession
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.layout.ImageVariant
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.theme.UiTheme

data class UiUsage(val images: Set<String>, val icons: Set<IconSize>, val surfaces: Set<SurfaceSize>) {

    companion object {

        fun collect(
            screens: Collection<ScreenDefinition>,
            themes: Map<String, UiTheme>,
            kit: UiKit,
            warn: (String) -> Unit,
        ): UiUsage {
            val images = mutableSetOf<String>()
            val icons = mutableSetOf<IconSize>()
            val surfaces = mutableSetOf<SurfaceSize>()

            screens.forEach { definition ->
                val theme = themes[definition.theme]
                if (theme == null) {
                    warn("Tela '${definition.id}': tema '${definition.theme}' não existe")
                    return@forEach
                }
                images += theme.controlArt()

                val compiler = ScreenCompiler(kit) { warn("Tela '${definition.id}': $it") }
                val screen = compiler.compile(definition, theme, DialogSession(definition.id, DialogBinds()))
                UiLayout(kit).layout(screen).images.forEach { placed ->
                    when (val variant = placed.variant) {
                        ImageVariant.Plain -> images += placed.id
                        is ImageVariant.Icon -> icons += IconSize(placed.id, variant.size)
                        ImageVariant.Surface -> surfaces += SurfaceSize(placed.id, placed.width, placed.height)
                    }
                }
            }
            return UiUsage(images, icons, surfaces)
        }
    }
}
```

- [ ] **Step 5: `pack/UiPackWriter.kt`**

```kotlin
package one_block.textura.pack

import net.kyori.adventure.key.Key
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.GlyphAdvance
import one_block.textura.dialog.kit.IconScale
import one_block.textura.dialog.kit.IconSize
import one_block.textura.dialog.kit.PackedUiKit
import one_block.textura.dialog.kit.SurfaceComposer
import one_block.textura.dialog.kit.SurfaceSize
import one_block.textura.dialog.kit.UiArt
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiCodepoints
import one_block.textura.dialog.kit.UiFonts
import one_block.textura.dialog.kit.UiImage
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiPiece
import one_block.textura.dialog.kit.UiPieceSource
import one_block.textura.dialog.kit.UiPieces
import one_block.textura.dialog.kit.UiSlice
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.dialog.theme.UiTheme
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.texture.Texture
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.logging.Logger
import javax.imageio.ImageIO

class UiPackWriter(
    private val catalog: UiArtCatalog,
    private val textFont: UiTextFont,
    private val screens: Collection<ScreenDefinition>,
    private val themes: Map<String, UiTheme>,
    private val logger: Logger,
) {

    private data class Packed(val texture: Key, val height: Int, val piece: UiPiece)

    fun write(builder: PackBuilder): UiKit {
        val warned = mutableSetOf<String>()
        val usage = UiUsage.collect(screens, themes, BaseUiKit(catalog, textFont)) { if (warned.add(it)) logger.warning("[ui] $it") }

        val arts = usage.images.mapNotNull { id -> catalog.art(id) ?: missing(id) }.associateBy { it.id }
        val surfaces = usage.surfaces.mapNotNull { size -> surface(size)?.let { size to it } }.toMap()
        val icons = usage.icons.mapNotNull { size -> icon(size)?.let { size to it } }.toMap()
        val hit = UiPieceSource(HIT, BufferedImage(1, HIT_HEIGHT, BufferedImage.TYPE_INT_ARGB))

        val sources = arts.values.flatMap(::artSources) + surfaces.values + icons.values + hit
        val chars = UiCodepoints.assign(sources.map { it.name })
        val packed = sources.associate { it.name to texture(builder, it, chars.getValue(it.name)) }

        writeFonts(builder, packed.values)
        logger.info("[ui] ${sources.size} peça(s), ${surfaces.size} surface(s), ${icons.size} ícone(s), ${screens.size} tela(s).")

        return PackedUiKit(
            images = arts.mapValues { (id, art) -> image(id, art, packed) },
            surfaces = surfaces.mapValues { (size, source) -> plain(packed.getValue(source.name), size.width, size.height) },
            icons = icons.mapValues { (size, source) -> plain(packed.getValue(source.name), source.image.width, size.size) },
            textFont = textFont,
            hit = packed.getValue(HIT).piece,
        )
    }

    private fun plain(packed: Packed, width: Int, height: Int): UiImage =
        UiImage(packed.piece.char, packed.piece.advance, width, height)

    private fun image(id: String, art: UiArt, packed: Map<String, Packed>): UiImage {
        val main = packed.getValue(id).piece
        val slice = art.slice?.takeUnless { it.nine }?.let { s ->
            UiSlice(
                left = s.left,
                right = s.right,
                leftCap = packed["$id#left"]?.piece,
                rightCap = packed["$id#right"]?.piece,
                middles = UiPieces.MIDDLE_SIZES.associateWith { packed.getValue("$id#mid$it").piece },
            )
        }
        return UiImage(main.char, main.advance, art.image.width, art.image.height, slice)
    }

    private fun artSources(art: UiArt): List<UiPieceSource> {
        val main = UiPieceSource(art.id, art.image)
        val slice = art.slice?.takeUnless { it.nine } ?: return listOf(main)
        return listOf(main) + UiPieces.slices(art.id, art.image, slice.left, slice.right)
    }

    private fun surface(size: SurfaceSize): UiPieceSource? {
        val art = catalog.art(size.id) ?: return missing(size.id)
        val slice = art.slice?.takeIf { it.nine } ?: run {
            logger.warning("[ui] Surface '${size.id}' precisa de .slice com 'esquerda direita topo base'")
            return null
        }
        if (size.width > UiArtCatalog.MAX_WIDTH || size.height > UiArtCatalog.MAX_HEIGHT) {
            logger.warning("[ui] Surface '${size.id}' ${size.width}x${size.height} passa de ${UiArtCatalog.MAX_WIDTH}x${UiArtCatalog.MAX_HEIGHT}; fundo não será desenhado")
            return null
        }
        val image = SurfaceComposer.compose(art.image, slice, size.width, size.height) ?: run {
            logger.warning("[ui] Surface '${size.id}' ${size.width}x${size.height} é menor que as tampas")
            return null
        }
        return UiPieceSource("surface:${size.id}@${size.width}x${size.height}", image)
    }

    private fun icon(size: IconSize): UiPieceSource? {
        val art = catalog.art(size.id) ?: return missing(size.id)
        return UiPieceSource("icon:${size.id}@${size.size}", IconScale.resize(art.image, size.size))
    }

    private fun <T> missing(id: String): T? {
        logger.warning("[ui] Imagem '$id' não encontrada em resource/assets/oneblock/textures/ui/ nem nos glyphs")
        return null
    }

    private fun texture(builder: PackBuilder, source: UiPieceSource, char: String): Packed {
        val padded = UiPieces.padded(source.image)
        val key = Key.key(NAMESPACE, "ui_gen/${hash(source.name)}.png")
        builder.pack.texture(Texture.texture(key, Writable.bytes(png(padded))))
        return Packed(key, padded.height, UiPiece(char, GlyphAdvance.of(source.image)))
    }

    private fun writeFonts(builder: PackBuilder, pieces: Collection<Packed>) {
        val textSpace = FontProvider.space(mapOf(" " to UiTextFont.SPACE_ADVANCE, "\u200c" to 0))

        for (k in 0 until UiFonts.VARIANTS) {
            builder.addFont(
                UiFonts.image(k),
                pieces.map { FontProvider.bitMap(it.texture, it.height, UiFonts.BASE_ASCENT - k, listOf(it.piece.char)) },
            )
            builder.addFont(
                UiFonts.text(k),
                listOf<FontProvider>(textSpace) + textFont.providers.map { FontProvider.bitMap(it.file, it.height, it.ascent - k, it.chars) },
            )
        }
        builder.addFont(UiFonts.SPACE, listOf(FontProvider.space(UiFonts.spaceChars().entries.associate { (amount, char) -> char to amount })))
    }

    private fun png(image: BufferedImage): ByteArray =
        ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }

    private fun hash(name: String): String =
        MessageDigest.getInstance("SHA-1").digest(name.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(16)

    companion object {
        private const val NAMESPACE = "oneblock"
        private const val HIT = "ui:#hit"
        private const val HIT_HEIGHT = 9
    }
}
```

- [ ] **Step 6: Modificar `pack/ResourcePackBuilder.kt`**

1. Adicione `ui: UiPackWriter` como **último** parâmetro do construtor (`private val ui: UiPackWriter`).
2. Acima da classe, adicione `data class BuiltPack(val zip: File, val uiKit: UiKit)` e o import `one_block.textura.dialog.kit.UiKit`.
3. Troque `build()` por:

```kotlin
    fun build(): BuiltPack {
        val builder = PackBuilder(packFormat)

        addResourceFiles(builder)

        fonts.write(builder)
        items.write(builder)
        blocks.write(builder)
        blocks.writeLangOverrides(builder)
        val uiKit = ui.write(builder)
        mergeExternalPacks(builder)

        return BuiltPack(builder.build(File(generatedFolder, "resourcepack.zip")), uiKit)
    }
```

Os dois comentários `//` e os KDoc já existentes no arquivo ficam como estão.

4. Em `addResourceFiles`, troque o filtro:

```kotlin
        val files = assets.resourceFiles()
            .filterNot { it.packPath in langsSobrescritos || it.packPath.endsWith(SLICE_EXTENSION) }
```

e adicione na classe:

```kotlin
    private companion object {
        const val SLICE_EXTENSION = ".slice"
    }
```

- [ ] **Step 7: Modificar `TexturaPlugin.kt`: registries, kit e catálogo**

Imports novos:

```kotlin
import one_block.textura.dialog.kit.UiArtCatalog
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.kit.UiTextFont
import one_block.textura.dialog.kit.UiTextFontLoader
import one_block.textura.pack.UiPackWriter
import one_block.textura.registry.UiScreenRegistry
import one_block.textura.registry.UiThemeRegistry
```

Campos, junto aos outros `lateinit`:

```kotlin
    lateinit var uiScreenRegistry: UiScreenRegistry
        private set
    lateinit var uiThemeRegistry: UiThemeRegistry
        private set
    @Volatile
    var uiKit: UiKit? = null
        private set
```

Em `onEnable`, logo depois de `BlockApi.init(blockRegistry, blockAllocator)`:

```kotlin
        uiScreenRegistry = UiScreenRegistry(logger)
        uiThemeRegistry = UiThemeRegistry(logger)
```

e junto aos `mkdirs`:

```kotlin
        File(dataFolder, "configs/ui").mkdirs()
```

Em `reloadConfigData`, depois de `blockRegistry.load(...)`:

```kotlin
        uiThemeRegistry.load(assets.configFolder("ui"))
        uiScreenRegistry.load(assets.configFolder("ui"))
```

Novos métodos na classe:

```kotlin
    fun uiCatalog(): UiArtCatalog {
        val assets = PackAssets(dataFolder)
        val glyphsFolder = assets.configFolder("glyphs")
        return UiArtCatalog(File(assets.resourceRoot, UI_ART_FOLDER), { id ->
            glyphRegistry.glyphs[id]?.takeIf { it.sheetKey == null }?.let { assets.resolveGlyph(it.caminho, glyphsFolder)?.file }
        }, logger)
    }

    fun uiTextFont(): UiTextFont {
        val assets = PackAssets(dataFolder)
        val json = File(assets.configFolder("fonts"), config.getString("ui.fonte-texto") ?: DEFAULT_UI_TEXT_FONT)
        return UiTextFontLoader.load(json, assets.resourceRoot, logger)
    }
```

No `private companion object` existente, adicione:

```kotlin
        const val UI_ART_FOLDER = "assets/oneblock/textures/ui"
        const val DEFAULT_UI_TEXT_FONT = "custom.json"
```

- [ ] **Step 8: Modificar `TexturaPlugin.generatePack`**

Troque a construção do builder e a linha do zip por:

```kotlin
                val ui = UiPackWriter(uiCatalog(), uiTextFont(), uiScreenRegistry.screens.values, uiThemeRegistry.themes, logger)
                val builder = ResourcePackBuilder(
                    glyphRegistry, fontRegistry, titleFontRegistry, itemRegistry,
                    blockRegistry, blockAllocator, dataFolder, logger,
                    config.getInt("pack.formato", PackBuilder.DEFAULT_PACK_FORMAT),
                    ui,
                )
                val built = builder.build()
                val zip = built.zip
                uiKit = built.uiKit
                val hash = PackHash.calculateSHA1(zip)
```

e, na mensagem de sucesso, depois de `§7Blocos`, adicione:

```kotlin
                    sender.sendMessage("§7Telas de UI: ${uiScreenRegistry.screens.size}")
```

- [ ] **Step 9: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 8: Opener, status do pack, API e comando

**Files:**
- Create: `dialog/DialogTokens.kt`, `listener/PackStatusTracker.kt`, `dialog/DialogOpener.kt`, `api/DialogApi.kt`
- Modify: `TexturaPlugin.kt` (`onEnable`, `reloadConfigData`, novo `auditUi`)
- Modify: `command/TexturaCommand.kt`

**Interfaces:**
- Consumes: Tasks 2–7. `TexturaPlugin.uiKit`, `uiScreenRegistry`, `uiThemeRegistry`, `applyPack(Player)`, `uiCatalog()`, `uiTextFont()`
- Produces:
  - `class DialogTokens { fun reserve(): Long; fun commit(id: UUID, token: Long); fun isCurrent(id: UUID, token: Long): Boolean; fun forget(id: UUID) }`
  - `class PackStatusTracker(onQuit: (UUID) -> Unit) : Listener { fun loaded(player: Player): Boolean }`
  - `class DialogOpener(plugin: JavaPlugin, kit: () -> UiKit?, screens: UiScreenRegistry, themes: UiThemeRegistry, packStatus: PackStatusTracker, tokens: DialogTokens, applyPack: (Player) -> Unit) { fun open(player: Player, screenId: String, binds: DialogBinds): Boolean }`
  - `object DialogApi { fun init(opener: DialogOpener); fun open(player: Player, screenId: String, binds: DialogBinds.() -> Unit = {}): Boolean }`

- [ ] **Step 1: `dialog/DialogTokens.kt`**

```kotlin
package one_block.textura.dialog

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class DialogTokens {

    private val counter = AtomicLong()
    private val current = ConcurrentHashMap<UUID, Long>()

    fun reserve(): Long = counter.incrementAndGet()

    fun commit(id: UUID, token: Long) {
        current[id] = token
    }

    fun isCurrent(id: UUID, token: Long): Boolean = current[id] == token

    fun forget(id: UUID) {
        current.remove(id)
    }
}
```

- [ ] **Step 2: `listener/PackStatusTracker.kt`**

```kotlin
package one_block.textura.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PackStatusTracker(private val onQuit: (UUID) -> Unit) : Listener {

    private val loaded = ConcurrentHashMap.newKeySet<UUID>()

    fun loaded(player: Player): Boolean = player.uniqueId in loaded

    @EventHandler
    fun onStatus(event: PlayerResourcePackStatusEvent) {
        val id = event.player.uniqueId
        when (event.status) {
            Status.SUCCESSFULLY_LOADED -> loaded += id
            Status.DECLINED, Status.FAILED_DOWNLOAD, Status.INVALID_URL, Status.FAILED_RELOAD, Status.DISCARDED -> loaded -= id
            else -> Unit
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        loaded -= event.player.uniqueId
        onQuit(event.player.uniqueId)
    }
}
```

- [ ] **Step 3: `dialog/DialogOpener.kt`**

```kotlin
package one_block.textura.dialog

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.textura.api.DialogBinds
import one_block.textura.api.TexturaTags
import one_block.textura.dialog.json.ScreenCompiler
import one_block.textura.dialog.json.ScreenDefinition
import one_block.textura.dialog.kit.UiKit
import one_block.textura.dialog.layout.UiLayout
import one_block.textura.dialog.node.UiAction
import one_block.textura.dialog.render.UiRenderer
import one_block.textura.listener.PackStatusTracker
import one_block.textura.registry.UiScreenRegistry
import one_block.textura.registry.UiThemeRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DialogOpener(
    private val plugin: JavaPlugin,
    private val kit: () -> UiKit?,
    private val screens: UiScreenRegistry,
    private val themes: UiThemeRegistry,
    private val packStatus: PackStatusTracker,
    private val tokens: DialogTokens,
    private val applyPack: (Player) -> Unit,
) {

    private val mini = MiniMessage.miniMessage()
    private val warned = ConcurrentHashMap.newKeySet<String>()

    private val callbackOptions = ClickCallback.Options.builder()
        .uses(ClickCallback.UNLIMITED_USES)
        .lifetime(Duration.ofMinutes(5))
        .build()

    private val closeOptions = ClickCallback.Options.builder()
        .uses(1)
        .lifetime(Duration.ofHours(12))
        .build()

    fun open(player: Player, screenId: String, binds: DialogBinds): Boolean =
        show(player, DialogSession(screenId, binds))

    private fun show(player: Player, session: DialogSession): Boolean {
        val definition = screens.screens[session.screenId] ?: run {
            plugin.logger.warning("[ui] Tela '${session.screenId}' não existe")
            return false
        }
        val theme = themes.themes[definition.theme] ?: run {
            plugin.logger.warning("[ui:${definition.id}] Tema '${definition.theme}' não existe")
            return false
        }
        val snapshot = kit() ?: run {
            player.sendMessage(Component.text("Interface indisponível agora, tente de novo em instantes.", NamedTextColor.RED))
            return false
        }
        if (!packStatus.loaded(player)) {
            player.showDialog(fallback())
            return false
        }

        val token = tokens.reserve()
        val warn: (String) -> Unit = { message ->
            if (warned.add("${System.identityHashCode(snapshot)}|${definition.id}|$message")) {
                plugin.logger.warning("[ui:${definition.id}] $message")
            }
        }
        val screen = ScreenCompiler(snapshot, warn).compile(definition, theme, session)
        val renderer = UiRenderer(snapshot, { action -> callback(player.uniqueId, token, action, session) }, warn)
        val lines = renderer.render(UiLayout(snapshot).layout(screen))
        val body = DialogBody.plainMessage(Component.join(JoinConfiguration.newlines(), lines), UiRenderer.dialogWidth(screen.width))

        player.showDialog(dialog(definition, body))
        tokens.commit(player.uniqueId, token)
        return true
    }

    private fun callback(id: UUID, token: Long, action: UiAction, session: DialogSession): ClickEvent =
        ClickEvent.callback({ audience: Audience -> onClick(audience, id, token, action, session) }, callbackOptions)

    private fun onClick(audience: Audience, id: UUID, token: Long, action: UiAction, session: DialogSession) {
        val player = audience as? Player ?: return
        if (!tokens.isCurrent(id, token)) return

        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (!player.isOnline || !tokens.isCurrent(id, token)) return@Runnable
            action.run(player)
            if (action.reopen) show(player, session)
        })
    }

    private fun dialog(definition: ScreenDefinition, body: DialogBody): Dialog = Dialog.create { factory ->
        val title = definition.title?.let { mini.deserialize(it, TexturaTags.resolver) } ?: Component.empty()
        val base = DialogBase.builder(title)
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogBase.DialogAfterAction.NONE)
            .body(listOf(body))
            .build()
        val close = definition.close?.let { label ->
            ActionButton.builder(mini.deserialize(label, TexturaTags.resolver))
                .width(CLOSE_WIDTH)
                .action(DialogAction.customClick({ _, audience -> audience.closeDialog() }, closeOptions))
                .build()
        }
        factory.empty().base(base).type(if (close != null) DialogType.notice(close) else DialogType.notice())
    }

    private fun fallback(): Dialog = Dialog.create { factory ->
        val base = DialogBase.builder(Component.text("Textura necessária", NamedTextColor.YELLOW))
            .canCloseWithEscape(true)
            .body(listOf(DialogBody.plainMessage(Component.text("Aceite a textura do servidor para ver esta tela.", NamedTextColor.GRAY))))
            .build()
        val apply = ActionButton.builder(Component.text("Aplicar textura", NamedTextColor.GREEN))
            .width(CLOSE_WIDTH)
            .action(DialogAction.customClick({ _, audience ->
                val player = audience as? Player ?: return@customClick
                Bukkit.getScheduler().runTask(plugin, Runnable { applyPack(player) })
            }, closeOptions))
            .build()
        factory.empty().base(base).type(DialogType.notice(apply))
    }

    private companion object {
        const val CLOSE_WIDTH = 150
    }
}
```

Se o compilador recusar `return@customClick`, troque a lambda por `{ _, audience -> (audience as? Player)?.let { player -> Bukkit.getScheduler().runTask(plugin, Runnable { applyPack(player) }) } }`.

- [ ] **Step 4: `api/DialogApi.kt`**

```kotlin
package one_block.textura.api

import one_block.textura.dialog.DialogOpener
import org.bukkit.entity.Player

object DialogApi {

    @Volatile
    private var opener: DialogOpener? = null

    fun init(opener: DialogOpener) {
        this.opener = opener
    }

    fun open(player: Player, screenId: String, binds: DialogBinds.() -> Unit = {}): Boolean =
        opener?.open(player, screenId, DialogBinds().apply(binds)) ?: false
}
```

- [ ] **Step 5: Ligar no `TexturaPlugin`**

Imports:

```kotlin
import one_block.textura.api.DialogApi
import one_block.textura.dialog.DialogOpener
import one_block.textura.dialog.DialogTokens
import one_block.textura.dialog.kit.BaseUiKit
import one_block.textura.dialog.kit.UiUsage
import one_block.textura.listener.PackStatusTracker
```

Campo:

```kotlin
    private val dialogTokens = DialogTokens()
```

Em `onEnable`, depois de `HudApi.init(hudManager)`:

```kotlin
        val packStatus = PackStatusTracker(dialogTokens::forget)
        DialogApi.init(DialogOpener(this, { uiKit }, uiScreenRegistry, uiThemeRegistry, packStatus, dialogTokens, ::applyPack))
```

Depois de `commandManager.registerCommand(HudCommand())`:

```kotlin
        commandManager.commandCompletions.registerCompletion("uiscreens") { uiScreenRegistry.screens.keys }
```

Junto aos outros `registerEvents`:

```kotlin
        Bukkit.getPluginManager().registerEvents(packStatus, this)
```

No fim de `reloadConfigData`:

```kotlin
        auditUi()
```

Novo método:

```kotlin
    private fun auditUi() {
        val kit = uiKit ?: return
        val usage = UiUsage.collect(uiScreenRegistry.screens.values, uiThemeRegistry.themes, BaseUiKit(uiCatalog(), uiTextFont())) { }
        val missing = usage.surfaces.filter { kit.surface(it) == null }.map { "surface ${it.id} ${it.width}x${it.height}" } +
            usage.icons.filter { kit.icon(it) == null }.map { "ícone ${it.id} ${it.size}px" } +
            usage.images.filter { !kit.has(it) }.map { "imagem $it" }
        if (missing.isNotEmpty()) {
            logger.warning("[ui] Fora do pack atual (rode /textura gerar): ${missing.joinToString(", ")}")
        }
    }
```

`reloadConfigData()` roda em `onEnable` antes de existir kit. Nesse momento `auditUi` volta cedo porque `uiKit` é `null`, e isso é o esperado.

- [ ] **Step 6: Comando `/textura dialog`**

Em `command/TexturaCommand.kt`, imports:

```kotlin
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Optional
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import one_block.textura.api.DialogApi
```

Método:

```kotlin
    @Subcommand("dialog")
    @CommandCompletion("@uiscreens @players")
    fun dialog(sender: CommandSender, id: String, @Optional target: OnlinePlayer?) {
        val player = target?.player ?: sender as? Player ?: run {
            sender.sendMessage("§cUso: /textura dialog <id> <jogador>")
            return
        }
        if (!DialogApi.open(player, id)) {
            sender.sendMessage("§eTela '$id' não abriu: tela/tema inexistente, pack não gerado ou jogador sem textura. Veja o console.")
        }
    }
```

- [ ] **Step 7: Build**

Run: `./gradlew :plugins:textura:localJar`
Expected: `BUILD SUCCESSFUL`

---

### Task 9: Arte placeholder, tema, tela de exemplo, docs e verificação in-game

**Files:**
- Create: `server/plugins/Textura/resource/assets/oneblock/textures/ui/stone/{sheet,head,segment,segment_selected,button,switch_on,switch_off}.png` + `.png.slice` (gerados)
- Create: `server/plugins/Textura/configs/ui/stone.yml`, `server/plugins/Textura/configs/ui/settings.json`
- Modify: `server/plugins/Textura/config.yml`, `server/plugins/Textura/README.md`, `CLAUDE.md`

**Interfaces:** consome tudo; não produz código.

- [ ] **Step 1: Gerar a arte placeholder**

Salve no scratchpad como `make-ui-art.ps1` e rode com a ferramenta PowerShell (`& "<caminho>\make-ui-art.ps1"`):

```powershell
Add-Type -AssemblyName System.Drawing
$root = 'D:\one-block\server\plugins\Textura\resource\assets\oneblock\textures\ui\stone'
New-Item -ItemType Directory -Force $root | Out-Null

function Color($rgb) { [System.Drawing.Color]::FromArgb(255, $rgb[0], $rgb[1], $rgb[2]) }

function Save($bmp, $name, $slice) {
  $bmp.Save("$root\$name.png", [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  if ($slice) { [System.IO.File]::WriteAllText("$root\$name.png.slice", $slice) }
}

function Box($name, $w, $h, $fill, $border, $slice) {
  $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear((Color $fill))
  $pen = New-Object System.Drawing.Pen (Color $border), 1
  $g.DrawRectangle($pen, 0, 0, $w - 1, $h - 1)
  $pen.Dispose(); $g.Dispose()
  Save $bmp $name $slice
}

function Switch($name, $track, $border, $knob, $knobX) {
  $bmp = New-Object System.Drawing.Bitmap 16, 8, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear((Color $track))
  $pen = New-Object System.Drawing.Pen (Color $border), 1
  $g.DrawRectangle($pen, 0, 0, 15, 7)
  $brush = New-Object System.Drawing.SolidBrush (Color $knob)
  $g.FillRectangle($brush, $knobX, 1, 6, 6)
  $pen.Dispose(); $brush.Dispose(); $g.Dispose()
  Save $bmp $name $null
}

Box 'sheet' 16 16 @(49,50,56) @(22,22,26) '3 3 3 3'
Box 'head' 16 15 @(70,72,80) @(22,22,26) '3 3 3 3'
Box 'segment' 24 12 @(62,63,70) @(28,28,32) '2 2'
Box 'segment_selected' 24 12 @(214,150,52) @(120,80,20) '2 2'
Box 'button' 40 16 @(62,63,70) @(28,28,32) '2 2'
Switch 'switch_on' @(64,168,88) @(26,80,40) @(240,240,240) 9
Switch 'switch_off' @(90,90,98) @(40,40,46) @(200,200,200) 1
```

Confira que existem 7 PNGs e 5 `.slice` em `...\textures\ui\stone\`.

- [ ] **Step 2: `server/plugins/Textura/configs/ui/stone.yml`**

```yaml
stone:
  icon-gap: 3
  icon-size: 8
  text:
    heading: { color: "#ffffff", bold: true }
    body: { color: "#e6e6e6" }
    muted: { color: "#9a9a9a" }
    tooltip: { color: "#c8c8c8" }
  surfaces:
    sheet: ui:stone/sheet
    head: ui:stone/head
  switch:
    on: ui:stone/switch_on
    off: ui:stone/switch_off
    labels: { on: "On", off: "Off" }
  segmented:
    art: ui:stone/segment
    selected-art: ui:stone/segment_selected
    text-style: muted
    selected-text-style: body
    pad: 4
  button:
    art: ui:stone/button
    text-style: body
  icons:
    users: crown
    mail: scroll
    person-add: heart
    chat: book
    monitor: magnifying_glass
    sidebar: key
    sparkle: star
```

- [ ] **Step 3: `server/plugins/Textura/configs/ui/settings.json`**

Copie `docs/superpowers/plans/2026-10-12-dialog-ui-json-settings.json` para `server/plugins/Textura/configs/ui/settings.json`, sem alterações. É o JSON de exemplo do usuário com `"title": "Settings"` no raiz e com os tooltips usando `{value}` no lugar do estado fixo.

```bash
cp docs/superpowers/plans/2026-10-12-dialog-ui-json-settings.json server/plugins/Textura/configs/ui/settings.json
```

- [ ] **Step 4: `config.yml` e docs**

Acrescente ao fim de `server/plugins/Textura/config.yml`:

```yaml
ui:
  fonte-texto: custom.json
```

Em `server/plugins/Textura/README.md`, adicione uma seção `## configs/ui/ — telas de dialog`, antes de `## config.yml`, contendo:
- a tabela de containers e a de folhas da spec (seção "Formato do JSON");
- o yml do tema;
- a ordem de resolução de ids `ui:`/`glyph:`/alias;
- o formato do `.slice`: 2 números = fatia horizontal; 4 = surface;
- a regra "tamanho de surface ou ícone novo só existe depois de `/textura gerar`";
- o comando `/textura dialog <id> [jogador]`.

Em `CLAUDE.md`, na seção "Textura plugin", acrescente `dialog/` à lista de pacotes e um bullet novo:

```markdown
- `dialog/` desenha telas dentro do dialog vanilla (técnica de `docs/superpowers/specs/2026-10-12-dialog-ui-json-design.md`): telas em `configs/ui/<id>.json`, temas em `configs/ui/*.yml`, arte em `resource/assets/oneblock/textures/ui/`. O `/textura gerar` gera as fontes `oneblock:ui/*` e publica o `UiKit`; surfaces e ícones escalados são pré-renderizados por tamanho, então mudar o tamanho de uma tela exige regerar. Outros plugins abrem telas só por `DialogApi.open(player, id) { switch(...); segmented(...); button(...) }`.
```

- [ ] **Step 5: Build, deploy e gerar**

Com o servidor parado, rode `./gradlew :plugins:textura:localDeploy`. Esperado: `BUILD SUCCESSFUL`.

Inicie o servidor. No console, esperado:
- a linha `[ui] N peça(s), 2 surface(s), 7 ícone(s), 1 tela(s).` (as contagens podem variar);
- **nenhum** aviso `chave desconhecida`.

- [ ] **Step 6: Verificação in-game (usuário)**

Peça ao usuário para entrar com o cliente 1.21.11, aceitar o pack e rodar `/textura dialog settings`. Confira junto com ele:

1. Os dois painéis `sheet` com cabeçalho `head` aparecem, as linhas estão alinhadas e não há deslocamento lateral entre elas.
2. Os ícones aparecem nos cabeçalhos e nas linhas, nos glyphs do alias, com 9 px.
3. O hover em cada switch e segmented mostra o tooltip com `{value}` substituído (`On`/`Off`, `Friends`…).
4. Clicar num switch alterna e reabre; clicar num segmento seleciona e reabre. Testar também clicando na **borda inferior** de cada controle.
5. **Review Focus 2:** guardar o dialog aberto, rodar `/textura dialog settings` de novo e clicar em algo; depois fechar e reabrir. Só a tela atual responde.
6. **Review Focus 3:** mudar o texto `"Who can send you messages."` para uma frase de 80 caracteres, rodar `/textura reload` e reabrir. A linha é cortada com `...` e nada quebra para outra linha.
7. **Review Focus 1:** mudar `"gap": 9` do `Body` de Social para `"gap": 12`, rodar `/textura reload` (o console avisa "Fora do pack atual") e reabrir. O fundo do sheet some, a tela abre e o console mostra `Surface 'ui:stone/sheet' … não pré-gerada`. Desfazer, rodar `/textura reload` e depois `/textura gerar` e `/textura aplicarall`.
8. **Review Focus 5:** trocar `"icon": "ui:mail"` por `"icon": "ui:nao_existe"` e rodar `/textura reload`. O console avisa uma vez, a caixa fica vazia e o resto desenha. Desfazer.
9. **Review Focus 4:** com um segundo jogador que recusou o pack (ou usando `/textura dialog settings <ele>` antes do pack carregar), aparece `Textura necessária`.

Anote qualquer divergência e corrija antes de seguir. Se 1–9 passarem, a feature está pronta, com a Task 10 opcional.

---

### Task 10 (opcional): shader que remove o contorno do corpo

Só execute se o spike (Task 1, pergunta 7) mostrou o contorno branco e o usuário quiser removê-lo.

**Files:**
- Create: `plugins/textura/src/main/resources/ui/gui.vsh`, `plugins/textura/src/main/resources/ui/gui.fsh`
- Modify: `pack/UiPackWriter.kt`, `TexturaPlugin.kt` (passar a flag), `server/plugins/Textura/config.yml`

- [ ] **Step 1: Extrair os shaders vanilla do cliente 1.21.11**

```bash
mkdir -p "$SCRATCH/vanilla" && cd "$SCRATCH/vanilla" && tar -xf "$APPDATA/.minecraft/versions/1.21.11/1.21.11.jar" assets/minecraft/shaders/core/gui.vsh assets/minecraft/shaders/core/gui.fsh
```

`$SCRATCH` é o diretório de scratchpad da sessão. Se o jar não estiver nesse caminho, pergunte ao usuário onde está.

- [ ] **Step 2: Escrever as versões modificadas**

Copie os dois arquivos para `plugins/textura/src/main/resources/ui/` **sem alterar** declarações, `#version` e `#moj_import`, e acrescente apenas o que segue.

`gui.vsh`, antes de `void main`:

```glsl
out float uiOutline;

float uiEdge(vec3 position, vec4 color) {
    if (color.r < 0.99 || color.g < 0.99 || color.b < 0.99) return 0.0;
    int screenWidth = int(round(2.0 / ProjMat[0][0]));
    int x = int(round(position.x));
    int widths[2] = int[2](264, 408);
    for (int i = 0; i < 2; i++) {
        int left = (screenWidth - widths[i]) / 2;
        int right = left + widths[i];
        if (abs(x - left) <= 1 || abs(x - right) <= 1) return 1.0;
    }
    return 0.0;
}
```

e, no fim de `main`, `uiOutline = uiEdge(Position, Color);`. Se o vanilla nomear o atributo de cor ou a matriz de outro jeito, use os nomes do vanilla.

`gui.fsh`: declare `in float uiOutline;` e, na primeira linha de `main`, `if (uiOutline > 0.5) discard;`.

- [ ] **Step 3: Escrever no pack atrás da flag**

Em `UiPackWriter`, adicione o parâmetro de construtor `private val shader: Boolean` (último) e, no fim de `write`, antes do `return`:

```kotlin
        if (shader) {
            SHADERS.forEach { name ->
                builder.pack.unknownFile("assets/minecraft/shaders/core/$name", Writable.resource(javaClass.classLoader, "ui/$name"))
            }
        }
```

com `private val SHADERS = listOf("gui.vsh", "gui.fsh")` no `companion object`. Em `TexturaPlugin.generatePack`, passe `config.getBoolean("ui.shader", false)` como último argumento do `UiPackWriter`. Em `config.yml`, abaixo de `fonte-texto`, adicione `  shader: false`.

- [ ] **Step 4: Build, deploy e verificar**

Defina `ui.shader: true`, rode `localDeploy`, `/textura gerar` e `/textura aplicarall`, e peça ao usuário para abrir `/textura dialog settings`:
- o contorno some;
- inventário, chat e menus continuam iguais.

Se qualquer GUI quebrar, volte para `ui.shader: false` e relate ao usuário. Se a borda só sumir em parte, ajuste a tolerância `<= 1`.
