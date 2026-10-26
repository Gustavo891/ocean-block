package one_block.textura.registry

import org.bukkit.configuration.file.YamlConfiguration
import one_block.textura.model.GlyphData
import one_block.textura.model.GlyphFont
import one_block.textura.glyph.GlyphWidthMeasurer
import one_block.textura.pack.PackAssets
import java.io.File
import java.io.IOException
import java.util.logging.Logger

class GlyphRegistry(private val logger: Logger) {

    private val staging = linkedMapOf<String, GlyphData>()

    @Volatile
    var glyphs: Map<String, GlyphData> = emptyMap()
        private set

    @Volatile
    var sizeByChar: Map<String, Int> = emptyMap()
        private set

    /** Unicode ocupado (forma normalizada) -> descrição do glyph que o reservou. */
    private val usedUnicodes = mutableMapOf<String, String>()
    private val dirtyFiles = mutableSetOf<File>()
    private var currentUnicode = 0xE000
    private var assets: PackAssets? = null

    /**
     * [assets] resolve o `caminho` do yml contra `resource/`; sem ele o caminho é
     * lido relativo a [glyphsFolder], que é o layout antigo.
     */
    fun load(glyphsFolder: File, assets: PackAssets? = null) {
        staging.clear()
        usedUnicodes.clear()
        dirtyFiles.clear()
        currentUnicode = 0xE000
        this.assets = assets
        if (!glyphsFolder.exists()) {
            glyphsFolder.mkdirs()
            glyphs = emptyMap()
            sizeByChar = emptyMap()
            return
        }

        val configs = YamlFiles.collect(glyphsFolder).associateWith { YamlConfiguration.loadConfiguration(it) }

        val declaracoes = configs.flatMap { (file, config) -> collectDeclarations(file, config) }
        declaracoes.forEach { usedUnicodes[normalize(it.unicode)] = it.describe() }
        resolveConflicts(configs, declaracoes)

        configs.forEach { (file, config) -> loadYaml(file, config, glyphsFolder) }

        dirtyFiles.forEach { file -> configs[file]?.let { save(it, file) } }
        dirtyFiles.clear()

        sizeByChar = staging.values.associate { it.char to it.size }
        glyphs = staging.toMap()
        staging.clear()
    }

    /**
     * Um unicode escrito à mão no yml. [index] aponta a posição na lista quando a
     * entrada é uma folha; `-1` quando o valor é um escalar.
     */
    private data class UnicodeDeclaration(
        val file: File,
        val path: String,
        val index: Int,
        val id: String,
        val unicode: String
    ) {
        fun describe(): String = "'$id' (${file.name})"
    }

    /**
     * Cataloga todo unicode declarado explicitamente num arquivo: o `unicode` de um
     * glyph simples, cada item do `unicode` de uma folha e o `unicode-specify` de
     * cada fonte nomeada. Nada é alterado aqui — só listado.
     */
    private fun collectDeclarations(file: File, config: YamlConfiguration): List<UnicodeDeclaration> {
        val result = mutableListOf<UnicodeDeclaration>()

        for (key in config.getKeys(false)) {
            val folha = config.contains("$key.amount")
            val ids = if (folha) {
                config.getStringList("$key.ids")
            } else {
                listOf(config.getString("$key.id") ?: key)
            }

            readUnicodes(config, "$key.unicode").forEachIndexed { index, unicode ->
                result += UnicodeDeclaration(
                    file = file,
                    path = "$key.unicode",
                    index = if (folha) index else -1,
                    id = ids.getOrNull(index) ?: key,
                    unicode = unicode
                )
            }

            val fontes = config.getConfigurationSection("$key.fonts") ?: continue
            for (name in fontes.getKeys(false)) {
                val path = "$key.fonts.$name.unicode-specify"
                val specify = config.getString(path)
                if (specify.isNullOrEmpty()) continue
                result += UnicodeDeclaration(
                    file = file,
                    path = path,
                    index = -1,
                    id = "${ids.firstOrNull() ?: key}@$name",
                    unicode = specify
                )
            }
        }

        return result
    }

