package one_block.system.minions.behavior

import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.item_builder.itemStack
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Mob
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.EquipmentSlot

class TerminatorBehavior : MinionBehavior {

    override fun spawn(placedMinion: PlacedMinion, location: Location) {
        val armorStand = spawnArmorStand(location) {
            setItem(EquipmentSlot.HEAD, itemStack(Material.NETHERITE_HELMET) {})
            setItem(EquipmentSlot.CHEST, itemStack(Material.NETHERITE_CHESTPLATE) {})
            setItem(EquipmentSlot.LEGS, itemStack(Material.NETHERITE_LEGGINGS) {})
            setItem(EquipmentSlot.FEET, itemStack(Material.NETHERITE_BOOTS) {})
            setItem(EquipmentSlot.HAND, itemStack(Material.DIAMOND_SWORD) {})
        }

        PlacedMinionManager.register(placedMinion, armorStand, spawnDisplay(placedMinion, armorStand))
    }

    override fun tick(placedMinion: PlacedMinion): Boolean {
        val target = placedMinion.location.toBukkit()!!
            .getNearbyLivingEntities(5.0)
            .filterIsInstance<Mob>()
            .firstOrNull()

        val tool = placedMinion.getTool()
        val damage = if (tool != null) {
            val sharpness = tool.getEnchantmentLevel(Enchantment.SHARPNESS)
            4.0 + sharpness * 1.5
        } else 4.0

        target?.damage(damage)
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
