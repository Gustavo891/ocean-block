package ocean_block.system.islands.choice

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import ocean_block.system.islands.island.type.IslandType
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

class ChoiceHolder(val confirming: IslandType?) : InventoryHolder {
    lateinit var menu: Inventory

    override fun getInventory(): Inventory = menu
}

object IslandTypeMenu {

    private const val SIZE = 27
    private const val TITLE = "Escolha sua ilha"
    private const val PREVIEW_SLOT = 13
    const val CONFIRM_SLOT = 11
    const val BACK_SLOT = 15

    val TYPE_SLOTS = mapOf(10 to IslandType.PADRAO, 12 to IslandType.DESERTO, 14 to IslandType.GELO, 16 to IslandType.JANGADA)

    private val ICONS = mapOf(
        IslandType.PADRAO to Material.GRASS_BLOCK,
        IslandType.DESERTO to Material.SAND,
        IslandType.GELO to Material.SNOW_BLOCK,
        IslandType.JANGADA to Material.OAK_PLANKS,
    )

    fun open(player: Player) {
        val counts = IslandChoice.counts()
        val menu = create(ChoiceHolder(null), TITLE)
        TYPE_SLOTS.forEach { (slot, type) ->
            val lore = listOf(type.descricao, ChoiceRules.countText(counts[type] ?: 0), "Clique para escolher")
            menu.setItem(slot, item(ICONS.getValue(type), type.nome, lore))
        }
        player.openInventory(menu)
    }

    fun confirm(player: Player, type: IslandType) {
        val menu = create(ChoiceHolder(type), "Criar a ilha ${type.nome}?")
        menu.setItem(CONFIRM_SLOT, item(Material.LIME_WOOL, "Confirmar", listOf("Não dá para trocar depois.")))
        menu.setItem(PREVIEW_SLOT, item(ICONS.getValue(type), type.nome, listOf(type.descricao)))
        menu.setItem(BACK_SLOT, item(Material.RED_WOOL, "Voltar", emptyList()))
        player.openInventory(menu)
    }

    private fun create(holder: ChoiceHolder, title: String): Inventory =
        Bukkit.createInventory(holder, SIZE, Component.text(title)).also { holder.menu = it }

    private fun item(material: Material, name: String, lore: List<String>): ItemStack = ItemStack(material).apply {
        editMeta { meta ->
            meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
            meta.lore(lore.map { Component.text(it, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false) })
        }
    }
}