    /**
     * O primeiro a declarar um unicode fica com ele; qualquer declaração posterior
     * do mesmo unicode é trocada por um livre e reescrita no yml. Como [usedUnicodes]
     * já contém todas as declarações antes desta etapa, o substituto nunca cai em
     * cima de um unicode escrito à mão em outro arquivo.
     */
    private fun resolveConflicts(
        configs: Map<File, YamlConfiguration>,
        declaracoes: List<UnicodeDeclaration>
    ) {
        val donos = mutableMapOf<String, String>()

        for (dec in declaracoes) {
            val norm = normalize(dec.unicode)
            val dono = donos[norm]

            if (dono == null) {
                donos[norm] = dec.describe()
                usedUnicodes[norm] = dec.describe()
                continue
            }

            val substituto = nextFreeUnicode()
            logger.warning(
                "Conflito de unicode '${dec.unicode}' entre ${dec.describe()} e $dono. " +
                    "Trocando ${dec.describe()} para '$substituto'."
            )

            val config = configs[dec.file] ?: continue
            writeUnicode(config, dec, substituto)
            donos[normalize(substituto)] = dec.describe()
            usedUnicodes[normalize(substituto)] = dec.describe()
            dirtyFiles.add(dec.file)
        }
    }

    private fun writeUnicode(config: YamlConfiguration, dec: UnicodeDeclaration, unicode: String) {
        if (dec.index < 0) {
            config.set(dec.path, unicode)
            return
        }
        val atual = readUnicodes(config, dec.path).toMutableList()
        if (dec.index !in atual.indices) return
        atual[dec.index] = unicode
        config.set(dec.path, atual)
    }

    /**
     * Unicodes de um caminho: um escalar (glyph simples) ou uma lista (folha).
     * Lê o valor bruto porque `getString` numa lista devolve o `toString` dela em
     * vez de `null`, o que fazia folhas escaparem da checagem de conflito.
     */
    private fun readUnicodes(config: YamlConfiguration, path: String): List<String> =
        when (val raw = config.get(path)) {
            is List<*> -> raw.mapNotNull { it?.toString()?.takeIf(String::isNotEmpty) }
            is String -> if (raw.isEmpty()) emptyList() else listOf(raw)
            else -> emptyList()
        }

    private fun loadYaml(file: File, config: YamlConfiguration, glyphsFolder: File) {
        for (key in config.getKeys(false)) {
            val caminho = config.getString("$key.caminho") ?: continue
            val carregou = if (config.contains("$key.amount")) {
                loadSheet(config, key, file, caminho, glyphsFolder)
            } else {
                loadSingle(config, key, file, caminho, glyphsFolder)
            }
            if (carregou) dirtyFiles.add(file)
        }
    }

    /** Atributos compartilhados por uma entrada do yml — a folha inteira, quando é folha. */
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

        // Largura em pixels do glyph (para medir texto). O yml manda quando declara
        // 'size'; senão deriva da imagem PNG. Não persiste — fica derivado.
        val size = if (config.contains("$key.size")) {
            config.getInt("$key.size")
        } else {
            GlyphWidthMeasurer.measure(glyphImage(caminho, glyphsFolder), logger) ?: GlyphData.DEFAULT_SIZE
        }

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

        var unicode = readUnicodes(config, "$key.unicode").firstOrNull()
        if (unicode.isNullOrEmpty()) {
            unicode = nextFreeUnicode()
            config.set("$key.unicode", unicode)
            changed = true
        }
        usedUnicodes[normalize(unicode)] = describe(file, id)

        val attrs = readAttributes(config, key, caminho, glyphsFolder) { changed = true }
        val fonts = loadFonts(config, key, attrs.ascent, attrs.height, unicode) { changed = true }

        warnDuplicate(file, id)
        staging[id] = GlyphData(
            key, id, caminho, unicode,
            attrs.ascent, attrs.height, attrs.size, attrs.default, attrs.showInGui, fonts,
            raise = config.getInt("$key.subida", 0)
        )

