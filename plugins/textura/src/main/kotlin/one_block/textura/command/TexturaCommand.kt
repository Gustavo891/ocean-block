package one_block.textura.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Optional
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import one_block.textura.api.DialogApi
import one_block.textura.api.DialogOpenResult
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.JoinConfiguration
import one_block.textura.text.TextMetrics
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import one_block.textura.TexturaPlugin
import one_block.textura.glyph.GlyphImportResult
import one_block.textura.glyph.GlyphImporter
import one_block.textura.pack.PackAssets
import one_block.textura.hud.anchor.HudAnchorProbe

@CommandAlias("textura")
@CommandPermission("textura.admin")
class TexturaCommand(private val plugin: TexturaPlugin) : BaseCommand() {

    @Subcommand("uispike")
    fun uiSpike(player: Player) {
        fun bar(shift: Int) = Component.empty().append(TextMetrics.getSpacingComponent(shift)).append(Component.text("|"))
        val hover = HoverEvent.showText(Component.text("span"))
        val lines = listOf(
            bar(398),
            bar(399),
            Component.text("|"),
            Component.empty().append(TextMetrics.getSpacingComponent(100).hoverEvent(hover)),
            Component.empty().append(TextMetrics.getSpacingComponent(100).hoverEvent(hover)).append(Component.text("|")),
            Component.text("||||||||||"),
            Component.text("||||||||||"),
        )
        val body = DialogBody.plainMessage(Component.join(JoinConfiguration.newlines(), lines), 408)
        val dialog = Dialog.create { factory ->
            factory.empty()
                .base(DialogBase.builder(Component.text("spike")).body(listOf(body)).build())
                .type(DialogType.notice())
        }
        player.showDialog(dialog)
    }

    @Subcommand("dialog")
    @CommandCompletion("@uiscreens @players")
    fun dialog(sender: CommandSender, id: String, @Optional target: OnlinePlayer?) {
        val player = target?.player ?: sender as? Player ?: run {
            sender.sendMessage("§cUso: /textura dialog <id> <jogador>")
            return
        }
        when (DialogApi.openResult(player, id)) {
            DialogOpenResult.Shown -> Unit
            DialogOpenResult.NeedsPack -> sender.sendMessage("§eTela '$id' não abriu: o jogador está sem a textura ou o pack ainda não foi gerado.")
            DialogOpenResult.MissingScreen -> sender.sendMessage("§cTela '$id' não existe em configs/ui.")
            DialogOpenResult.Failed -> sender.sendMessage("§cTela '$id' não abriu: tema inexistente ou erro ao montar. Veja o console.")
        }
    }

    @Subcommand("hudtest")
    @CommandCompletion("on|off")
    fun hudTest(player: Player, @Optional estado: String?) {
        if (!player.isOp) {
            player.sendMessage("§cSó operadores podem usar este teste.")
            return
        }
        when (estado?.lowercase() ?: "on") {
            "on" -> {
                HudAnchorProbe.show(player)
                player.sendMessage("§aHUD de teste ancorada exibida. Sem o shader do pack ela aparece embolada no topo.")
            }
            "off" -> {
                HudAnchorProbe.hide(player)
                player.sendMessage("§eHUD de teste removida.")
            }
            else -> player.sendMessage("§cUso: /textura hudtest [on|off]")
        }
    }

    @Subcommand("gerar")
    fun gerar(sender: CommandSender) {
        sender.sendMessage("§aGerando resource pack...")
        plugin.generatePack(sender)
    }

    @Subcommand("aplicar")
    fun aplicar(sender: CommandSender, player: Player) {
        plugin.applyPack(player)
        sender.sendMessage("§aTextura aplicada para ${player.name}")
    }

    @Subcommand("aplicarall")
    fun aplicarAll(sender: CommandSender) {
        for (on in Bukkit.getOnlinePlayers()) {
            plugin.applyPack(on)
        }
        sender.sendMessage("§aTextura aplicada para todos.")
    }

    @Subcommand("reload")
    fun reload(sender: CommandSender) {
        if (plugin.isGenerating) {
            sender.sendMessage("§eUma geracao do pack esta em andamento. Rode o reload quando ela terminar.")
            return
        }
        plugin.reloadConfigData()
        sender.sendMessage("§aTextura reloaded.")
    }

