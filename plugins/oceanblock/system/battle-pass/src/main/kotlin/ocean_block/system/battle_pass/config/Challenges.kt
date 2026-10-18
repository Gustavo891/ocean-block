package ocean_block.system.battle_pass.config

import ocean_block.libs.item_builder.itemStack
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

enum class Challenges(
    val displayName: String,
    val description: List<String>,
    val icon: ItemStack,
    val xpReward: Int,
    val objective: ChallengeObjective,
) {

    BREAK_200(
        displayName = "Minerador",
        description = listOf("Quebre 200 blocos do centro de sua ilha."),
        icon = itemStack(Material.GRASS_BLOCK) {},
        xpReward = 100,
        objective = ChallengeObjective.BreakOneBlock(goal = 200),
    ),
    CRAFT_LEATHER(
        displayName = "Fabricante",
        description = listOf("Fabrique <white>x16 <gray>couros no /craft."),
        icon = itemStack(Material.LEATHER) {},
        xpReward = 200,
        objective = ChallengeObjective.CustomCraft("leather", goal = 16),
    ),
    SELL_5000(
        displayName = "Comerciante",
        description = listOf("Venda itens no total de R$5.000."),
        icon = itemStack(Material.EMERALD) {},
        xpReward = 150,
        objective = ChallengeObjective.SellValue(goal = 5000),
    ),
    SELL_128_IRONS(
        displayName = "Comerciante",
        description = listOf("Venda x128 Ferros Brutos"),
        icon = itemStack(Material.RAW_IRON) {},
        xpReward = 200,
        objective = ChallengeObjective.SellItem(material = Material.RAW_IRON, goal = 128),
    );

    val id: String get() = name

    companion object {
        val ordered: List<Challenges> by lazy { entries.toList() }

        fun byId(id: String?): Challenges? =
            id?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