        return changed
    }

    /**
     * Folha vertical: uma imagem com [amount] fatias empilhadas, cada uma virando um
     * glyph próprio. `ids` e `unicode` são listas paralelas do tamanho de `amount`; a
     * lista de unicode é gerada e persistida quando ausente ou incompleta.
     */
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

        var changed = false
        val declarados = readUnicodes(config, "$key.unicode")
        val unicodes = if (declarados.size == amount) {
            declarados
        } else {
            if (declarados.isNotEmpty()) {
                logger.warning(
                    "Folha '$key' (${file.name}) tem ${declarados.size} unicodes para amount=$amount; " +
                        "completando os faltantes."
                )
            }
            val completos = List(amount) { index -> declarados.getOrNull(index) ?: nextFreeUnicode() }
            config.set("$key.unicode", completos)
            changed = true
            completos
        }

        val attrs = readAttributes(config, key, caminho, glyphsFolder) { changed = true }

        ids.forEachIndexed { index, id ->
            val unicode = unicodes[index]
            usedUnicodes[normalize(unicode)] = describe(file, id)
            val fonts = loadFonts(config, key, attrs.ascent, attrs.height, unicode) { changed = true }
            warnDuplicate(file, id)
            staging[id] = GlyphData(
                key, id, caminho, unicode,
                attrs.ascent, attrs.height, attrs.size, attrs.default, attrs.showInGui, fonts,
                sheetKey = key,
                sheetIndex = index
            )
        }

        return changed
    }

    /**
     * Resolve as fontes nomeadas de um glyph. Onde `ascent`/`height` da fonte não
     * estão definidos, copia os globais e persiste de volta no yml; onde
     * `unicode-specify` está vazio, herda o [unicodeGlobal] (sem persistir).
     */
    private fun loadFonts(
        config: YamlConfiguration,
        key: String,
        ascentGlobal: Int,
        heightGlobal: Int,
        unicodeGlobal: String,
        onChange: () -> Unit
    ): Map<String, GlyphFont> {
        val section = config.getConfigurationSection("$key.fonts") ?: return emptyMap()
        val fonts = mutableMapOf<String, GlyphFont>()

        for (name in section.getKeys(false)) {
            val base = "$key.fonts.$name"

            val ascent = if (config.contains("$base.ascent")) {
                config.getInt("$base.ascent")
            } else {
                config.set("$base.ascent", ascentGlobal)
                onChange()
                ascentGlobal
            }

            val height = if (config.contains("$base.height")) {
                config.getInt("$base.height")
            } else {
                config.set("$base.height", heightGlobal)
                onChange()
                heightGlobal
            }

            val specify = config.getString("$base.unicode-specify")
            val unicode = if (specify.isNullOrEmpty()) unicodeGlobal else specify

            // Namespace da fonte: 'oneblock' faz o glyph entrar em oneblock:<nome>,
            // que e o que <font:oneblock:...> procura.
            val namespace = config.getString("$base.namespace")?.takeIf { it.isNotBlank() }
                ?: one_block.textura.model.FontData.DEFAULT_NAMESPACE

            fonts[name] = GlyphFont(name, ascent, height, unicode, namespace)
        }

        return fonts
    }

    /** Próximo unicode livre na Private Use Area, pulando os já reservados. */
    private fun nextFreeUnicode(): String {
        var candidate = toUnicodeString(currentUnicode)
        while (usedUnicodes.containsKey(normalize(candidate))) {
            currentUnicode++
            candidate = toUnicodeString(currentUnicode)
        }
        currentUnicode++
        return candidate
    }

    /**
     * Normaliza o unicode para uma chave de comparação canônica: resolve tanto a
     * forma `\uXXXX` (qualquer caixa) quanto o caractere literal para o mesmo
     * valor, de modo que representações equivalentes contem como colisão.
     */
    private fun normalize(unicode: String): String = runCatching {
        if (unicode.startsWith("\\u")) {
            unicode.substring(2).toInt(16).toChar().toString()
        } else {
            unicode
        }
    }.getOrDefault(unicode)

    private fun glyphImage(caminho: String, glyphsFolder: File): File =
        assets?.resolveGlyph(caminho, glyphsFolder)?.file ?: File(glyphsFolder, caminho)

    private fun describe(file: File, id: String): String = "'$id' (${file.name})"

    private fun warnDuplicate(file: File, id: String) {
        if (staging.containsKey(id)) logger.warning("Glyph '$id' duplicado em ${file.name}; a ultima definicao vence.")
    }

    private fun save(config: YamlConfiguration, file: File) {
        try {
            config.save(file)
        } catch (e: IOException) {
            logger.warning("Erro ao salvar ${file.name}: ${e.message}")
        }
    }

    private fun toUnicodeString(codePoint: Int): String {
        return "\\u" + String.format("%04X", codePoint)
    }
}