    @Subcommand("importglyph")
    fun importGlyph(sender: CommandSender, nome: String, args: Array<String>) {
        val unicode: String?
        val link: String
        when (args.size) {
            1 -> { unicode = null; link = args[0] }
            2 -> { unicode = args[0]; link = args[1] }
            else -> {
                sender.sendMessage("§cUso: /textura importglyph <nome> [unicode] <link.png>")
                return
            }
        }

        if (plugin.glyphRegistry.glyphs.containsKey(nome)) {
            sender.sendMessage("§cJa existe um glyph com o id '$nome'.")
            return
        }
        if (unicode != null && plugin.glyphRegistry.glyphs.values.any {
                it.unicode.equals(unicode, ignoreCase = true) || it.char == unicode
            }) {
            sender.sendMessage("§cEsse unicode ja esta em uso por outro glyph.")
            return
        }

        sender.sendMessage("§eBaixando imagem e importando glyph '$nome'...")
        val glyphsFolder = PackAssets(plugin.dataFolder).configFolder("glyphs")
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val result = GlyphImporter(glyphsFolder).import(nome, unicode, link)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                when (result) {
                    is GlyphImportResult.Failure ->
                        sender.sendMessage("§cFalha ao importar: ${result.reason}")
                    is GlyphImportResult.Success -> {
                        if (plugin.isGenerating) {
                            sender.sendMessage("§eGlyph '${result.id}' importado. Uma geracao ja esta em andamento: rode /textura reload e /textura gerar quando ela terminar.")
                        } else {
                            sender.sendMessage("§aGlyph '${result.id}' importado (${result.caminho}). Regenerando pack...")
                            plugin.reloadConfigData()
                            plugin.generatePack(sender)
                        }
                    }
                }
            })
        })
    }

    @Subcommand("glyphs")
    fun glyphs(sender: CommandSender) {
        sender.sendMessage("§e--- Lista de Glyphs ---")
        if (plugin.glyphRegistry.glyphs.isEmpty()) {
            sender.sendMessage("§cNenhum glyph registrado.")
            return
        }
        for (glyph in plugin.glyphRegistry.glyphs.values) {
            val registeredFonts = buildList {
                if (glyph.default) add("default")
                addAll(glyph.fonts.keys)
            }.joinToString(", ")

            val message = Component.text("§7- ")
                .append(Component.text("§f${glyph.id}")
                    .clickEvent(ClickEvent.copyToClipboard(glyph.char))
                    .hoverEvent(HoverEvent.showText(Component.text("§aClique para copiar o caractere (área de transferência)"))))
                .append(Component.text(" §8($registeredFonts)"))
            val full = if (glyph.showInGui) message.append(Component.text(" §7- §b[§f${glyph.char}§b]")) else message

            sender.sendMessage(full)
        }
    }

    @Subcommand("fonts")
    fun fonts(sender: CommandSender) {
        sender.sendMessage("§e--- Lista de Fontes ---")
        if (plugin.fontRegistry.fonts.isEmpty()) {
            sender.sendMessage("§cNenhuma fonte registrada.")
            return
        }
        for (font in plugin.fontRegistry.fonts.values) {
            sender.sendMessage("§7- §f${font.id} §8(Ascent: ${font.ascent})")
        }
    }

    @Subcommand("testfont")
    fun testfont(sender: CommandSender, font: String, text: Array<String>) {
        if (!plugin.fontRegistry.fonts.containsKey(font)) {
            sender.sendMessage("§cFonte '$font' não encontrada.")
            return
        }

        val fullText = text.joinToString(" ")

        val message = Component.text("\n\n\n")
            .append(Component.text(fullText).font(net.kyori.adventure.key.Key.key("minecraft", font)))
            .append(Component.text("\n\n\n"))

        sender.sendMessage(message)
    }

    @Subcommand("itens")
    fun itens(sender: Player) {
        plugin.viewFrame.open(one_block.textura.menu.ItemsMenu::class.java, sender)
    }

    @Subcommand("blocos")
    fun blocos(sender: Player) {
        plugin.viewFrame.open(one_block.textura.menu.BlocksMenu::class.java, sender)
    }
}
