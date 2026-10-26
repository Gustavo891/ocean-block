package ocean_block.system.islands.dialog

import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewFrame
import ocean_block.libs.formatter.NumberFormatter
import ocean_block.libs.instance.Instance
import ocean_block.system.currencies.Currencies
import ocean_block.system.islands.island.Island
import ocean_block.system.islands.island.IslandSetting
import ocean_block.system.islands.island.IslandUpgrade
import ocean_block.system.islands.view.IslandView
import ocean_block.system.islands.view.MembersView
import ocean_block.system.islands.view.SettingsView
import ocean_block.system.islands.view.UpgradeView
import ocean_block.system.islands.view.VisitsView
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogBinds
import one_block.textura.api.DialogClick
import one_block.textura.api.DialogOpenResult
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object IslandDialog {

    private const val SCREEN = "ilha"
    private const val WAIT_SECONDS = 4L
    private const val COINS = "coins"
    private const val MAX_SLOTS = MemberCards.SLOTS
    private const val NAME_CHARS = 7

    fun open(player: Player, island: Island, viewFrame: ViewFrame) {
        val members = listOf(island.ownerName) + memberNames(island).take(MAX_SLOTS - 1)
        val bust = PlayerSkins.bust(player.playerProfile).completeOnTimeout(null, WAIT_SECONDS, TimeUnit.SECONDS)
        val faces = members.map { PlayerSkins.face(it).completeOnTimeout(null, WAIT_SECONDS, TimeUnit.SECONDS) }
        CompletableFuture.allOf(bust, *faces.toTypedArray()).whenComplete { _, error ->
            Bukkit.getScheduler().runTask(Instance, Runnable {
                if (!player.isOnline) return@Runnable
                if (error != null) Instance.logger.warning("[ilha] menu da ilha sem skin: ${error.message}")
                val cards = members.mapIndexed { index, name ->
                    MemberCards.Card(faces[index].getNow(null), owner = index == 0, online = Bukkit.getPlayerExact(name) != null)
                }
                show(player, island, viewFrame, Portraits(bust.getNow(null), MemberCards.render(cards), members))
            })
        }
    }

    private class Portraits(val bust: BufferedImage?, val cards: BufferedImage, val members: List<String>)

    private fun memberNames(island: Island): List<String> = island.members.values.map { it.name }.sortedBy { it.lowercase() }

    private fun show(player: Player, island: Island, viewFrame: ViewFrame, portraits: Portraits) {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, island, viewFrame, portraits) }
        if (result == DialogOpenResult.MissingScreen || result == DialogOpenResult.Failed) {
            viewFrame.open(IslandView::class.java, player, island)
        }
    }

    private fun bind(binds: DialogBinds, player: Player, island: Island, viewFrame: ViewFrame, portraits: Portraits) {
        portraits.bust?.let { image -> binds.pixels("jogador") { image } }
        binds.pixels("cartoes") { portraits.cards }
        (0 until MAX_SLOTS).forEach { index ->
            val name = portraits.members.getOrNull(index)
            binds.text("cartao$index") { name?.uppercase()?.take(NAME_CHARS) ?: "CONVITE" }
            binds.text("cartao${index}_estilo") { if (name == null) "muted" else "body" }
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
        binds.button("abrir_membros") { click -> openView(click, viewFrame, MembersView::class.java, island) }
        binds.button("abrir_melhorias") { click -> openView(click, viewFrame, UpgradeView::class.java, island) }
        binds.button("abrir_configuracoes") { click -> openView(click, viewFrame, SettingsView::class.java, island) }
        binds.button("abrir_visitas") { click -> openView(click, viewFrame, VisitsView::class.java, island) }
        binds.button("abrir_nivel") { click -> command(click, "nivel") }
        binds.button("abrir_missoes") { click -> command(click, "missoes") }
        binds.button("abrir_receitas") { click -> command(click, "receitas") }
    }

    private fun openView(click: DialogClick, viewFrame: ViewFrame, view: Class<out View>, island: Island) {
        click.close()
        later(click) {
            it.playSound(it, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f)
            viewFrame.open(view, it, island)
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
