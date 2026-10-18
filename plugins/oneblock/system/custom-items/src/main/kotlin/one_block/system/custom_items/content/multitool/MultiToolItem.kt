package one_block.system.custom_items.content.multitool

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.util.TriState
import one_block.libs.item_builder.meta
import one_block.system.custom_items.item.CustomItem
import org.bukkit.Material
import org.bukkit.block.BlockType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class MultiToolItem : CustomItem() {

    override val id = "multitool"
    override val material = Material.NETHERITE_PICKAXE
    override val displayName = "<gradient:#f7971e:#ffd200>MultiTool</gradient>"

    override fun build(amount: Int): ItemStack {
        val item = super.build(amount)

        item.meta<ItemMeta> { isUnbreakable = true }
        item.setData(DataComponentTypes.TOOL, toolComponent())

        return item
    }

    private fun toolComponent(): Tool {
        val blocks = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK)
        val builder = Tool.tool()
            .defaultMiningSpeed(1f)
            .damagePerBlock(0)

        MINEABLE_TAGS.forEach { tag ->
            builder.addRule(Tool.rule(blocks.getTag(tag), MINING_SPEED, TriState.TRUE))
        }

        return builder.build()
    }

    private companion object {
        const val MINING_SPEED = 9f

        val MINEABLE_TAGS: List<TagKey<BlockType>> = listOf(
            BlockTypeTagKeys.MINEABLE_PICKAXE,
            BlockTypeTagKeys.MINEABLE_AXE,
            BlockTypeTagKeys.MINEABLE_SHOVEL,
            BlockTypeTagKeys.MINEABLE_HOE,
        )
    }
}
