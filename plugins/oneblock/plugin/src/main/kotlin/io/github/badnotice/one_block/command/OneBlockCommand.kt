package io.github.badnotice.one_block.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import one_block.libs.toast.ToastApi
import one_block.libs.toast.ToastType
import one_block.system.sell.manager.SellManager
import org.bukkit.Material
import org.bukkit.entity.Player

@CommandAlias("oneblock")
class OneBlockCommand : BaseCommand() {

    @Subcommand("reload")
    fun onReload(sender: Player) {
        SellManager.load()

        sender.sendRichMessage("<green>Recarregado com sucesso!")
    }

    @Subcommand("toast")
    @CommandPermission("oneblock.admin")
    fun onToast(sender: Player, type: String, icon: String, text: String) {
        val toastType = ToastType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) }
        if (toastType == null) {
            val tipos = ToastType.entries.joinToString(", ") { it.name.lowercase() }
            sender.sendRichMessage("<red>Tipo inválido. Disponíveis: <white>$tipos")
            return
        }

        val material = Material.matchMaterial(icon)
        if (material == null || !material.isItem) {
            sender.sendRichMessage("<red>Material inválido: <white>$icon")
            return
        }

        ToastApi.send(sender, toastType, material, text)
    }
}
