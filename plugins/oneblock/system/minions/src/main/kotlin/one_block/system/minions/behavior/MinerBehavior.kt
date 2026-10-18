package one_block.system.minions.behavior

import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import one_block.libs.formatter.Mini
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.libs.item_builder.meta
import one_block.system.islands.hud.IslandHud
import one_block.system.islands.island.IslandManager
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.model.MinionModel
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import one_block.textura.api.BlockApi
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.util.EulerAngle
import java.util.UUID

class MinerBehavior : MinionBehavior {

    private val cooldowns = mutableMapOf<UUID, Int>()
    private val headTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWExYjUzODk3MTdhNjFhMGU5NDIxYzk3OTg4NDA1ZDVhMzM2YTVhYzFlZThlMGYyZjM1Njc4ZDFkNjFmNDU1MCJ9fX0="

    override fun placement(clickedBlock: Block, face: BlockFace): MinionPlacement? {
        val standBlock = clickedBlock.getRelative(face)
        if (!standBlock.type.isAir) return null

        val location = standBlock.location.toCenterLocation().apply {
            y = standBlock.y.toDouble()
            yaw = 0.0f
            pitch = 0.0f
        }
        return MinionPlacement(location, face.oppositeFace)
    }

    override fun spawn(placedMinion: PlacedMinion, location: Location) {
        val spawnLocation = location.clone().apply {
            yaw = MinionFacing.yaw(MinionFacing.of(placedMinion))
            pitch = 0.0f
        }

        val armorStand =
            if (MinionModel.isAvailable()) spawnModeled(placedMinion, spawnLocation)
            else spawnFallback(placedMinion, spawnLocation)

        val display = spawnDisplay(placedMinion, armorStand)

        PlacedMinionManager.register(placedMinion, armorStand, display)
        placedMinion.type.updateDisplay(placedMinion)
    }

    private fun spawnModeled(placedMinion: PlacedMinion, location: Location): ArmorStand {
        val armorStand = spawnArmorStand(location) {
            isMarker = true
            isVisible = false
            isSilent = true
        }

        val attached = MinionModel.attach(placedMinion.uuid, armorStand, MODEL_ID, location.yaw, MODEL_SCALE)
        if (attached) {
            MinionModel.markOverride(MODEL_ID, MINE_ANIMATION)
            MinionModel.playLoop(placedMinion.uuid, IDLE_ANIMATION)
        } else {
            armorStand.isMarker = false
            armorStand.isVisible = true
        }
        return armorStand
    }

    private fun spawnFallback(placedMinion: PlacedMinion, location: Location): ArmorStand {
        return spawnArmorStand(location) {
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
            setItem(EquipmentSlot.HAND, itemStack(Material.DIAMOND_PICKAXE) {})
        }
    }

    override fun tick(placedMinion: PlacedMinion): Boolean {
        val current = cooldowns[placedMinion.uuid] ?: 0
        if (current > 0) {
            cooldowns[placedMinion.uuid] = current - 1
            return false
        }

        val speed = placedMinion.getUpgradeLevel(MinionUpgrade.MinerSpeed)
        val intervalSeconds = (10 - speed).coerceAtLeast(1)
        cooldowns[placedMinion.uuid] = intervalSeconds

        if (!MinionModel.has(placedMinion.uuid)) {
            animateArm(placedMinion.uuid)
            mine(placedMinion)
            return true
        }

        val intervalTicks = intervalSeconds * 20L
        val animationSpeed =
            if (intervalTicks < ANIMATION_LENGTH_TICKS) ANIMATION_LENGTH_TICKS.toDouble() / intervalTicks
            else 1.0

        MinionModel.playAnimation(placedMinion.uuid, MINE_ANIMATION, animationSpeed)

        val minionUuid = placedMinion.uuid
        val breakDelay = (BREAK_DELAY_TICKS / animationSpeed).toLong().coerceAtLeast(1L)
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            val current = PlacedMinionManager.get(minionUuid) ?: return@Runnable
            if (PlacedMinionManager.getArmorStand(minionUuid) == null) return@Runnable
            mine(current)
        }, breakDelay)

        return true
    }

    override fun onUnregister(uuid: UUID) {
        cooldowns.remove(uuid)
        MinionModel.detach(uuid)
    }

    override fun spawnDisplay(placedMinion: PlacedMinion, armorStand: ArmorStand): TextDisplay {
        val base = placedMinion.location.toBukkit() ?: armorStand.location
        return spawnDisplay(base.clone().add(0.0, 1.9, 0.0)) { displayText(placedMinion) }
    }

    override fun updateDisplay(placedMinion: PlacedMinion) {
        val display = PlacedMinionManager.getDisplay(placedMinion.uuid) ?: return
        display.text(displayText(placedMinion))
    }

    private fun displayText(placedMinion: PlacedMinion): Component {
        var component: Component = Component.empty()
        if (!placedMinion.hasEnergy()) {
            component = component.append(Mini.mm.deserialize("<red>🪫 Sem energia")).appendNewline()
        }
        if (placedMinion.isStorageFull()) {
            component = component.append(Mini.mm.deserialize(storageFullWarning(placedMinion))).appendNewline()
        }

        val status = if (placedMinion.enabled && placedMinion.hasEnergy() && !placedMinion.isPaused()) {
            ''
        } else {
            ''
        }

        component = component.append(
            Mini.mm.deserialize(
                "<white>$status ${placedMinion.type.prettyName}"
            )
        )

        return component
    }

    private fun mine(placedMinion: PlacedMinion) {

        val location = placedMinion.location.toBukkit() ?: return
        val target = location.block.getRelative(MinionFacing.of(placedMinion))
        val island = IslandManager.getIslandAt(location) ?: return
        val isCenter = island.center.isSameBlock(target.location)

        if (!isCenter && target.type == Material.AIR) return

        playBreakEffect(target)
        placedMinion.countProduced()

        val drops = contentsOf(target) + (BlockApi.dropsAt(target) ?: target.getDrops(MINE_TOOL))

        if (isCenter) {
            island.processBlockBreak(target)
            IslandHud.refresh(island)
            island.markDirty()
        } else {
            target.type = Material.AIR
        }

        val accepted = drops.filter { placedMinion.accepts(it) }
        val leftover = placedMinion.addToStorage(accepted)
        if (placedMinion.fullMode != one_block.system.minions.enums.FullMode.TRASH) {
            leftover.forEach { target.world.dropItemNaturally(location.clone(), it) }
        }
    }

    private fun contentsOf(block: Block): List<ItemStack> {
        val container = block.getState(false) as? Container ?: return emptyList()
        val items = container.inventory.contents.filterNotNull().filter { !it.type.isAir }

        container.inventory.clear()
        return items
    }

    private fun playBreakEffect(block: Block) {
        if (block.type == Material.AIR) return

        val center = block.location.toCenterLocation()
        val sounds = block.blockSoundGroup
        block.world.spawnParticle(Particle.BLOCK, center, 30, 0.3, 0.3, 0.3, 0.0, block.blockData)
        block.world.playSound(center, sounds.breakSound, sounds.volume, sounds.pitch)
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

    private companion object {
        val MINE_TOOL = org.bukkit.inventory.ItemStack(Material.NETHERITE_PICKAXE)

        const val MODEL_ID = "miner3"
        const val IDLE_ANIMATION = "idle"
        const val MINE_ANIMATION = "miner"
        const val MODEL_SCALE = 0.7
        const val ANIMATION_LENGTH_TICKS = 40L
        const val BREAK_DELAY_TICKS = 34L
    }
}
