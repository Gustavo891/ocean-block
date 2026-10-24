package ocean_block.system.machines.miner

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.system.custom_items.block.PlacedBlock
import ocean_block.system.custom_items.block.PlacedBlockManager
import ocean_block.system.machines.MachineMenus
import ocean_block.system.machines.energy.EnergyNetwork
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack

class MinerMenu : View() {

    private val blockState = initialState<PlacedBlock>()

    private val pagination = lazyPaginationState(
        { ctx -> blockState.get(ctx).storageItems().toMutableList() }
    ) { _, builder, _, value ->
        builder.renderWith { value }
            .onClick { click -> withdraw(click, value) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(MachineMenus.title("Minerador"))
        config.scheduleUpdate(MachineMenus.UPDATE_TICKS)
        config.layout(
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "  L C I  ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('L')
            .renderWith { powerItem(MinerBlock.isOn(blockState.get(render))) }
            .onClick { click ->
                val placed = blockState.get(click)
                if (!MachineMenus.requireAccess(click, placed)) return@onClick
                val on = MinerBlock.toggle(placed)
                click.player.playSound(click.player, Sound.BLOCK_LEVER_CLICK, 0.8f, if (on) 1.2f else 0.8f)
                click.update()
            }
        render.layoutSlot('C')
            .renderWith { MachineMenus.icon(Material.HOPPER, "<green>Coletar tudo", listOf("<gray>Move todos os itens guardados", "<gray>para o seu inventário.")) }
            .onClick { click -> collectAll(click) }
        render.layoutSlot('I')
            .renderWith { statusItem(blockState.get(render)) }
    }

    private fun withdraw(click: SlotClickContext, value: ItemStack) {
        val placed = blockState.get(click)
        if (!MachineMenus.requireAccess(click, placed)) return
        val stored = placed.storageItems()
        val index = stored.indexOfFirst { it.isSimilar(value) && it.amount == value.amount }
        if (index == -1) return refresh(click)

        val returned = click.player.inventory.addItem(value.clone()).values.firstOrNull()
        if (returned != null && returned.amount == value.amount) {
            click.player.sendRichMessage("<red>Seu inventário está cheio.")
            return
        }
        val updated = if (returned == null) {
            stored.filterIndexed { position, _ -> position != index }
        } else {
            stored.mapIndexed { position, item -> if (position == index) returned else item }
        }
        save(placed, updated)
        refresh(click)
    }

    private fun collectAll(click: SlotClickContext) {
        val placed = blockState.get(click)
        if (!MachineMenus.requireAccess(click, placed)) return
        val remaining = placed.storageItems().flatMap { click.player.inventory.addItem(it.clone()).values }
        save(placed, remaining)
        refresh(click)
        if (remaining.isNotEmpty()) click.player.sendRichMessage("<yellow>Seu inventário encheu; parte dos itens ficou no minerador.")
    }

    private fun save(placed: PlacedBlock, items: List<ItemStack>) {
        placed.setStorage(items)
        placed.counters[MinerBlock.FULL_KEY] = 0
        PlacedBlockManager.saveNow(placed)
    }

    private fun refresh(click: SlotClickContext) {
        pagination.get(click).forceUpdate()
        click.update()
    }

    private fun powerItem(on: Boolean): ItemStack = MachineMenus.icon(
        if (on) Material.REDSTONE_TORCH else Material.LEVER,
        if (on) "<green>Ligado" else "<red>Desligado",
        listOf(
            "<gray>Ligado, a broca gira sem parar e",
            "<gray>puxa <yellow>20 E/s <gray>o tempo todo.",
            "",
            if (on) "<yellow>Clique para desligar." else "<yellow>Clique para ligar.",
        ),
    )

    private fun statusItem(placed: PlacedBlock): ItemStack {
        val status = when {
            !MinerBlock.isOn(placed) -> "<gray>Desligado"
            placed.counters[MinerBlock.FULL_KEY] == 1L -> "<red>Armazenamento cheio"
            EnergyNetwork.hasNoPower(placed) -> "<red>Sem energia"
            else -> MinerBlock.belowLabel(placed)
        }
        return MachineMenus.icon(
            Material.IRON_PICKAXE,
            "<yellow>Minerador",
            listOf(
                "<gray>Estado: $status",
                "<gray>Energia no raio: <yellow>${EnergyNetwork.available(placed)} E",
                "<gray>Pilhas: <white>${placed.storageItems().size}/${MinerRules.STORAGE}",
                "",
                "<gray>Clique num item para retirá-lo.",
            ),
        )
    }
}
