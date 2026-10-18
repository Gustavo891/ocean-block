package one_block.system.custom_items.content.vacuum_hopper

import one_block.libs.item_builder.itemStack
import one_block.system.craft.CraftCategory
import one_block.system.craft.forge.ForgeRecipe
import one_block.system.craft.forge.ForgeRequirement
import one_block.system.custom_items.block.CustomBlock
import one_block.system.custom_items.block.PlacedBlock
import one_block.system.custom_items.block.PlacedBlockManager
import one_block.system.custom_items.block.context.BlockClickContext
import one_block.system.custom_items.content.vacuum_hopper.view.VacuumHopperView
import one_block.system.minions.Minions
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

class VacuumHopper : CustomBlock() {

    override val id = ID
    override val material = Material.CONDUIT
    override val displayName = "<aqua>Vacuum Hopper"
    override val lore = listOf(
        "<gray>Suga os itens dropados",
        "<gray>num raio ao redor.",
        "",
        "<dark_gray>Clique-direito para abrir."
    )
    override val limitPerIsland = VacuumHopperUpgrade.LIMIT_PER_ISLAND
    override val upgrades = VacuumHopperUpgrade.entries.toList()

    override fun storageCapacity(placed: PlacedBlock): Int =
        VacuumHopperUpgrade.storageSlotsFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Storage))

    override fun tickInterval(placed: PlacedBlock): Long =
        VacuumHopperUpgrade.tickIntervalFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Speed))

    fun radius(placed: PlacedBlock): Double =
        VacuumHopperUpgrade.radiusFor(placed.getUpgradeLevel(VacuumHopperUpgrade.Radius))

    override fun tick(placed: PlacedBlock) {
        val capacity = storageCapacity(placed)
        if (placed.isStorageFull(capacity)) {
            notifyFull(placed)
            return
        }

        val center = placed.location.toBukkit()?.add(0.5, 0.5, 0.5) ?: return
        val world = center.world ?: return
        val range = radius(placed)

        val inventory = placed.buildStorageInventory(capacity)
        var changed = false

        try {
            world.getNearbyEntities(center, range, range, range) { it is Item }.forEach { entity ->
                val drop = entity as Item
                if (drop.pickupDelay >= Short.MAX_VALUE.toInt()) return@forEach

                val stack = drop.itemStack
                if (!placed.accepts(stack.type)) return@forEach

                val leftover = inventory.addItem(stack.clone()).values.firstOrNull()

                if (leftover != null && leftover.amount == stack.amount) {
                    return@forEach
                }

                changed = true

                if (leftover == null) {
                    drop.remove()
                } else {
                    drop.itemStack = leftover
                }
            }
        } finally {
            if (changed) {
                placed.setStorage(inventory)
                PlacedBlockManager.markDirty(placed)
            }
        }

        if (changed && placed.isStorageFull(capacity)) notifyFull(placed)
    }

    override fun onRightClick(ctx: BlockClickContext) {
        ctx.cancel()
        ctx.viewFrame.open(VacuumHopperView::class.java, ctx.player, ctx.placed)
    }

    private fun notifyFull(placed: PlacedBlock) {
        if (placed.notifiedFull) return
        placed.notifiedFull = true
        PlacedBlockManager.markDirty(placed)

        val owner = Bukkit.getPlayer(placed.ownerUuid) ?: return
        owner.sendRichMessage("<yellow>Seu <aqua>Vacuum Hopper<yellow> está cheio e parou de sugar.")
        owner.playSound(owner, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f)
    }

    companion object {
        const val ID = "vacuum_hopper"

        fun clearFullNotice(placed: PlacedBlock) {
            if (!placed.notifiedFull) return
            placed.notifiedFull = false
            PlacedBlockManager.saveNow(placed)
        }

        fun forgeRecipe(hopper: VacuumHopper) = ForgeRecipe(
            id = "vacuum_hopper",
            prettyName = "Vacuum Hopper",
            icon = hopper.buildItem(),
            result = hopper.buildItem(),
            requirements = listOf(
                ForgeRequirement("Conduit", ItemStack(Material.CONDUIT), 1),
                ForgeRequirement("Barra de Netherite", itemStack(Material.NETHERITE_INGOT) {}, 4),
                ForgeRequirement("Peça Mecânica", Minions.machinePart(), 8),
            ),
            category = CraftCategory.MINERACAO,
        )
    }
}
