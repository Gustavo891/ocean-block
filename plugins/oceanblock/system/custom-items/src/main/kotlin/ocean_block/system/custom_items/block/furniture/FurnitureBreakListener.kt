package ocean_block.system.custom_items.block.furniture

import ocean_block.system.custom_items.block.CustomBlockRegistry
import ocean_block.system.custom_items.block.PlacedBlockManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID

class FurnitureBreakListener : Listener {

    private val hits = mutableMapOf<UUID, Pair<UUID, FurnitureHit>>()

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    fun onHit(event: PlayerInteractEvent) {
        if (event.action != Action.LEFT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.BARRIER) return
        val player = event.player
        if (player.gameMode == GameMode.CREATIVE) return
        val placed = PlacedBlockManager.get(block) ?: return
        val custom = CustomBlockRegistry.get(placed.blockId)?.takeIf { it.material == Material.BARRIER } ?: return
        event.isCancelled = true

        val now = Bukkit.getCurrentTick()
        val previous = hits[player.uniqueId]?.takeIf { it.first == placed.uuid }?.second
        val count = FurnitureHits.next(previous, now)
        val center = block.location.add(0.5, 0.5, 0.5)
        val feel = custom.furnitureFeel.createBlockData()
        if (count < FurnitureHits.TO_BREAK) {
            hits[player.uniqueId] = placed.uuid to FurnitureHit(count, now)
            block.world.spawnParticle(Particle.BLOCK, center, 8, 0.3, 0.3, 0.3, feel)
            block.world.playSound(center, feel.soundGroup.hitSound, 0.8f, 0.8f + count * 0.15f)
            return
        }
        hits.remove(player.uniqueId)
        val breakEvent = BlockBreakEvent(block, player)
        Bukkit.getPluginManager().callEvent(breakEvent)
        if (breakEvent.isCancelled) return
        block.setType(Material.AIR, true)
        block.world.spawnParticle(Particle.BLOCK, center, 24, 0.35, 0.35, 0.35, feel)
        block.world.playSound(center, feel.soundGroup.breakSound, 1f, 0.9f)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        hits.remove(event.player.uniqueId)
    }
}
