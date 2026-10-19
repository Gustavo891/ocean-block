package ocean_block.system.custom_items.content.paxel

import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Material
import org.bukkit.block.BlockType

enum class PaxelTier(
    val id: String,
    val displayName: String,
    val tierName: String,
    val material: Material,
    val miningSpeed: Float,
    val incorrectTag: TagKey<BlockType>,
    val attackDamage: Double,
    val axe: Material,
    val shovel: Material,
    val hoe: Material,
    val sword: Material,
) {
    Iron(
        id = "iron_paxel",
        displayName = "<white>Paxel de Ferro",
        tierName = "de Ferro",
        material = Material.IRON_PICKAXE,
        miningSpeed = 6f,
        incorrectTag = BlockTypeTagKeys.INCORRECT_FOR_IRON_TOOL,
        attackDamage = 5.0,
        axe = Material.IRON_AXE,
        shovel = Material.IRON_SHOVEL,
        hoe = Material.IRON_HOE,
        sword = Material.IRON_SWORD,
    ),
    Diamond(
        id = "diamond_paxel",
        displayName = "<aqua>Paxel de Diamante",
        tierName = "de Diamante",
        material = Material.DIAMOND_PICKAXE,
        miningSpeed = 8f,
        incorrectTag = BlockTypeTagKeys.INCORRECT_FOR_DIAMOND_TOOL,
        attackDamage = 6.0,
        axe = Material.DIAMOND_AXE,
        shovel = Material.DIAMOND_SHOVEL,
        hoe = Material.DIAMOND_HOE,
        sword = Material.DIAMOND_SWORD,
    ),
    Netherite(
        id = "netherite_paxel",
        displayName = "<dark_gray>Paxel de Netherita",
        tierName = "de Netherita",
        material = Material.NETHERITE_PICKAXE,
        miningSpeed = 9f,
        incorrectTag = BlockTypeTagKeys.INCORRECT_FOR_NETHERITE_TOOL,
        attackDamage = 7.0,
        axe = Material.NETHERITE_AXE,
        shovel = Material.NETHERITE_SHOVEL,
        hoe = Material.NETHERITE_HOE,
        sword = Material.NETHERITE_SWORD,
    ),
    ;

    val attackSpeed: Double get() = SWORD_ATTACK_SPEED

    companion object {
        const val SWORD_ATTACK_SPEED = -2.4
    }
}
