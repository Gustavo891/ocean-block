package one_block.textura.notify.region

import one_block.textura.hud.anchor.ScreenAnchor
import one_block.textura.notify.layout.LineGeometry
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.function.Consumer

internal object RegionConfigParser {

    const val MIN_LINES = 1
    const val MAX_LINES = 15
    const val MIN_SPACING = -2
    const val MAX_SPACING = 32
    const val MAX_OFFSET = 4096
    const val DEFAULT_LINES = 3
    const val DEFAULT_SPACING = 1

    val ANCHORS: Map<String, ScreenAnchor> = linkedMapOf(
        "TOPO_ESQUERDA" to ScreenAnchor.TOP_LEFT,
        "TOPO_DIREITA" to ScreenAnchor.TOP_RIGHT,
        "MEIO_ESQUERDA" to ScreenAnchor.MIDDLE_LEFT,
        "MEIO_DIREITA" to ScreenAnchor.MIDDLE_RIGHT,
        "BASE_ESQUERDA" to ScreenAnchor.BOTTOM_LEFT,
        "BASE_DIREITA" to ScreenAnchor.BOTTOM_RIGHT,
        "CENTRO" to ScreenAnchor.CENTER,
    )

    private val DIRECTIONS = mapOf("baixo" to StackDirection.DOWN, "cima" to StackDirection.UP)

    val DEFAULTS: List<NotificationRegion> = listOf(
        NotificationRegion("coleta", ScreenAnchor.MIDDLE_RIGHT, 0, 70, 6, DEFAULT_SPACING, true, StackDirection.DOWN),
        NotificationRegion("avisos", ScreenAnchor.TOP_LEFT, 2, 5, 5, DEFAULT_SPACING, true, StackDirection.DOWN, animated = true),
        NotificationRegion("xp", ScreenAnchor.CENTER, 8, 28, 1, DEFAULT_SPACING, false, StackDirection.DOWN, animated = true),
        NotificationRegion("toasts", ScreenAnchor.TOP_RIGHT, -2, 5, 6, 1, false, StackDirection.DOWN, animated = true),
    )

    fun load(file: File, warn: Consumer<String>): List<NotificationRegion> {
        if (!file.isFile) {
            warn.accept("${file.path} não existe; usando as regiões padrão (${DEFAULTS.joinToString { it.id }})")
            return DEFAULTS
        }
        val config = runCatching { YamlConfiguration().apply { load(file) } }.getOrElse {
            warn.accept("${file.name} inválido (${it.message}); usando as regiões padrão")
            return DEFAULTS
        }
        return parse(config, warn)
    }

    fun parse(root: ConfigurationSection, warn: Consumer<String>): List<NotificationRegion> {
        val regions = linkedMapOf<String, NotificationRegion>()
        for (key in root.getKeys(false)) {
            val section = root.getConfigurationSection(key)
            if (section == null) {
                warn.accept("região '$key' não é uma seção; ignorada")
                continue
            }
            val region = region(key, section, warn) ?: continue
            if (region.id in regions) {
                warn.accept("região '${region.id}' repetida; mantida a primeira")
                continue
            }
            regions[region.id] = region
        }
        return regions.values.toList()
    }

    private fun region(key: String, section: ConfigurationSection, warn: Consumer<String>): NotificationRegion? {
        val id = section.getString("id")?.trim()?.takeIf { it.isNotEmpty() } ?: key
        val anchorName = section.getString("ancora")?.trim()?.uppercase()
        val anchor = ANCHORS[anchorName]
        if (anchor == null) {
            warn.accept("região '$id': ancora '${anchorName ?: ""}' inválida (use ${ANCHORS.keys.joinToString()}); ignorada")
            return null
        }

        val x = int(section, id, "x", 0, -MAX_OFFSET, MAX_OFFSET, warn)
        val y = int(section, id, "y", 0, -MAX_OFFSET, MAX_OFFSET, warn)
        val lines = int(section, id, "linhas", DEFAULT_LINES, MIN_LINES, MAX_LINES, warn)
        val spacing = int(section, id, "espacamento", DEFAULT_SPACING, MIN_SPACING, MAX_SPACING, warn)
        val background = bool(section, id, "fundo", true, warn)
        val direction = direction(section, id, anchor, warn)
        val animated = bool(section, id, "animacao", false, warn)
        if (animated && y < 0) {
            warn.accept("região '$id': região animada precisa de y >= 0; ignorada")
            return null
        }

        val draft = NotificationRegion(id, anchor, x, y, lines, spacing, background, direction, animated)
        val fitting = LineGeometry.fittingLines(draft)
        if (fitting < 1) {
            warn.accept("região '$id': y $y sobe mais que o shader alcança; ignorada")
            return null
        }
        if (fitting < lines) {
            warn.accept("região '$id': só $fitting de $lines linhas cabem no alcance do shader; linhas reduzidas")
        }
        return draft.copy(maxLines = fitting)
    }

    private fun direction(section: ConfigurationSection, id: String, anchor: ScreenAnchor, warn: Consumer<String>): StackDirection {
        val fallback = if (anchor.isBottom) StackDirection.UP else StackDirection.DOWN
        val raw = section.get("direcao") ?: return fallback
        val parsed = DIRECTIONS[raw.toString().trim().lowercase()]
        if (parsed == null) {
            warn.accept("região '$id': direcao '$raw' inválida (use baixo ou cima); usando ${name(fallback)}")
            return fallback
        }
        return parsed
    }

    private fun name(direction: StackDirection): String = DIRECTIONS.entries.first { it.value == direction }.key

    private fun int(section: ConfigurationSection, id: String, key: String, fallback: Int, min: Int, max: Int, warn: Consumer<String>): Int {
        val raw = section.get(key) ?: return fallback
        val value = (raw as? Number)?.takeIf { it.toDouble() == it.toInt().toDouble() }?.toInt()
            ?: raw.toString().trim().toIntOrNull()
        if (value == null) {
            warn.accept("região '$id': $key '$raw' não é um número inteiro; usando $fallback")
            return fallback
        }
        if (value < min || value > max) {
            val clamped = value.coerceIn(min, max)
            warn.accept("região '$id': $key $value fora de $min..$max; usando $clamped")
            return clamped
        }
        return value
    }

    private fun bool(section: ConfigurationSection, id: String, key: String, fallback: Boolean, warn: Consumer<String>): Boolean {
        val raw = section.get(key) ?: return fallback
        if (raw is Boolean) return raw
        return when (raw.toString().trim().lowercase()) {
            "true", "sim" -> true
            "false", "nao", "não" -> false
            else -> {
                warn.accept("região '$id': $key '$raw' não é true/false; usando $fallback")
                fallback
            }
        }
    }
}
