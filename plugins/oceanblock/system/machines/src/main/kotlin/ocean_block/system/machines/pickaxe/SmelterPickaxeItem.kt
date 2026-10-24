package ocean_block.system.machines.pickaxe

import ocean_block.system.custom_items.item.CustomItem
import ocean_block.system.custom_items.item.CustomItemRegistry
import ocean_block.system.machines.MachineIds
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent

object SmelterPickaxeItem : CustomItem() {

    override val id: String = MachineIds.PICKAXE
    override val material: Material = Material.IRON_PICKAXE
    override val itemModel: NamespacedKey = NamespacedKey("oneblock", MachineIds.PICKAXE)
    override val displayName: String = "<gold>Picareta fundidora"
    override val lore: List<String> = listOf(
        "<gray>Minério de ferro, cobre e ouro",
        "<gray>já sai em lingote.",
        "<gray>Pedregulho sai como pedra.",
    )

    override fun listeners(): List<Listener> = listOf(DropListener)

    private object DropListener : Listener {

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        fun onDrop(event: BlockDropItemEvent) {
            val tool = event.player.inventory.itemInMainHand
            if (CustomItemRegistry.fromItemStack(tool) !== SmelterPickaxeItem) return
            var smelted = false
            event.items.forEach { entity ->
                val stack = entity.itemStack
                val result = AutoSmelt.smelt(stack.type.name)?.let(Material::matchMaterial) ?: return@forEach
                entity.itemStack = stack.withType(result)
                smelted = true
            }
            if (!smelted) return
            val center = event.block.location.add(0.5, 0.5, 0.5)
            event.block.world.spawnParticle(Particle.FLAME, center, 6, 0.25, 0.25, 0.25, 0.01)
            event.block.world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 1.6f)
        }
    }
}
