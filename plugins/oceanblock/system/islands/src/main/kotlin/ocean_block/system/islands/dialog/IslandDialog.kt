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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object IslandDialog {

    const val REFRESH_TICKS = 5L

    private const val SCREEN = "ilha"
    private const val WAIT_SECONDS = 4L
    private const val COINS = "coins"
    private const val FRAME_MILLIS = REFRESH_TICKS * 50

    private val viewers = ConcurrentHashMap.newKeySet<UUID>()

    fun open(player: Player, island: Island, viewFrame: ViewFrame) {
        PlayerSkins.frames(player).completeOnTimeout(emptyList(), WAIT_SECONDS, TimeUnit.SECONDS).whenComplete { frames, error ->
            Bukkit.getScheduler().runTask(Instance, Runnable {
                if (!player.isOnline) return@Runnable
                if (error != null) Instance.logger.warning("[ilha] menu da ilha sem skin: ${error.message}")
                show(player, island, viewFrame, frames.orEmpty())
            })
        }
    }

    fun refresh() {
        viewers.removeIf { id ->
            val player = Bukkit.getPlayer(id)
            player == null || !player.isOnline || !DialogApi.refresh(player, SCREEN)
        }
    }

    fun forget(player: UUID) {
        viewers.remove(player)
    }

    private fun show(player: Player, island: Island, viewFrame: ViewFrame, frames: List<BufferedImage>) {
        val result = DialogApi.openResult(player, SCREEN) { binds -> bind(binds, player, island, viewFrame, frames) }
        when (result) {
            DialogOpenResult.Shown -> if (frames.size > 1) viewers += player.uniqueId
            DialogOpenResult.MissingScreen, DialogOpenResult.Failed -> viewFrame.open(IslandView::class.java, player, island)
            else -> Unit
        }
    }

    private fun bind(binds: DialogBinds, player: Player, island: Island, viewFrame: ViewFrame, frames: List<BufferedImage>) {
        if (frames.isNotEmpty()) binds.pixels("jogador") { frames[((System.currentTimeMillis() / FRAME_MILLIS) % frames.size).toInt()] }
        binds.text("titulo") { if (island.ownerUuid == player.uniqueId) "SUA ILHA" else "ILHA DE ${island.ownerName.uppercase()}" }
        binds.text("dono") { island.ownerName.uppercase() }
        binds.text("status") { if (island.isPublic) "PÚBLICA" else "PRIVADA" }
        binds.text("status_estilo") { if (island.isPublic) "good" else "bad" }
        binds.text("membros") { "${island.members.size}/${island.maxMembers()} MEMBROS" }
        binds.text("borda") { "BORDA ${island.borderSize()}x${island.borderSize()}" }
        binds.text("gerador") { "NÍVEL ${island.getUpgradeLevel(IslandUpgrade.Generator)}" }
        binds.text("moedas") { NumberFormatter.format(Currencies.findCurrency(COINS)?.find(player.uniqueId)?.balance ?: 0.0) }
        binds.text("configuracoes") { "${IslandSetting.entries.count(island::isSettingEnabled)} DE ${IslandSetting.entries.size} LIGADAS" }
        binds.text("visitas") { "${if (island.isPublic) "PÚBLICA" else "PRIVADA"} | ${island.blackListNames().size} BLOQ." }

        binds.button("fechar") { click -> close(click) }
        binds.button("abrir_ilha") { click ->
            close(click)
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

    private fun close(click: DialogClick) {
        viewers.remove(click.player.uniqueId)
        click.close()
    }

    private fun openView(click: DialogClick, viewFrame: ViewFrame, view: Class<out View>, island: Island) {
        close(click)
        later(click) {
            it.playSound(it, Sound.UI_BUTTON_CLICK, 0.6f, 1.2f)
            viewFrame.open(view, it, island)
        }
    }

    private fun command(click: DialogClick, command: String) {
        close(click)
        later(click) { it.performCommand(command) }
    }

    private fun later(click: DialogClick, action: (Player) -> Unit) {
        val player = click.player
        Bukkit.getScheduler().runTask(Instance, Runnable { if (player.isOnline) action(player) })
    }
}
