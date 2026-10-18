package one_block.system.craft.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import one_block.system.craft.CraftStation
import one_block.system.craft.forge.ForgeRecipeManager
import one_block.system.craft.view.CraftCategoriesView
import one_block.system.craft.view.CraftView
import org.bukkit.entity.Player

@CommandAlias("craft")
class CraftCommand(
    private val viewFrame: ViewFrame
): BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        val categorias = ForgeRecipeManager.categoriesOf(CraftStation.CRAFT)

        if (categorias.isEmpty()) {
            sender.sendRichMessage("<red>Nenhuma receita disponível.")
            return
        }

        if (categorias.size == 1) {
            viewFrame.open(CraftView::class.java, sender, categorias.first())
            return
        }

        viewFrame.open(CraftCategoriesView::class.java, sender)
    }
}
