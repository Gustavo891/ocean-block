package ocean_block.system.custom_items.block

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.instance.Instance
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.custom_items.block.context.BreakContext
import ocean_block.system.custom_items.block.context.PlaceContext
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandManager
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID

class CustomBlockListener(private val viewFrame: ViewFrame) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val custom = CustomBlockRegistry.fromItemStack(event.itemInHand) ?: return
        val player = event.player
        val block = event.blockPlaced

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode colocar isso aqui.")
            return
        }

        val current = PlacedBlockManager.countOnIsland(custom.id, island.uuid)
        if (current >= custom.limitPerIsland) {
            event.isCancelled = true
            player.sendRichMessage("<red>Limite de <white>${custom.limitPerIsland}<red> por ilha atingido.")
            return
        }

        val location = PlacedBlockManager.keyOf(block)
        val placed = PlacedBlockCodec.read(event.itemInHand, custom.id, location, player.uniqueId)
            ?: PlacedBlock(
                uuid = UUID.randomUUID(),
                location = location,
                blockId = custom.id,
                ownerUuid = player.uniqueId,
            )

        runHook(custom.id) { custom.onPlace(PlaceContext(player, block, placed, event)) }
        if (event.isCancelled) return

        PlacedBlockManager.register(placed)
        player.sendRichMessage("<green>Bloco colocado. <gray>(${current + 1}/${custom.limitPerIsland})")
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        val placed = PlacedBlockManager.get(block) ?: return
        val custom = CustomBlockRegistry.get(placed.blockId) ?: run {
            Instance.logger.warning(
                "[custom-items] (break) bloco ${placed.uuid} em ${placed.location} " +
                    "tem blockId '${placed.blockId}' sem CustomBlock registrado."
            )
            return
        }
        val player = event.player

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode quebrar isso.")
            return
        }

        runHook(custom.id) { custom.onBreak(BreakContext(player, block, placed, event)) }
        if (event.isCancelled) return

        event.isDropItems = false
        PlacedBlockManager.unregister(placed.location)

        val dropAt = block.location.clone().add(0.5, 0.5, 0.5)
        block.world.dropItemNaturally(dropAt, custom.buildItem(placed))
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        if (player.isSneaking) return

        val block = event.clickedBlock ?: return
        val placed = PlacedBlockManager.get(block) ?: return
        val custom = CustomBlockRegistry.get(placed.blockId) ?: return

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não tem acesso a este bloco.")
            return
        }

        event.isCancelled = true
        runHook(custom.id) {
            custom.onRightClick(BlockClickContext(player, block, placed, viewFrame, event))
        }
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { PlacedBlockManager.get(it) != null }) event.isCancelled = true
    }

    @EventHandler
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { PlacedBlockManager.get(it) != null }) event.isCancelled = true
    }

    private fun protect(blocks: MutableList<Block>) {
        blocks.removeIf { PlacedBlockManager.get(it) != null }
    }

    private fun canManage(island: Island, player: Player): Boolean =
        island.ownerUuid == player.uniqueId || island.isMember(player.name)

    private inline fun runHook(id: String, block: () -> Unit) {
        runCatching { block() }
            .onFailure { Instance.logger.warning("[custom-items] hook de bloco '$id' falhou: ${it.message}") }
    }
}
