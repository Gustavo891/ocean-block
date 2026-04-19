package one_block.system.minions.behavior

import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.LeatherArmorMeta

class FarmerBehavior : MinionBehavior {

    override fun spawn(placedMinion: PlacedMinion, location: Location) {
        val armorStand = spawnArmorStand(location) {
            setItem(EquipmentSlot.HEAD, itemStack(Material.HAY_BLOCK) {})
            setItem(EquipmentSlot.CHEST, itemStack(Material.LEATHER_CHESTPLATE) {
                meta<LeatherArmorMeta> { setColor(Color.BLUE) }
            })
            setItem(EquipmentSlot.LEGS, itemStack(Material.LEATHER_LEGGINGS) {
                meta<LeatherArmorMeta> { setColor(Color.BLUE) }
            })
            setItem(EquipmentSlot.FEET, itemStack(Material.LEATHER_BOOTS) {
                meta<LeatherArmorMeta> { setColor(Color.BLUE) }
            })
            setItem(EquipmentSlot.HAND, itemStack(Material.DIAMOND_HOE) {})
        }
        PlacedMinionManager.register(placedMinion, armorStand, spawnDisplay(placedMinion, armorStand))
    }

    override fun tick(placedMinion: PlacedMinion): Boolean {
        val center = placedMinion.location.toBukkit() ?: return false
        val world = center.world
        val tool = placedMinion.getTool()
        val farmY = center.blockY - 1

        for (x in -4..4) {
            for (z in -4..4) {
                val block = world.getBlockAt(center.blockX + x, farmY, center.blockZ + z)

                when (block.type) {
                    Material.SUGAR_CANE,
                    Material.BAMBOO,
                    Material.CACTUS -> {
                        val above = block.location.clone().add(0.0, 1.0, 0.0).block
                        if (above.type == block.type) {
                            var current = above
                            while (current.type == block.type) {
                                val drops = if (tool != null) current.getDrops(tool) else current.getDrops()
                                current.type = Material.AIR
                                drops.forEach { world.dropItemNaturally(current.location, it) }
                                current = current.location.clone().add(0.0, 1.0, 0.0).block
                            }
                        }
                    }

                    Material.MELON,
                    Material.PUMPKIN -> {
                        val drops = if (tool != null) block.getDrops(tool) else block.getDrops()
                        block.type = Material.AIR
                        drops.forEach { world.dropItemNaturally(block.location, it) }
                    }

                    Material.WHEAT,
                    Material.CARROTS,
                    Material.POTATOES,
                    Material.BEETROOTS,
                    Material.NETHER_WART,
                    Material.COCOA,
                    Material.SWEET_BERRY_BUSH -> {
                        val ageable = block.blockData as? Ageable ?: continue
                        if (ageable.age == ageable.maximumAge) {
                            val drops = if (tool != null) block.getDrops(tool) else block.getDrops()
                            drops.forEach { world.dropItemNaturally(block.location, it) }
                            block.type = Material.AIR
                        }
                    }

                    else -> {}
                }
            }
        }

        return true
    }

    override fun spawnDisplay(placedMinion: PlacedMinion, armorStand: ArmorStand): TextDisplay {
        return spawnDisplay(armorStand.location.clone().add(0.0, 1.2, 0.0)) {
            text(MiniMessage.miniMessage().deserialize(
                "<sprite:items:item/diamond_pickaxe> <white>${placedMinion.type.prettyName}"
            ))
        }
    }

    override fun updateDisplay(placedMinion: PlacedMinion) {
    }

}
