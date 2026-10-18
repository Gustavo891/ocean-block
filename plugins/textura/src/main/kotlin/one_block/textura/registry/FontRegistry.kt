package one_block.textura.registry

import org.bukkit.configuration.file.YamlConfiguration
import one_block.textura.model.FontData
import java.io.File

class FontRegistry {

    @Volatile
    var fonts: Map<String, FontData> = emptyMap()
        private set

    fun load(fontsFolder: File) {
        if (!fontsFolder.exists()) {
            fontsFolder.mkdirs()
            fonts = emptyMap()
            return
        }

        val fontsYml = File(fontsFolder, "fonts.yml")
        if (!fontsYml.exists()) {
            fonts = emptyMap()
            return
        }

        val loaded = linkedMapOf<String, FontData>()
        val config = YamlConfiguration.loadConfiguration(fontsYml)
        for (key in config.getKeys(false)) {
            val id = config.getString("$key.id") ?: continue
            val caminho = config.getString("$key.caminho") ?: continue
            // 'altura' mantido como fallback para fonts.yml no formato antigo.
            val ascent = config.getInt("$key.ascent", config.getInt("$key.altura", 8))
            val namespace = config.getString("$key.namespace")?.takeIf { it.isNotBlank() }
                ?: FontData.DEFAULT_NAMESPACE

            loaded[id] = FontData(key, id, caminho, ascent, namespace)
        }
        fonts = loaded.toMap()
    }
}
