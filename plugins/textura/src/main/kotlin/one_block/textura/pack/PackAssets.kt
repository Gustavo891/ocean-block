package one_block.textura.pack

import net.kyori.adventure.key.Key
import java.io.File

/**
 * Um asset resolvido: o arquivo em disco e o caminho que ele ocupa dentro do
 * pack (ex.: `assets/oneblock/textures/gui/shop.png`).
 */
data class PackAsset(val file: File, val packPath: String, val inResource: Boolean = false) {

    /**
     * Chave preservando a extensão — é o que as fontes usam, porque o cliente
     * resolve `file` do provider como `textures/<file>`.
     */
    val keyWithExtension: Key get() = PackAssets.keyOf(packPath, keepExtension = true)

    /** Chave sem extensão — é o que os modelos usam em `textures.layer0`. */
    val key: Key get() = PackAssets.keyOf(packPath, keepExtension = false)
}

/**
 * Traduz os caminhos declarados nos ymls para arquivos em disco e para posições
 * dentro do pack.
 *
 * O layout novo separa registro de asset:
 *
 * ```
 * configs/{glyphs,fonts,items}/   ymls e JSONs de definição
 * resource/assets/<ns>/...        assets, na mesma estrutura do pack final
 * ```
 *
 * Um caminho de yml que começa com `assets/` é lido literalmente dentro de
 * `resource/`, e a chave do pack sai da própria posição do arquivo — o lugar do
 * arquivo *é* a chave, sem campo extra.
 *
 * O layout antigo (`glyphs/`, `fonts/`, `items/` na raiz, com a chave derivada
 * do id) continua funcionando: todo resolvedor cai nele quando o arquivo não
 * aparece em `resource/`. Assim a migração não precisa ser atômica.
 */
class PackAssets(private val dataFolder: File) {

    val resourceRoot: File = File(dataFolder, "resource")
    private val configsRoot: File = File(dataFolder, "configs")

    /**
     * Pasta de configuração de [kind] (`glyphs`, `fonts`, `items`): `configs/<kind>`
     * quando existe, senão a pasta legada na raiz da pasta de dados.
     */
    fun configFolder(kind: String): File {
        val novo = File(configsRoot, kind)
        if (novo.isDirectory) return novo
        return File(dataFolder, kind)
    }

    /**
     * Resolve um asset declarado no yml.
     *
     * @param declared caminho como está no yml
     * @param legacyRoot pasta onde o layout antigo procurava o arquivo
     * @param legacyPackPath posição no pack quando o caminho é do layout antigo
     */
    fun resolve(declared: String, legacyRoot: File, legacyPackPath: String): PackAsset? {
        if (declared.startsWith(ASSETS_PREFIX)) {
            val file = File(resourceRoot, declared)
            return if (file.isFile) PackAsset(file, declared, inResource = true) else null
        }

        // Layout antigo: o arquivo mora na pasta legada, mas ainda assim pode ter
        // sido movido para resource/ sem o yml ter sido atualizado.
        val moved = File(resourceRoot, legacyPackPath)
        if (moved.isFile) return PackAsset(moved, legacyPackPath, inResource = true)

        val legacyFile = File(legacyRoot, declared)
        if (legacyFile.isFile) return PackAsset(legacyFile, legacyPackPath)

        return null
    }

    /**
     * Resolve a imagem de um glyph. O `caminho` do yml é relativo a `resource/` no
     * layout novo (`assets/<ns>/textures/...`) e à pasta de glyphs no antigo.
     */
    fun resolveGlyph(declared: String, glyphsFolder: File): PackAsset? = resolve(
        declared = declared,
        legacyRoot = glyphsFolder,
        legacyPackPath = "assets/minecraft/textures/$declared",
    )

    /** Arquivos de idioma de `resource/`, que o pack mescla em vez de copiar. */
    fun langFiles(): List<PackAsset> = resourceFiles()
        .filter { it.packPath.startsWith(LANG_PREFIX) && it.packPath.endsWith(".json") }

    /** Todos os arquivos de `resource/`, com o caminho relativo que terão no pack. */
    fun resourceFiles(): List<PackAsset> {
        if (!resourceRoot.isDirectory) return emptyList()
        return resourceRoot.walkTopDown()
            .filter { it.isFile }
            .map { PackAsset(it, it.relativeTo(resourceRoot).invariantSeparatorsPath, inResource = true) }
            .toList()
    }

    companion object {
        private const val ASSETS_PREFIX = "assets/"

        const val LANG_PREFIX = "assets/minecraft/lang/"
        const val DEFAULT_LANG = "assets/minecraft/lang/pt_br.json"

        /**
         * Converte um caminho de pack em chave: `assets/oneblock/textures/gui/x.png`
         * vira `oneblock:gui/x.png` (ou `oneblock:gui/x` sem extensão).
         *
         * Caminhos fora do formato `assets/<ns>/<tipo>/<resto>` viram uma chave
         * `minecraft:<caminho>`, que é o comportamento do layout antigo.
         */
        fun keyOf(packPath: String, keepExtension: Boolean): Key {
            val semExtensao = if (keepExtension) packPath else packPath.substringBeforeLast('.')
            val partes = semExtensao.split('/')

            if (partes.size < 4 || partes[0] != "assets") {
                return Key.key("minecraft", semExtensao)
            }

            val namespace = partes[1]
            // partes[2] é a categoria (textures, models, ...), que não entra na chave.
            val caminho = partes.drop(3).joinToString("/")
            return Key.key(namespace, caminho)
        }
    }
}
