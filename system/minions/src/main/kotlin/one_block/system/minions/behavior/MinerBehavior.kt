package one_block.system.minions.behavior

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.system.islands.bossbar.BossbarManager
import one_block.system.islands.island.IslandManager
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.util.EulerAngle
import java.util.UUID

class MinerBehavior : MinionBehavior {

    private val cooldowns = mutableMapOf<UUID, Int>()
    private val headTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWExYjUzODk3MTdhNjFhMGU5NDIxYzk3OTg4NDA1ZDVhMzM2YTVhYzFlZThlMGYyZjM1Njc4ZDFkNjFmNDU1MCJ9fX0="

    override fun spawn(placedMinion: PlacedMinion, location: Location) {
        val armorStand = spawnArmorStand(location) {
            setItem(EquipmentSlot.HEAD, itemStack(Material.PLAYER_HEAD) {
                meta<SkullMeta> {
                    playerProfile = Bukkit.createProfile(UUID.randomUUID(), "1").apply {
                        setProperty(
                            ProfileProperty(
                                "textures",
                                headTexture
                            )
                        )
                    }
                }
            })
            setItem(EquipmentSlot.CHEST, itemStack(Material.LEATHER_CHESTPLATE) {
                meta<LeatherArmorMeta> { setColor(Color.ORANGE) }
            })
            setItem(EquipmentSlot.LEGS, itemStack(Material.LEATHER_LEGGINGS) {
                meta<LeatherArmorMeta> { setColor(Color.ORANGE) }
            })
            setItem(EquipmentSlot.FEET, itemStack(Material.LEATHER_BOOTS) {
                meta<LeatherArmorMeta> { setColor(Color.ORANGE) }
            })
            if(placedMinion.getTool() != null) {
                setItem(EquipmentSlot.HAND, itemStack(placedMinion.getTool()!!.type) {})
            }
        }
        val display = spawnDisplay(placedMinion, armorStand)

        PlacedMinionManager.register(placedMinion, armorStand, display)
        placedMinion.type.updateDisplay(placedMinion)
    }

    override fun tick(placedMinion: PlacedMinion): Boolean {
        val current = cooldowns[placedMinion.uuid] ?: 0
        if (current > 0) {
            cooldowns[placedMinion.uuid] = current - 1
            return false
        }

        val speed = placedMinion.getUpgradeLevel(MinionUpgrade.MinerSpeed)
        val efficiency = placedMinion.getTool()?.getEnchantmentLevel(Enchantment.EFFICIENCY) ?: 0
        cooldowns[placedMinion.uuid] = (10 - speed - efficiency).coerceAtLeast(1)

        mine(placedMinion)
        return true
    }

    override fun onUnregister(uuid: UUID) {
        cooldowns.remove(uuid)
    }

    override fun spawnDisplay(placedMinion: PlacedMinion, armorStand: ArmorStand): TextDisplay {
        if(placedMinion.location.toBukkit() == null) return spawnDisplay(armorStand.location.clone().add(0.0, 1.4, 0.0)) { displayText(placedMinion) }
        return spawnDisplay(placedMinion.location.toBukkit()!!.clone().add(0.0, 1.4, 0.0)) { displayText(placedMinion) }
    }

    override fun updateDisplay(placedMinion: PlacedMinion) {
        val display = PlacedMinionManager.getDisplay(placedMinion.uuid) ?: return
        display.text(displayText(placedMinion))
    }

    private fun displayText(placedMinion: PlacedMinion): Component {
        var component = Component.empty()
        if (placedMinion.getTool() == null) {
            component = component.append(MiniMessage.miniMessage().deserialize("<red>⛏ Sem ferramenta")).appendNewline()
        } else if(!placedMinion.hasEnergy()) {
            component = component.append(MiniMessage.miniMessage().deserialize("<red>\uD83E\uDEAB Sem energia")).appendNewline()
        }

        component = component.append(
            MiniMessage.miniMessage().deserialize(
                "<glyph:legendary> <white>${placedMinion.type.prettyName}"
            ).appendNewline()
                .append(
                    MiniMessage.miniMessage()
                        .deserialize("<white>Status: ${if (placedMinion.enabled) "<green>Ativado" else "<red>Desativado"}")
                )
        )

        return component
    }

    private fun mine(placedMinion: PlacedMinion) {
        val tool = placedMinion.getTool() ?: return
        val location = placedMinion.location.toBukkit() ?: return
        val blockBelow = location.block.getRelative(BlockFace.DOWN)
        val island = IslandManager.getIslandAt(location) ?: return

        animateArm(placedMinion.uuid)

        if (island.center.isSameBlock(blockBelow.location)) {
            val drops = blockBelow.getDrops(tool)
            island.processBlockBreak(blockBelow)
            val dropLocation = blockBelow.location.add(0.5, 0.5, 0.5)
            for (item in drops) {
                val dropped = blockBelow.world.dropItem(dropLocation, item)
                dropped.velocity = dropped.velocity.multiply(0.1)
            }
            BossbarManager.upgradeBossBar(island)
            island.markDirty()
        } else {
            if (blockBelow.type != Material.AIR) {
                val drops = blockBelow.getDrops(tool)
                blockBelow.type = Material.AIR
                drops.forEach { blockBelow.world.dropItemNaturally(location.clone(), it) }
            }
        }
    }

    private fun animateArm(uuid: UUID) {
        val armorStand = PlacedMinionManager.getArmorStand(uuid) ?: return
        val scheduler = Bukkit.getScheduler()
        val frames = listOf(
            0L to EulerAngle(-Math.PI * 0.2, 0.0, 0.0),
            1L to EulerAngle(-Math.PI * 0.5, 0.0, 0.0),
            2L to EulerAngle(-Math.PI * 0.7, 0.0, 0.0),
            3L to EulerAngle(-Math.PI * 0.4, 0.0, 0.0),
            4L to EulerAngle(Math.PI * 0.1, 0.0, 0.0),
            5L to EulerAngle(Math.PI * 0.3, 0.0, 0.0),
            6L to EulerAngle(Math.PI * 0.1, 0.0, 0.0),
            7L to EulerAngle(Math.PI * 0.05, 0.0, 0.0),
            8L to EulerAngle(0.0, 0.0, 0.0),
        )
        frames.forEach { (tick, pose) ->
            scheduler.runTaskLater(Instance, Runnable {
                if (!armorStand.isDead) armorStand.rightArmPose = pose
            }, tick)
        }
    }

}
