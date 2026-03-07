package one_block.system.islands.island

import org.bukkit.Material
import org.bukkit.entity.Player

enum class IslandUpgrade(
    val prettyName: String,
    val iconMaterial: Material,
    val maxLevel: Int,
) {
    Members("Membros", Material.PLAYER_HEAD, 4) {
        override fun execute(player: Player, island: Island) {
            val size = island.getUpgradeLevel(this)
            island.setUpgradeLevel(this, size + 1)

            player.sendRichMessage("<green>Membros evoluído com sucesso! <dark_gray>(${Island.BORDER_SIZE[size]} -> ${Island.BORDER_SIZE[size + 1]})")
        }
    },
    Size("Borda", Material.CLOCK, 3) {
        override fun execute(player: Player, island: Island) {
            val size = island.getUpgradeLevel(this)

            island.upgradeBorderSize()
            player.sendRichMessage("<green>Borda evoluída com sucesso! <dark_gray>(${Island.BORDER_SIZE[size]} -> ${Island.BORDER_SIZE[size + 1]})")
        }
    },
    CropGrowth("Plantação", Material.WHEAT_SEEDS, 5) {
        override fun execute(player: Player, island: Island) {
        }
    };

    abstract fun execute(player: Player, island: Island)

    fun getPrice(level: Int): Double {
        return when (this) {
            Members -> when (level) {
                0 -> 50000.0
              /*  1 -> 20_000.0
                2 -> 30_000.0
                3 -> 40_000.0
                4 -> 50_000.0
                5 -> 60_000.0*/
                else -> Double.MAX_VALUE
            }

            Size -> when (level) {
                0 -> 50000.0
                /*             1 -> 20_000.0
                             2 -> 30_000.0
                             3 -> 40_000.0
                             4 -> 50_000.0
                             5 -> 60_000.0*/
                else -> Double.MAX_VALUE
            }

            CropGrowth -> when (level) {
                   0 -> 500000.0
                /*    1 -> 20_000.0
                    2 -> 30_000.0
                    3 -> 40_000.0
                    4 -> 50_000.0
                    5 -> 60_000.0*/
                else -> Double.MAX_VALUE
            }
        }
    }

}