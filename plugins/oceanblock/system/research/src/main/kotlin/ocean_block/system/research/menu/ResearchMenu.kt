package ocean_block.system.research.menu

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.RenderContext
import me.devnatan.inventoryframework.context.SlotClickContext
import ocean_block.libs.formatter.Mini
import ocean_block.libs.item_builder.itemStack
import ocean_block.libs.item_builder.meta
import ocean_block.libs.item_builder.name
import ocean_block.libs.item_builder.setLore
import ocean_block.system.research.data.ResearchManager
import ocean_block.system.research.model.ResearchCategory
import ocean_block.system.research.model.ResearchDefinition
import ocean_block.system.research.model.ResearchStatus
import ocean_block.system.research.rules.ResearchRules
import ocean_block.system.research.tree.ResearchTree
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.UUID

class ResearchMenu : View() {

    private val island = initialState<UUID>()

    private val pagination = computedPaginationState(
        { ctx -> definitions(ctx.player).toMutableList() }
    ) { ctx, builder, _, def ->
        builder.renderWith { researchItem(island.get(ctx), def) }
            .onClick { click -> deliver(click, def) }
    }

    override fun onInit(config: ViewConfigBuilder) {
        config.cancelInteractions()
        config.title(Mini.mm.deserialize(TITLE))
        config.scheduleUpdate(UPDATE_TICKS)
        config.layout(
            "C OOOOOOO",
            "C OOOOOOO",
            "C OOOOOOO",
            "V OOOOOOO",
            "    < >  ",
        )
    }

    override fun onFirstRender(render: RenderContext) {
        ResearchCategory.entries.forEachIndexed { index, category ->
            render.slot(index + 1, 1)
                .renderWith { categoryItem(render.player, category) }
                .onClick { click -> select(click, category) }
        }
        render.layoutSlot('V').renderWith { slotsItem(island.get(render)) }
        render.layoutSlot('<')
            .renderWith { arrowItem("<yellow>Página anterior") }
            .onClick { click -> pagination.get(click).back() }
        render.layoutSlot('>')
            .renderWith { arrowItem("<yellow>Próxima página") }
            .onClick { click -> pagination.get(click).advance() }
    }

    private fun select(click: SlotClickContext, category: ResearchCategory) {
        ResearchSelections.set(click.player.uniqueId, category)
        click.player.playSound(click.player, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f)
        refresh(click, firstPage = true)
    }

    private fun deliver(click: SlotClickContext, def: ResearchDefinition) {
        val islandUuid = island.get(click)
        val now = System.currentTimeMillis()
        val status = ResearchManager.read(islandUuid) { ResearchRules.status(def, it, now) }
        if (status != ResearchStatus.Available) {
            click.player.playSound(click.player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 1f)
            return
        }
        ResearchDelivery.deliver(click.player, islandUuid, def)
        refresh(click, firstPage = false)
    }

    private fun refresh(context: Context, firstPage: Boolean) {
        val pages = pagination.get(context)
        if (firstPage) pages.switchTo(0)
        pages.forceUpdate()
        context.update()
    }

    private fun researchItem(islandUuid: UUID, def: ResearchDefinition): ItemStack {
        val now = System.currentTimeMillis()
        val (name, lore, done) = ResearchManager.read(islandUuid) { state ->
            Triple(ResearchLore.name(def, state, now), ResearchLore.lore(def, state, now), ResearchRules.status(def, state, now) == ResearchStatus.Done)
        }
        val material = Material.matchMaterial(def.icon.removePrefix(ITEM_PREFIX)) ?: Material.BOOK
        return itemStack(material) {
            meta {
                name(name)
                setLore { lore.forEach { +it } }
                if (done) addEnchant(Enchantment.UNBREAKING, 1, true)
                addItemFlags(*ItemFlag.entries.toTypedArray())
            }
        }
    }

    private fun categoryItem(player: Player, category: ResearchCategory): ItemStack {
        val active = category == ResearchSelections.get(player.uniqueId)
        val material = Material.matchMaterial(category.material) ?: Material.BOOK
        return itemStack(material) {
            meta {
                name(ResearchLore.categoryName(category, active))
                setLore { ResearchLore.categoryLore(active).forEach { +it } }
                if (active) addEnchant(Enchantment.UNBREAKING, 1, true)
                addItemFlags(*ItemFlag.entries.toTypedArray())
            }
        }
    }

    private fun slotsItem(islandUuid: UUID): ItemStack {
        val now = System.currentTimeMillis()
        val (name, lore) = ResearchManager.read(islandUuid) { state ->
            ResearchLore.slotsName(state.running.size, ResearchRules.slots(state, ResearchTree.byId)) to ResearchLore.queueLore(state, now)
        }
        return itemStack(Material.CLOCK) {
            meta {
                name(name)
                setLore { lore.forEach { +it } }
            }
        }
    }

    private fun arrowItem(label: String): ItemStack = itemStack(Material.PAPER) {
        meta {
            itemModel = BLANK
            name(label)
        }
    }

    private fun definitions(player: Player): List<ResearchDefinition> {
        val category = ResearchSelections.get(player.uniqueId)
        return ResearchTree.all.filter { it.category == category }
    }

    companion object {
        private const val TITLE = "<white><shift:-8><glyph:pesquisa_menu><font:oneblock:gui><shift:-170><dark_gray>Pesquisa"
        private const val ITEM_PREFIX = "item:"
        private const val UPDATE_TICKS = 20L
        private val BLANK = NamespacedKey("oneblock", "blank")
    }
}
