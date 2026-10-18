package ocean_block.system.craft.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.system.craft.CraftStation
import ocean_block.system.craft.forge.ForgeRecipeManager
import ocean_block.system.craft.view.CraftView
import ocean_block.system.craft.view.ForgeCategoriesView
import org.bukkit.entity.Player

@CommandAlias("forja|forge")
class ForgeCommand(
    private val viewFrame: ViewFrame
) : BaseCommand() {

    @Default
    fun onDefault(sender: Player) {
        val categorias = ForgeRecipeManager.categoriesOf(CraftStation.FORJA)

        if (categorias.isEmpty()) {
            sender.sendRichMessage("<red>A forja ainda não tem nenhuma receita.")
            return
        }

        if (categorias.size == 1) {
            viewFrame.open(CraftView::class.java, sender, categorias.first())
            return
        }

        viewFrame.open(ForgeCategoriesView::class.java, sender)
    }
}
