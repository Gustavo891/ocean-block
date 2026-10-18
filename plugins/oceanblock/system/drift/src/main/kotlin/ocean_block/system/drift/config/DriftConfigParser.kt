package ocean_block.system.drift.config

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

object DriftConfigParser {

    private const val DEFAULT_SCALE = 0.6
    private const val DEFAULT_MAX_STACK = 64

    private class MaterialRules(val isItem: (Material) -> Boolean, val maxStack: (Material) -> Int)

    fun parse(
        root: ConfigurationSection,
        warn: (String) -> Unit,
        isItem: (Material) -> Boolean = { true },
        maxStack: (Material) -> Int = { DEFAULT_MAX_STACK },
    ): DriftConfig? {
        val rules = MaterialRules(isItem, maxStack)
        val types = parseTypes(root.getConfigurationSection("detritos"), rules, warn)
        if (types.isEmpty()) return null
        return DriftConfig(parseSettings(root, warn), types)
    }

    private fun parseSettings(root: ConfigurationSection, warn: (String) -> Unit): DriftSettings {
        val current = root.getConfigurationSection("corrente")
        val spawn = root.getConfigurationSection("spawn")
        val pickup = root.getConfigurationSection("coleta")

        return DriftSettings(
            direction = parseDirection(current?.getString("direcao"), warn),
            speed = number(current?.get("velocidade"), "corrente.velocidade", 0.05, 0.001, null, warn),
            laneLength = number(current?.get("comprimento"), "corrente.comprimento", 40.0, 1.0, null, warn),
            laneWidth = number(current?.get("largura"), "corrente.largura", 10.0, 0.0, null, warn),
            surfaceY = number(current?.get("altura"), "corrente.altura", 62.6, null, null, warn),
            spawnIntervalTicks = number(spawn?.get("intervalo-ticks"), "spawn.intervalo-ticks", 100.0, 1.0, null, warn).toLong(),
            maxPerLot = number(spawn?.get("maximo-por-lote"), "spawn.maximo-por-lote", 8.0, 1.0, null, warn).toInt(),
            stuckSeconds = number(spawn?.get("tempo-preso-segundos"), "spawn.tempo-preso-segundos", 60.0, 1.0, null, warn).toInt(),
            hookRadius = number(pickup?.get("raio-gancho"), "coleta.raio-gancho", 2.0, 0.1, null, warn),
            touchRadius = number(pickup?.get("raio-toque"), "coleta.raio-toque", 1.5, 0.1, null, warn),
        )
    }

    private fun parseDirection(name: String?, warn: (String) -> Unit): CurrentDirection {
        if (name == null) return CurrentDirection.EAST
        val direction = CurrentDirection.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (direction == null) warn("[deriva] direcao invalida '$name'; usando EAST.")
        return direction ?: CurrentDirection.EAST
    }

    private fun parseTypes(
        section: ConfigurationSection?,
        rules: MaterialRules,
        warn: (String) -> Unit,
    ): List<DebrisType> =
        section?.getKeys(false)?.mapNotNull { id ->
            section.getConfigurationSection(id)?.let { parseType(id, it, rules, warn) }
        } ?: emptyList()

    private fun parseType(id: String, section: ConfigurationSection, rules: MaterialRules, warn: (String) -> Unit): DebrisType? {
        val weight = section.getInt("peso", 0)
        if (weight <= 0) {
            warn("[deriva] detrito '$id' ignorado: peso invalido.")
            return null
        }

        val display = parseMaterial(section.getString("exibicao"), rules.isItem)
        if (display == null) {
            warn("[deriva] detrito '$id' ignorado: exibicao invalida '${section.getString("exibicao")}'.")
            return null
        }

        val drops = section.getMapList("drops").mapNotNull { parseDrop(id, it, rules, warn) }
        if (drops.isEmpty()) warn("[deriva] detrito '$id' sem drops validos.")

        val scale = number(section.get("escala"), "detritos.$id.escala", DEFAULT_SCALE, 0.05, null, warn).toFloat()
        return DebrisType(id, weight, display, scale, drops)
    }

    private fun parseDrop(typeId: String, raw: Map<*, *>, rules: MaterialRules, warn: (String) -> Unit): DropEntry? {
        val material = parseMaterial(raw["material"]?.toString(), rules.isItem)
        if (material == null) {
            warn("[deriva] drop de '$typeId' ignorado: material invalido '${raw["material"]}'.")
            return null
        }

        val label = "drop $material de '$typeId'"
        val stack = rules.maxStack(material).coerceAtLeast(1)
        val min = number(raw["min"], "$label.min", 1.0, 1.0, stack.toDouble(), warn).toInt()
        val max = number(raw["max"], "$label.max", min.toDouble(), null, stack.toDouble(), warn).toInt()
        val chance = number(raw["chance"], "$label.chance", 1.0, 0.0, 1.0, warn)

        if (max < min) {
            warn("[deriva] $label: max $max menor que min $min; usando $min.")
            return DropEntry(material, min, min, chance)
        }
        return DropEntry(material, min, max, chance)
    }

    private fun number(
        raw: Any?,
        label: String,
        default: Double,
        lower: Double?,
        upper: Double?,
        warn: (String) -> Unit,
    ): Double {
        if (raw == null) return default

        val value = (raw as? Number)?.toDouble() ?: raw.toString().trim().toDoubleOrNull()
        if (value == null || !value.isFinite()) {
            warn("[deriva] $label invalido '$raw'; usando $default.")
            return default
        }
        if (lower != null && value < lower) {
            warn("[deriva] $label abaixo do minimo ($value); usando $lower.")
            return lower
        }
        if (upper != null && value > upper) {
            warn("[deriva] $label acima do maximo ($value); usando $upper.")
            return upper
        }
        return value
    }

    private fun parseMaterial(name: String?, isItem: (Material) -> Boolean): Material? =
        name?.let { Material.getMaterial(it.trim().uppercase()) }?.takeUnless { it.isLegacy || !isItem(it) }
}
