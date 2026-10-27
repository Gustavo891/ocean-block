package ocean_block.system.level.config

data class LevelConfig(
    val debrisDefault: Int,
    val debris: Map<String, Int>,
    val generator: Map<String, Int>,
    val recipeBase: Int,
    val recipePerLevel: Int,
) {

    fun recipeXp(level: Int): Int = recipeBase + recipePerLevel * level

    fun debrisXp(typeId: String): Int = debris[typeId] ?: debrisDefault

    fun generatorXp(material: String): Int = generator[material] ?: 0

    companion object {
        val DEFAULT = LevelConfig(
            debrisDefault = 4,
            debris = mapOf("sucata" to 4, "barril" to 10, "barril_ferro" to 25, "rede_rasgada" to 5, "recife" to 8),
            generator = mapOf(
                "COBBLESTONE" to 1, "STONE" to 1, "COAL_ORE" to 3, "COPPER_ORE" to 4, "IRON_ORE" to 5,
                "REDSTONE_ORE" to 6, "GOLD_ORE" to 8, "LAPIS_ORE" to 8, "EMERALD_ORE" to 15, "DIAMOND_ORE" to 20,
            ),
            recipeBase = 10,
            recipePerLevel = 3,
        )
    }
}
