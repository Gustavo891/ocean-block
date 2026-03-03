package one_block.system.minions.enums

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand

enum class MinionType {
    Terminator{
        override fun spawn(location: Location): ArmorStand {
            return location.world.spawn(location, ArmorStand::class.java).apply {
                isInvulnerable = true
                setGravity(false)
                setMarker(false)
                setSmall(true)
                setArms(true)

                setHelmet(itemStack(Material.NETHERITE_HELMET))
                setChestplate(itemStack(Material.NETHERITE_CHESTPLATE))
                setLeggings(itemStack(Material.NETHERITE_LEGGINGS))
                setBoots(itemStack(Material.NETHERITE_BOOTS))
            }
        }

        override fun executeAction() {
        }
    };

    abstract fun spawn(location: Location): ArmorStand

    abstract fun executeAction()
}