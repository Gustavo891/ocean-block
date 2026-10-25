package ocean_block.system.machines.miner

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.system.custom_items.CustomIds
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
            " L B C I ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('L')
            .renderWith { blockState.get(render).let { powerItem(MinerBlock.isOn(it), MinerBlock.tier(it)) } }
            .onClick { click ->
                val placed = blockState.get(click)
                if (!MachineMenus.requireAccess(click, placed)) return@onClick
                val on = MinerBlock.toggle(placed)
                click.player.playSound(click.player, Sound.BLOCK_LEVER_CLICK, 0.8f, if (on) 1.2f else 0.8f)
                click.update()
            }
        render.layoutSlot('B')
            .renderWith { drillItem(MinerBlock.tier(blockState.get(render))) }
            .onClick { click -> unmount(click) }
        render.layoutSlot('C')
            .renderWith { MachineMenus.icon(Material.HOPPER, "<green>Coletar tudo", listOf("<gray>Move todos os itens guardados", "<gray>para o seu inventário.")) }
            .onClick { click -> collectAll(click) }
        render.layoutSlot('I')
            .renderWith { statusItem(blockState.get(render)) }
    }

    override fun onClick(click: SlotClickContext) {
        if (!click.isOnEntityContainer) return
        click.isCancelled = true
        val placed = blockState.get(click)
        if (!MachineMenus.requireAccess(click, placed)) return
        val event = click.clickOrigin
        val stack = event.currentItem ?: return
        val inserted = DrillTier.byItemId(CustomIds.of(stack))
        val swap = inserted?.let { DrillSwap.mount(MinerBlock.tier(placed), it) }
        if (swap == null) {
            click.player.playSound(click.player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            return
        }
        stack.amount -= 1
        event.currentItem = stack.takeIf { it.amount > 0 }
        MinerBlock.setTier(placed, swap.next)
        swap.returned?.let { giveDrill(click, it) }
        click.player.playSound(click.player, Sound.BLOCK_ANVIL_USE, 0.5f, 1.4f)
        click.update()
    }

    private fun unmount(click: SlotClickContext) {
        val placed = blockState.get(click)
        if (!MachineMenus.requireAccess(click, placed)) return
        val swap = DrillSwap.unmount(MinerBlock.tier(placed)) ?: return
        val drill = swap.returned?.let(DrillItem::of)?.build() ?: return
        if (click.player.inventory.firstEmpty() == -1) {
            click.player.sendRichMessage("<red>Seu inventário está cheio.")
            return
        }
        click.player.inventory.addItem(drill)
        MinerBlock.setTier(placed, swap.next)
        click.player.playSound(click.player, Sound.ENTITY_ITEM_PICKUP, 0.8f, 1f)
        click.update()
    }

    private fun giveDrill(click: SlotClickContext, tier: DrillTier) {
        val drill = DrillItem.of(tier)?.build() ?: return
        click.player.inventory.addItem(drill).values.forEach { click.player.world.dropItemNaturally(click.player.location, it) }
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

    private fun drillItem(tier: DrillTier): ItemStack {
        val lore = DrillItem.details(tier) + listOf(
            "",
            if (tier == DrillTier.IRON) "<yellow>Clique numa broca do seu inventário" else "<yellow>Clique para tirar a broca.",
            if (tier == DrillTier.IRON) "<yellow>para encaixar." else "<gray>Clique numa broca do inventário para trocar.",
        )
        return MachineMenus.icon(Material.PAPER, DrillItem.name(tier), lore).apply {
            editMeta { it.itemModel = MinerDisplay.drillModel(tier) }
        }
    }

    private fun powerItem(on: Boolean, tier: DrillTier): ItemStack = MachineMenus.icon(
        if (on) Material.REDSTONE_TORCH else Material.LEVER,
        if (on) "<green>Ligado" else "<red>Desligado",
        listOf(
            "<gray>Ligado, a broca gira sem parar e",
            "<gray>puxa <yellow>${tier.energyPerSecond} E/s <gray>o tempo todo.",
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
                "<gray>Broca: ${DrillItem.name(MinerBlock.tier(placed))}",
                "<gray>Energia no raio: <yellow>${EnergyNetwork.available(placed)} E",
                "<gray>Pilhas: <white>${placed.storageItems().size}/${MinerRules.STORAGE}",
                "",
                "<gray>Clique num item para retirá-lo.",
            ),
        )
    }
}
