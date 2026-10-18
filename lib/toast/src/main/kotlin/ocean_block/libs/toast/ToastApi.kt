package ocean_block.libs.toast

import net.kyori.adventure.text.Component
import ocean_block.libs.formatter.Mini
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object ToastApi {

    fun send(player: Player, type: ToastType, icon: Material, text: String) {
        send(player, type, ItemStack(icon), Mini.deserialize(text))
    }

    fun send(player: Player, type: ToastType, icon: ItemStack, text: Component) {
        ToastService.send(player, type, icon, text)
    }

    fun send(players: Collection<Player>, type: ToastType, icon: Material, text: String) {
        if (players.isEmpty()) return

        val stack = ItemStack(icon)
        val component = Mini.deserialize(text)

        players.forEach { ToastService.send(it, type, stack, component) }
    }
}
