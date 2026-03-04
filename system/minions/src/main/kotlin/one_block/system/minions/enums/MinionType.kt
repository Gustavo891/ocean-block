package one_block.system.minions.enums

import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.system.minions.placed_minion.PlacedMinion
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Mob
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta

enum class MinionType {
    Terminator {
        override fun spawn(location: Location): ArmorStand {
            return spawnArmorStand(location) {
                setHelmet(itemStack(Material.NETHERITE_HELMET) { })
                setChestplate(itemStack(Material.NETHERITE_CHESTPLATE) { })
                setLeggings(itemStack(Material.NETHERITE_LEGGINGS) { })
                setBoots(itemStack(Material.NETHERITE_BOOTS) { })
            }
        }

        override fun executeAction(placedMinion: PlacedMinion) {
            val target = placedMinion.location.toBukkit()!!
                .getNearbyLivingEntities(5.0)
                .filterIsInstance<Mob>()
                .firstOrNull()

            target?.damage(4.0)
        }
    },
    Farmer {
        override fun spawn(location: Location): ArmorStand {
            return spawnArmorStand(location) {
                setHelmet(itemStack(Material.HAY_BLOCK) {
                })

                setChestplate(itemStack(Material.LEATHER_CHESTPLATE) {
                    meta<LeatherArmorMeta> {
                        setColor(Color.BLUE)
                    }
                })

                setLeggings(itemStack(Material.LEATHER_LEGGINGS) {
                    meta<LeatherArmorMeta> {
                        setColor(Color.BLUE)
                    }
                })

                setBoots(itemStack(Material.LEATHER_BOOTS) {
                    meta<LeatherArmorMeta> {
                        setColor(Color.BLUE)
                    }
                })
            }
        }

        override fun executeAction(placedMinion: PlacedMinion) {

            val center = placedMinion.location.toBukkit() ?: return
            val world = center.world

            val farmY = center.blockY - 1

            for (x in -4..4) {
                for (z in -4..4) {

                    val block = world.getBlockAt(
                        center.blockX + x,
                        farmY,
                        center.blockZ + z
                    )

                    when (block.type) {

                        Material.SUGAR_CANE,
                        Material.BAMBOO,
                        Material.CACTUS -> {

                            val above = block.location.clone().add(0.0, 1.0, 0.0).block
                            if (above.type == block.type) {

                                var current = above

                                while (current.type == block.type) {

                                    val drops = current.getDrops()
                                    current.type = Material.AIR

                                    drops.forEach {
                                        world.dropItemNaturally(current.location, it)
                                    }

                                    current = current.location.clone()
                                        .add(0.0, 1.0, 0.0).block
                                }
                            }
                        }

                        Material.MELON,
                        Material.PUMPKIN -> {

                            val drops = block.getDrops()
                            block.type = Material.AIR

                            drops.forEach {
                                world.dropItemNaturally(block.location, it)
                            }
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

                                val drops = block.getDrops()

                                drops.forEach {
                                    world.dropItemNaturally(block.location, it)
                                }

                                block.type = Material.AIR
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    };

    abstract fun spawn(location: Location): ArmorStand

    abstract fun executeAction(placedMinion: PlacedMinion)

    fun spawnArmorStand(location: Location, block: ArmorStand.() -> Unit): ArmorStand {
        return location.world.spawn(location, ArmorStand::class.java).apply {
            isInvulnerable = true
            setGravity(false)
            setMarker(false)
            setSmall(true)
            setArms(true)

            block(this)
        }
    }
}