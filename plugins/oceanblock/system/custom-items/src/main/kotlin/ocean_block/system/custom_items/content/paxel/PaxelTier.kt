package ocean_block.system.custom_items.content.paxel

import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Material
import org.bukkit.block.BlockType

enum class PaxelTier(
    val id: String,
    val displayName: String,
    val material: Material,
    val miningSpeed: Float,
    val incorrectTag: TagKey<BlockType>,
    val attackDamage: Double,
) {
    Iron(
        id = "iron_paxel",
        displayName = "<white>Paxel de Ferro",
        material = Material.IRON_PICKAXE,
        miningSpeed = 6f,
        incorrectTag = BlockTypeTagKeys.INCORRECT_FOR_IRON_TOOL,
        attackDamage = 5.0,
    ),
    Diamond(
        id = "diamond_paxel",
        displayName = "<aqua>Paxel de Diamante",
        material = Material.DIAMOND_PICKAXE,
        miningSpeed = 8f,
        incorrectTag = BlockTypeTagKeys.INCORRECT_FOR_DIAMOND_TOOL,
        attackDamage = 6.0,
    ),
    Netherite(
        id = "netherite_paxel",
        displayName = "<dark_gray>Paxel de Netherita",
        material = Material.NETHERITE_PICKAXE,
        miningSpeed = 9f,
        incorrectTag = BlockTypeTagKeys.INCORRECT_FOR_NETHERITE_TOOL,
        attackDamage = 7.0,
    ),
    ;

    val attackSpeed: Double get() = SWORD_ATTACK_SPEED

    companion object {
        const val SWORD_ATTACK_SPEED = -2.4
    }
}
