package ocean_block.system.islands.island

import org.bukkit.entity.Player

enum class IslandUpgrade(
    val prettyName: String,
    val texture: String,
    val maxLevel: Int,
    val lore: List<String>,
) {

    Members("Membros",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmZlOGRiYTZiOWE4ZTc3ZjA1YmQyNjJiYmU2MTAyZGY4NGNmN2YxMDZhYTQwY2QwOTc1MzQ2MjFmY2E1YTk5MSJ9fX0=",
        1,
        listOf("Aumente a quantidade de jogadores", "que você pode adicionar.")) {
        override fun execute(player: Player, island: Island) {
            val size = island.getUpgradeLevel(this)
            island.setUpgradeLevel(this, size + 1)

            player.sendRichMessage("<green>Membros evoluído com sucesso! <dark_gray>(${Island.BORDER_SIZE[size]} -> ${Island.BORDER_SIZE[size + 1]})")
        }
    },
    Size("Borda",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==",
        1,
        listOf("Aumente o tamanho da borda")) {
        override fun execute(player: Player, island: Island) {
            val size = island.getUpgradeLevel(this)

            island.upgradeBorderSize()
            player.sendRichMessage("<green>Borda evoluída com sucesso! <dark_gray>(${Island.BORDER_SIZE[size]} -> ${Island.BORDER_SIZE[size + 1]})")
        }
    },
    CropGrowth("Plantação",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTY3MGJhNmNiY2Q2MGNjYTI5MDZiMGM0YWJiNzE2ODIwODUxZDVjMjhhNDJlYmY5NTQ3YTE1ZTc2MTE3NjhkMSJ9fX0=",
        0,
        listOf("Aumente a velocidade que as", "plantações crescem.")) {
        override fun execute(player: Player, island: Island) {
        }
    };

    abstract fun execute(player: Player, island: Island)

    fun getPrice(level: Int): Double {
        return when (this) {
            Members -> when (level) {
                0 -> 50000.0
                else -> Double.MAX_VALUE
            }

            Size -> when (level) {
                0 -> 50000.0
                else -> Double.MAX_VALUE
            }

            CropGrowth -> when (level) {
                0 -> 5000000.0
                else -> Double.MAX_VALUE
            }
        }
    }

    companion object {
        fun getPage(page: Int): List<IslandUpgrade> {
            val index = page * 3
            return entries.drop(index).take(3)
        }
    }


}
