package ocean_block.system.islands.dialog

import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.instance.Instance
import ocean_block.system.currencies.Currencies
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandSetting
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.islands.view.IslandView
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogClick
import one_block.textura.api.DialogOpenResult
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture

object IslandDialog {

    private const val SCREEN = "ilha"
    private const val COINS = "coins"
    private const val MAX_SLOTS = MemberCards.SLOTS

    fun open(player: Player, island: Island, viewFrame: ViewFrame) {
        val bust = IslandPortraits.bust(player.playerProfile)
        val cards = IslandPortraits.cards(island)
        CompletableFuture.allOf(bust, cards).whenComplete { _, error ->
            if (error != null) Instance.logger.warning("[ilha] menu da ilha sem skin: ${error.message}")
            IslandScreens.later(player) {
                show(it, island, viewFrame, Portraits(bust.getNow(null), cards.getNow(null)))
            }
        }
    }

    private class Portraits(val bust: BufferedImage?, val cards: IslandPortraits.Cards?)

    private fun show(player: Player, island: Island, viewFrame: ViewFrame, portraits: Portraits) {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, island, viewFrame, portraits) }
        if (result == DialogOpenResult.MissingScreen || result == DialogOpenResult.Failed) {
            viewFrame.open(IslandView::class.java, player, island)
        }
    }

    private fun bind(binds: DialogBinds, player: Player, island: Island, viewFrame: ViewFrame, portraits: Portraits) {
        portraits.bust?.let { image -> binds.pixels("jogador") { image } }
        portraits.cards?.let { cards -> binds.pixels("cartoes") { cards.image } }
        (0 until MAX_SLOTS).forEach { index ->
            val name = portraits.cards?.members?.getOrNull(index)
            binds.text("cartao$index") { IslandPortraits.tooltip(name, index) }
            binds.button("clicar_cartao$index") { click ->
                if (name == null && island.ownerUuid == player.uniqueId) NameDialog.invite(click, island) { open(it, island, viewFrame) }
                else dialog(click) { MembersDialog.open(it, island, viewFrame) }
            }
        }
        binds.text("titulo") { if (island.ownerUuid == player.uniqueId) "SUA ILHA" else "ILHA DE ${island.ownerName.uppercase()}" }
        binds.text("status") { if (island.isPublic) "PÚBLICA" else "PRIVADA" }
        binds.text("status_estilo") { if (island.isPublic) "good" else "bad" }
        binds.text("membros") { "${island.members.size}/${island.maxMembers()} MEMBROS" }
        binds.text("borda") { "BORDA ${island.borderSize()}x${island.borderSize()}" }
        binds.text("gerador") { "NÍVEL ${island.getUpgradeLevel(IslandUpgrade.Generator)}" }
        binds.text("configuracoes") { "${IslandSetting.entries.count(island::isSettingEnabled)} DE ${IslandSetting.entries.size} LIGADAS" }
        binds.text("visitas") { "${if (island.isPublic) "PÚBLICA" else "PRIVADA"} | ${island.blackListNames().size} BLOQ." }
        binds.text("moedas") { NumberFormatter.format(Currencies.findCurrency(COINS)?.find(player.uniqueId)?.balance ?: 0.0) }

        binds.button("fechar") { click -> click.close() }
        binds.button("abrir_ilha") { click ->
            click.close()
            later(click) { island.teleportTo(it) }
        }
        binds.button("abrir_melhorias") { click -> dialog(click) { UpgradesDialog.open(it, island, viewFrame) } }
        binds.button("abrir_configuracoes") { click -> dialog(click) { SettingsDialog.open(it, island, viewFrame) } }
        binds.button("abrir_visitas") { click -> dialog(click) { VisitsDialog.open(it, island, viewFrame) } }
        binds.button("abrir_nivel") { click -> command(click, "nivel") }
        binds.button("abrir_missoes") { click -> command(click, "missoes") }
        binds.button("abrir_receitas") { click -> command(click, "receitas") }
    }

    private fun dialog(click: DialogClick, open: (Player) -> Unit) {
        IslandScreens.later(click.player) {
            it.playSound(it, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f)
            open(it)
        }
    }

    private fun command(click: DialogClick, command: String) {
        click.close()
        later(click) { it.performCommand(command) }
    }

    private fun later(click: DialogClick, action: (Player) -> Unit) {
        val player = click.player
        Bukkit.getScheduler().runTask(Instance, Runnable { if (player.isOnline) action(player) })
    }
}
