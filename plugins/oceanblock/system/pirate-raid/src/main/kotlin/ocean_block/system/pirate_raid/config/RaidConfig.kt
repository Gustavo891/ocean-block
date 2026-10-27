package ocean_block.system.pirate_raid.config

import ocean_block.system.pirate_raid.rules.Band
import ocean_block.system.pirate_raid.rules.LootEntry

data class SuperPirateConfig(val health: Double, val rolls: Int, val coins: Int)

data class RaidConfig(
    val chance: Double,
    val intervalTicks: Long,
    val minLevel: Int,
    val durationSeconds: Int,
    val emptySeconds: Int,
    val leashDistance: Double,
    val boatDistance: Double,
    val boatMinDistance: Double,
    val boatSpeed: Double,
    val landingDistance: Double,
    val coinMin: Int,
    val coinMax: Int,
    val levelFactor: Double,
    val lootDespawnSeconds: Int,
    val victoryCoins: Int,
    val victoryXpFraction: Double,
    val bruteRolls: Int,
    val superPirate: SuperPirateConfig,
    val bands: List<Band>,
    val drops: List<LootEntry>,
) {
    companion object {
        val DEFAULT = RaidConfig(
            chance = 0.03,
            intervalTicks = 12_000,
            minLevel = 10,
            durationSeconds = 300,
            emptySeconds = 30,
            leashDistance = 64.0,
            boatDistance = 32.0,
            boatMinDistance = 16.0,
            boatSpeed = 0.25,
            landingDistance = 8.0,
            coinMin = 150,
            coinMax = 400,
            levelFactor = 0.05,
            lootDespawnSeconds = 120,
            victoryCoins = 3000,
            victoryXpFraction = 0.2,
            bruteRolls = 2,
            superPirate = SuperPirateConfig(health = 80.0, rolls = 3, coins = 5),
            bands = listOf(
                Band(10, listOf(3, 4, 5), 0.0),
                Band(20, listOf(4, 5, 6), 0.25),
                Band(30, listOf(5, 6, 8), 0.35),
                Band(40, listOf(6, 8, 10), 0.5),
            ),
            drops = listOf(
                LootEntry("moeda", 1.0, 1, 1),
                LootEntry("perola", 0.2, 1, 2),
                LootEntry("DIAMOND", 0.1, 1, 1),
                LootEntry("GOLDEN_APPLE", 0.08, 1, 1),
                LootEntry("IRON_INGOT", 0.3, 1, 3),
                LootEntry("GOLD_INGOT", 0.15, 1, 2),
                LootEntry("EMERALD", 0.1, 1, 2),
            ),
        )
    }
}
