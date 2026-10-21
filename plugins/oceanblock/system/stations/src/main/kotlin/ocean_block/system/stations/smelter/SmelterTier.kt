package ocean_block.system.stations.smelter

enum class SmelterTier(val blockId: String, val ticksPerIngot: Int, val bonusChance: Double, val hoppers: Boolean) {
    BASIC("fundicao", 200, 0.0, false),
    IMPROVED("fundicao_melhorada", 100, 0.25, false),
    HOPPER("fundicao_funil", 100, 0.25, true),
}
