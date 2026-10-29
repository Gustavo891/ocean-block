package ocean_block.system.islands.choice

import net.kyori.adventure.text.Component
import ocean_block.system.islands.dialog.IslandScreens
import ocean_block.system.islands.island.type.IslandType
import org.bukkit.entity.Player

object IslandTypeDialog {

    private const val LIST = "ilha_tipo"
    private const val CONFIRM = "ilha_tipo_confirmar"
    private const val FAREWELL = "Até a próxima!"

    fun open(player: Player) {
        val rows = ChoiceRules.rows(IslandChoice.counts())
        IslandScreens.show(player, LIST, { IslandTypeMenu.open(it) }) { binds ->
            rows.forEach { row ->
                val id = row.getValue("id")
                binds.text("${id}_nome") { row.getValue("nome") }
                binds.text("${id}_descricao") { row.getValue("descricao").uppercase() }
                binds.text("${id}_contagem") { row.getValue("contagem").uppercase() }
                binds.button("escolher_$id") { click ->
                    IslandScreens.later(click.player) { confirm(it, IslandType.fromId(id)) }
                }
            }
            binds.closed { leave(player) }
        }
    }

    fun confirm(player: Player, type: IslandType) {
        IslandScreens.show(player, CONFIRM, { IslandTypeMenu.confirm(it, type) }) { binds ->
            binds.text("tipo") { type.id }
            binds.text("pergunta") { "CRIAR A ILHA ${type.nome.uppercase()}?" }
            binds.button("confirmar") { click -> IslandScreens.later(click.player) { IslandChoice.confirm(it, type) } }
            binds.button("voltar") { click -> IslandScreens.later(click.player) { open(it) } }
            binds.closed { leave(player) }
        }
    }

    fun leave(player: Player) {
        if (player.isOnline) player.kick(Component.text(FAREWELL))
    }
}
