package ocean_block.system.level.config

data class LevelConfig(
    val debrisDefault: Int,
    val debris: Map<String, Int>,
    val generator: Map<String, Int>,
    val recipeBase: Int,
    val recipePerLevel: Int,
    val mobDefault: Int,
    val mobs: Map<String, Int>,
) {

    fun recipeXp(level: Int): Int = recipeBase + recipePerLevel * level

    fun debrisXp(typeId: String): Int = debris[typeId] ?: debrisDefault

    fun generatorXp(material: String): Int = generator[material] ?: 0

    fun mobXp(entityType: String): Int = mobs[entityType.uppercase()] ?: mobDefault

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
            mobDefault = 1,
            mobs = mapOf(
                "ZOMBIE" to 3, "HUSK" to 3, "DROWNED" to 3, "ZOMBIE_VILLAGER" to 3, "SKELETON" to 3, "STRAY" to 3, "BOGGED" to 3,
                "SPIDER" to 3, "SLIME" to 3, "MAGMA_CUBE" to 3, "SILVERFISH" to 3, "ENDERMITE" to 3, "VEX" to 3,
                "ZOMBIFIED_PIGLIN" to 3, "PIGLIN" to 3, "CAVE_SPIDER" to 4, "PHANTOM" to 4,
                "CREEPER" to 5, "WITCH" to 5, "PILLAGER" to 5, "VINDICATOR" to 5, "BLAZE" to 5, "GHAST" to 5, "BREEZE" to 5,
                "HOGLIN" to 5, "ZOGLIN" to 5, "SHULKER" to 5, "GUARDIAN" to 6, "PIGLIN_BRUTE" to 6,
                "ENDERMAN" to 8, "WITHER_SKELETON" to 8, "EVOKER" to 8,
                "ELDER_GUARDIAN" to 10, "RAVAGER" to 10, "WARDEN" to 10, "WITHER" to 10, "ENDER_DRAGON" to 10,
            ),
        )
    }
}
