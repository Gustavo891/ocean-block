package ocean_block.system.custom_items.content.paxel

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.BlockTypeKeys
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.set.RegistrySet
import io.papermc.paper.registry.tag.TagKey
import net.kyori.adventure.util.TriState
import ocean_block.system.craft.CraftCategory
import ocean_block.system.craft.forge.ForgeRecipe
import ocean_block.system.craft.forge.ForgeRequirement
import ocean_block.system.custom_items.item.CustomItem
import ocean_block.system.custom_items.item.CustomItemRegistry
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.BlockType
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack

class PaxelItem(private val tier: PaxelTier) : CustomItem() {

    override val id = tier.id
    override val material = tier.material
    override val itemModel = NamespacedKey("oneblock", tier.id)
    override val displayName = tier.displayName
    override val lore = listOf(
        "<dark_gray>Picareta, machado, pá, enxada e espada em um só.",
    )
    override val heldTickInterval = 0L

    override fun build(amount: Int): ItemStack {
        val stack = super.build(amount)
        stack.setData(DataComponentTypes.TOOL, toolComponent())
        applyCombatAttributes(stack)
        return stack
    }

    fun forgeRecipe(): ForgeRecipe = ForgeRecipe(
        id = tier.id,
        prettyName = tier.displayName,
        icon = build(),
        result = build(),
        requirements = tier.craftTools.map { (name, material) ->
            ForgeRequirement(
                prettyName = name,
                item = ItemStack(material),
                amount = 1,
                matcher = { it.type == material && CustomItemRegistry.fromItemStack(it) == null },
            )
        },
        category = CraftCategory.MINERACAO,
    )

    private fun applyCombatAttributes(stack: ItemStack) {
        stack.editMeta { meta ->
            meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                AttributeModifier(
                    NamespacedKey("oneblock", "${tier.id}_damage"),
                    tier.attackDamage,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND,
                )
            )

            meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                AttributeModifier(
                    NamespacedKey("oneblock", "${tier.id}_speed"),
                    tier.attackSpeed,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND,
                )
            )
        }
    }

    private fun toolComponent(): Tool {
        val blocks = RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK)

        val builder = Tool.tool()
            .defaultMiningSpeed(1f)
            .damagePerBlock(1)

        builder.addRule(Tool.rule(blocks.getTag(tier.incorrectTag), tier.miningSpeed, TriState.FALSE))

        builder.addRule(Tool.rule(INSTANT_BLOCKS, INSTANT_SPEED, TriState.TRUE))

        builder.addRule(
            Tool.rule(blocks.getTag(BlockTypeTagKeys.SWORD_INSTANTLY_MINES), INSTANT_SPEED, TriState.TRUE)
        )

        MINEABLE_TAGS.forEach { tag ->
            builder.addRule(Tool.rule(blocks.getTag(tag), tier.miningSpeed, TriState.TRUE))
        }

        return builder.build()
    }

    companion object {
        private const val INSTANT_SPEED = 1000f

        private val INSTANT_BLOCKS: RegistryKeySet<BlockType> = RegistrySet.keySet(
            RegistryKey.BLOCK,
            BlockTypeKeys.COBWEB,
        )

        private val MINEABLE_TAGS: List<TagKey<BlockType>> = listOf(
            BlockTypeTagKeys.MINEABLE_PICKAXE,
            BlockTypeTagKeys.MINEABLE_AXE,
            BlockTypeTagKeys.MINEABLE_SHOVEL,
            BlockTypeTagKeys.MINEABLE_HOE,
            BlockTypeTagKeys.SWORD_EFFICIENT,
        )

        fun all(): List<PaxelItem> = PaxelTier.entries.map { PaxelItem(it) }
    }
}
