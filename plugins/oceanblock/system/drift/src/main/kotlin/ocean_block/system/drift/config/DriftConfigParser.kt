package ocean_block.system.drift.config

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

object DriftConfigParser {

    private const val DEFAULT_SCALE = 0.6

    fun parse(root: ConfigurationSection, warn: (String) -> Unit): DriftConfig? {
        val types = parseTypes(root.getConfigurationSection("detritos"), warn)
        if (types.isEmpty()) return null
        return DriftConfig(parseSettings(root, warn), types)
    }

    private fun parseSettings(root: ConfigurationSection, warn: (String) -> Unit): DriftSettings {
        val current = root.getConfigurationSection("corrente")
        val spawn = root.getConfigurationSection("spawn")
        val pickup = root.getConfigurationSection("coleta")

        return DriftSettings(
            direction = parseDirection(current?.getString("direcao"), warn),
            speed = (current?.getDouble("velocidade", 0.05) ?: 0.05).coerceAtLeast(0.001),
            laneLength = (current?.getDouble("comprimento", 80.0) ?: 80.0).coerceAtLeast(1.0),
            laneWidth = (current?.getDouble("largura", 10.0) ?: 10.0).coerceAtLeast(0.0),
            surfaceY = current?.getDouble("altura", 62.6) ?: 62.6,
            spawnIntervalTicks = (spawn?.getLong("intervalo-ticks", 200L) ?: 200L).coerceAtLeast(1L),
            maxPerLot = (spawn?.getInt("maximo-por-lote", 8) ?: 8).coerceAtLeast(1),
            stuckSeconds = (spawn?.getInt("tempo-preso-segundos", 60) ?: 60).coerceAtLeast(1),
            hookRadius = (pickup?.getDouble("raio-gancho", 2.0) ?: 2.0).coerceAtLeast(0.1),
            touchRadius = (pickup?.getDouble("raio-toque", 1.5) ?: 1.5).coerceAtLeast(0.1),
        )
    }

    private fun parseDirection(name: String?, warn: (String) -> Unit): CurrentDirection {
        if (name == null) return CurrentDirection.EAST
        val direction = CurrentDirection.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (direction == null) warn("[deriva] direcao invalida '$name'; usando EAST.")
        return direction ?: CurrentDirection.EAST
    }

    private fun parseTypes(section: ConfigurationSection?, warn: (String) -> Unit): List<DebrisType> =
        section?.getKeys(false)?.mapNotNull { id ->
            section.getConfigurationSection(id)?.let { parseType(id, it, warn) }
        } ?: emptyList()

    private fun parseType(id: String, section: ConfigurationSection, warn: (String) -> Unit): DebrisType? {
        val weight = section.getInt("peso", 0)
        if (weight <= 0) {
            warn("[deriva] detrito '$id' ignorado: peso invalido.")
            return null
        }

        val display = parseMaterial(section.getString("exibicao"))
        if (display == null) {
            warn("[deriva] detrito '$id' ignorado: exibicao invalida '${section.getString("exibicao")}'.")
            return null
        }

        val drops = section.getMapList("drops").mapNotNull { parseDrop(id, it, warn) }
        val scale = section.getDouble("escala", DEFAULT_SCALE).coerceAtLeast(0.05).toFloat()
        return DebrisType(id, weight, display, scale, drops)
    }

    private fun parseDrop(typeId: String, raw: Map<*, *>, warn: (String) -> Unit): DropEntry? {
        val material = parseMaterial(raw["material"]?.toString())
        if (material == null) {
            warn("[deriva] drop de '$typeId' ignorado: material invalido '${raw["material"]}'.")
            return null
        }

        val min = (raw["min"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1
        val max = ((raw["max"] as? Number)?.toInt() ?: min).coerceAtLeast(min)
        val chance = ((raw["chance"] as? Number)?.toDouble() ?: 1.0).coerceIn(0.0, 1.0)
        return DropEntry(material, min, max, chance)
    }

    private fun parseMaterial(name: String?): Material? =
        name?.let { Material.getMaterial(it.trim().uppercase()) }?.takeUnless { it.isLegacy }
}
