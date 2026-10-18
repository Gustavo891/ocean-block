package one_block.textura.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import org.bukkit.entity.Player
import one_block.textura.api.HudApi
import one_block.textura.hud.HudContent
import one_block.textura.hud.HudSecondary
import one_block.textura.hud.HudSide

/**
 * Comando de demonstração da [HudApi]. Usa sempre o slot `"test"`.
 *
 * - `/hud show <texto>` — cria/atualiza a HUD com o texto dado.
 * - `/hud sec <left|right> <texto>` — HUD secundária na mesma linha.
 * - `/hud unsec` — remove só a secundária.
 * - `/hud hide` — remove a HUD.
 */
@CommandAlias("hud")
@CommandPermission("textura.admin")
class HudCommand : BaseCommand() {

    private companion object {
        const val SLOT = "test"
    }

    @Subcommand("show")
    fun show(player: Player, texto: Array<String>) {
        val raw = texto.joinToString(" ").ifBlank { "Teste" }
        HudApi.show(player, SLOT, HudContent(text = raw))
        player.sendMessage("§aHUD exibida.")
    }

    @Subcommand("sec")
    fun secondary(player: Player, lado: String, texto: Array<String>) {
        val side = when (lado.lowercase()) {
            "left", "esquerda" -> HudSide.LEFT
            "right", "direita" -> HudSide.RIGHT
            else -> {
                player.sendMessage("§cLado inválido. Use left ou right.")
                return
            }
        }
        if (!HudApi.has(player, SLOT)) {
            player.sendMessage("§cCrie a HUD principal antes com /hud show.")
            return
        }
        val raw = texto.joinToString(" ").ifBlank { "Teste" }
        HudApi.showSecondary(player, SLOT, HudSecondary(text = raw, side = side))
        player.sendMessage("§aHUD secundária exibida.")
    }

    @Subcommand("unsec")
    fun unsecondary(player: Player) {
        HudApi.hideSecondary(player, SLOT)
        player.sendMessage("§eHUD secundária removida.")
    }

    @Subcommand("hide")
    fun hide(player: Player) {
        HudApi.hide(player, SLOT)
        player.sendMessage("§eHUD removida.")
    }
}
