package one_block.system.spawners.listeners

import me.devnatan.inventoryframework.ViewFrame
import one_block.system.islands.island.Island
import one_block.system.islands.island.IslandManager
import one_block.system.spawners.rules.StackRules
import one_block.system.spawners.models.PlacedSpawner
import one_block.system.spawners.manager.PlacedSpawnerManager
import one_block.system.spawners.models.SpawnerType
import one_block.system.spawners.SpawnersSystem
import one_block.system.spawners.views.SpawnerView
import org.bukkit.block.Block
import org.bukkit.block.CreatureSpawner
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

class SpawnerBlockListener(private val viewFrame: ViewFrame) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val type = SpawnersSystem.typeOf(event.itemInHand) ?: return
        val player = event.player
        val block = event.blockPlaced

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode colocar isso aqui.")
            return
        }

        val location = PlacedSpawnerManager.keyOf(block)
        if (PlacedSpawnerManager.hasNeighborWithin(location)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Muito perto de outro spawner. <gray>(mínimo de 5 blocos)")
            return
        }

        val placed = PlacedSpawner(
            uuid = UUID.randomUUID(),
            location = location,
            typeId = type.name,
            ownerUuid = player.uniqueId,
        )

        applySpawnerState(block, type)
        PlacedSpawnerManager.register(placed)
        player.sendRichMessage("<green>Spawner colocado. <gray>Clique com outro na mão para empilhar.")
    }

    @EventHandler(ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val block = event.block
        val placed = PlacedSpawnerManager.get(block) ?: return
        val type = placed.type() ?: return
        val player = event.player

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não pode quebrar isso.")
            return
        }

        event.isDropItems = false
        val stack = placed.stackSize
        PlacedSpawnerManager.unregister(placed.location)

        val dropAt = block.location.clone().add(0.5, 0.5, 0.5)
        block.world.dropItemNaturally(dropAt, SpawnersSystem.build(type, stack))
        player.sendRichMessage("<green>Spawner recolhido. <gray>(${stack}x)")
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

        val player = event.player
        if (player.isSneaking) return

        val block = event.clickedBlock ?: return
        val placed = PlacedSpawnerManager.get(block) ?: return
        val type = placed.type() ?: return

        val island = IslandManager.getIslandAt(block.location)
        if (island == null || !canManage(island, player)) {
            event.isCancelled = true
            player.sendRichMessage("<red>Você não tem acesso a este spawner.")
            return
        }

        event.isCancelled = true

        val inHand = player.inventory.itemInMainHand
        val handType = SpawnersSystem.typeOf(inHand)

        if (handType == null) {
            openMenu(player, placed)
            return
        }

        if (handType != type) {
            player.sendRichMessage("<red>Esse spawner é de outro mob.")
            return
        }

        stackFromHand(player, placed, inHand.amount)
    }

    private fun stackFromHand(player: Player, placed: PlacedSpawner, offered: Int) {
        val result = StackRules.add(placed.stackSize, offered)
        if (result.consumed == 0) {
            player.sendRichMessage("<red>Este spawner já está no limite de <white>${StackRules.MAX_STACK}<red>.")
            return
        }

        placed.stackSize = result.newStack
        if (result.leftover <= 0) {
            player.inventory.setItemInMainHand(null)
        } else {
            player.inventory.itemInMainHand.amount = result.leftover
        }
        PlacedSpawnerManager.saveNow(placed)

        player.sendRichMessage(
            "<green>+${result.consumed} spawner(s). <gray>Total: <white>${placed.stackSize}<gray>/${StackRules.MAX_STACK}"
        )
    }

    private fun openMenu(player: Player, placed: PlacedSpawner) {
        viewFrame.open(SpawnerView::class.java, player, placed)
    }

    private fun applySpawnerState(block: Block, type: SpawnerType) {
        val state = block.state as? CreatureSpawner ?: return
        state.spawnedType = type.entityType
        state.requiredPlayerRange = 0
        state.update(true, false)
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) = protect(event.blockList())

    @EventHandler
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (event.blocks.any { PlacedSpawnerManager.get(it) != null }) event.isCancelled = true
    }

    @EventHandler
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (event.blocks.any { PlacedSpawnerManager.get(it) != null }) event.isCancelled = true
    }

    private fun protect(blocks: MutableList<Block>) {
        blocks.removeIf { PlacedSpawnerManager.get(it) != null }
    }

    private fun canManage(island: Island, player: Player): Boolean =
        island.ownerUuid == player.uniqueId || island.isMember(player.name)
}
