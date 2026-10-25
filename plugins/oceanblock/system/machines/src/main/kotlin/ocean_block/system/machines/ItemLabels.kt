package ocean_block.system.machines

import one_block.textura.api.TranslationApi
import org.bukkit.Material

object ItemLabels {

    fun icon(material: String): String = when {
        material == "DRIED_KELP_BLOCK" -> "dried_kelp"
        material == "SMOOTH_QUARTZ" -> "quartz_block_bottom"
        material == "SMOOTH_SANDSTONE" -> "sandstone_top"
        material == "SMOOTH_RED_SANDSTONE" -> "red_sandstone_top"
        material.endsWith("_WOOD") -> material.removeSuffix("_WOOD").lowercase() + "_log"
        else -> material.lowercase()
    }

    fun name(material: String): String =
        Material.matchMaterial(material)?.let { TranslationApi.translate(it.translationKey()) } ?: fallbackName(material)

    fun fallbackName(material: String): String =
        material.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
