package one_block.system.mobdrops

import one_block.system.mobdrops.manager.MobDrop
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

object MobDrops {

    private val extraDrops = mutableMapOf<EntityType, MutableList<MobDrop>>()

    fun addDrop(type: EntityType, item: () -> ItemStack, chance: Double) {
        extraDrops
            .computeIfAbsent(type) { mutableListOf() }
            .add(MobDrop(item, chance))
    }

    fun getDrops(type: EntityType): List<MobDrop> {
        return extraDrops[type] ?: emptyList()
    }
}
