package one_block.textura.dialog

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class PlayerSettings(private val plugin: JavaPlugin) {

    fun get(player: Player, key: String): String? =
        namespaced(key)?.let { player.persistentDataContainer.get(it, PersistentDataType.STRING) }

    fun set(player: Player, key: String, value: String) {
        val namespaced = namespaced(key) ?: run {
            plugin.logger.warning("[ui] Chave de setting inválida '$key': use [a-z0-9_.-]")
            return
        }
        player.persistentDataContainer.set(namespaced, PersistentDataType.STRING, value)
    }

    private fun namespaced(key: String): NamespacedKey? =
        if (DialogValues.validKey(key)) NamespacedKey(plugin, "ui_$key") else null
}
