package ocean_block.system.machines

object ItemLabels {

    fun icon(material: String): String = when {
        material == "DRIED_KELP_BLOCK" -> "dried_kelp"
        material == "SMOOTH_QUARTZ" -> "quartz_block_bottom"
        material == "SMOOTH_SANDSTONE" -> "sandstone_top"
        material == "SMOOTH_RED_SANDSTONE" -> "red_sandstone_top"
        material.endsWith("_WOOD") -> material.removeSuffix("_WOOD").lowercase() + "_log"
        else -> material.lowercase()
    }
}
