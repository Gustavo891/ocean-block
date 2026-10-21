package ocean_block.system.research.table

import ocean_block.system.custom_items.block.CustomBlock
import ocean_block.system.custom_items.block.context.BlockClickContext
import ocean_block.system.islands.island.IslandManager
import ocean_block.system.research.screen.ResearchScreen
import org.bukkit.Material

object ResearchTableBlock : CustomBlock() {

    const val ID = "mesa_pesquisa"

    override val id: String = ID
    override val material: Material = Material.CARTOGRAPHY_TABLE
    override val displayName: String = "<white>Mesa de Pesquisa"
    override val lore: List<String> = listOf(
        "<gray>Entregue itens para liberar",
        "<gray>estações e melhorias para o raft.",
    )
    override val limitPerIsland: Int = 2

    override fun onRightClick(ctx: BlockClickContext) {
        val island = IslandManager.getIslandAt(ctx.block.location) ?: return
        ResearchScreen.open(ctx.player, island.uuid)
    }
}
