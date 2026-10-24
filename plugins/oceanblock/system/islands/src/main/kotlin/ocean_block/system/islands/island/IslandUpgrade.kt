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

            player.sendRichMessage("<green>Membros evoluído com sucesso! <dark_gray>(${(size + 1) * Island.MEMBERS_PER_LEVEL} -> ${(size + 2) * Island.MEMBERS_PER_LEVEL} membros)")
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
    },
    Generator("Gerador",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==",
        4,
        listOf("O gerador de pedra da ilha passa", "a formar minérios melhores.")) {
        override fun execute(player: Player, island: Island) {
            val level = island.getUpgradeLevel(this) + 1
            island.setUpgradeLevel(this, level)

            player.sendRichMessage("<green>Gerador evoluído com sucesso! <dark_gray>(nível ${level - 1} -> $level)")
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

            Generator -> when (level) {
                0 -> 25000.0
                1 -> 100000.0
                2 -> 400000.0
                3 -> 1500000.0
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
