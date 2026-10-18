package one_block.textura.dialog

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import one_block.textura.api.DialogBinds
import one_block.textura.dialog.json.DefStep
import one_block.textura.dialog.node.UiOutcome
import one_block.textura.dialog.text.Placeholders
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object DialogActions {

    fun run(steps: List<DefStep>, player: Player, session: DialogSession, warn: (String) -> Unit): UiOutcome {
        val placeholders = Placeholders(session)
        for (step in steps) {
            when (step) {
                is DefStep.Set -> step.pairs.forEach { (key, value) -> session.set(key, placeholders.plain(value)) }
                is DefStep.Message -> player.sendMessage(placeholders.rich(step.text))
                is DefStep.Command -> {
                    val line = placeholders.command(step.line) { key -> warn("valor de '{$key}' recusado em comando") }
                        ?: return UiOutcome.Refresh
                    Bukkit.dispatchCommand(if (step.console) Bukkit.getConsoleSender() else player, line)
                }
                is DefStep.Sound -> {
                    val key = runCatching { Key.key(step.id) }.getOrNull()
                    if (key == null) warn("som '${step.id}' inválido") else player.playSound(Sound.sound(key, Sound.Source.MASTER, step.volume, step.pitch))
                }
                is DefStep.Page -> return UiOutcome.Page(step.id)
                is DefStep.Open -> return UiOutcome.Open(step.id, DialogBinds(), step.args.mapValues { placeholders.plain(it.value) })
                DefStep.Close -> return UiOutcome.Close
                DefStep.Refresh -> Unit
            }
        }
        return UiOutcome.Refresh
    }
}
