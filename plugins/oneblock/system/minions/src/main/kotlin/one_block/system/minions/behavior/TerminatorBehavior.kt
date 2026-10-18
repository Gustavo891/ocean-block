package one_block.system.minions.behavior

import net.kyori.adventure.text.Component
import one_block.libs.formatter.Mini
import one_block.libs.instance.Instance
import one_block.libs.item_builder.itemStack
import one_block.system.minions.enums.MinionUpgrade
import one_block.system.minions.listener.TerminatorKill
import one_block.system.minions.model.MinionModel
import one_block.system.minions.placed_minion.PlacedMinion
import one_block.system.minions.placed_minion.PlacedMinionManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Mob
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID

class TerminatorBehavior : MinionBehavior {

    private val cooldowns = mutableMapOf<UUID, Int>()

    override fun spawn(placedMinion: PlacedMinion, location: Location) {
        val armorStand =
            if (MinionModel.isAvailable()) spawnModeled(placedMinion, location)
            else spawnFallback(location)

        PlacedMinionManager.register(placedMinion, armorStand, spawnDisplay(placedMinion, armorStand))
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
            MinionModel.markOverride(MODEL_ID, ATTACK_ANIMATION)
            MinionModel.playLoop(placedMinion.uuid, IDLE_ANIMATION)
        } else {
            armorStand.isMarker = false
            armorStand.isVisible = true
            applyArmor(armorStand)
        }
        return armorStand
    }

    private fun spawnFallback(location: Location): ArmorStand =
        spawnArmorStand(location) { applyArmor(this) }

    private fun applyArmor(armorStand: ArmorStand) = with(armorStand) {
        setItem(EquipmentSlot.HEAD, itemStack(Material.NETHERITE_HELMET) {})
        setItem(EquipmentSlot.CHEST, itemStack(Material.NETHERITE_CHESTPLATE) {})
        setItem(EquipmentSlot.LEGS, itemStack(Material.NETHERITE_LEGGINGS) {})
        setItem(EquipmentSlot.FEET, itemStack(Material.NETHERITE_BOOTS) {})
        setItem(EquipmentSlot.HAND, itemStack(Material.DIAMOND_SWORD) {})
    }

    override fun tick(placedMinion: PlacedMinion): Boolean {
        val remaining = cooldowns[placedMinion.uuid] ?: 0
        if (remaining > 0) {
            cooldowns[placedMinion.uuid] = remaining - 1
            return false
        }

        val location = placedMinion.location.toBukkit() ?: return false

        val target = location.getNearbyLivingEntities(RANGE)
            .filterIsInstance<Mob>()
            .filter { !it.isDead }
            .minByOrNull { it.location.distanceSquared(location) }
            ?: return false

        cooldowns[placedMinion.uuid] =
            MinionUpgrade.terminatorCooldownFor(placedMinion.getUpgradeLevel(MinionUpgrade.TerminatorSpeed))

        val damage = damageOf(placedMinion)

        if (!MinionModel.has(placedMinion.uuid)) {
            TerminatorKill.mark(target, placedMinion.uuid)
            target.damage(damage)
            return true
        }

        MinionModel.face(placedMinion.uuid, MinionFacing.yawTowards(location, target.location))

        val animationSpeed =
            if (TICK_INTERVAL_TICKS < ANIMATION_LENGTH_TICKS) ANIMATION_LENGTH_TICKS.toDouble() / TICK_INTERVAL_TICKS
            else 1.0

        MinionModel.playAnimation(placedMinion.uuid, ATTACK_ANIMATION, animationSpeed)

        val minionUuid = placedMinion.uuid
        val hitDelay = (HIT_DELAY_TICKS / animationSpeed).toLong().coerceAtLeast(1L)
        Bukkit.getScheduler().runTaskLater(Instance, Runnable {
            if (PlacedMinionManager.get(minionUuid) == null) return@Runnable
            if (PlacedMinionManager.getArmorStand(minionUuid) == null) return@Runnable
            if (target.isDead) return@Runnable

            TerminatorKill.mark(target, minionUuid)
            target.damage(damage)
            target.world.playSound(target.location, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f)
        }, hitDelay)

        return true
    }

    override fun onUnregister(uuid: UUID) {
        cooldowns.remove(uuid)
        MinionModel.detach(uuid)
    }

    override fun spawnDisplay(placedMinion: PlacedMinion, armorStand: ArmorStand): TextDisplay {
        val base = placedMinion.location.toBukkit() ?: armorStand.location
        return spawnDisplay(base.clone().add(0.0, 1.9, 0.0)) { text(displayText(placedMinion)) }
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

        return component.append(
            Mini.mm.deserialize(
                "<white>$status ${placedMinion.type.prettyName}"
            )
        )
    }

    private fun damageOf(placedMinion: PlacedMinion): Double =
        MinionUpgrade.terminatorDamageFor(placedMinion.getUpgradeLevel(MinionUpgrade.TerminatorDamage))

    private companion object {
        const val MODEL_ID = "knight"
        const val IDLE_ANIMATION = "idle"
        const val ATTACK_ANIMATION = "attack"
        const val MODEL_SCALE = 0.7

        const val TICK_INTERVAL_TICKS = 20L
        const val ANIMATION_LENGTH_TICKS = 20L
        const val HIT_DELAY_TICKS = 12L

        const val RANGE = 5.0
    }
}
