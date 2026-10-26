package ocean_block.system.custom_items.content.paxel

object PaxelRecipeRules {

    val SHAPE: List<String> = listOf("APS", " T ", " T ")

    fun ingredients(pickaxe: String): Map<Char, String> {
        val prefix = pickaxe.removeSuffix("_PICKAXE")
        return mapOf(
            'A' to "${prefix}_AXE",
            'P' to "${prefix}_PICKAXE",
            'S' to "${prefix}_SHOVEL",
            'T' to "STICK",
        )
    }

    fun allowsMatrix(customIds: List<String?>): Boolean = customIds.all { it == null }

    fun key(paxelId: String): String = "paxel_$paxelId"
}
