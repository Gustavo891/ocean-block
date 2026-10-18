package ocean_block.system.custom_items.block

import ocean_block.libs.instance.Instance
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CustomBlockRegistry {

    private val blocks = mutableMapOf<String, CustomBlock>()

    fun register(block: CustomBlock) {
        if (blocks.containsKey(block.id)) {
            Instance.logger.warning("[custom-items] id de bloco duplicado ignorado: ${block.id}")
            return
        }
        blocks[block.id] = block
    }

    fun get(id: String): CustomBlock? = blocks[id]

    fun all(): Collection<CustomBlock> = blocks.values

    fun ids(): Set<String> = blocks.keys.toSet()

    fun fromItemStack(item: ItemStack?): CustomBlock? {
        if (item == null || item.type.isAir) return null
        val meta = item.itemMeta ?: return null
        val id = meta.persistentDataContainer.get(CustomBlockKeys.ID, PersistentDataType.STRING) ?: return null
        return blocks[id]
    }
}
